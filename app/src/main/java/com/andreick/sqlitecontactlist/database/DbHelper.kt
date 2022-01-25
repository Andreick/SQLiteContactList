package com.andreick.sqlitecontactlist.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.andreick.sqlitecontactlist.database.DbContract.ContactEntry
import android.provider.BaseColumns
import com.andreick.sqlitecontactlist.Contact

class DbHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "Contact.db"
        private const val SQL_DROP_CONTACT_TABLE = "DROP TABLE IF EXISTS ${ContactEntry.TABLE_NAME}"
        private const val SQL_CREATE_CONTACT_TABLE = "CREATE TABLE ${ContactEntry.TABLE_NAME} (" +
                "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                "${ContactEntry.COLUMN_NAME_NAME} TEXT NOT NULL," +
                "${ContactEntry.COLUMN_NAME_PHONE} TEXT NOT NULL)"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_CONTACT_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion != newVersion) {
            db?.execSQL(SQL_DROP_CONTACT_TABLE)
            onCreate(db)
        }
    }

    fun insertContact(name: String, phone: String): Long {
        val db = writableDatabase ?: return -1
        /*val sql = "INSERT INTO ${ContactEntry.TABLE_NAME} " +
                "(${ContactEntry.COLUMN_NAME_NAME}, ${ContactEntry.COLUMN_NAME_PHONE}) " +
                "VALUES (?, ?)"
        val args = arrayOf(name, phone)
        db.execSQL(sql, args)*/
        val values = ContentValues().apply {
            put(ContactEntry.COLUMN_NAME_NAME, name)
            put(ContactEntry.COLUMN_NAME_PHONE, phone)
        }
        return db.insert(ContactEntry.TABLE_NAME, null, values)
    }

    fun fetchAllContacts() : List<Contact> {
        val contactList = mutableListOf<Contact>()
        val db = readableDatabase ?: return contactList
        val sql = "SELECT * FROM ${ContactEntry.TABLE_NAME}"
        val cursor = db.rawQuery(sql, null) ?: return contactList
        with(cursor) {
            while (moveToNext()) {
                val contact = Contact(
                    getInt(getColumnIndexOrThrow(BaseColumns._ID)),
                    getString(getColumnIndexOrThrow(ContactEntry.COLUMN_NAME_NAME)),
                    getString(getColumnIndexOrThrow(ContactEntry.COLUMN_NAME_PHONE))
                )
                contactList.add(contact)
            }
            close()
        }
        return contactList
    }

    fun findContactByIdOrNull(id: Int): Contact? {
        var contact: Contact? = null
        val db = readableDatabase ?: return contact
        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf("$id")
        val cursor = db.query(
            ContactEntry.TABLE_NAME,
            null,
            selection, selectionArgs,
            null, null, null
        ) ?: return contact
        with(cursor) {
            if (moveToNext()) {
                contact = Contact(
                    getInt(getColumnIndexOrThrow(BaseColumns._ID)),
                    getString(getColumnIndexOrThrow(ContactEntry.COLUMN_NAME_NAME)),
                    getString(getColumnIndexOrThrow(ContactEntry.COLUMN_NAME_PHONE))
                )
            }
            close()
        }
        return contact
    }

    fun searchContacts(searchString: String) : List<Contact> {
        val contactList = mutableListOf<Contact>()
        val db = readableDatabase ?: return contactList
        /*val sql = "SELECT * FROM ${ContactEntry.TABLE_NAME} " +
                "WHERE ${ContactEntry.COLUMN_NAME_NAME} " +
                "LIKE ?"
        val args = arrayOf("%$searchString%")
        val cursor = db.rawQuery(sql, args) ?: return mutableListOf()*/
        val selection = "${ContactEntry.COLUMN_NAME_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$searchString%")
        val cursor = db.query(
            ContactEntry.TABLE_NAME,
            null,
            selection, selectionArgs,
            null, null, null
        ) ?: return contactList
        with(cursor) {
            while (moveToNext()) {
                val contact = Contact(
                    getInt(getColumnIndexOrThrow(BaseColumns._ID)),
                    getString(getColumnIndexOrThrow(ContactEntry.COLUMN_NAME_NAME)),
                    getString(getColumnIndexOrThrow(ContactEntry.COLUMN_NAME_PHONE))
                )
                contactList.add(contact)
            }
            close()
        }
        return contactList
    }

    fun updateContact(contact: Contact): Int {
        val db = writableDatabase ?: return 0
        val values = ContentValues().apply {
            put(ContactEntry.COLUMN_NAME_NAME, contact.name)
            put(ContactEntry.COLUMN_NAME_PHONE, contact.phone)
        }
        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf("${contact.id}")
        return db.update(
            ContactEntry.TABLE_NAME,
            values,
            selection, selectionArgs
        )
    }

    fun deleteContactById(id: Int): Int {
        val db = writableDatabase ?: return 0
        val selection = "${BaseColumns._ID} = ?"
        val selectionArgs = arrayOf("$id")
        return db.delete(ContactEntry.TABLE_NAME, selection, selectionArgs)
    }
}