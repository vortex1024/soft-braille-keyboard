package com.dalton.braillekeyboard;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EmojiDictionary {

    public static class EmojiEntry implements Comparable<EmojiEntry> {
        public final String keyword;
        public final String emoji;
        public final String name;

        public EmojiEntry(String keyword, String emoji, String name) {
            this.keyword = keyword;
            this.emoji = emoji;
            this.name = name;
        }

        @Override
        public int compareTo(EmojiEntry other) {
            return this.keyword.compareTo(other.keyword);
        }
    }

    private static List<EmojiEntry> cachedDictionary = new ArrayList<EmojiEntry>();
    private static String cachedLang = "";

    private static void loadForLocale(Context context, Locale locale) {
        String lang = locale != null ? locale.getLanguage() : "en";
        // Handle Traditional Chinese for Taiwan
        if (lang.equals("zh") && locale != null && "TW".equals(locale.getCountry())) {
            lang = "zh-Hant";
        }
        
        if (lang.equals(cachedLang)) {
            return;
        }

        List<EmojiEntry> newDict = new ArrayList<EmojiEntry>();
        try {
            String fileName = "emojis/" + lang + ".json";
            InputStream is = context.getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, "UTF-8");
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String emoji = obj.getString("e");
                String name = obj.optString("n", "");
                JSONArray keywords = obj.getJSONArray("k");
                for (int j = 0; j < keywords.length(); j++) {
                    newDict.add(new EmojiEntry(keywords.getString(j).toLowerCase(), emoji, name));
                }
            }
            Collections.sort(newDict);
            cachedDictionary = newDict;
            cachedLang = lang;
        } catch (Exception e) {
            // Fallback to English if current locale fails
            if (!lang.equals("en")) {
                loadForLocale(context, Locale.ENGLISH);
            }
        }
    }

    public static List<EmojiEntry> search(Context context, String query, Locale locale) {
        loadForLocale(context, locale);
        List<EmojiEntry> results = new ArrayList<EmojiEntry>();
        if (query == null || query.isEmpty()) {
            return results;
        }
        String lowerQuery = query.toLowerCase();
        for (EmojiEntry entry : cachedDictionary) {
            if (entry.keyword.startsWith(lowerQuery)) {
                results.add(entry);
            }
        }
        return results;
    }
}
