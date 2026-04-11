package com.example.al_qari;

import java.io.InputStream;
import java.io.IOException;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

public class AlJawfActivity_lp extends AppCompatActivity {

    private View overviewCard;
    private TextView card1Title, card1Body, card2Title, card2Body;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.al_jawf_lp);

        // 🧠 Get module info from Intent (Lessons or Practice)
        String module = getIntent().getStringExtra("module");
        if (module == null)
            module = "Lessons"; // fallback

        // ✅ Update global trackers for IntentRouter
        QuranLessonActivity.currentMode = module;
        QuranLessonActivity.currentSubModule = "Al-Jauf";

        // --------------------------
        // ✅ Navigation Bar Buttons
        // --------------------------
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

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // Top Bar
        TextView topBarText = findViewById(R.id.topBarText);
        topBarText.setVisibility(View.VISIBLE);
        topBarText.setText("الجوف");
        topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);

        // --------------------------
        // 🔹 Initialize Cards
        // --------------------------
        overviewCard = findViewById(R.id.overviewCard);
        card1Title = findViewById(R.id.card1Title);
        card1Body = findViewById(R.id.card1Body);
        card2Title = findViewById(R.id.card2Title);
        card2Body = findViewById(R.id.card2Body);

        // Hide overviewCard since Al-Jawf has no image
        if (overviewCard != null)
            overviewCard.setVisibility(View.GONE);

        // --------------------------
        // 📖 Load Description
        // --------------------------
        String fullText;
        try (InputStream descIs = getAssets().open("Makharij/ALJauf/Description.txt")) {
            byte[] buffer = new byte[descIs.available()];
            descIs.read(buffer);
            fullText = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            fullText = "Description not found.";
        }

        // --------------------------
        // 🪶 Split into two sections manually
        // --------------------------
        String part1 = "";
        String part2 = "";

        // You can adjust these split markers as needed
        int splitIndex = fullText.indexOf("These three Madd");
        if (splitIndex > 0) {
            part1 = fullText.substring(0, splitIndex).trim();
            part2 = fullText.substring(splitIndex).trim();
        } else {
            part1 = fullText;
        }

        card1Title.setText("General Overview");
        card1Body.setText(part1);

        if (!part2.isEmpty()) {
            card2Title.setText("How the Madd Letters Function");
            card2Body.setText(part2);
            findViewById(R.id.card2).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.card2).setVisibility(View.GONE);
        }

        // --------------------------
        // 🅰️ Buttons for letters
        // --------------------------
        MaterialButton btnAlif = findViewById(R.id.btnAlif);
        MaterialButton btnWaw = findViewById(R.id.btnWaw);
        MaterialButton btnYa = findViewById(R.id.btnYa);

        Log.d("AlJawfActivity_lp", "Module: " + module);

        if ("Lessons".equalsIgnoreCase(module)) {
            btnAlif.setOnClickListener(v -> openLetterLesson(
                    "ا",
                    "Makharij/ALJauf/Alif/Description.txt",
                    "Makharij/ALJauf/Alif/01_alif.mp3",
                    "Makharij/ALJauf/Alif/aa.mp3",
                    "Makharij/ALJauf/Alif/alif.png"));

            btnWaw.setOnClickListener(v -> openLetterLesson(
                    "و",
                    "Makharij/ALJauf/Waw/Description.txt",
                    "Makharij/ALJauf/Waw/27_waw.mp3",
                    "Makharij/ALJauf/Waw/wu (waw).mp3",
                    "Makharij/ALJauf/Waw/waw.png"));

            btnYa.setOnClickListener(v -> openLetterLesson(
                    "ي",
                    "Makharij/ALJauf/Ya/Description.txt",
                    null,
                    null,
                    null));
        } else if ("Practice".equalsIgnoreCase(module)) {
            btnAlif.setOnClickListener(v -> openLetterPractice("ا"));
            btnWaw.setOnClickListener(v -> openLetterPractice("و"));
            btnYa.setOnClickListener(v -> openLetterPractice("ي"));
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
