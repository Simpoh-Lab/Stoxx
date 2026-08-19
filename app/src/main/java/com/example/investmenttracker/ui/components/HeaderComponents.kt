package com.example.investmenttracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.investmenttracker.UserPreferences
import com.example.investmenttracker.loadOrInitializeUserAvatar
import java.util.Calendar

@Composable
fun HeaderSection(
    userName: String
) {
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var profileUriString by remember { mutableStateOf<String?>(null) }
    var defaultAvatarRes by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        profileUriString = UserPreferences.getCustomAvatarUri(context)
        defaultAvatarRes = loadOrInitializeUserAvatar(context)
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "Good Morning,"
        in 12..17 -> "Good Afternoon,"
        else -> "Good Evening,"
    }

    // Single large rounded card for the header as per sketch
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height((screenHeight.value * 0.12f).dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (screenWidth.value * 0.06f).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size((screenHeight.value * 0.07f).dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3B3B52)),
                contentAlignment = Alignment.Center
            ) {
                if (!profileUriString.isNullOrBlank()) {
                    AsyncImage(
                        model = profileUriString,
                        contentDescription = "User Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else if (defaultAvatarRes != 0) {
                    Image(
                        painter = painterResource(id = defaultAvatarRes),
                        contentDescription = "Default Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Profile Avatar",
                        tint = Color.White,
                        modifier = Modifier.size((screenHeight.value * 0.04f).dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    color = Color.White,
                    fontSize = (screenWidth.value * 0.055f).sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userName,
                    color = Color.White,
                    fontSize = (screenWidth.value * 0.055f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SettingsHeaderSection(onHomeClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "App Setting",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}