package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TestConfig
import com.example.data.model.TestPreset
import com.example.data.model.VerifyMethod
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TestConfigPanel(
    config: TestConfig,
    onConfigChange: (TestConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomSizeInput by remember { mutableStateOf(config.preset == TestPreset.CUSTOM) }
    var customSizeText by remember { mutableStateOf(config.customSizeMb.toString()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardSurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "⚙️ TEST CONFIGURATION & PRESETS",
            color = TextSecondaryDark,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Target Test Size Presets
        Text("テストサイズ (Target Size):", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TestPreset.entries.forEach { preset ->
                FilterChip(
                    selected = config.preset == preset,
                    onClick = {
                        showCustomSizeInput = (preset == TestPreset.CUSTOM)
                        onConfigChange(config.copy(preset = preset))
                    },
                    label = { Text(preset.displayName, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan,
                        selectedLabelColor = ObsidianDark,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondaryDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = CardBorderDark,
                        selectedBorderColor = CyberCyan,
                        enabled = true,
                        selected = config.preset == preset
                    )
                )
            }
        }

        // Custom Size Input Field
        AnimatedVisibility(visible = config.preset == TestPreset.CUSTOM) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                OutlinedTextField(
                    value = customSizeText,
                    onValueChange = { input ->
                        customSizeText = input
                        val parsed = input.toLongOrNull() ?: 512L
                        onConfigChange(config.copy(customSizeMb = parsed))
                    },
                    label = { Text("カスタムサイズ (MB)", color = TextSecondaryDark) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CardBorderDark,
                        focusedTextColor = TextPrimaryDark,
                        unfocusedTextColor = TextPrimaryDark
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Block Size Selection
        Text("ブロックサイズ (Chunk Size):", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(4, 16, 64).forEach { sizeMb ->
                FilterChip(
                    selected = config.blockSizeMb == sizeMb,
                    onClick = { onConfigChange(config.copy(blockSizeMb = sizeMb)) },
                    label = { Text("${sizeMb} MB", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = LaserPurple,
                        selectedLabelColor = TextPrimaryDark,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondaryDark
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("テスト終了時にデータ削除", color = TextPrimaryDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("テスト用に作成されたダミーファイルを自動消去します", color = TextSecondaryDark, fontSize = 11.sp)
            }
            Switch(
                checked = config.autoDeleteTestFiles,
                onCheckedChange = { onConfigChange(config.copy(autoDeleteTestFiles = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = CyberCyan,
                    checkedTrackColor = CyberCyanDim
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("⚠️ 偽装USBデモモード", color = HazardAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("実機の代わりに容量偽装エラーを人工生成して検証UIを動作確認します", color = TextSecondaryDark, fontSize = 11.sp)
            }
            Switch(
                checked = config.simulateFakeDrive,
                onCheckedChange = { onConfigChange(config.copy(simulateFakeDrive = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = HazardAmber,
                    checkedTrackColor = HazardAmber.copy(alpha = 0.4f)
                )
            )
        }
    }
}
