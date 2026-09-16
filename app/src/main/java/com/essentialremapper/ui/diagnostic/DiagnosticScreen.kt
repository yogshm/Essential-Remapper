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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.essentialremapper.accessibility.EssentialButtonAccessibilityService
import com.essentialremapper.domain.action.ActionExecutionEvent
import com.essentialremapper.domain.action.ActionResult
import com.essentialremapper.domain.device.DeviceProfile
import com.essentialremapper.domain.gesture.GestureEvent
import com.essentialremapper.domain.gesture.GestureType
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

    val diagnosticRawEvents by EssentialButtonAccessibilityService.buttonEventDetector.diagnosticHistory.collectAsState()
    val latestGesture by EssentialButtonAccessibilityService.gestureRecognizer.latestGesture.collectAsState()
    val diagnosticGestures by EssentialButtonAccessibilityService.gestureRecognizer.diagnosticGestureHistory.collectAsState()

    val latestActionExecution by EssentialButtonAccessibilityService.actionDispatcher.latestExecution.collectAsState()
    val diagnosticActions by EssentialButtonAccessibilityService.actionDispatcher.actionHistory.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
                            text = "Phase 5 Action Dispatch Console",
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
                actions = {
                    IconButton(
                        onClick = {
                            EssentialButtonAccessibilityService.buttonEventDetector.clearHistory()
                            EssentialButtonAccessibilityService.gestureRecognizer.clearDiagnosticHistory()
                            EssentialButtonAccessibilityService.actionDispatcher.clearHistory()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear All History",
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
            Spacer(modifier = Modifier.height(4.dp))

            // 1. Accessibility Service Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isServiceActive) StatusGreenContainer else Color(0xFF2C1616)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isServiceActive) "●" else "○",
                                fontSize = 14.sp,
                                color = if (isServiceActive) StatusGreenLight else Color(0xFFE57373)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isServiceActive) "Service Active (scanCode ${deviceProfile.essentialButtonScanCode})" else "Service Disabled",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
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
                                Text("Enable", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. ACTION EXECUTION CARD (Phase 5)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTION EXECUTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = if (latestActionExecution != null) "Last Dispatched" else "Idle",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (latestActionExecution != null) StatusGreenLight else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (latestActionExecution != null) {
                        val exec = latestActionExecution!!
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = exec.gesture.displayName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "→",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = exec.action.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = exec.result.message,
                                    fontSize = 11.sp,
                                    color = Color(0xFFBBBBBB)
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when (exec.result) {
                                                is ActionResult.Success -> StatusGreen
                                                is ActionResult.Failure -> NothingRed
                                                is ActionResult.Unavailable -> Color(0xFFE65100)
                                            },
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = when (exec.result) {
                                            is ActionResult.Success -> "SUCCESS"
                                            is ActionResult.Failure -> "FAILURE"
                                            is ActionResult.Unavailable -> "BLOCKED"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = exec.formattedTime,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color.Gray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Awaiting physical button press to execute configured action...",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Tab Selector: Actions (N) | Gestures (M) | Raw Events (K)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = NothingRed
                    )
                },
                divider = { HorizontalDivider(color = Color(0xFF262626)) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Actions (${diagnosticActions.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Gestures (${diagnosticGestures.size})",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Text(
                            text = "Raw (${diagnosticRawEvents.size})",
                            fontWeight = if (selectedTabIndex == 2) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Tab Content
            when (selectedTabIndex) {
                0 -> {
                    // Actions History
                    if (diagnosticActions.isEmpty()) {
                        EmptyConsolePlaceholder(
                            title = "No Actions Executed Yet",
                            description = "Configured actions (Flashlight, Camera, Media, Apps) will log their execution results and timestamps here."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(diagnosticActions) { exec ->
                                ActionHistoryCard(exec)
                            }
                        }
                    }
                }
                1 -> {
                    // Recognized Gestures History
                    if (diagnosticGestures.isEmpty()) {
                        EmptyConsolePlaceholder(
                            title = "No Gestures Recognized Yet",
                            description = "Press the physical Essential Button:\n• Single Press: Click & release (<350ms)\n• Double Press: Click twice quickly\n• Long Press: Hold down for >700ms"
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(diagnosticGestures) { gesture ->
                                GestureHistoryCard(gesture)
                            }
                        }
                    }
                }
                2 -> {
                    // Raw Key Events History
                    if (diagnosticRawEvents.isEmpty()) {
                        EmptyConsolePlaceholder(
                            title = "Awaiting Button Press",
                            description = "Raw physical key events (scanCode 250) will be listed here with hardware timestamps."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(diagnosticRawEvents) { event ->
                                DiagnosticEventCard(event)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionHistoryCard(exec: ActionExecutionEvent) {
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
                                color = when (exec.gesture) {
                                    GestureType.SinglePress -> Color(0xFF007ACC)
                                    GestureType.DoublePress -> NothingRed
                                    GestureType.LongPress -> Color(0xFFE65100)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = exec.gesture.displayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = exec.action.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    if (exec.isScreenLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF3E2723), RoundedCornerShape(4.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "LOCKED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .background(
                            color = when (exec.result) {
                                is ActionResult.Success -> StatusGreen
                                is ActionResult.Failure -> NothingRed
                                is ActionResult.Unavailable -> Color(0xFFE65100)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = when (exec.result) {
                            is ActionResult.Success -> "SUCCESS"
                            is ActionResult.Failure -> "FAILURE"
                            is ActionResult.Unavailable -> "BLOCKED"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exec.result.message,
                    fontSize = 11.sp,
                    color = Color(0xFFB0B0B0),
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = exec.formattedTime,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun GestureHistoryCard(gesture: GestureEvent) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191919)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            color = when (gesture.type) {
                                GestureType.SinglePress -> Color(0xFF007ACC)
                                GestureType.DoublePress -> NothingRed
                                GestureType.LongPress -> Color(0xFFE65100)
                            },
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = gesture.type.displayName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (gesture.isScreenLocked) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF3E2723), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LOCKED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = gesture.formattedTime,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
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

@Composable
fun EmptyConsolePlaceholder(title: String, description: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF777777),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
