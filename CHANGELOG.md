# PWS Android

## [2.0.3-rc.3](https://github.com/alelk/pws-android/compare/v2.0.3-rc.2...v2.0.3-rc.3) (2025-03-03)


### Bug Fixes

* **build:** add target iosX64 ([5bdb7db](https://github.com/alelk/pws-android/commit/5bdb7dbf4aa7d28d6a6a4d73f4eca30e09e4c2e3))

## [2.0.3-rc.2](https://github.com/alelk/pws-android/compare/v2.0.3-rc.1...v2.0.3-rc.2) (2025-03-03)


### Bug Fixes

* **build:** add target iosSimulatorArm64 ([96e3482](https://github.com/alelk/pws-android/commit/96e34822d06cdfc5e58e73c5d4d18fc62f354a02))

## [2.0.3-rc.1](https://github.com/alelk/pws-android/compare/v2.0.2...v2.0.3-rc.1) (2025-03-03)


### Bug Fixes

* **build:** enable cross platform ios compilation ([37d7134](https://github.com/alelk/pws-android/commit/37d7134e733e039bbf78c0f6deba8d9e3d1d7d16))

## [2.0.2](https://github.com/alelk/pws-android/compare/v2.0.1...v2.0.2) (2025-03-02)


### Bug Fixes

* **app:** fix books fragment issue: display book short name ([30bc993](https://github.com/alelk/pws-android/commit/30bc9937b8842f556f30db940a086db8ea874fea))
* **database:** rebuild database 2.0.0 files ([d2b9758](https://github.com/alelk/pws-android/commit/d2b9758f84efe98237adb7e28124a3d15bcf4447))

## [2.0.1](https://github.com/alelk/pws-android/compare/v2.0.0...v2.0.1) (2025-03-01)


### Bug Fixes

* **search:** fix song search issue: search by song name; search from song activity ([#73](https://github.com/alelk/pws-android/issues/73)) ([8ea0ae5](https://github.com/alelk/pws-android/commit/8ea0ae5cfa6efd95de27494e9f850c07678390ae))

# [2.0.0](https://github.com/alelk/pws-android/compare/v1.15.0...v2.0.0) (2025-03-01)


* feat!: new pre-release ([13a6f2f](https://github.com/alelk/pws-android/commit/13a6f2f57c8e3aeee925913267a3dcaa16dec64b))


### Bug Fixes

* **app:** reimplement history view model ([2e2e2a3](https://github.com/alelk/pws-android/commit/2e2e2a350c969977af3c25b53d1fe0ae891b6dd7))
* **ci:** fix publish script ([f82807b](https://github.com/alelk/pws-android/commit/f82807b9a26c86d8effc184c90347a52b496ac87))
* **database:** add database-test-fixtures module ([c61f9b8](https://github.com/alelk/pws-android/commit/c61f9b8145beb820ddbee11f5305a7270e4c8c0d))
* **database:** add pws debug-ru database v2.0.0 ([ed6e9f7](https://github.com/alelk/pws-android/commit/ed6e9f7b111a14824b1175e912ff5d01191dfb3b))
* **database:** fix song suggestions providing issue ([3b6f1cd](https://github.com/alelk/pws-android/commit/3b6f1cde29974c247465b24790ff720813e6cfe4))
* **database:** mark room-ktx dependency as runtime-only ([3158642](https://github.com/alelk/pws-android/commit/31586428b424171603d75b0b94636a05f433aee5))
* **database:** remove room ktx dependency from jvmMain ([521e8a8](https://github.com/alelk/pws-android/commit/521e8a87c989847ac363ccfb27844e8afcf70a95))
* **database:** update song-references dao: use domain song id type in all method signatures ([24ab03c](https://github.com/alelk/pws-android/commit/24ab03c0c2c9c371c6090dea369bb55142aaa065))
* **project:** update gradle version & kotlin version ([ead7018](https://github.com/alelk/pws-android/commit/ead70182dcb8ddbfbefa84ba2fcc4c2b76accbb8))


### Features

* **database:** implement pws 1x migration data provider ([eae4eff](https://github.com/alelk/pws-android/commit/eae4eff8cde3de549d345d9971774cd279a26321))
* **database:** implement user data migration from previous database v1x ([9e71939](https://github.com/alelk/pws-android/commit/9e719399dca634364449c23fa74a1b9f6a510440))
* **database:** pws db 1x data provider: fetch tags ([5aefd59](https://github.com/alelk/pws-android/commit/5aefd595185918b3115bb5f69f8a31f99cb35260))
* **database:** update song-references dao: get references by song id ([3851bd9](https://github.com/alelk/pws-android/commit/3851bd96fd059e686666e83baa8cf307e4d1ecf9))
* **database:** update song-references dao: get references by song id and ref song number ([b8b1b63](https://github.com/alelk/pws-android/commit/b8b1b6367181b172e8480f4cf1f5df7520e8bb29))
* **publication:** configure multiplatform github packages publishing ([8969470](https://github.com/alelk/pws-android/commit/8969470af9d68723016c5773788af2ea2fdf7141))
* **publication:** configure multiplatform github packages publishing ([#69](https://github.com/alelk/pws-android/issues/69)) ([8c2507c](https://github.com/alelk/pws-android/commit/8c2507cbae881be43390a5e5030b7e479c08fa3d))
* **serialization:** implement domain model serialization ([#68](https://github.com/alelk/pws-android/issues/68)) ([31bb52b](https://github.com/alelk/pws-android/commit/31bb52b615ac0fd9ab303afb61d164a22bcce626))


### BREAKING CHANGES

* database schema has changed

# [2.0.0-rc.15](https://github.com/alelk/pws-android/compare/v2.0.0-rc.14...v2.0.0-rc.15) (2025-03-01)


### Bug Fixes

* **database:** add pws debug-ru database v2.0.0 ([ed6e9f7](https://github.com/alelk/pws-android/commit/ed6e9f7b111a14824b1175e912ff5d01191dfb3b))

# [2.0.0-rc.14](https://github.com/alelk/pws-android/compare/v2.0.0-rc.13...v2.0.0-rc.14) (2025-03-01)


### Features

* **database:** implement pws 1x migration data provider ([eae4eff](https://github.com/alelk/pws-android/commit/eae4eff8cde3de549d345d9971774cd279a26321))
* **database:** implement user data migration from previous database v1x ([9e71939](https://github.com/alelk/pws-android/commit/9e719399dca634364449c23fa74a1b9f6a510440))
* **database:** pws db 1x data provider: fetch tags ([5aefd59](https://github.com/alelk/pws-android/commit/5aefd595185918b3115bb5f69f8a31f99cb35260))

# [2.0.0-rc.13](https://github.com/alelk/pws-android/compare/v2.0.0-rc.12...v2.0.0-rc.13) (2025-02-24)


### Bug Fixes

* **app:** reimplement history view model ([2e2e2a3](https://github.com/alelk/pws-android/commit/2e2e2a350c969977af3c25b53d1fe0ae891b6dd7))

# [2.0.0-rc.12](https://github.com/alelk/pws-android/compare/v2.0.0-rc.11...v2.0.0-rc.12) (2025-02-24)


### Bug Fixes

* **database:** fix song suggestions providing issue ([3b6f1cd](https://github.com/alelk/pws-android/commit/3b6f1cde29974c247465b24790ff720813e6cfe4))

# [2.0.0-rc.11](https://github.com/alelk/pws-android/compare/v2.0.0-rc.10...v2.0.0-rc.11) (2025-02-24)


### Bug Fixes

* **database:** update song-references dao: use domain song id type in all method signatures ([24ab03c](https://github.com/alelk/pws-android/commit/24ab03c0c2c9c371c6090dea369bb55142aaa065))

# [2.0.0-rc.10](https://github.com/alelk/pws-android/compare/v2.0.0-rc.9...v2.0.0-rc.10) (2025-02-24)


### Features

* **database:** update song-references dao: get references by song id and ref song number ([b8b1b63](https://github.com/alelk/pws-android/commit/b8b1b6367181b172e8480f4cf1f5df7520e8bb29))

# [2.0.0-rc.9](https://github.com/alelk/pws-android/compare/v2.0.0-rc.8...v2.0.0-rc.9) (2025-02-24)


### Features

* **database:** update song-references dao: get references by song id ([3851bd9](https://github.com/alelk/pws-android/commit/3851bd96fd059e686666e83baa8cf307e4d1ecf9))

# [2.0.0-rc.8](https://github.com/alelk/pws-android/compare/v2.0.0-rc.7...v2.0.0-rc.8) (2025-02-24)


### Bug Fixes

* **database:** remove room ktx dependency from jvmMain ([521e8a8](https://github.com/alelk/pws-android/commit/521e8a87c989847ac363ccfb27844e8afcf70a95))

# [2.0.0-rc.7](https://github.com/alelk/pws-android/compare/v2.0.0-rc.6...v2.0.0-rc.7) (2025-02-24)


### Bug Fixes

* **database:** add database-test-fixtures module ([c61f9b8](https://github.com/alelk/pws-android/commit/c61f9b8145beb820ddbee11f5305a7270e4c8c0d))

# [2.0.0-rc.6](https://github.com/alelk/pws-android/compare/v2.0.0-rc.5...v2.0.0-rc.6) (2025-02-21)


### Bug Fixes

* **project:** update gradle version & kotlin version ([ead7018](https://github.com/alelk/pws-android/commit/ead70182dcb8ddbfbefa84ba2fcc4c2b76accbb8))

# [2.0.0-rc.5](https://github.com/alelk/pws-android/compare/v2.0.0-rc.4...v2.0.0-rc.5) (2025-02-20)


### Bug Fixes

* **database:** mark room-ktx dependency as runtime-only ([3158642](https://github.com/alelk/pws-android/commit/31586428b424171603d75b0b94636a05f433aee5))


### Features

* **publication:** configure multiplatform github packages publishing ([8969470](https://github.com/alelk/pws-android/commit/8969470af9d68723016c5773788af2ea2fdf7141))

# [2.0.0-rc.4](https://github.com/alelk/pws-android/compare/v2.0.0-rc.3...v2.0.0-rc.4) (2025-02-20)


### Features

* **publication:** configure multiplatform github packages publishing ([#69](https://github.com/alelk/pws-android/issues/69)) ([8c2507c](https://github.com/alelk/pws-android/commit/8c2507cbae881be43390a5e5030b7e479c08fa3d))

# [2.0.0-rc.3](https://github.com/alelk/pws-android/compare/v2.0.0-rc.2...v2.0.0-rc.3) (2025-02-18)


### Features

* **serialization:** implement domain model serialization ([#68](https://github.com/alelk/pws-android/issues/68)) ([31bb52b](https://github.com/alelk/pws-android/commit/31bb52b615ac0fd9ab303afb61d164a22bcce626))

# [2.0.0-rc.2](https://github.com/alelk/pws-android/compare/v2.0.0-rc.1...v2.0.0-rc.2) (2025-02-16)


### Bug Fixes

* **ci:** fix publish script ([f82807b](https://github.com/alelk/pws-android/commit/f82807b9a26c86d8effc184c90347a52b496ac87))

# [2.0.0-rc.1](https://github.com/alelk/pws-android/compare/v1.15.0...v2.0.0-rc.1) (2025-02-16)


* feat!: new pre-release ([13a6f2f](https://github.com/alelk/pws-android/commit/13a6f2f57c8e3aeee925913267a3dcaa16dec64b))


### BREAKING CHANGES

* database schema has changed

# [1.15.0](https://github.com/alelk/pws-android/compare/v1.14.0...v1.15.0) (2025-02-16)


### Features

* **release:** update app version: v32 ([0d4122e](https://github.com/alelk/pws-android/commit/0d4122efc5eed036f17ee21cdacb06c0a9ef7d49))

# [1.14.0](https://github.com/alelk/pws-android/compare/v1.13.0...v1.14.0) (2025-02-13)


### Features

* **dependency-injection:** Add Hilt dependency injection to SongPreferencesDialogFragment ([#65](https://github.com/alelk/pws-android/issues/65)) ([abc039d](https://github.com/alelk/pws-android/commit/abc039dc8cd75af98845a082107ae6020533e38f))

# [1.13.0](https://github.com/alelk/pws-android/compare/v1.12.0...v1.13.0) (2025-02-12)


### Features

* **category:** Add sorting functionality for tag songs list ([#64](https://github.com/alelk/pws-android/issues/64)) ([1629a04](https://github.com/alelk/pws-android/commit/1629a0478ea072119e200f6574510502ce14df2e))

# [1.12.0](https://github.com/alelk/pws-android/compare/v1.11.4...v1.12.0) (2025-02-11)


### Features

* **favorites:** Add sorting direction and menu icons for favorites list ([#63](https://github.com/alelk/pws-android/issues/63)) ([5e0686c](https://github.com/alelk/pws-android/commit/5e0686cbb8e2aacef3ed719b78c2e83151d0df1c))

## [1.11.4](https://github.com/alelk/pws-android/compare/v1.11.3...v1.11.4) (2025-02-04)


### Bug Fixes

* update library versions ([#56](https://github.com/alelk/pws-android/issues/56)) ([be27b34](https://github.com/alelk/pws-android/commit/be27b346e012da75f5bc714fa3b87075a2bcf145))

## [1.11.3](https://github.com/alelk/pws-android/compare/v1.11.2...v1.11.3) (2025-02-03)


### Bug Fixes

* **search:** fix case-insensitive search ([#55](https://github.com/alelk/pws-android/issues/55)) ([6498749](https://github.com/alelk/pws-android/commit/64987498e68310addc16340a2f03e30f47404628))

## [1.11.2](https://github.com/alelk/pws-android/compare/v1.11.1...v1.11.2) (2025-01-02)


### Bug Fixes

* configure domain package publication ([#50](https://github.com/alelk/pws-android/issues/50)) ([8e0af70](https://github.com/alelk/pws-android/commit/8e0af70cc35d399bbc8ec549b34c150fc73f5d65))

## [1.11.1](https://github.com/alelk/pws-android/compare/v1.11.0...v1.11.1) (2025-01-01)


### Bug Fixes

* add build script; prepare for new public release ([#49](https://github.com/alelk/pws-android/issues/49)) ([157ebd3](https://github.com/alelk/pws-android/commit/157ebd32ff2a0ee2231dcf439d4c15c9eb7e7e31))

# [1.11.0](https://github.com/alelk/pws-android/compare/v1.10.7...v1.11.0) (2025-01-01)


### Features

* **backup:** backup feature refactoring ([#48](https://github.com/alelk/pws-android/issues/48)) ([e544c4d](https://github.com/alelk/pws-android/commit/e544c4db874bb349569cc80ec4dca89f131a982a))

## [1.10.7](https://github.com/alelk/pws-android/compare/v1.10.6...v1.10.7) (2024-12-30)


### Bug Fixes

* **database:** fix app starting with empty database ([#45](https://github.com/alelk/pws-android/issues/45)) ([02c1d70](https://github.com/alelk/pws-android/commit/02c1d706ee744ee11f92f70fefcb1c48fe994daf))

## [1.10.6](https://github.com/alelk/pws-android/compare/v1.10.5...v1.10.6) (2024-12-30)


### Bug Fixes

* **database:** configure database migration for import/export feature ([#41](https://github.com/alelk/pws-android/issues/41)) ([fe183b2](https://github.com/alelk/pws-android/commit/fe183b20935993a543cc0db42068654e07948596))

## [1.10.5](https://github.com/alelk/pws-android/compare/v1.10.4...v1.10.5) (2024-12-18)


### Bug Fixes

* **share:** fix issue: copying wrong song from context menu ([#43](https://github.com/alelk/pws-android/issues/43)) ([af1114b](https://github.com/alelk/pws-android/commit/af1114b84bde242b7bb8cd26bf40f31a9b7afb2b))

## [1.10.4](https://github.com/alelk/pws-android/compare/v1.10.3...v1.10.4) (2024-10-18)


### Bug Fixes

* **database:** optimize build scripts ([de2694a](https://github.com/alelk/pws-android/commit/de2694a38ff7da9446912b691e087a0159c19357))

## [1.10.3](https://github.com/alelk/pws-android/compare/v1.10.2...v1.10.3) (2024-10-18)


### Bug Fixes

* **database:** database refactoring: remove redundant dao methods ([7a078ad](https://github.com/alelk/pws-android/commit/7a078ade9ffa7a62715653fa461d587c5d30c31e))

## [1.10.2](https://github.com/alelk/pws-android/compare/v1.10.1...v1.10.2) (2024-10-18)


### Bug Fixes

* **publishing:** fix maven publishing (include jvmJar artifact) ([d509d85](https://github.com/alelk/pws-android/commit/d509d858a7c540de140255706e06ace1e7a511f8))

## [1.10.1](https://github.com/alelk/pws-android/compare/v1.10.0...v1.10.1) (2024-10-18)


### Bug Fixes

* **publishing:** fix maven publishing ([f6c2885](https://github.com/alelk/pws-android/commit/f6c28852d3aa80781b803a6c4c761f190f55eadc))

# [1.10.0](https://github.com/alelk/pws-android/compare/v1.9.1...v1.10.0) (2024-10-18)


### Features

* **database:** implement jvm database ([48c073c](https://github.com/alelk/pws-android/commit/48c073c737c4b5e6fe389611bcb94d0f7952f4ff))

## [1.9.1](https://github.com/alelk/pws-android/compare/v1.9.0...v1.9.1) (2024-10-17)


### Bug Fixes

* **publish:** configure release publishing ([29ddea2](https://github.com/alelk/pws-android/commit/29ddea2b93f71c95c17dba09f052fc6eb99dd177))

# [1.9.0](https://github.com/alelk/pws-android/compare/v1.8.0...v1.9.0) (2024-10-17)


### Features

* **app:** reimplement books fragment using room dao ([4346070](https://github.com/alelk/pws-android/commit/4346070fe6e34985b59cae36b02fd9157aec8740))
* **app:** reimplement history and favorites fragment using room dao ([71ea5c2](https://github.com/alelk/pws-android/commit/71ea5c2ee01a640d7ece16c57eaecb5bfe025f74))
* **database:** implement room entities and daos ([a19c6c1](https://github.com/alelk/pws-android/commit/a19c6c1d4faf7d48bab22320cafa58ffbe5d6896))
