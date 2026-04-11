import os
import time
import sqlite3
import torch
import torchaudio
from datetime import datetime
from transformers import Wav2Vec2ForCTC, Wav2Vec2Processor

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

MODEL_NAME = "nrshoudi/wav2vec2-large-xls-r-300m-Arabic-phoneme-based"


# ---------------------------------------------------------
# 1. Database Setup
# ---------------------------------------------------------
# DB_PATH = "/content/drive/MyDrive/server/Practice/makharij_stats.db"
DB_PATH = os.path.join(os.path.dirname(__file__), "makharij_stats.db")


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

    # Fetch existing record (if any)
    c.execute("SELECT best_conf, worst_conf FROM makharij_stats WHERE key = ?", (makraj_key,))
    row = c.fetchone()

    if row:
        old_best, old_worst = row
    else:
        old_best, old_worst = 0.0, 1.0

    # Decide new best/worst
    if correct:
        new_best = max(old_best, conf)
        new_worst = min(old_worst, conf)
    else:
        # keep old stats untouched
        new_best = old_best
        new_worst = old_worst

    # Insert or update
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


# ---------------------------------------------------------
# 2. Audio Preprocessing
# ---------------------------------------------------------
def load_and_preprocess_audio(file_path, target_sr=16000):
    waveform, sr = torchaudio.load(file_path)

    # make mono
    if waveform.shape[0] > 1:
        waveform = waveform.mean(dim=0, keepdim=True)

    # resample
    waveform = torchaudio.functional.resample(waveform, sr, target_sr)

    # normalize
    waveform = waveform / waveform.abs().max()

    return waveform.squeeze(0).numpy(), target_sr


# ---------------------------------------------------------
# 3. Load Model
# ---------------------------------------------------------
processor = Wav2Vec2Processor.from_pretrained(MODEL_NAME)
model = Wav2Vec2ForCTC.from_pretrained(MODEL_NAME).to(DEVICE)
model.eval()


# ---------------------------------------------------------
# 4. Predict phoneme + confidence
# ---------------------------------------------------------
def predict_phoneme(audio_np, sr=16000):
    inputs = processor(audio_np, sampling_rate=sr,
                       return_tensors="pt", padding="longest").input_values.to(DEVICE)

    with torch.no_grad():
        logits = model(inputs).logits  # shape: (1, seq_len, vocab)
    
    probs = torch.softmax(logits, dim=-1)

    # take max prob for each timestep
    max_probs, max_ids = torch.max(probs, dim=-1)

    # majority vote for phoneme
    # print(max_ids)
    # predicted_id = torch.mode(max_ids[0])[0].item()
    # phoneme = processor.tokenizer.convert_ids_to_tokens(predicted_id)

    # confidence = average max prob across timesteps
    confidence = max_probs[0].mean().item()

    input_values = processor(audio_np, sampling_rate=sr, return_tensors="pt").input_values.to(DEVICE)
    with torch.no_grad():
        logits = model(input_values).logits
    predicted_ids = torch.argmax(logits, dim=-1)
    phoneme = processor.batch_decode(predicted_ids)[0][0]

    return phoneme, confidence


# ---------------------------------------------------------
# 5. Feedback generator
# ---------------------------------------------------------
def feedback(conf, correct):
    """
    correct = True if predicted phoneme matches ground truth
    """
    if not correct:
        return "Try again, the pronunciation didn't match."

    if conf > 0.85:
        return "Excellent! Perfect articulation."
    elif conf > 0.65:
        return "Good job! Getting better."
    elif conf > 0.45:
        return "Okay, but try to be clearer."
    else:
        return "Try harder, articulation wasn't clear."


# ---------------------------------------------------------
# 6. Main evaluate function
# ---------------------------------------------------------
def evaluate_attempt(audio_path, ground_truth_phoneme, makraj_key):
    audio, sr = load_and_preprocess_audio(audio_path)

    pred_phoneme, confidence = predict_phoneme(audio, sr)

    print("Predicted:", pred_phoneme)
    print("Ground truth:", ground_truth_phoneme)
    print("Confidence:", round(confidence, 3))

    correct = (pred_phoneme == ground_truth_phoneme)

    # update DB
    update_db(makraj_key, confidence, correct)

    # generate feedback
    msg = feedback(confidence, correct)
    return {
        "predicted_phoneme": pred_phoneme,
        "ground_truth": ground_truth_phoneme,
        "confidence": confidence,
        "correct": correct,
        "message": msg
    }


# ---------------------------------------------------------
# 7. Example usage
# ---------------------------------------------------------
AUDIO_DIR = r"C:\Users\User\Downloads\FYP\sounds\chunks_labeled"

# List of ground-truth phonemes in correct order
ARABIC_PHONEMES = [
    "ا","ب","ت","ث","ج","ح","خ","د","ذ","ر","ز","س","ش","ص","ض","ط","ظ","ع","غ",
    "ف","ق","ك","ل","م","ن","ه","و","ي"
]


def run_batch_test():
    print("\n=== Running Batch Phoneme Test ===\n")

    files = sorted(os.listdir(AUDIO_DIR))   # ensures alphabetical order
    results = []

    for idx, file in enumerate(files):
        if not file.lower().endswith((".mp3", ".wav")):
            continue

        # ground truth based on alphabet order
        if idx >= len(ARABIC_PHONEMES):
            print(f"⚠ Warning: No GT label for file {file}, skipping.")
            continue

        gt = ARABIC_PHONEMES[idx]
        makraj_key = f"phoneme_{gt}"

        path = os.path.join(AUDIO_DIR, file)

        print(f"\nProcessing: {file}")
        print(f"GT Label: {gt}")

        result = evaluate_attempt(
            audio_path=path,
            ground_truth_phoneme=gt,
            makraj_key=makraj_key
        )

        print("Result →", result)
        results.append(result)

    print("\n=== Batch Test Complete ===")
    return results


if __name__ == "__main__":
    init_db()

    mode = input("Choose mode: (1) Single Test  (2) Batch Test: ")

    if mode.strip() == "1":
        print("\n=== Running Single Test ===\n")
        result = evaluate_attempt(
            audio_path=r"C:\Users\User\Downloads\FYP\sounds\chunks_labeled\02_baa.mp3",
            ground_truth_phoneme="ب",
            makraj_key="baa"
        )
        print(result)

    elif mode.strip() == "2":
        run_batch_test()

    else:
        print("Invalid choice.")
