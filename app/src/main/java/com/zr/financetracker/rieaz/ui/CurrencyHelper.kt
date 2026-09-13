package com.zr.financetracker.rieaz.ui

object CurrencyHelper {
    // Return BDT value of 1 unit of currency c
    fun getRateToBDT(currency: String): Double {
        return when (currency) {
            "£" -> 165.38
            "€" -> 142.52
            "$" -> 122.78
            "SAR" -> 32.70
            "MYR" -> 30.48
            else -> 1.0 // BDT "৳" or fallback
        }
    }

    // Convert from BDT to target currency
    fun convertFromBDT(amountInBDT: Double, targetCurrency: String): Double {
        val rate = getRateToBDT(targetCurrency)
        return amountInBDT / rate
    }

    // Convert from target currency to BDT
    fun convertToBDT(amountInTarget: Double, sourceCurrency: String): Double {
        val rate = getRateToBDT(sourceCurrency)
        return amountInTarget * rate
    }
}
