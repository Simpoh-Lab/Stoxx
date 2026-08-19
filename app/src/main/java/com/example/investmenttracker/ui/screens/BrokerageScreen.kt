package com.example.investmenttracker.ui.screens

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.investmenttracker.data.BrokerCardItem
import com.example.investmenttracker.data.*
import com.example.investmenttracker.UserPreferences
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BrokerageScreen(
    onBackToHome: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val brokers = remember {
        mutableStateListOf<BrokerCardItem>(
            UserPreferences.loadBrokerData(context, "webull", "Webull"),
            UserPreferences.loadBrokerData(context, "moomoo", "Moomoo"),
            UserPreferences.loadBrokerData(context, "coinbase", "Coinbase"),
            UserPreferences.loadBrokerData(context, "syfe", "Syfe"),
        )
    }

    var activeBrokerForImport by remember { mutableStateOf<BrokerCardItem?>(null) }
    var brokerToUnbind by remember { mutableStateOf<BrokerCardItem?>(null) }
    var importStatus by remember { mutableStateOf<ImportStatus>(ImportStatus.Idle) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            activeBrokerForImport?.let { broker ->
                coroutineScope.launch {
                    importStatus = ImportStatus.Checking
                    delay(1500.milliseconds) // Mock validation time
                    
                    val result = CsvHandler.processCsvImport(selectedUri, context)
                    
                    if (result.isValid) {
                        importStatus = ImportStatus.Found
                        delay(2000.milliseconds) // Show "Found!" state as per image
                        
                        val index = brokers.indexOfFirst { it.id == broker.id }
                        if (index != -1) {
                            val updatedBroker = broker.copy(
                                isImported = true,
                                accountValue = result.accountValue,
                                accountNumber = result.accountNumber,
                                hasBalance = result.hasBalance,
                                hasTransactions = result.hasTransactions
                            )
                            brokers[index] = updatedBroker
                            UserPreferences.saveBrokerData(context, updatedBroker)
                            
                            // Save imported transactions for visibility in Transactions screen
                            if (result.importedTransactions.isNotEmpty()) {
                                val currentTx = UserPreferences.loadTransactions(context).toMutableList()
                                currentTx.addAll(0, result.importedTransactions)
                                UserPreferences.saveTransactions(context, currentTx)
                            }
                        }
                        importStatus = ImportStatus.Idle
                    } else {
                        importStatus = ImportStatus.Invalid
                        delay(2500.milliseconds) // Show "Invalid" state as per image
                        importStatus = ImportStatus.Idle
                    }
                }
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF1E1E2E),
        bottomBar = {
            AppBottomNavigation(
                onHomeClick = onBackToHome,
                onGraphClick = onNavigateToTransactions,
                onSettingsClick = onNavigateToSettings,
                onBrokerageClick = { /* Current */ },
                highlightIndex = 2
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                BrokerageHeaderSection(onHomeClick = onBackToHome)

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(brokers) { broker ->
                        BrokerGridCard(
                            broker = broker,
                        ) {
                            if (!broker.isImported) {
                                activeBrokerForImport = broker
                                filePickerLauncher.launch("text/*")
                            } else {
                                brokerToUnbind = broker
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "More Brokers Coming...",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Unbind Confirmation Dialog
            if (brokerToUnbind != null) {
                AlertDialog(
                    onDismissRequest = { brokerToUnbind = null },
                    containerColor = Color(0xFF2B2B3D),
                    title = { Text("Unbind Broker", color = Color.White) },
                    text = { Text("Are you sure you want to unbind ${brokerToUnbind?.name}?", color = Color.LightGray) },
                    confirmButton = {
                        TextButton(onClick = {
                            brokerToUnbind?.let { broker ->
                                UserPreferences.clearBrokerData(context, broker.id)
                                val index = brokers.indexOfFirst { it.id == broker.id }
                                if (index != -1) {
                                    brokers[index] = BrokerCardItem(id = broker.id, name = broker.name, isImported = false)
                                }
                            }
                            brokerToUnbind = null
                        }) {
                            Text("Unbind", color = Color(0xFFE57373))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { brokerToUnbind = null }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                )
            }

            // Validation Overlays
            if (importStatus != ImportStatus.Idle) {
                ImportStatusOverlay(status = importStatus)
            }
        }
    }
}

enum class ImportStatus {
    Idle, Checking, Found, Invalid
}

@Composable
fun ImportStatusOverlay(status: ImportStatus) {
    Dialog(onDismissRequest = {}) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .height(350.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Brokerage",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFF3B3B52), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                    }
                    Box(
                        modifier = Modifier.size(48.dp).background(Color(0xFF3B3B52), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null, tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                when (status) {
                    ImportStatus.Checking -> {
                        CircularProgressIndicator(color = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Checking file...", color = Color.Gray, fontSize = 14.sp)
                    }
                    ImportStatus.Found -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Valid File", color = Color(0xFF4CAF50), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("importing data...", color = Color.Gray, fontSize = 13.sp)
                    }
                    ImportStatus.Invalid -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFE57373), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Invalid File", color = Color(0xFFE57373), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("please try again...", color = Color.Gray, fontSize = 13.sp)
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun BrokerageHeaderSection(onHomeClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Brokerage",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun BrokerGridCard(
    broker: BrokerCardItem,
    onCardClick: () -> Unit,
) {
    val context = LocalContext.current

    val iconResId = remember(broker.id) {
        context.resources.getIdentifier("ic_broker_${broker.id}", "drawable", context.packageName)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            if (iconResId != 0) {
                Image(
                    painter = painterResource(id = iconResId),
                    contentDescription = broker.name,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = broker.name,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clickable { onCardClick() },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (broker.isImported) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(36.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Transaction History",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "- Click Again To Unbind -",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Import CSV",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CSV File",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "(import transaction history)",
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}