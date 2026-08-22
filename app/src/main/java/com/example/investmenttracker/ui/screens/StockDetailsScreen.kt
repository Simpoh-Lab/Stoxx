package com.example.investmenttracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.investmenttracker.CurrentScreen
import com.example.investmenttracker.UserPreferences
import com.example.investmenttracker.data.LiveNewsItem
import com.example.investmenttracker.data.PriceAlert
import com.example.investmenttracker.data.StockItem
import com.example.investmenttracker.data.fetchPortfolioNews
import com.example.investmenttracker.data.fetchSGXStockDynamic
import com.example.investmenttracker.ui.components.HeaderSection
import com.example.investmenttracker.ui.components.NewsSection
import com.example.investmenttracker.ui.components.OutlinedSection
import com.example.investmenttracker.ui.components.SparklineChart
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

// Provides a detailed view of a selected stock, including its price history, alerts, and relevant news.
@Composable
fun StockDetailsScreen(
    stock: StockItem,
    onBackToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBrokerage: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    var currentStock by remember { mutableStateOf(stock) }
    val newsList = remember { mutableStateListOf<LiveNewsItem>() }
    var isNewsLoading by remember { mutableStateOf(false) }
    
    val activeAlerts = remember { mutableStateListOf<PriceAlert>() }
    var showAddAlertDialog by remember { mutableStateOf(false) }
    var showSuccessOverlay by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(stock.symbol) {
        isNewsLoading = true
        val savedAlerts = UserPreferences.loadPriceAlerts(context).filter { it.symbol == stock.symbol }
        activeAlerts.clear()
        activeAlerts.addAll(savedAlerts)
        
        val updatedNews = fetchPortfolioNews(listOf(stock.symbol))
        newsList.clear()
        newsList.addAll(updatedNews)
        isNewsLoading = false

        // 10-second polling for stock data auto-update
        while (true) {
            try {
                val updatedStock = fetchSGXStockDynamic(stock.symbol)
                if (updatedStock != null) {
                    currentStock = updatedStock
                }
            } catch (e: Exception) {
                // Silently fail
            }
            kotlinx.coroutines.delay(10000)
        }
    }

    Scaffold(
        containerColor = Color(0xFF1E1E2E)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Top Bar (10%)
            HeaderSection(
                userName = UserPreferences.getUsername(context),
                isMenuOpen = isMenuOpen,
                currentScreen = CurrentScreen.STOCK_DETAILS,
                onMenuToggle = { isMenuOpen = !isMenuOpen },
                onNavigate = { screen ->
                    isMenuOpen = false
                    when (screen) {
                        CurrentScreen.HOME -> onBackToHome()
                        CurrentScreen.TRANSACTIONS -> onNavigateToTransactions()
                        CurrentScreen.SETTINGS -> onNavigateToSettings()
                        else -> {}
                    }
                },
                customTitle = "Stock Detailed View",
                customSubtitle = "Analyze your stock here!",
                showProfile = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Graph Section (30%)
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenHeight.value * 0.3f).dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SparklineChart(
                            data = currentStock.chartData,
                            lineColor = if (currentStock.isGain) Color(0xFF4CAF50) else Color(0xFFE57373),
                            showLatestPriceLine = true,
                            latestPrice = currentStock.currentPrice,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Information & Alerts Section (25%)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenHeight.value * 0.26f).dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left: Stock Info
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedSection(title = "Stock") {
                        StockDetailInfoSection(stock = currentStock)
                    }
                }

                // Right: Alerts
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedSection(
                        title = "Alert",
                        action = {
                            if (activeAlerts.size < 5) {
                                Button(
                                    onClick = { showAddAlertDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2B2B3D)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                    modifier = Modifier.height(32.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f))
                                ) {
                                    Text("+ New Alert", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF4CAF50))
                                }
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeAlerts.take(5).forEach { alert ->
                                AlertRowItem(
                                    alert = alert,
                                    onToggle = {
                                        val updated = alert.copy(isActive = !alert.isActive)
                                        val index = activeAlerts.indexOf(alert)
                                        if (index != -1) {
                                            activeAlerts[index] = updated
                                            val masterList = UserPreferences.loadPriceAlerts(context).toMutableList()
                                            masterList.removeAll { it.id == alert.id }
                                            masterList.add(updated)
                                            UserPreferences.savePriceAlerts(context, masterList)
                                        }
                                    },
                                    onDelete = {
                                        activeAlerts.remove(alert)
                                        val masterList = UserPreferences.loadPriceAlerts(context).toMutableList()
                                        masterList.removeAll { it.id == alert.id }
                                        UserPreferences.savePriceAlerts(context, masterList)
                                    }
                                )
                            }
                            
                            if (activeAlerts.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("No alerts set", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. News Section (25%)
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedSection(title = "News") {
                    NewsSection(
                        newsList = newsList,
                        isLoading = isNewsLoading,
                        hasStocks = true
                    )
                }
            }

            // 5. NO UI ZONE (10%)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (showAddAlertDialog) {
        AddPriceAlertDialog(
            onDismiss = { showAddAlertDialog = false },
            onConfirm = { price, isAbove ->
                val newAlert = PriceAlert(id = UUID.randomUUID().toString(), symbol = stock.symbol, price = price, isAbove = isAbove)
                activeAlerts.add(newAlert)
                UserPreferences.savePriceAlerts(context, UserPreferences.loadPriceAlerts(context) + newAlert)
                showAddAlertDialog = false
                showSuccessOverlay = true
            }
        )
    }

    if (showSuccessOverlay) {
        NotificationSuccessOverlay(onDismiss = { showSuccessOverlay = false })
    }
}

// Represents a single price alert setting within the details screen's alert list.
@Composable
fun AlertRowItem(alert: PriceAlert, onToggle: () -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val color = if (alert.isAbove) Color(0xFF4CAF50) else Color(0xFFE57373)
        val icon = if (alert.isAbove) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = alert.price,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Toggle
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(18.dp)
                .background(
                    color = if (alert.isActive) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFE57373).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(9.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (alert.isActive) Color(0xFF4CAF50) else Color(0xFFE57373),
                    shape = RoundedCornerShape(9.dp)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (alert.isActive) "ON" else "OFF",
                color = if (alert.isActive) Color(0xFF4CAF50) else Color(0xFFE57373),
                fontSize = 8.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(14.dp))
        }
    }
}

