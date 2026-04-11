package com.example.al_qari;

import android.content.Intent;
import android.widget.FrameLayout;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AlLisaanActivity_lp extends AppCompatActivity {

    private ImageView overviewImage;
    private FrameLayout popupContainer;
    private ImageView popupImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.al_lisaan_lp);

        // 🧠 Get module info from Intent (Lessons or Practice)
        String module = getIntent().getStringExtra("module");
        if (module == null)
            module = "Lessons"; // fallback

        // ✅ Update global trackers for IntentRouter
        QuranLessonActivity.currentMode = module;
        QuranLessonActivity.currentSubModule = "Al-Lisaan";

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // === Top Bar ===
        TextView topBarText = findViewById(R.id.topBarText);
        topBarText.setText("اللِّسَان");
        topBarText.setVisibility(View.VISIBLE);
        topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);

        // === Overview Image ===
        overviewImage = findViewById(R.id.overviewImage);
        loadOverviewImage();

        // Popup elements
        popupContainer = findViewById(R.id.popupContainer);
        popupImage = findViewById(R.id.popupImage);

        // Click listener to show popup
        overviewImage.setOnClickListener(v -> showPopup(overviewImage));

        // Close popup when clicked
        if (popupContainer != null) {
            popupContainer.setOnClickListener(v -> hidePopup());
        }

        // === TextViews for each card ===
        TextView cardOverviewText = findViewById(R.id.cardOverviewText);
        TextView cardDeepestText = findViewById(R.id.cardDeepestText);
        TextView cardMiddleText = findViewById(R.id.cardMiddleText);
        TextView cardEdgesText = findViewById(R.id.cardEdgesText);
        TextView cardTipText = findViewById(R.id.cardTipText);

        // === Parse description file and assign sections ===
        String fullText = readAssetFile("Makharij/ALLisaan/Description.txt");
        assignSections(fullText, cardOverviewText, cardDeepestText, cardMiddleText, cardEdgesText, cardTipText);

        // === Buttons (18 total) ===
        int[] buttonIds = {
                R.id.btnTaa, R.id.btnThaa, R.id.btnJeem, R.id.btnDal, R.id.btnDhal, R.id.btnRa,
                R.id.btnZay, R.id.btnSeen, R.id.btnSheen, R.id.btnSaad, R.id.btnDhaud, R.id.btnTau,
                R.id.btnZua, R.id.btnQaaf, R.id.btnKaaf, R.id.btnLam, R.id.btnNoon, R.id.btnYaa
        };
        String[] letters = {
                "ت", "ث", "ج", "د", "ذ", "ر", "ز", "س", "ش", "ص", "ض", "ط", "ظ", "ق", "ك", "ل", "ن", "ي"
        };

        for (int i = 0; i < buttonIds.length; i++) {
            final String letter = letters[i];
            MaterialButton btn = findViewById(buttonIds[i]);

            if ("Lessons".equalsIgnoreCase(module)) {
                btn.setOnClickListener(v -> openQuranLesson(letter));
            } else if ("Practice".equalsIgnoreCase(module)) {
                btn.setOnClickListener(v -> openLetterPractice(letter));
            }
        }

        // === Bottom Navigation Bar ===
        View navInclude = findViewById(R.id.include_bottomBar);
        if (navInclude != null) {
            View btnQuranBasics = navInclude.findViewById(R.id.btnQuranBasics);
            View btnMemorize = navInclude.findViewById(R.id.btnMemorize);
            View btnSalah = navInclude.findViewById(R.id.btnSalah);

            if (btnQuranBasics != null)
                btnQuranBasics.setOnClickListener(v -> startActivity(new Intent(this, Quran_Basics.class)));
            if (btnMemorize != null)
                btnMemorize.setOnClickListener(v -> startActivity(new Intent(this, Memorization.class)));
            if (btnSalah != null)
                btnSalah.setOnClickListener(v -> startActivity(new Intent(this, Salah.class)));
        }
    }

    // === Helper: Load overview image ===
    private void loadOverviewImage() {
        try (InputStream is = getAssets().open("Makharij/ALLisaan/anatomy_overview.png")) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            overviewImage.setImageBitmap(bmp);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // === Helper: Read full text file ===
    private String readAssetFile(String path) {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open(path)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    // === Helper: Split text into sections and assign ===
    private void assignSections(String text, TextView overview, TextView deepest,
            TextView middle, TextView edges, TextView tip) {

        overview.setText(extractOverview(text));
        deepest.setText(extractSection(text, "The deepest part of the tongue:", "The middle of the tongue:"));
        middle.setText(extractSection(text, "The middle of the tongue:", "The edges of the tongue:"));
        // ✅ include “region of the tongue” as part of edges
        edges.setText(extractSection(text, "The edges of the tongue:", "The tip of the tongue:"));
        tip.setText(extractSection(text, "The tip of the tongue:", null));
    }

    // === Helper: extract section between two headers ===
    private String extractSection(String fullText, String startHeader, String nextHeader) {
        int start = fullText.indexOf(startHeader);
        if (start == -1)
            return "";
        int end = (nextHeader != null) ? fullText.indexOf(nextHeader, start + startHeader.length()) : -1;
        if (end == -1)
            end = fullText.length();
        return fullText.substring(start, end).trim();
    }

    // === Helper: extract introduction before first section ===
    private String extractOverview(String fullText) {
        int first = fullText.indexOf("The deepest part of the tongue:");
        if (first == -1)
            return fullText.trim();
        return fullText.substring(0, first).trim();
    }

    // === Open Letter Lesson ===
    private void openQuranLesson(String letter) {
        Intent intent = new Intent(this, QuranLessonActivity.class);
        intent.putExtra("letter", letter);

        try {
            InputStream descIs = getAssets().open("Makharij/ALLisaan/" + letter + "/Description.txt");
            byte[] descBuffer = new byte[descIs.available()];
            descIs.read(descBuffer);
            descIs.close();
            intent.putExtra("description", new String(descBuffer));

            intent.putExtra("audio1", "Makharij/ALLisaan/" + letter + "/" + getAudioFile1(letter));

            String audio2Path = getAudioFile2(letter);
            if (audio2Path != null)
                intent.putExtra("audio2", audio2Path);

            String imagePath = getImagePath(letter);
            if (imagePath != null)
                intent.putExtra("image", imagePath);

        } catch (IOException e) {
            e.printStackTrace();
        }

        startActivity(intent);
    }

    // === Audio & Image mapping helpers ===
    private String getAudioFile1(String letter) {
        switch (letter) {
            case "ت":
                return "03_taa.mp3";
            case "ث":
                return "04_thaa.mp3";
            case "ج":
                return "05_jeem.mp3";
            case "د":
                return "08_dal.mp3";
            case "ذ":
                return "09_dhal.mp3";
            case "ر":
                return "10_ra.mp3";
            case "ز":
                return "11_zay.mp3";
            case "س":
                return "12_seen.mp3";
            case "ش":
                return "13_sheen.mp3";
            case "ص":
                return "14_sad.mp3";
            case "ض":
                return "15_dhaud.mp3";
            case "ط":
                return "16_tau.mp3";
            case "ظ":
                return "17_zua.mp3";
            case "ق":
                return "21_qaf.mp3";
            case "ك":
                return "22_kaf.mp3";
            case "ل":
                return "23_lam.mp3";
            case "ن":
                return "25_noon.mp3";
            case "ي":
                return "28_ya.mp3";
            default:
                return "";
        }
    }

    private String getAudioFile2(String letter) {
        switch (letter) {
            case "ت":
                return "ta.mp3";
            case "ث":
                return "ths (tha).mp3";
            case "ج":
                return "jj.mp3";
            case "د":
                return "dd (daal).mp3";
            case "ذ":
                return "zaa (thaal).mp3";
            case "ر":
                return "rr (ra).mp3";
            case "ز":
                return "zaw (zay).mp3";
            case "س":
                return "ss (siin).mp3";
            case "ش":
                return "sh (shiin).mp3";
            case "ص":
                return "sss (saad).mp3";
            case "ض":
                return "dd (daud).mp3";
            case "ط":
                return "taa (tauin).mp3";
            case "ظ":
                return "thaa (thuain).mp3";
            case "ق":
                return "qq (qaf).mp3";
            case "ك":
                return "kk (kaf).mp3";
            case "ل":
                return "ll (lam).mp3";
            case "ن":
                return "nn (nuun).mp3";
            case "ي":
                return "yii (ya).mp3";
            default:
                return null;
        }
    }

    private String getImagePath(String letter) {
        switch (letter) {
            case "ت":
                return "Makharij/ALLisaan/ت/ta.png";
            case "ث":
                return "Makharij/ALLisaan/ث/tsa.png";
            case "ج":
                return "Makharij/ALLisaan/ج/jeem.png";
            case "د":
                return "Makharij/ALLisaan/د/da.png";
            case "ذ":
                return "Makharij/ALLisaan/ذ/zal.png";
            case "ر":
                return "Makharij/ALLisaan/ر/ra.png";
            case "ز":
                return "Makharij/ALLisaan/ز/zay.png";
            case "س":
                return "Makharij/ALLisaan/س/seen.png";
            case "ش":
                return "Makharij/ALLisaan/ش/sheen.png";
            case "ص":
                return "Makharij/ALLisaan/ص/suad.png";
            case "ض":
                return "Makharij/ALLisaan/ض/dhaw.png";
            case "ط":
                return "Makharij/ALLisaan/ط/taw.png";
            case "ظ":
                return "Makharij/ALLisaan/ظ/zua.png";
            case "ق":
                return "Makharij/ALLisaan/ق/qaf.png";
            case "ك":
                return "Makharij/ALLisaan/ك/kaf.png";
            case "ل":
                return "Makharij/ALLisaan/ل/laam.png";
            case "ن":
                return "Makharij/ALLisaan/ن/noon.png";
            case "ي":
                return "Makharij/ALLisaan/ي/ya.png";
            default:
                return null;
        }
    }

    private void openLetterPractice(String letter) {
        Intent intent = new Intent(this, QuranPracticeActivity.class);
        intent.putExtra("letter", letter);
        startActivity(intent);
    }

    private void showPopup(ImageView sourceImage) {
        // Copy bitmap from clicked ImageView into popupImage
        popupImage.setImageDrawable(sourceImage.getDrawable());

        popupContainer.setAlpha(0f);
        popupContainer.setVisibility(View.VISIBLE);
        popupContainer.animate().alpha(1f).setDuration(200).start();
    }

    private void hidePopup() {
        popupContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            popupContainer.setVisibility(View.GONE);
        }).start();
    }
}
