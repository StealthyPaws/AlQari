package com.example.al_qari;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class Quran_Basics extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quran_basics);

        // --- ✅ Navigation Bar Logic ---
        View navInclude = findViewById(R.id.includeButtons);

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

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // --- Top Bar ---
        TextView topBarText = findViewById(R.id.topBarText);
        if (topBarText != null) {
            topBarText.setText("Quran Basics");
            topBarText.setVisibility(View.VISIBLE);
            topBarText.setTypeface(topBarText.getTypeface(), Typeface.BOLD);
        }

        // --- Center Buttons ---
        findViewById(R.id.btnLessons).setOnClickListener(v -> {
            Intent intent = new Intent(Quran_Basics.this, QuranModuleActivity.class);
            intent.putExtra("module", "Lessons");
            startActivity(intent);
        });

        findViewById(R.id.btnPractice).setOnClickListener(v -> {
            Intent intent = new Intent(Quran_Basics.this, QuranModuleActivity.class);
            intent.putExtra("module", "Practice");
            startActivity(intent);
        });

        findViewById(R.id.btnTests).setOnClickListener(v -> {
            Intent intent = new Intent(Quran_Basics.this, QuranModuleActivity.class);
            intent.putExtra("module", "Test");
            startActivity(intent);
        });

    }
}
