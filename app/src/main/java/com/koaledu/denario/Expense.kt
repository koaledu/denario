// app/src/main/java/com/koaledu/denario/Expense.kt
package com.koaledu.denario

import java.text.NumberFormat
import java.util.Locale

// A simple class to represent one expense record from the database.
data class Expense(val amount: Double, val description: String) {

    // This function controls how the object is displayed in the ListView.
    // We override it to show a nicely formatted string.
    override fun toString(): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US) // e.g., $1,234.56
        val formattedAmount = format.format(amount)

        return if (description.isNotBlank()) {
            "$formattedAmount - $description"
        } else {
            formattedAmount
        }
    }
}