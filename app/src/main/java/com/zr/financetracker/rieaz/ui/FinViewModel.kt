package com.zr.financetracker.rieaz.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zr.financetracker.rieaz.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = FinRepository(db)

    // SharedPreferences for lightweight settings (Theme, first-launch backup)
    private val prefs = application.getSharedPreferences("fintrack_prefs", Context.MODE_PRIVATE)

    // Global selected month state (shared across Home & Reports)
    // Default selected month = current calendar month on launch
    private val _selectedMonth = MutableStateFlow(getCurrentMonthEnglish())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    private val _selectedYear = MutableStateFlow(getCurrentYearString())
    val selectedYear: StateFlow<String> = _selectedYear.asStateFlow()

    // Dashboard toggle: Expense | Income
    private val _dashboardToggleType = MutableStateFlow("expense")
    val dashboardToggleType: StateFlow<String> = _dashboardToggleType.asStateFlow()

    // Transactions screen Filter: "All" | "Expense" | "Income"
    private val _transactionsFilterType = MutableStateFlow("All")
    val transactionsFilterType: StateFlow<String> = _transactionsFilterType.asStateFlow()

    // Transactions screen search query
    private val _transactionsSearchQuery = MutableStateFlow("")
    val transactionsSearchQuery: StateFlow<String> = _transactionsSearchQuery.asStateFlow()

    // Budget screen sub-tab: "CATEGORIES" | "MERCHANTS"
    private val _budgetSubTab = MutableStateFlow("CATEGORIES")
    val budgetSubTab: StateFlow<String> = _budgetSubTab.asStateFlow()

    // Theme Toggle State
    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("pref_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Repository Flows
    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile(1, "User", "৳", "en", true, true) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserProfile(1, "User", "৳", "en", true, true)
        )

    val allBudgets: StateFlow<List<Budget>> = combine(
        repository.allBudgets,
        allTransactions,
        selectedMonth,
        userProfile
    ) { budgets, txs, month, profile ->
        budgets.map { b ->
            val spentForMonth = txs.filter { it.month == month && it.type == "expense" && it.category == b.category }.sumOf { it.amount }
            val status = FinRepository.calculateStatusMessage(b.category, spentForMonth, b.limitAmount, profile.language)
            b.copy(spentAmount = spentForMonth, statusMessage = status)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMerchantBudgets: StateFlow<List<MerchantBudget>> = combine(
        repository.allMerchantBudgets,
        allTransactions,
        selectedMonth,
        userProfile
    ) { mBudgets, txs, month, profile ->
        mBudgets.map { m ->
            val spentForMonth = txs.filter { 
                it.month == month && 
                it.type == "expense" && 
                it.merchant?.lowercase()?.trim() == m.merchant.lowercase().trim() 
            }.sumOf { it.amount }
            val status = FinRepository.calculateMerchantStatusMessage(m.merchant, spentForMonth, m.limitAmount, profile.language)
            m.copy(spentAmount = spentForMonth, statusMessage = status)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allTransactions
                .filter { it.isNotEmpty() }
                .firstOrNull()?.let { list ->
                    val hasCurrentMonthTx = list.any { it.month == getCurrentMonthEnglish() }
                    if (!hasCurrentMonthTx) {
                        val latestTx = list.maxByOrNull { it.date }
                        if (latestTx != null) {
                            _selectedMonth.value = latestTx.month
                        }
                    }
                }
        }
    }

    // Moshi Instance for Backup / Restore
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    private val backupAdapter = moshi.adapter(BackupContainer::class.java)

    // Notes State & Methods
    val allNotes: StateFlow<List<FinanceNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    val filteredNotes: StateFlow<List<FinanceNote>> = combine(
        allNotes,
        _noteSearchQuery
    ) { notes, query ->
        if (query.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.tag.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveNote(id: String? = null, title: String, content: String, tag: String = "Finance") {
        viewModelScope.launch {
            val noteId = if (!id.isNullOrBlank()) id else "n-${System.currentTimeMillis()}"
            val note = FinanceNote(
                id = noteId,
                title = title.trim(),
                content = content.trim(),
                date = getTodayString(),
                tag = tag,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveNote(note)
        }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    // Derived State: Today's Spend in local currency (regardless of type = expense only)
    val todaySpendAmount: StateFlow<Double> = allTransactions.map { list ->
        val todayStr = getTodayString()
        list.filter { it.date == todayStr && it.type == "expense" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalanceAmount: StateFlow<Double> = allTransactions.map { list ->
        val totalEarned = list.filter { it.type == "income" }.sumOf { it.amount }
        val totalSpent = list.filter { it.type == "expense" }.sumOf { it.amount }
        totalEarned - totalSpent
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    data class YearlySavingsStats(
        val totalEarned: Double,
        val totalSpent: Double,
        val netSavings: Double,
        val savingsPercent: Float,
        val spendPercent: Float
    )

    val yearlySavingsStats: StateFlow<YearlySavingsStats> = combine(
        allTransactions,
        selectedYear
    ) { txs, year ->
        val yearlyTxs = txs.filter { it.date.startsWith("$year-") || it.date.substringBefore("-") == year }
        val earned = yearlyTxs.filter { it.type == "income" }.sumOf { it.amount }
        val spent = yearlyTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val net = earned - spent
        val savingsPct = if (earned > 0.0) (net / earned).coerceIn(0.0, 1.0).toFloat() else 0f
        val spendPct = if (earned > 0.0) (spent / earned).coerceIn(0.0, 5.0).toFloat() else 1f
        YearlySavingsStats(earned, spent, net, savingsPct, spendPct)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), YearlySavingsStats(0.0, 0.0, 0.0, 0f, 0f))

    val availableYears: StateFlow<List<String>> = allTransactions.map { list ->
        val years = list.map { it.date.substringBefore("-") }.filter { it.length == 4 }.toSet().toMutableList()
        val currentYear = getCurrentYearString()
        if (!years.contains(currentYear)) {
            years.add(currentYear)
        }
        years.sortedDescending()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(getCurrentYearString()))

    // Derived State: Monthly Savings statistics for currently selected month
    data class MonthlySavingsStats(
        val totalEarned: Double,
        val totalSpent: Double,
        val netSavings: Double,
        val savingsPercent: Float, // 0f to 1f
        val spendPercent: Float     // 0f to 1f
    )

    val monthlySavingsStats: StateFlow<MonthlySavingsStats> = combine(
        allTransactions,
        selectedMonth
    ) { txs, month ->
        val monthlyTxs = txs.filter { it.month == month }
        val earned = monthlyTxs.filter { it.type == "income" }.sumOf { it.amount }
        val spent = monthlyTxs.filter { it.type == "expense" }.sumOf { it.amount }
        val net = earned - spent
        val savingsPct = if (earned > 0.0) (net / earned).coerceIn(0.0, 1.0).toFloat() else 0f
        val spendPct = if (earned > 0.0) (spent / earned).coerceIn(0.0, 5.0).toFloat() else 1f
        MonthlySavingsStats(earned, spent, net, savingsPct, spendPct)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthlySavingsStats(0.0, 0.0, 0.0, 0f, 0f))

    // Derived State: Category spending summary for currently selected month
    data class CategorySummaryItem(
        val category: String,
        val amount: Double,
        val colorHex: Long,
        val count: Int
    )

    val categorySummaryList: StateFlow<List<CategorySummaryItem>> = combine(
        allTransactions,
        selectedMonth
    ) { txs, month ->
        val monthlyExpenses = txs.filter { it.month == month && it.type == "expense" }
        val grouped = monthlyExpenses.groupBy { it.category }
        
        grouped.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            CategorySummaryItem(
                category = cat,
                amount = sum,
                colorHex = getHexForCategory(cat),
                count = list.size
            )
        }.sortedByDescending { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derived State: Cumulative Spend Trend over selected month
    // We compute amount at days: 1, 5, 10, 15, 20, 25, 30
    val cumulativeSpendTrend: StateFlow<List<Double>> = combine(
        allTransactions,
        selectedMonth
    ) { txs, month ->
        val monthlyExpenses = txs.filter { it.month == month && it.type == "expense" }
        val days = listOf(1, 5, 10, 15, 20, 25, 30)
        days.map { dayThreshold ->
            monthlyExpenses.filter { tx ->
                val day = tx.date.substringAfterLast("-", "0").toIntOrNull() ?: 0
                day <= dayThreshold
            }.sumOf { it.amount }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

    // Transactions filtering based on type and query (No longer filtered by selectedMonth per user request!)
    val filteredTransactions: StateFlow<List<Transaction>> = combine(
        allTransactions,
        transactionsFilterType,
        transactionsSearchQuery
    ) { list, filter, query ->
        list.filter { tx ->
            val matchesType = when (filter) {
                "Expense" -> tx.type == "expense"
                "Income" -> tx.type == "income"
                else -> true
            }
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                tx.title.contains(query, ignoreCase = true) ||
                        (tx.merchant?.contains(query, ignoreCase = true) ?: false) ||
                        tx.amount.toString().contains(query)
            }
            matchesType && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Home list of transactions (just filtered by toggle on Home tab: Expense vs Income)
    val latestDailyActivityList: StateFlow<List<Transaction>> = combine(
        allTransactions,
        selectedMonth,
        dashboardToggleType
    ) { list, month, toggle ->
        list.filter { tx ->
            tx.month == month && tx.type == toggle
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun setSelectedMonth(month: String) {
        _selectedMonth.value = month
    }

    fun setSelectedYear(year: String) {
        _selectedYear.value = year
    }

    fun setDashboardToggleType(type: String) {
        _dashboardToggleType.value = type
    }

    fun setTransactionsFilterType(type: String) {
        _transactionsFilterType.value = type
    }

    fun setTransactionsSearchQuery(q: String) {
        _transactionsSearchQuery.value = q
    }

    fun setBudgetSubTab(tab: String) {
        _budgetSubTab.value = tab
    }

    fun toggleTheme() {
        val newVal = !_isDarkMode.value
        _isDarkMode.value = newVal
        prefs.edit().putBoolean("pref_dark_mode", newVal).apply()
    }

    // Add / Edit operations
    fun addTransaction(title: String, amount: Double, type: String, category: String, date: String, merchant: String?) {
        viewModelScope.launch {
            val id = "t-${System.currentTimeMillis()}"
            val month = getMonthNameFromDate(date)
            val tx = Transaction(id, title, amount, category, type, date, month, merchant?.trim())
            repository.insertTransaction(tx)
            // Auto switch visible list to the transaction's month on save
            _selectedMonth.value = month
        }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch {
            repository.deleteTransaction(id)
        }
    }

    fun updateTransaction(id: String, title: String, amount: Double, type: String, category: String, date: String, merchant: String?) {
        viewModelScope.launch {
            val month = getMonthNameFromDate(date)
            val updated = Transaction(id, title, amount, category, type, date, month, merchant?.trim())
            repository.updateTransaction(id, updated)
            _selectedMonth.value = month
        }
    }

    fun updateCategoryLimit(category: String, limit: Double) {
        viewModelScope.launch {
            val activeLang = userProfile.value.language
            val activeBudgets = allBudgets.value
            val matchObj = activeBudgets.find { it.category == category }
            if (matchObj != null) {
                val status = FinRepository.calculateStatusMessage(category, matchObj.spentAmount, limit, activeLang)
                repository.saveBudget(matchObj.copy(limitAmount = limit, statusMessage = status))
            } else {
                val status = FinRepository.calculateStatusMessage(category, 0.0, limit, activeLang)
                repository.saveBudget(Budget(category, limit, 0.0, category, status))
            }
        }
    }

    fun addMerchantBudgetCeiling(merchant: String, limit: Double) {
        viewModelScope.launch {
            val key = merchant.trim()
            if (key.isBlank()) return@launch
            val activeLang = userProfile.value.language

            // Find current spent at this merchant among transactions
            val spentSum = allTransactions.value
                .filter { it.type == "expense" && it.merchant?.lowercase()?.trim() == key.lowercase() }
                .sumOf { it.amount }

            val status = FinRepository.calculateMerchantStatusMessage(key, spentSum, limit, activeLang)
            val mb = MerchantBudget(
                merchant = key,
                limitAmount = limit,
                spentAmount = spentSum,
                icon = "Merchant",
                statusMessage = status
            )
            repository.saveMerchantBudget(mb)
        }
    }

    fun deleteMerchantBudgetCeiling(merchant: String) {
        viewModelScope.launch {
            repository.deleteMerchantBudget(merchant)
        }
    }

    fun updateProfile(name: String, currency: String, language: String, notifyTx: Boolean, notifyBudget: Boolean) {
        viewModelScope.launch {
            val updated = UserProfile(1, name, currency, language, notifyTx, notifyBudget)
            repository.saveProfile(updated)
        }
    }

    fun factoryResetApp() {
        viewModelScope.launch {
            repository.factoryReset()
            // Reset to defaults
            _selectedMonth.value = getCurrentMonthEnglish()
            _dashboardToggleType.value = "expense"
            _transactionsFilterType.value = "All"
            _transactionsSearchQuery.value = ""
        }
    }

    fun wipeAllTransactions() {
        viewModelScope.launch {
            repository.wipeAllTransactions()
        }
    }

    // Backup & Restore Serialization Methods
    suspend fun getExportJsonString(): String {
        val listTxs = repository.allTransactions.firstOrNull() ?: emptyList()
        val listBudgets = repository.allBudgets.firstOrNull() ?: emptyList()
        val listMerchants = repository.allMerchantBudgets.firstOrNull() ?: emptyList()
        val activeProfile = repository.userProfile.firstOrNull() ?: userProfile.value
        val listNotes = repository.allNotes.firstOrNull() ?: emptyList()

        val container = BackupContainer(
            version = "finpulse-v2.5",
            exportedAt = getTodayString(),
            transactions = listTxs,
            budgets = listBudgets,
            merchantBudgets = listMerchants,
            userProfile = activeProfile,
            notes = listNotes
        )
        return backupAdapter.toJson(container)
    }

    fun restoreBackupJson(jsonString: String, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch {
            try {
                val container = backupAdapter.fromJson(jsonString)
                if (container != null && container.transactions != null && container.budgets != null) {
                    repository.importBackupData(
                        transactions = container.transactions,
                        budgets = container.budgets,
                        merchantBudgets = container.merchantBudgets ?: emptyList(),
                        userProfile = container.userProfile,
                        notes = container.notes ?: emptyList()
                    )
                    // Reset month to current after restore
                    _selectedMonth.value = getCurrentMonthEnglish()
                    onSuccess()
                } else {
                    onError()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onError()
            }
        }
    }

    // Time Utility Helpers
    private fun getCurrentMonthEnglish(): String {
        val sdf = SimpleDateFormat("MMMM", Locale.ENGLISH)
        return sdf.format(Date())
    }

    private fun getCurrentYearString(): String {
        val sdf = SimpleDateFormat("yyyy", Locale.US)
        return sdf.format(Date())
    }

    fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getMonthNameFromDate(dateStr: String): String {
        // expected dateStr format: "yyyy-MM-dd"
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfInput.parse(dateStr) ?: Date()
            val sdfOutput = SimpleDateFormat("MMMM", Locale.ENGLISH)
            sdfOutput.format(date)
        } catch (e: Exception) {
            getCurrentMonthEnglish()
        }
    }

    fun getDaysLeftInCurrentMonth(): Int {
        val calendar = Calendar.getInstance()
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        return (maxDays - currentDay).coerceAtLeast(0)
    }

    private fun getHexForCategory(cat: String): Long {
        return when (cat.lowercase().trim()) {
            "food" -> 0xFFF97316
            "rent" -> 0xFF6366F1
            "transport" -> 0xFFEC4899
            "health" -> 0xFFF43F5E
            "education" -> 0xFF8B5CF6
            "entertainment" -> 0xFFEAB308
            "utility" -> 0xFF14B8A6
            "shopping" -> 0xFF10B981
            "income" -> 0xFF22C55E
            else -> 0xFF64748B
        }
    }
}
