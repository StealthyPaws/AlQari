# android_practice.py
import os
import uuid
import base64
import sqlite3
import torch
import torchaudio
from datetime import datetime
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"
MODEL_NAME = "nrshoudi/wav2vec2-large-xls-r-300m-Arabic-phoneme-based"

# Updated DB name

DB_PATH = r"/content/drive/MyDrive/server/backend_v1/database/qbpractice.db"

TMP_DIR = r"/content/drive/MyDrive/server/backend_v1/tmp_audio"
os.makedirs(TMP_DIR, exist_ok=True)

# -----------------------------------------------------------
# Database
# -----------------------------------------------------------
def init_db():
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    c.execute("""
        CREATE TABLE IF NOT EXISTS makharij_stats (
            key TEXT PRIMARY KEY,
            attempts INTEGER DEFAULT 0,
            last_attempt_time TEXT,
            best_conf REAL DEFAULT 0,
            worst_conf REAL DEFAULT 1,
            last_conf REAL DEFAULT 0
        );
    """)
    conn.commit()
    conn.close()


def update_db(makraj_key, conf, correct):
    conn = sqlite3.connect(DB_PATH)
    c = conn.cursor()
    now = datetime.now().isoformat()

    c.execute("SELECT best_conf, worst_conf FROM makharij_stats WHERE key = ?", (makraj_key,))
    row = c.fetchone()
    old_best, old_worst = row if row else (0.0, 1.0)

    if correct:
        new_best = max(old_best, conf)
        new_worst = min(old_worst, conf)
    else:
        new_best, new_worst = old_best, old_worst

    c.execute("""
        INSERT INTO makharij_stats (key, attempts, last_attempt_time, best_conf, worst_conf, last_conf)
        VALUES (?, 1, ?, ?, ?, ?)
        ON CONFLICT(key) DO UPDATE SET
            attempts = attempts + 1,
            last_attempt_time = excluded.last_attempt_time,
            best_conf = ?,
            worst_conf = ?,
            last_conf = excluded.last_conf;
    """, (makraj_key, now, new_best, new_worst, conf, new_best, new_worst))

    conn.commit()
    conn.close()


# -----------------------------------------------------------
# Audio processing
# -----------------------------------------------------------
def load_and_preprocess_audio(file_path, target_sr=16000):
    waveform, sr = torchaudio.load(file_path)
    if waveform.shape[0] > 1:
        waveform = waveform.mean(dim=0, keepdim=True)
    waveform = torchaudio.functional.resample(waveform, sr, target_sr)
    waveform = waveform / waveform.abs().max()
    return waveform.squeeze(0).numpy(), target_sr


# -----------------------------------------------------------
# Load model once
# -----------------------------------------------------------
print("Loading Arabic Wav2Vec2 model...")
processor = Wav2Vec2Processor.from_pretrained(MODEL_NAME)
model = Wav2Vec2ForCTC.from_pretrained(MODEL_NAME).to(DEVICE)
model.eval()
print("Model loaded.")


# -----------------------------------------------------------
# Prediction
# -----------------------------------------------------------
def predict_phoneme(audio_np, sr=16000):
    inputs = processor(audio_np, sampling_rate=sr, return_tensors="pt").input_values.to(DEVICE)
    with torch.no_grad():
        logits = model(inputs).logits
    probs = torch.softmax(logits, dim=-1)
    confidence = torch.max(probs, dim=-1)[0][0].mean().item()
    predicted_ids = torch.argmax(logits, dim=-1)
    decoded = processor.batch_decode(predicted_ids)[0]
    phoneme = decoded[0] if len(decoded) > 0 else ""
    return phoneme, confidence


# -----------------------------------------------------------
# Feedback
# -----------------------------------------------------------
def feedback(conf, correct):
    if not correct:
        return "Try again, the pronunciation didn't match.", "fail"
    if conf > 0.85:
        return "Excellent! Perfect articulation.", "excellent"
    elif conf > 0.65:
        return "Good job! Getting better.", "good"
    elif conf > 0.45:
        return "Okay, but try to be clearer.", "mediocre"
    else:
        return "Try harder, articulation wasn't clear.", "pass"


# -----------------------------------------------------------
# Evaluate
# -----------------------------------------------------------
def evaluate_attempt(audio_path, ground_truth_phoneme):
    audio, sr = load_and_preprocess_audio(audio_path)
    pred_phoneme, confidence = predict_phoneme(audio, sr)
    correct = (pred_phoneme == ground_truth_phoneme)
    update_db(ground_truth_phoneme, confidence, correct)
    remark, category = feedback(confidence, correct)

    return {
        "predicted_phoneme": pred_phoneme,
        "gt": ground_truth_phoneme,
        "correct": correct,
        "message": remark,
        "category": category
    }


# -----------------------------------------------------------
# Practice handler (called by main.py)
# -----------------------------------------------------------
def process_practice(audio_b64, gt, mime):
    import base64
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    unique_id = uuid.uuid4().hex[:6]

    ext = ".wav"
    tmp_path = os.path.join(TMP_DIR, f"practice_{timestamp}_{unique_id}{ext}")

    # Save incoming audio
    with open(tmp_path, "wb") as f:
        f.write(base64.b64decode(audio_b64))

    # Evaluate attempt
    result = evaluate_attempt(tmp_path, gt)

    # Select prerecorded audio for the returned category
    category = result["category"]
    remarks_dir = "/content/drive/MyDrive/server/backend_v1/remarks"
    audio_path = os.path.join(remarks_dir, f"{category}.wav")

    # Fallback if file missing
    if not os.path.exists(audio_path):
        # If for some reason file not found, default to fail.wav
        audio_path = os.path.join(remarks_dir, "fail.wav")

    # Load prerecorded audio feedback file
    with open(audio_path, "rb") as f:
        audio_bytes = f.read()

    audio_b64_out = base64.b64encode(audio_bytes).decode("utf-8")

    return {
        "remarks": result["message"],       # text feedback
        "audio_b64": audio_b64_out,         # prerecorded audio
        "mime": "audio/wav"
    }

