package com.example.investmenttracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.investmenttracker.data.BrokerCardItem
import kotlin.random.Random

// Displays a horizontal scrollable list of linked brokerages and their performance summary.
@Composable
fun BrokerageHomeSection(
    linkedBrokers: List<BrokerCardItem>,
    onBrokerClick: (BrokerCardItem) -> Unit
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((screenHeight.value * 0.15f).dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(linkedBrokers) { broker ->
                BrokerHomeItem(
                    broker = broker, 
                    screenWidth = configuration.screenWidthDp.dp,
                    onClick = { onBrokerClick(broker) }
                )
            }
        }
    }
}

// A summary view of a single linked brokerage for the home dashboard.
@Composable
fun BrokerHomeItem(
    broker: BrokerCardItem, 
    screenWidth: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val iconResId = remember(broker.id) {
        val id = context.resources.getIdentifier("ic_broker_${broker.id}", "drawable", context.packageName)
        if (id != 0) id else 0
    }

    // Mocking P/L data for the home screen as per wireframe
    val mockPL = remember { (100..2000).random() * (if (Random.nextBoolean()) 1 else -1) }
    val mockPercent = remember { (1..15).random() }
    val isGain = mockPL >= 0
    val color = if (isGain) Color(0xFF4CAF50) else Color(0xFFE57373)
    val sign = if (isGain) "+" else "-"

    Card(
        modifier = Modifier
            .width((screenWidth.value * 0.35f).dp)
            .fillMaxHeight()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Broker Logo (Circular)
            Box(
                modifier = Modifier
                    .size((screenWidth.value * 0.1f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1E2E)),
                contentAlignment = Alignment.Center
            ) {
                if (iconResId != 0) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = broker.name,
                        modifier = Modifier.size((screenWidth.value * 0.07f).dp).clip(CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(
                    text = broker.name,
                    color = Color.White,
                    fontSize = (screenWidth.value * 0.035f).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${sign}\$${Math.abs(mockPL)}",
                    color = color,
                    fontSize = (screenWidth.value * 0.032f).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "(${mockPercent}%)",
                    color = color,
                    fontSize = (screenWidth.value * 0.028f).sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
