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
import com.google.android.material.button.MaterialButton;

public class Salah extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.salah);

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // set topbarText
        View topBar = findViewById(R.id.include_topBar);
        TextView topBarText = topBar.findViewById(R.id.topBarText); // ✅ Correct
        topBarText.setText("Salah");
        topBarText.setVisibility(View.VISIBLE);
        topBarText.setTypeface(topBarText.getTypeface(), Typeface.BOLD);

        // 🔹 "Memorize" button
        MaterialButton btnMemorize = findViewById(R.id.btnMemorize);
        if (btnMemorize != null) {
            btnMemorize.setOnClickListener(v -> startActivity(new Intent(this, SalahMemorize.class)));
        }

        // 🔹 "Log" button
        MaterialButton btnLog = findViewById(R.id.btnLog);
        if (btnLog != null) {
            btnLog.setOnClickListener(v -> startActivity(new Intent(this, SalahLog.class)));
        }

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
    }
}
