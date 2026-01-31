import os
import json
import urllib.request
import shutil

# Target languages based on Braille tables
LANGUAGES = [
    'cs', 'da', 'de', 'el', 'en', 'es', 'fi', 'fr', 'hr', 'it', 
    'nl', 'pl', 'pt', 'ro', 'ru', 'sk', 'sv', 'tr', 'vi', 'zh', 'zh-Hant',
    'ar', 'hi'
]

BASE_URLS = [
    "https://raw.githubusercontent.com/unicode-org/cldr-json/main/cldr-json/cldr-annotations-modern",
    "https://unpkg.com/cldr-annotations-modern@44.0.0"
]
OUTPUT_DIR = os.path.join("assets", "emojis")

def fetch_json(url):
    print(f"    Trying {url}")
    try:
        with urllib.request.urlopen(url) as response:
            if response.getcode() == 200:
                return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"      Failed: {e}")
    return None

def process_language(lang):
    print(f"Processing {lang}...")
    
    data_annotations = None
    data_derived = None

    for base_url in BASE_URLS:
        if not data_annotations:
            # Fetch basic annotations
            url_annotations = f"{base_url}/annotations/{lang}/annotations.json"
            data_annotations = fetch_json(url_annotations)
        
        if not data_derived:
            # Fetch derived annotations (often contains important keywords)
            url_derived = f"{base_url}/annotationsDerived/{lang}/annotations.json"
            data_derived = fetch_json(url_derived)
        
        if data_annotations and data_derived:
            break
    
    if not data_annotations and not data_derived:
        print(f"  No data found for {lang}")
        return

    combined_data = []
    
    # Helper to process data
    def extract_data(source_data):
        if not source_data: return
        
        # The structure is usually annotations -> {lang} -> annotations -> {emoji} -> { default: [], tts: [] }
        # Or similar. Let's inspect the first key.
        
        try:
            # The keys might differ slightly, usually "annotations" top level
            top_level = source_data.get("annotations", {})
            identity = top_level.get("identity", {})
            # language = identity.get("language", lang)
            
            annotations = top_level.get("annotations", {})
            
            for emoji_char, info in annotations.items():
                keywords = info.get("default", [])
                tts_list = info.get("tts", [])
                tts = tts_list[0] if isinstance(tts_list, list) and tts_list else ""
                
                all_keywords = set(keywords)
                if tts:
                    all_keywords.add(tts)
                
                if all_keywords:
                    combined_data.append({
                        "emoji": emoji_char,
                        "keywords": list(all_keywords),
                        "name": tts
                    })
        except Exception as e:
            print(f"  Error parsing data for {lang}: {e}")

    extract_data(data_annotations)
    extract_data(data_derived)
    
    # Merge duplicates (same emoji, merge keywords and keep name)
    final_map = {}
    for item in combined_data:
        emoji = item['emoji']
        if emoji not in final_map:
            final_map[emoji] = {"k": set(), "n": item["name"]}
        final_map[emoji]["k"].update(item['keywords'])
        if not final_map[emoji]["n"] and item["name"]:
            final_map[emoji]["n"] = item["name"]
    
    # Convert to final list
    final_list = []
    for emoji, data in final_map.items():
        final_list.append({
            "e": emoji,
            "k": list(data["k"]),
            "n": data["n"]
        })
    
    # Save to file
    outfile = os.path.join(OUTPUT_DIR, f"{lang}.json")
    with open(outfile, 'w', encoding='utf-8') as f:
        json.dump(final_list, f, ensure_ascii=False, separators=(',', ':'))
    print(f"  Saved {len(final_list)} emojis to {outfile}")

def main():
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
        
    for lang in LANGUAGES:
        process_language(lang)

if __name__ == "__main__":
    main()
