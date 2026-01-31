/*
 * Copyright (C) 2016 The Soft Braille Keyboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dalton.braillekeyboard;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

/**
 * Allows the Braille IME to interface with the Android TTS service. This class
 * provides a lot of helper methods that help speak messages in text in the
 * appropriate format at the right times according to local app settings. It
 * ultimately saves a lot of code duplication.
 * 
 * You should always call shutdown() when you are finished with the service.
 */
public class Speech {

    /**
     * Implement this to receive a callback when the Android speech service is
     * ready.
     */
    public interface OnReadyListener {

        /**
         * Called when the tts is ready for use.
         */
        void ttsReady();
    }

    // Flush speech utterances
    public static final int QUEUE_FLUSH = TextToSpeech.QUEUE_FLUSH;
    // Queue speech utterances
    public static final int QUEUE_ADD = TextToSpeech.QUEUE_ADD;

    private static final int MAX_SPEECH_LENGTH = 3900;
    private static final String SHUTDOWN_ID = "SHUTDOWN";
    private static TextToSpeech tts;

    private SoundPool soundPool;
    private final Map<String, Integer> soundIdMap = new HashMap<String, Integer>();
    private final AudioManager audioManager;
    private final Map<String, String> speechMap = new HashMap<String, String>();
    private final boolean useAccessibilityVolume;
    private final float speechRate;
    @SuppressLint("NewApi")
    private final UtteranceProgressListener progressListener = new UtteranceProgressListener() {

        @Override
        public void onStart(String utteranceId) {
            audioManager.requestAudioFocus(audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }

        @Override
        @Deprecated
        public void onError(String utteranceId) {
        }

        @Override
        public void onDone(String utteranceId) {
            audioManager.abandonAudioFocus(audioFocusChangeListener);
            if (SHUTDOWN_ID.equals(utteranceId)) {
                doShutdown();
            }
        }
    };

    private final OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
        }
    };

    private boolean canSpeak;

    /**
     * Construct a Speech instance for integration with the Android tts service
     * and various helper methods specific for SBK.
     * 
     * @param context
     *            The application context.
     * @param listener
     *            The callback that will be invoked when the tts service is
     *            ready for use.
     */
    public Speech(final Context context, final OnReadyListener listener) {
        audioManager = (AudioManager) context
                .getSystemService(Context.AUDIO_SERVICE);

        useAccessibilityVolume = Options.getBooleanPreference(context,
                R.string.pref_use_accessibility_volume_key, false);

        String rateString = Options.getStringPreference(context,
                R.string.pref_speech_rate_key, context.getString(R.string.pref_speech_rate_default));
        float rate = 1.0f;
        try {
            int rateValue = Integer.parseInt(rateString);
            if (rateValue <= 50) {
                // Map 0-50 to 0.1-1.0
                rate = 0.1f + (rateValue / 50.0f) * 0.9f;
            } else {
                // Map 50-100 to 1.0-8.0
                rate = 1.0f + ((rateValue - 50) / 50.0f) * 7.0f;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        speechRate = rate;

        // Init SoundPool
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int usage = useAccessibilityVolume ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                    : AudioAttributes.USAGE_MEDIA;
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
        }
        loadSounds(context);

        // Some symbols are not spoken natively by TTS engines, so add them into
        // the map from strings.xml
        setSpeechMap(context, speechMap);

        String engine = Options.getStringPreference(context,
                R.string.pref_text_to_speech_engine_key, null);

        if (canSpeak || tts != null) {
            doShutdown();
        }

        tts = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setSpeechRate(speechRate);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        int usage = useAccessibilityVolume ? AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                                : AudioAttributes.USAGE_MEDIA;
                        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                                .setUsage(usage)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build();
                        tts.setAudioAttributes(audioAttributes);
                    }
                    canSpeak = true;
                    setProgressListener();
                    listener.ttsReady();
                }
            }
        }, engine);
    }

    @SuppressLint("NewApi")
    private void setProgressListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (tts != null && canSpeak) {
                tts.setOnUtteranceProgressListener(progressListener);
            }
        }
    }

    /**
     * Releases the android tts resources. You should always call this method
     * when you are finished with the service.
     * 
     * @param message
     *            The message to be spoken on shutdown if any.
     */
    public void shutdown(String message) {
        if (tts != null) {
            if (message != null
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                ttsSpeak(message, QUEUE_FLUSH, null, SHUTDOWN_ID);
            } else {
                doShutdown();
            }
        }
    }

    private void doShutdown() {
        if (tts != null && canSpeak) {
            tts.stop();
            setLocale(Locale.getDefault());
            tts.shutdown();
            canSpeak = false;
        }
        tts = null;
    }

    /**
     * Speak a string of text using the specific queuing mode.
     * 
     * @param context
     *            The application context.
     * @param text
     *            The text to be spoken.
     * @param mode
     *            The queuing mode to be used see QUEUE_ADD and QUEUE_FLUSH.
     */
    public void speak(Context context, CharSequence text, int mode) {
        speak(context, "%s", text, mode);
    }

    /**
     * Speak a string of text using a format string and the specific queuing
     * mode.
     * 
     * @param context
     *            The application context.
     * @param format
     *            The format string to be used to speak text.
     * @param text
     *            The text to be spoken.
     * @param mode
     *            The queuing mode to be used see QUEUE_ADD and QUEUE_FLUSH.
     */
    public void speak(Context context, String format, CharSequence text,
            int mode) {
        if (text != null) {
            if (Options.getBooleanPreference(context, R.string.pref_use_wav_key, false)) {
                String filename = getSoundFilename(text.toString());
                if (filename != null && soundIdMap.containsKey(filename)) {
                    soundPool.play(soundIdMap.get(filename), 1, 1, 0, 0, 1.0f);
                    return;
                }
            }
            if (text.equals(" ")) {
                // say "space
                text = context.getString(R.string.space);
            } else if (text.length() < 2 && text.length() > 0
                    && Character.isUpperCase(text.charAt(0))) {
                // announce capitalisation
                text = String.format(context.getString(R.string.capital), text);
            } else if (text.equals("\n")) {
                // newline
                text = context.getString(R.string.newline);
            } else if (text.toString().trim().equals("")) {
                // say "blank"
                text = context.getString(R.string.blank);
            }

            String textToSpeak = String.format(format,
                    extractPunctuation(text.toString()));
            // Speak the text ensuring that we don't overflow the buffer.
            divideAndSpeak(textToSpeak, mode, null);
        }
    }

    /**
     * Speak a password as a series of asterisks.
     * 
     * @param context
     *            The application context.
     * @param text
     *            The password to be spoken as asterisks.
     */
    public void speakPassword(Context context, String text) {
        speakPassword(context, "%s", text, QUEUE_FLUSH);
    }

    /**
     * Speak a password as asterisks with the given queuing strategy.
     * 
     * @param context
     *            The application context.
     * @param formatter
     *            Formatter string to speak the text of the password.
     * @param text
     *            The text of the password to be spoken as asterisks.
     * @param mode
     *            The queuing mode see QUEUE_FLUSH and QUEUE_ADD.
     */
    public void speakPassword(Context context, String formatter, String text,
            int mode) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append('*');
        }
        speak(context, String.format(formatter, sb.toString()), mode);
    }

    /**
     * Given the current state of application preferences decide how a password
     * should be spoken either read normally or said as asterisks.
     * 
     * @param context
     *            The application context.
     * @param format
     *            The formatter string to speak the text.
     * @param text
     *            The text to be spoken normally or as a password.
     * @param isPasswordField
     *            true if this text field is of type password.
     * @param mode
     *            The queuing mode to use for this utterance see QUEUE_ADD and
     *            QUEUE_FLUSH.
     */
    public void readConsiderPassword(Context context, String format,
            String text, boolean isPasswordField, int mode) {
        if (!isPasswordField
                || Options.getBooleanPreference(context,
                        R.string.pref_echo_passwords_key, false)) {
            speak(context, format, text, mode);
        } else {
            speakPassword(context, format, text, mode);
        }
    }

    /**
     * Set the locale of the tts engine.
     * 
     * @param locale
     *            The locale to set the engine to.
     * @return true if the engine could be set to this locale false if the
     *         engine does not support the locale.
     */
    public boolean setLocale(Locale locale) {
        // Somehow tts can be null while canSpeak is true.
        // TODO This is really a work around, but the state that causes this
        // should be fully understood and canSpeak's state should be updated
        // accordingly.
        if (tts != null
                && canSpeak
                && tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE) {
            tts.setLanguage(locale);
            return true;
        }
        return false;
    }

    public void stop() {
        if (tts != null) {
            tts.stop();
        }
    }

    // Divide a long utterance into segments before passing it to the Android
    // tts service for speaking.
    private void divideAndSpeak(final String text, final int queueMode,
            final HashMap<String, String> params) {
        int end = MAX_SPEECH_LENGTH < text.length() ? MAX_SPEECH_LENGTH : text
                .length();
        end = getBestEnd(text, end);

        if (canSpeak) {
            ttsSpeak(text.substring(0, end), queueMode, params, null);
        }

        for (int i = end; i < text.length(); i += MAX_SPEECH_LENGTH) {
            end = (i + MAX_SPEECH_LENGTH) < text.length() ? (i + MAX_SPEECH_LENGTH)
                    : text.length();
            end = getBestEnd(text, end);

            if (canSpeak) {
                ttsSpeak(text.substring(i, end), QUEUE_ADD, null, null);
            }
        }
    }

    @SuppressLint("NewApi")
    private void ttsSpeak(String text, int queueMode,
            HashMap<String, String> params, String id) {
        if (!canSpeak || tts == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (id == null) {
                id = String.valueOf(System.currentTimeMillis());
            }

            Bundle bundle = new Bundle();
            if (params != null) {
                for (String key : params.keySet()) {
                    bundle.putString(key, params.get(key));
                }
            }

            tts.speak(text, queueMode, bundle, id);
        } else {
            tts.speak(text, queueMode, params);
        }
    }

    // Find the best endpoint to speak until.
    // This is either the current endpoint if it is the actual end of the text.
    // Otherwise we back track until a white space separator so that the
    // segments of speech sound clean.
    private static int getBestEnd(String text, int end) {
        // separators to divide segments at eg. whitespace so it sounds clean.
        String[] items = { " ", "\n" };
        int bestEnd = end;
        if (text.length() != end) {
            bestEnd = -1;

            // the endpoint isn't actually the end of the text String.
            // back track and pick the closest separator index.
            for (int i = 0; i < items.length; i++) {
                // store the temporary segment separator index.
                int temp = text.substring(0, end).lastIndexOf(items[i]);
                // pick the longest segment.
                if (temp > bestEnd) {
                    bestEnd = temp;
                }
            }
        }
        return bestEnd < end && bestEnd > 0 ? bestEnd : end;
    }

    // If the string is just one character make sure we speak the actual
    // punctuation symbol for it. Otherwise it can be spoken natively.
    private String extractPunctuation(String text) {
        String symbol = null;
        if (text.length() == 1) {
            symbol = speechMap.get(text.substring(0, 1));
        }
        return symbol == null ? text : symbol;
    }

    // Many symbols are not spoken by tts properly.
    // Load the translations into a map to be used at speaking time from
    // strings.xml.
    private static void setSpeechMap(Context context,
            Map<String, String> punctuationSpokenEquivalentsMap) {
        // Symbols that most TTS engines can't speak
        punctuationSpokenEquivalentsMap.put("?",
                context.getString(R.string.punctuation_questionmark));
        punctuationSpokenEquivalentsMap.put(" ",
                context.getString(R.string.punctuation_space));
        punctuationSpokenEquivalentsMap.put("\n",
                context.getString(R.string.newline));
        punctuationSpokenEquivalentsMap.put(",",
                context.getString(R.string.punctuation_comma));
        punctuationSpokenEquivalentsMap.put(".",
                context.getString(R.string.punctuation_dot));
        punctuationSpokenEquivalentsMap.put("!",
                context.getString(R.string.punctuation_exclamation));
        punctuationSpokenEquivalentsMap.put("(",
                context.getString(R.string.punctuation_open_paren));
        punctuationSpokenEquivalentsMap.put(")",
                context.getString(R.string.punctuation_close_paren));
        punctuationSpokenEquivalentsMap.put("\"",
                context.getString(R.string.punctuation_double_quote));
        punctuationSpokenEquivalentsMap.put("\'",
                context.getString(R.string.punctuation_single_quote));
        punctuationSpokenEquivalentsMap.put("/",
                context.getString(R.string.punctuation_slash));
        punctuationSpokenEquivalentsMap.put("\\",
                context.getString(R.string.punctuation_backslash));
        punctuationSpokenEquivalentsMap.put(";",
                context.getString(R.string.punctuation_semicolon));
        punctuationSpokenEquivalentsMap.put(":",
                context.getString(R.string.punctuation_colon));
        punctuationSpokenEquivalentsMap.put("{",
                context.getString(R.string.punctuation_left_brace));
        punctuationSpokenEquivalentsMap.put("}",
                context.getString(R.string.punctuation_right_brace));
    }

    private void loadSounds(Context context) {
        try {
            String[] files = context.getAssets().list("sounds");
            if (files != null) {
                for (String file : files) {
                    try {
                        AssetFileDescriptor afd = context.getAssets().openFd("sounds/" + file);
                        int soundId = soundPool.load(afd, 1);
                        String key = file.replace(".wav", "");
                        soundIdMap.put(key, soundId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getSoundFilename(String text) {
        if (text == null) return null;
        if (text.length() == 1) {
            char c = text.charAt(0);
            if (Character.isLetter(c)) {
                 if (Character.isUpperCase(c)) {
                     return Character.toLowerCase(c) + "_cap";
                 }
                 return String.valueOf(c);
            }
            if (Character.isDigit(c)) {
                return String.valueOf(c);
            }
            switch (c) {
                case ' ': return "space";
                case '\n': return "newline";
                case '.': return "dot";
                case ',': return "comma";
                case '?': return "question";
                case '!': return "exclamation";
                case '(': return "open_paren";
                case ')': return "close_paren";
                case '"': return "double_quote";
                case '\'': return "single_quote";
                case '/': return "slash";
                case '\\': return "backslash";
                case ';': return "semicolon";
                case ':': return "colon";
                case '{': return "left_brace";
                case '}': return "right_brace";
                case '@': return "at";
                case '#': return "hash";
                case '$': return "dollar";
                case '%': return "percent";
                case '^': return "caret";
                case '&': return "ampersand";
                case '*': return "asterisk";
                case '_': return "underscore";
                case '+': return "plus";
                case '-': return "minus";
                case '=': return "equals";
                case '<': return "less_than";
                case '>': return "greater_than";
                case '[': return "left_bracket";
                case ']': return "right_bracket";
                case '|': return "pipe";
                case '~': return "tilde";
                case '`': return "grave";
            }
        }
        return null;
    }
}
