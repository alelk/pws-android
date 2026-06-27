package io.github.alelk.pws.database

// Future Room migrations go here.
//
// History:
//   v14 (this release) — initial schema shipped via asset DB (pws.db). No migration needed:
//     fresh installs copy v14 asset; upgrades from older releases get a fresh v14 DB and user
//     data is replayed by `migrateDataFromPrevDatabase` from the previous on-disk file.
