# database/quran_db.py

import sqlite3
from pathlib import Path

DB_PATH = "/content/drive/MyDrive/server/backend_v1/database/quran.db"

def dict_factory(cursor, row):
    result = {}
    for idx, col in enumerate(cursor.description):
        result[col[0]] = row[idx]
    return result

def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = dict_factory
    return conn
