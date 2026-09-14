package com.essentialremapper.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.essentialremapper.accessibility.EssentialButtonAccessibilityService
import com.essentialremapper.util.AccessibilityHelper
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.domain.action.RemapAction
import com.essentialremapper.domain.device.DeviceProfile
import com.essentialremapper.domain.gesture.GestureType
import com.essentialremapper.ui.theme.NothingRed
import com.essentialremapper.ui.theme.StatusGreenLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    settings: RemapperSettings,
    deviceProfile: DeviceProfile,
    onActionSelected: (GestureType, RemapAction) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostic: () -> Unit
) {
    var activeGestureForSheet by remember { mutableStateOf<GestureType?>(null) }
    val sheetState = rememberModalBottomSheetState()

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
                            text = "Phase 2 active. Tap any card to customize action persistence.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (activeGestureForSheet != null) {
        val gesture = activeGestureForSheet!!
        ModalBottomSheet(
            onDismissRequest = { activeGestureForSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Select Action for ${gesture.displayName}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                val availableActions = listOf(
                    RemapAction.None,
                    RemapAction.Flashlight,
                    RemapAction.RotationLock,
                    RemapAction.MediaPlayPause,
                    RemapAction.MediaNextTrack,
                    RemapAction.MediaPreviousTrack,
                    RemapAction.LaunchApp("com.nothing.camera", "Camera")
                )

                availableActions.forEach { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onActionSelected(gesture, action)
                                activeGestureForSheet = null
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getActionIcon(action),
                                contentDescription = null,
                                tint = NothingRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = action.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
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

            // Accessibility Status Indicator (Phase 3)
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

                Row {
                    if (!isAccessibilityActive) {
                        OutlinedButton(
                            onClick = {
                                context.startActivity(AccessibilityHelper.createAccessibilitySettingsIntent())
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Enable", fontSize = 11.sp, color = NothingRed)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Button(
                        onClick = onOpenDiagnostic,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626)),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Diagnostics", fontSize = 11.sp, color = Color.White)
                    }
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
        shape = RoundedCornerShape(14.dp),
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
        is RemapAction.RotationLock -> Icons.Default.ScreenRotation
        is RemapAction.MediaPlayPause -> Icons.Default.PlayArrow
        is RemapAction.MediaNextTrack -> Icons.Default.SkipNext
        is RemapAction.MediaPreviousTrack -> Icons.Default.SkipPrevious
        is RemapAction.LaunchApp -> Icons.Default.CameraAlt
        else -> Icons.Default.TouchApp
    }
}
