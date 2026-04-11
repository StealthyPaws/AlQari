# quran_db_full_build_fixed.py
"""
Rebuilds quran_db from scratch:
 - Drops and creates database and tables
 - Fetches IndoPak Arabic (quran.com bulk endpoint)
 - Fetches audio URLs (all pages) from quran.com recitations
 - Fetches English (Asad) and Urdu (Junagarhi) translations per-surah from alquran.cloud (bulk)
 - Inserts everything into MySQL
 - Prints summary tables and exports CSVs to EXPORT_DIR
"""

import os
import time
import json
import requests
from requests.adapters import HTTPAdapter, Retry
import mysql.connector
from prettytable import PrettyTable
import pandas as pd

# --------------------------
# CONFIG
# --------------------------
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "12345678",   # <-- change if needed
    "database": "quran_db"
}

EXPORT_DIR = r"C:\Users\mahee\Downloads\FYP"   # change if you want a different export folder
os.makedirs(EXPORT_DIR, exist_ok=True)

# API endpoints
QURANCOM_CHAPTERS = "https://api.quran.com/api/v4/chapters"
QURANCOM_INDOPAK = "https://api.quran.com/api/v4/quran/verses/indopak"
QURANCOM_AUDIO_BY_CHAPTER = "https://api.quran.com/api/v4/recitations/7/by_chapter/{surah_id}"
ALQURAN_SURAH_EDITIONS = "https://api.alquran.cloud/v1/surah/{surah_id}/editions/en.asad,ur.junagarhi"

# --------------------------
# HTTP session with retries
# --------------------------
session = requests.Session()
retries = Retry(total=5, backoff_factor=0.8,
                status_forcelist=(500, 502, 503, 504),
                allowed_methods=frozenset(['GET', 'POST']))
adapter = HTTPAdapter(max_retries=retries)
session.mount("https://", adapter)
session.mount("http://", adapter)
DEFAULT_TIMEOUT = 20  # seconds

def http_get(url, **kwargs):
    """GET with session, retries and timeout."""
    timeout = kwargs.pop("timeout", DEFAULT_TIMEOUT)
    resp = session.get(url, timeout=timeout, **kwargs)
    resp.raise_for_status()
    return resp

# --------------------------
# 0. Quick check for MySQL connectivity
# --------------------------
print("Connecting to MySQL...")
try:
    conn0 = mysql.connector.connect(host=DB_CONFIG["host"], user=DB_CONFIG["user"], password=DB_CONFIG["password"])
    conn0.close()
except Exception as e:
    print("ERROR: cannot connect to MySQL with provided credentials. Fix DB_CONFIG and retry.")
    raise

# --------------------------
# 1. Fetch chapters metadata and full IndoPak map
# --------------------------
print("Fetching chapter metadata (Quran.com)...")
chapters = http_get(QURANCOM_CHAPTERS).json().get("chapters", [])
print(f"Found {len(chapters)} chapters.")

print("Fetching full IndoPak Arabic verses (this may take a few seconds)...")
indopak_resp = http_get(QURANCOM_INDOPAK)
indopak_verses = indopak_resp.json().get("verses", [])
# map verse_key -> text_indopak
indopak_map = {v["verse_key"]: v.get("text_indopak", "") for v in indopak_verses}
print(f"Loaded IndoPak for {len(indopak_map)} verses.")

# --------------------------
# 2. Create DB and tables (fresh)
# --------------------------
print("Rebuilding database (DROP -> CREATE)...")
# Connect without database for drop/create
root_conn = mysql.connector.connect(host=DB_CONFIG["host"], user=DB_CONFIG["user"], password=DB_CONFIG["password"])
root_cursor = root_conn.cursor()
root_cursor.execute("DROP DATABASE IF EXISTS quran_db")
root_cursor.execute("CREATE DATABASE quran_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci")
root_cursor.close()
root_conn.close()

# Now connect to new DB
conn = mysql.connector.connect(host=DB_CONFIG["host"], user=DB_CONFIG["user"], password=DB_CONFIG["password"], database=DB_CONFIG["database"])
cursor = conn.cursor()

