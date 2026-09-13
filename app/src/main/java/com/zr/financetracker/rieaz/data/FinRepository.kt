package com.zr.financetracker.rieaz.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.max

class FinRepository(private val db: AppDatabase) {

    private val transactionDao = db.transactionDao()
    private val budgetDao = db.budgetDao()
    private val merchantBudgetDao = db.merchantBudgetDao()
    private val userProfileDao = db.userProfileDao()
    private val noteDao = db.noteDao()

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactionsFlow()
    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgetsFlow()
    val allMerchantBudgets: Flow<List<MerchantBudget>> = merchantBudgetDao.getAllMerchantBudgetsFlow()
    val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfileFlow()
    val allNotes: Flow<List<FinanceNote>> = noteDao.getAllNotesFlow()

    suspend fun saveNote(note: FinanceNote) {
        noteDao.insertNote(note)
    }

    suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    suspend fun clearAllNotes() {
        noteDao.clearAllNotes()
    }

    suspend fun getProfileDirect(): UserProfile? {
        return userProfileDao.getUserProfileDirect()
    }

    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.saveUserProfile(profile)
        // Refresh all status messages based on new language if language changed
        recalculateAllBudgetStatuses(profile.language)
    }

    suspend fun saveBudget(budget: Budget) {
        budgetDao.insertBudget(budget)
    }

    suspend fun saveMerchantBudget(mb: MerchantBudget) {
        merchantBudgetDao.insertMerchantBudget(mb)
    }

    suspend fun deleteMerchantBudget(merchant: String) {
        merchantBudgetDao.deleteMerchantBudget(merchant)
    }

    // Insert transaction and apply budget impact
    suspend fun insertTransaction(transaction: Transaction) {
        // First insert into DB
        transactionDao.insertTransaction(transaction)

        val profile = getProfileDirect()
        val lang = profile?.language ?: "en"

        if (transaction.type == "expense") {
            // Apply category budget impact
            updateCategoryBudgetImpact(transaction.category, transaction.amount, lang, isAddition = true)

            // Apply merchant budget impact
            transaction.merchant?.let { m ->
                if (m.isNotBlank()) {
                    updateMerchantBudgetImpact(m.trim(), transaction.amount, lang, isAddition = true)
                }
            }
        }
    }

    // Delete transaction and reverse budget impact
    suspend fun deleteTransaction(id: String) {
        val transaction = transactionDao.getTransactionById(id) ?: return
        transactionDao.deleteTransactionById(id)

        val profile = getProfileDirect()
        val lang = profile?.language ?: "en"

        if (transaction.type == "expense") {
            // Reverse category budget impact
            updateCategoryBudgetImpact(transaction.category, -transaction.amount, lang, isAddition = true)

            // Reverse merchant budget impact
            transaction.merchant?.let { m ->
                if (m.isNotBlank()) {
                    updateMerchantBudgetImpact(m.trim(), -transaction.amount, lang, isAddition = true)
                }
            }
        }
    }

    // Update/Edit transaction
    suspend fun updateTransaction(oldId: String, newTransaction: Transaction) {
        val oldTransaction = transactionDao.getTransactionById(oldId) ?: return

        val profile = getProfileDirect()
        val lang = profile?.language ?: "en"

        // 1. Reverse old impact if old was expense
        if (oldTransaction.type == "expense") {
            updateCategoryBudgetImpact(oldTransaction.category, -oldTransaction.amount, lang, isAddition = true)
            oldTransaction.merchant?.let { m ->
                if (m.isNotBlank()) {
                    updateMerchantBudgetImpact(m.trim(), -oldTransaction.amount, lang, isAddition = true)
                }
            }
        }

        // 2. Insert new transaction (replacing or writing with new ID)
        if (oldId != newTransaction.id) {
            transactionDao.deleteTransactionById(oldId)
        }
        transactionDao.insertTransaction(newTransaction)

        // 3. Apply new impact if new is expense
        if (newTransaction.type == "expense") {
            updateCategoryBudgetImpact(newTransaction.category, newTransaction.amount, lang, isAddition = true)
            newTransaction.merchant?.let { m ->
                if (m.isNotBlank()) {
                    updateMerchantBudgetImpact(m.trim(), newTransaction.amount, lang, isAddition = true)
                }
            }
        }
    }

    // Core helper for Category budget adjustments
    private suspend fun updateCategoryBudgetImpact(category: String, amountDiff: Double, lang: String, isAddition: Boolean) {
        val budget = budgetDao.getBudgetByCategory(category)
        if (budget != null) {
            val newSpent = max(0.0, budget.spentAmount + amountDiff)
            val newStatus = calculateStatusMessage(category, newSpent, budget.limitAmount, lang)
            val updatedBudget = budget.copy(spentAmount = newSpent, statusMessage = newStatus)
            budgetDao.insertBudget(updatedBudget)
        } else {
            // Safe fallback if category not found (unlikely as seeded, but for custom/Other)
            val defaultLimit = 2000.0
            val newSpent = max(0.0, amountDiff)
            val status = calculateStatusMessage(category, newSpent, defaultLimit, lang)
            budgetDao.insertBudget(
                Budget(category, defaultLimit, newSpent, category, status)
            )
        }
    }

    // Core helper for Merchant budget adjustments
    private suspend fun updateMerchantBudgetImpact(merchant: String, amountDiff: Double, lang: String, isAddition: Boolean) {
        val key = merchant.lowercase().trim()
        // We match merchant budgets case-insensitively. Let's find matches.
        val budgets = merchantBudgetDao.getAllMerchantBudgetsDirect()
        val matchedBudget = budgets.find { it.merchant.lowercase().trim() == key }

        if (matchedBudget != null) {
            val newSpent = max(0.0, matchedBudget.spentAmount + amountDiff)
            val newStatus = calculateMerchantStatusMessage(matchedBudget.merchant, newSpent, matchedBudget.limitAmount, lang)
            val updated = matchedBudget.copy(spentAmount = newSpent, statusMessage = newStatus)
            merchantBudgetDao.insertMerchantBudget(updated)
        } else {
            // Even if there is no explicit limit set yet, we don't automatically create a merchant budget ceiling
            // UNLESS the user already initialized it, OR if we want to track it with a default limit of 0.0 or unlimited.
            // Requirement says: "Active Budgets Summary: Shows all category/merchant budgets that have a limit set."
            // "Merchant Budgets List: Same structure as Category Budgets but for merchant names. Users can add merchant budgets from Settings screen."
            // So if a limit is not set, we don't generate the budget status or card on the dashboard. But if it is set, we adjust its spentAmount.
        }
    }

    // Recalculates all budget statuses (called when language toggles)
    private suspend fun recalculateAllBudgetStatuses(lang: String) {
        val cBudgets = budgetDao.getAllBudgetsDirect()
        for (b in cBudgets) {
            val newStatus = calculateStatusMessage(b.category, b.spentAmount, b.limitAmount, lang)
            budgetDao.insertBudget(b.copy(statusMessage = newStatus))
        }

        val mBudgets = merchantBudgetDao.getAllMerchantBudgetsDirect()
        for (m in mBudgets) {
            val newStatus = calculateMerchantStatusMessage(m.merchant, m.spentAmount, m.limitAmount, lang)
            merchantBudgetDao.insertMerchantBudget(m.copy(statusMessage = newStatus))
        }
    }

    suspend fun factoryReset() {
        transactionDao.clearAllTransactions()
        merchantBudgetDao.clearAllMerchantBudgets()
        
        // Reset category budgets spent to 0.0 with statuses
        val currentProfile = getProfileDirect()
        val lang = currentProfile?.language ?: "en"
        val cBudgets = budgetDao.getAllBudgetsDirect()
        for (b in cBudgets) {
            val status = calculateStatusMessage(b.category, 0.0, b.limitAmount, lang)
            budgetDao.insertBudget(b.copy(spentAmount = 0.0, statusMessage = status))
        }
    }

    suspend fun wipeAllTransactions() {
        transactionDao.clearAllTransactions()
        
        val currentProfile = getProfileDirect()
        val lang = currentProfile?.language ?: "en"

        // Reset category spending to 0
        val cBudgets = budgetDao.getAllBudgetsDirect()
        for (b in cBudgets) {
            val status = calculateStatusMessage(b.category, 0.0, b.limitAmount, lang)
            budgetDao.insertBudget(b.copy(spentAmount = 0.0, statusMessage = status))
        }

        // Reset merchant spending to 0
        val mBudgets = merchantBudgetDao.getAllMerchantBudgetsDirect()
        for (m in mBudgets) {
            val status = calculateMerchantStatusMessage(m.merchant, 0.0, m.limitAmount, lang)
            merchantBudgetDao.insertMerchantBudget(m.copy(spentAmount = 0.0, statusMessage = status))
        }
    }

    // Perform database import/restore
    suspend fun importBackupData(
        transactions: List<Transaction>,
        budgets: List<Budget>,
        merchantBudgets: List<MerchantBudget>,
        userProfile: UserProfile?,
        notes: List<FinanceNote> = emptyList()
    ) {
        // Clear all current data first
        transactionDao.clearAllTransactions()
        budgetDao.clearAllBudgets()
        merchantBudgetDao.clearAllMerchantBudgets()
        userProfileDao.clearUserProfile()
        if (notes.isNotEmpty()) {
            noteDao.clearAllNotes()
            for (n in notes) {
                noteDao.insertNote(n)
            }
        }

        // Insert new data from backup
        for (t in transactions) {
            transactionDao.insertTransaction(t)
        }
        budgetDao.insertBudgets(budgets)
        for (mb in merchantBudgets) {
            merchantBudgetDao.insertMerchantBudget(mb)
        }
        if (userProfile != null) {
            userProfileDao.saveUserProfile(userProfile)
        } else {
            // default
            userProfileDao.saveUserProfile(
                UserProfile(id = 1, "User", "৳", "en", true, true)
            )
        }

        // final recalculation of budget status messages based on active language
        val currentLang = userProfile?.language ?: "en"
        recalculateAllBudgetStatuses(currentLang)
    }

    companion object {
        fun translateCategory(category: String, toBn: Boolean): String {
            if (!toBn) return category
            return when (category.lowercase().trim()) {
                "food" -> "খাবার ও রেস্তোরাঁ"
                "rent" -> "বাড়িভাড়া"
                "transport" -> "পরিবহন ও যাতায়াত"
                "health" -> "চিকিৎসা ও স্বাস্থ্য"
                "education" -> "শিক্ষা ও পড়াশোনা"
                "entertainment" -> "বিনোদন"
                "utility" -> "ইউটিলিটি বিল"
                "shopping" -> "কেনাকাটা ও শপিং"
                "income" -> "আয় ও উপার্জিত বেতন"
                "other" -> "অন্যান্য খরচ"
                else -> category
            }
        }

        fun calculateStatusMessage(category: String, spent: Double, limit: Double, language: String): String {
            val isBn = language == "bn"
            val ratio = if (limit > 0) spent / limit else 0.0
            return when {
                spent > limit -> {
                    if (isBn) "আপনি আপনার ${translateCategory(category, true)} বাজেটসীমা অতিক্রম করেছেন!"
                    else "You have exceeded your $category limit!"
                }
                ratio > 0.8 -> {
                    if (isBn) "সতর্ক থাকুন, ${translateCategory(category, true)} বাজেটসীমা প্রায় পূর্ণ"
                    else "Careful, spending on $category is almost exceeded"
                }
                else -> {
                    if (isBn) "আপনার ${translateCategory(category, true)} খরচ নিয়ন্ত্রণে রয়েছে"
                    else "Your $category spending is on track"
                }
            }
        }

        fun calculateMerchantStatusMessage(merchant: String, spent: Double, limit: Double, language: String): String {
            val isBn = language == "bn"
            val ratio = if (limit > 0) spent / limit else 0.0
            return when {
                spent > limit -> {
                    if (isBn) "আপনি $merchant-এ বাজেটসীমা অতিক্রম করেছেন!"
                    else "You have exceeded your $merchant limit!"
                }
                ratio > 0.8 -> {
                    if (isBn) "সতর্ক থাকুন, $merchant-এ বাজেটসীমা প্রায় পূর্ণ"
                    else "Careful, almost at your $merchant limit"
                }
                else -> {
                    if (isBn) "$merchant-এ আপনার খরচ নিয়ন্ত্রণে রয়েছে"
                    else "Your spending at $merchant is on track"
                }
            }
        }
    }
}
