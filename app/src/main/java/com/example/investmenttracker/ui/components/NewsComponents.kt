package com.example.investmenttracker.ui.components

import android.content.Intent
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@Composable
fun NewsSection(
    newsList: List<LiveNewsItem>,
    isLoading: Boolean,
    hasStocks: Boolean = true,
    onRefresh: (() -> Unit)? = null,
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "News on the stock",
                color = Color.White,
                fontSize = (screenWidth.value * 0.05f).sp,
                fontWeight = FontWeight.Bold
            )

            if (onRefresh != null) {
                Spacer(modifier = Modifier.width((screenWidth.value * 0.02f).dp))

                Box(
                    modifier = Modifier
                        .size((screenWidth.value * 0.07f).dp)
                        .background(Color(0xFF2B2B3D), shape = RoundedCornerShape(6.dp))
                        .clickable(enabled = !isLoading) { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh News",
                        tint = if (isLoading) Color.Gray else Color.White,
                        modifier = Modifier.size((screenWidth.value * 0.04f).dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height((screenHeight.value * 0.015f).dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenHeight.value * 0.15f).dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50), modifier = Modifier.size((screenWidth.value * 0.08f).dp))
            }
        } else if (!hasStocks) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenHeight.value * 0.12f).dp)
                    .background(Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add new stocks to get latest news",
                    color = Color.Gray,
                    fontSize = (screenWidth.value * 0.035f).sp
                )
            }
        } else if (newsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenHeight.value * 0.12f).dp)
                    .background(Color.White.copy(alpha = 0.02f), shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No recent news (last 5 days) for your stocks.",
                    color = Color.Gray,
                    fontSize = (screenWidth.value * 0.035f).sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy((screenWidth.value * 0.03f).dp)
            ) {
                items(newsList) { news ->
                    LiveNewsCard(news = news)
                }
            }
        }
    }
}

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
            R.drawable.news_image_7,
            R.drawable.news_image_8,
            R.drawable.news_image_9,
            R.drawable.news_image_10
        )
    }

    val newsImageRes = remember(news.title, news.timestamp) {
        // Use title and timestamp for much better randomization across different articles
        val index = abs((news.title + news.timestamp).hashCode()) % newsDrawableList.size
        newsDrawableList[index]
    }

    Card(
        modifier = Modifier
            .width((screenWidth.value * 0.38f).dp)
            .height((screenHeight.value * 0.16f).dp)
            .clickable { showConfirmDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Column(modifier = Modifier.padding((screenWidth.value * 0.02f).dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((screenHeight.value * 0.075f).dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(id = newsImageRes),
                    contentDescription = "News Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height((screenHeight.value * 0.008f).dp))

            Text(
                text = news.title,
                color = Color.White,
                fontSize = (screenWidth.value * 0.032f).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                lineHeight = (screenWidth.value * 0.04f).sp,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height((screenHeight.value * 0.005f).dp))

            Text(
                text = "· ${news.stockSymbol} | ${news.source} · ${getRelativeTimeString(news.timestamp)}",
                color = Color(0xFF4CAF50),
                fontSize = (screenWidth.value * 0.024f).sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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