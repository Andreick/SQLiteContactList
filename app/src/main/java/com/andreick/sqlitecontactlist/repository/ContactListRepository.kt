package com.andreick.sqlitecontactlist.repository

import android.database.sqlite.SQLiteException
import android.provider.BaseColumns
import com.andreick.sqlitecontactlist.Contact
import com.andreick.sqlitecontactlist.base.BaseRepository
import com.andreick.sqlitecontactlist.database.DbContract
import com.andreick.sqlitecontactlist.database.DbHelper

open class ContactListRepository(dbHelper: DbHelper? = null) : BaseRepository(dbHelper) {

    open fun searchContacts(
        searchString: String,
        onSuccess: (List<Contact>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        try {
            val db = readableDatabase ?: throw SQLiteException("Null readable database")
            /*val sql = "SELECT * FROM ${ContactEntry.TABLE_NAME} " +
                    "WHERE ${ContactEntry.COLUMN_NAME_NAME} " +
                    "LIKE ?"
            val args = arrayOf("%$searchString%")
            val cursor = db.rawQuery(sql, args) ?: return mutableListOf()*/
            val selection = "${DbContract.ContactEntry.COLUMN_NAME_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$searchString%")
            val cursor = db.query(
                DbContract.ContactEntry.TABLE_NAME,
                null,
                selection, selectionArgs,
                null, null, null
            ) ?: throw SQLiteException("Null cursor")
            val contactList = mutableListOf<Contact>()
            with(cursor) {
                while (moveToNext()) {
                    val contact = Contact(
                        getInt(getColumnIndexOrThrow(BaseColumns._ID)),
                        getString(getColumnIndexOrThrow(DbContract.ContactEntry.COLUMN_NAME_NAME)),
                        getString(getColumnIndexOrThrow(DbContract.ContactEntry.COLUMN_NAME_PHONE))
                    )
                    contactList.add(contact)
                }
                close()
            }
            onSuccess(contactList)
        } catch (e: SQLiteException) {
            onFailure(e)
        }
    }
}