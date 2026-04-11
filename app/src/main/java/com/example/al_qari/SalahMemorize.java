package com.example.al_qari;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class SalahMemorize extends AppCompatActivity {

    private Spinner spinnerSurahs, spinnerSupplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.salah_memorize);

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // --- ✅ Top Bar Setup ---
        View topBar = findViewById(R.id.include_topBar);
        if (topBar != null) {
            android.widget.TextView topBarText = topBar.findViewById(R.id.topBarText);
            if (topBarText != null) {
                topBarText.setText("Salah Memorize");
                topBarText.setVisibility(View.VISIBLE);
                topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);
            }
        }

        // --- ✅ Navigation Bar Logic ---
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

        // --- 🟢 Spinner + Button setup ---
        spinnerSurahs = findViewById(R.id.spinnerSurahs);
        spinnerSupplication = findViewById(R.id.spinnerSupplication);
        MaterialButton btnStart = findViewById(R.id.btnStart);

        // Load surah names
        ArrayList<String> surahList = loadSurahNames(this);
        if (surahList.isEmpty()) {
            surahList.add("الفاتحة - Al-Fatihah");
            surahList.add("المسد - Al-Masad");
            surahList.add("الإخلاص - Al-Ikhlas");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                surahList);

        // Supplication dropdown
        ArrayList<String> supplications = new ArrayList<>();
        supplications.add("رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِنْ ذُرِّيَّتِي");

        ArrayAdapter<String> supplicationAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, supplications);

        spinnerSurahs.setAdapter(adapter);
        spinnerSupplication.setAdapter(supplicationAdapter);

        // On Start button click
        btnStart.setOnClickListener(v -> Toast.makeText(this, "Memorize Salah coming soon", Toast.LENGTH_SHORT).show());
    }

    // 🧠 Function to read Surahs from assets/surahs.txt (split on second '-')
    // --- Load only the first names from surahs.txt (e.g., "Al-Fatiha") ---
    private ArrayList<String> loadSurahNames(Context context) {
        ArrayList<String> surahs = new ArrayList<>();
        AssetManager assetManager = context.getAssets();

        try (InputStream is = assetManager.open("surahs.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;

                // Match everything from start until the first " - " (space-dash-space)
                // Handles both "Al-Kahf - The Cave..." and "Maryam - Mary..."
                String[] parts = line.split("\\s-\\s", 2);
                if (parts.length > 0) {
                    String englishName = parts[0].trim();

                    // Remove numbering or stray colons (e.g. ":18" or "18:")
                    englishName = englishName.replaceAll("[:0-9]+$", "").trim();

                    if (!englishName.isEmpty()) {
                        surahs.add(englishName);
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return surahs;
    }

}
