import json
with open('assets/emojis/en.json', encoding='utf-8') as f:
    d = json.load(f)
for i in d:
    if 'kiss' in i['k']:
        print(f"Emoji: {i['e']} | Keywords: {i['k']}")
