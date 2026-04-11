# =============================================
# 1. Install sqlite3 and download mysql2sqlite
# =============================================
!apt-get install -y sqlite3 >/dev/null

!wget -q https://raw.githubusercontent.com/mysql2sqlite/mysql2sqlite/master/mysql2sqlite
!chmod +x mysql2sqlite

print("mysql2sqlite installed.")


# =============================================
# 2. Define paths
# =============================================
DB_PATH = "/content/drive/MyDrive/server/backend_v1/database/quran.db"
SRC_AYAH  = "/content/drive/MyDrive/server/backend_v1/database/quran_db/quran_db_ayahs.sql"
SRC_SURAH = "/content/drive/MyDrive/server/backend_v1/database/quran_db/quran_db_surahs.sql"

# Remove old DB if exists
!rm -f "$DB_PATH"


# =============================================
# 3. Merge both MySQL dump files
# =============================================
!cat "$SRC_SURAH" "$SRC_AYAH" > full_dump.sql
print("Merged SQL dump created.")


# =============================================
# 4. Convert MySQL dump → SQLite-compatible SQL
# =============================================
!./mysql2sqlite full_dump.sql > converted.sql
print("Conversion to SQLite-compatible SQL complete.")


# =============================================
# 5. Create SQLite DB
# =============================================
!sqlite3 "$DB_PATH" < converted.sql
print("SQLite database created at:", DB_PATH)


# =============================================
# 6. Validate tables
# =============================================
import sqlite3

try:
    conn = sqlite3.connect(DB_PATH)
    cur = conn.cursor()

    cur.execute("SELECT COUNT(*) FROM surahs")
    print("Total Surahs:", cur.fetchone()[0])

    cur.execute("SELECT COUNT(*) FROM ayahs")
    print("Total Ayahs:", cur.fetchone()[0])

    conn.close()
except Exception as e:
    print("Validation error:", e)

print("Done.")
