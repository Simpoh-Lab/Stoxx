package com.example.investmenttracker.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.investmenttracker.R
import com.example.investmenttracker.data.LiveNewsItem
import kotlin.math.abs

// Displays a horizontal list of news cards related to the user's stock portfolio.
@Composable
fun NewsSection(
    newsList: List<LiveNewsItem>,
    isLoading: Boolean,
    hasStocks: Boolean = true,
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((screenHeight.value * 0.2f).dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else if (!hasStocks || newsList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (!hasStocks) "Add stocks to see news" else "No recent news found",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(newsList) { news ->
                    LiveNewsCard(news = news)
                }
            }
        }
    }
}

// A clickable card representing a single news article with an image banner and source details.
@Composable
fun LiveNewsCard(news: LiveNewsItem) {
    var showConfirmDialog by remember { mutableStateOf(value = false) }
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val newsDrawableList = remember {
        listOf(
            R.drawable.news_image_1,
            R.drawable.news_image_2,
            R.drawable.news_image_3,
            R.drawable.news_image_4,
            R.drawable.news_image_5,
            R.drawable.news_image_6,
            R.drawable.news_image_7
        )
    }

    val newsImageRes = remember(news.title, news.timestamp) {
        val index = abs((news.title + news.timestamp).hashCode()) % newsDrawableList.size
        val resId = newsDrawableList[index]
        try {
            val hasData = context.resources.openRawResource(resId).use { it.available() > 0 }
            if (hasData) resId else R.drawable.news_image_1
        } catch (e: Exception) {
            R.drawable.news_image_1 
        }
    }

    Card(
        modifier = Modifier
            .width((screenWidth.value * 0.45f).dp)
            .fillMaxHeight()
            .clickable { showConfirmDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Column(modifier = Modifier.padding((screenWidth.value * 0.02f).dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E1E2E))
            ) {
                Image(
                    painter = painterResource(id = newsImageRes),
                    contentDescription = "News Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height((screenHeight.value * 0.01f).dp))

            Text(
                text = news.title,
                color = Color.White,
                fontSize = (screenWidth.value * 0.03f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = (screenWidth.value * 0.04f).sp,
                modifier = Modifier.weight(0.3f)
            )

            Text(
                text = "${news.stockSymbol} | ${news.source} · ${getRelativeTimeString(news.timestamp)}",
                color = Color(0xFF4CAF50),
                fontSize = (screenWidth.value * 0.026f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = Color(0xFF2B2B3D),
            title = {
                Text(
                    text = "Read Article",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Open Browser to view article?",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        val intent = Intent(Intent.ACTION_VIEW, news.link.toUri())
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Open Browser")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

// Converts a millisecond timestamp into a human-readable relative time string (e.g., '2h ago').
fun getRelativeTimeString(timestamp: Long): String {
    if (timestamp == 0L) return "Recent"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 60 -> "${minutes.coerceAtLeast(1)}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
