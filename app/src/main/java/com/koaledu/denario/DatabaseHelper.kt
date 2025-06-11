package com.koaledu.denario

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    // Companion object to hold database constants
    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "Denario.db"

        // Table and Column Names
        const val TABLE_GASTOS = "Gasto"
        const val COL_ID = "id_gasto"
        const val COL_MONTO = "monto"
        const val COL_DESCRIPCION = "descripcion"
        const val COL_TIMESTAMP = "timestamp" // To know when the expense was made
    }

    // This is called the first time a database is accessed.
    // We create our tables here.
    override fun onCreate(db: SQLiteDatabase) {
        val createTableStatement = "CREATE TABLE $TABLE_GASTOS (" +
                "$COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COL_MONTO REAL NOT NULL CHECK($COL_MONTO >= 0), " +
                "$COL_DESCRIPCION TEXT, " +
                "$COL_TIMESTAMP INTEGER NOT NULL)"
        db.execSQL(createTableStatement)
    }

    // This is called if the database version number changes.
    // It prevents users' apps from breaking when you change the database design.
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GASTOS")
        onCreate(db)
    }

    // Function to add a new expense to the database
    fun addExpense(amount: Double, description: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_MONTO, amount)
            put(COL_DESCRIPCION, description)
            put(COL_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_GASTOS, null, values)
        db.close()
    }

    fun getAllExpenses(): List<Expense> {
        val expenseList = ArrayList<Expense>()
        val db = this.readableDatabase

        // Query to get all expenses, ordered by the newest first
        val query = "SELECT * FROM $TABLE_GASTOS ORDER BY $COL_ID DESC"
        val cursor = db.rawQuery(query, null)

        if (cursor.moveToFirst()) {
            do {
                val amountIndex = cursor.getColumnIndex(COL_MONTO)
                val descriptionIndex = cursor.getColumnIndex(COL_DESCRIPCION)

                // Check if columns exist to prevent errors
                if (amountIndex != -1 && descriptionIndex != -1) {
                    val amount = cursor.getDouble(amountIndex)
                    val description = cursor.getString(descriptionIndex)
                    val expense = Expense(amount, description)
                    expenseList.add(expense)
                }
            } while (cursor.moveToNext())
        }

        // Clean up
        cursor.close()
        db.close()
        return expenseList
    }
}