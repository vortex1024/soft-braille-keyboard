# Gemini CLI Session Summary - February 2, 2026

## Accomplishments

### 1. Bug Fixes
- **Character Duplication in Star Taxi:** Fixed a persistent character duplication issue.
    - **Final Root Cause Identified:** The keyboard was using Android's "composing text" feature (`setComposingText`). Many apps (Star Taxi, Chrome address bar) do not handle composition spans correctly and append the entire composing buffer to the existing text on every update, instead of replacing it. This lead to the "a", "aac", "aacace" pattern.
    - **Final Solution:**
        1. **Disabled system-level prediction/composition** globally in `BrailleIME.java`.
        2. **Standardized on "Manual Differential Update"**: The keyboard now manually calculates the difference between what's in the editor and what it needs to write.
        3. **Append-Only Behavior**: For standard typing, the keyboard now only calls `commitText` for the single new character. It **never** uses delete or replace commands unless a contraction or backspace truly changes the previous text.
    - **Result:** The keyboard now behaves like a standard physical keyboard, adding letters one-by-one. This is the most compatible method for Android and completely eliminates duplication in problematic apps.

## Technical Notes
- **Robustness:** By avoiding `setComposingText` and `deleteSurroundingText` for standard appends, we maximize compatibility with non-standard Android UI frameworks.