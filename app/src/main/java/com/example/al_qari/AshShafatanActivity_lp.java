package com.example.al_qari;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class AshShafatanActivity_lp extends AppCompatActivity {

    private TextView topBarText;
    private TextView cardOverviewText;
    private TextView cardBottomText;
    private TextView cardMeetingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ash_shafatan_lp);

        // 🧠 Get module info from Intent (Lessons or Practice)
        String module = getIntent().getStringExtra("module");
        if (module == null)
            module = "Lessons";

        // ✅ Update global trackers for IntentRouter
        QuranLessonActivity.currentMode = module;
        QuranLessonActivity.currentSubModule = "Ash-Shafatan";

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
        topBarText = findViewById(R.id.topBarText);
        topBarText.setText("الشفتان");
        topBarText.setVisibility(TextView.VISIBLE);
        topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);

        // --- 🟣 Initialize Views ---
        cardOverviewText = findViewById(R.id.cardOverviewText);
        cardBottomText = findViewById(R.id.cardbottomText);
        cardMeetingText = findViewById(R.id.cardMeetingText);

        // --- 🟣 Load Description Data ---
        loadDescriptionText();

        // --- Load Description Data ---
        loadDescriptionText();

        // Letter buttons
        MaterialButton btnBa = findViewById(R.id.btnBa);
        MaterialButton btnFa = findViewById(R.id.btnFa);
        MaterialButton btnMeem = findViewById(R.id.btnMeem);
        MaterialButton btnWaw = findViewById(R.id.btnWaw);

        MaterialButton[] buttons = { btnBa, btnFa, btnMeem, btnWaw };
        String[] letters = { "ب", "ف", "م", "و" };

        for (int i = 0; i < buttons.length; i++) {
            final String letter = letters[i];
            MaterialButton btn = buttons[i];

            if ("Lessons".equalsIgnoreCase(module)) {
                btn.setOnClickListener(v -> {
                    switch (letter) {
                        case "ب":
                            openLetterLesson("ب",
                                    "Makharij/Ashufataan/ب/Description.txt",
                                    "Makharij/Ashufataan/ب/02_baa.mp3",
                                    "Makharij/Ashufataan/ب/bb.mp3",
                                    "Makharij/Ashufataan/ب/ba.png");
                            break;
                        case "ف":
                            openLetterLesson("ف",
                                    "Makharij/Ashufataan/ف/Description.txt",
                                    "Makharij/Ashufataan/ف/20_fa.mp3",
                                    "Makharij/Ashufataan/ف/ff (fa).mp3",
                                    "Makharij/Ashufataan/ف/fa.png");
                            break;
                        case "م":
                            openLetterLesson("م",
                                    "Makharij/Ashufataan/م/Description.txt",
                                    "Makharij/Ashufataan/م/24_meem.mp3",
                                    "Makharij/Ashufataan/م/mm (miim).mp3",
                                    "Makharij/Ashufataan/م/meem.png");
                            break;
                        case "و":
                            openLetterLesson("و",
                                    "Makharij/Ashufataan/و/Description.txt",
                                    "Makharij/Ashufataan/و/27_waw.mp3",
                                    "Makharij/Ashufataan/و/wu (waw).mp3",
                                    "Makharij/Ashufataan/و/waw.png");
                            break;
                    }
                });
            } else if ("Practice".equalsIgnoreCase(module)) {
                btn.setOnClickListener(v -> openLetterPractice(letter));
            }
        }
        // --- ✅ Navigation Bar Logic ---
        View navInclude = findViewById(R.id.include_bottomBar);
        if (navInclude != null) {
            View btnQuranBasics = navInclude.findViewById(R.id.btnQuranBasics);
            View btnMemorize = navInclude.findViewById(R.id.btnMemorize);
            View btnSalah = navInclude.findViewById(R.id.btnSalah);

            if (btnQuranBasics != null) {
                btnQuranBasics.setOnClickListener(v -> startActivity(new Intent(this, Quran_Basics.class)));
            }
            if (btnMemorize != null) {
                btnMemorize.setOnClickListener(v -> startActivity(new Intent(this, Memorization.class)));
            }
            if (btnSalah != null) {
                btnSalah.setOnClickListener(v -> startActivity(new Intent(this, Salah.class)));
            }
        }
    }

    // --- Function to Load Description.txt ---
    private void loadDescriptionText() {
        try {
            InputStream inputStream = getAssets().open("Makharij/Ashufataan/Description.txt");
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
            reader.close();
            String content = builder.toString().trim();

            // Split sections
            String[] parts = content.split("The bottom lip touching the upper incisors:");
            if (parts.length > 1) {
                String overviewPart = parts[0].trim();
                String[] subParts = parts[1].split("The meeting of the two lips:");

                String bottomPart = subParts[0].trim();
                String meetingPart = (subParts.length > 1) ? subParts[1].trim() : "";

                // Remove headings from visible text
                cardOverviewText.setText(overviewPart);
                cardBottomText.setText(bottomPart);
                cardMeetingText.setText(meetingPart);
            }

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
