package com.example.al_qari;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_RECORD_AUDIO = 1001;
    private TextToSpeech tts;
    private boolean startedAgent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Step 1: Show splash first
        setContentView(R.layout.activity_splash);

        // Step 2: Wait for 2 seconds, then load the real layout
        new Handler().postDelayed(() -> {
            setContentView(R.layout.activity_main);
            initializeMainScreen();
        }, 2000);
    }

    private void initializeMainScreen() {
        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        // Access and set Arabic title dynamically instead of hiding it
        TextView topBarText = findViewById(R.id.topBarText);
        if (topBarText != null) {
            topBarText.setText("Al-Qari");
            topBarText.setVisibility(View.VISIBLE); // show it again
        }

        ImageView star = findViewById(R.id.imgStar);
        if (star != null) {
            AlphaAnimation sparkle = new AlphaAnimation(0.6f, 1.0f); // fades gently, not too dim
            sparkle.setDuration(2000); // slower fade (2 seconds per cycle)
            sparkle.setRepeatMode(Animation.REVERSE);
            sparkle.setRepeatCount(Animation.INFINITE);
            star.startAnimation(sparkle);
        }

        // Initialize TTS
        initTtsThenRun();

        // Button navigation
        View btnQuranBasics = findViewById(R.id.btnQuranBasics);
        if (btnQuranBasics != null) {
            btnQuranBasics.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Quran_Basics.class);
                startActivity(intent);
            });
        }

        View btnMemorize = findViewById(R.id.btnMemorize);
        if (btnMemorize != null) {
            btnMemorize.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Memorization.class);
                startActivity(intent);
            });
        }

        View btnSalah = findViewById(R.id.btnSalah);
        if (btnSalah != null) {
            btnSalah.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, Salah.class);
                startActivity(intent);
            });
        }
    }

    /* -------------------- TTS + permission -------------------- */

    private void initTtsThenRun() {
        tts = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.SUCCESS) {
                askMicPermission();
                return;
            }
            tts.setLanguage(Locale.getDefault());
            speakAndThen(
                    "Welcome to Al Qari. I will use the microphone to listen and reply by voice.",
                    this::askMicPermission);
        });
    }

    private void askMicPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceAgentOnce();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[] { Manifest.permission.RECORD_AUDIO },
                    REQ_RECORD_AUDIO);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_RECORD_AUDIO)
            return;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceAgentOnce();
        } else {
            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    this, Manifest.permission.RECORD_AUDIO);
            if (showRationale) {
                speakAndThen(
                        "The microphone is required so I can hear you. I will ask again.",
                        this::askMicPermission);
            } else {
                speakAndThen(
                        "Microphone is off. I will open app settings. Go to Permissions, then Microphone, and choose Allow.",
                        this::openAppSettings);
            }
        }
    }

    private void openAppSettings() {
        try {
            Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open settings", Toast.LENGTH_LONG).show();
        }
    }

    private void speakAndThen(String msg, Runnable after) {
        if (tts == null) {
            after.run();
            return;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
            }

            @Override
            public void onError(String id) {
                runOnUiThread(after);
            }

            @Override
            public void onDone(String id) {
                runOnUiThread(after);
            }
        });
        tts.speak(msg, TextToSpeech.QUEUE_FLUSH, null, "intro");
    }

    /* -------------------- Voice Agent Start -------------------- */

    private void startVoiceAgentOnce() {
        if (startedAgent)
            return;
        startedAgent = true;

        Intent serviceIntent = new Intent(this, AudioStreamerService.class);
        ContextCompat.startForegroundService(this, serviceIntent);

        Toast.makeText(this, "Voice Agent started and listening…", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
