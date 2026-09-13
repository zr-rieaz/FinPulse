package com.zr.financetracker.rieaz.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupContainer(
    val version: String,
    val exportedAt: String,
    val transactions: List<Transaction>,
    val budgets: List<Budget>,
    val merchantBudgets: List<MerchantBudget>,
    val userProfile: UserProfile?,
    val notes: List<FinanceNote> = emptyList()
)
