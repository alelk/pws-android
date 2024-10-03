/*
 * Copyright (C) 2018 The P&W Songs Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alelk.pws.database.table

/**
 * Pws Book Table
 *
 * Created by Alex Elkin on 22.04.2015.
 */
@Deprecated("use room db")
object PwsBookTable : PwsTable {
  const val TABLE_BOOKS = "books"
  const val COLUMN_ID = "_id"
  const val COLUMN_VERSION = "version"
  const val COLUMN_NAME = "name"
  const val COLUMN_DISPLAYSHORTNAME = "displayshortname"
  const val COLUMN_DISPLAYNAME = "displayname"
  const val COLUMN_EDITION = "edition"
  const val COLUMN_RELEASEDATE = "releasedate"
  const val COLUMN_AUTHORS = "authors"
  const val COLUMN_CREATORS = "creators"
  const val COLUMN_REVIEWERS = "reviewers"
  const val COLUMN_EDITORS = "editors"
  const val COLUMN_DESCRIPTION = "description"
  const val COLUMN_LOCALE = "locale"
}