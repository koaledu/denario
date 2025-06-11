// app/src/main/java/com/koaledu/denario/MainActivity.kt
package com.koaledu.denario

import android.content.Context
import android.os.Bundle
import android.text.InputType
// Import ArrayAdapter and ListView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var totalBalanceTextView: TextView
    private lateinit var amountEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var subtractButton: Button
    // NEW: Add ListView and its Adapter
    private lateinit var expenseHistoryListView: ListView
    private lateinit var expenseAdapter: ArrayAdapter<Expense>

    // Database and Preferences
    private lateinit var dbHelper: DatabaseHelper
    private val prefsName = "DenarioPrefs"
    private val balanceKey = "currentBalance"
    private val firstRunKey = "isFirstRun"

    private var currentBalance: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        // Find existing views
        totalBalanceTextView = findViewById(R.id.totalBalanceTextView)
        amountEditText = findViewById(R.id.amountEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        subtractButton = findViewById(R.id.subtractButton)
        // NEW: Find the ListView
        expenseHistoryListView = findViewById(R.id.expenseHistoryListView)

        // NEW: Initialize the adapter for the list
        // It uses a simple, built-in layout for each row.
        expenseAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, ArrayList<Expense>())
        expenseHistoryListView.adapter = expenseAdapter

        subtractButton.setOnClickListener {
            subtractExpense()
        }
    }

    override fun onResume() {
        super.onResume()
        loadBalance()
        checkForFirstRun()
        updateBalanceDisplay()
        // NEW: Refresh the history list every time the app is opened
        refreshExpenseList()
    }

    // NEW: Function to fetch data and update the ListView
    private fun refreshExpenseList() {
        // Get all expenses from the database
        val expenses = dbHelper.getAllExpenses()
        // Clear the adapter of any old data
        expenseAdapter.clear()
        // Add all the new data
        expenseAdapter.addAll(expenses)
        // Notify the adapter to refresh the UI
        expenseAdapter.notifyDataSetChanged()
    }

    // ... (keep the other functions: checkForFirstRun, showSetInitialBalanceDialog, etc.) ...

    // --- The rest of the file remains the same until subtractExpense() ---

    private fun loadBalance() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val defaultBalanceBits = java.lang.Double.doubleToRawLongBits(0.0)
        currentBalance = java.lang.Double.longBitsToDouble(prefs.getLong(balanceKey, defaultBalanceBits))
    }

    private fun saveBalance(newBalance: Double) {
        currentBalance = newBalance
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong(balanceKey, java.lang.Double.doubleToRawLongBits(currentBalance))
        editor.apply()
    }

    private fun updateBalanceDisplay() {
        totalBalanceTextView.text = String.format(Locale.US, "$%.2f", currentBalance)
    }

    private fun subtractExpense() {
        val amountStr = amountEditText.text.toString()
        val description = descriptionEditText.text.toString()

        if (amountStr.isBlank()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
            return
        }

        val amountToSubtract = amountStr.toDoubleOrNull()
        if (amountToSubtract == null || amountToSubtract <= 0) {
            Toast.makeText(this, "Please enter a valid, positive amount", Toast.LENGTH_SHORT).show()
            return
        }

        dbHelper.addExpense(amountToSubtract, description)

        val newBalance = currentBalance - amountToSubtract
        saveBalance(newBalance)

        updateBalanceDisplay()

        amountEditText.text.clear()
        descriptionEditText.text.clear()

        Toast.makeText(this, "Expense recorded!", Toast.LENGTH_SHORT).show()

        // NEW: Refresh the list immediately after adding a new expense
        refreshExpenseList()
    }

    // The functions checkForFirstRun and showSetInitialBalanceDialog don't need changes.
    // Just make sure they are still in your file.
    private fun checkForFirstRun() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean(firstRunKey, true)
        if (isFirstRun) {
            showSetInitialBalanceDialog()
            prefs.edit().putBoolean(firstRunKey, false).apply()
        }
    }
    private fun showSetInitialBalanceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Your Initial Balance")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Enter total money you have"
        builder.setView(input)
        builder.setPositiveButton("OK") { _, _ ->
            val amountStr = input.text.toString()
            val initialAmount = amountStr.toDoubleOrNull() ?: 0.0
            saveBalance(initialAmount)
            updateBalanceDisplay()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        builder.setCancelable(false)
        builder.show()
    }
}