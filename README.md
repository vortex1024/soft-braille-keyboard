# Soft Braille Keyboard

## Introduction

This is an Android input method which displays a virtual on-screen Braille keyboard for use by a blind person. This facilitates much faster and more comfortable input for the blind on Android with a variety of powerful editing commands and support for a number of different languages.

This project is a fork of the original work by **Daniel Dalton**. The original repository can be found here: [danieldalton10/Soft-Braille-Keyboard](https://github.com/danieldalton10/Soft-Braille-Keyboard).

**Download the latest version:** [Latest Release](https://github.com/vortex1024/soft-braille-keyboard/releases/latest)

For a detailed manual on how to use the keyboard, please see the [Keyboard Guide](https://goo.gl/lD1v49).

This application is licensed under the Apache License, Version 2.0.

---

## What's New

### What's New in Version 4.01
*   **New & Updated Braille Tables:**
    *   Added **Romanian Literary Braille** support (Grade 1).
    *   Improved **Italian Literary Braille** table (fixed Dot 1 mapping and encoding issues).
*   **Gesture Fixes & Updates:**
    *   **Restored Classic Toggle:** Re-enabled the **Dot 2 swipe down** gesture to toggle Keyboard Echo (Speech), fixing a regression from the previous version.
    *   **New "Close Keyboard" Gesture:** Mapped to **Dots 4 and 6 swiping Right** for a more reliable way to dismiss the IME.
*   **Automation & Accessibility for Developers:**
    *   **GitHub Actions Integration:** Added a cloud build system that triggers on every commit. This allows anyone to **fork the project and make changes directly on GitHub** (e.g., adding tables or editing code) without needing to install Android Studio or the SDK locally—the APK will be built automatically in the "Actions" tab.

### What's New in Version 4.0
#### Emoji Support
The keyboard now features a locale-aware emoji search mode.
- **Toggle Emoji Mode:** Hold **Dot 1** and perform a **Swipe Down with Dot 5**.
- **Search:** Type the name of the emoji using Braille. The search is incremental.
- **Navigate Results:**
    - **Next Emoji:** Swipe Right with **Dot 6**.
    - **Previous Emoji:** Swipe Left with **Dot 6**.
- **Insert Emoji:** Swipe Up with **Dot 6**.
- **Exit:** Toggling the mode off or inserting an emoji will return you to standard typing.

### Enhanced Audio & TTS
- **Accessibility Volume:** Support for the system accessibility volume stream has been added for both TTS and WAV feedback.
- **TTS Engine Selection:** Users can now select from all installed TTS engines on the device.
- **Speech Rate:** A new setting allows fine-tuning the speech rate of the keyboard's echo.
- **WAV Feedback:** High-performance audio feedback using pre-generated WAV files. By default, sounds are provided for **English**. Adding support for other languages requires development knowledge (see the WAV Feedback section below).

### Improved Typing Logic
- **Number Mode Exit:** Typing a **Capital Sign (Dot 6)** while in Number Mode will now automatically switch the keyboard to Capital Mode and commit the current number, streamlining transitions between numbers and text (currently optimized for English).

### Bug Fixes
- **Chrome Address Bar:** Fixed a critical bug where the first character typed in the Google Chrome address bar (and other fields) would be duplicated.
- **eBay Search & Compatibility:** Resolved issues where the keyboard would not appear or function correctly in certain search fields like eBay.

---

## Building the App

The project uses Gradle and supports modern Android development tools.

### Prerequisites
*   **JDK 21:** The build is configured to use Java 21.
*   **Android SDK & NDK:** 
    *   **Important:** If your Windows user path contains spaces (e.g., `C:\Users\AMAIS 10`), you **MUST** use the 8.3 short path format (e.g., `C:\Users\AMAIS1~1`) in your `local.properties` file for the `sdk.dir` property to avoid NDK build failures.

### Setup
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/vortex1024/soft-braille-keyboard.git
    cd Soft-Braille-Keyboard
    ```
2.  **Configure SDK Location:**
    Create a `local.properties` file in the root directory:
    ```properties
    sdk.dir=C:/Users/YOURUS~1/AppData/Local/Android/Sdk
    ```

### Build Commands
*   **Debug Build:**
    ```bash
    .\gradlew.bat assembleDebug
    ```
    APK location: `build/outputs/apk/debug/Soft-Braille-Keyboard-debug.apk`
*   **Release Build:**
    ```bash
    .\gradlew.bat assembleRelease
    ```
*   **Clean:**
    ```bash
    .\gradlew.bat clean
    ```

---

## WAV Feedback Feature

This app includes a high-performance audio feedback system that uses pre-generated WAV files instead of the system TTS engine for single character echoes, significantly reducing latency.

*   **Activation:** Go to Keyboard Settings -> Keyboard Feedback -> Enable "Use WAV feedback".
*   **Mechanism:** When enabled, the app checks `assets/sounds/` for a WAV file matching the character. If not found, it falls back to TTS.

### Adding Support for Other Languages
Adding sounds for a new language requires modifying the project's source code and scripts:
1.  **Generate WAVs:** Modify `scripts/generate_audio.py` to update `VOICE_ARGS` (e.g., `-vfr` for French) and the `CHARS` dictionary. Run `python scripts/generate_audio.py`.
2.  **Update Android Logic:** Update the `getSoundFilename` method in `src/com/dalton/braillekeyboard/Speech.java` to map special characters to their respective filenames.

---

## Project Structure
*   `:` (Root) - The main application module.
*   `:client` - BrailleBack client library.
*   `:service` - BrailleBack service library.

## Adding a Braille Table
1.  Add the table to `brailleback/braille/service/jni/liblouiswrapper/liblouis/tables/`.
2.  Update `brailleback/braille/service/res/xml/tables.xml`.
3.  Run `mktranslationtables` from the `brailleback/braille/service` directory.
4.  Update `res/values/arrays.xml` to include the new table ID.
