package com.example.al_qari;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * QuranLessonActivity
 * -------------------
 * Displays an individual letter lesson in this order:
 * 1) (Optional) Image
 * 2) Makhrij card + Audio 1
 * 3) How to Pronounce card + Audio 2
 * 4) Practice button
 */
public class QuranLessonActivity extends AppCompatActivity {

    // ===== Global static trackers =====
    public static String currentLetter = null;
    public static String currentDescriptionPath = null;
    public static String currentAudioPath = null;
    public static String currentSubModule = null;
    public static String currentMode = null;

    // ===== UI elements =====
    private TextView topBarText, makhrajTitle, makhrajBody, pronounceTitle, pronounceBody;
    private ImageView letterImage;
    private LinearLayout letterImageCard; // parent container
    private MaterialButton audioBtn1, audioBtn2, practiceBtn;
    private View makhrajCard, pronounceCard;

    // Popup views
    private android.widget.FrameLayout popupContainer;
    private ImageView popupImage;

    // ===== Media players =====
    private MediaPlayer mediaPlayer1, mediaPlayer2;

    private static class LessonSections {
        String makhraj;
        String pronounce;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quran_lesson_activity);

        // ---- Init Views ----
        topBarText = findViewById(R.id.topBarText);
        letterImage = findViewById(R.id.letterImage);
        letterImageCard = findViewById(R.id.letterImageCard);
        makhrajCard = findViewById(R.id.makhrajCard);
        pronounceCard = findViewById(R.id.pronounceCard);
        makhrajTitle = findViewById(R.id.makhrajTitle);
        makhrajBody = findViewById(R.id.makhrajBody);
        pronounceTitle = findViewById(R.id.pronounceTitle);
        pronounceBody = findViewById(R.id.pronounceBody);
        audioBtn1 = findViewById(R.id.audioBtn1);
        audioBtn2 = findViewById(R.id.audioBtn2);
        practiceBtn = findViewById(R.id.practiceBtn);

        // Popup init
        popupContainer = findViewById(R.id.popupContainer);
        popupImage = findViewById(R.id.popupImage);

        if (popupContainer != null) {
            popupContainer.setOnClickListener(v -> hidePopup());
        }

        // ---- Intent Extras ----
        final String letter = getIntent().getStringExtra("letter");
        final String descriptionPath = getIntent().getStringExtra("description");
        final String audio1Path = getIntent().getStringExtra("audio1");
        final String audio2Path = getIntent().getStringExtra("audio2");
        final String imagePath = getIntent().getStringExtra("image");
        final String subModule = getIntent().getStringExtra("subModule");
        final String module = getIntent().getStringExtra("module");

        currentLetter = letter;
        currentDescriptionPath = descriptionPath;
        currentAudioPath = audio1Path;
        currentSubModule = subModule;
        currentMode = module;

        // ---- Top Bar ----
        if (topBarText != null) {
            topBarText.setText(letter != null ? letter : "");
            topBarText.setVisibility(View.VISIBLE);
            topBarText.setTypeface(topBarText.getTypeface(), android.graphics.Typeface.BOLD);
        }

        AssetManager am = getAssets();

