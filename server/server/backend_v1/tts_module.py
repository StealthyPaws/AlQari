# tts_module.py
import torch
from transformers import AutoProcessor, SeamlessM4Tv2Model
from pathlib import Path
import soundfile as sf

# =========================================================
# CONFIG
# =========================================================
MODEL_NAME = "facebook/seamless-m4t-v2-large"
DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

# create output folder
OUT_DIR = Path(r"/content/drive/MyDrive/server/backend_v1/FYP/intent_classifier_pipeline/tts_outputs")
OUT_DIR.mkdir(exist_ok=True)

# load once
print("[TTS] Loading Seamless M4T for speech synthesis...")
processor = AutoProcessor.from_pretrained(MODEL_NAME)
model = SeamlessM4Tv2Model.from_pretrained(MODEL_NAME).to(DEVICE)


# =========================================================
# MAIN FUNCTION
# =========================================================
def synthesize_speech(text: str, target_lang: str = "urd"):
    """
    Generates speech audio from text using Seamless M4T.
    target_lang: 'eng' or 'urd'
    Returns path to generated .wav file
    """
    inputs = processor(text=text, src_lang="eng", return_tensors="pt").to(DEVICE)
    with torch.no_grad():
        audio_out = model.generate(**inputs, tgt_lang=target_lang, generate_speech=True)

    # decode and save
    audio = audio_out[0].cpu().numpy().squeeze()
    out_path = OUT_DIR / f"response_{target_lang}.wav"
    sf.write(out_path, audio, 16000)
    print(f"[TTS] Saved speech to: {out_path}")
    return out_path
