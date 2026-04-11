package com.example.al_qari;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Memorization_H extends AppCompatActivity {

    private static final String TAG = "Memorization_H_AUDIO";

    private TextView surahName;
    private LinearLayout ayahContainer;
    private int surahId;

    private final OkHttpClient client = new OkHttpClient();
    private MediaPlayer mediaPlayer;

    private final List<String> fullAudioList = new ArrayList<>();
    private int playAllIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memorization_h);

        Log.d(TAG, "onCreate started");

        surahName = findViewById(R.id.surahName);
        ayahContainer = findViewById(R.id.hAyahContainer);

        surahId = getIntent().getIntExtra("surah_id", -1);
        Log.d(TAG, "SurahId received: " + surahId);

        if (surahId != -1) {
            fetchSurahName(surahId);
            fetchAyahs(surahId);
        } else {
            Log.e(TAG, "Invalid surah_id in intent");
        }

        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

        ImageButton btnPlayAll = findViewById(R.id.btnPlayAll);
        btnPlayAll.setOnClickListener(v -> {
            Log.d(TAG, "Play ALL button clicked");
            playAllAyahs();
        });
    }

    private void fetchSurahName(int surahId) {
        String url = ApiConfig.BASE_URL + "/get_surah_name?surah_id=" + surahId;
        Log.d(TAG, "Fetching Surah name from: " + url);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Surah name fetch FAILED", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "Surah name response code: " + response.code());
                if (!response.isSuccessful()) return;

                try {
                    String body = response.body().string();
                    Log.d(TAG, "Surah name response body: " + body);

                    JSONObject json = new JSONObject(body);
                    String nameArabic = json.getString("name_arabic");

                    runOnUiThread(() -> surahName.setText("سورة " + nameArabic));

                } catch (JSONException e) {
                    Log.e(TAG, "JSON error in surah name", e);
                }
            }
        });
    }

    private void fetchAyahs(int surahId) {
        String url = ApiConfig.BASE_URL + "/get_ayahs?surah_id=" + surahId + "&offset=0&limit=1000";
        Log.d(TAG, "Fetching AYAH list from: " + url);

        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Ayah fetch FAILED", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "Ayah list response code: " + response.code());

                try {
                    String body = response.body().string();
                    Log.d(TAG, "Ayah JSON raw response: " + body);

                    JSONObject json = new JSONObject(body);
                    JSONArray ayahs = json.getJSONArray("ayahs");

                    runOnUiThread(() -> displayAyahs(ayahs));

                } catch (JSONException e) {
                    Log.e(TAG, "Ayah list JSON parse error", e);
                }
            }
        });
    }

    private void displayAyahs(JSONArray arr) {
        LayoutInflater inflater = LayoutInflater.from(this);

        try {
            for (int i = 0; i < arr.length(); i++) {

                JSONObject ayah = arr.getJSONObject(i);
                String arabic = ayah.getString("arabic_indopak");
                int verseNumber = ayah.getInt("verse_number");

                String rawPath = ayah.getString("audio_url");
                Log.d(TAG, "RAW audio path from DB: " + rawPath);

                String audioUrl = ApiConfig.BASE_URL + "/" + rawPath;
                Log.d(TAG, "FINAL FULL audio URL: " + audioUrl);

                fullAudioList.add(audioUrl);

                View itemView = inflater.inflate(R.layout.item_ayah_memorization, ayahContainer, false);

                TextView tvAyahNum = itemView.findViewById(R.id.ayahNumberBox);
                TextView tvArabic = itemView.findViewById(R.id.tvAyahArabic);
                ImageButton btnPlay = itemView.findViewById(R.id.btnPlayAyah);

                tvAyahNum.setText(convertToArabicNum(verseNumber));

                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append(arabic).append("   ");

                Bitmap markerBitmap = drawNumberOnMarker(verseNumber);
                int size = (int) (getResources().getDisplayMetrics().density * 40);
                Bitmap scaled = Bitmap.createScaledBitmap(markerBitmap, size, size, true);

                BitmapDrawable drawable = new BitmapDrawable(getResources(), scaled);
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());

                SpannableStringBuilder markerSpan = new SpannableStringBuilder(" ");
                markerSpan.setSpan(new ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                builder.append(markerSpan).append(" ");
                tvArabic.setText(builder);

                btnPlay.setOnClickListener(v -> {
                    Log.d(TAG, "User tapped PLAY for ayah #" + verseNumber);
                    Log.d(TAG, "Playing AYAH audio URL: " + audioUrl);
                    playAudio(audioUrl);
                });

                ayahContainer.addView(itemView);
            }

        } catch (Exception e) {
            Log.e(TAG, "displayAyahs ERROR", e);
        }
    }

    private void playAudio(String url) {
        Log.d(TAG, "Attempting to play audio: " + url);

        try {
            if (mediaPlayer != null) mediaPlayer.reset();
            else mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer ERROR what=" + what + " extra=" + extra);
                return true;
            });

            mediaPlayer.setDataSource(url);
            Log.d(TAG, "MediaPlayer setDataSource SUCCESS");

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer PREPARED. Starting playback");
                mp.start();
            });

            mediaPlayer.prepareAsync();
            Log.d(TAG, "prepareAsync called");

        } catch (Exception e) {
            Log.e(TAG, "AUDIO PLAY ERROR", e);
        }
    }

    private void playAllAyahs() {
        Log.d(TAG, "PLAY ALL invoked");

        if (fullAudioList.isEmpty()) {
            Log.e(TAG, "No audio URLs loaded. fullAudioList EMPTY");
            return;
        }

        playAllIndex = 0;
        playNextAyah();
    }

    private void playNextAyah() {
        if (playAllIndex >= fullAudioList.size()) {
            Log.d(TAG, "PlayAll FINISHED");
            return;
        }

        String url = fullAudioList.get(playAllIndex);
        Log.d(TAG, "PlayAll: Now playing index " + playAllIndex + " URL=" + url);

        try {
            if (mediaPlayer != null) mediaPlayer.reset();
            else mediaPlayer = new MediaPlayer();

            mediaPlayer.setOnPreparedListener(MediaPlayer::start);

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "PlayAll MediaPlayer ERROR what=" + what + " extra=" + extra);
                return true;
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                playAllIndex++;
                playNextAyah();
            });

            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

        } catch (Exception e) {
            Log.e(TAG, "PlayAll ERROR", e);
        }
    }

    private Bitmap drawNumberOnMarker(int number) {
        Bitmap base = BitmapFactory.decodeResource(getResources(), R.drawable.marker)
                .copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(base);
        Paint paint = new Paint();

        paint.setColor(android.graphics.Color.WHITE);
        paint.setTextSize(base.getWidth() / 2.5f);
        paint.setFakeBoldText(true);
        paint.setAntiAlias(true);
        paint.setTextAlign(Paint.Align.CENTER);

        float x = base.getWidth() / 2f;
        float y = (base.getHeight() / 2f) - ((paint.descent() + paint.ascent()) / 2);

        canvas.drawText(convertToArabicNum(number), x, y, paint);
        return base;
    }

    private String convertToArabicNum(int number) {
        String[] d = {"٠","١","٢","٣","٤","٥","٦","٧","٨","٩"};
        StringBuilder out = new StringBuilder();
        for (char c : String.valueOf(number).toCharArray()) out.append(d[c - '0']);
        return out.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
