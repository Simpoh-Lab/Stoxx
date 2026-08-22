package com.example.investmenttracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.investmenttracker.ui.components.HeaderSection
import com.example.investmenttracker.ui.components.OutlinedSection
import com.example.investmenttracker.*

// Allows users to customize their profile, update their username, and toggle notification settings.
@Composable
fun SettingsScreen(
    onBackToHome: () -> Unit,
    onNavigateToTransactions: () -> Unit
) {
    val context = LocalContext.current

    var username by remember { mutableStateOf(UserPreferences.getUsername(context)) }
    var selectedImageUri by remember { mutableStateOf(UserPreferences.getCustomAvatarUri(context)) }
    val defaultAvatarRes = remember { loadOrInitializeUserAvatar(context) }
    var isMenuOpen by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            selectedImageUri = it.toString()
            UserPreferences.saveCustomAvatarUri(context, it)
        }
    }

    Scaffold(
        containerColor = Color(0xFF1E1E2E)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Header (10%)
            HeaderSection(
                userName = username,
                isMenuOpen = isMenuOpen,
                currentScreen = CurrentScreen.SETTINGS,
                onMenuToggle = { isMenuOpen = !isMenuOpen },
                onNavigate = { screen ->
                    isMenuOpen = false
                    when (screen) {
                        CurrentScreen.HOME -> onBackToHome()
                        CurrentScreen.TRANSACTIONS -> onNavigateToTransactions()
                        else -> {}
                    }
                },
                customTitle = "Setting",
                customSubtitle = "App customization here!",
                showProfile = false
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. User Settings Section (15% height card)
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                OutlinedSection(title = "User") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Name Input Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Name",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(100.dp)
                            )
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .background(Color(0xFF1E1E2E), RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicTextField(
                                        value = username,
                                        onValueChange = { if (it.length <= 20) username = it },
                                        textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                        cursorBrush = SolidColor(Color.White),
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Profile Photo Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Profile Photo",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(100.dp)
                            )
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .background(Color(0xFF3B3B52), shape = CircleShape)
                                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedImageUri != null) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(0.5f).clip(CircleShape)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = defaultAvatarRes),
                                            contentDescription = "Default Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(0.5f).clip(CircleShape)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Text(
                                    text = "(Click to change)",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Save Button
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    UserPreferences.saveUsername(context, username)
                                    // Could add a toast or feedback here
                                },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .height(36.dp)
                                    .width(80.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Release Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(0.8f),
                    thickness = 0.5.dp,
                    color = Color.Gray.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "release 0.2.1-alpha (Developed by Simpoh Lab)",
                    color = Color.Gray.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // NO UI ZONE (10%)
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
