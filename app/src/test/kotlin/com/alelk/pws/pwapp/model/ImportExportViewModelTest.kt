package com.alelk.pws.pwapp.model

import com.alelk.pws.database.dao.Favorite
import com.alelk.pws.database.dao.SongDetails
import com.alelk.pws.pwapp.theme.AppTheme
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.alelk.pws.domain.model.Color
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

class ImportExportViewModelTest : FunSpec({
    val mapper = jacksonObjectMapper()

    test("export data should include version field") {
        val exportData = ExportData(
            preferences = Preferences(
                songTextSize = 16f,
                songTextExpanded = true,
                appTheme = AppTheme.LIGHT,
                booksPreferences = listOf(
                    BookPreference(
                        bookName = "Test Book",
                        bookShortName = "TB",
                        preference = 1
                    )
                )
            ),
            favorites = listOf(
                Favorite(
                    id = 1,
                    songNumber = 1,
                    songName = "Test Song",
                    bookDisplayName = "Test Book",
                    songNumberId = 1,
                    bookShortName = "TB"
                )
            ),
            categories = listOf(
                CategoryDetails(
                    name = "Test Category",
                    color = Color(255, 0, 0),
                    songs = listOf(
                        SongDetails(
                            bookName = "Test Book",
                            bookShortName = "TB",
                            songNumber = 1,
                            songName = "Test Song",
                            songText = "Test lyrics",
                            songTonality = "C",
                            bibleReferences = "John 3:16"
                        )
                    )
                )
            ),
            editedSongs = listOf(
                SongDetails(
                    bookName = "Test Book",
                    bookShortName = "TB",
                    songNumber = 1,
                    songName = "Test Song",
                    songText = "Test lyrics",
                    songTonality = "C",
                    bibleReferences = "John 3:16"
                )
            )
        )

        val json = mapper.writeValueAsString(exportData)
        val parsedData = mapper.readValue(json, ExportData::class.java)

        parsedData.version shouldBe 1
    }

    test("should be able to parse json without version field") {
        val jsonWithoutVersion = """
            {
                "preferences": {
                    "songTextSize": 16.0,
                    "songTextExpanded": true,
                    "appTheme": "LIGHT",
                    "booksPreferences": [{
                        "bookName": "Test Book",
                        "bookShortName": "TB",
                        "preference": 1
                    }]
                },
                "favorites": [{
                    "id": 1,
                    "songNumber": 1,
                    "songName": "Test Song",
                    "bookDisplayName": "Test Book",
                    "songNumberId": 1,
                    "bookShortName": "TB"
                }],
                "categories": null,
                "editedSongs": null
            }
        """.trimIndent()

        val parsedData = mapper.readValue(jsonWithoutVersion, ExportData::class.java)

        parsedData shouldNotBe null
        parsedData.version shouldBe 1
        parsedData.preferences.songTextSize shouldBe 16.0f
        parsedData.preferences.songTextExpanded shouldBe true
        parsedData.preferences.appTheme shouldBe AppTheme.LIGHT
        parsedData.favorites?.size shouldBe 1
        parsedData.categories shouldBe null
        parsedData.editedSongs shouldBe null
    }

    test("exported json should have correct format and include all fields") {
        val exportData = ExportData(
            version = 1,
            preferences = Preferences(
                songTextSize = 16f,
                songTextExpanded = true,
                appTheme = AppTheme.LIGHT,
                booksPreferences = listOf(
                    BookPreference(
                        bookName = "Test Book",
                        bookShortName = "TB",
                        preference = 1
                    )
                )
            ),
            favorites = listOf(
                Favorite(
                    id = 1,
                    songNumber = 1,
                    songName = "Test Song",
                    bookDisplayName = "Test Book",
                    songNumberId = 1,
                    bookShortName = "TB"
                )
            ),
            categories = listOf(
                CategoryDetails(
                    name = "Test Category",
                    color = Color(255, 0, 0),
                    songs = listOf(
                        SongDetails(
                            bookName = "Test Book",
                            bookShortName = "TB",
                            songNumber = 1,
                            songName = "Test Song",
                            songText = "Test lyrics",
                            songTonality = "C",
                            bibleReferences = "John 3:16"
                        )
                    )
                )
            ),
            editedSongs = listOf(
                SongDetails(
                    bookName = "Test Book",
                    bookShortName = "TB",
                    songNumber = 1,
                    songName = "Test Song",
                    songText = "Test lyrics",
                    songTonality = "C",
                    bibleReferences = "John 3:16"
                )
            )
        )

        val json = mapper.writeValueAsString(exportData)

        // Check that all required fields are present
        json shouldContain """"version":1"""
        json shouldContain """"songTextSize":16.0"""
        json shouldContain """"songTextExpanded":true"""
        json shouldContain """"appTheme":"LIGHT""""
        json shouldContain """"bookName":"Test Book""""
        json shouldContain """"bookShortName":"TB""""
        json shouldContain """"preference":1"""
        json shouldContain """"songNumber":1"""
        json shouldContain """"songName":"Test Song""""
        json shouldContain """"bookDisplayName":"Test Book""""
        json shouldContain """"songNumberId":1"""
        json shouldContain """"name":"Test Category""""
        json shouldContain """"r":255,"g":0,"b":0"""
        json shouldContain """"songText":"Test lyrics""""
        json shouldContain """"songTonality":"C""""
        json shouldContain """"bibleReferences":"John 3:16""""

        // Verify that the JSON can be parsed back correctly
        val parsedData = mapper.readValue(json, ExportData::class.java)
        parsedData.version shouldBe 1
        parsedData.preferences.songTextSize shouldBe 16.0f
        parsedData.preferences.songTextExpanded shouldBe true
        parsedData.preferences.appTheme shouldBe AppTheme.LIGHT
        parsedData.favorites?.size shouldBe 1
        parsedData.categories?.size shouldBe 1
        parsedData.editedSongs?.size shouldBe 1

        // Verify color serialization
        val category = parsedData.categories?.first()
        category?.color?.r shouldBe 255
        category?.color?.g shouldBe 0
        category?.color?.b shouldBe 0
    }

    test("should parse real JSON example correctly") {
        val realJson = """
            {
                "preferences": {
                    "songTextSize": 27.1,
                    "songTextExpanded": true,
                    "appTheme": "LIGHT",
                    "booksPreferences": [
                        {
                            "bookName": "Песнь Возрождения 3300",
                            "bookShortName": "ПВ-3300",
                            "preference": 40
                        }
                    ]
                },
                "favorites": [
                    {
                        "id": 1,
                        "songNumber": 2005,
                        "songName": "Благо есть славить Господа",
                        "bookDisplayName": "Песнь Возрождения 3300",
                        "songNumberId": 6210,
                        "bookShortName": "ПВ-3300"
                    }
                ],
                "categories": [
                    {
                        "name": "Liza",
                        "color": {
                            "r": 219,
                            "g": 29,
                            "b": 226
                        },
                        "songs": [
                            {
                                "bookName": "Песнь Возрождения 3055",
                                "bookShortName": "ПВ-3055",
                                "songNumber": 2532,
                                "songName": "Научи меня мой Господь во всем",
                                "songText": null,
                                "songTonality": null,
                                "bibleReferences": null
                            }
                        ]
                    }
                ],
                "editedSongs": [
                    {
                        "bookName": "Песнь Возрождения 3300",
                        "bookShortName": "ПВ-3300",
                        "songNumber": 190,
                        "songName": "Обетованья всегда пребудут",
                        "songText": "1.\nОбетованья всегда пребудут...",
                        "songTonality": "g major",
                        "bibleReferences": ""
                    }
                ]
            }
        """.trimIndent()

        val exportData = jacksonObjectMapper().readValue(realJson, ExportData::class.java)

        // Verify preferences
        exportData.preferences.songTextSize shouldBe 27.1f
        exportData.preferences.songTextExpanded shouldBe true
        exportData.preferences.appTheme shouldBe AppTheme.LIGHT
        exportData.preferences.booksPreferences?.size shouldBe 1
        exportData.preferences.booksPreferences?.first()?.let {
            it.bookName shouldBe "Песнь Возрождения 3300"
            it.bookShortName shouldBe "ПВ-3300"
            it.preference shouldBe 40
        }

        // Verify favorites
        exportData.favorites?.size shouldBe 1
        exportData.favorites?.first()?.let {
            it.id shouldBe 1
            it.songNumber shouldBe 2005
            it.songName shouldBe "Благо есть славить Господа"
            it.bookDisplayName shouldBe "Песнь Возрождения 3300"
            it.songNumberId shouldBe 6210
            it.bookShortName shouldBe "ПВ-3300"
        }

        // Verify categories
        exportData.categories?.size shouldBe 1
        exportData.categories?.first()?.let {
            it.name shouldBe "Liza"
            it.color.r shouldBe 219
            it.color.g shouldBe 29
            it.color.b shouldBe 226
            it.songs.size shouldBe 1
            it.songs.first().let { song ->
                song.bookName shouldBe "Песнь Возрождения 3055"
                song.bookShortName shouldBe "ПВ-3055"
                song.songNumber shouldBe 2532
                song.songName shouldBe "Научи меня мой Господь во всем"
                song.songText shouldBe null
                song.songTonality shouldBe null
                song.bibleReferences shouldBe null
            }
        }

        // Verify edited songs
        exportData.editedSongs?.size shouldBe 1
        exportData.editedSongs?.first()?.let {
            it.bookName shouldBe "Песнь Возрождения 3300"
            it.bookShortName shouldBe "ПВ-3300"
            it.songNumber shouldBe 190
            it.songName shouldBe "Обетованья всегда пребудут"
            it.songTonality shouldBe "g major"
            it.bibleReferences shouldBe ""
            it.songText?.startsWith("1.\nОбетованья всегда пребудут") shouldBe true
        }
    }
}) 