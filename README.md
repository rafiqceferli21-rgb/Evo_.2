# EVO-2 Android

EVO-2 is a small Android voice assistant for Azerbaijani/Turkish-style commands. It can search for music in YouTube, prepare a WhatsApp message for a contact, and switch the phone flashlight on or off.

## Open and run in Android Studio

1. Install Android Studio (Hedgehog or newer) and its Android SDK Platform 35. Android Studio's bundled JDK 17 is sufficient.
2. Download or clone this repository, then choose **File → Open** in Android Studio and select the repository root (the folder containing `settings.gradle.kts`).
3. Allow Gradle sync to finish. If Android Studio asks to install Android SDK Platform 35 or Build Tools, accept the prompt.
4. Connect an Android phone with USB debugging enabled, or create an emulator with a camera/flashlight if available.
5. Select the **app** run configuration and press **Run**. You can also build an APK from **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

The project uses Gradle 8.9, Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Java 17, minSdk 26 and targetSdk 35.

## Voice commands

Tap **Dinlə** and say a command. Examples:

- “YouTube-da [mahnının adı] aç” or “YouTube-də [mahnının adı] oxut” — opens YouTube search results. Choose a result to start playback.
- “Fənəri yandır” / “Fənəri söndür” — toggles the torch.
- For WhatsApp, say the message or type it in the message field, choose a contact with **Kontakt seç**, then tap **WhatsApp-da hazırla**. Android opens a prefilled conversation for review; tap Send in WhatsApp yourself.

The speech engine depends on the speech recognition service installed on the device. EVO-2 requests Azerbaijani (`az-AZ`); device services may fall back to another supported language. If recognition is unavailable, enter the song/message in the fields and use the action buttons.

## Permissions and privacy

- **Microphone (`RECORD_AUDIO`)** is requested only when you tap the listening button. Speech is handled by Android's configured recognition service; its network/privacy behavior depends on that service and device settings.
- **Camera (`CAMERA`)** is requested only when you use the flashlight. Android requires this permission for torch access on supported devices. EVO-2 does not capture photos or video.
- EVO-2 does not request contacts permission. Contact selection uses Android's system contact picker and uses the selected phone number only to open WhatsApp.
- WhatsApp must be installed to open directly. If it is not available, Android offers compatible messaging apps.
- YouTube search requires an internet connection. Search opens results and does not force autoplay; playback selection stays with the user.

## Project structure

```text
app/src/main/java/com/evo2/assistant/MainActivity.kt  UI, voice flow, command routing
app/src/main/AndroidManifest.xml                     App declaration and permissions
```

The app uses Android platform APIs and does not need an API key or backend.
