package com.example.investmenttracker.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.investmenttracker.R
import com.example.investmenttracker.UserPreferences
import com.example.investmenttracker.data.BrokerCardItem
import com.example.investmenttracker.data.LiveNewsItem
import com.example.investmenttracker.data.StockItem
import com.example.investmenttracker.data.fetchPortfolioNews
import com.example.investmenttracker.data.fetchSGXStockDynamic
import com.example.investmenttracker.getSavedStockSymbols
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

// Handles initial data loading and displays an animated logo during app startup.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplashScreen(
    onDataLoaded: () -> Unit,
    stocks: MutableList<StockItem>,
    newsList: MutableList<LiveNewsItem>,
    brokers: MutableList<BrokerCardItem>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Get screen dimensions for percentage calculation
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    var usernameInput by remember { mutableStateOf("") }
    var isNewUser by remember { mutableStateOf(UserPreferences.getUsername(context) == "User") }
    var isReadyToLoad by remember { mutableStateOf(!isNewUser) }
    val loadingText by remember { mutableStateOf("loading data...") }

    // Fading animation for loading text (slow flashing)
    val infiniteTransition = rememberInfiniteTransition(label = "loading_text")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Data Loading Logic
    LaunchedEffect(isReadyToLoad) {
        if (isReadyToLoad) {
            // 1. Initial Delay for visual effect
            delay(500.milliseconds)
            
            // 2. Fetch all necessary data
            coroutineScope.launch {
                // Refresh Brokers
                val loadedBrokers = listOf(
                    UserPreferences.loadBrokerData(context, "webull", "Webull"),
                    UserPreferences.loadBrokerData(context, "moomoo", "Moomoo"),
                    UserPreferences.loadBrokerData(context, "coinbase", "Coinbase"),
                    UserPreferences.loadBrokerData(context, "syfe", "Syfe")
                )
                brokers.clear()
                brokers.addAll(loadedBrokers)

                // Refresh Stocks and News
                val savedSymbols = getSavedStockSymbols(context)
                if (savedSymbols.isNotEmpty()) {
                    val updatedStocks = coroutineScope {
                        savedSymbols.map { symbol ->
                            async { fetchSGXStockDynamic(symbol) }
                        }.awaitAll().filterNotNull()
                    }
                    stocks.clear()
                    stocks.addAll(updatedStocks)

                    val updatedNews = fetchPortfolioNews(savedSymbols)
                    newsList.clear()
                    newsList.addAll(updatedNews)
                }

                delay(1000.milliseconds) // Ensure splash is seen
                onDataLoaded()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E1E2E), Color(0xFF161622))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Application Logo (15% of screen height)
            Box(
                modifier = Modifier
                    .size((screenHeight.value * 0.15f).dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF2B2B3D))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.image_secondary),
                    contentDescription = "App Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (!isReadyToLoad) {
                Spacer(modifier = Modifier.height(32.dp))
                
                // 2. User Registration (New User Registration Slip style)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFF2B2B3D), RoundedCornerShape(28.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(28.dp))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Box(modifier = Modifier.weight(1f)) {
                                if (usernameInput.isEmpty()) {
                                    Text("Enter Name", color = Color.Gray, fontSize = 16.sp)
                                }
                                BasicTextField(
                                    value = usernameInput,
                                    onValueChange = { usernameInput = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                                    cursorBrush = SolidColor(Color.White),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (usernameInput.isNotBlank()) {
                                        UserPreferences.saveUsername(context, usernameInput.trim())
                                        isReadyToLoad = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Confirm",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Loading Text at the bottom
        Text(
            text = loadingText,
            color = Color.Gray,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontStyle = FontStyle.Italic,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = (screenHeight.value * 0.08f).dp)
                .alpha(alpha),
            textAlign = TextAlign.Center
        )
    }
}
