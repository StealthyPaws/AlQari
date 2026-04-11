import json
import torch
import re
from pathlib import Path
from collections import Counter
from sentence_transformers import SentenceTransformer, util
from rapidfuzz import fuzz


# =========================================================
# CONFIG
# =========================================================
BASE_DIR = Path(r"C:\Users\User\Downloads\FYP\intent_classifier_pipeline")
DATA_PATH = BASE_DIR / "quran_basics_tolerant1.json"
EMBED_SAVE_PATH = BASE_DIR / "quran_basics_tolerant1.embeddings.pt"
KEYWORD_SAVE_PATH = BASE_DIR / "quran_basics_tolerant1.keywords.json"
MODEL_NAME = "all-MiniLM-L6-v2"

# =========================================================
# GLOBALS (cached in memory)
# =========================================================
_model = None
_example_index = None
_example_embeddings = None
_domain_keywords = []
_action_keywords = []
_semantic_keywords = []


# =========================================================
# INTERNAL HELPERS
# =========================================================
def _load_model():
    global _model
    if _model is None:
        print("[Normalizer] Loading model...")
        _model = SentenceTransformer(MODEL_NAME)
    return _model


def _load_embeddings():
    global _example_index, _example_embeddings
    if _example_index is not None and _example_embeddings is not None:
        return _example_index, _example_embeddings

    if not EMBED_SAVE_PATH.exists():
        raise FileNotFoundError(f"[ERROR] Missing embeddings file at {EMBED_SAVE_PATH}")
    print("[Normalizer] Loading embeddings...")
    embed_data = torch.load(EMBED_SAVE_PATH, map_location="cpu")
    _example_index = embed_data["example_index"]
    _example_embeddings = embed_data["example_embeddings"]
    return _example_index, _example_embeddings


def _load_keywords():
    global _domain_keywords, _action_keywords, _semantic_keywords
    if _domain_keywords and _action_keywords:
        return _domain_keywords, _action_keywords, _semantic_keywords

    if not KEYWORD_SAVE_PATH.exists():
        raise FileNotFoundError(f"[ERROR] Missing keyword file at {KEYWORD_SAVE_PATH}")
    print("[Normalizer] Loading keyword data...")
    with open(KEYWORD_SAVE_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    _domain_keywords = data.get("domain_keywords", [])
    _action_keywords = data.get("action_keywords", [])
    _semantic_keywords = data.get("semantic_keywords", [])
    return _domain_keywords, _action_keywords, _semantic_keywords


def _extract_keywords(text):
    text_lower = text.lower()
    domain_hits = [kw for kw in _domain_keywords if kw in text_lower]
    action_hits = [kw for kw in _action_keywords if kw in text_lower]
    semantic_hits = [kw for kw in _semantic_keywords if kw in text_lower]
    return domain_hits, action_hits, semantic_hits


# =========================================================
# MAIN FUNCTION
# =========================================================
def normalize_user_input(user_text: str, top_k: int = 5):
    """
    Normalize a user query against dataset examples.
    Returns:
        (matched_example, intent, confidence)
    """
    model = _load_model()
    example_index, example_embeddings = _load_embeddings()
    _load_keywords()  # ensure keywords loaded

    # user_emb = model.encode(user_text, convert_to_tensor=True)
    # cos_scores = util.cos_sim(user_emb, example_embeddings)[0]
    user_emb = model.encode(user_text, convert_to_tensor=True)

    # --- ensure same device for both ---
    device = example_embeddings.device
    user_emb = user_emb.to(device)

    cos_scores = util.cos_sim(user_emb, example_embeddings)[0]

    top_results = torch.topk(cos_scores, k=top_k)

    user_domains, user_actions, user_semantic = _extract_keywords(user_text)

    candidates = []
    for score, idx in zip(top_results.values, top_results.indices):
        ex = example_index[idx]
        fuzzy_score = fuzz.partial_ratio(user_text.lower(), ex["example"].lower())
        ex_domains, ex_actions, ex_semantic = _extract_keywords(ex["example"])

        domain_overlap = len(set(user_domains) & set(ex_domains))
        action_overlap = len(set(user_actions) & set(ex_actions))
        semantic_overlap = len(set(user_semantic) & set(ex_semantic))

        combined = (
            0.6 * float(score)
            + 0.25 * (fuzzy_score / 100)
            + 0.1 * domain_overlap
            + 0.05 * (action_overlap + semantic_overlap * 0.5)
        )
        candidates.append((combined, ex))

    best_score, best_match = max(candidates, key=lambda x: x[0])
    confidence = round(best_score * 100, 2)

    return best_match["example"], best_match["intent"], confidence


# =========================================================
# DEBUG MODE (optional standalone test)
# =========================================================
if __name__ == "__main__":
    print("[Normalizer] Ready. Type text to test (or 'exit'):\n")
    while True:
        text = input("User: ").strip()
        if text.lower() in ["exit", "quit"]:
            break
        match, intent, conf = normalize_user_input(text)
        label = "⚠️  OOD / Low Confidence" if conf < 50 else "✅  Confident"
        print(f"→ Mapped to: '{match}'  |  Intent: {intent}  |  Confidence: {conf}%  |  {label}\n")
