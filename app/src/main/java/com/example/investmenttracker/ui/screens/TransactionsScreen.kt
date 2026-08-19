package com.example.investmenttracker.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.investmenttracker.UserPreferences
import com.example.investmenttracker.data.TransactionItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun TransactionsScreen(
    onBackToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToBrokerage: () -> Unit
) {
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    
    // Load persisted transactions
    val transactions = remember { 
        mutableStateListOf<TransactionItem>().apply {
            addAll(UserPreferences.loadTransactions(context))
        }
    }

    // Default mock if empty
    LaunchedEffect(Unit) {
        if (transactions.isEmpty()) {
            val initial = listOf(
                TransactionItem(
                    id = UUID.randomUUID().toString(),
                    date = "19/8/2026",
                    description = "Bought 100 unit D05 using Webull",
                    amount = "1014.08",
                    isDebit = true,
                    brokerage = "Webull",
                    isAuto = false,
                    units = "100",
                    action = "Bought",
                    pricePerUnit = "10.12",
                    fees = "1.91",
                    pnl = "12.34",
                    pnlPercent = "1.23"
                )
            )
            transactions.addAll(initial)
            UserPreferences.saveTransactions(context, transactions)
        }
    }

    Scaffold(
        containerColor = Color(0xFF1E1E2E),
        bottomBar = {
            AppBottomNavigation(
                onHomeClick = onBackToHome,
                onGraphClick = { /* Current */ },
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
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Transactions",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subheader
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Brokerage / Manual",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF2B2B3D), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .clickable { showAddDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF2B2B3D), shape = RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.3f))

            // List
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                itemsIndexed(transactions) { index, item ->
                    TransactionRow(
                        item = item,
                        showBackground = index % 2 != 0
                    )
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.1f))
                }
                
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalDivider(modifier = Modifier.width(60.dp), color = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Up-To-Date",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        NewTransactionDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { date, action, brokerage, price, units ->
                val description = if (action == "Dividend") {
                    "Dividend for D05 received at $brokerage"
                } else {
                    "$action $units unit D05 using $brokerage"
                }
                val newItem = TransactionItem(
                    id = UUID.randomUUID().toString(),
                    date = date,
                    description = description,
                    amount = price,
                    isDebit = action == "Bought",
                    brokerage = brokerage,
                    isAuto = false,
                    units = units,
                    action = action,
                    pricePerUnit = try { 
                        val p = price.toDoubleOrNull() ?: 0.0
                        val u = units.toDoubleOrNull() ?: 1.0
                        if (u != 0.0) String.format(Locale.US, "%.2f", p / u) else "0.00"
                    } catch (e: Exception) { "0.00" },
                    fees = "0.00",
                    pnl = "0.00",
                    pnlPercent = "0.00"
                )
                transactions.add(0, newItem)
                UserPreferences.saveTransactions(context, transactions)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String) -> Unit
) {
    var date by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var action by remember { mutableStateOf("Bought") }
    var brokerage by remember { mutableStateOf("Webull") }
    var price by remember { mutableStateOf("12.34") }
    var units by remember { mutableStateOf("100") }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var actionExpanded by remember { mutableStateOf(false) }
    var brokerageExpanded by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
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
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "New Transaction",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "- Manual Input -",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Date Field
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Date", color = Color.White, fontSize = 20.sp, modifier = Modifier.width(100.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .background(Color(0xFF1E1E2E), RoundedCornerShape(4.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .clickable { showDatePicker = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(date, color = Color.White, fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Units Field
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Units", color = Color.White, fontSize = 20.sp, modifier = Modifier.width(100.dp))
                    TransactionNumberInputField(
                        modifier = Modifier.weight(1f),
                        value = units,
                        onValueChange = { units = it },
                        onIncrement = {
                            val u = units.toIntOrNull() ?: 0
                            units = (u + 1).toString()
                        },
                        onDecrement = {
                            val u = units.toIntOrNull() ?: 0
                            if (u > 0) units = (u - 1).toString()
                        },
                        keyboardType = KeyboardType.Number
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Action Dropdown
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Action", color = Color.White, fontSize = 20.sp, modifier = Modifier.width(100.dp))
                    ExposedDropdownMenuBox(
                        expanded = actionExpanded,
                        onExpandedChange = { actionExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(action, color = if (action == "Bought") Color(0xFFE57373) else Color(0xFF4CAF50), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = actionExpanded,
                            onDismissRequest = { actionExpanded = false },
                            modifier = Modifier.background(Color(0xFF2B2B3D))
                        ) {
                            listOf("Bought", "Sold", "Dividend").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = {
                                        action = option
                                        actionExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Brokerage Dropdown
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Brokerage", color = Color.White, fontSize = 20.sp, modifier = Modifier.width(100.dp))
                    ExposedDropdownMenuBox(
                        expanded = brokerageExpanded,
                        onExpandedChange = { brokerageExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color(0xFF1E1E2E), RoundedCornerShape(4.dp))
                                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(brokerage, color = Color.White, fontSize = 15.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }
                        ExposedDropdownMenu(
                            expanded = brokerageExpanded,
                            onDismissRequest = { brokerageExpanded = false },
                            modifier = Modifier.background(Color(0xFF2B2B3D))
                        ) {
                            listOf("Webull", "Moomoo", "Coinbase", "Syfe").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
                                    onClick = {
                                        brokerage = option
                                        brokerageExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Price Field
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.width(100.dp)) {
                        Text("Price", color = Color.White, fontSize = 20.sp)
                        Text("(including fee)", color = Color.Gray, fontSize = 10.sp)
                    }
                    TransactionNumberInputField(
                        modifier = Modifier.weight(1f),
                        value = price,
                        onValueChange = { price = it },
                        onIncrement = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            price = String.format(Locale.US, "%.2f", p + 0.01)
                        },
                        onDecrement = {
                            val p = price.toDoubleOrNull() ?: 0.0
                            if (p > 0) price = String.format(Locale.US, "%.2f", Math.max(0.0, p - 0.01))
                        },
                        prefix = "$",
                        keyboardType = KeyboardType.Decimal
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Horizontal Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .background(Color(0xFFE57373).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFE57373), RoundedCornerShape(12.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", color = Color(0xFFE57373), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    // Confirm Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                            .clickable { onConfirm(date, action, brokerage, price, units) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Confirm", color = Color(0xFF4CAF50), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionNumberInputField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(Color(0xFF1E1E2E), RoundedCornerShape(4.dp))
            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (prefix != null) {
                Text(prefix, color = Color.White, modifier = Modifier.padding(end = 4.dp))
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increment",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp).clickable { onIncrement() }
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Decrement",
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp).clickable { onDecrement() }
                )
            }
        }
    }
}

@Composable
fun TransactionRow(item: TransactionItem, showBackground: Boolean) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (showBackground) Color.Gray.copy(alpha = 0.05f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.date,
                color = Color.White,
                fontSize = 13.sp,
                modifier = Modifier.width(85.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )
            }
            
            Text(
                text = "${if (item.isDebit) "-" else "+"}\$${item.amount}",
                color = if (item.isDebit) Color(0xFFE57373) else Color(0xFF4CAF50),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 85.dp, top = 8.dp)
                    .fillMaxWidth()
            ) {
                if (item.pricePerUnit.isNotEmpty()) {
                    Text(
                        text = "· D05 @ ${item.pricePerUnit}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                if (item.fees.isNotEmpty()) {
                    Text(
                        text = "· Fee @ ${item.fees}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
                if (item.pnl.isNotEmpty()) {
                    val pnlColor = if (item.pnl.startsWith("-")) Color(0xFFE57373) else Color(0xFF4CAF50)
                    Text(
                        text = "· Profit/Loss @ ${item.pnl} (${item.pnlPercent}%)",
                        color = pnlColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
