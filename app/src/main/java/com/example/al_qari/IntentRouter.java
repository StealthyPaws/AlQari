package com.example.al_qari;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.util.Locale;
import java.util.ArrayDeque;
import java.util.Deque;


/**
 * IntentRouter
 * -------------
 * Handles navigation between modules, lessons, and letters.
 * Supports "next_letter", "previous_letter" navigation, and repeat actions.
 */
public class IntentRouter {

    // =====================================================================
    // 🔹 Current Session Info
    // =====================================================================
    private static String currentModule = "";      // e.g., Lessons or Practice
    private static String currentSubModule = "";   // e.g., Al-Halq
    private static String currentLetter = "";      // e.g., 'ع'

    // Global MediaPlayer instance for cross-intent control
    private static MediaPlayer globalMediaPlayer = null;
    private static TextToSpeech ttsInstance = null;

    // =====================================================================
    // 🔹 Repeat Tracking Variables
    // =====================================================================
    private static String lastIntent = null;             // stores the last action
    private static String lastDescriptionText = null;    // stores last description text
    private static String lastAudioPath = null;          // stores last played audio path

    // =====================================================================
    private static Deque<String> intentQueue = new ArrayDeque<>();


    // =====================================================================
    // 🔹 Helper: Speak + Toast
    // =====================================================================
    // Speak feedback
    private static void speakFeedback(Context context, String message) {
        // ✅ Run Toast safely on the main (UI) thread
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });

        // ✅ Text-to-speech initialization
        new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                TextToSpeech tts = new TextToSpeech(context, null);
                tts.setLanguage(Locale.ENGLISH);
                tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "feedback");
            }
        });
    }


    // =====================================================================
    // 🔹 Helper: Play audio safely from assets
    // =====================================================================
    // Play audio from assets
    private static void playAudioFromAssets(Context context, String assetPath) {
        if (assetPath == null || assetPath.trim().isEmpty()) {
            speakFeedback(context, "Letter audio not available.");
            return;
        }
        try {
            if (globalMediaPlayer != null) {
                globalMediaPlayer.stop();
                globalMediaPlayer.release();
            }
            globalMediaPlayer = new MediaPlayer();
            android.content.res.AssetFileDescriptor afd = context.getAssets().openFd(assetPath);
            globalMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            globalMediaPlayer.prepare();
            globalMediaPlayer.start();
            lastAudioPath = assetPath;
            lastIntent = "play_sound_letter";
        } catch (IOException e) {
            speakFeedback(context, "Audio file not found: " + assetPath);
        }
    }

    // =====================================================================
    // 🔹 Route main intents
    // =====================================================================
    public static void route(Context context, String intent) {


        // 🧠 Skip irrelevant intents — don't change state or history
        if (intent == null || intent.trim().isEmpty() || intent.equals("irrelevant")) {
            speakFeedback(context, "No valid command detected.");
            return;
        }
        // 🌀 If intent = repeat → replay most recent valid intent
        if (intent.equals("repeat")) {
            if (!intentQueue.isEmpty()) {
                String last = intentQueue.pop(); // get most recent
                speakFeedback(context, "Repeating your last command: " + last);

                // 🔁 Push it back so it remains the most recent intent
                intentQueue.push(last);

                // Now actually repeat it
                route(context, last);
            } else {
                speakFeedback(context, "No recent command to repeat.");
            }
            return;
        }

        // ✅ Otherwise, store this intent in the queue
        intentQueue.push(intent);


        Intent next = null;

        switch (intent) {

            // ============================================================== //
            // 📘 Lesson Modules
            // ============================================================== //
            case "open_module_al_jauf":
                openModule(context, "Al-Jauf");
                return;
            case "open_module_al_halq":
                openModule(context, "Al-Halq");
                return;
            case "open_module_al_lisaan":
                openModule(context, "Al-Lisaan");
                return;
            case "open_module_ash_shafataan":
                openModule(context, "Ash-Shafataan");
                return;
            case "open_module_al_khaishoom":
                openModule(context, "Al-Khaishoom");
                return;


            // ============================================================== //
            // 🧠 Practice Modules
            // ============================================================== //
            case "practice_module_al_jauf":
                next = new Intent(context, AlJawfActivity_lp.class);
                next.putExtra("module", "Practice");
                currentModule = "Practice"; currentSubModule = "Al-Jauf";
                break;

            case "practice_module_al_halq":
                next = new Intent(context, AlHalqActivity_lp.class);
                next.putExtra("module", "Practice");
                currentModule = "Practice"; currentSubModule = "Al-Halq";
                break;

            case "practice_module_al_lisaan":
                next = new Intent(context, AlLisaanActivity_lp.class);
                next.putExtra("module", "Practice");
                currentModule = "Practice"; currentSubModule = "Al-Lisaan";
                break;

            case "practice_module_ash_shafataan":
                next = new Intent(context, AshShafatanActivity_lp.class);
                next.putExtra("module", "Practice");
                currentModule = "Practice"; currentSubModule = "Ash-Shafataan";
                break;

            case "practice_module_al_khaishoom":
                next = new Intent(context, AlKhayshumActivity_lp.class);
                next.putExtra("module", "Practice");
                currentModule = "Practice"; currentSubModule = "Al-Khaishoom";
                break;

            // ============================================================== //
            // 🎧 Audio Placeholder Intents
            // ============================================================== //
            case "play_audio_surah_ayah":
            case "play_audio_surah_ayah_verse_by_verse":
            case "play_audio_surah_ayah_word_by_word":
                speakFeedback(context, "Audio feature not implemented yet.");
                return;

//            // ============================================================== //
//            // 🔁 Repeat Features
//            // ============================================================== //
//            case "repeat":
//                if (lastIntent != null && !lastIntent.equals("repeat")) {
//                    speakFeedback(context, "Repeating previous action.");
//                    String temp = lastIntent; // prevent recursion on itself
//                    lastIntent = null;
//                    route(context, temp);
//                } else {
//                    speakFeedback(context, "No previous action to repeat.");
//                }
//                return;


            case "repeat_description":
                if (lastDescriptionText != null && !lastDescriptionText.trim().isEmpty()) {
                    if (ttsInstance == null) {
                        ttsInstance = new TextToSpeech(context.getApplicationContext(), status -> {
                            if (status == TextToSpeech.SUCCESS) {
                                ttsInstance.setLanguage(Locale.ENGLISH);
                                ttsInstance.speak(lastDescriptionText, TextToSpeech.QUEUE_FLUSH, null, "repeat_desc_tts");
                            }
                        });
                    } else {
                        if (ttsInstance.isSpeaking()) ttsInstance.stop();
                        ttsInstance.setLanguage(Locale.ENGLISH);
                        ttsInstance.speak(lastDescriptionText, TextToSpeech.QUEUE_FLUSH, null, "repeat_desc_tts");
                    }
                    Toast.makeText(context, "📖 Repeating description...", Toast.LENGTH_SHORT).show();
                } else {
                    speakFeedback(context, "No previous description to repeat.");
                }
                return;

            case "repeat_sound":
                if (lastAudioPath != null) {
                    playAudioFromAssets(context, lastAudioPath);
                } else {
                    speakFeedback(context, "No previous sound to repeat.");
                }
                return;

            // ============================================================== //
            // ⏩ Next / ⏪ Previous Letter
            // ============================================================== //
            case "next_letter":
                navigateLetter(context, true);
                return;
            case "previous_letter":
                navigateLetter(context, false);
                return;

            // 🔊 Play current letter audio
            case "play_sound_letter":
                playAudioFromAssets(context, QuranLessonActivity.currentAudioPath);
                return;

            // ==============================================================
            // 🔄 Take To Lesson / Practice / Test
            // ==============================================================
            case "take_to_lesson":
                handleModuleSwitch(context, "Lessons");
                return;

            case "take_to_practice":
                handleModuleSwitch(context, "Practice");
                return;

            case "take_to_test":
                speakFeedback(context, "Test mode not implemented yet.");
                return;

            // ============================================================== //
            // 📖 Read Description Features
            // ============================================================== //
            case "read_description_letter":
                if (QuranLessonActivity.currentSubModule != null &&
                        QuranLessonActivity.currentLetter != null)
                    readDescription(context, QuranLessonActivity.currentSubModule, QuranLessonActivity.currentLetter);
                else
                    speakFeedback(context, "No active letter to describe.");
                return;

            case "read_description_module_al_halq":
                readDescription(context, "ALHalaq", null);
                return;

            case "read_description_module_al_jauf":
                readDescription(context, "ALJauf", null);
                return;

            case "read_description_module_al_khaishoom":
                readDescription(context, "ALKhushyum", null);
                return;

            case "read_description_module_al_lisaan":
                readDescription(context, "ALLisaan", null);
                return;

            case "read_description_module_ash_shafataan":
                readDescription(context, "Ashufataan", null);
                return;


            // ============================================================== //
            // 🕮 Additional Reading / Not Yet Implemented
            // ============================================================== //
            case "read_description_surah":
            case "read_personalization":
            case "read_progress_milestones":
            case "read_updates":
                speakFeedback(context, "This feature is not implemented yet.");
                return;
            case "repeat_verse":
                speakFeedback(context, "This feature is not implemented yet.");
                return;

            // ============================================================== //
            // 🔹 Test Modules (Not implemented yet)
            // ============================================================== //
            case "test_module_al_halq":
            case "test_module_al_jauf":
            case "test_module_al_khaishoom":
            case "test_module_al_lisaan":
            case "test_module_ash_shafataan":
                speakFeedback(context, "Test module not implemented yet.");
                return;

            // ============================================================== //
            // ❌ Unknown Intent
            // ============================================================== //
            default:
                speakFeedback(context, "Unknown intent: " + intent);
                return;
        }

        // ✅ Start activity if 'next' exists
        if (next != null) {
            next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(next);
            speakFeedback(context, "Opening " + currentModule + " → " + currentSubModule);
            lastIntent = intent;
        }
    }


    // --------------------------------------------------
    // 🔹 Module opener (first letter)
    // --------------------------------------------------
