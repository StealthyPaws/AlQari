# seamless_stt.py
from transformers import pipeline
import sounddevice as sd
import numpy as np
import time
from scipy import signal
import queue, threading
import torchaudio
import torch

# =========================================================
# GLOBAL MODEL INITIALIZATION (loaded only once)
# =========================================================
print("🚀 Loading Seamless M4T  model globally...")

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
MODEL_NAME = "facebook/seamless-m4t-v2-large"
DTYPE = torch.float16 if torch.cuda.is_available() else torch.float32

# Create a global pipeline (shared across all calls)
asr_pipe = pipeline(
    task="automatic-speech-recognition",
    model=MODEL_NAME,
    dtype=DTYPE,
    device=DEVICE
)

print(f"✅ Seamless M4T ready on {DEVICE} (dtype={DTYPE})")


# =========================================================
# CLASS: SeamlessStream (reuses global pipeline)
# =========================================================
class SeamlessStream:
    def __init__(self, lang="eng", chunk_duration=5):
        self.pipe = asr_pipe  # reuse global instance
        self.lang = lang      # default target language
        self.chunk_duration = chunk_duration
        self.sample_rate = 16000
        self.audio_queue = queue.Queue()
        self.running = False

    # --- Audio recording ---
    def record_audio(self, duration=None):
        duration = duration or self.chunk_duration
        audio = sd.rec(int(duration * self.sample_rate),
                       samplerate=self.sample_rate,
                       channels=1,
                       dtype="float32")
        sd.wait()
        return np.squeeze(audio)

    def is_silent(self, audio, energy_thresh=0.002, speech_ratio_thresh=0.05):
        rms = np.sqrt(np.mean(audio**2))
        speech_ratio = np.mean(np.abs(audio) > energy_thresh)
        return rms < energy_thresh or speech_ratio < speech_ratio_thresh

    def light_denoise(self, audio, noise_reduction=0.2):
        f, t, Zxx = signal.stft(audio, fs=self.sample_rate, nperseg=512)
        mag, phase = np.abs(Zxx), np.angle(Zxx)
        noise_floor = np.percentile(mag, 10, axis=1, keepdims=True)
        mag_denoised = np.maximum(mag - noise_reduction * noise_floor, 0)
        _, clean_audio = signal.istft(mag_denoised * np.exp(1j * phase),
                                      fs=self.sample_rate,
                                      nperseg=512)
        return np.float32(clean_audio[:len(audio)])

    def clean_text(self, text):
        parts = text.split()
        if len(parts) > 6:
            half = len(parts) // 2
            if " ".join(parts[:half]) == " ".join(parts[half:]):
                return " ".join(parts[:half])
        unique_tokens = []
        for w in parts:
            if not (len(unique_tokens) > 1 and unique_tokens[-1] == w):
                unique_tokens.append(w)
        return " ".join(unique_tokens)

    # --- Worker loop ---
    def _listen_worker(self, callback):
        while self.running:
            audio = self.record_audio()
            if self.is_silent(audio):
                continue
            denoised = self.light_denoise(audio)
            start = time.time()

            # ✅ specify target language to prevent M4T warning
            result = self.pipe(
                denoised,
                generate_kwargs={"tgt_lang": self.lang}
            )
            text = self.clean_text(result["text"])
            detected_lang = result.get("language", self.lang)

            latency = time.time() - start
            print(f"[🎤] ({detected_lang}) {text} ({latency:.2f}s)")

            callback({"text": text, "language": detected_lang})

    def start_stream(self, callback):
        if self.running:
            print("Already running.")
            return
        self.running = True
        t = threading.Thread(target=self._listen_worker, args=(callback,), daemon=True)
        t.start()
        print("🎧 Listening stream started.")

    def stop_stream(self):
        self.running = False
        print("🛑 Stopped listening stream.")


# =========================================================
# FUNCTION: transcribe_audio (uses global model)
# =========================================================
def transcribe_audio(audio_path: str, return_lang: bool = False):
    print(f"[STT] Loading audio: {audio_path}")
    waveform, sample_rate = torchaudio.load(audio_path)

    # auto-detect: choose English unless Urdu filename is detected
    tgt_lang = "urd" if "urdu" in audio_path.lower() else "eng"

    start = time.time()
    # ✅ explicitly specify tgt_lang to fix warning
    result = asr_pipe(waveform, generate_kwargs={"tgt_lang": tgt_lang})
    latency = round(time.time() - start, 2)

    text = result["text"]
    detected_lang = result.get("language", tgt_lang)

    print(f"[STT] ({detected_lang}) '{text}' ({latency}s)")

    if return_lang:
        return text, detected_lang
    else:
        return {"text": text, "language": detected_lang}


# =========================================================
# EXAMPLE USAGE
# =========================================================
if __name__ == "__main__":
    def print_chunk(text):
        print(f"📝 Text chunk ready: {text}")

    stream = SeamlessStream(lang="eng")  # you can change to "urd" here if needed
    stream.start_stream(print_chunk)
    try:
        while True:
            time.sleep(1)
    except KeyboardInterrupt:
        stream.stop_stream()