        // ---- Image (Optional) ----
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            try (InputStream imgStream = am.open(imagePath)) {
                Drawable drawable = Drawable.createFromStream(imgStream, null);
                letterImage.setImageDrawable(drawable);
                letterImage.setVisibility(View.VISIBLE);
                letterImageCard.setVisibility(View.VISIBLE);

                // Set click listener to show popup
                letterImageCard.setOnClickListener(v -> showPopup(letterImage));

            } catch (IOException e) {
                // Hide both if not found or failed to load
                letterImage.setVisibility(View.GONE);
                letterImageCard.setVisibility(View.GONE);
            }
        } else {
            letterImage.setVisibility(View.GONE);
            letterImageCard.setVisibility(View.GONE);
        }

        // ---- Load Description ----
        if (descriptionPath != null) {
            try (InputStream descStream = am.open(descriptionPath);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(descStream))) {

                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                    sb.append(line).append("\n");

                LessonSections sections = parseDescription(sb.toString());

                if (sections.makhraj != null && !sections.makhraj.isEmpty()) {
                    makhrajTitle.setText("Makhrij (Articulation Point)");
                    makhrajBody.setText(sections.makhraj);
                    makhrajCard.setVisibility(View.VISIBLE);
                } else {
                    makhrajCard.setVisibility(View.GONE);
                }

                if (sections.pronounce != null && !sections.pronounce.isEmpty()) {
                    pronounceTitle.setText("How to Pronounce");
                    pronounceBody.setText(sections.pronounce);
                    pronounceCard.setVisibility(View.VISIBLE);
                } else {
                    pronounceCard.setVisibility(View.GONE);
                }

            } catch (IOException e) {
                makhrajCard.setVisibility(View.GONE);
                pronounceCard.setVisibility(View.GONE);
            }
        } else {
            makhrajCard.setVisibility(View.GONE);
            pronounceCard.setVisibility(View.GONE);
        }

        // ---- Audio 1 ----
        if (audio1Path != null && !audio1Path.trim().isEmpty()) {
            try {
                mediaPlayer1 = new MediaPlayer();
                AssetFileDescriptor afd1 = am.openFd(audio1Path);
                mediaPlayer1.setDataSource(afd1.getFileDescriptor(), afd1.getStartOffset(), afd1.getLength());
                mediaPlayer1.prepare();
                audioBtn1.setOnClickListener(v -> toggleAudio(mediaPlayer1));
                audioBtn1.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                audioBtn1.setVisibility(View.GONE);
            }
        } else {
            audioBtn1.setVisibility(View.GONE);
        }

        // ---- Audio 2 ----
        if (audio2Path != null && !audio2Path.trim().isEmpty()) {
            try {
                mediaPlayer2 = new MediaPlayer();
                AssetFileDescriptor afd2 = am.openFd(audio2Path);
                mediaPlayer2.setDataSource(afd2.getFileDescriptor(), afd2.getStartOffset(), afd2.getLength());
                mediaPlayer2.prepare();
                audioBtn2.setOnClickListener(v -> toggleAudio(mediaPlayer2));
                audioBtn2.setVisibility(View.VISIBLE);
            } catch (IOException e) {
                audioBtn2.setVisibility(View.GONE);
            }
        } else {
            audioBtn2.setVisibility(View.GONE);
        }

        // ---- Practice ----
        practiceBtn.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuranPracticeActivity.class);
            intent.putExtra("letter", letter);
            startActivity(intent);
        });

        // --- Navigation Bar Logic ---
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

    private void toggleAudio(MediaPlayer player) {
        if (player.isPlaying())
            player.pause();
        else
            player.start();
    }

    private LessonSections parseDescription(String raw) {
        LessonSections out = new LessonSections();
        if (raw == null)
            return out;

        String text = raw.replace("\r\n", "\n").trim();
        String h1 = "Makhrij (Articulation Point)";
        String h2 = "How to Pronounce";

        int i1 = indexOfIgnoreCase(text, h1);
        int i2 = indexOfIgnoreCase(text, h2);

        if (i1 >= 0 && i2 > i1) {
            int startBody1 = text.indexOf('\n', i1);
            out.makhraj = text.substring(Math.max(startBody1 + 1, i1), i2).trim();
        } else if (i1 >= 0) {
            int startBody1 = text.indexOf('\n', i1);
            out.makhraj = text.substring(Math.max(startBody1 + 1, i1)).trim();
        }

        if (i2 >= 0) {
            int startBody2 = text.indexOf('\n', i2);
            out.pronounce = text.substring(Math.max(startBody2 + 1, i2)).trim();
        }

        return out;
    }

    private int indexOfIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().indexOf(needle.toLowerCase());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer(mediaPlayer1);
        releasePlayer(mediaPlayer2);
        mediaPlayer1 = null;
        mediaPlayer2 = null;
        currentLetter = currentDescriptionPath = currentAudioPath = currentSubModule = currentMode = null;
    }

    private void releasePlayer(MediaPlayer mp) {
        if (mp != null) {
            if (mp.isPlaying())
                mp.stop();
            mp.release();
        }
    }

    private void showPopup(ImageView sourceImage) {
        if (popupImage == null || popupContainer == null)
            return;

        // Copy drawable from clicked ImageView into popupImage
        popupImage.setImageDrawable(sourceImage.getDrawable());

        popupContainer.setAlpha(0f);
        popupContainer.setVisibility(View.VISIBLE);
        popupContainer.animate().alpha(1f).setDuration(200).start();
    }

    private void hidePopup() {
        if (popupContainer == null)
            return;

        popupContainer.animate().alpha(0f).setDuration(150).withEndAction(() -> {
            popupContainer.setVisibility(View.GONE);
        }).start();
    }
}
