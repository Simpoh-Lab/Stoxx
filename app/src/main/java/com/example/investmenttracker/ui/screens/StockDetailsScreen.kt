package com.example.investmenttracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.investmenttracker.UserPreferences
import com.example.investmenttracker.data.LiveNewsItem
import com.example.investmenttracker.data.PriceAlert
import com.example.investmenttracker.data.StockItem
import com.example.investmenttracker.data.fetchPortfolioNews
import com.example.investmenttracker.data.fetchSGXStockDynamic
import com.example.investmenttracker.ui.components.LiveNewsCard
import com.example.investmenttracker.ui.components.SparklineChart
import kotlinx.coroutines.delay
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

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
                // Silently fail or log error in a real app
            }
            delay(10000)
        }
    }

    Scaffold(
        containerColor = Color(0xFF1E1E2E),
        bottomBar = {
            AppBottomNavigation(
                onHomeClick = onBackToHome,
                onGraphClick = onNavigateToTransactions,
                onSettingsClick = onNavigateToSettings,
                onBrokerageClick = onNavigateToBrokerage,
                highlightIndex = 1
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 1. Top Graph
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenHeight * 0.2f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
            ) {
                Box(modifier = Modifier.fillMaxSize().padding((screenWidth.value * 0.04f).dp), contentAlignment = Alignment.Center) {
                    SparklineChart(
                        data = currentStock.chartData,
                        lineColor = if (currentStock.isGain) Color(0xFF4CAF50) else Color(0xFFE57373),
                        showLatestPriceLine = true,
                        latestPrice = currentStock.currentPrice,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height((screenHeight.value * 0.02f).dp))

            // 2. Stock Title
            Column {
                Text(
                    text = "SGX : ${currentStock.symbol}",
                    color = Color.White,
                    fontSize = (screenWidth.value * 0.07f).sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = currentStock.name,
                    color = Color.Gray,
                    fontSize = (screenWidth.value * 0.045f).sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height((screenHeight.value * 0.03f).dp))

            // 3. Alerts and Information Split
            Row(modifier = Modifier.height((screenHeight.value * 0.2f).dp)) {
                // Left Column: Alerts Section
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .padding((screenWidth.value * 0.03f).dp)
                ) {
                    Text("Alerts", color = Color.White, fontSize = (screenWidth.value * 0.04f).sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height((screenHeight.value * 0.01f).dp))
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy((screenHeight.value * 0.01f).dp)
                    ) {
                        activeAlerts.take(3).forEach { alert ->
                            AlertRowItem(
                                alert = alert,
                                onToggle = {
                                    val updated = alert.copy(isActive = !alert.isActive)
                                    val index = activeAlerts.indexOf(alert)
                                    if (index != -1) activeAlerts[index] = updated
                                    UserPreferences.savePriceAlerts(context, activeAlerts)
                                },
                                onDelete = {
                                    activeAlerts.remove(alert)
                                    UserPreferences.savePriceAlerts(context, activeAlerts)
                                },
                                screenWidth = screenWidth
                            )
                        }
                    }

                    if (activeAlerts.size < 3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((screenHeight.value * 0.045f).dp)
                                .background(Color(0xFF3B3B52), shape = RoundedCornerShape(12.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .clickable { showAddAlertDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ADD +", color = Color.White, fontSize = (screenWidth.value * 0.035f).sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))

                // Right Column: Stock Information Grid
                Column(modifier = Modifier.weight(0.8f)) {
                    StockDetailInfoSection(stock = currentStock, screenHeight = screenHeight, screenWidth = screenWidth)
                }
            }

            Spacer(modifier = Modifier.height((screenHeight.value * 0.02f).dp))

            // 4. News Section
            Text(
                text = "News on the stock",
                color = Color.White,
                fontSize = (screenWidth.value * 0.045f).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height((screenHeight.value * 0.01f).dp))

            if (isNewsLoading) {
                Box(modifier = Modifier.fillMaxWidth().height((screenHeight.value * 0.18f).dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF4CAF50))
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy((screenWidth.value * 0.03f).dp)) {
                    items(newsList) { news ->
                        LiveNewsCard(news = news)
                    }
                }
            }
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

@Composable
fun AlertRowItem(alert: PriceAlert, onToggle: () -> Unit, onDelete: () -> Unit, screenWidth: androidx.compose.ui.unit.Dp) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Icon(
            imageVector = if (alert.isAbove) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = if (alert.isAbove) Color(0xFF4CAF50) else Color(0xFFE57373),
            modifier = Modifier.size((screenWidth.value * 0.05f).dp)
        )
        Text(
            text = alert.price,
            color = Color.White,
            fontSize = (screenWidth.value * 0.038f).sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f).padding(start = (screenWidth.value * 0.02f).dp)
        )
        
        // Toggle Pill
        Box(
            modifier = Modifier
                .width((screenWidth.value * 0.12f).dp)
                .height((screenWidth.value * 0.05f).dp)
                .background(
                    color = if (alert.isActive) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFE57373).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (alert.isActive) Color(0xFF4CAF50) else Color(0xFFE57373),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (alert.isActive) "ON" else "OFF",
                color = if (alert.isActive) Color(0xFF4CAF50) else Color(0xFFE57373),
                fontSize = (screenWidth.value * 0.025f).sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        
        Spacer(modifier = Modifier.width((screenWidth.value * 0.03f).dp))
        
        IconButton(onClick = onDelete, modifier = Modifier.size((screenWidth.value * 0.06f).dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size((screenWidth.value * 0.05f).dp))
        }
    }
}

@Composable
fun StockDetailInfoSection(stock: StockItem, screenHeight: androidx.compose.ui.unit.Dp, screenWidth: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy((screenHeight.value * 0.007f).dp)
    ) {
        DetailInfoRow(
            modifier = Modifier.weight(1f),
            label = "52-weeks HIGH", 
            value = "$${stock.high52Weeks}", 
            screenWidth = screenWidth
        )
        DetailInfoRow(
            modifier = Modifier.weight(1f),
            label = "52-weeks LOW", 
            value = "$${stock.low52Weeks}", 
            screenWidth = screenWidth
        )
        DetailInfoRow(
            modifier = Modifier.weight(1f),
            label = "Previous Close", 
            value = "$${stock.previousClose}", 
            screenWidth = screenWidth
        )
        DetailInfoRow(
            modifier = Modifier.weight(1f),
            label = "Market timing", 
            value = stock.marketTiming, 
            screenWidth = screenWidth
        )
    }
}

@Composable
fun DetailInfoRow(modifier: Modifier = Modifier, label: String, value: String, screenWidth: androidx.compose.ui.unit.Dp) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = (screenWidth.value * 0.03f).dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color.White, fontSize = (screenWidth.value * 0.022f).sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Color.Gray, fontSize = (screenWidth.value * 0.03f).sp, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
fun AppBottomNavigation(
    onHomeClick: () -> Unit,
    onGraphClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onBrokerageClick: () -> Unit,
    highlightIndex: Int = 1 // 0: Home, 1: Graph, 2: Settings, 3: Profile
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = (screenHeight.value * 0.08f).dp), // Extra padding for NO UI ZONE
        contentAlignment = Alignment.BottomCenter
    ) {
        // Fixed Menu Bar as per Sketch
        Row(
            modifier = Modifier
                .background(Color(0xFF2B2B3D), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = (screenWidth.value * 0.02f).dp, vertical = (screenHeight.value * 0.008f).dp),
            horizontalArrangement = Arrangement.spacedBy((screenWidth.value * 0.05f).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconSize = (screenWidth.value * 0.09f).dp
            val innerIconSize = (screenWidth.value * 0.06f).dp
            
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .size(iconSize)
                    .then(
                        if (highlightIndex == 0) Modifier.border(1.5.dp, Color(0xFF4CAF50), CircleShape)
                        else Modifier
                    )
            ) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = if (highlightIndex == 0) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(innerIconSize))
            }
            
            IconButton(
                onClick = onGraphClick,
                modifier = Modifier
                    .size(iconSize)
                    .then(
                        if (highlightIndex == 1) Modifier.border(1.5.dp, Color(0xFF4CAF50), CircleShape)
                        else Modifier
                    )
            ) {
                Icon(Icons.Default.BarChart, contentDescription = "Graph", tint = if (highlightIndex == 1) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(innerIconSize))
            }
            
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .size(iconSize)
                    .then(
                        if (highlightIndex == 3) Modifier.border(1.5.dp, Color(0xFF4CAF50), CircleShape)
                        else Modifier
                    )
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = if (highlightIndex == 3) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(innerIconSize))
            }
            
            IconButton(
                onClick = onBrokerageClick,
                modifier = Modifier
                    .size(iconSize)
                    .then(
                        if (highlightIndex == 2) Modifier.border(1.5.dp, Color(0xFF4CAF50), CircleShape)
                        else Modifier
                    )
            ) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = if (highlightIndex == 2) Color(0xFF4CAF50) else Color.White, modifier = Modifier.size(innerIconSize))
            }
        }
    }
}

@Composable
fun AddPriceAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean) -> Unit
) {
    var priceInput by remember { mutableStateOf("") }
    var isAboveSelected by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2B3D),
        title = { Text("Set Price Alert", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                color = if (isAboveSelected) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFF3B3B52),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { isAboveSelected = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Above",
                            color = if (isAboveSelected) Color(0xFF4CAF50) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(
                                color = if (!isAboveSelected) Color(0xFFE57373).copy(alpha = 0.2f) else Color(0xFF3B3B52),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { isAboveSelected = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Below",
                            color = if (!isAboveSelected) Color(0xFFE57373) else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Price (e.g. 12.34)", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50)
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formattedPrice = priceInput.toDoubleOrNull()?.let { 
                        String.format(java.util.Locale.US, "%.2f", it)
                    } ?: "0.00"
                    onConfirm(formattedPrice, isAboveSelected)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Active Notification")
            }
        }
    )
}

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
