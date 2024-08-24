-- Disable foreign key constraint checking to avoid issues during the drop.
PRAGMA foreign_keys = OFF;

-- Rollback for table psalm_tag
DROP TABLE IF EXISTS psalm_tag;

-- Rollback for table tag
DROP TABLE IF EXISTS tags;

-- Re-enable foreign key constraint checking after the operations are done.
PRAGMA foreign_keys = ON;
