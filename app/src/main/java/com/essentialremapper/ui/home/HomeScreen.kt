package com.essentialremapper.ui.home

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropLandscape
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.essentialremapper.accessibility.EssentialButtonAccessibilityService
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.domain.action.handlers.DeepLinkHandler
import com.essentialremapper.domain.device.DeviceProfile
import com.essentialremapper.domain.gesture.GestureType
import com.essentialremapper.ui.theme.NothingRed
import com.essentialremapper.ui.theme.StatusGreenLight
import com.essentialremapper.util.AccessibilityHelper

data class LaunchableAppInfo(
    val appName: String,
    val packageName: String,
    val iconDrawable: Drawable?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: RemapperSettings,
    deviceProfile: DeviceProfile,
    onActionSelected: (GestureType, RemapAction) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostic: () -> Unit
) {
    val context = LocalContext.current
    var activeGestureForSheet by remember { mutableStateOf<GestureType?>(null) }
    var isAppPickerOpen by remember { mutableStateOf(false) }
    var isDeepLinkDialogOpen by remember { mutableStateOf(false) }
    var deepLinkInput by remember { mutableStateOf("https://") }
    var deepLinkError by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Essential Remapper",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DeviceStatusCard(
                    deviceProfile = deviceProfile,
                    onOpenDiagnostic = onNavigateToDiagnostic
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "BUTTON MAPPINGS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            item {
                MappingCard(
                    gestureTitle = "Single Press",
                    gestureSubtitle = "Short tap on Essential Button",
                    action = settings.singlePressAction,
                    icon = getActionIcon(settings.singlePressAction),
                    onClick = { activeGestureForSheet = GestureType.SinglePress }
                )
            }

            item {
                MappingCard(
                    gestureTitle = "Double Press",
                    gestureSubtitle = "Two rapid clicks (${settings.doublePressTimeoutMs}ms window)",
                    action = settings.doublePressAction,
                    icon = getActionIcon(settings.doublePressAction),
                    onClick = { activeGestureForSheet = GestureType.DoublePress }
                )
            }

            item {
                MappingCard(
                    gestureTitle = "Long Press",
                    gestureSubtitle = "Hold for ${settings.longPressDurationMs}ms",
                    action = settings.longPressAction,
                    icon = getActionIcon(settings.longPressAction),
                    onClick = { activeGestureForSheet = GestureType.LongPress }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(NothingRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Actions execute natively through AccessibilityService with zero polling and zero telemetry.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Categorized Action Selection Bottom Sheet
    if (activeGestureForSheet != null) {
        val gesture = activeGestureForSheet!!
        ModalBottomSheet(
            onDismissRequest = { activeGestureForSheet = null },
            sheetState = sheetState,
            containerColor = Color(0xFF161616)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Configure Action",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Assign action for ${gesture.displayName}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(onClick = { activeGestureForSheet = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(480.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Category 1: Quick Actions
                    item {
                        ActionCategoryHeader("QUICK ACTIONS")
                    }
                    item {
                        ActionOptionRow(
                            title = "No Action",
                            subtitle = "Do nothing on this gesture",
                            icon = Icons.Default.TouchApp,
                            onClick = {
                                onActionSelected(gesture, RemapAction.None)
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Toggle Flashlight",
                            subtitle = "Turn rear camera flash on / off",
                            icon = Icons.Default.FlashlightOn,
                            onClick = {
                                onActionSelected(gesture, RemapAction.Flashlight)
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Launch Camera",
                            subtitle = "Open default camera (secure intent on lockscreen)",
                            icon = Icons.Default.CameraAlt,
                            onClick = {
                                onActionSelected(gesture, RemapAction.Camera)
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Toggle Rotation Lock",
                            subtitle = "Lock or unlock auto screen rotation",
                            icon = Icons.Default.ScreenRotation,
                            onClick = {
                                onActionSelected(gesture, RemapAction.RotationLock)
                                activeGestureForSheet = null
                            }
                        )
                    }

                    // Category 2: Media Controls
                    item {
                        ActionCategoryHeader("MEDIA CONTROLS")
                    }
                    item {
                        ActionOptionRow(
                            title = "Play / Pause",
                            subtitle = "Toggle playback on active media session",
                            icon = Icons.Default.PlayArrow,
                            onClick = {
                                onActionSelected(gesture, RemapAction.MediaPlayPause)
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Next Track",
                            subtitle = "Skip to next media track",
                            icon = Icons.Default.SkipNext,
                            onClick = {
                                onActionSelected(gesture, RemapAction.MediaNextTrack)
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Previous Track",
                            subtitle = "Skip to previous media track",
                            icon = Icons.Default.SkipPrevious,
                            onClick = {
                                onActionSelected(gesture, RemapAction.MediaPreviousTrack)
                                activeGestureForSheet = null
                            }
                        )
                    }

                    // Category 3: Applications & Deep Links
                    item {
                        ActionCategoryHeader("APPLICATIONS & SHORTCUTS")
                    }
                    item {
                        ActionOptionRow(
                            title = "Launch Application...",
                            subtitle = "Choose an installed app to launch",
                            icon = Icons.Default.Apps,
                            onClick = {
                                isAppPickerOpen = true
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Open Custom URL / Deep Link...",
                            subtitle = "Enter a safe web URL, tel, or custom deep link",
                            icon = Icons.Default.Link,
                            onClick = {
                                deepLinkInput = "https://"
                                deepLinkError = null
                                isDeepLinkDialogOpen = true
                            }
                        )
                    }

                    // Category 4: System Actions
                    item {
                        ActionCategoryHeader("SYSTEM ACCESSIBILITY ACTIONS")
                    }
                    item {
                        ActionOptionRow(
                            title = "Open Notifications",
                            subtitle = "Pull down system notification shade",
                            icon = Icons.Default.Notifications,
                            onClick = {
                                onActionSelected(gesture, RemapAction.SystemAction(RemapAction.SystemActionType.NOTIFICATIONS))
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Open Quick Settings",
                            subtitle = "Expand quick settings control tiles",
                            icon = Icons.Default.Tune,
                            onClick = {
                                onActionSelected(gesture, RemapAction.SystemAction(RemapAction.SystemActionType.QUICK_SETTINGS))
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Lock Screen",
                            subtitle = "Lock device immediately (requires Android 9+)",
                            icon = Icons.Default.Lock,
                            onClick = {
                                onActionSelected(gesture, RemapAction.SystemAction(RemapAction.SystemActionType.LOCK_SCREEN))
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Back",
                            subtitle = "Simulate system back navigation",
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            onClick = {
                                onActionSelected(gesture, RemapAction.SystemAction(RemapAction.SystemActionType.BACK))
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Home",
                            subtitle = "Navigate to system home launcher",
                            icon = Icons.Default.Home,
                            onClick = {
                                onActionSelected(gesture, RemapAction.SystemAction(RemapAction.SystemActionType.HOME))
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Recents",
                            subtitle = "Show recent application overview",
                            icon = Icons.Default.CropLandscape,
                            onClick = {
                                onActionSelected(gesture, RemapAction.SystemAction(RemapAction.SystemActionType.RECENTS))
                                activeGestureForSheet = null
                            }
                        )
                    }
                    item {
                        ActionOptionRow(
                            title = "Take Screenshot",
                            subtitle = "Capture full device screen",
                            icon = Icons.Default.PhotoCamera,
                            onClick = {
                                onActionSelected(gesture, RemapAction.SystemAction(RemapAction.SystemActionType.TAKE_SCREENSHOT))
                                activeGestureForSheet = null
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Installed Applications Picker Dialog
    if (isAppPickerOpen && activeGestureForSheet != null) {
        val targetGesture = activeGestureForSheet!!
        InstalledAppPickerDialog(
            context = context,
            onDismiss = { isAppPickerOpen = false },
            onAppSelected = { app ->
                onActionSelected(targetGesture, RemapAction.LaunchApp(packageName = app.packageName, appName = app.appName))
                isAppPickerOpen = false
                activeGestureForSheet = null
            }
        )
    }

    // Custom Deep Link Input Dialog
    if (isDeepLinkDialogOpen && activeGestureForSheet != null) {
        val targetGesture = activeGestureForSheet!!
        AlertDialog(
            onDismissRequest = { isDeepLinkDialogOpen = false },
            title = { Text("Enter URL / Deep Link", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Allowed schemes: https, http, content, tel, mailto",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deepLinkInput,
                        onValueChange = {
                            deepLinkInput = it
                            deepLinkError = null
                        },
                        isError = deepLinkError != null,
                        supportingText = {
                            if (deepLinkError != null) {
                                Text(deepLinkError!!, color = NothingRed)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val uriString = deepLinkInput.trim()
                        val parsed = try { Uri.parse(uriString) } catch (_: Exception) { null }
                        val scheme = parsed?.scheme?.lowercase()
                        if (scheme == null || scheme !in DeepLinkHandler.ALLOWED_SCHEMES) {
                            deepLinkError = "Scheme '${scheme ?: "none"}' is not allowed"
                        } else {
                            onActionSelected(targetGesture, RemapAction.DeepLink(uri = uriString))
                            isDeepLinkDialogOpen = false
                            activeGestureForSheet = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { isDeepLinkDialogOpen = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ActionCategoryHeader(title: String) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    )
}

@Composable
fun ActionOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF2B2B2B), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NothingRed,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
fun InstalledAppPickerDialog(
    context: Context,
    onDismiss: () -> Unit,
    onAppSelected: (LaunchableAppInfo) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var appsList by remember { mutableStateOf<List<LaunchableAppInfo>>(emptyList()) }

    LaunchedEffect(Unit) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        appsList = resolveInfos.map {
            LaunchableAppInfo(
                appName = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                iconDrawable = try { it.loadIcon(pm) } catch (_: Exception) { null }
            )
        }.sortedBy { it.appName.lowercase() }
    }

    val filteredApps = if (searchQuery.isBlank()) {
        appsList
    } else {
        appsList.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Select Application", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search installed apps...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            if (filteredApps.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No applications found", color = Color.Gray, fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAppSelected(app) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val bitmap = remember(app.iconDrawable) {
                                try {
                                    app.iconDrawable?.toBitmap()
                                } catch (_: Exception) { null }
                            }

                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF333333), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Apps, contentDescription = null, tint = Color.LightGray)
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = app.appName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = app.packageName,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun DeviceStatusCard(
    deviceProfile: DeviceProfile,
    onOpenDiagnostic: () -> Unit
) {
    val context = LocalContext.current
    val isConnected by EssentialButtonAccessibilityService.isServiceConnected.collectAsState()
    val isEnabledInSettings = AccessibilityHelper.isAccessibilityServiceEnabled(context)
    val isAccessibilityActive = isConnected || isEnabledInSettings

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0xFF2E7D32), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "CONNECTED",
                        style = MaterialTheme.typography.labelSmall,
                        color = StatusGreenLight
                    )
                }

                Text(
                    text = "ScanCode ${deviceProfile.essentialButtonScanCode}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = deviceProfile.displayName,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Model: ${deviceProfile.modelNames.firstOrNull() ?: "A059P"} • Codename: ${deviceProfile.deviceCodename}",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF262626))
            Spacer(modifier = Modifier.height(12.dp))

            // Accessibility Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAccessibilityActive) "●" else "○",
                        fontSize = 14.sp,
                        color = if (isAccessibilityActive) StatusGreenLight else Color(0xFFE57373)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAccessibilityActive) "Accessibility enabled" else "Accessibility disabled",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isAccessibilityActive) StatusGreenLight else Color(0xFFE57373)
                    )
                }

                OutlinedButton(
                    onClick = onOpenDiagnostic,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Diagnostic Console",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun MappingCard(
    gestureTitle: String,
    gestureSubtitle: String,
    action: RemapAction,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFF212121), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NothingRed,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = gestureTitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = action.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = gestureSubtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getActionIcon(action: RemapAction): ImageVector {
    return when (action) {
        is RemapAction.None -> Icons.Default.TouchApp
        is RemapAction.Flashlight -> Icons.Default.FlashlightOn
        is RemapAction.Camera -> Icons.Default.CameraAlt
        is RemapAction.RotationLock -> Icons.Default.ScreenRotation
        is RemapAction.MediaPlayPause -> Icons.Default.PlayArrow
        is RemapAction.MediaNextTrack -> Icons.Default.SkipNext
        is RemapAction.MediaPreviousTrack -> Icons.Default.SkipPrevious
        is RemapAction.LaunchApp -> Icons.Default.Apps
        is RemapAction.AppShortcut -> Icons.Default.Apps
        is RemapAction.DeepLink -> Icons.Default.Link
        is RemapAction.SystemAction -> when (action.systemType) {
            RemapAction.SystemActionType.NOTIFICATIONS -> Icons.Default.Notifications
            RemapAction.SystemActionType.QUICK_SETTINGS -> Icons.Default.Tune
            RemapAction.SystemActionType.LOCK_SCREEN -> Icons.Default.Lock
            RemapAction.SystemActionType.BACK -> Icons.AutoMirrored.Filled.ArrowBack
            RemapAction.SystemActionType.HOME -> Icons.Default.Home
            RemapAction.SystemActionType.RECENTS -> Icons.Default.CropLandscape
            RemapAction.SystemActionType.TAKE_SCREENSHOT -> Icons.Default.PhotoCamera
        }
    }
}
