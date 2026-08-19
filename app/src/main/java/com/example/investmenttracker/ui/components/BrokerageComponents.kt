package com.example.investmenttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.investmenttracker.data.BrokerCardItem
import kotlin.random.Random

@Composable
fun BrokerageHomeSection(linkedBrokers: List<BrokerCardItem>) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Column {
        Text(
            text = "🤵 Brokerage",
            color = Color.White,
            fontSize = (screenWidth.value * 0.05f).sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height((screenHeight.value * 0.015f).dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((screenHeight.value * 0.12f).dp)
                .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(24.dp))
                .padding(horizontal = (screenWidth.value * 0.04f).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((screenWidth.value * 0.06f).dp)
            ) {
                itemsIndexed(linkedBrokers) { index, broker ->
                    BrokerHomeItem(broker = broker, screenWidth = screenWidth)
                    
                    if (index < linkedBrokers.size - 1) {
                        Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height((screenHeight.value * 0.05f).dp)
                                .background(Color.Gray.copy(alpha = 0.3f))
                        )
                        Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))
                    }
                }
            }
        }
    }
}

@Composable
fun BrokerHomeItem(broker: BrokerCardItem, screenWidth: androidx.compose.ui.unit.Dp) {
    // Mocking P/L data for the home screen as per wireframe
    val mockPL = remember { (100..2000).random() * (if (Random.nextBoolean()) 1 else -1) }
    val mockPercent = remember { (1..15).random() }
    val isGain = mockPL >= 0
    val color = if (isGain) Color(0xFF4CAF50) else Color(0xFFE57373)
    val sign = if (isGain) "+" else "-"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = broker.name,
            color = Color.White,
            fontSize = (screenWidth.value * 0.038f).sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${sign}\$${Math.abs(mockPL)}",
            color = color,
            fontSize = (screenWidth.value * 0.038f).sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "(${mockPercent}%)",
            color = color,
            fontSize = (screenWidth.value * 0.032f).sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// Dummy Box component for spacers if needed (imported from foundation)
@Composable
private fun Box(modifier: Modifier) {
    androidx.compose.foundation.layout.Box(modifier = modifier)
}