print("Creating tables...")
cursor.execute("""
CREATE TABLE IF NOT EXISTS surahs (
    surah_id INT PRIMARY KEY,
    name_arabic VARCHAR(255),
    name_simple VARCHAR(255),
    name_english VARCHAR(255),
    revelation_place VARCHAR(20),
    total_ayahs INT
) CHARACTER SET = utf8mb4;
""")

cursor.execute("""
CREATE TABLE IF NOT EXISTS ayahs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    surah_id INT,
    verse_number INT,
    verse_key VARCHAR(20),
    arabic_indopak TEXT,
    audio_url VARCHAR(1000),
    english_translation LONGTEXT,
    urdu_translation LONGTEXT,
    FOREIGN KEY (surah_id) REFERENCES surahs(surah_id)
) CHARACTER SET = utf8mb4;
""")
conn.commit()
print("Tables created.")

# --------------------------
# 3. Helper: fetch all audio files for a chapter (paginated)
# --------------------------
def fetch_all_audio_files(surah_id):
    files = []
    page = 1
    per_page = 200
    while True:
        url = QURANCOM_AUDIO_BY_CHAPTER.format(surah_id=surah_id) + f"?page={page}&per_page={per_page}"
        try:
            data = http_get(url).json()
        except Exception as e:
            print(f"Warning: audio fetch failed for surah {surah_id} page {page}: {e}")
            break
        audio_files = data.get("audio_files", [])
        if not audio_files:
            break
        files.extend(audio_files)
        if len(audio_files) < per_page:
            break
        page += 1
        time.sleep(0.1)
    return files

# --------------------------
# 4. Helper: fetch surah translations from alquran.cloud (bulk)
# --------------------------
def fetch_surah_translations_alquran(surah_id):
    """
    Returns dict: verse_number -> {"english": text, "urdu": text}
    """
    url = ALQURAN_SURAH_EDITIONS.format(surah_id=surah_id)
    try:
        resp = http_get(url, timeout=30)
    except Exception as e:
        print(f"Warning: failed to fetch translations for surah {surah_id}: {e}")
        return {}
    js = resp.json()
    data = js.get("data", [])
    translations = {}
    # data is list of editions; each edition has 'edition' and 'ayahs'
    for edition in data:
        ident = edition.get("edition", {}).get("identifier", "")
        ayahs = edition.get("ayahs", [])
        for ay in ayahs:
            num = ay.get("numberInSurah") or ay.get("number")
            if num is None:
                continue
            if num not in translations:
                translations[num] = {"english": "", "urdu": ""}
            if "en.asad" in ident:
                translations[num]["english"] = ay.get("text", "")
            elif "ur.junagarhi" in ident:
                translations[num]["urdu"] = ay.get("text", "")
    return translations

