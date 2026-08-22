package com.example.investmenttracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// A reusable container with a border outline and a title that overlaps the top edge.
@Composable
fun OutlinedSection(
    title: String,
    action: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val titleRowHeight = 32.dp // Height that accommodates both text and buttons comfortably

    Box(modifier = Modifier.fillMaxWidth()) {
        // 1. The Outlined Border Container
        // Top padding is half of the titleRowHeight to ensure the line is perfectly centered vertically
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = titleRowHeight / 2)
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 16.dp)
        ) {
            content()
        }

        // 2. The Overlapping Title and Optional Action Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(titleRowHeight)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Title with background to mask the border line
            Box(
                modifier = Modifier
                    .background(Color(0xFF1E1E2E))
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            // Optional Action (e.g. + New Stock button)
            if (action != null) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF1E1E2E))
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    action()
                }
            }
        }
    }
}
