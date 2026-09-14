package com.essentialremapper.ui.diagnostic

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.essentialremapper.accessibility.EssentialButtonAccessibilityService
import com.essentialremapper.domain.device.DeviceProfile
import com.essentialremapper.domain.gesture.RawButtonAction
import com.essentialremapper.domain.gesture.RawButtonEvent
import com.essentialremapper.ui.theme.NothingRed
import com.essentialremapper.ui.theme.StatusGreen
import com.essentialremapper.ui.theme.StatusGreenContainer
import com.essentialremapper.ui.theme.StatusGreenLight
import com.essentialremapper.util.AccessibilityHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticScreen(
    deviceProfile: DeviceProfile,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isConnected by EssentialButtonAccessibilityService.isServiceConnected.collectAsState()
    val isEnabledInSettings = AccessibilityHelper.isAccessibilityServiceEnabled(context)
    val isServiceActive = isConnected || isEnabledInSettings

    val diagnosticEvents by EssentialButtonAccessibilityService.buttonEventDetector.diagnosticHistory.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Essential Button Diagnostic",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Phase 3 Hardware Interception Console",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Service Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceActive) StatusGreenContainer else Color(0xFF2C1616)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isServiceActive) "●" else "○",
                                fontSize = 16.sp,
                                color = if (isServiceActive) StatusGreenLight else Color(0xFFE57373)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServiceActive) "Accessibility enabled" else "Accessibility disabled",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isServiceActive) StatusGreenLight else Color(0xFFE57373)
                            )
                        }

                        if (!isServiceActive) {
                            Button(
                                onClick = {
                                    context.startActivity(AccessibilityHelper.createAccessibilitySettingsIntent())
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NothingRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enable", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isServiceActive) {
                            "Service is active. Filtering hardware scanCode ${deviceProfile.essentialButtonScanCode}. Privacy enforced: canRetrieveWindowContent=false."
                        } else {
                            "Service is not yet enabled. Tap 'Enable' to open Android Accessibility settings and turn on 'Essential Button Diagnostic Service'."
                        },
                        fontSize = 11.sp,
                        color = Color(0xFFCCCCCC),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Hardware Target Info
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = deviceProfile.displayName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Filter: ScanCode ${deviceProfile.essentialButtonScanCode} • KeyCode ${deviceProfile.essentialButtonKeyCode}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            EssentialButtonAccessibilityService.buttonEventDetector.clearHistory()
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", fontSize = 11.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DETECTED EVENTS (${diagnosticEvents.size} / 50)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFF262626))
            Spacer(modifier = Modifier.height(6.dp))

            if (diagnosticEvents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Awaiting Button Press",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Press the physical Essential Button on your phone (Single / Double / Long press, unlocked or locked).",
                            fontSize = 12.sp,
                            color = Color(0xFF777777),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(diagnosticEvents) { event ->
                        DiagnosticEventCard(event)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticEventCard(event: RawButtonEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191919)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (event.action == RawButtonAction.DOWN) StatusGreen else Color(0xFF1565C0),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = event.action.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "ScanCode: ${event.scanCode} | KeyCode: ${event.keyCode}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = event.formattedTime,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Column(
                modifier = Modifier
                    .background(Color(0xFF101010), RoundedCornerShape(6.dp))
                    .padding(8.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "DeviceId: ${event.deviceId} | Source: 0x${Integer.toHexString(event.source)} | Flags: 0x${Integer.toHexString(event.flags)}",
                    fontSize = 11.sp,
                    color = Color(0xFFB0B0B0),
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Screen Locked: ${event.isScreenLocked}",
                    fontSize = 11.sp,
                    color = if (event.isScreenLocked) Color(0xFFFFB74D) else Color(0xFF81C784),
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
