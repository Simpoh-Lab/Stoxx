package com.example.investmenttracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.window.Dialog
import com.example.investmenttracker.data.StockItem

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
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding((screenWidth.value * 0.03f).dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = if (stock.symbol.startsWith("SGX:")) stock.symbol else "SGX: ${stock.symbol}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (screenWidth.value * 0.04f).sp
                )
                Text(
                    text = stock.name,
                    color = Color.LightGray,
                    fontSize = (screenWidth.value * 0.032f).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(
                modifier = Modifier.weight(1.2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${stock.currentPrice} SGD",
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = (screenWidth.value * 0.035f).sp
                )
                Text(
                    text = stock.priceChangePercentage,
                    color = statusColor,
                    fontSize = (screenWidth.value * 0.028f).sp,
                    fontWeight = FontWeight.Medium
                )
            }

            SparklineChart(
                data = stock.chartData,
                lineColor = statusColor,
                modifier = Modifier
                    .width((screenWidth.value * 0.15f).dp)
                    .height((screenWidth.value * 0.09f).dp)
            )

            IconButton(onClick = { onDeleteClick(stock) }, modifier = Modifier.size((screenWidth.value * 0.1f).dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Stock",
                    tint = Color.Gray,
                    modifier = Modifier.size((screenWidth.value * 0.05f).dp)
                )
            }
        }
    }
}

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
        
        // Add a small buffer to the range to prevent flatlines if all points are very close
        val baseRange = max - min
        val range = if (baseRange < 1f) 10f else baseRange * 1.2f
        val adjustedMin = if (baseRange < 1f) min - 5f else min - (baseRange * 0.1f)

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
            style = Stroke(width = 2.dp.toPx())
        )
        
        if (showLatestPriceLine && latestPrice != null) {
            val lastValue = data.last()
            val y = size.height - ((lastValue - adjustedMin) / range * size.height)
            
            // Draw dotted line
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            
            // Draw price label background
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
            
            drawRoundRect(
                color = Color.Gray.copy(alpha = 0.5f),
                topLeft = Offset(boxX, boxY),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
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