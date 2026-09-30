package com.evo2.assistant

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

class VoiceCommandService : Service() {
    companion object {
        const val ACTION_START = "com.evo2.assistant.START_VOICE"
        const val ACTION_STOP = "com.evo2.assistant.STOP_VOICE"
        @Volatile var isRunning = false
            private set
        private const val CHANNEL_ID = "evo_voice_listener"
        private const val NOTIFICATION_ID = 202
    }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var localeIndex = 0
    private val locales = listOf("az-AZ", "tr-TR", "ru-RU", "en-US")

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isRunning) {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, notification("Başlamaq üçün dinləyirəm…"))
            isRunning = true
            listenAgain(250)
        }
        return START_STICKY
    }

    private fun listenAgain(delayMs: Long) {
        handler.postDelayed({ if (isRunning) listen() }, delayMs)
    }

    private fun listen() {
        if (!isRunning) return
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateNotification("Səs tanıma xidməti yoxdur. Google tətbiqini yoxla.")
            listenAgain(3000)
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            updateNotification("Mikrofon icazəsi yoxdur.")
            stopSelf()
            return
        }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { speech ->
                speech.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = updateNotification("Dinləyirəm · EVO-2 açıqdır")
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = updateNotification("Əmr yoxlanılır…")
                    override fun onError(error: Int) {
                        if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                            localeIndex = (localeIndex + 1).coerceAtMost(locales.lastIndex)
                        }
                        val message = if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) "Mikrofon icazəsini yoxla" else "Dinləyirəm · yenidən yoxlanır"
                        updateNotification(message)
                        listenAgain(if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) 1600 else 900)
                    }
                    override fun onResults(results: Bundle?) {
                        val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                        if (phrase.isNotBlank()) handleCommand(phrase)
                        listenAgain(650)
                    }
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                val recognitionIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, locales[localeIndex])
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, locales[localeIndex])
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                }
                speech.startListening(recognitionIntent)
            }
        } catch (_: Exception) {
            listenAgain(1400)
        }
    }

    private fun handleCommand(phrase: String) {
        val command = VoiceCommands.parse(phrase)
        when (command.type) {
                VoiceCommandType.YOUTUBE_HOME -> {
                    val opened = VoiceAccessibilityService.current?.openYoutube()
                        ?: openYoutubeFromService(null)
                    updateNotification(if (opened) "YouTube açıldı · “aşağı sürüşdür” deyə bilərsən" else "YouTube açıla bilmədi")
                }
                VoiceCommandType.YOUTUBE_SEARCH -> {
                    val opened = VoiceAccessibilityService.current?.openYoutube(command.query)
                        ?: openYoutubeFromService(command.query)
                    updateNotification(if (opened) "YouTube-da axtarılır: ${command.query}" else "YouTube açıla bilmədi")
                }
                VoiceCommandType.FLASH_ON -> setFlashlight(true)
                VoiceCommandType.FLASH_OFF -> setFlashlight(false)
                VoiceCommandType.SCROLL_DOWN -> {
                    val scrolled = VoiceAccessibilityService.current?.scrollScreenDown() == true
                    updateNotification(if (scrolled) "Ekran aşağı sürüşdürüldü" else "Ekran əmri üçün EVO-2 Əlçatanlıq xidmətini aktiv et")
                }
                VoiceCommandType.UNKNOWN -> updateNotification("Əmr: $phrase · Dinləyirəm")
        }
    }

    private fun openYoutubeFromService(query: String?): Boolean {
        return try {
            val intent = if (query.isNullOrBlank()) {
                packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                    ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/"))
            } else {
                Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com/results?search_query=${android.net.Uri.encode(query)}"))
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            true
        } catch (_: Exception) { false }
    }

    private fun setFlashlight(enabled: Boolean) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            updateNotification("Fənəri işlətmək üçün EVO-2-yə kamera icazəsi ver")
            return
        }
        try {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = manager.cameraIdList.firstOrNull { cameraId ->
                manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (id == null) updateNotification("Bu cihazda fənər yoxdur")
            else {
                manager.setTorchMode(id, enabled)
                updateNotification(if (enabled) "Fənər yandırıldı · dinləyirəm" else "Fənər söndürüldü · dinləyirəm")
            }
        } catch (_: Exception) {
            updateNotification("Fənər işləmədi; kamera icazəsini yoxla")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "EVO-2 səsli dinləmə", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(message: String): Notification {
        val stopIntent = Intent(this, VoiceCommandService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("EVO-2 səs əmrləri")
            .setContentText(message)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Dayandır", stopPending)
        return builder.build()
    }

    private fun updateNotification(message: String) {
        if (!isRunning) return
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(message))
    }

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
