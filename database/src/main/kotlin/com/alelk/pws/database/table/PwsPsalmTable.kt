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
 * Pws Psalm Table
 *
 * Created by Alex Elkin on 21.04.2015.
 */
@Deprecated("use room db")
object PwsPsalmTable : PwsTable {
  const val TABLE_PSALMS = "psalms"
  const val COLUMN_ID = "_id"
  const val COLUMN_NAME = "name"
  const val COLUMN_AUTHOR = "author"
  const val COLUMN_TRANSLATOR = "translator"
  const val COLUMN_COMPOSER = "composer"
  const val COLUMN_ANNOTATION = "bibleref"
  const val COLUMN_TEXT = "text"
}