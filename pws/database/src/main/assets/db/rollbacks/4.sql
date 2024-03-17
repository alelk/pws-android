-- SQLite does not support directly deleting a column from a table via ALTER TABLE
-- Instead, you need to create a new table without the 'edited' column,
-- migrate the data, drop the old table, and rename the new table.
BEGIN TRANSACTION;

-- Explicitly creating a new table without the 'edited' column
CREATE TABLE temp_psalms (
    _id INTEGER PRIMARY KEY AUTOINCREMENT,
    version TEXT NOT NULL,
    locale TEXT,
    name TEXT,
    author TEXT,
    translator TEXT,
    composer TEXT,
    tonalities TEXT,
    year TEXT,
    bibleref TEXT,
    text TEXT NOT NULL
);

-- Copying data from the old table to the new one, without the 'edited' column
INSERT INTO temp_psalms (_id, version, locale, name, author, translator, composer, tonalities, year, bibleref, text)
SELECT _id, version, locale, name, author, translator, composer, tonalities, year, bibleref, text
FROM psalms;

-- Delete old table
DROP TABLE psalms;

-- Rename the temporary table to its original name
ALTER TABLE temp_psalms RENAME TO psalms;

COMMIT;