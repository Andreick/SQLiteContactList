package com.andreick.sqlitecontactlist.database

import android.provider.BaseColumns

object DbContract {

    object ContactEntry : BaseColumns {
        const val TABLE_NAME = "contacts"
        const val COLUMN_NAME_NAME = "name"
        const val COLUMN_NAME_PHONE = "phone"
    }
}