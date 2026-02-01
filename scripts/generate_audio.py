import os
import subprocess
import sys

# Configuration
ESPEAK_PATH_NG = r"C:\Program Files\eSpeak NG\espeak-ng.exe"
ESPEAK_PATH_X86 = r"C:\Program Files (x86)\eSpeak\command_line\espeak.exe"
ESPEAK_PATH_64 = r"C:\Program Files\eSpeak\command_line\espeak.exe"

if os.path.exists(ESPEAK_PATH_NG):
    ESPEAK_BIN = ESPEAK_PATH_NG
elif os.path.exists(ESPEAK_PATH_X86):
    ESPEAK_BIN = ESPEAK_PATH_X86
elif os.path.exists(ESPEAK_PATH_64):
    ESPEAK_BIN = ESPEAK_PATH_64
else:
    # Try to find in PATH
    import shutil
    found = shutil.which("espeak")
    if found:
        ESPEAK_BIN = found
    else:
        print("Error: espeak.exe not found in standard Program Files locations or PATH.")
        sys.exit(1)

OUTPUT_DIR = os.path.join("assets", "sounds")
VOICE_ARGS = ["-ven", "-s", "450"]

# Character map: filename -> text to speak
CHARS = {
    # Letters
    "a": "a", "b": "b", "c": "c", "d": "d", "e": "e", "f": "f", "g": "g", 
    "h": "h", "i": "i", "j": "j", "k": "k", "l": "l", "m": "m", "n": "n", 
    "o": "o", "p": "p", "q": "q", "r": "r", "s": "s", "t": "t", "u": "u", 
    "v": "v", "w": "w", "x": "x", "y": "y", "z": "z",
    # Uppercase (using suffix _cap)
    "a_cap": "Capital a", "b_cap": "Capital b", "c_cap": "Capital c", "d_cap": "Capital d", 
    "e_cap": "Capital e", "f_cap": "Capital f", "g_cap": "Capital g", "h_cap": "Capital h", 
    "i_cap": "Capital i", "j_cap": "Capital j", "k_cap": "Capital k", "l_cap": "Capital l", 
    "m_cap": "Capital m", "n_cap": "Capital n", "o_cap": "Capital o", "p_cap": "Capital p", 
    "q_cap": "Capital q", "r_cap": "Capital r", "s_cap": "Capital s", "t_cap": "Capital t", 
    "u_cap": "Capital u", "v_cap": "Capital v", "w_cap": "Capital w", "x_cap": "Capital x", 
    "y_cap": "Capital y", "z_cap": "Capital z",
    # Numbers
    "0": "0", "1": "1", "2": "2", "3": "3", "4": "4", 
    "5": "5", "6": "6", "7": "7", "8": "8", "9": "9",
    # Punctuation & Special (Filenames must be valid)
    "space": "Space",
    "newline": "New line",
    "backspace": "Delete", # Assuming we might want this
    "dot": "Dot",
    "comma": "Comma",
    "question": "Question mark",
    "exclamation": "Exclamation",
    "open_paren": "Open paren",
    "close_paren": "Close paren",
    "double_quote": "Double quote",
    "single_quote": "Apostrophe",
    "slash": "Slash",
    "backslash": "Backslash",
    "semicolon": "Semicolon",
    "colon": "Colon",
    "left_brace": "Left brace",
    "right_brace": "Right brace",
    "at": "At",
    "hash": "Hash",
    "dollar": "Dollar",
    "percent": "Percent",
    "caret": "Caret",
    "ampersand": "Ampersand",
    "asterisk": "Asterisk",
    "underscore": "Underscore",
    "plus": "Plus",
    "minus": "Minus",
    "equals": "Equals",
    "less_than": "Less than",
    "greater_than": "Greater than",
    "left_bracket": "Left bracket",
    "right_bracket": "Right bracket",
    "pipe": "Pipe",
    "tilde": "Tilde",
    "grave": "Grave accent"
}

def ensure_dir(directory):
    if not os.path.exists(directory):
        os.makedirs(directory)

def generate_wav(filename, text):
    path = os.path.join(OUTPUT_DIR, f"{filename}.wav")
    cmd = [ESPEAK_BIN] + VOICE_ARGS + ["-w", path, text]
    try:
        subprocess.run(cmd, check=True)
        print(f"Generated: {path} -> '{text}'")
    except subprocess.CalledProcessError as e:
        print(f"Failed to generate {filename}: {e}")

def main():
    ensure_dir(OUTPUT_DIR)
    print(f"Using espeak at: {ESPEAK_BIN}")
    print(f"Outputting to: {os.path.abspath(OUTPUT_DIR)}")
    
    for filename, text in CHARS.items():
        generate_wav(filename, text)
        
    print("Done.")

if __name__ == "__main__":
    main()
