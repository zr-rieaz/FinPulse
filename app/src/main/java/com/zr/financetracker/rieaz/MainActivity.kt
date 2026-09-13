package com.zr.financetracker.rieaz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zr.financetracker.rieaz.data.Transaction
import com.zr.financetracker.rieaz.ui.FinViewModel
import com.zr.financetracker.rieaz.ui.Locales
import com.zr.financetracker.rieaz.ui.screens.*
import com.zr.financetracker.rieaz.ui.theme.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            FinPulseTheme(darkTheme = isDarkMode) {
                AppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(viewModel: FinViewModel) {
    // 1. Splash Screen Control (1.0 second duration for ultra fast loading)
    var isShowSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1000)
        isShowSplash = false
    }

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Solve the status bar icon / color theme bug dynamically
    LaunchedEffect(isDarkMode, isShowSplash) {
        activity?.enableEdgeToEdge(
            statusBarStyle = if (isShowSplash || isDarkMode) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
            navigationBarStyle = if (isShowSplash || isDarkMode) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            }
        )
    }

    if (isShowSplash) {
        SplashScreenLayout()
    } else {
        // App Core Shell Navigation
        val profile by viewModel.userProfile.collectAsState()
        val lang = profile.language
        val selectMonth by viewModel.selectedMonth.collectAsState()

        var selectedTab by remember { mutableStateOf(0) } // 0..4
        var activeEditTransaction by remember { mutableStateOf<Transaction?>(null) }
        var isShowAddEditModal by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = Locales.getString("appName", lang),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = Locales.getString("byRieaz", lang),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "${Locales.getString("cycleMonth", lang)}: ${Locales.getString(selectMonth, lang)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    actions = {
                        // Glossy unlocked PRO Badge
                        Row(
                            modifier = Modifier
                                .padding(end = 10.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = Locales.getString("proBadge", lang),
                                color = Color(0xFF10B981),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Theme Toggle Icon
                        val isDarkMode by viewModel.isDarkMode.collectAsState()
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                contentDescription = "Toggle Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    val tabs = listOf(
                        Triple("dashboard", Icons.Default.Home, Icons.Outlined.Home),
                        Triple("billingReports", Icons.Default.BarChart, Icons.Outlined.BarChart),
                        Triple("statementLogs", Icons.Default.Receipt, Icons.Outlined.Receipt),
                        Triple("notepad", Icons.Default.EditNote, Icons.Outlined.EditNote),
                        Triple("settings", Icons.Default.Settings, Icons.Outlined.Settings)
                    )

                    tabs.forEachIndexed { index, tabInfo ->
                        val isSelected = selectedTab == index
                        val labelKey = when (tabInfo.first) {
                            "dashboard" -> "tabDashboard"
                            "billingReports" -> "tabReports"
                            "statementLogs" -> "tabLogs"
                            "notepad" -> "tabNotepad"
                            "settings" -> "tabSettings"
                            else -> tabInfo.first
                        }
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = index },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tabInfo.second else tabInfo.third,
                                    contentDescription = Locales.getString(labelKey, lang)
                                )
                            },
                            label = {
                                Text(
                                    text = Locales.getString(labelKey, lang),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            },
                            modifier = Modifier.testTag("nav_tab_${tabInfo.first}")
                        )
                    }
                }
            },
            floatingActionButton = {
                // Show floating adding trigger button on Home (0), Reports (1), or Transactions (2)
                if (selectedTab in 0..2) {
                    FloatingActionButton(
                        onClick = {
                            activeEditTransaction = null
                            isShowAddEditModal = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .testTag("add_transaction_fab")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
            ) {
                // Page contents switcher based on active bottom selections
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        profile = profile,
                        onAddTransactionClick = {
                            activeEditTransaction = null
                            isShowAddEditModal = true
                        }
                    )
                    1 -> ReportsScreen(
                        viewModel = viewModel,
                        profile = profile,
                        onAddTransactionClick = {
                            activeEditTransaction = null
                            isShowAddEditModal = true
                        }
                    )
                    2 -> TransactionsScreen(
                        viewModel = viewModel,
                        profile = profile,
                        onEditSelectedTransaction = { tx ->
                            activeEditTransaction = tx
                            isShowAddEditModal = true
                        }
                    )
                    3 -> NotepadScreen(
                        viewModel = viewModel,
                        profile = profile
                    )
                    4 -> SettingsScreen(
                        viewModel = viewModel,
                        profile = profile
                    )
                }
            }

            // Sheet form modal injection overlay
            if (isShowAddEditModal) {
                AddEditTransactionSheet(
                    viewModel = viewModel,
                    profile = profile,
                    editingTransaction = activeEditTransaction,
                    onDismiss = {
                        isShowAddEditModal = false
                        activeEditTransaction = null
                    }
                )
            }
        }
    }
}

// App Launch Splash Screen Layout with Brand Identity
@Composable
fun SplashScreenLayout() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4C1D95), // Deep rich violet/purple
                        Color(0xFF2563EB)  // Royal professional blue
                    )
                )
            )
            .padding(24.dp)
    ) {
        // Center Content
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "WELCOME TO FINPULSE",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Connected to your financial health",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.padding(bottom = 32.dp),
                textAlign = TextAlign.Center
            )
            Card(
                modifier = Modifier
                    .size(240.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = R.drawable.finpulse_logo),
                        contentDescription = "FinPulse Logo",
                        modifier = Modifier
                            .size(210.dp)
                            .padding(12.dp)
                    )
                }
            }
        }

        // Bottom Content Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "App Version 2.4.3 - Build 47",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Developed by Rieaz",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "© 2026 FinPulse by Rieaz. All rights reserved.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
