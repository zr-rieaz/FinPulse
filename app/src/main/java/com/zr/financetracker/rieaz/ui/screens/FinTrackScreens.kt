package com.zr.financetracker.rieaz.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.zr.financetracker.rieaz.data.Budget
import com.zr.financetracker.rieaz.data.FinanceNote
import com.zr.financetracker.rieaz.data.MerchantBudget
import com.zr.financetracker.rieaz.data.Transaction
import com.zr.financetracker.rieaz.data.UserProfile
import com.zr.financetracker.rieaz.data.FinRepository
import com.zr.financetracker.rieaz.ui.Locales
import com.zr.financetracker.rieaz.ui.FinViewModel
import com.zr.financetracker.rieaz.ui.CurrencyHelper
import com.zr.financetracker.rieaz.ui.components.DonutChart
import com.zr.financetracker.rieaz.ui.components.SpendingTrendLineGraph
import com.zr.financetracker.rieaz.ui.components.getCategoryColor
import com.zr.financetracker.rieaz.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

// ========================================================
// 1. DASHBOARD SCREEN
// ========================================================
@Composable
fun DashboardScreen(
    viewModel: FinViewModel,
    profile: UserProfile,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.language
    val curr = profile.currency

    val totalBalance by viewModel.totalBalanceAmount.collectAsState()
    val monthlyStats by viewModel.monthlySavingsStats.collectAsState()
    val listTransactions by viewModel.latestDailyActivityList.collectAsState()
    val activeToggleType by viewModel.dashboardToggleType.collectAsState()
    val allBudgets by viewModel.allBudgets.collectAsState()
    val allTxs by viewModel.allTransactions.collectAsState()
    val selectMonth by viewModel.selectedMonth.collectAsState()

    val selectedYear by viewModel.selectedYear.collectAsState()
    val yearlyStats by viewModel.yearlySavingsStats.collectAsState()
    val availableYears by viewModel.availableYears.collectAsState()

    var expandedCategory by remember { mutableStateOf<String?>(null) }

    // Calculate dynamic spent amount specifically for current cycle month
    val dynamicBudgetsList = allBudgets.map { b ->
        val spentForMonth = allTxs.filter { it.month == selectMonth && it.type == "expense" && it.category == b.category }.sumOf { it.amount }
        val activeLang = profile.language
        val status = FinRepository.calculateStatusMessage(b.category, spentForMonth, b.limitAmount, activeLang)
        b.copy(spentAmount = spentForMonth, statusMessage = status)
    }

    val categoriesList = listOf("Food", "Rent", "Transport", "Health", "Education", "Entertainment", "Utility", "Shopping", "Other")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month select horizontal slider row
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = Locales.getString("cycleMonth", lang) + ": " + Locales.getString(selectMonth, lang),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val monthList = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
                    items(monthList) { m ->
                        val isSel = m == selectMonth
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setSelectedMonth(m) },
                            label = { Text(Locales.getString(m, lang), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Hero Card: Overall Balance
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_spending_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.getString("balance", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        // UNLOCKED SPARKLING BADGE
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Locales.getString("freeLifetimeUnl", lang),
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = String.format("%s%,.2f", curr, CurrencyHelper.convertFromBDT(totalBalance, curr)),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }



        // Monthly Savings progress aggregate
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Locales.getString("monthlySavings", lang),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(Locales.getString("earned", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%s%,.1f", curr, CurrencyHelper.convertFromBDT(monthlyStats.totalEarned, curr)), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FinSuccess)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Locales.getString("spent", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%s%,.1f", curr, CurrencyHelper.convertFromBDT(monthlyStats.totalSpent, curr)), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FinError)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Savings percentage bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.getString("netSavings", lang) + ": " + String.format("%s%,.1f", curr, CurrencyHelper.convertFromBDT(monthlyStats.netSavings, curr)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format("%.0f%%", monthlyStats.savingsPercent * 100),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (monthlyStats.netSavings >= 0) FinSuccess else FinError
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { monthlyStats.savingsPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (monthlyStats.netSavings >= 0) FinSuccess else FinError,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Yearly savings progress aggregate with historical year changer
        item {
            var expandedYearDropdown by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${Locales.getString("yearlySavings", lang)} (${selectedYear})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Box {
                            OutlinedButton(
                                onClick = { expandedYearDropdown = true },
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = selectedYear,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            DropdownMenu(
                                expanded = expandedYearDropdown,
                                onDismissRequest = { expandedYearDropdown = false }
                            ) {
                                availableYears.forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            viewModel.setSelectedYear(yr)
                                            expandedYearDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(Locales.getString("earned", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%s%,.1f", curr, CurrencyHelper.convertFromBDT(yearlyStats.totalEarned, curr)), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FinSuccess)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(Locales.getString("spent", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format("%s%,.1f", curr, CurrencyHelper.convertFromBDT(yearlyStats.totalSpent, curr)), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = FinError)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Locales.getString("netSavings", lang) + ": " + String.format("%s%,.1f", curr, CurrencyHelper.convertFromBDT(yearlyStats.netSavings, curr)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = String.format("%.0f%%", yearlyStats.savingsPercent * 100),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (yearlyStats.netSavings >= 0) FinSuccess else FinError
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { yearlyStats.savingsPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (yearlyStats.netSavings >= 0) FinSuccess else FinError,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }

        // Top category selection column (sorted by spending descending and expandable)
        item {
            val sortedCategoriesList = remember(categoriesList, allTxs, selectMonth) {
                categoriesList.map { cat ->
                    val spentForMonth = allTxs.filter { it.month == selectMonth && it.type == "expense" && it.category == cat }.sumOf { it.amount }
                    cat to spentForMonth
                }.sortedByDescending { it.second }
            }

            Column {
                Text(
                    text = Locales.getString("topCategories", lang),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sortedCategoriesList.forEach { (cat, spent) ->
                        val isExpanded = expandedCategory == cat
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedCategory = if (isExpanded) null else cat
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(getCategoryColor(cat), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = Locales.translateCategory(cat, lang),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = String.format("%s%,.0f", curr, CurrencyHelper.convertFromBDT(spent, curr)),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (spent > 0.0) FinError else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    val catTransactions = remember(allTxs, selectMonth, cat) {
                                        allTxs.filter { 
                                            it.month == selectMonth && 
                                            it.category == cat && 
                                            it.type == "expense" 
                                        }.sortedByDescending { it.date }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (catTransactions.isEmpty()) {
                                        Text(
                                            text = Locales.getString("noTransactions", lang),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            catTransactions.forEach { tx ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = if (!tx.merchant.isNullOrBlank()) tx.merchant else tx.title,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = tx.date,
                                                            fontSize = 10.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Text(
                                                        text = String.format("-%s%,.1f", curr, CurrencyHelper.convertFromBDT(tx.amount, curr)),
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = FinError
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

// ========================================================
// REUSABLE TRANSACTION ROW
// ========================================================
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    tx: Transaction,
    currencySymbol: String,
    languageCode: String,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    showEditIcon: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(Locales.getString("deleteTransactionTitle", languageCode), fontWeight = FontWeight.Bold) },
            text = { Text(Locales.getString("deleteTransactionConfirm", languageCode)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Locales.getString("ok", languageCode), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(Locales.getString("cancel", languageCode), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { showMenu = true },
                onLongClick = { showMenu = true }
            )
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(getCategoryColor(tx.category).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(getCategoryColor(tx.category), CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = tx.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    tx.merchant?.let { m ->
                        if (m.isNotBlank()) {
                            Text(
                                text = m,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Text(text = tx.date, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format("%s%s%,.2f", if (tx.type == "expense") "-" else "+", currencySymbol, CurrencyHelper.convertFromBDT(tx.amount, currencySymbol)),
                    fontSize = 13.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    color = if (tx.type == "expense") FinError else FinSuccess
                )
                if (showEditIcon) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Transaction",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Transaction",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEdit()
                },
                leadingIcon = { Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp)) }
            )
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    showDeleteConfirmDialog = true
                },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)) }
            )
        }
    }
}

// ========================================================
// 2. BILLING REPORTS SCREEN
// ========================================================
@Composable
fun ReportsScreen(
    viewModel: FinViewModel,
    profile: UserProfile,
    onAddTransactionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.language
    val curr = profile.currency

    val currentSelectedMonth by viewModel.selectedMonth.collectAsState()
    val summaryList by viewModel.categorySummaryList.collectAsState()
    val stats by viewModel.monthlySavingsStats.collectAsState()

    val monthList = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month select horizontal slider row
        item {
            Column {
                Text(
                    text = Locales.getString("cycleMonth", lang) + ": " + Locales.getString(currentSelectedMonth, lang),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(monthList) { m ->
                        val isSel = m == currentSelectedMonth
                        FilterChip(
                            selected = isSel,
                            onClick = { viewModel.setSelectedMonth(m) },
                            label = { Text(Locales.getString(m, lang), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        }

        // Circular Pie Chart Display aggregates
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val chartPairs = summaryList.map { it.category to it.amount }
                    DonutChart(
                        data = chartPairs,
                        totalAmount = stats.totalSpent,
                        currencySymbol = curr,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            }
        }

        // Add transaction inline prompt banner
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Locales.getString("categoryBreakdown", lang),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onAddTransactionClick,
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(Locales.getString("addTransactionBtn", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Categories breakdown details rows
        if (summaryList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Locales.getString("noTransactions", lang),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(summaryList) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(item.colorHex).copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.size(12.dp).background(Color(item.colorHex), CircleShape))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(Locales.translateCategory(item.category, lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "${item.count} " + if (lang == "bn") "টি লেনদেন" else "transactions",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format("%s%,.2f", curr, CurrencyHelper.convertFromBDT(item.amount, curr)),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.SansSerif
                            )
                            val ratioPct = if (stats.totalSpent > 0.0) (item.amount / stats.totalSpent) * 100 else 0.0
                            Text(
                                text = String.format("%.1f%%", ratioPct),
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

fun formatGooglePhotosDate(dateStr: String, lang: String): String {
    try {
        val sdfInput = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val date = sdfInput.parse(dateStr) ?: return dateStr
        
        val todayCal = java.util.Calendar.getInstance()
        val targetCal = java.util.Calendar.getInstance()
        targetCal.time = date
        
        todayCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        todayCal.set(java.util.Calendar.MINUTE, 0)
        todayCal.set(java.util.Calendar.SECOND, 0)
        todayCal.set(java.util.Calendar.MILLISECOND, 0)
        
        val targetZero = java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        
        val diffMs = todayCal.timeInMillis - targetZero.timeInMillis
        val diffDays = diffMs / (1000 * 60 * 60 * 24)
        
        if (diffDays == 0L) {
            return if (lang == "bn") "আজ" else "Today"
        } else if (diffDays == 1L) {
            return if (lang == "bn") "গতকাল" else "Yesterday"
        }
        
        val currentYear = todayCal.get(java.util.Calendar.YEAR)
        val targetYear = targetCal.get(java.util.Calendar.YEAR)
        
        val locale = if (lang == "bn") java.util.Locale("bn") else java.util.Locale.ENGLISH
        
        if (targetYear == currentYear) {
            val sdfOutput = java.text.SimpleDateFormat("EEE, MMM d", locale)
            return sdfOutput.format(date)
        } else {
            val sdfOutput = java.text.SimpleDateFormat("EEE, MMM d, yyyy", locale)
            return sdfOutput.format(date)
        }
    } catch (e: Exception) {
        return dateStr
    }
}

// ========================================================
// 3. TRANSACTIONS SCREEN (STATEMENT LOGS)
// ========================================================
@Composable
fun TransactionsScreen(
    viewModel: FinViewModel,
    profile: UserProfile,
    onEditSelectedTransaction: (Transaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = profile.language
    val curr = profile.currency

    val filterType by viewModel.transactionsFilterType.collectAsState()
    val searchQuery by viewModel.transactionsSearchQuery.collectAsState()
    val transactionsList by viewModel.filteredTransactions.collectAsState()
    var searchDate by remember { mutableStateOf<String?>(null) }

    val finalTransactionsList = if (searchDate == null) {
        transactionsList
    } else {
        transactionsList.filter { it.date == searchDate }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = Locales.getString("statementLogs", lang),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Text Search Bar & Calendar Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setTransactionsSearchQuery(it) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("transactions_search_input"),
                    placeholder = { Text(Locales.getString("searchPlaceholder", lang), fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setTransactionsSearchQuery("") }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    maxLines = 1,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = Color(0xFF4F46E5),
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    )
                )

                // Date Picker Button
                val context = LocalContext.current
                IconButton(
                    onClick = {
                        val calendar = java.util.Calendar.getInstance()
                        searchDate?.let { sd ->
                            try {
                                val parts = sd.split("-")
                                if (parts.size == 3) {
                                    calendar.set(java.util.Calendar.YEAR, parts[0].toInt())
                                    calendar.set(java.util.Calendar.MONTH, parts[1].toInt() - 1)
                                    calendar.set(java.util.Calendar.DAY_OF_MONTH, parts[2].toInt())
                                }
                            } catch (e: Exception) {}
                        }

                        android.app.DatePickerDialog(
                            context,
                            { _, year, monthOfYear, dayOfMonth ->
                                val formattedMonth = String.format("%02d", monthOfYear + 1)
                                val formattedDay = String.format("%02d", dayOfMonth)
                                searchDate = "$year-$formattedMonth-$formattedDay"
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (searchDate != null) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (searchDate != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Filter by date",
                        tint = if (searchDate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Selected Date Chip
        if (searchDate != null) {
            item {
                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable { searchDate = null }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (lang == "bn") "তারিখ: $searchDate" else "Date: $searchDate",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // All, Expense, Income Filter segmented Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("All", "Expense", "Income").forEach { tab ->
                    val isActive = filterType == tab
                    Button(
                        onClick = { viewModel.setTransactionsFilterType(tab) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .testTag("filter_tab_$tab"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        val key = when (tab) {
                            "Expense" -> "expense"
                            "Income" -> "income"
                            else -> "all"
                        }
                        Text(Locales.getString(key, lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        val grouped = finalTransactionsList.groupBy { it.date }.toList().sortedByDescending { it.first }

        // List View Content
        if (grouped.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Inbox, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(Locales.getString("noTransactions", lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            grouped.forEach { (date, txs) ->
                item(key = "header_$date") {
                    Text(
                        text = formatGooglePhotosDate(date, lang),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }
                items(txs, key = { it.id }) { tx ->
                    TransactionRow(
                        tx = tx,
                        currencySymbol = curr,
                        languageCode = lang,
                        onDelete = { viewModel.deleteTransaction(tx.id) },
                        onEdit = { onEditSelectedTransaction(tx) }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }


}

// ========================================================
// 4. CLEAN NOTEPAD SCREEN
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotepadScreen(
    viewModel: FinViewModel,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val lang = profile.language
    val notes by viewModel.filteredNotes.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val searchQuery by viewModel.noteSearchQuery.collectAsState()
    val context = LocalContext.current

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<FinanceNote?>(null) }
    var noteToDelete by remember { mutableStateOf<FinanceNote?>(null) }
    var selectedTagFilter by remember { mutableStateOf("All") }

    val tags = listOf("All", "Finance", "Budget", "Shopping", "Personal", "Notes")

    val displayedNotes = remember(notes, selectedTagFilter) {
        if (selectedTagFilter == "All") notes
        else notes.filter { it.tag.equals(selectedTagFilter, ignoreCase = true) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Header Banner
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Locales.getString("notepadTitle", lang),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (lang == "bn") "${allNotes.size}টি নোট সংরক্ষিত আছে" else "${allNotes.size} notes stored securely",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                editingNote = null
                                showAddEditDialog = true
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Locales.getString("newNote", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setNoteSearchQuery(it) },
                    placeholder = { Text(Locales.getString("searchNotes", lang), fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setNoteSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category tag filter chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tags) { tag ->
                        val isSelected = selectedTagFilter == tag
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedTagFilter = tag },
                            label = {
                                Text(
                                    text = if (tag == "All") Locales.getString("all", lang) else tag,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Empty State
            if (displayedNotes.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = Locales.getString("noNotes", lang),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    editingNote = null
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Locales.getString("newNote", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(displayedNotes, key = { it.id }) { note ->
                    NoteCardItem(
                        note = note,
                        onEdit = {
                            editingNote = note
                            showAddEditDialog = true
                        },
                        onDelete = {
                            noteToDelete = note
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add / Edit Note Dialog
    if (showAddEditDialog) {
        NoteAddEditDialog(
            note = editingNote,
            lang = lang,
            onDismiss = {
                showAddEditDialog = false
                editingNote = null
            },
            onSave = { id, title, content, tag ->
                viewModel.saveNote(id, title, content, tag)
                showAddEditDialog = false
                editingNote = null
                Toast.makeText(context, Locales.getString("saveNote", lang), Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Delete confirmation dialog
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text(Locales.getString("deleteNoteConfirm", lang), fontSize = 15.sp, fontWeight = FontWeight.Bold) },
            text = { Text(noteToDelete?.title ?: "") },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete?.let { viewModel.deleteNote(it.id) }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { noteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NoteCardItem(
    note: FinanceNote,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val tagColor = when (note.tag.lowercase()) {
        "finance" -> Color(0xFF10B981)
        "budget" -> Color(0xFF6366F1)
        "shopping" -> Color(0xFFF59E0B)
        "personal" -> Color(0xFFEC4899)
        else -> Color(0xFF8B5CF6)
    }

    Card(
        onClick = onEdit,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = note.tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = tagColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.date,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = note.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = note.content,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NoteAddEditDialog(
    note: FinanceNote?,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (id: String?, title: String, content: String, tag: String) -> Unit
) {
    var title by remember { mutableStateOf(note?.title ?: "") }
    var content by remember { mutableStateOf(note?.content ?: "") }
    var tag by remember { mutableStateOf(note?.tag ?: "Finance") }

    val tags = listOf("Finance", "Budget", "Shopping", "Personal", "Notes")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (note == null) Locales.getString("newNote", lang) else Locales.getString("editNote", lang),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Locales.getString("noteTitlePlaceholder", lang), fontSize = 12.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tag selector
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { t ->
                        val isSelected = tag == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { tag = t },
                            label = { Text(t, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(Locales.getString("noteContentPlaceholder", lang), fontSize = 12.sp) },
                    minLines = 4,
                    maxLines = 8,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() || content.isNotBlank()) {
                        onSave(note?.id, title.ifBlank { "Note" }, content, tag)
                    }
                },
                enabled = title.isNotBlank() || content.isNotBlank()
            ) {
                Text(Locales.getString("saveNote", lang))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(Locales.getString("cancel", lang))
            }
        }
    )
}

// Indicator detail layout
@Composable
fun BudgetIndicatorCard(
    title: String,
    iconCategory: String,
    spentAmount: Double,
    limitAmount: Double,
    statusMessage: String,
    currencySymbol: String
) {
    val pct = if (limitAmount > 0.0) (spentAmount / limitAmount).toFloat() else 0f
    val colorBar = when {
        pct > 1f -> FinError
        pct > 0.8f -> FinWarning
        else -> FinSuccess
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(getCategoryColor(iconCategory), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(
                    text = String.format("%s%,.0f / %s%,.0f", currencySymbol, CurrencyHelper.convertFromBDT(spentAmount, currencySymbol), currencySymbol, CurrencyHelper.convertFromBDT(limitAmount, currencySymbol)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = colorBar,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = statusMessage,
                color = colorBar,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ========================================================
// 5. SETTINGS SCREEN
// ========================================================
@Composable
fun SettingsScreen(
    viewModel: FinViewModel,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val lang = profile.language
    val curr = profile.currency

    val context = LocalContext.current
    val contentResolver = context.contentResolver
    val coroutineScope = rememberCoroutineScope()

    val budgetsList by viewModel.allBudgets.collectAsState()

    // Form settings variables
    var userNameInput by remember { mutableStateOf(profile.name) }
    var selectedCurr by remember { mutableStateOf(curr) }
    var isEnglishSelected by remember { mutableStateOf(lang == "en") }

    var alertTxRemi by remember { mutableStateOf(profile.notifyTransactions) }
    var alertBudgWarn by remember { mutableStateOf(profile.notifyBudgetAlerts) }

    var factoryConfirmOpen by remember { mutableStateOf(false) }
    var resetInputText by remember { mutableStateOf("") }

    // Backup Save Activity Result Document Selector
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val exportJson = viewModel.getExportJsonString()
                    contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(exportJson.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(context, Locales.getString("backupSaved", if(isEnglishSelected) "en" else "bn"), Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export Failure: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Backup Read Activity Selector picker
    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val incomingJson = contentResolver.openInputStream(uri)?.use { stream ->
                        stream.readBytes().toString(Charsets.UTF_8)
                    }
                    if (incomingJson != null) {
                        viewModel.restoreBackupJson(
                            incomingJson,
                            onSuccess = {
                                Toast.makeText(context, Locales.getString("backupRestored", if(isEnglishSelected) "en" else "bn"), Toast.LENGTH_SHORT).show()
                            },
                            onError = {
                                Toast.makeText(context, Locales.getString("invalidBackup", if(isEnglishSelected) "en" else "bn"), Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import Failure: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Center Profile Picture & Name Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile Avatar with Gradient and Border
                Box(
                    modifier = Modifier.size(86.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF4F46E5),
                                        Color(0xFF7C3AED),
                                        Color(0xFFEC4899)
                                    )
                                )
                            )
                            .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (userNameInput.trim().firstOrNull()?.uppercase() ?: "R"),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // Verified Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(24.dp)
                            .background(Color(0xFF10B981), CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Profile Name
                Text(
                    text = userNameInput.ifBlank { "Rieaz" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Status Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Text(
                        text = "FinPulse • $selectedCurr • ${if (isEnglishSelected) "English" else "বাংলা"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Section: Profile
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Locales.getString("profileSettings", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Username Text
                    Text(Locales.getString("username", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = userNameInput,
                        onValueChange = { userNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        maxLines = 1,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = Color(0xFF4F46E5),
                            focusedBorderColor = Color(0xFF4F46E5),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Currency row switcher (supports 2 rows for 6 currencies)
                    Text(Locales.getString("currency", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val currencyList = listOf("৳", "$", "€", "£", "MYR", "SAR")
                        currencyList.chunked(3).forEach { rowList ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowList.forEach { c ->
                                    val isActive = selectedCurr == c
                                    Button(
                                        onClick = { selectedCurr = c },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(c, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Language Selector Dropdown segmented chips
                    Text(Locales.getString("language", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { isEnglishSelected = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEnglishSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isEnglishSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("English", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Button(
                            onClick = { isEnglishSelected = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isEnglishSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (!isEnglishSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(38.dp)
                        ) {
                            Text("বাংলা", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.updateProfile(
                                name = userNameInput,
                                currency = selectedCurr,
                                language = if (isEnglishSelected) "en" else "bn",
                                notifyTx = alertTxRemi,
                                notifyBudget = alertBudgWarn
                            )
                            Toast.makeText(context, Locales.getString("profileSavedSuccess", if (isEnglishSelected) "en" else "bn"), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(Locales.getString("saveChanges", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section: System Notifications alert
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Locales.getString("notificationsSection", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Locales.getString("reminderToggle", lang), fontSize = 12.sp)
                        Switch(
                            checked = alertTxRemi,
                            onCheckedChange = { alertTxRemi = it }
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(Locales.getString("alertsToggle", lang), fontSize = 12.sp)
                        Switch(
                            checked = alertBudgWarn,
                            onCheckedChange = { alertBudgWarn = it }
                        )
                    }
                }
            }
        }

        // Section: Budget limits lists categories setter
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Locales.getString("budgetCeilings", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    budgetsList.forEach { categoryObj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.5f)) {
                                Box(modifier = Modifier.size(8.dp).background(getCategoryColor(categoryObj.category), CircleShape))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    Locales.translateCategory(categoryObj.category, lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            // Limit amount text inputs
                            val displayLimit = remember(categoryObj.limitAmount, curr) {
                                CurrencyHelper.convertFromBDT(categoryObj.limitAmount, curr)
                            }
                            var limitInputVal by remember(displayLimit) { mutableStateOf(displayLimit.toInt().toString()) }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.weight(0.5f)
                            ) {
                                OutlinedTextField(
                                    value = limitInputVal,
                                    onValueChange = { limitInputVal = it.filter { c -> c.isDigit() } },
                                    modifier = Modifier.width(115.dp),
                                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, textAlign = TextAlign.End),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                        cursorColor = Color(0xFF4F46E5),
                                        focusedBorderColor = Color(0xFF4F46E5),
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        val parseLimitDisplay = limitInputVal.toDoubleOrNull() ?: 0.0
                                        val parseLimit = CurrencyHelper.convertToBDT(parseLimitDisplay, curr)
                                        viewModel.updateCategoryLimit(categoryObj.category, parseLimit)
                                        Toast.makeText(context, Locales.getString("budgetCeilingUpdated", lang), Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Save,
                                        contentDescription = "Save limit",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), thickness = 0.5.dp)
                    }
                }
            }
        }

        // Section: JSON Backup & Restore
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Locales.getString("backupRestore", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = Locales.getString("backupRestoreDesc", lang),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                exportDocumentLauncher.launch("finpulse-backup-${viewModel.getTodayString()}.json")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Upload, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Locales.getString("exportJson", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                importDocumentLauncher.launch("application/json")
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(42.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(Locales.getString("restoreFrom", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Section: Danger Zone App Settings Factory Reset
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Locales.getString("dangerZone", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { factoryConfirmOpen = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(Locales.getString("factoryResetButton", lang), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section: Contact Us (Professional & Clean)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = Locales.getString("contactUs", lang),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = Locales.getString("contactUsDesc", lang),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // WhatsApp item button
                    Surface(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/8801628403390"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:01628403390"))
                                    context.startActivity(callIntent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "WhatsApp: 01628403390", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF25D366).copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF25D366), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = "WhatsApp",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = Locales.getString("whatsapp", lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "01628403390",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = Color(0xFF25D366),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // GitHub item button
                    Surface(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/zr-rieaz/"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "GitHub: https://github.com/zr-rieaz/", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.onSurface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "GitHub",
                                        tint = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = Locales.getString("githubProfile", lang),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "https://github.com/zr-rieaz/",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Locales.getString("appVersionLabel", lang) + " • " + Locales.getString("privacyGuarantee", lang),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Factory dialog confirm PIN check
    if (factoryConfirmOpen) {
        AlertDialog(
            onDismissRequest = { factoryConfirmOpen = false },
            title = { Text(Locales.getString("factoryReset", lang), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text(Locales.getString("resetPrompt", lang))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = resetInputText,
                        onValueChange = { resetInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("RESET") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            cursorColor = MaterialTheme.colorScheme.error,
                            focusedBorderColor = MaterialTheme.colorScheme.error,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (resetInputText.trim().uppercase() == "RESET") {
                            viewModel.factoryResetApp()
                            factoryConfirmOpen = false
                            resetInputText = ""
                            Toast.makeText(context, "App factory reset complete!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Locales.getString("confirm", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    factoryConfirmOpen = false
                    resetInputText = ""
                }) {
                    Text(Locales.getString("cancel", lang))
                }
            }
        )
    }
}

// ========================================================
// 6. ADD / EDIT TRANSACTION BOTTOM SHEET / MODAL DIALOG
// ========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionSheet(
    viewModel: FinViewModel,
    profile: UserProfile,
    editingTransaction: Transaction?, // if not null, we are editing
    onDismiss: () -> Unit
) {
    val lang = profile.language
    val curr = profile.currency
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val isEditMode = editingTransaction != null

    // Form entries
    var title by remember { mutableStateOf(editingTransaction?.title ?: "") }
    val initialAmount = remember(editingTransaction, curr) {
        editingTransaction?.let { CurrencyHelper.convertFromBDT(it.amount, curr) }
    }
    var amountStr by remember { mutableStateOf(initialAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var txType by remember { mutableStateOf(editingTransaction?.type ?: "expense") } // expense | income
    var selectedCategory by remember { mutableStateOf(editingTransaction?.category ?: "Food") }
    var dateSelected by remember { mutableStateOf(editingTransaction?.date ?: viewModel.getTodayString()) }
    var merchant by remember { mutableStateOf(editingTransaction?.merchant ?: "") }

    val categoriesList = listOf("Food", "Rent", "Transport", "Health", "Education", "Entertainment", "Utility", "Shopping", "Other")

    // Automatic income category override
    LaunchedEffect(txType) {
        if (txType == "income") {
            selectedCategory = "Income"
        } else if (selectedCategory == "Income") {
            selectedCategory = "Food"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.windowInsetsPadding(WindowInsets.ime)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = Locales.getString(if (isEditMode) "editTransaction" else "addTransaction", lang),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )

            // Segmented Switch Type Expense VS Income
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { txType = "expense" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (txType == "expense") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (txType == "expense") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(Locales.getString("expense", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { txType = "income" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (txType == "income") MaterialTheme.colorScheme.primary else Color.Transparent,
                        contentColor = if (txType == "income") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.weight(1f).height(36.dp),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(Locales.getString("income", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Title Field
            Column {
                Text(
                    text = if (lang == "bn") "বর্ণনা" else "Title",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(Locales.getString("titlePlaceholder", lang), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("add_transaction_title_field"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = Color(0xFF4F46E5),
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    )
                )
            }

            // Amount Decimal Field
            Column {
                Text(
                    text = Locales.getString("amount", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    leadingIcon = { Text(curr, fontWeight = FontWeight.Black) },
                    placeholder = { Text("0.00", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("add_transaction_amount_field"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = Color(0xFF4F46E5),
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    )
                )
            }

            // Date picker trigger
            Column {
                Text(
                    text = Locales.getString("date", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        .clickable {
                            val calendar = Calendar.getInstance()
                            // parse existing if possible
                            val currentTokens = dateSelected.split("-")
                            if (currentTokens.size == 3) {
                                calendar.set(Calendar.YEAR, currentTokens[0].toInt())
                                calendar.set(Calendar.MONTH, currentTokens[1].toInt() - 1)
                                calendar.set(Calendar.DAY_OF_MONTH, currentTokens[2].toInt())
                            }

                            val dialog = android.app.DatePickerDialog(
                                context,
                                { _, selectedYear, selectedMonth, selectedDayOfMonth ->
                                    val mStr = String.format(java.util.Locale.US, "%02d", selectedMonth + 1)
                                    val dStr = String.format(java.util.Locale.US, "%02d", selectedDayOfMonth)
                                    dateSelected = "$selectedYear-$mStr-$dStr"
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.show()
                        }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(dateSelected, fontSize = 13.sp)
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Merchant Field (Optional)
            Column {
                Text(
                    text = Locales.getString("merchant", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    placeholder = { Text(Locales.getString("merchantPlaceholder", lang), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth().testTag("add_transaction_merchant_field"),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        cursorColor = Color(0xFF4F46E5),
                        focusedBorderColor = Color(0xFF4F46E5),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    )
                )
            }

            // Category list (chips selector)
            if (txType == "expense") {
                Column {
                    Text(
                        text = Locales.getString("category", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(categoriesList) { cat ->
                            val isActive = selectedCategory == cat
                            FilterChip(
                                selected = isActive,
                                onClick = { selectedCategory = cat },
                                label = { Text(Locales.translateCategory(cat, lang), fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Trigger Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Locales.getString("cancel", lang), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val displayAmt = amountStr.toDoubleOrNull() ?: 0.0
                        val amount = CurrencyHelper.convertToBDT(displayAmt, curr)
                        if (title.isBlank()) {
                            Toast.makeText(context, "Please enter a valid title", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (displayAmt <= 0.0) {
                            Toast.makeText(context, "Please enter an amount greater than 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        if (isEditMode && editingTransaction != null) {
                            viewModel.updateTransaction(
                                id = editingTransaction.id,
                                title = title,
                                amount = amount,
                                type = txType,
                                category = selectedCategory,
                                date = dateSelected,
                                merchant = merchant
                            )
                        } else {
                            viewModel.addTransaction(
                                title = title,
                                amount = amount,
                                type = txType,
                                category = selectedCategory,
                                date = dateSelected,
                                merchant = merchant
                            )
                        }
                        keyboardController?.hide()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("add_transaction_submit_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(Locales.getString("saveTransaction", lang), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
