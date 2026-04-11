# main.py
from flask import Flask, request, jsonify
#import mysql.connector
import io
import wave
import numpy as np
import base64
import tempfile
from pathlib import Path
import time
import os
import random
import datetime
import uuid
import threading

from seamless_stt import transcribe_audio  # must return both text and detected_lang
from tts_module import synthesize_speech
from llm import predict  # Gemini-based LLM


# =========================================================
# CONFIG
# =========================================================
CONFIRM_THRESHOLD = 0.65
TMP_DIR = Path("temp_audio")
TMP_DIR.mkdir(exist_ok=True)

# path to pre-recorded responses
AUDIO_RESP_DIR = Path(r"/content/drive/MyDrive/server/backend_v1/audio_responses")
URDU_DIR = AUDIO_RESP_DIR / "urdu"
ENGLISH_DIR = AUDIO_RESP_DIR / "english"

app = Flask(__name__)

# =========================================================
# Helper to get random pre-recorded response
# =========================================================
def get_random_response_file(lang: str, category: str) -> str:
    folder = ENGLISH_DIR if lang == "eng" else URDU_DIR
    pattern = f"{category}_"
    files = [f for f in folder.glob(f"{pattern}*.wav")]
    if not files:
        raise FileNotFoundError(f"No prerecorded {category} files found for {lang}")
    chosen = random.choice(files)
    print(f"[AudioResponse] Selected: {chosen}")
    return str(chosen)


# =========================================================
# Main handler
# =========================================================
def handle_response(intent: str, conf: float, text: str, target_lang: str):
    print(f"[Handler] Intent={intent} | Conf={conf:.2f} | Lang={target_lang}")

    # Case 1: Irrelevant
    if intent == "irrelevant":
        wav_path = get_random_response_file(target_lang, "irrelevant")

    # Case 2: Low-confidence
    elif conf < CONFIRM_THRESHOLD:
        wav_path = get_random_response_file(target_lang, "repeat")

    # Case 3: Valid Intent
    else:
        if target_lang == "eng":
            responses = [
                "Okay, I’m navigating to {}.",
                "Got it, opening {} now.",
                "Sure, taking you to {}.",
                "Alright, moving to {}.",
                "On it! Opening {} right away."
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


# =========================================================
# Pipeline
# =========================================================
def run_pipeline(audio_path: str):
    results = {}

    # Step 1: STT
    print("\n[Pipeline] Step 1: Speech-to-Text (Seamless STT)")
    t0 = time.time()
    text, detected_lang = transcribe_audio(audio_path, return_lang=True)
    stt_time = round(time.time() - t0, 3)
    print(f"[Pipeline] Transcribed: '{text}' | Detected: {detected_lang} | Time: {stt_time}s")

    results["raw_text"] = text
    results["lang"] = detected_lang

    # Step 2: Intent classification
    print("\n[Pipeline] Step 2: Gemini LLM — Intent Prediction")
    t1 = time.time()
    llm_output = predict(text)
    llm_time = round(time.time() - t1, 3)

    intent = llm_output.get("intent", "unknown")
    conf = float(llm_output.get("confidence", 0.0))
    normalized = llm_output.get("normalized_text", text)

    print(f"[Pipeline] Intent: {intent} | Confidence: {conf:.2f} | LLM Time: {llm_time:.2f}")

    results.update({
        "normalized_text": normalized,
        "intent": intent,
        "confidence": conf,
        "llm_time": llm_time
    })

    # Step 3: Response synthesis
    handled = handle_response(intent, conf, text, detected_lang)
    results.update(handled)
    return results


# =========================================================
# Flask endpoint — queued processing
# =========================================================
processing_lock = threading.Lock()

@app.route("/intent", methods=["POST"])
def classify_intent():
    st = time.time()
    print("\n🎯 /intent endpoint hit")

    data = request.get_json()
    if not data or "audio_b64" not in data:
        return jsonify({"error": "Missing audio_b64"}), 400

    # Sequential queue processing
    with processing_lock:
        audio_b64 = data["audio_b64"]
        mime = data.get("mime", "audio/wav")
        suffix = ".wav" if "wav" in mime else ".mp3"
        timestamp = datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
        unique_id = uuid.uuid4().hex[:6]
        filename = f"intent_{timestamp}_{unique_id}{suffix}"
        tmp_path = TMP_DIR / filename

        try:
            with open(tmp_path, "wb") as f:
                f.write(base64.b64decode(audio_b64))
            print(f"✅ Audio saved at: {tmp_path}")
        except Exception as e:
            print(f"❌ Failed to write audio file: {e}")
            return jsonify({"error": "Failed to save audio"}), 500

        print("⏳ Waiting for previous audio to finish (if any)…")
        try:
            result = run_pipeline(str(tmp_path))
            print("\n================ FINAL RESULT ==================")
            for key, value in result.items():
                if key == "audio_b64":
                    print(f"{key}: <BASE64 DATA, {len(value)} chars>")
                else:
                    print(f"{key}: {value}")
            et = time.time()
            print(f"⏱ TOTAL TIME TAKEN: {round(et - st, 2)}s")
            print("================================================\n")
        except Exception as e:
            print(f"❌ Pipeline error: {e}")
            return jsonify({"error": str(e)}), 500
        finally:
            if os.path.exists(tmp_path):
                os.remove(tmp_path)
                print(f"🧹 Deleted temp file: {tmp_path}")

    return jsonify(result)



# =========================================================
# Entry
# =========================================================
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True, use_reloader=False)