# --------------------------
# 5. Insert surahs and ayahs
# --------------------------
print("Starting insert of surahs and ayahs (this will take a while)...")
for ch in chapters:
    surah_id = ch.get("id")
    name_arabic = ch.get("name_arabic")
    name_simple = ch.get("name_simple")
    name_english = ch.get("translated_name", {}).get("name", "")
    revelation_place = (ch.get("revelation_place") or "").capitalize()
    total_ayahs = ch.get("verses_count") or 0

    cursor.execute("""
        INSERT INTO surahs (surah_id, name_arabic, name_simple, name_english, revelation_place, total_ayahs)
        VALUES (%s, %s, %s, %s, %s, %s)
    """, (surah_id, name_arabic, name_simple, name_english, revelation_place, total_ayahs))
    conn.commit()

    # fetch translations in bulk (english + urdu)
    translations = fetch_surah_translations_alquran(surah_id)

    # fetch audio files (all pages)
    audio_files = fetch_all_audio_files(surah_id)
    audio_map = {a["verse_key"]: a.get("url", "") for a in audio_files}

    # insert every ayah 1..total_ayahs using verse_key surah:ayah
    for n in range(1, total_ayahs + 1):
        verse_key = f"{surah_id}:{n}"
        arabic_text = indopak_map.get(verse_key, "")
        audio_url = audio_map.get(verse_key, "")

        eng = translations.get(n, {}).get("english", "")
        ur = translations.get(n, {}).get("urdu", "")

        # Insert into ayahs
        cursor.execute("""
            INSERT INTO ayahs (surah_id, verse_number, verse_key, arabic_indopak, audio_url, english_translation, urdu_translation)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (surah_id, n, verse_key, arabic_text, audio_url, eng, ur))

    conn.commit()
    print(f"✅ Surah {surah_id} ({name_simple}) inserted with {total_ayahs} ayahs.")
    time.sleep(0.2)  # polite pause

print("🎉 All surahs and ayahs inserted successfully!")

# --------------------------
# 6. Display small summaries
# --------------------------
print("\n📖 Sample Surahs (first 10):")
cursor.execute("SELECT surah_id, name_simple, name_english, revelation_place, total_ayahs FROM surahs ORDER BY surah_id LIMIT 10")
rows = cursor.fetchall()
t = PrettyTable([desc[0] for desc in cursor.description])
for r in rows:
    t.add_row(r)
print(t)

print("\n📖 Ayah counts for first 10 surahs (should match total_ayahs):")
cursor.execute("SELECT s.surah_id, s.total_ayahs, COUNT(a.id) FROM surahs s LEFT JOIN ayahs a ON s.surah_id=a.surah_id GROUP BY s.surah_id ORDER BY s.surah_id LIMIT 10")
rows = cursor.fetchall()
t = PrettyTable(["Surah ID", "Declared total_ayahs", "Inserted ayahs"])
for r in rows:
    t.add_row(r)
print(t)

# show a sample of Surah 2 first 12 ayahs
sample_surah = 2
print(f"\n📖 Sample Ayahs (Surah {sample_surah} - first 12):")
cursor.execute("SELECT verse_key, arabic_indopak, english_translation, urdu_translation, audio_url FROM ayahs WHERE surah_id=%s ORDER BY verse_number LIMIT 12", (sample_surah,))
rows = cursor.fetchall()
t = PrettyTable([desc[0] for desc in cursor.description])
for r in rows:
    t.add_row(r)
print(t)

# --------------------------
# 7. Export CSVs (surahs + ayahs)
# --------------------------
print(f"\nExporting CSVs to {EXPORT_DIR} ...")
df_surahs = pd.read_sql("SELECT * FROM surahs ORDER BY surah_id", conn)
df_ayahs = pd.read_sql("SELECT * FROM ayahs ORDER BY surah_id, verse_number", conn)

surah_csv = os.path.join(EXPORT_DIR, "surahs.csv")
ayahs_csv = os.path.join(EXPORT_DIR, "ayahs.csv")
df_surahs.to_csv(surah_csv, index=False, encoding="utf-8-sig")
df_ayahs.to_csv(ayahs_csv, index=False, encoding="utf-8-sig")
print("✅ CSV export done:")
print(" -", surah_csv)
print(" -", ayahs_csv)

# --------------------------
# 8. Close
# --------------------------
cursor.close()
conn.close()
print("\n✅ Done. Database built with full surahs + ayahs + translations.")


# quran_export_csv.py
import os
import pandas as pd
import mysql.connector
from sqlalchemy import create_engine

# --------------------------
# CONFIG
# --------------------------
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "12345678",
    "database": "quran_db"
}

EXPORT_DIR = r"C:\Users\mahee\Downloads\FYP"
os.makedirs(EXPORT_DIR, exist_ok=True)

# --------------------------
# Use SQLAlchemy for pandas
# --------------------------
engine_url = f"mysql+mysqlconnector://{DB_CONFIG['user']}:{DB_CONFIG['password']}@{DB_CONFIG['host']}/{DB_CONFIG['database']}"
engine = create_engine(engine_url)

# --------------------------
# Export
# --------------------------
print(f"Exporting CSVs to {EXPORT_DIR} ...")

df_surahs = pd.read_sql("SELECT * FROM surahs ORDER BY surah_id", engine)
df_surahs.to_csv(os.path.join(EXPORT_DIR, "surahs.csv"), index=False, encoding="utf-8-sig")

df_ayahs = pd.read_sql("SELECT * FROM ayahs ORDER BY surah_id, verse_number", engine)
df_ayahs.to_csv(os.path.join(EXPORT_DIR, "ayahs.csv"), index=False, encoding="utf-8-sig")

print("✅ CSV export completed successfully!")