//    private static void openModule(Context context, String subModule, String firstLetter) {
//        QuranLessonActivity.currentSubModule = subModule;
//        QuranLessonActivity.currentLetter = "";
//        QuranLessonActivity.currentMode = "Lessons";
//
//        Intent intent = makeLessonIntent(context, subModule, firstLetter);
//        context.startActivity(intent);
//        speakFeedback(context, "Opening " + subModule);
//    }
    private static void openModule(Context context, String subModule) {
        QuranLessonActivity.currentSubModule = subModule;
        QuranLessonActivity.currentLetter = "";  // 👈 no letter yet
        QuranLessonActivity.currentMode = "Lessons";

        Intent next = null;
        switch (subModule) {
            case "Al-Jauf": next = new Intent(context, AlJawfActivity_lp.class); break;
            case "Al-Halq": next = new Intent(context, AlHalqActivity_lp.class); break;
            case "Al-Lisaan": next = new Intent(context, AlLisaanActivity_lp.class); break;
            case "Ash-Shafataan": next = new Intent(context, AshShafatanActivity_lp.class); break;
            case "Al-Khaishoom": next = new Intent(context, AlKhayshumActivity_lp.class); break;
        }

        if (next != null) {
            next.putExtra("module", "Lessons");
            next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(next);
            speakFeedback(context, "Opening " + subModule);
        }
    }

    private static void handleModuleSwitch(Context context, String targetModule) {
        String subModule = QuranLessonActivity.currentSubModule;
        String currentMode = QuranLessonActivity.currentMode;
        String currentLetter = QuranLessonActivity.currentLetter;

        // 🟡 If we are inside QuranLessonActivity (a specific letter)
        if (currentLetter != null && !currentLetter.trim().isEmpty()) {
            if ("Practice".equalsIgnoreCase(targetModule)) {
                speakFeedback(context, "Practice for " + currentLetter + " coming soon.");
                return;
            }
            if ("Test".equalsIgnoreCase(targetModule)) {
                speakFeedback(context, "Test for " + currentLetter + " coming soon.");
                return;
            }
        }

        // ✅ Normal switching between modules at the overview level (no letter open)
        if ("Lessons".equals(currentMode) && "Practice".equals(targetModule)) {
            Intent intent = null;

            switch (subModule) {
                case "Al-Halq": intent = new Intent(context, AlHalqActivity_lp.class); break;
                case "Al-Jauf": intent = new Intent(context, AlJawfActivity_lp.class); break;
                case "Al-Lisaan": intent = new Intent(context, AlLisaanActivity_lp.class); break;
                case "Ash-Shafataan": intent = new Intent(context, AshShafatanActivity_lp.class); break;
                case "Al-Khaishoom": intent = new Intent(context, AlKhayshumActivity_lp.class); break;
            }

            if (intent != null) {
                intent.putExtra("module", "Practice");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                speakFeedback(context, "Opening Practice for " + subModule);
            } else {
                speakFeedback(context, "Cannot switch to Practice from this module.");
            }
            return;
        }

        if ("Practice".equals(currentMode) && "Lessons".equals(targetModule)) {
            Intent intent = null;

            switch (subModule) {
                case "Al-Halq": intent = new Intent(context, AlHalqActivity_lp.class); break;
                case "Al-Jauf": intent = new Intent(context, AlJawfActivity_lp.class); break;
                case "Al-Lisaan": intent = new Intent(context, AlLisaanActivity_lp.class); break;
                case "Ash-Shafataan": intent = new Intent(context, AshShafatanActivity_lp.class); break;
                case "Al-Khaishoom": intent = new Intent(context, AlKhayshumActivity_lp.class); break;
            }

            if (intent != null) {
                intent.putExtra("module", "Lessons");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                speakFeedback(context, "Switching back to Lessons for " + subModule);
            } else {
                speakFeedback(context, "Cannot switch to Lessons from this module.");
            }
            return;
        }

        // Fallback
        speakFeedback(context, "Unable to switch modules from this screen.");
    }


    // =====================================================================
    // 🔹 Next/Previous Letter Logic
    // =====================================================================
