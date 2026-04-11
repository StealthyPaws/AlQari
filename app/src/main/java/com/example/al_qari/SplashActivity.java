package com.example.al_qari;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash); // your splash layout

        // Delay for 1 second (1000 ms), then move to MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // close splash so user can’t go back to it
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out); // optional fade animation
            }
        }, 1000);
    }
}
