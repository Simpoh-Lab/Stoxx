package com.example.investmenttracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.investmenttracker.data.StockItem

// A reusable list item component displaying summary information and a sparkline for a specific stock.
@Composable
fun StockCard(
    stock: StockItem,
    onCardClick: () -> Unit,
    onDeleteClick: (StockItem) -> Unit,
) {
    val statusColor = if (stock.isGain) Color(0xFF4CAF50) else Color(0xFFE57373)
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (screenWidth.value * 0.03f).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Symbol Box (e.g. D05)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFF1E1E2E), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stock.symbol.take(3),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 2. Symbol and Long Name
            Column(modifier = Modifier.weight(1.2f)) {
                Text(
                    text = if (stock.symbol.endsWith(".SI")) stock.symbol else "${stock.symbol}.SI",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = stock.name,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. P/L and Sparkline
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = stock.currentPrice,
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = stock.priceChangePercentage,
                    color = statusColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            SparklineChart(
                data = stock.chartData,
                lineColor = statusColor,
                modifier = Modifier
                    .width(40.dp)
                    .height(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // 4. Delete Button
            IconButton(
                onClick = { onDeleteClick(stock) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// Draws a mini line chart with an optional dotted latest-price line and price label.
@Composable
fun SparklineChart(
    data: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier,
    showLatestPriceLine: Boolean = false,
    latestPrice: String? = null
) {
    val textMeasurer = rememberTextMeasurer()
    
    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas
        val min = data.minOrNull() ?: 0f
        val max = data.maxOrNull() ?: 1f
        
        val baseRange = max - min
        val range = if (baseRange < 0.01f) 1f else baseRange * 1.2f
        val adjustedMin = if (baseRange < 0.01f) min - 0.5f else min - (baseRange * 0.1f)

        val path = Path()
        val widthStep = size.width / (data.size - 1)

        data.forEachIndexed { index, value ->
            val x = index * widthStep
            val y = size.height - ((value - adjustedMin) / range * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 1.5.dp.toPx())
        )
        
        if (showLatestPriceLine && latestPrice != null) {
            val lastValue = data.last()
            val y = size.height - ((lastValue - adjustedMin) / range * size.height)
            
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            val textLayoutResult = textMeasurer.measure(
                text = latestPrice,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            val boxWidth = textLayoutResult.size.width + 8.dp.toPx()
            val boxHeight = textLayoutResult.size.height + 4.dp.toPx()
            val boxX = size.width - boxWidth
            val boxY = y - boxHeight / 2
            
            drawRoundRect(
                color = Color(0xFF2B2B3D),
                topLeft = Offset(boxX, boxY),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            
            drawText(
                textMeasurer = textMeasurer,
                text = latestPrice,
                topLeft = Offset(boxX + 4.dp.toPx(), boxY + 2.dp.toPx()),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// Displays a popup dialog with a text field for entering and adding new stock ticker symbols.
@Composable
fun AddStockDialog(onDismiss: () -> Unit, onAddStock: (String) -> Unit) {
    var symbol by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2B2B3D),
        title = { Text("Add SGX Stock", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text("SGX Code (e.g. S63, D05, Z74)", color = Color.Gray) },
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
                    if (symbol.isNotBlank()) {
                        onAddStock(symbol)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}