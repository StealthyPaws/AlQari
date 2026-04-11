package com.example.al_qari;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.*;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.content.res.AssetFileDescriptor;


import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AudioStreamerService extends Service implements RecognitionListener {
    public static AudioStreamerService instance;
    private static final String TAG = "AudioStreamerService";
    private static final String CHANNEL_ID = "AudioStreamerChannel";
    private static final int SAMPLE_RATE = 16000;

    private SpeechService speechService;
    private Model voskModel;
    private AudioRecord recorder;

    private MediaPlayer fillerPlayer;
    private final AtomicBoolean isRecording = new AtomicBoolean(false);
    private final AtomicBoolean isUserSpeaking = new AtomicBoolean(false);
    private final ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private final AtomicBoolean isProcessingCommand = new AtomicBoolean(false);
    private final AtomicBoolean isFillerPlaying = new AtomicBoolean(false);

    private CoreApi api;

    @Override
    public void onCreate() {
        super.onCreate();
        api = ApiClient.get().create(CoreApi.class);
        createNotificationChannel();

        Log.i(TAG, "✅ AudioStreamerService created.");

        // Load the Vosk model asynchronously
        StorageService.unpack(this, "vosk-model-small-en-us-0.15", "model",
                (model) -> {
                    voskModel = model;
                    Log.i(TAG, "✅ Vosk model loaded successfully.");
                    startVoskListening();
                },
                (exception) -> Log.e(TAG, "❌ Failed to unpack Vosk model: " + exception.getMessage())
        );

        instance = this;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Al-Qari Mic Listener", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void startVoskListening() {
        try {
            Recognizer recognizer = new Recognizer(voskModel, SAMPLE_RATE);
            speechService = new SpeechService(recognizer, SAMPLE_RATE);
            speechService.startListening(this);
            Log.i(TAG, "🎧 Vosk started listening for 'start' and 'stop' keywords.");
        } catch (IOException e) {
            Log.e(TAG, "❌ Error starting Vosk listening: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Al-Qari is Listening")
                .setContentText("Listening for your commands…")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
        startRecordingLoop();
        return START_NOT_STICKY;
    }

    /* 🎙️ Start mic recording */
    private void startRecordingLoop() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission not granted", Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }

        int minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, minBuf);
            recorder.startRecording();
            isRecording.set(true);

            new Thread(() -> {
                byte[] buf = new byte[minBuf];
                while (isRecording.get()) {
                    int read = recorder.read(buf, 0, buf.length);
                    if (read > 0 && isUserSpeaking.get()) {
                        synchronized (audioBuffer) {
                            audioBuffer.write(buf, 0, read);
                        }
                    }
                }
            }, "AudioStreamerMicLoop").start();

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start mic: " + e.getMessage());
            stopSelf();
        }
    }

    /* 🗣️ Handle recognized keywords */
    @Override
    public void onResult(String hypothesis) {
        Log.d(TAG, "🔊 Partial result: " + hypothesis);

        // 🚫 If still processing, only show message if user tries to start again
        if (isProcessingCommand.get() && hypothesis.contains("start")) {
            Log.w(TAG, "⚠️ Ignored — system still processing last command.");
            Toast.makeText(getApplicationContext(),
                    "Please wait — previous command is being processed.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hypothesis.contains("start")) {
            onStartKeywordDetected();
        } else if (hypothesis.contains("stop")) {
            onStopKeywordDetected();
        }
    }

    @Override public void onFinalResult(String hypothesis) { Log.d(TAG, "✅ Final result: " + hypothesis); }
    @Override public void onPartialResult(String hypothesis) {}
    @Override public void onError(Exception e) { Log.e(TAG, "❌ Recognition error: " + e.getMessage()); }
    @Override public void onTimeout() { Log.i(TAG, "⏰ Vosk timeout."); }

    /* 🔹 Start and Stop handlers */
    public void onStartKeywordDetected() {
        if (isProcessingCommand.get()) {
            Log.w(TAG, "⚠️ Ignored — still processing previous command.");
            Toast.makeText(getApplicationContext(),
                    "Please wait — previous command is being processed.", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "🎬 'start' detected — begin recording user speech.");
        isUserSpeaking.set(true);
        synchronized (audioBuffer) {
            audioBuffer.reset();
        }
    }

    public void onStopKeywordDetected() {
        if (isProcessingCommand.get()) {
            Log.w(TAG, "⚠️ Ignored — still processing previous command.");
            return;
        }

        Log.i(TAG, "🛑 'stop' detected — sending audio to backend.");
        isUserSpeaking.set(false);
        isProcessingCommand.set(true); // block further commands until done

        byte[] pcmData;
        synchronized (audioBuffer) {
            pcmData = audioBuffer.toByteArray();
        }

        // Trim last 2 seconds (to remove "stop" word)
        int bytesPerSecond = SAMPLE_RATE * 2;
        int trimBytes = (int) (2 * bytesPerSecond);
        if (pcmData.length > trimBytes) {
            byte[] trimmed = new byte[pcmData.length - trimBytes];
            System.arraycopy(pcmData, 0, trimmed, 0, trimmed.length);
            pcmData = trimmed;
            Log.i(TAG, "✂️ Trimmed last 2s of audio.");
        }

        if (pcmData.length > 0) {
            byte[] wavData = pcm16ToWav(pcmData, SAMPLE_RATE);
            sendFullAudio(wavData);
        } else {
            Log.w(TAG, "⚠️ No audio recorded between start and stop.");
            isProcessingCommand.set(false);
        }

        synchronized (audioBuffer) {
            audioBuffer.reset();
        }
    }

    /* 🎧 Convert PCM → WAV */
    private byte[] pcm16ToWav(byte[] pcmData, int sampleRate) {
        int totalDataLen = pcmData.length + 36;
        int byteRate = sampleRate * 2;
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (DataOutputStream dos = new DataOutputStream(out)) {
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
        } catch (IOException e) {
            Log.e(TAG, "❌ Error converting PCM to WAV: " + e.getMessage());
        }

        return out.toByteArray();
    }

    /* 🚀 Send audio to backend via /intent */
    /* 🚀 Send audio to backend via /intent */
    private void sendFullAudio(byte[] wavData) {
        String base64Audio = Base64.encodeToString(wavData, Base64.NO_WRAP);
        Inbound body = new Inbound(base64Audio, "audio/wav");

        // 🎵 Start playing a random filler while waiting for backend
        playRandomFiller();

        api.classifyIntent(body).enqueue(new Callback<Outbound>() {
            @Override
            public void onResponse(Call<Outbound> call, Response<Outbound> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Log.w(TAG, "⚠️ Unsuccessful response: " + response.code());
                    isProcessingCommand.set(false);
                    return;
                }

                Outbound outbound = response.body();
                String intent = outbound.getIntent();
                String audioB64 = outbound.getAudioB64();

                Log.i(TAG, "✅ Intent recognized: " + intent);

                // ⏳ Wait until filler finishes before proceeding
                new Thread(() -> {
                    while (isFillerPlaying.get()) {
                        try { Thread.sleep(200); } catch (InterruptedException ignored) {}
                    }

                    if (audioB64 != null && !audioB64.isEmpty()) {
                        playReceivedAudio(audioB64, intent);
                    } else {
                        Log.i(TAG, "ℹ️ No audio in response — executing intent after filler.");
                        if (intent != null && !intent.isEmpty()) {
                            IntentRouter.route(getApplicationContext(), intent);
                        }
                        isProcessingCommand.set(false);
                    }
                }).start();
            }

            @Override
            public void onFailure(Call<Outbound> call, Throwable t) {
                Log.e(TAG, "❌ Backend error: " + t.getMessage());
                isProcessingCommand.set(false);
            }
        });
    }

    /* 🎶 Play random filler while waiting for backend */
    private void playRandomFiller() {
        if (isFillerPlaying.get()) return; // already playing one

        try {
            int randomIndex = (int) (Math.random() * 10) + 1;
            String fillerPath = "filler/filler/english1/filler_" + randomIndex + ".wav";

            fillerPlayer = new MediaPlayer();
            AssetFileDescriptor afd = getAssets().openFd(fillerPath);
            fillerPlayer.setDataSource(afd);
            afd.close();

            fillerPlayer.setOnCompletionListener(mp -> {
                isFillerPlaying.set(false);
                mp.release();
                Log.i(TAG, "🎵 Filler audio finished.");
            });

            fillerPlayer.prepare();
            fillerPlayer.start();
            isFillerPlaying.set(true);
            Log.i(TAG, "🎧 Playing filler: " + fillerPath);

        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to play filler: " + e.getMessage());
            isFillerPlaying.set(false);
        }
    }




    /* 🔊 Play received audio, then execute intent */
    private void playReceivedAudio(String audioB64, String intent) {
        try {
            byte[] audioBytes = Base64.decode(audioB64, Base64.DEFAULT);
            File tempAudio = new File(getCacheDir(), "response.wav");
            try (FileOutputStream fos = new FileOutputStream(tempAudio)) {
                fos.write(audioBytes);
            }

            MediaPlayer player = new MediaPlayer();
            player.setDataSource(tempAudio.getAbsolutePath());
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);

            player.setOnCompletionListener(mp -> {
                mp.release();
                Log.i(TAG, "🎵 Finished playing response audio.");

                if (intent != null && !intent.isEmpty()) {
                    IntentRouter.route(getApplicationContext(), intent);
                }

                isProcessingCommand.set(false); // ✅ Allow new command now
            });

            player.prepare();
            player.start();
            Log.i(TAG, "🎧 Playing received audio...");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to play received audio: " + e.getMessage());
            isProcessingCommand.set(false);
        }
    }

    public void pauseMic() {
        try {
            isUserSpeaking.set(false);
            isRecording.set(false);
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "pauseMic error", e);
        }
    }

    public void resumeMic() {
        try {
            startRecordingLoop();
        } catch (Exception e) {
            Log.e(TAG, "resumeMic error", e);
        }
    }


    /* 🧹 Cleanup */
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(TAG, "🛑 App removed from recents — stopping service.");
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "🧹 Service destroyed.");
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
        isRecording.set(false);
        if (recorder != null) {
            recorder.stop();
            recorder.release();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}