//    private static void handleLetterNavigation(Context context, boolean isNext) {
//        if (QuranLessonActivity.currentLetter == null || QuranLessonActivity.currentSubModule == null) {
//            speakFeedback(context, "You are not currently viewing a letter lesson.");
//            return;
//        }
//
//        String subModule = QuranLessonActivity.currentSubModule;
//        String current = QuranLessonActivity.currentLetter;
//
//        Map<String, String[]> lessons = new HashMap<>();
//        lessons.put("Al-Jauf", new String[]{"ا", "و", "ي"});
//        lessons.put("Al-Halq", new String[]{ "ه","ح", "خ","ع", "غ","ء"});
//        lessons.put("Al-Lisaan", new String[]{"ق","ك" ,"ج","ش","ي","ض","ن","ر","ل","ت","د","ط","س","ز","ص","ث","ذ","ظ"});
//        lessons.put("Ash-Shafataan", new String[]{"ب","م","و","ف"});
//        lessons.put("Al-Khaishoom", new String[]{"ن", "م"});
//
//        if (!lessons.containsKey(subModule)) {
//            speakFeedback(context, "Navigation not supported in this module.");
//            return;
//        }
//
//        String[] letters = lessons.get(subModule);
//        int index = -1;
//        for (int i = 0; i < letters.length; i++) {
//            if (letters[i].equals(current)) {
//                index = i;
//                break;
//            }
//        }
//
//        if (index == -1) {
//            speakFeedback(context, "Letter not found in this module.");
//            return;
//        }
//
//        int newIndex = isNext ? index + 1 : index - 1;
//        if (newIndex < 0 || newIndex >= letters.length) {
//            speakFeedback(context, isNext ? "This is the last letter." : "This is the first letter.");
//            return;
//        }
//
//        String newLetter = letters[newIndex];
//        speakFeedback(context, (isNext ? "Next letter: " : "Previous letter: ") + newLetter);
//
//        Intent intent = new Intent(context, QuranLessonActivity.class);
//        intent.putExtra("letter", newLetter);
//        intent.putExtra("module", QuranLessonActivity.currentMode);
//        intent.putExtra("subModule", subModule);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        context.startActivity(intent);
//    }

    // =====================================================================
    // 🔹 Read Description Helper
    private static void readDescription(Context context, String subModule, String letter) {
        String path;

        // 1️⃣ MODULE LEVEL: when no letter is passed
        if (letter == null || letter.trim().isEmpty()) {
            switch (subModule) {
                case "ALHalaq":
                    path = "Makharij/ALHalaq/Description.txt";
                    break;
                case "ALJauf":
                    path = "Makharij/ALJauf/Description.txt";
                    break;
                case "ALKhushyum":
                    path = "Makharij/ALKhushyum/Description.txt";
                    break;
                case "ALLisaan":
                    path = "Makharij/ALLisaan/Description.txt";
                    break;
                case "Ashufataan":
                    path = "Makharij/Ashufataan/Description.txt";
                    break;
                default:
                    speakFeedback(context, "Unknown module: " + subModule);
                    return;
            }
        }
        // 2️⃣ LETTER LEVEL: when a specific letter is given
        else {
            path = getDescriptionPath(subModule, letter);
        }

        // 3️⃣ Read the text and speak it
        try (InputStream is = context.getAssets().open(path)) {
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            String text = new String(buffer, "UTF-8").trim();
            lastDescriptionText = text;

            if (ttsInstance == null) {
                ttsInstance = new TextToSpeech(context.getApplicationContext(), status -> {
                    if (status == TextToSpeech.SUCCESS) {
                        ttsInstance.setLanguage(Locale.ENGLISH);
                        ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "desc_tts");
                    }
                });
            } else {
                if (ttsInstance.isSpeaking()) ttsInstance.stop();
                ttsInstance.setLanguage(Locale.ENGLISH);
                ttsInstance.speak(text, TextToSpeech.QUEUE_FLUSH, null, "desc_tts");
            }

            new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                Toast.makeText(context, "📖 Reading description for " + subModule, Toast.LENGTH_SHORT).show();
            });


        } catch (IOException e) {
            speakFeedback(context, "Description not found at: " + path);
        }
    }


    // --------------------------------------------------
    // 🔹 Next / Previous Letter Navigation
    // --------------------------------------------------
    // --------------------------------------------------
