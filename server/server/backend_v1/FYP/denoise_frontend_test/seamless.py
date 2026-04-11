# seamless.py
import torch
from transformers import pipeline

print("🎙️ Loading Seamless M4T (GPU if available)...")
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
DTYPE = torch.float16 if torch.cuda.is_available() else torch.float32

asr_pipe = pipeline(
    "automatic-speech-recognition",
    model="facebook/seamless-m4t-v2-large",
    dtype=DTYPE,
    device=0 if DEVICE == "cuda" else -1
)
print(f"✅ Seamless ready on {DEVICE} ({DTYPE})")

def transcribe_waveform(waveform, lang="eng"):
    """
    Transcribes a single waveform tensor.
    """
    result = asr_pipe(waveform, generate_kwargs={"tgt_lang": lang})
    return result.get("text", "").strip()

