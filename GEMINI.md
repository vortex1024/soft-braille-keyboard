# Gemini CLI Session Summary - January 29, 2026

## Accomplishments

### 1. Emoji Mode Improvements
- **Logic Fix:** Resolved a critical bug in `ActionHandler.java` where failed Braille translations during Emoji Mode would "fall through" and type raw Braille patterns into the editor. It now traps and consumes all input.
- **CLDR Data Integration:** Created `scripts/fetch_emojis.py` to fetch localized emoji annotations (keywords and short names) for 20+ languages from Unicode/CLDR.
- **Locale-Aware Search:** Rewrote `EmojiDictionary.java` to dynamically load JSON assets based on the active Braille table's locale.
- **Descriptive Navigation:** Updated the search UI to prioritize the emoji's "TTS short name," making it easier to distinguish between similar emojis (e.g., different types of "kiss" emojis).

### 2. Direct Boot Support
- **Manifest Updates:** Marked `BrailleIME` and `TranslatorService` as `android:directBootAware="true"`.
- **Storage Migration:** Refactored `Options.java` to use Device-Protected Storage (`createDeviceProtectedStorageContext()`) for all SharedPreferences, making the keyboard functional on the lock screen before device decryption.
- **Settings Migration Logic:** Implemented an automatic migration in `Options.java` (`moveSharedPreferencesFrom`) to transfer existing user settings from credential-protected storage to device-protected storage, preventing data loss.
- **Settings UI Alignment:** Updated `PreferenceIME.java` to ensure the settings activity correctly interacts with the protected storage area.

### 3. Build & Environment
- **SDK Path Fix:** Identified and fixed a build failure caused by spaces in the Android SDK path on Windows. Documented the requirement to use 8.3 short paths (e.g., `C:/Users/AMAIS1~1/...`) and forward slashes in `local.properties`.
- **Emoji Assets:** Populated `assets/emojis/` with localized JSON files for supported languages.

### 4. Bug Fixes
- **Chrome Address Bar Duplication:** Fixed an issue where the first character typed in the Google Chrome address bar (and potentially other fields) would be duplicated. This was caused by `deleteSurroundingText(0, 0)` being called unnecessarily when `composingText` was empty; added a check to skip deletion in this case.
- **Number Mode Exit:** Fixed an issue where typing Dot 6 (Capital Sign) while in Number Mode (after 3456) would fail to switch to Capital Mode and continue writing numbers. Added logic to detect Dot 6 input when the buffer contains a Number Sign, forcing a text commit to reset the state. **Update:** This fix is restricted to the **English locale** ("en") to avoid interfering with other languages where Dot 6 might have different meanings (e.g., as a punctuation mark in French or Spanish number modes).

## Technical Notes for Future Sessions
- **Emoji Data:** If new languages are added to `arrays.xml`, run `python scripts/fetch_emojis.py` after adding the lang code to the script's `LANGUAGES` list.
- **Direct Boot:** All new preference access must go through `Options.getSharedPreferences(context)` to maintain compatibility with the device-protected storage used on Android 7.0+.
- **Build Command:** Use `.\gradlew.bat assembleDebug` for testing. Ensure `local.properties` remains in the short-path format.

# Gemini CLI Session Summary - February 2, 2026

## Accomplishments

### 1. Bug Fixes
- **Character Duplication in Star Taxi:** Implemented a robust fix for character duplication in apps that handle input poorly (Star Taxi, Chrome address bar).
    - **Previous Reference Commit:** `6ffb3c1bcf064e2a3487032d1020b95f378b4ea9` (Last state with legacy composition logic).
    - **Root Cause:** The keyboard was using Android's "composing text" feature (`setComposingText`). Many apps do not handle composition spans correctly and append the entire composing buffer to the existing text on every update, instead of replacing it.
    - **Final Solution:**
        1. **Disabled system-level prediction/composition** globally in `BrailleIME.java`.
        2. **Manual Differential Update + Re-sync:** The keyboard now manually calculates the difference between what's in the editor (`getTextBeforeCursor`) and what it needs to write.
        3. **Append-Only Behavior:** For standard typing, the keyboard now only calls `commitText` for the single new character. It avoids delete/replace commands unless necessary.
    - **Result:** Completely eliminates duplication in problematic apps by mimicking standard physical keyboard behavior.

## Technical Notes
- **Input Compatibility:** Avoiding `setComposingText` and `deleteSurroundingText` for standard appends maximizes compatibility with non-standard Android UI frameworks.
