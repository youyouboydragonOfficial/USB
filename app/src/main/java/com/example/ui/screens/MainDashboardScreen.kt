package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TestPhase
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.UsbTesterViewModel

@Composable
fun MainDashboardScreen(
    viewModel: UsbTesterViewModel,
    onSelectUsbClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val driveInfo by viewModel.driveInfo.collectAsState()
    val testConfig by viewModel.testConfig.collectAsState()
    val progressState by viewModel.progressState.collectAsState()
    val speedPoints by viewModel.speedPoints.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var isConfigExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // 1. Drive Selection Card
        DriveSelectorCard(
            driveInfo = driveInfo,
            onSelectUsbClick = onSelectUsbClick,
            onSelectSandboxClick = { viewModel.selectSandboxStorage() }
        )

        // 2. Central Gauge & Real-time Metrics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardSurfaceDark)
                .border(1.dp, CardBorderDark, RoundedCornerShape(16.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CyberGauge(progressState = progressState)

                // Status Message Text Banner
                Text(
                    text = progressState.statusText,
                    color = if (progressState.corruptBlockCount > 0) CrimsonAlert else CyberCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                // Metrics Row Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricTile(
                        label = "MAX WRITE",
                        value = String.format("%.1f MB/s", progressState.maxWriteSpeedMbps),
                        color = CyberCyan
                    )
                    MetricTile(
                        label = "MAX READ",
                        value = String.format("%.1f MB/s", progressState.maxReadSpeedMbps),
                        color = NeonEmerald
                    )
                    MetricTile(
                        label = "ELAPSED",
                        value = formatDuration(progressState.elapsedTimeMs / 1000),
                        color = TextPrimaryDark
                    )
                    MetricTile(
                        label = "CORRUPT",
                        value = "${progressState.corruptBlockCount} BLOCKS",
                        color = if (progressState.corruptBlockCount > 0) CrimsonAlert else TextMutedDark
                    )
                }
            }
        }

        // 3. Main Test Action Control Buttons Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (progressState.phase) {
                TestPhase.WRITING, TestPhase.VERIFYING -> {
                    Button(
                        onClick = { viewModel.pauseTest() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HazardAmber, contentColor = ObsidianDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("一時停止", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.cancelTest() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonAlert),
                        border = BorderStroke(1.dp, CrimsonAlert),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Stop, contentDescription = "Cancel")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("中止", fontWeight = FontWeight.Bold)
                    }
                }
                TestPhase.PAUSED -> {
                    Button(
                        onClick = { viewModel.resumeTest() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald, contentColor = ObsidianDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Resume")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("再開", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {
                    Button(
                        onClick = {
                            if (driveInfo.isSelected) {
                                viewModel.startTest()
                            } else {
                                onSelectUsbClick()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (driveInfo.isSelected) CyberCyan else HazardAmber,
                            contentColor = ObsidianDark
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (driveInfo.isSelected) Icons.Default.PlayArrow else Icons.Default.Usb,
                            contentDescription = "Start Test"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (driveInfo.isSelected) "検証テスト開始" else "USBを選択して開始",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }

                    IconButton(
                        onClick = { viewModel.cleanupTestFiles() },
                        enabled = driveInfo.isSelected,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Cleanup", tint = if (driveInfo.isSelected) TextSecondaryDark else TextMutedDark)
                    }
                }
            }
        }

        // 4. Real-time Speed Graph
        SpeedGraphCanvas(speedPoints = speedPoints)

        // 5. Block Integrity Matrix Grid
        BlockMatrixGrid(blocks = progressState.blocks)

        // 6. Test Settings Collapsible Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
            border = BorderStroke(1.dp, CardBorderDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = CyberCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "テスト設定パラメータ (${testConfig.preset.displayName})",
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = { isConfigExpanded = !isConfigExpanded }) {
                        Icon(
                            imageVector = if (isConfigExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Config",
                            tint = TextSecondaryDark
                        )
                    }
                }

                AnimatedVisibility(visible = isConfigExpanded) {
                    TestConfigPanel(
                        config = testConfig,
                        onConfigChange = { viewModel.updateConfig(it) }
                    )
                }
            }
        }

        // 7. Live Terminal Console Log
        TerminalLogView(logs = logs)

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun MetricTile(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextMutedDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Text(value, color = color, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.ExtraBold)
    }
}

private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