// 🔹 Next / Previous Letter Navigation (Enhanced)
// --------------------------------------------------
    private static void navigateLetter(Context context, boolean next) {
        String subModule = QuranLessonActivity.currentSubModule;
        String current = QuranLessonActivity.currentLetter;
        String mode = QuranLessonActivity.currentMode;

        // 1️⃣ No module at all
        if (subModule == null || subModule.trim().isEmpty()) {
            speakFeedback(context, "No active module. Please open a module first.");
            return;
        }


        Map<String, String[]> map = getLetterMap();
        String[] letters = map.get(subModule);
        if (letters == null || letters.length == 0) {
            speakFeedback(context, "No letters found in " + subModule);
            return;
        }

        // 2️⃣ No letter yet (user opened module but not inside any letter)
        if (current == null || current.trim().isEmpty()) {
            if (next) {
                String firstLetter = letters[0];
                QuranLessonActivity.currentLetter = firstLetter;
                Intent i = makeLessonIntent(context, subModule, firstLetter);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
                speakFeedback(context, "Opening the first letter: " + firstLetter);
            } else {
                speakFeedback(context, "No previous letter yet. You can say 'next letter' to start.");
            }
            return;
        }

        // 3️⃣ Find current letter index
        int index = -1;
        for (int i = 0; i < letters.length; i++) {
            if (letters[i].equals(current)) {
                index = i;
                break;
            }
        }

        // If for some reason not found
        if (index == -1) {
            speakFeedback(context, "Current letter not found in " + subModule);
            return;
        }

        // 4️⃣ Move next or previous
        int newIndex = next ? index + 1 : index - 1;

        if (newIndex < 0) {
            speakFeedback(context, "This is the first letter. You can go next.");
            return;
        }

        if (newIndex >= letters.length) {
            speakFeedback(context, "This is the last letter, but you can go to previous.");
            return;
        }

        // 5️⃣ Navigate to the new letter
        String newLetter = letters[newIndex];
        QuranLessonActivity.currentLetter = newLetter;

        Intent i = makeLessonIntent(context, subModule, newLetter);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        speakFeedback(context, (next ? "Next: " : "Previous: ") + newLetter);
    }


    // --------------------------------------------------
    // 🔹 Helper: Build Intent for QuranLessonActivity
    // --------------------------------------------------
    private static Intent makeLessonIntent(Context context, String subModule, String letter) {
        Intent intent = new Intent(context, QuranLessonActivity.class);
        intent.putExtra("letter", letter);
        intent.putExtra("subModule", subModule);
        intent.putExtra("module", "Lessons");
        intent.putExtra("description", getDescriptionPath(subModule, letter));
        intent.putExtra("audio1", getAudio1Path(subModule, letter));
        intent.putExtra("audio2", getAudio2Path(subModule, letter));
        intent.putExtra("image", getImagePath(subModule, letter));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        QuranLessonActivity.currentAudioPath = getAudio1Path(subModule, letter);
        return intent;
    }

    // --------------------------------------------------
    // 🔹 Asset path mappings (based on your real structure)
    // --------------------------------------------------
    private static String getDescriptionPath(String subModule, String letter) {
        switch (subModule) {
            case "Al-Jauf": return "Makharij/ALJauf/" + getSubFolder("Al-Jauf", letter) + "/Description.txt";
            case "Al-Halq": return "Makharij/ALHalaq/" + getSubFolder("Al-Halq", letter) + "/Description.txt";
            case "Al-Lisaan": return "Makharij/ALLisaan/" + letter + "/Description.txt";
            case "Ash-Shafataan": return "Makharij/Ashufataan/" + letter + "/Description.txt";
            case "Al-Khaishoom": return "Makharij/ALKhushyum/" + letter + "/Description.txt";
            default: return "";
        }
    }

    private static String getAudio1Path(String subModule, String letter) {
        if (subModule.equals("Al-Lisaan")) return "Makharij/ALLisaan/" + letter + "/" + getAudioFile1(letter);
        if (subModule.equals("Al-Halq")) {
            switch (letter) {
                case "ح": return "Makharij/ALHalaq/Ḥāʾح/06_ha.mp3";
                case "خ": return "Makharij/ALHalaq/Kha/07_kha.mp3";
                case "ع": return "Makharij/ALHalaq/Ayn/18_ain.mp3";
                case "غ": return "Makharij/ALHalaq/Ghayn/19_ghain.mp3";
                case "ه": return "Makharij/ALHalaq/Hāʾ/26_haa.mp3";
                case "ء": return null;
            }
        }
        if (subModule.equals("Al-Jauf")) {
            switch (letter) {
                case "ا": return "Makharij/ALJauf/Alif/01_alif.mp3";
                case "و": return "Makharij/ALJauf/Waw/27_waw.mp3";
                case "ي": return null;
            }
        }
        if (subModule.equals("Ash-Shafataan")) {
            switch (letter) {
                case "ب": return "Makharij/Ashufataan/ب/02_baa.mp3";
                case "ف": return "Makharij/Ashufataan/ف/20_fa.mp3";
                case "م": return "Makharij/Ashufataan/م/24_meem.mp3";
                case "و": return "Makharij/Ashufataan/و/27_waw.mp3";
            }
        }
        if (subModule.equals("Al-Khaishoom")) {
            switch (letter) {
                case "ن": return "Makharij/ALKhushyum/ن/25_noon.mp3";
                case "م": return "Makharij/ALKhushyum/م/24_meem.mp3";
            }
        }
        return null;
    }

    private static String getAudio2Path(String subModule, String letter) {
        if (subModule.equals("Al-Lisaan"))
            return "Makharij/ALLisaan/" + letter + "/" + AlLisaanActivity_lp_getAudioFile2(letter);
        if (subModule.equals("Al-Halq")) {
            switch (letter) {
                case "ح": return "Makharij/ALHalaq/Ḥāʾح/hha (hhha).mp3";
                case "خ": return "Makharij/ALHalaq/Kha/khh (kha).mp3";
                case "ع": return "Makharij/ALHalaq/Ayn/aaa (ayn).mp3";
                case "غ": return "Makharij/ALHalaq/Ghayn/ghh (ghayn).mp3";
                case "ه": return "Makharij/ALHalaq/Hāʾ/h (ha ه).mp3";
            }
        }
        return null;
    }

    private static String getImagePath(String subModule, String letter) {
        if (subModule.equals("Al-Lisaan"))
            return AlLisaanActivity_lp_getImagePath(letter);
        if (subModule.equals("Ash-Shafataan"))
            return "Makharij/Ashufataan/" + letter + "/" + letter + ".png";
        if (subModule.equals("Al-Jauf"))
            return "Makharij/ALJauf/" + getSubFolder("Al-Jauf", letter) + "/" + letter + ".png";
        return null;
    }

    private static String getSubFolder(String subModule, String letter) {
        switch (subModule) {
            case "Al-Jauf":
                if (letter.equals("ا")) return "Alif";
                if (letter.equals("و")) return "Waw";
                if (letter.equals("ي")) return "Ya";
                break;
            case "Al-Halq":
                if (letter.equals("ح")) return "Ḥāʾح";
                if (letter.equals("خ")) return "Kha";
                if (letter.equals("ع")) return "Ayn";
                if (letter.equals("غ")) return "Ghayn";
                if (letter.equals("ه")) return "Hāʾ";
                if (letter.equals("ء")) return "Hamzah";
                break;
        }
        return letter;
    }

    private static Map<String, String[]> getLetterMap() {
        Map<String, String[]> map = new HashMap<>();
        map.put("Al-Jauf", new String[]{"ا", "و", "ي"});
        map.put("Al-Halq", new String[]{"ء", "ه", "ع", "ح", "غ", "خ"});
        map.put("Al-Lisaan", new String[]{"ت","ث","ج","د","ذ","ر","ز","س","ش","ص","ض","ط","ظ","ق","ك","ل","ن","ي"});
        map.put("Ash-Shafataan", new String[]{"ب","ف","م","و"});
        map.put("Al-Khaishoom", new String[]{"ن","م"});
        return map;
    }

    // pull helper mappings from AlLisaanActivity
    private static String getAudioFile1(String letter) {
        switch (letter) {
            case "ت": return "03_taa.mp3";
            case "ث": return "04_thaa.mp3";
            case "ج": return "05_jeem.mp3";
            case "د": return "08_dal.mp3";
            case "ذ": return "09_dhal.mp3";
            case "ر": return "10_ra.mp3";
            case "ز": return "11_zay.mp3";
            case "س": return "12_seen.mp3";
            case "ش": return "13_sheen.mp3";
            case "ص": return "14_sad.mp3";
            case "ض": return "15_dhaud.mp3";
            case "ط": return "16_tau.mp3";
            case "ظ": return "17_zua.mp3";
            case "ق": return "21_qaf.mp3";
            case "ك": return "22_kaf.mp3";
            case "ل": return "23_lam.mp3";
            case "ن": return "25_noon.mp3";
            case "ي": return "28_ya.mp3";
        }
        return "";
    }

    private static String AlLisaanActivity_lp_getAudioFile2(String letter) {
        switch (letter) {
            case "ت": return "ta.mp3";
            case "ث": return "ths (tha).mp3";
            case "ج": return "jj.mp3";
            case "د": return "dd (daal).mp3";
            case "ذ": return "zaa (thaal).mp3";
            case "ر": return "rr (ra).mp3";
            case "ز": return "zaw (zay).mp3";
            case "س": return "ss (siin).mp3";
            case "ش": return "sh (shiin).mp3";
            case "ص": return "sss (saad).mp3";
            case "ض": return "dd (daud).mp3";
            case "ط": return "taa (tauin).mp3";
            case "ظ": return "thaa (thuain).mp3";
            case "ق": return "qq (qaf).mp3";
            case "ك": return "kk (kaf).mp3";
            case "ل": return "ll (lam).mp3";
            case "ن": return "nn (nuun).mp3";
            case "ي": return "yii (ya).mp3";
        }
        return null;
    }

    private static String AlLisaanActivity_lp_getImagePath(String letter) {
        switch (letter) {
            case "ت": return "Makharij/ALLisaan/ت/ta.png";
            case "ث": return "Makharij/ALLisaan/ث/tsa.png";
            case "ج": return "Makharij/ALLisaan/ج/jeem.png";
            case "د": return "Makharij/ALLisaan/د/da.png";
            case "ذ": return "Makharij/ALLisaan/ذ/zal.png";
            case "ر": return "Makharij/ALLisaan/ر/ra.png";
            case "ز": return "Makharij/ALLisaan/ز/zay.png";
            case "س": return "Makharij/ALLisaan/س/seen.png";
            case "ش": return "Makharij/ALLisaan/ش/sheen.png";
            case "ص": return "Makharij/ALLisaan/ص/suad.png";
            case "ض": return "Makharij/ALLisaan/ض/dhaw.png";
            case "ط": return "Makharij/ALLisaan/ط/taw.png";
            case "ظ": return "Makharij/ALLisaan/ظ/zua.png";
            case "ق": return "Makharij/ALLisaan/ق/qaf.png";
            case "ك": return "Makharij/ALLisaan/ك/kaf.png";
            case "ل": return "Makharij/ALLisaan/ل/laam.png";
            case "ن": return "Makharij/ALLisaan/ن/noon.png";
            case "ي": return "Makharij/ALLisaan/ي/ya.png";
        }
        return null;
    }
}