// Displays a grid of key financial statistics and market information for a specific stock.
@Composable
fun StockDetailInfoSection(stock: StockItem) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        DetailInfoRow(label = "Symbol", value = stock.symbol + ".SI")
        DetailInfoRow(label = "52-week High", value = "$" + stock.high52Weeks)
        DetailInfoRow(label = "52-week Low", value = "$" + stock.low52Weeks)
        DetailInfoRow(label = "Prev Close", value = "$" + stock.previousClose)
        DetailInfoRow(label = "Type", value = stock.type)
        DetailInfoRow(label = "Market Time", value = stock.marketTiming)
    }
}

// A reusable row component for displaying a label and its corresponding value in the info section.
@Composable
fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2B2B3D), RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

// A popup dialog for creating new price alerts for the current stock.
@Composable
fun AddPriceAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var priceInput by remember { mutableStateOf("0.00") }
    var isAboveSelected by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "New Alert",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                // 1. Direction Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Direction",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(90.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Below Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isAboveSelected) Color(0xFFE57373) else Color.Transparent)
                                .clickable { isAboveSelected = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Below \u2193",
                                color = if (!isAboveSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Above Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isAboveSelected) Color(0xFF4CAF50) else Color.Transparent)
                                .clickable { isAboveSelected = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Above \u2191",
                                color = if (isAboveSelected) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 2. Price Row with Steppers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Price",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(90.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("$ ", color = Color.White, fontSize = 15.sp)
                            BasicTextField(
                                value = priceInput,
                                onValueChange = { priceInput = it },
                                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                cursorBrush = SolidColor(Color.White),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            
                            // Steppers
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Increase",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp).clickable {
                                        val current = priceInput.toDoubleOrNull() ?: 0.0
                                        priceInput = String.format(Locale.US, "%.2f", current + 0.01)
                                    }
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Decrease",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp).clickable {
                                        val current = priceInput.toDoubleOrNull() ?: 0.0
                                        priceInput = String.format(Locale.US, "%.2f", Math.max(0.0, current - 0.01))
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373).copy(alpha = 0.8f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = {
                            val formattedPrice = priceInput.toDoubleOrNull()?.let { 
                                String.format(Locale.US, "%.2f", it)
                            } ?: "0.00"
                            onConfirm(formattedPrice, isAboveSelected)
                        },
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Shows a confirmation message after a price alert has been successfully created.
@Composable
fun NotificationSuccessOverlay(onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(5000.milliseconds)
        onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .height(350.dp),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Notification",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Remember to Turn on at",
                    color = Color.LightGray,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "≡ -> App Setting -> Turn on Notification",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
