package com.example.investmenttracker.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    
    var usernameInput by remember { mutableStateOf("") }
    var isNewUser by remember { mutableStateOf(UserPreferences.getUsername(context) == "User") }
    var isReadyToLoad by remember { mutableStateOf(!isNewUser) }
    var loadingText by remember { mutableStateOf("Loading app data...") }

    // Fading animation for loading text
    val infiniteTransition = rememberInfiniteTransition(label = "loading_text")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
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
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(horizontal = (screenWidth.value * 0.08f).dp)
        ) {
            // App Logo (Dynamic Canvas Drawing) - ~35% of screen width
            Box(
                modifier = Modifier
                    .size((screenWidth.value * 0.35f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2B2B3D)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size((screenWidth.value * 0.2f).dp)) {
                    val strokeWidth = (screenWidth.value * 0.02f).dp.toPx()
                    // Three diagonal lines as in wireframe
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.7f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.7f, 0f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.3f, size.height),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.3f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.85f),
                        end = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.15f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height((screenHeight.value * 0.06f).dp))

            if (!isReadyToLoad) {
                // New User Onboarding
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = "Welcome to ${stringResource(id = R.string.app_name)}",
                        color = Color.White,
                        fontSize = (screenWidth.value * 0.05f).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height((screenHeight.value * 0.03f).dp))
                    
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Enter your name", color = Color.Gray) },
                        placeholder = { Text("e.g. Justin", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF4CAF50),
                            cursorColor = Color(0xFF4CAF50)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height((screenHeight.value * 0.03f).dp))
                    
                    Button(
                        onClick = {
                            if (usernameInput.isNotBlank()) {
                                UserPreferences.saveUsername(context, usernameInput.trim())
                                isReadyToLoad = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((screenHeight.value * 0.07f).dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Get Started", fontSize = (screenWidth.value * 0.04f).sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Loading Animation
                Text(
                    text = loadingText,
                    color = Color.LightGray,
                    fontSize = (screenWidth.value * 0.04f).sp,
                    modifier = Modifier.alpha(alpha),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
