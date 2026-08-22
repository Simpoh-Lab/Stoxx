package com.example.investmenttracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.investmenttracker.CurrentScreen
import com.example.investmenttracker.R
import com.example.investmenttracker.UserPreferences
import com.example.investmenttracker.data.StockItem
import com.example.investmenttracker.data.TransactionItem
import com.example.investmenttracker.data.fetchSGXStockDynamic
import com.example.investmenttracker.ui.components.HeaderSection
import com.example.investmenttracker.ui.components.SparklineChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Displays a history of investment transactions and provides tools for adding and filtering entries.
@Composable
fun TransactionsScreen(
    onBackToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBrokerage: () -> Unit,
    stocks: List<StockItem>
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }
    
    val transactionsMaster = remember { 
        mutableStateListOf<TransactionItem>().apply {
            addAll(UserPreferences.loadTransactions(context))
        }
    }

    // Filter States
    var selectedBroker by remember { mutableStateOf("All") }
    var startDate by remember { mutableStateOf<String?>(null) }
    var endDate by remember { mutableStateOf<String?>(null) }

    val filteredTransactions = remember(selectedBroker, startDate, endDate) {
        derivedStateOf {
            transactionsMaster.filter { item ->
                val matchesBroker = if (selectedBroker == "All") true else item.brokerage.equals(selectedBroker, ignoreCase = true)
                
                val matchesDate = try {
                    val itemDate = parseDate(item.date)
                    val start = startDate?.let { parseDate(it) }
                    val end = endDate?.let { parseDate(it) }
                    
                    (start == null || !itemDate.before(start)) && (end == null || !itemDate.after(end))
                } catch (e: Exception) { true }
                
                matchesBroker && matchesDate
            }
        }
    }.value

    val username = remember { UserPreferences.getUsername(context) }
    var transactionToDelete by remember { mutableStateOf<TransactionItem?>(null) }
    var expandedTransactionId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = Color(0xFF1E1E2E)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Header (10%)
            HeaderSection(
                userName = username,
                isMenuOpen = isMenuOpen,
                currentScreen = CurrentScreen.TRANSACTIONS,
                onMenuToggle = { isMenuOpen = !isMenuOpen },
                onNavigate = { screen ->
                    isMenuOpen = false
                    when (screen) {
                        CurrentScreen.HOME -> onBackToHome()
                        CurrentScreen.SETTINGS -> onNavigateToSettings()
                        else -> {}
                    }
                },
                customTitle = "Transactions",
                customSubtitle = "View Order History here!",
                showProfile = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Control Buttons (5%)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonHeight = 36.dp

                    // Brokers Pill
                    Box(
                        modifier = Modifier
                            .height(buttonHeight)
                            .background(Color(0xFF2B2B3D), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .clickable { onNavigateToBrokerage() }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Brokers", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Add Button
                        Box(
                            modifier = Modifier
                                .size(buttonHeight)
                                .background(Color(0xFF2B2B3D), RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { showAddDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        // Filter Button
                        Box(
                            modifier = Modifier
                                .size(buttonHeight)
                                .background(Color(0xFF2B2B3D), RoundedCornerShape(8.dp))
                                .border(
                                    1.dp,
                                    Color.Gray.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { showFilterDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Thin line across the screen
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Transactions List (75%)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    itemsIndexed(filteredTransactions) { index, item ->
                        TransactionCard(
                            item = item,
                            initialStockData = stocks.find { it.symbol.uppercase() == item.symbol.uppercase() },
                            isExpanded = expandedTransactionId == item.id,
                            onExpandToggle = {
                                expandedTransactionId = if (expandedTransactionId == item.id) null else item.id
                            },
                            onLongClick = {
                                if (!item.isAuto && item.brokerage == "Manual") {
                                    transactionToDelete = item
                                }
                            }
                        )
                    }
                }
            }

            // 4. NO UI ZONE (10%)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            containerColor = Color(0xFF2B2B3D),
            title = { Text("Delete Transaction", color = Color.White) },
            text = { Text("Are you sure you want to delete this manual transaction?", color = Color.LightGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        transactionToDelete?.let {
                            transactionsMaster.remove(it)
                            UserPreferences.saveTransactions(context, transactionsMaster)
                        }
                        transactionToDelete = null
                    }
                ) {
                    Text("Delete", color = Color(0xFFE57373))
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    if (showFilterDialog) {
        FilterTransactionsDialog(
            currentBroker = selectedBroker,
            currentStart = startDate,
            currentEnd = endDate,
            onDismiss = { showFilterDialog = false },
            onApply = { broker, start, end ->
                selectedBroker = broker
                startDate = start
                endDate = end
                showFilterDialog = false
            }
        )
    }

    if (showAddDialog) {
        NewTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { date, time, symbol, units, price, fee ->
                val newItem = TransactionItem(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    time = time,
                    symbol = symbol,
                    description = "Bought $units units $symbol @ \$$price",
                    amount = String.format(Locale.US, "%.2f", (units.toDoubleOrNull() ?: 0.0) * (price.toDoubleOrNull() ?: 0.0) + (fee.toDoubleOrNull() ?: 0.0)),
                    isDebit = true,
                    brokerage = "Manual",
                    isAuto = false,
                    units = units,
                    action = "Bought",
                    pricePerUnit = price,
                    fees = fee,
                    pnl = "+0.00",
                    pnlPercent = "0.00"
                )
                transactionsMaster.add(0, newItem)
                UserPreferences.saveTransactions(context, transactionsMaster)
                showAddDialog = false
            }
        )
    }
}

// Helper to parse dates with multiple possible formats (manual vs csv)
private fun parseDate(dateStr: String): Date {
    val formats = listOf("yyyy/MM/dd", "dd/MM/yyyy")
    for (format in formats) {
        try {
            return SimpleDateFormat(format, Locale.US).parse(dateStr)!!
        } catch (e: Exception) {}
    }
    return Date(0) // Fallback
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTransactionsDialog(
    currentBroker: String,
    currentStart: String?,
    currentEnd: String?,
    onDismiss: () -> Unit,
    onApply: (String, String?, String?) -> Unit
) {
    var broker by remember { mutableStateOf(currentBroker) }
    var startDate by remember { mutableStateOf(currentStart) }
    var endDate by remember { mutableStateOf(currentEnd) }
    
    var brokerExpanded by remember { mutableStateOf(false) }
    
    // Date Picker state management
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    
    if (showStartPicker || showEndPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false; showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val date = state.selectedDateMillis?.let {
                        SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(it))
                    }
                    if (showStartPicker) startDate = date else endDate = date
                    showStartPicker = false
                    showEndPicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Black)
        ) {
            Column(
                modifier = Modifier.padding(24.dp), 
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Filter Orders", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                
                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray.copy(alpha = 0.3f))

                // Broker Filter
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Source", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = brokerExpanded,
                        onExpandedChange = { brokerExpanded = it }
                    ) {
                        Box(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(broker, color = Color.White, fontSize = 14.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.Gray)
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = brokerExpanded,
                            onDismissRequest = { brokerExpanded = false }
                        ) {
                            listOf("All", "Webull SG", "Moomoo", "Syfe", "Coinbase", "Manual").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { broker = option; brokerExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Date Filter
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("From", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { showStartPicker = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(startDate ?: "YYYY/MM/DD", color = if (startDate != null) Color.White else Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("To", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clickable { showEndPicker = true }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(endDate ?: "YYYY/MM/DD", color = if (endDate != null) Color.White else Color.Gray, fontSize = 12.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(
                        onClick = { broker = "All"; startDate = null; endDate = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset", color = Color.Gray)
                    }
                    Button(
                        onClick = { onApply(broker, startDate, endDate) },
                        modifier = Modifier.weight(1.5f).height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply Filter", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
    item: TransactionItem,
    initialStockData: StockItem? = null,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    
    // Live stock data for this specific transaction's symbol
    var liveStockData by remember { mutableStateOf<StockItem?>(initialStockData) }

    // Fetch latest stock data if not present to get previousClose and chart
    LaunchedEffect(item.symbol) {
        if (liveStockData == null && item.symbol.isNotEmpty()) {
            try {
                val fetched = fetchSGXStockDynamic(item.symbol)
                if (fetched != null) {
                    liveStockData = fetched
                }
            } catch (e: Exception) {
                // Ignore fetch errors
            }
        }
    }

    // Calculate P/L relative to current live data
    val calculatedPnlData = remember(item, liveStockData) {
        val stock = liveStockData
        if (stock != null && item.action == "Bought") {
            val prevCloseNum = stock.previousClose.toDoubleOrNull() ?: 0.0
            val unitsNum = item.units.toDoubleOrNull() ?: 0.0
            val totalCostNum = item.amount.toDoubleOrNull() ?: 0.0
            
            // P/L relative to the latest market previous close
            val currentVal = prevCloseNum * unitsNum
            val pnl = currentVal - totalCostNum
            val pnlPercent = if (totalCostNum != 0.0) (pnl / totalCostNum) * 100.0 else 0.0
            
            val sign = if (pnl >= 0) "+" else "-"
            Pair(
                String.format(Locale.US, "%s$%.2f", sign, kotlin.math.abs(pnl)),
                String.format(Locale.US, "%s%.2f%%", sign, kotlin.math.abs(pnlPercent))
            )
        } else {
            val pnl = item.pnl
            val pnlPercent = item.pnlPercent
            // Ensure sign is present for consistency
            val sign = if (pnl.startsWith("+") || pnl.startsWith("-")) "" else "+"
            Pair("$sign$pnl", "$sign$pnlPercent%")
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onExpandToggle,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D)),
        border = if (isExpanded) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Summary Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Logo Circle
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E2E)),
                    contentAlignment = Alignment.Center
                ) {
                    val isManual = !item.brokerage.contains("Webull", ignoreCase = true) &&
                                   !item.brokerage.contains("Moomoo", ignoreCase = true) &&
                                   !item.brokerage.contains("Syfe", ignoreCase = true) &&
                                   !item.brokerage.contains("Coinbase", ignoreCase = true)

                    val brokerLogoRes = remember(item.brokerage) {
                        when {
                            item.brokerage.contains("Webull", ignoreCase = true) -> R.drawable.ic_broker_webull
                            item.brokerage.contains("Moomoo", ignoreCase = true) -> R.drawable.ic_broker_moomoo
                            item.brokerage.contains("Syfe", ignoreCase = true) -> R.drawable.ic_broker_syfe
                            item.brokerage.contains("Coinbase", ignoreCase = true) -> R.drawable.ic_broker_coinbase
                            else -> R.drawable.ic_application_submark
                        }
                    }
                    Image(
                        painter = painterResource(id = brokerLogoRes),
                        contentDescription = item.brokerage,
                        modifier = Modifier
                            .size(if (isManual) 24.dp else 20.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "${item.date}  |  ${item.description} in ${item.brokerage}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }

            // Expanded Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        // Left: Info Grid
                        Column(modifier = Modifier.weight(1f)) {
                            DetailText("Broker", item.brokerage)
                            DetailText("Date", item.date)
                            DetailText("Time", item.time.ifEmpty { "Unknown" })
                            DetailText("Unit", item.units)
                        }

                        // Middle: Financials
                        Column(modifier = Modifier.weight(1f)) {
                            DetailText("Price", "$${item.pricePerUnit}")
                            DetailText("Fee", "$${item.fees}")
                            DetailText("Total", "$${item.amount}")
                            DetailText(
                                "P/L", 
                                "${calculatedPnlData.first} (${calculatedPnlData.second})", 
                                color = if (calculatedPnlData.first.startsWith("+")) Color(0xFF4CAF50) else Color(0xFFE57373)
                            )
                        }

                        // Right: Graph
                        Box(
                            modifier = Modifier
                                .weight(1.2f)
                                .height(100.dp)
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            val chartData = liveStockData?.chartData ?: listOf(10f, 15f, 12f, 18f, 16f, 22f)
                            SparklineChart(
                                data = chartData,
                                lineColor = if (liveStockData?.isGain == false) Color(0xFFE57373) else Color(0xFF4CAF50),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailText(label: String, value: String, color: Color = Color.White) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", color = Color.Gray, fontSize = 11.sp)
        Text(value, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, String) -> Unit
) {
    val context = LocalContext.current
    
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())) }
    var time by remember { mutableStateOf(SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())) }
    var symbol by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("1") }
    var price by remember { mutableStateOf("1.00") }
    var fee by remember { mutableStateOf("1.00") }
    
    var isCheckingSymbol by remember { mutableStateOf(false) }
    var isSymbolValidRemote by remember { mutableStateOf<Boolean?>(null) } // null = unchecked, true = valid, false = invalid

    // Validation Logic
    val isDateValid = remember(date) {
        try {
            val selected = SimpleDateFormat("yyyy/MM/dd", Locale.US).parse(date)
            val today = SimpleDateFormat("yyyy/MM/dd", Locale.US).parse(SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date()))
            selected != null && today != null && !selected.after(today)
        } catch (e: Exception) { false }
    }

    val isTimeValid = remember(date, time) {
        if (!isDateValid) return@remember false
        try {
            val now = Date()
            val sdf = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US)
            val selectedFull = sdf.parse("$date $time")
            selectedFull != null && !selectedFull.after(now)
        } catch (e: Exception) { false }
    }

    val isSymbolFormatValid = remember(symbol) { 
        symbol.matches("^[A-Z0-9]+\\.SI$".toRegex()) 
    }
    
    val isUnitsValid = remember(units) { 
        val u = units.toDoubleOrNull() ?: 0.0
        u >= 1.0 
    }
    
    val isPriceValid = remember(price) { 
        val p = price.toDoubleOrNull() ?: 0.0
        p >= 0.01
    }
    
    val isFeeValid = remember(fee) { 
        val f = fee.toDoubleOrNull() ?: 0.0
        f >= 0.01
    }
    
    // Live symbol validation
    LaunchedEffect(symbol) {
        if (isSymbolFormatValid) {
            isCheckingSymbol = true
            isSymbolValidRemote = null
            try {
                val stock = fetchSGXStockDynamic(symbol)
                isSymbolValidRemote = (stock != null)
            } catch (e: Exception) {
                isSymbolValidRemote = false
            } finally {
                isCheckingSymbol = false
            }
        } else {
            isSymbolValidRemote = null
        }
    }

    val isFormValid = isDateValid && isTimeValid && isSymbolFormatValid && 
                     (isSymbolValidRemote == true) && isUnitsValid && isPriceValid && isFeeValid

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date(it))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                modifier = Modifier.padding(16.dp), 
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Transaction Slip", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                SlipInputRow(
                    label = "Date", 
                    value = date, 
                    icon = Icons.Default.CalendarToday, 
                    isValid = isDateValid,
                    onClick = { showDatePicker = true }
                )
                
                SlipInputRow(
                    label = "Time", 
                    value = time, 
                    icon = Icons.Default.Schedule,
                    isValid = isTimeValid
                ) { time = it }
                
                // Symbol with format validation and remote check hint
                Column {
                    SlipInputRow(
                        label = "Symbol", 
                        value = symbol, 
                        placeholder = "e.g. D05.SI",
                        isValid = (isSymbolFormatValid && isSymbolValidRemote != false) || symbol.isEmpty(),
                        icon = if (isCheckingSymbol) null else if (isSymbolValidRemote == true) Icons.Default.CheckCircle else null
                    ) { 
                        symbol = it.uppercase().trim() 
                    }
                    if (isCheckingSymbol) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 8.dp),
                            color = Color(0xFF4CAF50),
                            trackColor = Color.Transparent
                        )
                    }
                }
                
                // Number Inputs with Steppers and validation
                SlipNumberInputRow("Unit", units, increment = 1.0, isValid = isUnitsValid) { units = it }
                SlipNumberInputRow("Price", price, increment = 0.01, prefix = "$", isValid = isPriceValid) { price = it }
                SlipNumberInputRow("Fee", fee, increment = 0.01, prefix = "$", isValid = isFeeValid) { fee = it }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text("Cancel", fontWeight = FontWeight.Bold) }
                    
                    Button(
                        onClick = { if (isFormValid) onConfirm(date, time, symbol, units, price, fee) },
                        enabled = isFormValid,
                        modifier = Modifier.weight(1f).height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Text("Confirm", fontWeight = FontWeight.Bold, color = if (isFormValid) Color.White else Color.Gray) 
                    }
                }
            }
        }
    }
}

