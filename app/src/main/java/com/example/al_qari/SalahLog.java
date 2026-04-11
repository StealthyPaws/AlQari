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

public class SalahLog extends AppCompatActivity {

    private Spinner spinnerSurahs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.salah_log);

        // --- ✅ Navigation Bar Logic ---
        View navInclude = findViewById(R.id.include_bottomBar);
        if (navInclude != null) {
            View btnQuranBasics = navInclude.findViewById(R.id.btnQuranBasics);
            View navBtnMemorize = navInclude.findViewById(R.id.btnMemorize);
            View btnSalah = navInclude.findViewById(R.id.btnSalah);

            if (btnQuranBasics != null) {
                btnQuranBasics.setOnClickListener(v -> startActivity(new Intent(this, Quran_Basics.class)));
            }
            if (navBtnMemorize != null) {
                navBtnMemorize.setOnClickListener(v -> startActivity(new Intent(this, Memorization.class)));
            }
            if (btnSalah != null) {
                btnSalah.setOnClickListener(v -> startActivity(new Intent(this, Salah.class)));
            }
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

        // --- Top Bar setup ---
        View topBar = findViewById(R.id.include_topBar);
        if (topBar != null) {
            View topText = topBar.findViewById(R.id.topBarText);
            if (topText instanceof android.widget.TextView) {
                ((android.widget.TextView) topText).setText("Salah Guide");
            }
        }

        // --- Surah Spinner ---
        spinnerSurahs = findViewById(R.id.spinnerSurahs);
        ArrayList<String> surahList = loadSurahNames(this);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                surahList);
        spinnerSurahs.setAdapter(adapter);

        // --- Record Button ---
        MaterialButton btnRecord = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(
                v -> Toast.makeText(this, "Recording feature coming soon!", Toast.LENGTH_SHORT).show());

        // --- Salah Buttons (View Logs placeholder) ---
        int[] salahButtons = {
                R.id.btnFajr, R.id.btnDhuhr, R.id.btnAsr,
                R.id.btnMaghrib, R.id.btnEisha
        };

        for (int id : salahButtons) {
            MaterialButton btn = findViewById(id);
            btn.setOnClickListener(
                    v -> Toast.makeText(this, "Logs feature not yet implemented.", Toast.LENGTH_SHORT).show());
        }
    }

    // --- Load Surahs from surahs.txt (split on second '-') ---
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
