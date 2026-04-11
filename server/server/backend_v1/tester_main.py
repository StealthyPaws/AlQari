# import time
# import json
# from pathlib import Path

# from seamless_stt import transcribe_audio
# from tts_module import synthesize_speech
# from llm import predict  # ← use Gemini-based LLM for all NLP steps

# # ---------------- CONFIG ----------------
# AUDIO_DIR = Path(r"C:\Users\User\Downloads\FYP")
# CONFIRM_THRESHOLD = 0.65  # Gemini gives confidence in [0,1]

# USE_STT = True


# # ---------------- MAIN PIPELINE ----------------
# def run_pipeline(audio_path: str = None, input_text: str = None):
#     results = {}

#     # Initialize timing
#     stt_time = llm_time = tts_time = 0.0

#     # ----- STEP 1: STT -----
#     if audio_path and USE_STT:
#         print("\n[Pipeline] Step 1: Speech-to-Text (Seamless STT)")
#         t0 = time.time()
#         text = transcribe_audio(audio_path)
#         stt_time = round(time.time() - t0, 3)
#         print(f"[Pipeline] Transcribed: '{text}' | Time: {stt_time}s")
#     else:
#         text = input_text or ""
#         stt_time = 0.0

#     results["raw_text"] = text
#     results["stt_time"] = stt_time

#     # ----- STEP 2: LLM (Normalization + Intent) -----
#     print("\n[Pipeline] Step 2: Gemini LLM — Normalization + Intent Prediction")
#     t1 = time.time()
#     llm_output = predict(text)
#     llm_time = round(time.time() - t1, 3)

#     normalized = llm_output.get("normalized_text", text)
#     intent = llm_output.get("intent", "unknown")
#     conf = float(llm_output.get("confidence", 0.0))
#     print(f"[Pipeline] Normalized: '{normalized}' | Intent: {intent} | Confidence: {conf:.2f}")

#     results.update({
#         "normalized_text": normalized,
#         "intent": intent,
#         "confidence": conf,
#         "llm_time": llm_time
#     })
#     print(intent)
#     # ----- STEP 3: CONFIDENCE CHECK -----
#     if conf >= CONFIRM_THRESHOLD:
#         response_text = f"Okay, I’m navigating to {intent.replace('_', ' ')}."
#         print(f"[Pipeline] Response: {response_text}")
#         t2 = time.time()
#         synthesize_speech(response_text)
#         tts_time = round(time.time() - t2, 3)
#         results["response"] = response_text
#     # elif intent == "irrelevant":
#     #     return {"intent" : "irrelevant", "wavfileresponse":"irrelevant command sorry cannot answer.wav"}

#     else:
#         print(f"[Pipeline] Low confidence ({conf:.2f}). Asking for confirmation.")
#         confirm_prompt = f"Do you want me to execute {intent.replace('_', ' ')}?"
#         synthesize_speech(confirm_prompt)
#         print(f"🔊 {confirm_prompt}")

#         user_reply = input("Your response (yes/no): ").lower().strip()
#         if user_reply in ["yes", "y", "sure", "ok"]:
#             response_text = f"Okay, I’m navigating to {intent.replace('_', ' ')}."
#             synthesize_speech(response_text)
#             print(f"🔊 {response_text}")
#             results["response"] = response_text
#         else:
#             synthesize_speech("Alright, please tell me again what you want to do.")
#             print("🔊 Alright, please tell me again what you want to do.")
#             new_text = input("Enter again: ").strip()
#             return run_pipeline(input_text=new_text)

#     # ----- STEP 4: TIME SUMMARY -----
#     total_time = stt_time + llm_time + tts_time
#     print("\n=== ⏱ TIME SUMMARY ===")
#     print(f"STT time:   {stt_time}s")
#     print(f"LLM time:   {llm_time}s")
#     print(f"TTS time:   {tts_time}s")
#     print(f"--------------------------")
#     print(f"Total time: {round(total_time, 3)}s")

#     results.update({
#         "tts_time": tts_time,
#         "total_time": total_time
#     })

#     print("\n✅ [Pipeline Complete]")
#     return results


# # ---------------- MAIN ENTRY ----------------
# if __name__ == "__main__":
#     print("=== AI Qari LLM-Driven Unified Pipeline ===")

#     mode = input("Choose mode (1=Audio file, 2=Manual text): ").strip()
#     if mode == "1":
#         audio_file = input("Enter audio filename (inside audio_inputs folder): ").strip()
#         audio_path = AUDIO_DIR / audio_file
#         if not audio_path.exists():
#             print(f"❌ File not found: {audio_path}")
#         else:
#             out = run_pipeline(audio_path=str(audio_path))
#             print("\n--- Pipeline Output ---")
#             for k, v in out.items():
#                 print(f"{k}: {v}")
#     else:
#         text = input("Enter text manually: ").strip()
#         out = run_pipeline(input_text=text)
#         print("\n--- Pipeline Output ---")
#         for k, v in out.items():
#             print(f"{k}: {v}")
import base64
import tempfile
from pathlib import Path
import time
import os
import random

