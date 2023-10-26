package com.example.diary

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBhelper(context: Context, factory: SQLiteDatabase.CursorFactory?) :
    SQLiteOpenHelper(context, DATABASE_NAME, factory, DATABASE_VERSION) {

    // below is the method for creating a database by a sqlite query
    override fun onCreate(db: SQLiteDatabase) {
        // below is a sqlite query, where column names
        // along with their data types is given
        val query = ("CREATE TABLE " + TABLE_NAME + " (" +
                ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                DATE_COL + " TEXT NOT NULL UNIQUE," +
                IMG_COL + " TEXT," +
                DIARY_COL + " TEXT" + ")")

        // we are calling sqlite
        // method for executing our query
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, p1: Int, p2: Int) {
        // this method is to check if table already exists
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // This method is for adding data in our database
    fun addDiary(date: String, img: ByteArray, diary: String ){

        // below we are creating
        // a content values variable
        val values = ContentValues()

        // we are inserting our values
        // in the form of key-value pair
        values.put(DATE_COL, date)
        values.put(IMG_COL, img)
        values.put(DIARY_COL, diary)
        
        // here we are creating a
        // writable variable of
        // our database as we want to
        // insert value in our database
        val db = this.writableDatabase

        // all values are inserted into database
        db.insert(TABLE_NAME, null, values)

        // at last we are
        // closing our database
        db.close()
    }

    // below method is to get
    // all data from our database
    fun getData(date: String): Cursor? {

        // here we are creating a readable
        // variable of our database
        // as we want to read value from it
        val db = this.readableDatabase

        // below code returns a cursor to
        // read data from the database

        return db.rawQuery("SELECT * FROM $TABLE_NAME where $DATE_COL = '$date'", null)
    }

    fun getDatabase(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    fun update(date: String, img: ByteArray, diary: String ) {
        val db = this.readableDatabase
        val values = ContentValues()

        // we are inserting our values
        // in the form of key-value pair
        values.put(DATE_COL, date)
        values.put(IMG_COL, img)
        values.put(DIARY_COL, diary)

        db.update(TABLE_NAME, values, "date=?", arrayOf(date))
        db.close()
    }

    fun removeRow(date: String){
        val db = this.readableDatabase
        db.delete(TABLE_NAME, "date=?", arrayOf(date))
        db.close()
    }

    companion object{
        // here we have defined variables for our database

        // below is variable for database name
        private val DATABASE_NAME = "DIARY_DATABASE"

        // below is the variable for database version
        private val DATABASE_VERSION = 1

        // below is the variable for table name
        val TABLE_NAME = "diary_table"

        val ID_COL = "id"

        // below is the variable for date column
        val DATE_COL = "date"

        // below is the variable for image column
        val IMG_COL = "img"

        // below is the variable for diary column
        val DIARY_COL = "diary"
    }
}