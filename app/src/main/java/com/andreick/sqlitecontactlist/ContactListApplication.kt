package com.andreick.sqlitecontactlist

import android.app.Application
import com.andreick.sqlitecontactlist.database.DbHelper

class ContactListApplication : Application() {

    var dbHelper: DbHelper? = null
        private set

    companion object {
        lateinit var instance: ContactListApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        dbHelper = DbHelper(this)
    }
}