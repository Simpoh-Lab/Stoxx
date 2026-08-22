package com.example.investmenttracker.ui.components

import androidx.compose.foundation.Image
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.investmenttracker.CurrentScreen
import com.example.investmenttracker.UserPreferences
import com.example.investmenttracker.loadOrInitializeUserAvatar
import java.util.Calendar

// Renders the dashboard header featuring a greeting, user name, and profile photo.
@Composable
fun HeaderSection(
    userName: String,
    isMenuOpen: Boolean,
    currentScreen: CurrentScreen,
    onMenuToggle: () -> Unit,
    onNavigate: (CurrentScreen) -> Unit,
    customTitle: String? = null,
    customSubtitle: String? = null,
    showProfile: Boolean = true
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

    // Animation for the menu icon rotation
    val rotationAngle by animateFloatAsState(
        targetValue = if (isMenuOpen) 90f else 0f,
        label = "menu_rotation"
    )

    val displayTitle = customTitle ?: greeting
    val displaySubtitle = customSubtitle ?: userName

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = (screenWidth.value * 0.04f).dp)
    ) {
        // 1. The Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height((screenHeight.value * 0.1f).dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B2B3D))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = (screenWidth.value * 0.06f).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showProfile) {
                    // Profile Photo
                    Box(
                        modifier = Modifier
                            .size((screenHeight.value * 0.065f).dp)
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
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User Profile Avatar",
                                tint = Color.White,
                                modifier = Modifier.size((screenHeight.value * 0.035f).dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width((screenWidth.value * 0.04f).dp))
                }

                // Greeting and Username
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = if (showProfile) Alignment.Start else Alignment.Start
                ) {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontSize = (screenWidth.value * 0.045f).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = displaySubtitle,
                        color = Color.Gray,
                        fontSize = (screenWidth.value * 0.04f).sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Menu Button (Hamburger or X) with Rotation
                IconButton(
                    onClick = onMenuToggle,
                    modifier = Modifier
                        .size((screenHeight.value * 0.07f).dp)
                        .background(Color(0xFF3B3B52), CircleShape)
                        .rotate(rotationAngle)
                ) {
                    Icon(
                        imageVector = if (isMenuOpen) Icons.Default.Close else Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size((screenHeight.value * 0.035f).dp)
                    )
                }
            }
        }

        // 2. The Navigation Button Row with Expand/Fade Animation
        AnimatedVisibility(
            visible = isMenuOpen,
            enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color(0xFF2B2B3D), shape = RoundedCornerShape(25.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NavButton(
                        text = "HOME", 
                        isActive = currentScreen == CurrentScreen.HOME, 
                        onClick = { onNavigate(CurrentScreen.HOME) },
                        modifier = Modifier.weight(1f)
                    )
                    NavButton(
                        text = "TRANSACTION", 
                        isActive = currentScreen == CurrentScreen.TRANSACTIONS, 
                        onClick = { onNavigate(CurrentScreen.TRANSACTIONS) },
                        modifier = Modifier.weight(1.4f)
                    )
                    NavButton(
                        text = "SETTING", 
                        isActive = currentScreen == CurrentScreen.SETTINGS, 
                        onClick = { onNavigate(CurrentScreen.SETTINGS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// A stylized navigation button for the header menu.
@Composable
fun NavButton(
    text: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) Color(0xFF4CAF50) else Color(0xFF3B3B52))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isActive) Color.White else Color.LightGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// A standardized, bold header specifically for the Settings screen.
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
