package com.andreick.sqlitecontactlist.base

import android.database.sqlite.SQLiteDatabase
import com.andreick.sqlitecontactlist.database.DbHelper

open class BaseRepository(var dbHelper: DbHelper? = null) {
    val readableDatabase: SQLiteDatabase?
        get() = dbHelper?.readableDatabase

    val writableDatabase: SQLiteDatabase?
        get() = dbHelper?.writableDatabase
}