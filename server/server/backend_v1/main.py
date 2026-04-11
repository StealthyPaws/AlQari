# main.py

from flask import Flask, request, jsonify
import base64
import datetime
import uuid
import os
import threading
from pathlib import Path

from seamless_stt import transcribe_audio
from tts_module import synthesize_speech
from llm import predict

# Import your practice module
import practice


# SQLite DB
from database.quran_db import get_db


# Initialize DB for practice
practice.init_db()

# =========================================================
# CONFIG
# =========================================================
CONFIRM_THRESHOLD = 0.65
TMP_DIR = Path("temp_audio")
TMP_DIR.mkdir(exist_ok=True)

AUDIO_RESP_DIR = Path(r"/content/drive/MyDrive/server/backend_v1/audio_responses")
URDU_DIR = AUDIO_RESP_DIR / "urdu"
ENGLISH_DIR = AUDIO_RESP_DIR / "english"

app = Flask(__name__)

# memorize helper  functions
def get_audio_b64(relative_path):
    try:
        full_path = os.path.join("/content/drive/MyDrive/server/backend_v1", relative_path)

        print("[AUDIO] Full path =", full_path)

        if not os.path.exists(full_path):
            print("[AUDIO] FILE DOES NOT EXIST")
            return None

        with open(full_path, "rb") as f:
            data = f.read()
            return base64.b64encode(data).decode("utf-8")

    except Exception as e:
        print("[AUDIO] Error reading", relative_path, "=>", e)
        return None



# intent helper functions
def get_random_response_file(lang: str, category: str) -> str:
    folder = ENGLISH_DIR if lang == "eng" else URDU_DIR
    pattern = f"{category}_"
    files = [f for f in folder.glob(f"{pattern}*.wav")]
    if not files:
        raise FileNotFoundError(f"No prerecorded {category} files found for {lang}")
    import random
    chosen = random.choice(files)
    print(f"[AudioResponse] Selected: {chosen}")
    return str(chosen)


def handle_response(intent: str, conf: float, text: str, target_lang: str):
    print(f"[Handler] Intent={intent} | Conf={conf:.2f} | Lang={target_lang}")

    if intent == "irrelevant":
        wav_path = get_random_response_file(target_lang, "irrelevant")

    elif conf < CONFIRM_THRESHOLD:
        wav_path = get_random_response_file(target_lang, "repeat")

    else:
        import random
        if target_lang == "eng":
            responses = [
                "Okay, I’m navigating to {}.",
                "Got it, opening {} now.",
                "Sure, taking you to {}.",
                "Alright, moving to {}.",
                "On it, opening {} right away."
            ]
        else:
            responses = [
                "ٹھیک ہے، میں جا رہی ہوں {} کی طرف۔",
                "ہو گیا، ابھی کھول رہی ہوں {}۔",
                "جی بہتر، لے چلتی ہوں {} کی طرف۔",
                "ٹھیک ہے، ابھی دکھاتی ہوں {}۔",
                "بس ایک لمحہ، ابھی لے کر جا رہی ہوں {}۔"
            ]

        response_text = random.choice(responses).format(intent.replace("_", " "))
        wav_path = synthesize_speech(response_text, target_lang)

    with open(wav_path, "rb") as f:
        audio_bytes = f.read()

    audio_b64 = base64.b64encode(audio_bytes).decode("utf-8")
    return {
        "intent": intent,
        "audio_b64": audio_b64,
        "mime": "audio/wav"
    }

@app.route("/intent", methods=["POST"])
def classify_intent():
    data = request.get_json()
    if not data or "audio_b64" not in data:
        return jsonify({"error": "Missing audio_b64"}), 400

    audio_b64 = data["audio_b64"]
    mime = data.get("mime", "audio/wav")
    suffix = ".wav"

    with processing_lock:
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        unique_id = uuid.uuid4().hex[:6]
        filename = f"intent_{timestamp}_{unique_id}{suffix}"
        tmp_path = TMP_DIR / filename

        with open(tmp_path, "wb") as f:
            f.write(base64.b64decode(audio_b64))

        try:
            result = run_pipeline(str(tmp_path))
        except Exception as e:
            print("❌ Pipeline error:", e)
            return jsonify({"error": str(e)}), 500
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)

        return jsonify(result)

