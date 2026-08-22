package com.example.investmenttracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import com.example.investmenttracker.data.BrokerCardItem
import com.example.investmenttracker.data.LiveNewsItem
import com.example.investmenttracker.data.NotificationHelper
import com.example.investmenttracker.data.StockItem
import com.example.investmenttracker.data.fetchPortfolioNews
import com.example.investmenttracker.data.fetchSGXStockDynamic
import com.example.investmenttracker.ui.components.AddStockDialog
import com.example.investmenttracker.ui.components.BrokerageHomeSection
import com.example.investmenttracker.ui.components.HeaderSection
import com.example.investmenttracker.ui.components.NewsSection
import com.example.investmenttracker.ui.components.OutlinedSection
import com.example.investmenttracker.ui.components.StockCard
import com.example.investmenttracker.ui.screens.BrokerageScreen
import com.example.investmenttracker.ui.screens.SettingsScreen
import com.example.investmenttracker.ui.screens.SplashScreen
import com.example.investmenttracker.ui.screens.StockDetailsScreen
import com.example.investmenttracker.ui.screens.TransactionsScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.time.Duration.Companion.milliseconds

enum class CurrentScreen {
    SPLASH, HOME, SETTINGS, BROKERAGE, STOCK_DETAILS, TRANSACTIONS
}

class MainActivity : ComponentActivity() {
    // Entry point of the activity, sets up the Compose UI and initializes navigation.
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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