@Composable
fun SlipNumberInputRow(
    label: String,
    value: String,
    increment: Double,
    prefix: String? = null,
    isValid: Boolean = true,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp, 
                    color = if (isValid) Color.Gray.copy(alpha = 0.5f) else Color(0xFFE57373), 
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prefix != null) Text(prefix, color = Color.White, modifier = Modifier.padding(end = 4.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(color = if (isValid) Color.White else Color(0xFFE57373), fontSize = 15.sp),
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
                            val current = value.toDoubleOrNull() ?: 0.0
                            val newVal = current + increment
                            onValueChange(if (increment < 1.0) String.format(Locale.US, "%.2f", newVal) else newVal.toInt().toString())
                        }
                    )
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrease",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp).clickable {
                            val current = value.toDoubleOrNull() ?: 0.0
                            val newVal = Math.max(0.0, current - increment)
                            onValueChange(if (increment < 1.0) String.format(Locale.US, "%.2f", newVal) else newVal.toInt().toString())
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SlipInputRow(
    label: String, 
    value: String, 
    placeholder: String = "",
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    prefix: String? = null,
    isValid: Boolean = true,
    onClick: (() -> Unit)? = null,
    onValueChange: ((String) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp, 
                    color = if (isValid) Color.Gray.copy(alpha = 0.5f) else Color(0xFFE57373), 
                    shape = RoundedCornerShape(8.dp)
                )
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (prefix != null) Text(prefix, color = Color.White, modifier = Modifier.padding(end = 4.dp))
                if (onValueChange != null) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(placeholder, color = Color.Gray, fontSize = 15.sp)
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            textStyle = TextStyle(color = if (isValid) Color.White else Color(0xFFE57373), fontSize = 15.sp),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                } else {
                    Text(value, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                }
                if (icon != null) Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}
