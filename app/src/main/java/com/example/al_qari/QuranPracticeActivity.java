package com.example.al_qari;

import android.Manifest;
import android.view.View;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import java.io.File;
import java.io.FileOutputStream;

import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import android.speech.tts.TextToSpeech; // ADDED
import java.util.Locale; // ADDED

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomsheet.BottomSheetDialog; // ADDED
import android.view.LayoutInflater; // ADDED

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QuranPracticeActivity extends AppCompatActivity {

    private static final String TAG = "QuranPractice";
    private static final int SAMPLE_RATE = 16000;
    private static final int REQ_MIC = 200;

    private TextView practiceLetter;
    private MaterialButton btnRecord;
    private MaterialCardView resultCard;
    private TextView practiceResult;

    private AudioRecord audioRecord;
    private volatile boolean isRecording = false;
    private ByteArrayOutputStream pcmBuffer;

    private String letter;

    private TextToSpeech tts; // ADDED for voice output

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.quran_practice_activity);

        practiceLetter = findViewById(R.id.practiceLetter);
        btnRecord = findViewById(R.id.btnRecord);
        resultCard = findViewById(R.id.resultCard);
        practiceResult = findViewById(R.id.practiceResult);

        letter = getIntent().getStringExtra("letter");
        if (letter != null)
            practiceLetter.setText(letter);

        resultCard.setVisibility(View.GONE);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.RECORD_AUDIO }, REQ_MIC);
        }

        // ADDED Text to Speech init
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
                Log.d(TAG, "LOG >>> TTS initialized");
            }
        });

        btnRecord.setOnClickListener(v -> handleRecord());
    }

    private void handleRecord() {
        if (!isRecording)
            startRecording();
        else
            stopRecording();
    }

    private void startRecording() {
        Log.d(TAG, "LOG >>> StartRecording: Triggered");

        btnRecord.setEnabled(false);
        practiceResult.setText("Initializing...");

        new Thread(() -> {
            try {
                if (AudioStreamerService.instance != null) {
                    Log.d(TAG, "LOG >>> Requesting service to pause mic");
                    AudioStreamerService.instance.pauseMic();
                }

                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> {
                        practiceResult.setText("Permission not granted");
                        btnRecord.setEnabled(true);
                    });
                    return;
                }

                int bufferSize = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                Log.d(TAG, "LOG >>> bufferSize=" + bufferSize);

                audioRecord = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize);

                if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "LOG >>> Mic failed to initialize");
                    runOnUiThread(() -> {
                        practiceResult.setText("Mic init failed");
                        btnRecord.setEnabled(true);
                    });
                    return;
                }

                pcmBuffer = new ByteArrayOutputStream();
                audioRecord.startRecording();
                isRecording = true;

                Log.d(TAG, "LOG >>> Recording started successfully");

                runOnUiThread(() -> {
                    btnRecord.setText("Stop Recording");
                    btnRecord.setEnabled(true);
                    resultCard.setVisibility(View.VISIBLE);
                    practiceResult.setText("Recording…");
                });

                byte[] buffer = new byte[bufferSize];
                while (isRecording) {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read > 0)
                        pcmBuffer.write(buffer, 0, read);
                }

            } catch (Exception e) {
                Log.e(TAG, "LOG >>> Error in startRecording", e);
                runOnUiThread(() -> {
                    practiceResult.setText("Error: " + e.getMessage());
                    btnRecord.setEnabled(true);
                });
            }
        }).start();
    }

    private void stopRecording() {
        Log.d(TAG, "LOG >>> StopRecording: Triggered");

        btnRecord.setEnabled(false);
        practiceResult.setText("Processing…");

        new Thread(() -> {
            try {
                isRecording = false;
                Thread.sleep(50);

                if (audioRecord != null) {
                    try {
                        audioRecord.stop();
                    } catch (Exception ignored) {
                    }
                    audioRecord.release();
                    audioRecord = null;
                }

                byte[] pcmData = pcmBuffer != null ? pcmBuffer.toByteArray() : new byte[0];

                Log.d(TAG, "LOG >>> PCM bytes captured: " + pcmData.length);

                byte[] wav = pcmToWav(pcmData, SAMPLE_RATE);

                Log.d(TAG, "LOG >>> WAV size after conversion: " + wav.length);

                runOnUiThread(() -> {
                    btnRecord.setText("Start Recording");
                    btnRecord.setEnabled(true);
                    practiceResult.setText("Uploading...");
                });

                sendToBackend(wav);

                if (AudioStreamerService.instance != null) {
                    Log.d(TAG, "LOG >>> Requesting service to resume mic");
                    AudioStreamerService.instance.resumeMic();
                }

            } catch (Exception e) {
                Log.e(TAG, "LOG >>> Error in stopRecording", e);
                runOnUiThread(() -> {
                    practiceResult.setText("Error: " + e.getMessage());
                    btnRecord.setEnabled(true);
                });
            }
        }).start();
    }

    private void sendToBackend(byte[] wavData) {

        Log.d(TAG, "LOG >>> Preparing to send to backend");
        Log.d(TAG, "LOG >>> Raw WAV byte size=" + wavData.length);

        try {
            String base64 = Base64.encodeToString(wavData, Base64.NO_WRAP);
            Log.d(TAG, "LOG >>> Base64 length=" + base64.length());

            InboundPractice body = new InboundPractice(base64, letter, "audio/wav");

            CoreApi api = ApiClient.get().create(CoreApi.class);

            Log.d(TAG, "LOG >>> Sending Retrofit request to /practice");
            Log.d(TAG, "Sending to URL: " + ApiConfig.BASE_URL + "/practice");

            api.checkPractice(body).enqueue(new Callback<OutboundPractice>() {
                @Override
                public void onResponse(Call<OutboundPractice> call, Response<OutboundPractice> res) {

                    Log.d(TAG, "LOG >>> HTTP Response received. Code=" + res.code());

                    if (!res.isSuccessful() || res.body() == null) {
                        Log.e(TAG, "LOG >>> Server returned error or empty body");
                        practiceResult.setText("Server error " + res.code());
                        return;
                    }

                    OutboundPractice out = res.body();
                    Log.d(TAG, "LOG >>> Server remarks: " + out.getRemarks());

                    // DISPLAY EXACT REMARK, NO PREFIX
                    String returnedText = out.getRemarks();
                    practiceResult.setText(returnedText);

                    // Show BottomSheet Feedback
                    showFeedbackBottomSheet(returnedText);

                    // SPEAK THE RETURNED TEXT
                    if (returnedText != null && !returnedText.isEmpty()) {
                        Log.d(TAG, "LOG >>> Speaking returned text");
                        tts.speak(returnedText, TextToSpeech.QUEUE_FLUSH, null, "tts1");
                    }

                    // Return audio
                    if (out.getAudioB64() != null && !out.getAudioB64().isEmpty()) {
                        Log.d(TAG, "LOG >>> Server returned audio in response");
                        // playReturnedAudio(out.getAudioB64());
                    }
                }

                @Override
                public void onFailure(Call<OutboundPractice> call, Throwable t) {
                    Log.e(TAG, "LOG >>> Retrofit onFailure", t);
                    practiceResult.setText("Network error: " + t.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "LOG >>> Error preparing network request", e);
            practiceResult.setText("Error sending: " + e.getMessage());
        }
    }

    private void playReturnedAudio(String base64Audio) {
        try {
            byte[] audioBytes = Base64.decode(base64Audio, Base64.DEFAULT);

            File tempWav = new File(getCacheDir(), "returned_audio.wav");
            FileOutputStream fos = new FileOutputStream(tempWav);
            fos.write(audioBytes);
            fos.close();

            Log.d(TAG, "LOG >>> Saved returned audio to: " + tempWav.getAbsolutePath());

            MediaPlayer player = new MediaPlayer();
            player.setDataSource(tempWav.getAbsolutePath());
            player.prepare();
            player.start();

            Log.d(TAG, "LOG >>> Playing returned audio");

        } catch (Exception e) {
            Log.e(TAG, "LOG >>> Failed to play returned audio", e);
        }
    }

    private byte[] pcmToWav(byte[] pcmData, int sampleRate) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(out);

            int totalDataLen = pcmData.length + 36;
            int byteRate = sampleRate * 2;

            dos.writeBytes("RIFF");
            dos.writeInt(Integer.reverseBytes(totalDataLen));
            dos.writeBytes("WAVE");
            dos.writeBytes("fmt ");
            dos.writeInt(Integer.reverseBytes(16));
            dos.writeShort(Short.reverseBytes((short) 1));
            dos.writeShort(Short.reverseBytes((short) 1));
            dos.writeInt(Integer.reverseBytes(sampleRate));
            dos.writeInt(Integer.reverseBytes(byteRate));
            dos.writeShort(Short.reverseBytes((short) 2));
            dos.writeShort(Short.reverseBytes((short) 16));
            dos.writeBytes("data");
            dos.writeInt(Integer.reverseBytes(pcmData.length));
            dos.write(pcmData);

            return out.toByteArray();

        } catch (Exception e) {
            Log.e(TAG, "LOG >>> pcmToWav error", e);
            return pcmData;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown(); // ADDED
        }
    }

    private void showFeedbackBottomSheet(String message) {
        if (message == null)
            return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_feedback, null);
        bottomSheetDialog.setContentView(view);

        TextView title = view.findViewById(R.id.feedbackTitle);
        TextView msg = view.findViewById(R.id.feedbackMessage);
        MaterialButton btnGotIt = view.findViewById(R.id.btnGotIt);

        msg.setText(message);

        int colorRes;
        String titleText;

        if (message.contains("Excellent")) {
            colorRes = R.color.duo_light_green;
            titleText = "Excellent!";
        } else if (message.contains("Good job")) {
            colorRes = R.color.duo_light_green;
            titleText = "Good Job!";
        } else if (message.contains("Okay")) {
            colorRes = R.color.duo_orange;
            titleText = "Okay";
        } else {
            // Default to Red for "Try again" or "Try harder"
            colorRes = R.color.duo_red;
            titleText = "Incorrect";
        }

        title.setText(titleText);
        title.setTextColor(ContextCompat.getColor(this, colorRes));
        btnGotIt.setBackgroundTintList(ContextCompat.getColorStateList(this, colorRes));

        btnGotIt.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }
}
