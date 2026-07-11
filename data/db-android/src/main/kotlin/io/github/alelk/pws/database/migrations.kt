package io.github.alelk.pws.database

// Future Room migrations go here.
//
// History:
//   v14 (this release) — initial schema. Upgrades from older releases (pws.1.8.0.db etc.) get
//     a fresh v14 DB; books and user data are replayed by `migrateDataFromPrevDatabase`.
