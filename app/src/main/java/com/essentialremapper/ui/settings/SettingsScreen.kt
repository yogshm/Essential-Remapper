package com.essentialremapper.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.essentialremapper.data.model.HapticIntensity
import com.essentialremapper.data.model.OverlayStyle
import com.essentialremapper.data.model.RemapperSettings
import com.essentialremapper.domain.device.DeviceProfile
import com.essentialremapper.ui.theme.NothingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: RemapperSettings,
    deviceProfile: DeviceProfile,
    onUpdateSettings: ((RemapperSettings) -> RemapperSettings) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Device Section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeader("DEVICE")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = deviceProfile.displayName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Model: ${deviceProfile.modelNames.firstOrNull() ?: "A059P"} • Codename: ${deviceProfile.deviceCodename}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Hardware Key: ScanCode ${deviceProfile.essentialButtonScanCode}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 2. Gestures Section
            item {
                SectionHeader("GESTURES & TIMINGS")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Double Press Timeout", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("${settings.doublePressTimeoutMs} ms", fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = NothingRed)
                        }
                        Slider(
                            value = settings.doublePressTimeoutMs.toFloat(),
                            onValueChange = { newValue ->
                                onUpdateSettings { it.copy(doublePressTimeoutMs = newValue.toLong()) }
                            },
                            valueRange = 200f..600f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = NothingRed, activeTrackColor = NothingRed)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Long Press Duration", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("${settings.longPressDurationMs} ms", fontSize = 14.sp, fontFamily = FontFamily.Monospace, color = NothingRed)
                        }
                        Slider(
                            value = settings.longPressDurationMs.toFloat(),
                            onValueChange = { newValue ->
                                onUpdateSettings { it.copy(longPressDurationMs = newValue.toLong()) }
                            },
                            valueRange = 400f..1200f,
                            steps = 7,
                            colors = SliderDefaults.colors(thumbColor = NothingRed, activeTrackColor = NothingRed)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Lock Screen Execution",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )

                        SettingSwitchRow(
                            title = "Double Press when Locked",
                            subtitle = "Allows flashlight/actions while screen is off",
                            checked = settings.lockScreenSettings.doublePressEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings {
                                    it.copy(lockScreenSettings = it.lockScreenSettings.copy(doublePressEnabled = checked))
                                }
                            }
                        )
                    }
                }
            }

            // 3. Visual Overlay Section
            item {
                SectionHeader("VISUAL OVERLAY")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingSwitchRow(
                            title = "Visual Indicator",
                            subtitle = "Show button feedback animation near the physical key",
                            checked = settings.overlayEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(overlayEnabled = checked) }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Style", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateSettings { it.copy(overlayStyle = OverlayStyle.NOTHING_DOT_MATRIX) }
                                }
                        ) {
                            RadioButton(
                                selected = settings.overlayStyle == OverlayStyle.NOTHING_DOT_MATRIX,
                                onClick = {
                                    onUpdateSettings { it.copy(overlayStyle = OverlayStyle.NOTHING_DOT_MATRIX) }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NothingRed)
                            )
                            Text("Nothing Dot-Matrix", fontSize = 14.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateSettings { it.copy(overlayStyle = OverlayStyle.ANDROID_STOCK) }
                                }
                        ) {
                            RadioButton(
                                selected = settings.overlayStyle == OverlayStyle.ANDROID_STOCK,
                                onClick = {
                                    onUpdateSettings { it.copy(overlayStyle = OverlayStyle.ANDROID_STOCK) }
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = NothingRed)
                            )
                            Text("Android Stock Pill", fontSize = 14.sp)
                        }
                    }
                }
            }

            // 4. Haptics Section
            item {
                SectionHeader("HAPTICS")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingSwitchRow(
                            title = "Haptic Feedback",
                            subtitle = "Vibrate on button action recognition",
                            checked = settings.hapticEnabled,
                            onCheckedChange = { checked ->
                                onUpdateSettings { it.copy(hapticEnabled = checked) }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Intensity", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HapticIntensity.entries.filter { it != HapticIntensity.DISABLED }.forEach { intensity ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        onUpdateSettings { it.copy(hapticIntensity = intensity) }
                                    }
                                ) {
                                    RadioButton(
                                        selected = settings.hapticIntensity == intensity,
                                        onClick = {
                                            onUpdateSettings { it.copy(hapticIntensity = intensity) }
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = NothingRed)
                                    )
                                    Text(intensity.displayName, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Backup Section
            item {
                SectionHeader("BACKUP")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "JSON Configuration Backup",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Storage Access Framework export and import will be available in Phase 11.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // 6. About Section
            item {
                SectionHeader("ABOUT")
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Essential Button Remapper",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version 1.0.0-alpha • Phase 2 Build",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Privacy First: No internet permissions, zero analytics, zero data collection. All configurations stay 100% on your device.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            lineHeight = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NothingRed
            )
        )
    }
}
