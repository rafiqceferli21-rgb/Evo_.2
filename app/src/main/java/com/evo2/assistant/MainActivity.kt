package com.evo2.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var statusView: TextView
    private lateinit var transcriptView: TextView
    private lateinit var songInput: EditText
    private lateinit var contactInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var voiceButton: Button
    private var selectedPhoneNumber: String? = null
    private var torchCameraId: String? = null
    private var torchOn = false
    private var pendingTorchState: Boolean? = null

    companion object {
        private const val MIC_PERMISSION = 20
        private const val CAMERA_PERMISSION = 21
        private const val CONTACT_PICKER = 22
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(11, 16, 32)
        window.navigationBarColor = Color.rgb(11, 16, 32)
        window.decorView.systemUiVisibility = 0
        buildScreen()
    }

    private fun buildScreen() {
        val background = Color.rgb(11, 16, 32)
        val card = Color.rgb(22, 30, 52)
        val accent = Color.rgb(102, 226, 196)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(24))
            setBackgroundColor(background)
        }
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(content)

        content.addView(label("EVO-2", 30f, Color.WHITE, true))
        content.addView(label("Səsli köməkçin hazırdır", 15f, Color.rgb(169, 181, 205), false).also {
            it.setPadding(0, dp(3), 0, dp(18))
        })

        val voiceCard = panel(card)
        voiceCard.addView(label("SƏSLİ ƏMR", 12f, accent, true))
        voiceCard.addView(label("Mahnı axtar, fənəri idarə et və ya mesaj hazırla.", 14f, Color.WHITE, false).also {
            it.setPadding(0, dp(8), 0, dp(14))
        })
        voiceButton = actionButton("🎙  Daimi dinləməni başlat", accent, background)
        voiceButton.setOnClickListener { toggleVoiceListening() }
        voiceCard.addView(voiceButton, matchWrap())
        transcriptView = label("Əmr burada görünəcək", 14f, Color.rgb(190, 201, 220), false).also {
            it.setPadding(0, dp(12), 0, 0)
        }
        voiceCard.addView(transcriptView)
        voiceCard.addView(label("YouTube açılandan sonra da dinləyəcək. “Ekranı aşağı sürüşdür” əmri üçün Əlçatanlıq icazəsini aktiv et.", 13f, Color.rgb(169, 181, 205), false).also {
            it.setPadding(0, dp(12), 0, dp(8))
        })
        val accessibilityButton = actionButton("Sürüşdürmə icazəsini aktiv et", Color.rgb(49, 67, 98), Color.WHITE)
        accessibilityButton.setOnClickListener {
            try { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            catch (_: Exception) { sayStatus("Parametrlərdən Əlçatanlıq bölməsini aç.") }
        }
        voiceCard.addView(accessibilityButton, matchWrap())
        content.addView(voiceCard, matchWrap(dp(14)))

        val musicCard = panel(card)
        musicCard.addView(label("YouTube musiqi axtarışı", 18f, Color.WHITE, true))
        musicCard.addView(label("Axtarış nəticəsini aç; ifanı YouTube-da özün seç.", 13f, Color.rgb(169, 181, 205), false).also {
            it.setPadding(0, dp(5), 0, dp(12))
        })
        songInput = input("Mahnının adı")
        musicCard.addView(songInput, matchWrap(dp(9)))
        val youtubeButton = actionButton("YouTube-da axtar", Color.rgb(255, 98, 98), Color.WHITE)
        youtubeButton.setOnClickListener {
            val query = songInput.text.toString().trim()
            if (query.isBlank()) sayStatus("Əvvəlcə mahnının adını daxil et və ya səsli əmr ver.")
            else openYouTube(query)
        }
        musicCard.addView(youtubeButton, matchWrap())
        content.addView(musicCard, matchWrap(dp(14)))

        val messageCard = panel(card)
        messageCard.addView(label("WhatsApp mesajı", 18f, Color.WHITE, true))
        messageCard.addView(label("Kontaktı seç, mətni yaz və WhatsApp-da yoxlayıb göndər.", 13f, Color.rgb(169, 181, 205), false).also {
            it.setPadding(0, dp(5), 0, dp(12))
        })
        contactInput = input("Kontaktın adı və ya nömrəsi")
        messageCard.addView(contactInput, matchWrap(dp(8)))
        val pickButton = actionButton("Kontakt seç", Color.rgb(49, 67, 98), Color.WHITE)
        pickButton.setOnClickListener { pickContact() }
        messageCard.addView(pickButton, matchWrap(dp(8)))
        messageInput = input("Mesajın mətni").apply {
            minLines = 3
            gravity = Gravity.TOP or Gravity.START
        }
        messageCard.addView(messageInput, matchWrap(dp(8)))
        val whatsappButton = actionButton("WhatsApp-da hazırla", Color.rgb(37, 211, 102), background)
        whatsappButton.setOnClickListener { composeWhatsApp() }
        messageCard.addView(whatsappButton, matchWrap(dp(8)))
        content.addView(messageCard, matchWrap(dp(14)))

        val torchCard = panel(card)
        torchCard.addView(label("Telefonun fənəri", 18f, Color.WHITE, true))
        torchCard.addView(label("Səsli əmr: “Fənəri yandır” və ya “Fənəri söndür”.", 13f, Color.rgb(169, 181, 205), false).also {
            it.setPadding(0, dp(5), 0, dp(12))
        })
        val torchButton = actionButton("Fənəri dəyiş", Color.rgb(255, 196, 87), background)
        torchButton.setOnClickListener { requestTorch(!torchOn) }
        torchCard.addView(torchButton, matchWrap())
        content.addView(torchCard, matchWrap(dp(14)))

        statusView = label("Hazırdır", 13f, Color.rgb(169, 181, 205), false).also {
            it.gravity = Gravity.CENTER
            it.setPadding(0, dp(4), 0, 0)
        }
        content.addView(statusView, matchWrap())
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(root)
    }

    private fun panel(color: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(16), dp(16), dp(16))
        background = rounded(color, dp(18).toFloat())
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun input(hintText: String) = EditText(this).apply {
        hint = hintText
        textSize = 15f
        setTextColor(Color.WHITE)
        setHintTextColor(Color.rgb(140, 153, 178))
        setPadding(dp(13), dp(12), dp(13), dp(12))
        background = rounded(Color.rgb(13, 20, 38), dp(12).toFloat())
        setSingleLine(false)
    }

    private fun actionButton(text: String, color: Int, textColor: Int) = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 15f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(textColor)
        background = rounded(color, dp(13).toFloat())
        minHeight = dp(48)
        stateListAnimator = null
    }

    private fun rounded(color: Int, radius: Float) = android.graphics.drawable.GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun matchWrap(bottom: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = dp(bottom) }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toggleVoiceListening() {
        if (VoiceCommandService.isRunning) {
            stopService(Intent(this, VoiceCommandService::class.java).setAction(VoiceCommandService.ACTION_STOP))
            voiceButton.text = "🎙  Daimi dinləməni başlat"
            sayStatus("Səsli dinləmə dayandırıldı.")
            return
        }
        val needed = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= 33) needed.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = needed.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) startVoiceService()
        else requestPermissions(missing.toTypedArray(), MIC_PERMISSION)
    }

    private fun startVoiceService() {
        try {
            val intent = Intent(this, VoiceCommandService::class.java).setAction(VoiceCommandService.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(intent) else startService(intent)
            voiceButton.text = "■  Daimi dinləməni dayandır"
            sayStatus("EVO-2 dinləyir. YouTube açılandan sonra da əmr verə bilərsən.")
        } catch (_: Exception) {
            sayStatus("Daimi dinləmə başlamadı. Mikrofon və bildiriş icazələrini yoxla.")
        }
    }

    private fun openYouTube(query: String) {
        val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            sayStatus("YouTube axtarışı açıldı: $query")
        } catch (_: Exception) {
            sayStatus("YouTube və brauzer açıla bilmədi.")
        }
    }

    private fun pickContact() {
        val intent = Intent(Intent.ACTION_PICK, Uri.parse("content://com.android.contacts/data/phones"))
        try {
            startActivityForResult(intent, CONTACT_PICKER)
        } catch (_: Exception) {
            sayStatus("Kontakt seçimi açıla bilmədi. Nömrəni əl ilə daxil et.")
        }
    }

    @Deprecated("Activity result callback for the system contact picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CONTACT_PICKER || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        try {
            contentResolver.query(uri, arrayOf("display_name", "data1"), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0).orEmpty()
                    val phone = cursor.getString(1).orEmpty()
                    selectedPhoneNumber = phone
                    contactInput.setText(if (name.isBlank()) phone else name)
                    sayStatus("Kontakt seçildi: ${if (name.isBlank()) phone else name}")
                }
            }
        } catch (_: Exception) {
            selectedPhoneNumber = null
            sayStatus("Kontakt nömrəsi oxunmadı. Nömrəni əl ilə daxil et.")
        }
    }

    private fun composeWhatsApp() {
        val entered = contactInput.text.toString().trim()
        val phone = entered.takeIf { it.any(Char::isDigit) } ?: selectedPhoneNumber
        val message = messageInput.text.toString().trim()
        if (phone.isNullOrBlank()) {
            sayStatus("Kontaktı seç və ya nömrəni ölkə kodu ilə daxil et.")
            return
        }
        if (message.isBlank()) {
            sayStatus("Mesaj mətnini daxil et və ya səsli əmr ver.")
            return
        }
        val internationalNumber = phone.filter(Char::isDigit)
        if (internationalNumber.isBlank()) {
            sayStatus("Telefon nömrəsini ölkə kodu ilə daxil et.")
            return
        }
        val deepLink = Uri.parse("https://wa.me/$internationalNumber?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, deepLink)
        try {
            intent.setPackage("com.whatsapp")
            startActivity(intent)
            sayStatus("Mesaj WhatsApp-da hazırlanıb. Göndərməzdən əvvəl yoxla.")
        } catch (_: Exception) {
            intent.setPackage(null)
            try {
                startActivity(Intent.createChooser(intent, "Mesajı hansı tətbiqdə açmaq istəyirsən?"))
                sayStatus("WhatsApp tapılmadı; uyğun mesajlaşma tətbiqi seç.")
            } catch (_: Exception) {
                sayStatus("Mesajlaşma tətbiqi açıla bilmədi. WhatsApp quraşdırılıb-quraşdırılmadığını yoxla.")
            }
        }
    }

    private fun requestTorch(enabled: Boolean) {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            pendingTorchState = enabled
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION)
            return
        }
        setTorch(enabled)
    }

    private fun setTorch(enabled: Boolean) {
        try {
            val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = torchCameraId?.takeIf { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: manager.cameraIdList.firstOrNull { cameraId ->
                manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (id == null) {
                sayStatus("Bu cihazda fənər mövcud deyil.")
                return
            }
            manager.setTorchMode(id, enabled)
            torchCameraId = id
            torchOn = enabled
            sayStatus(if (enabled) "Fənər yandırıldı." else "Fənər söndürüldü.")
        } catch (_: SecurityException) {
            sayStatus("Fənər üçün kamera icazəsi lazımdır.")
        } catch (_: Exception) {
            sayStatus("Fənər idarə edilə bilmədi. Kamera başqa tətbiqdə istifadə olunur? ")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MIC_PERMISSION -> {
                val micIndex = permissions.indexOf(Manifest.permission.RECORD_AUDIO)
                val micGranted = micIndex >= 0 && grantResults.getOrNull(micIndex) == PackageManager.PERMISSION_GRANTED
                val cameraIndex = permissions.indexOf(Manifest.permission.CAMERA)
                val cameraGranted = (cameraIndex >= 0 && grantResults.getOrNull(cameraIndex) == PackageManager.PERMISSION_GRANTED) ||
                    checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (micGranted) {
                    startVoiceService()
                    if (!cameraGranted) sayStatus("Dinləmə başladı. Fənər əmri üçün EVO-2-yə kamera icazəsi ver.")
                } else sayStatus("Daimi dinləmə üçün mikrofon icazəsi lazımdır.")
            }
            CAMERA_PERMISSION -> {
                val desired = pendingTorchState
                pendingTorchState = null
                if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED && desired != null) setTorch(desired)
                else sayStatus("Fənər üçün kamera icazəsi verilmədi.")
            }
        }
    }

    private fun sayStatus(text: String) {
        if (::statusView.isInitialized) statusView.text = text
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (::voiceButton.isInitialized) {
            voiceButton.text = if (VoiceCommandService.isRunning) "■  Daimi dinləməni dayandır" else "🎙  Daimi dinləməni başlat"
        }
    }
}
