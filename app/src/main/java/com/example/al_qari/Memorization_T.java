package com.example.al_qari;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Memorization_T extends AppCompatActivity {

    private static final String TAG = "Memorization_T";
    private static final int LIMIT = 10; // number of ayahs per load

    private LinearLayout ayahContainer;
    private ImageButton btnToggle;
    private TextView surahName;
    private ScrollView surahScrollView;

    private boolean isTextVisible = false;
    private boolean isLoading = false;
    private boolean allLoaded = false;
    private int surahId;
    private int offset = 0;
    private int totalAyahs = Integer.MAX_VALUE; // will update after first fetch

    private OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memorization_t);

        // Twinkling stars animation
        ImageView stars = findViewById(R.id.imgStars);
        if (stars != null) {
            AlphaAnimation twinkle = new AlphaAnimation(0.3f, 1.0f);
            twinkle.setDuration(800);
            twinkle.setRepeatMode(Animation.REVERSE);
            twinkle.setRepeatCount(Animation.INFINITE);
            stars.startAnimation(twinkle);
        }

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

        ayahContainer = findViewById(R.id.ayahContainer);
        btnToggle = findViewById(R.id.btnToggle);
        surahName = findViewById(R.id.surahName);
        surahScrollView = findViewById(R.id.surahScrollView);

        btnToggle.setImageResource(R.drawable.invisible);

        surahId = getIntent().getIntExtra("surah_id", -1);

        if (surahId != -1) {
            fetchSurahName(surahId);
            fetchAyahs(surahId, offset, LIMIT);
        }

        btnToggle.setOnClickListener(v -> toggleAyahVisibility());

        // Detect scroll to bottom
        surahScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            View view = surahScrollView.getChildAt(surahScrollView.getChildCount() - 1);
            int diff = (view.getBottom() - (surahScrollView.getHeight() + surahScrollView.getScrollY()));

            if (diff < 200 && !isLoading && !allLoaded) {
                Log.d(TAG, "Reached near bottom — loading next ayahs...");
                offset += LIMIT;
                fetchAyahs(surahId, offset, LIMIT);
            }
        });
    }

    private void fetchSurahName(int surahId) {
        String url = ApiConfig.BASE_URL + "get_surah_name?surah_id=" + surahId;
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch Surah name", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful())
                    return;
                try {
                    JSONObject json = new JSONObject(response.body().string());
                    String arabicName = json.getString("name_arabic");

                    runOnUiThread(() -> {
                        surahName.setText("سورة " + arabicName);
                        surahName.setTextDirection(View.TEXT_DIRECTION_RTL);
                        surahName.setTextSize(32);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            surahName.setTypeface(getResources().getFont(R.font.indopak));
                        else
                            surahName.setTypeface(ResourcesCompat.getFont(Memorization_T.this, R.font.indopak));
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing Surah name JSON", e);
                }
            }
        });
    }

    private void fetchAyahs(int surahId, int offset, int limit) {
        isLoading = true;
        String url = ApiConfig.BASE_URL + "get_ayahs?surah_id=" + surahId +
                "&offset=" + offset + "&limit=" + limit;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isLoading = false;
                Log.e(TAG, "Failed to fetch ayahs", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isLoading = false;
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Unsuccessful ayah response: " + response.code());
                    return;
                }

                try {
                    JSONObject jsonResponse = new JSONObject(response.body().string());
                    JSONArray ayahsArray = jsonResponse.getJSONArray("ayahs");
                    totalAyahs = jsonResponse.getInt("total");

                    if (ayahsArray.length() == 0) {
                        allLoaded = true;
                        Log.d(TAG, "All ayahs loaded for this Surah.");
                        return;
                    }

                    runOnUiThread(() -> addAyahsToView(ayahsArray));

                } catch (Exception e) {
                    Log.e(TAG, "Error parsing ayah JSON", e);
                }
            }
        });
    }

    private void addAyahsToView(JSONArray ayahsArray) {
        try {
            SpannableStringBuilder ayahBuilder = new SpannableStringBuilder();

            for (int i = 0; i < ayahsArray.length(); i++) {
                JSONObject ayah = ayahsArray.getJSONObject(i);
                String arabicText = ayah.getString("arabic_indopak");
                int verseNumber = ayah.getInt("verse_number");

                ayahBuilder.append(arabicText).append(" ");

                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker);
                int size = (int) (getResources().getDisplayMetrics().density * 36);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
                BitmapDrawable markerDrawable = new BitmapDrawable(getResources(), scaledBitmap);
                markerDrawable.setBounds(0, 0, markerDrawable.getIntrinsicWidth(), markerDrawable.getIntrinsicHeight());

                String arabicNum = convertToArabicNumeral(verseNumber);
                SpannableStringBuilder numberSpan = new SpannableStringBuilder(arabicNum);
                numberSpan.setSpan(new ImageSpan(markerDrawable, ImageSpan.ALIGN_BASELINE),
                        0, arabicNum.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                ayahBuilder.append(numberSpan).append(" ");
            }

            TextView ayahTextView = new TextView(this);
            ayahTextView.setText(ayahBuilder);
            ayahTextView.setTextDirection(View.TEXT_DIRECTION_RTL);
            ayahTextView.setTextSize(28);
            ayahTextView.setLineSpacing(0, 1.3f);
            ayahTextView.setTextColor(getResources().getColor(android.R.color.white));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ayahTextView.setTypeface(getResources().getFont(R.font.indopak));
            else
                ayahTextView.setTypeface(ResourcesCompat.getFont(this, R.font.indopak));

            ayahTextView.setVisibility(isTextVisible ? View.VISIBLE : View.INVISIBLE);

            ayahContainer.addView(ayahTextView);

        } catch (Exception e) {
            Log.e(TAG, "Error updating UI with ayahs", e);
        }
    }

    private void toggleAyahVisibility() {
        isTextVisible = !isTextVisible;
        btnToggle.setImageResource(isTextVisible ? R.drawable.invisible : R.drawable.visible);

        for (int i = 0; i < ayahContainer.getChildCount(); i++) {
            View view = ayahContainer.getChildAt(i);
            view.setVisibility(isTextVisible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private String convertToArabicNumeral(int number) {
        String[] arabicDigits = { "٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩" };
        StringBuilder arabicNumber = new StringBuilder();
        for (char digit : String.valueOf(number).toCharArray()) {
            arabicNumber.append(arabicDigits[digit - '0']);
        }
        return arabicNumber.toString();
    }
}
