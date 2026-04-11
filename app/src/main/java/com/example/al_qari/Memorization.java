package com.example.al_qari;

import android.graphics.Typeface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Memorization extends AppCompatActivity {

    private LinearLayout buttonContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memorization);

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // Top bar from include
        View topBar = findViewById(R.id.includeTopBar);
        TextView topBarText = topBar.findViewById(R.id.topBarText);
        topBarText.setText("Memorization");
        topBarText.setVisibility(View.VISIBLE);
        topBarText.setTypeface(topBarText.getTypeface(), Typeface.BOLD);

        // Button container
        buttonContainer = findViewById(R.id.buttonContainer);

        // Load Surahs
        List<String> surahs = loadSurahsFromFile();

        // Create buttons dynamically
        createButtons(surahs);

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

    private List<String> loadSurahsFromFile() {
        List<String> surahList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(getAssets().open("surahs.txt")))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    surahList.add(line.trim());
                }
            }
        } catch (Exception e) {
            Log.e("Memorization", "Failed to load Surahs: " + e.getMessage());
        }
        return surahList;
    }

    private void createButtons(List<String> surahs) {
        if (buttonContainer == null) {
            Log.e("createButtons", "buttonContainer is null!");
            return;
        }

        Typeface indoPakFont = null;
        try {
            indoPakFont = Typeface.createFromAsset(getAssets(), "fonts/indopak.ttf");
        } catch (Exception e) {
            Log.e("createButtons", "Font not found: " + e.getMessage());
        }

        for (int i = 0; i < surahs.size(); i++) {
            String surahLine = surahs.get(i);
            String[] parts = surahLine.split(":", 2);
            if (parts.length < 2)
                continue;

            // --- Extract Surah ID safely ---
            int surahId = -1;
            try {
                surahId = Integer.parseInt(parts[0].trim());
            } catch (NumberFormatException e) {
                Log.e("createButtons", "Invalid Surah ID format: " + parts[0]);
                continue;
            }

            String names = parts[1].trim();
            String[] nameParts = names.split(" - ");
            String buttonText = String.join("\n", nameParts);

            // --- Create button ---
            MaterialButton btn = new MaterialButton(this);
            btn.setText(buttonText);
            btn.setTextSize(16);
            btn.setAllCaps(false);
            if (indoPakFont != null)
                btn.setTypeface(indoPakFont);
            btn.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            // Cycle through colors: Blue -> Green -> Pink -> Lavender
            int[] buttonDrawables = {
                    R.drawable.bg_duo_button_blue,
                    R.drawable.bg_duo_button_green,
                    R.drawable.bg_duo_button_pink,
                    R.drawable.bg_duo_button_lavender
            };
            try {
                btn.setBackgroundResource(buttonDrawables[i % buttonDrawables.length]);
                btn.setBackgroundTintList(null);
            } catch (Exception e) {
                Log.e("createButtons", "Drawable not found: " + e.getMessage());
            }

            btn.setCornerRadius((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics()));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90,
                            getResources().getDisplayMetrics()));
            params.setMargins(0, 16, 0, 16);
            btn.setLayoutParams(params);

            final int finalSurahId = surahId;

            // --- OnClickListener ---
            btn.setOnClickListener(v -> {
                Log.d("Memorization", "Button clicked for Surah ID: " + finalSurahId);
                try {
                    Intent intent = new Intent(Memorization.this, Memorization_module_activity.class);
                    intent.putExtra("surah_id", finalSurahId); // ✅ send as integer
                    Log.d("Memorization", "Starting Memorization_module_activity with surah_id: " + finalSurahId);
                    startActivity(intent);
                } catch (Exception e) {
                    Log.e("Memorization", "Error starting activity: " + e.getMessage());
                }
            });

            buttonContainer.addView(btn);
        }
    }
}