@app.route("/practice", methods=["POST"])
def run_practice():
    try:
        data = request.get_json()
        if not data or "audio_b64" not in data or "gt" not in data:
            return jsonify({"error": "audio_b64 and gt required"}), 400

        audio_b64 = data["audio_b64"]
        gt = data["gt"]
        mime = data.get("mime", "audio/wav")

        output = practice.process_practice(audio_b64, gt, mime)

        return jsonify(output)

    except Exception as e:
        print("❌ Practice error:", e)
        return jsonify({"error": "Server error"}), 500


# ================================================
@app.route("/get_ayahs", methods=["GET"])
def get_ayahs():
    surah_id = request.args.get("surah_id", 1, type=int)
    offset = request.args.get("offset", 0, type=int)
    limit = request.args.get("limit", 10, type=int)

    print("\n----------------------")
    print("[API] /get_ayahs called")
    print(f"Requested surah_id = {surah_id}")
    print(f"Offset = {offset}, Limit = {limit}")

    conn = get_db()
    cur = conn.cursor()

    print("[DB] Querying ayahs...")

    cur.execute("""
        SELECT surah_id, verse_number, arabic_indopak, audio_url
        FROM ayahs
        WHERE surah_id = ?
        ORDER BY verse_number ASC
        LIMIT ? OFFSET ?
    """, (surah_id, limit, offset))

    rows = cur.fetchall()

    print(f"[DB] Retrieved {len(rows)} rows")

    ayahs = []
    for row in rows:
        audio_rel = row["audio_url"]   # Example: "Alafasy/mp3/001001.mp3"
        print("[AUDIO] DB audio_url =", audio_rel)

        # Convert audio file to base64
        audio_b64 = get_audio_b64(audio_rel)  
        
        if audio_b64 is None:
            print("[AUDIO] File missing! Returning empty base64 for", audio_rel)
            audio_b64 = ""

        ayahs.append({
            "surah_id": row["surah_id"],
            "verse_number": row["verse_number"],
            "arabic_indopak": row["arabic_indopak"],
            "audio_url": audio_rel,     # Keep original for debugging
            "audio_b64": audio_b64,     # Base64 MP3 data
            "mime": "audio/mp3"
        })

    cur.execute("SELECT COUNT(*) AS total FROM ayahs WHERE surah_id = ?", (surah_id,))
    total = cur.fetchone()["total"]

    print(f"[DB] Total ayahs in Surah {surah_id}: {total}")

    conn.close()
    print("[DB] Connection closed")

    print("[API] Responding with ayahs JSON")
    print("----------------------\n")

    return jsonify({
        "ayahs": ayahs,
        "total": total,
        "offset": offset,
        "limit": limit
    })

@app.route("/get_surah_name", methods=["GET"])
def get_surah_name():
    surah_id = request.args.get("surah_id", 1, type=int)

    print("\n----------------------")
    print("[API] /get_surah_name called")
    print(f"Requested surah_id = {surah_id}")

    conn = get_db()
    cur = conn.cursor()

    print("[DB] Fetching surah name...")

    cur.execute("SELECT name_arabic FROM surahs WHERE surah_id = ?", (surah_id,))
    row = cur.fetchone()

    conn.close()
    print("[DB] Connection closed")

    if row:
        print(f"[DB] Surah found: {row['name_arabic']}")
        print("[API] Responding with surah name")
        print("----------------------\n")
        return jsonify({"name_arabic": row["name_arabic"]})

    print("[DB] Surah NOT found")
    print("[API] Returning 404")
    print("----------------------\n")
    return jsonify({"error": "Surah not found"}), 404



# =========================================================
# INTENT PIPELINE (unchanged)
# =========================================================
processing_lock = threading.Lock()

def run_pipeline(audio_path: str):
    results = {}

    # STT
    print("\n[Pipeline] Step 1: STT")
    text, detected_lang = transcribe_audio(audio_path, return_lang=True)

    results["raw_text"] = text
    results["lang"] = detected_lang

    # LLM
    llm_output = predict(text)
    intent = llm_output.get("intent", "unknown")
    conf = float(llm_output.get("confidence", 0.0))
    normalized = llm_output.get("normalized_text", text)

    results.update({
        "normalized_text": normalized,
        "intent": intent,
        "confidence": conf
    })

    handled = handle_response(intent, conf, text, detected_lang)
    results.update(handled)
    return results

# Entry
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True, use_reloader=False)
