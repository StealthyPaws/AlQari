# stt.py
import torch
import torchaudio
from transformers import pipeline
from pyannote.audio import Pipeline as VADPipeline
from pyannote.audio import Audio
from pyannote.core import Segment

# =========================================================
# MODEL INITIALIZATION
# =========================================================
print("🚀 Loading Seamless M4T model for STT...")
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
MODEL_NAME = "facebook/seamless-m4t-v2-large"
DTYPE = torch.float16 if torch.cuda.is_available() else torch.float32

# Load ASR model once globally
asr_pipe = pipeline(
    task="automatic-speech-recognition",
    model=MODEL_NAME,
    dtype=DTYPE,
    device=DEVICE
)

# Load VAD model
print("🔊 Loading Pyannote VAD pipeline...")
vad_pipeline = VADPipeline.from_pretrained("pyannote/voice-activity-detection")
audio_loader = Audio(sample_rate=16000)

print(f"✅ Models ready (STT on {DEVICE}, dtype={DTYPE})")

# =========================================================
# FUNCTION: Speech-to-Text with VAD
# =========================================================
def transcribe_audio(wav_path: str, lang: str = "eng"):
    """
    Performs VAD + STT using Seamless M4T v2-large.
    Returns combined transcription text.
    """
    print(f"\n[INFO] Processing file: {wav_path}")
    # --- Step 1: Run Voice Activity Detection ---
    speech_segments = vad_pipeline(wav_path)
    print(f"[VAD] Detected {len(speech_segments)} speech segments")

    all_text = []

    # --- Step 2: Loop through speech segments and transcribe ---
    for i, segment in enumerate(speech_segments.get_timeline()):
        print(f"[VAD] Segment {i+1}: {segment.start:.2f}s → {segment.end:.2f}s")
        waveform, sample_rate = audio_loader.crop(wav_path, segment)

        # Resample if necessary
        if sample_rate != 16000:
            waveform = torchaudio.functional.resample(waveform, sample_rate, 16000)

        # Transcribe segment
        try:
            result = asr_pipe(waveform, generate_kwargs={"tgt_lang": lang})
            text = result.get("text", "").strip()
            print(f"[STT] Segment {i+1} → {text}")
            all_text.append(text)
        except Exception as e:
            print(f"[ERROR] Segment {i+1} failed: {e}")

    final_text = " ".join(all_text)
    print(f"\n📝 Final Transcription:\n{final_text}\n")
    return final_text
