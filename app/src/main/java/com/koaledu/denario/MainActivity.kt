package com.koaledu.denario

import android.content.Context
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var totalBalanceTextView: TextView
    private lateinit var amountEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var subtractButton: Button
    private lateinit var expenseHistoryListView: ListView
    private lateinit var expenseAdapter: ArrayAdapter<Expense>

    private lateinit var dbHelper: DatabaseHelper
    private val prefsName = "DenarioPrefs"
    private val balanceKey = "currentBalance"
    private val firstRunKey = "isFirstRun"

    private var currentBalance: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        totalBalanceTextView = findViewById(R.id.totalBalanceTextView)
        amountEditText = findViewById(R.id.amountEditText)
        descriptionEditText = findViewById(R.id.descriptionEditText)
        subtractButton = findViewById(R.id.subtractButton)
        expenseHistoryListView = findViewById(R.id.expenseHistoryListView)

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
        refreshExpenseList()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_reset -> {
                showResetConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showResetConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("¿Borrar datos?")
        builder.setMessage("¿Está seguro(a) de borrar su historial y su dinero? Esta acción no se puede revertir.")

        builder.setPositiveButton("Borrar") { _, _ ->
            performReset()
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun performReset() {
        dbHelper.deleteAllExpenses()

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()

        loadBalance()
        updateBalanceDisplay()
        refreshExpenseList()

        checkForFirstRun()

        Toast.makeText(this, "Los datos han sido borrados", Toast.LENGTH_SHORT).show()
    }

    private fun refreshExpenseList() {
        val expenses = dbHelper.getAllExpenses()
        expenseAdapter.clear()
        expenseAdapter.addAll(expenses)
        expenseAdapter.notifyDataSetChanged()
    }

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
            Toast.makeText(this, "Por favor ingrese una cantidad", Toast.LENGTH_SHORT).show()
            return
        }

        val amountToSubtract = amountStr.toDoubleOrNull()
        if (amountToSubtract == null || amountToSubtract <= 0) {
            Toast.makeText(this, "Por favor ingrese una cantidad válida positiva", Toast.LENGTH_SHORT).show()
            return
        }

        dbHelper.addExpense(amountToSubtract, description)

        val newBalance = currentBalance - amountToSubtract
        saveBalance(newBalance)

        updateBalanceDisplay()
        amountEditText.text.clear()
        descriptionEditText.text.clear()
        Toast.makeText(this, "¡Gasto guardado!", Toast.LENGTH_SHORT).show()
        refreshExpenseList()
    }

    private fun checkForFirstRun() {
        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean(firstRunKey, true)
        if (isFirstRun) {
            showSetInitialBalanceDialog()
        }
    }

    private fun showSetInitialBalanceDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Establezca su dinero")
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        input.hint = "Dinero total"
        builder.setView(input)
        builder.setPositiveButton("Aceptar") { _, _ ->
            val amountStr = input.text.toString()
            val initialAmount = amountStr.toDoubleOrNull() ?: 0.0
            saveBalance(initialAmount)
            updateBalanceDisplay()
            val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(firstRunKey, false).apply()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(firstRunKey, false).apply()
            dialog.cancel()
        }
        builder.setCancelable(false)
        builder.show()
    }
}