                // System Back Button Handling
                BackHandler(enabled = currentScreen != CurrentScreen.HOME && currentScreen != CurrentScreen.SPLASH) {
                    currentScreen = CurrentScreen.HOME
                }

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
                            onNavigateToTransactions = { currentScreen = CurrentScreen.TRANSACTIONS },
                            onStockClick = { stock ->
                                selectedStock = stock
                                currentScreen = CurrentScreen.STOCK_DETAILS
                            },
                            stocks = stocks,
                            newsList = newsList
                        )
                        CurrentScreen.SETTINGS -> SettingsScreen(
                            onBackToHome = { currentScreen = CurrentScreen.HOME },
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
                            onNavigateToBrokerage = { currentScreen = CurrentScreen.BROKERAGE },
                            stocks = stocks
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

// The main dashboard screen displaying user profile, news, and stock watchlist.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onStockClick: (StockItem) -> Unit,
    stocks: MutableList<StockItem>,
    newsList: MutableList<LiveNewsItem>
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var isNewsLoading by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showStockErrorPopup by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val username = remember { UserPreferences.getUsername(context).ifBlank { "User" } }

    var isMenuOpen by remember { mutableStateOf(false) }

    // Auto-hide error popup after 3 seconds
    LaunchedEffect(showStockErrorPopup) {
        if (showStockErrorPopup) {
            delay(3000)
            showStockErrorPopup = false
        }
    }

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

    // Fetches updated stock prices for all saved symbols in the background.
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
                    val currentPriceText = stock.currentPrice.replace("S$", "").replace("$", "").trim()
                    val currentPrice = currentPriceText.toDoubleOrNull() ?: 0.0

                    stockAlerts.forEach { alert ->
                        val targetPrice = alert.price.toDoubleOrNull() ?: 0.0
                        val isTriggered = if (alert.isAbove) currentPrice >= targetPrice else currentPrice <= targetPrice
                        
                        // Only notify if triggered, active, and global notifications are enabled
                        if (isTriggered && alert.isActive && UserPreferences.getNotificationsEnabled(context)) {
                            NotificationHelper.showPriceAlert(
                                context = context,
                                symbol = stock.symbol,
                                currentPrice = stock.currentPrice,
                                targetPrice = alert.price,
                                isAbove = alert.isAbove
                            )
                        }
                    }
                }
            }

            // Always update to ensure the list matches the saved symbols exactly
            stocks.clear()
            stocks.addAll(updatedStocks)
        }
    }

    // Performs a full data refresh including brokers, stock prices, and news.
    suspend fun refreshAllData() {
        try {
            isLoading = true
            isNewsLoading = true
            
            // 1. Refresh Brokers
            val loadedBrokers = listOf(
                UserPreferences.loadBrokerData(context, "webull", "Webull"),
                UserPreferences.loadBrokerData(context, "moomoo", "Moomoo"),
                UserPreferences.loadBrokerData(context, "coinbase", "Coinbase"),
                UserPreferences.loadBrokerData(context, "syfe", "Syfe")
            )

            // 2. Refresh Stock Prices & Colors (Parallel)
            refreshStockPricesOnly()

            // 3. Refresh News
            val savedSymbols = getSavedStockSymbols(context)
            if (savedSymbols.isNotEmpty()) {
                val updatedNews = fetchPortfolioNews(savedSymbols)
                
                // Check for new news to trigger System 2 notification
                if (updatedNews.isNotEmpty()) {
                    val latestItem = updatedNews.first()
                    val lastNotifiedTime = UserPreferences.getLastNewsTimestamp(context)
                    
                    // Only notify if this is a newer article than the last one seen
                    if (latestItem.timestamp > lastNotifiedTime && UserPreferences.getNotificationsEnabled(context)) {
                        NotificationHelper.showNewsAlert(context, latestItem.stockSymbol, latestItem.title)
                        UserPreferences.saveLastNewsTimestamp(context, latestItem.timestamp)
                    }
                }

                newsList.clear()
                newsList.addAll(updatedNews)
            }
        } catch (_: Exception) {
            // Silently ignore or handle error
        } finally {
            isLoading = false
            isNewsLoading = false
        }
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
        containerColor = Color(0xFF1E1E2E)
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
            ) {
                // 1. Header with integrated Navigation Menu
                HeaderSection(
                    userName = username,
                    isMenuOpen = isMenuOpen,
                    currentScreen = CurrentScreen.HOME,
                    onMenuToggle = { isMenuOpen = !isMenuOpen },
                    onNavigate = { screen ->
                        isMenuOpen = false
                        when (screen) {
                            CurrentScreen.HOME -> { /* Already here */ }
                            CurrentScreen.TRANSACTIONS -> onNavigateToTransactions()
                            CurrentScreen.SETTINGS -> onNavigateToSettings()
                            else -> {}
                        }
                    }
                )

                Spacer(modifier = Modifier.height((screenHeight.value * 0.02f).dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = (screenWidth.value * 0.04f).dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // 2. News Section with Outlined Backdrop
                        OutlinedSection(title = "News") {
                            NewsSection(
                                newsList = newsList,
                                isLoading = isNewsLoading,
                                hasStocks = stocks.isNotEmpty()
                            )
                        }

                        Spacer(modifier = Modifier.height((screenHeight.value * 0.03f).dp))

                        // 3. Stocks Section with Outlined Backdrop
                        OutlinedSection(
                            title = "Stocks",
                            action = {
                                Button(
                                    onClick = { showAddDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B3D)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Text("+ New Stock", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF4CAF50))
                                }
                            }
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                                if (isLoading && stocks.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                                    }
                                } else if (stocks.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Your watchlist is empty", color = Color.Gray)
                                    }
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(stocks) { stock ->
                                            StockCard(
                                                stock = stock,
                                                onCardClick = { onStockClick(stock) },
                                                onDeleteClick = { itemToDelete ->
                                                    removeStockSymbol(context, itemToDelete.symbol)
                                                    stocks.remove(itemToDelete)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Stock Not Found Pop-up Overlay
                    if (showStockErrorPopup) {
                        StockErrorPopup(
                            onDismiss = { showStockErrorPopup = false },
                            modifier = Modifier
                                .align(Alignment.Center)
                                .fillMaxWidth(0.9f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showAddDialog) {
        AddStockDialog(
            onDismiss = { showAddDialog = false },
            onAddStock = { rawSymbol ->
                showAddDialog = false
                coroutineScope.launch {
                    try {
                        isLoading = true
                        val fetchedStock = fetchSGXStockDynamic(rawSymbol)
                        if (fetchedStock != null) {
                            saveStockSymbol(context, rawSymbol)
                            refreshAllData()
                        } else {
                            showStockErrorPopup = true
                        }
                    } catch (_: Exception) {
                        showStockErrorPopup = true
                    } finally {
                        isLoading = false
                    }
                }
            }
        )
    }
}

// A centered pop-up that appears when a stock ticker cannot be found or there is a network error.
@Composable
fun StockErrorPopup(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(240.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Stock Invalid",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
                
                Text(
                    text = "Possible Cause :",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "1. No Internet Connection\n2. Wrong Stock Code",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                
                Spacer(modifier = Modifier.weight(0.1f))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "\u2192 Check and Try Again !",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("OK", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
