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
 * Pws Book Statistic Table
 *
 * Created by Alex Elkin on 17.03.2016.
 */
object PwsBookStatisticTable : PwsTable {
    const val TABLE_BOOKSTATISTIC = "bookstatistic"
    const val COLUMN_ID = "_id"
    const val COLUMN_BOOKID = "bookid"
    const val COLUMN_USERPREFERENCE = "userpref"
    const val COLUMN_READINGS = "readings"
    const val COLUMN_RATING = "rating"
}