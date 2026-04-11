#memorization.py
import torch
import sounddevice as sd
import numpy as np
from queue import Queue
import threading
import regex as re
import editdistance
from transformers import WhisperProcessor, WhisperForConditionalGeneration

# ------------------ Device ------------------
device = "cuda" if torch.cuda.is_available() else "cpu"

# ------------------ Whisper ------------------
MODEL_NAME = "tarteel-ai/whisper-base-ar-quran"
processor = WhisperProcessor.from_pretrained(MODEL_NAME)
model = WhisperForConditionalGeneration.from_pretrained(MODEL_NAME).to(device).eval()

# ------------------ Audio ------------------
SR = 16000
CHUNK = 10000
MIN_SPEECH = SR * 1.5
ENERGY_THRESHOLD = 0.01

audio_queue = Queue()
buffer = []

# ------------------ Surah Text ------------------
SURAH_IHKLAS = """
قُلْ هُوَ اللَّهُ أَحَدٌ
اللَّهُ الصَّمَدُ
لَمْ يَلِدْ وَلَمْ يُولَدْ
وَلَمْ يَكُنْ لَهُ كُفُوًا أَحَدٌ
"""

# ------------------ Arabic Normalization ------------------
def normalize_arabic(text):
    text = re.sub(r'[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED]', '', text)
    text = text.replace('إ','ا').replace('أ','ا').replace('آ','ا').replace('ٱ','ا')
    text = text.replace('ى','ي').replace('ئ','ي').replace('ؤ','و').replace('ة','ه')
    text = text.replace('ـ','')
    text = re.sub(r'\s+', ' ', text).strip()
    return text

def tokenize_arabic(text):
    normalized = normalize_arabic(text)
    return normalized.split()

expected_words = tokenize_arabic(SURAH_IHKLAS)

# ------------------ Alignment Engine ------------------
MAX_DIST = 2
NEIGHBOR_WINDOW = 2

