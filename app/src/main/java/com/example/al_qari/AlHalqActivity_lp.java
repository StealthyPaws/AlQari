package com.example.al_qari;

import android.content.Intent;
import android.content.res.AssetManager;
import java.nio.charset.StandardCharsets;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.InputStream;

public class AlHalqActivity_lp extends AppCompatActivity {

    private FrameLayout popupContainer;
    private ImageView popupImage;

    private ImageView overviewImage;
    private ImageView adnaHalaqImage, wastaHalaqImage, aqsaHalaqImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.al_halq_lp);

        // Get module info
        String module = getIntent().getStringExtra("module");
        if (module == null) module = "Lessons";

        QuranLessonActivity.currentMode = module;
        QuranLessonActivity.currentSubModule = "Al-Halq";

        // Stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // Top bar
        TextView topBarText = findViewById(R.id.topBarText);
        topBarText.setText("الحلق");
        topBarText.setVisibility(TextView.VISIBLE);
        topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);

        // Image views
        overviewImage = findViewById(R.id.overviewImage);
        adnaHalaqImage = findViewById(R.id.adnaHalaqImage);
        wastaHalaqImage = findViewById(R.id.wastaHalaqImage);
        aqsaHalaqImage = findViewById(R.id.aqsaHalaqImage);

        // Popup
        popupContainer = findViewById(R.id.popupContainer);
        popupImage = findViewById(R.id.popupImage);

        // Load images
        loadHalaqImages();

        // Load description content
        loadDescriptionText();

        overviewImage.setOnClickListener(v -> showPopup(overviewImage));
        adnaHalaqImage.setOnClickListener(v -> showPopup(adnaHalaqImage));
        wastaHalaqImage.setOnClickListener(v -> showPopup(wastaHalaqImage));
        aqsaHalaqImage.setOnClickListener(v -> showPopup(aqsaHalaqImage));

        if (popupContainer != null) {
            popupContainer.setOnClickListener(v -> hidePopup());
        }

        // Buttons
        MaterialButton btnH = findViewById(R.id.btnH);
        MaterialButton btnHaa = findViewById(R.id.btnHaa);
        MaterialButton btnKhaa = findViewById(R.id.btnKhaa);
        MaterialButton btnAin = findViewById(R.id.btnAin);
        MaterialButton btnGhayn = findViewById(R.id.btnGhayn);
        MaterialButton btnHamza = findViewById(R.id.btnHamza);

        if ("Lessons".equalsIgnoreCase(module)) {

            btnH.setOnClickListener(v -> openLetterLesson("ه",
                    "Makharij/ALHalaq/Hāʾ/Description.txt",
                    "Makharij/ALHalaq/Hāʾ/26_haa.mp3",
                    "Makharij/ALHalaq/Hāʾ/h (ha ه).mp3",
                    null));

            btnHaa.setOnClickListener(v -> openLetterLesson("ح",
                    "Makharij/ALHalaq/Ḥāʾح/Description.txt",
                    "Makharij/ALHalaq/Ḥāʾح/06_ha.mp3",
                    "Makharij/ALHalaq/Ḥāʾح/hha (hhha).mp3",
                    null));

            btnKhaa.setOnClickListener(v -> openLetterLesson("خ",
                    "Makharij/ALHalaq/Kha/Description.txt",
                    "Makharij/ALHalaq/Kha/07_kha.mp3",
                    "Makharij/ALHalaq/Kha/khh (kha).mp3",
                    null));

            btnAin.setOnClickListener(v -> openLetterLesson("ع",
                    "Makharij/ALHalaq/Ayn/Description.txt",
                    "Makharij/ALHalaq/Ayn/18_ain.mp3",
                    "Makharij/ALHalaq/Ayn/aaa (ayn).mp3",
                    null));

            btnGhayn.setOnClickListener(v -> openLetterLesson("غ",
                    "Makharij/ALHalaq/Ghayn/Description.txt",
                    "Makharij/ALHalaq/Ghayn/19_ghain.mp3",
                    "Makharij/ALHalaq/Ghayn/ghh (ghayn).mp3",
                    null));

            btnHamza.setOnClickListener(v -> openLetterLesson("ء",
                    "Makharij/ALHalaq/Hamzah/Description.txt",
                    null,
                    null,
                    null));

        } else if ("Practice".equalsIgnoreCase(module)) {

            btnH.setOnClickListener(v -> openLetterPractice("ه"));
            btnHaa.setOnClickListener(v -> openLetterPractice("ح"));
            btnKhaa.setOnClickListener(v -> openLetterPractice("خ"));
            btnAin.setOnClickListener(v -> openLetterPractice("ع"));
            btnGhayn.setOnClickListener(v -> openLetterPractice("غ"));
            btnHamza.setOnClickListener(v -> openLetterPractice("ء"));
        }

        // Bottom navigation
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

    private void loadDescriptionText() {
        TextView overview = findViewById(R.id.overview);
        TextView cardDeepestText = findViewById(R.id.cardDeepestText);
        TextView cardmiddleText = findViewById(R.id.cardmiddleText);
        TextView cardclosestText = findViewById(R.id.cardclosestText);

        String content = loadAssetText("Makharij/ALHalaq/Description.txt");
        if (content.isEmpty()) return;

        try {
            String generalOverview = content.split("The deepest part of the throat:")[0].trim();

            String deepestPart = content.substring(
                    content.indexOf("The deepest part of the throat:") + "The deepest part of the throat:".length(),
                    content.indexOf("The middle part of the throat:")).trim();

            String middlePart = content.substring(
                    content.indexOf("The middle part of the throat:") + "The middle part of the throat:".length(),
                    content.indexOf("The closest part of the throat:")).trim();

            String closestPart = content.substring(
                    content.indexOf("The closest part of the throat:") + "The closest part of the throat:".length()).trim();

            overview.setText(generalOverview);
            cardDeepestText.setText(deepestPart);
            cardmiddleText.setText(middlePart);
            cardclosestText.setText(closestPart);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String loadAssetText(String filePath) {
        try {
            InputStream is = getAssets().open(filePath);
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private void showPopup(ImageView sourceImage) {
        popupImage.setImageDrawable(sourceImage.getDrawable());
        popupContainer.setAlpha(0f);
        popupContainer.setVisibility(View.VISIBLE);
        popupContainer.animate().alpha(1f).setDuration(200).start();
    }

    private void hidePopup() {
        popupContainer.animate().alpha(0f).setDuration(150)
                .withEndAction(() -> popupContainer.setVisibility(View.GONE))
                .start();
    }

    private void loadHalaqImages() {
        AssetManager am = getAssets();
        try {
            InputStream isOverview = am.open("Makharij/ALHalaq/anatomy_overview.png");
            overviewImage.setImageBitmap(BitmapFactory.decodeStream(isOverview));

            InputStream isAdna = am.open("Makharij/ALHalaq/adna_halaq.png");
            adnaHalaqImage.setImageBitmap(BitmapFactory.decodeStream(isAdna));

            InputStream isWasta = am.open("Makharij/ALHalaq/wasta_halaq.png");
            wastaHalaqImage.setImageBitmap(BitmapFactory.decodeStream(isWasta));

            InputStream isAqsa = am.open("Makharij/ALHalaq/aqsa_halaq.png");
            aqsaHalaqImage.setImageBitmap(BitmapFactory.decodeStream(isAqsa));

            isOverview.close();
            isAdna.close();
            isWasta.close();
            isAqsa.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openLetterLesson(String letter, String description, String audio1, String audio2, String image) {
        Intent intent = new Intent(this, QuranLessonActivity.class);
        intent.putExtra("letter", letter);
        intent.putExtra("description", description);
        intent.putExtra("audio1", audio1);
        intent.putExtra("audio2", audio2);
        intent.putExtra("image", image);
        startActivity(intent);
    }

    private void openLetterPractice(String letter) {
        Intent intent = new Intent(this, QuranPracticeActivity.class);
        intent.putExtra("letter", letter);
        startActivity(intent);
    }
}
