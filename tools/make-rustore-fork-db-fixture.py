#!/usr/bin/env python3
"""
Builds the upgrade test fixture `test-db/v11-rustore-2.3.1/pws.2.3.0.dbz` from the database shipped
inside the published RuStore fork APK 2.3.1 (plan 2026-09-29, T-A04).

The fork's schema is Room v11 and its file name is `pws.2.3.0.db`. The shipped database carries no
user data, so the script:
  1. extracts `assets/db/pws.2.3.0.dbz.*` from the APK and unpacks it;
  2. trims it to three books (PV3300 — the rustore seed book — plus PV800 and YunostIisusu, i.e. user data
     in books *other than* PV3300) and their first 40 song numbers, to keep the fixture small;
  3. adds user data the way the fork writes it: favorites, history, edited songs, a custom tag and
     predefined-tag links, spread over all three books;
  4. zips the result as `pws.2.3.0.dbz` (the format read by the db-android test helpers).

Usage:
  python3 tools/make-rustore-fork-db-fixture.py \
    ../pws-android-rustore/output/pws-app-release-2.3.1-ru.apk \
    data/db-android/src/test/resources/test-db/v11-rustore-2.3.1/

Expected user data (asserted by MigrateLegacyDatabaseFileTest): 4 favorites, 5 history records,
2 edited songs, 1 custom tag with 3 songs, 2 extra predefined-tag links (on top of the predefined
links that ship with the fork's content).
"""
import io
import os
import sqlite3
import sys
import tempfile
import zipfile

BOOKS = ("PV3300", "PV800", "YunostIisusu")
MAX_NUMBER = 40


def extract_db(apk_path: str, work_dir: str) -> str:
    with zipfile.ZipFile(apk_path) as apk:
        parts = sorted(
            (n for n in apk.namelist() if n.startswith("assets/db/pws.2.3.0.dbz.")),
            key=lambda n: int(n.rsplit(".", 1)[1]),
        )
        if not parts:
            sys.exit("no assets/db/pws.2.3.0.dbz.* in the APK — is this the fork 2.3.1?")
        dbz = b"".join(apk.read(n) for n in parts)
    with zipfile.ZipFile(io.BytesIO(dbz)) as z:
        z.extract("pws.2.3.0.db", work_dir)
    return os.path.join(work_dir, "pws.2.3.0.db")


def song_id(c: sqlite3.Connection, book: str, number: int) -> int:
    row = c.execute("SELECT song_id FROM song_numbers WHERE book_id=? AND number=?", (book, number)).fetchone()
    if row is None:
        sys.exit(f"{book} #{number} not in the trimmed database")
    return row[0]


def trim(c: sqlite3.Connection) -> None:
    marks = ",".join("?" * len(BOOKS))
    c.execute(f"DELETE FROM song_numbers WHERE book_id NOT IN ({marks}) OR number > ?", (*BOOKS, MAX_NUMBER))
    c.execute(f"DELETE FROM book_statistic WHERE id NOT IN ({marks})", BOOKS)
    c.execute(f"DELETE FROM books WHERE id NOT IN ({marks})", BOOKS)
    c.execute("DELETE FROM songs WHERE id NOT IN (SELECT song_id FROM song_numbers)")
    c.execute("DELETE FROM song_references WHERE song_id NOT IN (SELECT id FROM songs) OR ref_song_id NOT IN (SELECT id FROM songs)")
    c.execute("DELETE FROM song_tags WHERE song_id NOT IN (SELECT id FROM songs)")
    c.execute("INSERT INTO songs_fts(songs_fts) VALUES ('rebuild')")


def add_user_data(c: sqlite3.Connection) -> None:
    # favorites (position ascending = order of adding)
    for position, (book, number) in enumerate([("PV3300", 1), ("PV3300", 5), ("PV800", 3), ("YunostIisusu", 2)], start=1):
        c.execute("INSERT INTO favorites(song_id, book_id, position) VALUES (?, ?, ?)", (song_id(c, book, number), book, position))
    # history — same timestamp format as the fork (LocalDateTime ISO, space separator)
    for book, number, ts in [
        ("PV3300", 1, "2025-10-29 18:25:04.412076"),
        ("PV3300", 1, "2025-10-30 09:01:12.000001"),
        ("PV800", 3, "2025-10-30 09:05:00.5"),
        ("YunostIisusu", 7, "2025-11-01 20:00:00"),
        ("PV800", 10, "2025-11-02 07:30:45.123"),
    ]:
        c.execute("INSERT INTO history(song_id, book_id, access_timestamp) VALUES (?, ?, ?)", (song_id(c, book, number), book, ts))
    # edited songs
    for book, number in [("YunostIisusu", 2), ("PV800", 3)]:
        sid = song_id(c, book, number)
        c.execute("UPDATE songs SET lyric = lyric || ?, edited = 1 WHERE id = ?", ("\n\n[fixture: edited by user]", sid))
    # a custom tag over three books + two predefined-tag links
    c.execute("INSERT INTO tags(id, name, priority, color, predefined) VALUES ('custom-00001', 'Мои любимые', 0, '#942729', 0)")
    for book, number in [("PV3300", 5), ("PV800", 3), ("YunostIisusu", 2)]:
        c.execute("INSERT OR IGNORE INTO song_tags(song_id, tag_id, priority) VALUES (?, 'custom-00001', 0)", (song_id(c, book, number),))
    predefined = c.execute("SELECT id FROM tags WHERE predefined = 1 ORDER BY priority LIMIT 1").fetchone()[0]
    for book, number in [("PV800", 10), ("YunostIisusu", 7)]:
        c.execute("INSERT OR IGNORE INTO song_tags(song_id, tag_id, priority) VALUES (?, ?, 0)", (song_id(c, book, number), predefined))


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    apk, out_dir = sys.argv[1], sys.argv[2]
    with tempfile.TemporaryDirectory() as work:
        db_path = extract_db(apk, work)
        c = sqlite3.connect(db_path)
        trim(c)
        add_user_data(c)
        c.commit()
        c.execute("VACUUM")
        c.close()
        os.makedirs(out_dir, exist_ok=True)
        target = os.path.join(out_dir, "pws.2.3.0.dbz")
        with zipfile.ZipFile(target, "w", zipfile.ZIP_DEFLATED) as z:
            z.write(db_path, "pws.2.3.0.db")
        print(f"fixture written: {target} ({os.path.getsize(target)} bytes)")


if __name__ == "__main__":
    main()