from seamless_stt import transcribe_audio  # must return (text, detected_lang)
from tts_module import synthesize_speech
from llm import predict  # Gemini-based LLM

# =========================================================
# CONFIG
# =========================================================
CONFIRM_THRESHOLD = 0.65
TMP_DIR = Path("temp_audio")
TMP_DIR.mkdir(exist_ok=True)

# path to pre-recorded responses
AUDIO_RESP_DIR = Path(r"C:\Users\User\Downloads\FYP\backend_v1\audio_responses")
URDU_DIR = AUDIO_RESP_DIR / "urdu"
ENGLISH_DIR = AUDIO_RESP_DIR / "english"


# =========================================================
# Helper to get random pre-recorded response
# =========================================================
def get_random_response_file(lang: str, category: str) -> str:
    """
    Picks a random .wav file from /audio_responses/{lang}/{category}_*.wav
    lang: 'eng' or 'urd'
    category: 'irrelevant' or 'repeat'
    """
    folder = ENGLISH_DIR if lang == "eng" else URDU_DIR
    pattern = f"{category}_"
    files = [f for f in folder.glob(f"{pattern}*.wav")]
    if not files:
        raise FileNotFoundError(f"No prerecorded {category} files found for {lang}")
    chosen = random.choice(files)
    print(f"[AudioResponse] Selected: {chosen}")
    return str(chosen)


# =========================================================
# Response handler
# =========================================================
def handle_response(intent: str, conf: float, text: str, target_lang: str):
    """
    Handles response generation:
    - irrelevant → pre-recorded .wav
    - low confidence → pre-recorded .wav
    - valid intent → runtime TTS
    """
    print(f"[Handler] Intent={intent} | Conf={conf:.2f} | Lang={target_lang}")

    # --------------- Case 1: Irrelevant ---------------
    if intent == "irrelevant":
        wav_path = get_random_response_file(target_lang, "irrelevant")
        return {"intent": "irrelevant", "wav_response": wav_path}

    # --------------- Case 2: Low-confidence ---------------
    elif conf < CONFIRM_THRESHOLD:
        wav_path = get_random_response_file(target_lang, "repeat")
        return {"intent": "unclear_repeat", "wav_response": wav_path}

    # --------------- Case 3: Valid Intent ---------------
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
        return {"intent": intent, "wav_response": wav_path}


# =========================================================
# Pipeline
# =========================================================
def run_pipeline(audio_path: str):
    results = {}

    # --- Step 1: STT ---
    print("\n[Pipeline] Step 1: Speech-to-Text (Seamless STT)")
    t0 = time.time()
    text, detected_lang = transcribe_audio(audio_path, return_lang=True)
    stt_time = round(time.time() - t0, 3)
    print(f"[Pipeline] Transcribed: '{text}' | Detected: {detected_lang} | Time: {stt_time}s")

    results["raw_text"] = text
    results["lang"] = detected_lang

    # --- Step 2: Intent classification ---
    print("\n[Pipeline] Step 2: Gemini LLM — Intent Prediction")
    t1 = time.time()
    llm_output = predict(text)
    llm_time = round(time.time() - t1, 3)

    intent = llm_output.get("intent", "unknown")
    conf = float(llm_output.get("confidence", 0.0))
    normalized = llm_output.get("normalized_text", text)

    print(f"[Pipeline] Intent: {intent} | Confidence: {conf:.2f}")

    results.update({
        "normalized_text": normalized,
        "intent": intent,
        "confidence": conf,
        "llm_time": llm_time
    })

    # --- Step 3: Handle response ---
    handled = handle_response(intent, conf, text, detected_lang)
    results.update(handled)

    return results


# =========================================================
# CLI ENTRY
# =========================================================
if __name__ == "__main__":
    print("🎙️ AI Qari Pipeline Tester\n")
    audio_path = input("Enter path to audio file (.wav): ").strip().replace('"', '')

    if not os.path.exists(audio_path):
        print(f"❌ File not found: {audio_path}")
        exit()

    try:
        result = run_pipeline(audio_path)
        print("\n✅ Final Output:")
        for k, v in result.items():
            print(f"  {k}: {v}")
        print(f"\n🎧 Output audio: {result['wav_response']}")
    except Exception as e:
        print(f"\n❌ Error during pipeline: {e}")
