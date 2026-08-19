package com.example.investmenttracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.investmenttracker.data.LiveNewsItem
import com.example.investmenttracker.data.StockItem
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import com.example.investmenttracker.data.*
import com.example.investmenttracker.ui.components.*
import com.example.investmenttracker.ui.screens.*


enum class CurrentScreen {
    SPLASH, HOME, SETTINGS, BROKERAGE, STOCK_DETAILS, TRANSACTIONS
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Notification Channel
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(CurrentScreen.SPLASH) }
                var selectedStock by remember { mutableStateOf<StockItem?>(null) }
                
                // Shared state for stocks and news to ensure data is ready after splash
                val stocks = remember { mutableStateListOf<StockItem>() }
                val newsList = remember { mutableStateListOf<LiveNewsItem>() }
                val brokers = remember { mutableStateListOf<BrokerCardItem>() }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E2E)
                ) {
                    when (currentScreen) {
                        CurrentScreen.SPLASH -> SplashScreen(
                            onDataLoaded = { 
                                currentScreen = CurrentScreen.HOME 
                            },
                            stocks = stocks,
                            newsList = newsList,
                            brokers = brokers
                        )
                        CurrentScreen.HOME -> MainScreen(
                            onNavigateToSettings = { currentScreen = CurrentScreen.SETTINGS },
                            onNavigateToBrokerage = { currentScreen = CurrentScreen.BROKERAGE },
                            onNavigateToTransactions = { currentScreen = CurrentScreen.TRANSACTIONS },
                            onStockClick = { stock ->
                                selectedStock = stock
                                currentScreen = CurrentScreen.STOCK_DETAILS
                            },
                            initialStocks = stocks,
                            initialNews = newsList,
                            initialBrokers = brokers
                        )
                        CurrentScreen.SETTINGS -> SettingsScreen(
                            onBackToHome = { currentScreen = CurrentScreen.HOME },
                            onNavigateToBrokerage = { currentScreen = CurrentScreen.BROKERAGE },
                            onNavigateToTransactions = { currentScreen = CurrentScreen.TRANSACTIONS }
                        )
                        CurrentScreen.BROKERAGE -> BrokerageScreen(
                            onBackToHome = { currentScreen = CurrentScreen.HOME },
                            onNavigateToSettings = { currentScreen = CurrentScreen.SETTINGS },
                            onNavigateToTransactions = { currentScreen = CurrentScreen.TRANSACTIONS }
                        )
                        CurrentScreen.TRANSACTIONS -> TransactionsScreen(
                            onBackToHome = { currentScreen = CurrentScreen.HOME },
                            onNavigateToSettings = { currentScreen = CurrentScreen.SETTINGS },
                            onNavigateToBrokerage = { currentScreen = CurrentScreen.BROKERAGE }
                        )
                        CurrentScreen.STOCK_DETAILS -> {
                            selectedStock?.let { stock ->
                                StockDetailsScreen(
                                    stock = stock,
                                    onBackToHome = { currentScreen = CurrentScreen.HOME },
                                    onNavigateToSettings = { currentScreen = CurrentScreen.SETTINGS },
                                    onNavigateToBrokerage = { currentScreen = CurrentScreen.BROKERAGE },
                                    onNavigateToTransactions = { currentScreen = CurrentScreen.TRANSACTIONS }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToBrokerage: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onStockClick: (StockItem) -> Unit,
    initialStocks: List<StockItem>,
    initialNews: List<LiveNewsItem>,
    initialBrokers: List<BrokerCardItem>
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val stocks = remember { mutableStateListOf<StockItem>().apply { addAll(initialStocks) } }
    val newsList = remember { mutableStateListOf<LiveNewsItem>().apply { addAll(initialNews) } }
    var isNewsLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val username = remember { UserPreferences.getUsername(context).ifBlank { "User" } }
    val brokers = remember { mutableStateListOf<BrokerCardItem>().apply { addAll(initialBrokers) } }

    // Permission Launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        UserPreferences.saveNotificationsEnabled(context, isGranted)
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    suspend fun refreshStockPricesOnly() {
        val currentSymbols = getSavedStockSymbols(context)
        if (currentSymbols.isEmpty()) {
            stocks.clear()
            return
        }
        
        coroutineScope {
            val updatedStocks = currentSymbols.map { symbol ->
                async { fetchSGXStockDynamic(symbol) }
            }.awaitAll().filterNotNull()

            // Check Price Alerts
            val allAlerts = UserPreferences.loadPriceAlerts(context)
            if (allAlerts.isNotEmpty()) {
                updatedStocks.forEach { stock ->
                    val stockAlerts = allAlerts.filter { it.symbol == stock.symbol }
                    val currentPrice = stock.currentPrice.replace("S$", "").replace("$", "").toDoubleOrNull() ?: 0.0

                    stockAlerts.forEach { alert ->
                        val targetPrice = alert.price.toDoubleOrNull() ?: 0.0
                        val isTriggered = if (alert.isAbove) currentPrice >= targetPrice else currentPrice <= targetPrice
                        
                        if (isTriggered && UserPreferences.getNotificationsEnabled(context)) {
                            NotificationHelper.showPriceAlert(
                                context = context,
                                symbol = stock.symbol,
                                currentPrice = stock.currentPrice,
                                targetPrice = alert.price,
                                isAbove = alert.isAbove
                            )
                            // Remove alert once triggered? User didn't specify, but usually good practice.
                            // For now I'll leave it to let them see it multiple times if price fluctuates.
                        }
                    }
                }
            }

            // Always update to ensure the list matches the saved symbols exactly
            stocks.clear()
            stocks.addAll(updatedStocks)
        }
    }

    suspend fun refreshAllData() {
        isLoading = true
        isNewsLoading = true
        
        // 1. Refresh Brokers
        val loadedBrokers = listOf(
            UserPreferences.loadBrokerData(context, "webull", "Webull"),
            UserPreferences.loadBrokerData(context, "moomoo", "Moomoo"),
            UserPreferences.loadBrokerData(context, "coinbase", "Coinbase"),
            UserPreferences.loadBrokerData(context, "syfe", "Syfe")
        )
        brokers.clear()
        brokers.addAll(loadedBrokers)

        // 2. Refresh Stock Prices & Colors (Parallel)
        refreshStockPricesOnly()

        // 3. Refresh News
        val savedSymbols = getSavedStockSymbols(context)
        if (savedSymbols.isNotEmpty()) {
            newsList.clear()
            val updatedNews = fetchPortfolioNews(savedSymbols)
            newsList.addAll(updatedNews)
        }

        isLoading = false
        isNewsLoading = false
    }

    // 1. Initial Data check - if empty, try to refresh once
    LaunchedEffect(Unit) {
        if (stocks.isEmpty()) {
            refreshAllData()
        }
    }

    // 2. 1-Minute Stock Auto-Refresh (only if there are stocks)
    LaunchedEffect(Unit) {
        while (true) {
            delay(60000.milliseconds) // 1 minute
            if (stocks.isNotEmpty()) {
                refreshStockPricesOnly()
            }
        }
    }

    // 3. Top-of-the-Hour Clock Trigger for Full Refresh
    LaunchedEffect(Unit) {
        while (true) {
            val now = Calendar.getInstance()
            val minutes = now.get(Calendar.MINUTE)
            val seconds = now.get(Calendar.SECOND)
            val millis = now.get(Calendar.MILLISECOND)

            val millisUntilNextHour = ((60 - minutes - 1) * 60 * 1000L) +
                    ((60 - seconds - 1) * 1000L) +
                    (1000L - millis)

            delay(millisUntilNextHour.milliseconds)

            refreshAllData()
        }
    }

    // 3. Pull-to-Refresh Gesture
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        containerColor = Color(0xFF1E1E2E),
        bottomBar = {
            AppBottomNavigation(
                onHomeClick = { /* Already Home */ },
                onGraphClick = onNavigateToTransactions,
                onSettingsClick = onNavigateToSettings,
                onBrokerageClick = onNavigateToBrokerage,
                highlightIndex = 0
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isNewsLoading || isLoading,
            onRefresh = {
                coroutineScope.launch {
                    refreshAllData()
                }
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding((screenWidth.value * 0.04f).dp)
            ) {
                HeaderSection(
                    userName = username
                )

                Spacer(modifier = Modifier.height((screenHeight.value * 0.025f).dp))

                val linkedBrokers = brokers.filter { it.isImported }
                if (linkedBrokers.isNotEmpty()) {
                    BrokerageHomeSection(linkedBrokers = linkedBrokers)
                    Spacer(modifier = Modifier.height((screenHeight.value * 0.025f).dp))
                }

                NewsSection(
                    newsList = newsList,
                    isLoading = isNewsLoading,
                    hasStocks = stocks.isNotEmpty(),
                    onRefresh = {
                        coroutineScope.launch {
                            refreshAllData()
                        }
                    }
                )

                Spacer(modifier = Modifier.height((screenHeight.value * 0.025f).dp))

                // Stocks Section Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "📊 Stocks",
                            color = Color.White,
                            fontSize = (screenWidth.value * 0.05f).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))

                        Box(
                            modifier = Modifier
                                .size((screenWidth.value * 0.07f).dp)
                                .background(Color(0xFF2B2B3D), shape = RoundedCornerShape(6.dp))
                                .clickable { 
                                    showAddDialog = true 
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add, 
                                contentDescription = "Add Stock",
                                tint = Color.White,
                                modifier = Modifier.size((screenWidth.value * 0.045f).dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height((screenHeight.value * 0.015f).dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.size((screenWidth.value * 0.1f).dp))
                    }
                } else if (stocks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No stocks added yet. Tap '+' to add.",
                            color = Color.Gray,
                            fontSize = (screenWidth.value * 0.035f).sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(stocks) { stock ->
                            StockCard(
                                stock = stock,
                                onCardClick = { onStockClick(stock) },
                                onDeleteClick = { itemToDelete ->
                                    removeStockSymbol(context, itemToDelete.symbol)
                                    stocks.remove(itemToDelete)
                                    coroutineScope.launch {
                                        refreshAllData()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onAddStock = { rawSymbol ->
                showAddDialog = false
                coroutineScope.launch {
                    isLoading = true
                    val fetchedStock = fetchSGXStockDynamic(rawSymbol)
                    if (fetchedStock != null) {
                        saveStockSymbol(context, rawSymbol)
                        refreshAllData()
                    } else {
                        showErrorDialog = true
                    }
                    isLoading = false
                }
            }
        )
    }

    if (showErrorDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            containerColor = Color(0xFF2B2B3D),
            title = { Text("Stock Not Found", color = Color.White) },
            text = { Text("The stock code you entered could not be found or the service is temporarily unavailable. Please check the code and try again.", color = Color.LightGray) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { showErrorDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("OK")
                }
            }
        )
    }
}