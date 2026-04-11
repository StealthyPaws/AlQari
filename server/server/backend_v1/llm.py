import json
import re
import time
from pathlib import Path
from typing import Dict, Any
import google.generativeai as genai

# ----------------------------
# CONFIG
# ----------------------------
DATA_PATH = r"/content/drive/MyDrive/server/backend_v1/quran_basics_intents_v2_new.json"
MODEL_NAME = "gemini-2.5-pro"  # or "gemini-1.5-flash"
MAX_FEW_SHOTS = 3              # few examples per intent
API_KEY = "AIzaSyDXIgZCKjx06ghW4LWLP4bGhAf-fGgMpOA"  # <-- paste your Gemini API key here
# import google.generativeai as genai

# genai.configure(api_key=API_KEY)

# for m in genai.list_models():
#     print(m.name)

# ----------------------------
# SETUP
# ----------------------------
genai.configure(api_key=API_KEY)

# ----------------------------
# LOAD DATA
# ----------------------------
print("[INFO] Loading dataset...")
with open(DATA_PATH, "r", encoding="utf-8") as f:
    dataset = json.load(f)

# Collect valid intents and few-shot examples
intents = [item["intent"] for item in dataset]
examples = []
for item in dataset:
    for ex in item.get("examples", [])[:MAX_FEW_SHOTS]:
        examples.append({
            "intent": item["intent"],
            "example": ex
        })

print(f"[INFO] Loaded {len(intents)} intents, {len(examples)} examples")

# ----------------------------
# PROMPT BUILDER
# # ----------------------------
# def build_prompt(user_text: str) -> str:
#     few_shots = "\n".join([
#         f"User: {ex['example']}\nNormalized: {ex['example']}\nIntent: {ex['intent']}\n"
#         for ex in examples
#     ])
#     valid_intents = ", ".join([f"'{i}'" for i in intents])
    
#     prompt = f"""
# You are an AI assistant specialized in Qur'anic recitation and Tajweed learning.
# Your task:
# 1. Normalize the given transcription (fix spelling or ASR confusion, e.g., 'hips' → 'hifz', 'jowph' → 'jawf').
# 2. Predict the intent ONLY from this valid intent list: [{valid_intents}].
# 3. Never invent a new intent.
# 4. Respond ONLY with valid JSON in the format below.
# 5. Confidence must be between 0.0 and 1.0.

# Few-shot examples:
# {few_shots}

# Now analyze this input:
# User: {user_text}

# Return your result in this JSON format:
# {{
#   "normalized_text": "...",
#   "intent": "...",
#   "confidence": ...
# }}
# """
#     return prompt.strip()
def build_prompt(user_text: str) -> str:
    few_shots = "\n".join([
        f"User: {ex['example']}\nNormalized: {ex['example']}\nIntent: {ex['intent']}\n"
        for ex in examples
    ])
    valid_intents = ", ".join([f"'{i}'" for i in intents])

    prompt = f"""
You are an AI assistant specialized in Qur'anic recitation, Tajweed learning, and Qur'an memorization support.

Your task:
1. Normalize the given transcription (fix ASR or spelling confusions, e.g., 'hips' → 'hifz', 'jowph' → 'jawf').
2. Predict the intent ONLY from this valid intent list: [{valid_intents}].
3. Never invent a new intent.
4. Respond ONLY with valid JSON in the exact format shown.
5. Confidence must be between 0.0 and 1.0.

Content safety and domain restriction rules:
- If the user input is unrelated to the Qur'an or Tajweed (e.g., songs, jokes, YouTube, general chat, stories, political or sectarian discussions, or sensitive religious topics), classify it as intent = "irrelevant" with confidence = 0.0.
- You must never respond to or execute unrelated commands (like "open YouTube", "play music", "search the web", etc.).
- You must not generate or discuss any sensitive religious, sectarian, or controversial topics (like Sunni/Shia or political issues).
- Your scope is strictly Qur'an-based educational dialogue and pronunciation correction.

Few-shot examples:
{few_shots}

Now analyze this input:
User: {user_text}

Return your result in this JSON format:
{{
  "normalized_text": "...",
  "intent": "...",
  "confidence": ...
}}
"""
    return prompt.strip()

# ----------------------------
# INFERENCE FUNCTION
# ----------------------------
def predict(user_text: str) -> Dict[str, Any]:
    prompt = build_prompt(user_text)
    model = genai.GenerativeModel(MODEL_NAME)
    
    start = time.time()
    response = model.generate_content(prompt)
    end = time.time()
    
    text = response.text if hasattr(response, "text") else str(response)
    
    # Try to extract valid JSON
    try:
        json_str = re.search(r"\{.*\}", text, re.S).group()
        parsed = json.loads(json_str)
    except Exception:
        parsed = {
            "normalized_text": user_text,
            "intent": "unknown",
            "confidence": 0.0
        }

    parsed["inference_time"] = round(end - start, 2)
    return parsed

# ----------------------------
# TEST INTERACTIVELY
# ----------------------------
if __name__ == "__main__":
    print("\n[READY] Type something (or 'exit' to quit):\n")
    while True:
        text = input("User: ").strip()
        if text.lower() in ["exit", "quit"]:
            break
        result = predict(text)
        print(json.dumps(result, indent=2))