class InteractiveToleranceAlignment:
    def _init_(self, expected_words, tolerance="hard", ayah_starts=None, translation_dict=None):
        """
        tolerance: "easy", "intermediate", "hard"
        ayah_starts: list of indices where each ayah starts (for hard mode)
        translation_dict: dict mapping normalized arabic word -> translation
        """
        self.expected_words = expected_words
        self.current_idx = 0
        self.results = []
        self.tolerance = tolerance
        self.ayah_starts = ayah_starts if ayah_starts else [0]
        self.translation_dict = translation_dict if translation_dict else {}
        self.hard_attempts = 0

    def align(self, recognized_words):
        for word in recognized_words:
            if self.current_idx >= len(self.expected_words):
                self.results.append(("EXTRA", word))
                print(f"❌ Extra word: '{word}'")
                continue

            expected_word = self.expected_words[self.current_idx]
            dist = editdistance.eval(word, expected_word)

            # ---------------- Exact/close match ----------------
            if dist <= MAX_DIST:
                self.results.append(("OK", word))
                print(f"✅ OK: '{word}'")
                self.current_idx += 1
                continue

            # ---------------- Neighbor check ----------------
            best_idx = None
            best_dist = float('inf')
            for offset in range(-NEIGHBOR_WINDOW, NEIGHBOR_WINDOW+1):
                idx = self.current_idx + offset
                if 0 <= idx < len(self.expected_words):
                    d = editdistance.eval(word, self.expected_words[idx])
                    if d < best_dist:
                        best_dist = d
                        best_idx = idx

            if best_dist <= MAX_DIST and best_idx != self.current_idx:
                self.results.append(("OK_NEIGHBOR", word))
                print(f"ℹ Neighbor OK: '{word}' (near expected '{self.expected_words[best_idx]}')")
                continue  # do not advance current_idx

            # ---------------- Mistake handling ----------------
            if self.tolerance == "easy":
                print(f"❌ Mistake: You said '{word}', expected '{expected_word}'. Please repeat.")
                self.results.append(("MISTAKE", word))

            elif self.tolerance == "intermediate":
                # If first mistake, prompt user to recall
                if getattr(self, "intermediate_stage", 0) == 0:
                    print(f"❌ Mistake: Try to recall the word instead of '{word}'...")
                    self.results.append(("MISTAKE", word))
                    self.intermediate_stage = 1
                    break  # wait for next user input on same word

                # If second attempt, give translation hint
                elif self.intermediate_stage == 1:
                    hint = self.translation_dict.get(expected_word, "hint: think carefully")
                    print(f"💡 Hint: {hint}")
                    self.intermediate_stage = 2
                    break  # wait for next user input on same word

                # If third attempt, show the word
                elif self.intermediate_stage == 2:
                    print(f"📝 The word was: '{expected_word}'")
                    self.intermediate_stage = 0
                    self.current_idx += 1  # move to next word after showing the answer
                    break  # continue with next input

                # If user recites correctly at any stage
                if dist <= MAX_DIST:
                    print(f"✅ OK: '{word}'")
                    self.results.append(("OK", word))
                    self.current_idx += 1
                    self.intermediate_stage = 0


            elif self.tolerance == "hard":
                # Determine start of current ayah
                ayah_start = max([s for s in self.ayah_starts if s <= self.current_idx])

                # Initialize per-ayah attempt tracking
                if getattr(self, "hard_attempts_dict", None) is None:
                    self.hard_attempts_dict = {}

                if ayah_start not in self.hard_attempts_dict:
                    self.hard_attempts_dict[ayah_start] = {"attempts": 0, "in_repeat": False}

                ayah_state = self.hard_attempts_dict[ayah_start]

                # Check if user is correctly repeating from ayah start
                if self.current_idx == ayah_start:
                    ayah_state["in_repeat"] = True

                # Evaluate word
                if dist <= MAX_DIST:
                    print(f"✅ OK: '{word}'")
                    self.results.append(("OK", word))
                    self.current_idx += 1

                    # If user was repeating from start, count as an attempt
                    if ayah_state["in_repeat"]:
                        ayah_state["attempts"] += 1
                        ayah_state["in_repeat"] = False

                    # Reset attempts if successfully finished the ayah
                    if self.current_idx > max(self.ayah_starts) and ayah_state["attempts"] > 0:
                        ayah_state["attempts"] = 0

                else:
                    if ayah_state["attempts"] < 3:
                        print(f"❌ Mistake: Repeat from start of ayah (index {ayah_start}). Attempt {ayah_state['attempts']+1}/3")
                        self.current_idx = ayah_start
                        ayah_state["in_repeat"] = True
                    else:
                        print(f"📝 The word was: '{expected_word}'")
                        self.current_idx += 1
                        ayah_state["attempts"] = 0
                        ayah_state["in_repeat"] = False

    def print_summary(self):
        ok = [r for r in self.results if r[0]=="OK"]
        neighbor = [r for r in self.results if r[0]=="OK_NEIGHBOR"]
        mistakes = [r for r in self.results if r[0]=="MISTAKE"]
        extra = [r for r in self.results if r[0]=="EXTRA"]
        print(f"\nSummary: OK={len(ok)}, Neighbor_OK={len(neighbor)}, Mistakes={len(mistakes)}, EXTRA={len(extra)}\n")
# ------------------ Transcribe Worker ------------------
aligner = InteractiveToleranceAlignment(expected_words)

def transcribe_worker():
    while True:
        data = audio_queue.get()
        if data is None:
            break
        audio = np.concatenate(data).astype(np.float32)
        inputs = processor(audio, sampling_rate=SR, return_tensors="pt").to(device)
        with torch.no_grad():
            ids = model.generate(inputs.input_features, max_new_tokens=150)
        txt = processor.batch_decode(ids, skip_special_tokens=True)[0].strip()
        if txt:
            print("📌 Transcribed:", txt)
            tokens = tokenize_arabic(txt)
            aligner.align(tokens)
        buffer.clear()

# ------------------ Callback ------------------
def callback(indata, frames, time, status):
    if status.input_overflow:
        return
    mono = indata[:,0].astype(np.float32)
    buffer.append(mono)
    total_audio = np.concatenate(buffer)
    if len(total_audio) >= MIN_SPEECH:
        rms = np.sqrt(np.mean(total_audio**2))
        if rms > ENERGY_THRESHOLD:
            audio_queue.put(buffer.copy())
        buffer.clear()

# ------------------ Run Stream ------------------
thread = threading.Thread(target=transcribe_worker, daemon=True)
print("\n🎤 Ready. Speak Quran.... Ctrl+C to stop\n")
thread.start()

try:
    with sd.InputStream(channels=1, samplerate=SR, blocksize=CHUNK, callback=callback):
        while True:
            sd.sleep(100)
except KeyboardInterrupt:
    audio_queue.put(None)
    print("\n🛑 Stopped.")