package com.example.al_qari;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class QuranModuleActivity extends AppCompatActivity {

    private String module; // Lessons, Practice, Test

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quran_module_activity);

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

        // Get module from intent
        module = getIntent().getStringExtra("module");
        if (module == null)
            module = "Lessons"; // default

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // Update top bar text dynamically
        TextView topBarText = findViewById(R.id.topBarText);
        topBarText.setText(module);
        topBarText.setVisibility(View.VISIBLE);
        topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);

        // Set button click listeners
        findViewById(R.id.btnAlJawf).setOnClickListener(v -> openSection("AlJawf"));
        findViewById(R.id.btnAlHalq).setOnClickListener(v -> openSection("AlHalq"));
        findViewById(R.id.btnAlLisan).setOnClickListener(v -> openSection("AlLisan"));
        findViewById(R.id.btnAshShafatan).setOnClickListener(v -> openSection("AshShafatan"));
        findViewById(R.id.btnAlKhayshum).setOnClickListener(v -> openSection("AlKhayshum"));
    }

    private void openSection(String section) {
        if ("Test".equals(module)) {
            Toast.makeText(this, "Test module coming soon for " + section, Toast.LENGTH_SHORT).show();
            return;
        }

        // Lessons and Practice open the same activity, just pass module
        Intent intent = getSectionIntent(section);
        intent.putExtra("module", module); // pass "Lessons" or "Practice"
        startActivity(intent);
    }

    private Intent getSectionIntent(String section) {
        switch (section) {
            case "AlJawf":
                return new Intent(this, AlJawfActivity_lp.class);
            case "AlHalq":
                return new Intent(this, AlHalqActivity_lp.class);
            case "AlLisan":
                return new Intent(this, AlLisaanActivity_lp.class);
            case "AshShafatan":
                return new Intent(this, AshShafatanActivity_lp.class);
            case "AlKhayshum":
                return new Intent(this, AlKhayshumActivity_lp.class);
            default:
                return new Intent(this, AlJawfActivity_lp.class);
        }
    }
}
