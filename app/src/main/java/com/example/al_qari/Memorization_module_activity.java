package com.example.al_qari;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class Memorization_module_activity extends AppCompatActivity {

    private Button btnTester, btnHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memorization_module_activity);

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        btnTester = findViewById(R.id.btnTester);
        btnHelper = findViewById(R.id.btnHelper);

        // Get the Surah ID as an integer
        int surahId = getIntent().getIntExtra("surah_id", -1);

        if (surahId == -1) {
            Toast.makeText(this, "No Surah ID received", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Tester button click
        btnTester.setOnClickListener(v -> {
            Intent tIntent = new Intent(Memorization_module_activity.this, Memorization_T.class);
            tIntent.putExtra("surah_id", surahId); // send as int
            tIntent.putExtra("mode", "tester");
            startActivity(tIntent);
        });

        // Helper button click
        btnHelper.setOnClickListener(v -> {
            Intent pIntent = new Intent(Memorization_module_activity.this, Memorization_H.class);
            pIntent.putExtra("surah_id", surahId); // send as int
            pIntent.putExtra("mode", "helper");
            startActivity(pIntent);
        });

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
}
