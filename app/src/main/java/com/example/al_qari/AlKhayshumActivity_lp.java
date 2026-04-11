package com.example.al_qari;

import android.content.Intent;
import android.widget.FrameLayout;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AlKhayshumActivity_lp extends AppCompatActivity {

    private ImageView overviewImage;
    private TextView cardOverviewText;
    // Popup views
    private FrameLayout popupContainer;
    private ImageView popupImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.al_khayshum_lp);

        // 🧠 Get module info from Intent (Lessons or Practice)
        String module = getIntent().getStringExtra("module");
        if (module == null)
            module = "Lessons"; // default

        // ✅ Update global trackers for IntentRouter
        QuranLessonActivity.currentMode = module;
        QuranLessonActivity.currentSubModule = "Al-Khaishoom";

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // 🟣 Top Bar
        TextView topBarText = findViewById(R.id.topBarText);
        topBarText.setText("الخيشوم");
        topBarText.setVisibility(TextView.VISIBLE);
        topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);

        // 🟣 Initialize Views
        overviewImage = findViewById(R.id.overviewImage);
        cardOverviewText = findViewById(R.id.cardOverviewText);

        // Popup setup
        popupContainer = findViewById(R.id.popupContainer);
        popupImage = findViewById(R.id.popupImage);

        if (popupContainer != null) {
            popupContainer.setOnClickListener(v -> hidePopup());
        }

        overviewImage.setOnClickListener(v -> showPopup(overviewImage));

        // 🟢 Load Overview Image & Description
        loadOverviewImage();
        loadDescriptionText();

        // 🟣 Buttons
        MaterialButton btnNoon = findViewById(R.id.btnNoon);
        MaterialButton btnMeem = findViewById(R.id.btnMeem);

        if ("Lessons".equalsIgnoreCase(module)) {
            btnNoon.setOnClickListener(v -> openLetterLesson(
                    "ن",
                    "Makharij/ALKhushyum/ن/Description.txt",
                    "Makharij/ALKhushyum/ن/25_noon.mp3",
                    "Makharij/ALKhushyum/ن/nn (nuun).mp3",
                    null));

            btnMeem.setOnClickListener(v -> openLetterLesson(
                    "م",
                    "Makharij/ALKhushyum/م/Description.txt",
                    "Makharij/ALKhushyum/م/24_meem.mp3",
                    "Makharij/ALKhushyum/م/mm (miim).mp3",
                    null));
        } else if ("Practice".equalsIgnoreCase(module)) {
            btnNoon.setOnClickListener(v -> openLetterPractice("ن"));
            btnMeem.setOnClickListener(v -> openLetterPractice("م"));
        }

        // 🟣 Navigation
        View navInclude = findViewById(R.id.include_bottomBar);
        if (navInclude != null) {
            navInclude.findViewById(R.id.btnQuranBasics)
                    .setOnClickListener(v -> startActivity(new Intent(this, Quran_Basics.class)));
            navInclude.findViewById(R.id.btnMemorize)
                    .setOnClickListener(v -> startActivity(new Intent(this, Memorization.class)));
            navInclude.findViewById(R.id.btnSalah)
                    .setOnClickListener(v -> startActivity(new Intent(this, Salah.class)));
        }
    }

    // 🟢 Load Overview Image
    private void loadOverviewImage() {
        try (InputStream is = getAssets().open("Makharij/ALKhushyum/anatomy_overview.png")) {
            Bitmap bmp = BitmapFactory.decodeStream(is);
            overviewImage.setImageBitmap(bmp);
        } catch (IOException e) {
            e.printStackTrace();
            overviewImage.setVisibility(View.GONE);
        }
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

    // 🟢 Load Description Text from assets
    private void loadDescriptionText() {
        String filePath = "Makharij/ALKhushyum/Description.txt";
        AssetManager am = getAssets();
        StringBuilder text = new StringBuilder();

        try (InputStream is = am.open(filePath);
                BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line).append("\n");
            }
            cardOverviewText.setText(text.toString().trim());
        } catch (IOException e) {
            e.printStackTrace();
            cardOverviewText.setText("Description not available.");
        }
    }

    // 🟢 Open Letter Lesson
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
