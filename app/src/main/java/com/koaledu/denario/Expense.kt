package com.koaledu.denario

import java.text.NumberFormat
import java.util.Locale

data class Expense(val amount: Double, val description: String) {

    override fun toString(): String {
        val format = NumberFormat.getCurrencyInstance(Locale.US)
        val formattedAmount = format.format(amount)

        return if (description.isNotBlank()) {
            "$formattedAmount - $description"
        } else {
            formattedAmount
        }
    }
}