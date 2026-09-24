package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockInfo
import com.example.data.model.BlockState
import com.example.ui.theme.*

@Composable
fun BlockMatrixGrid(
    blocks: List<BlockInfo>,
    modifier: Modifier = Modifier
) {
    var selectedBlock by remember { mutableStateOf<BlockInfo?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardSurfaceDark)
            .border(1.dp, CardBorderDark, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🧩 BLOCK INTEGRITY MATRIX (${blocks.size} BLOCKS)",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            val passedCount = blocks.count { it.state == BlockState.PASSED }
            val corruptCount = blocks.count { it.state == BlockState.CORRUPTED }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "PASS: $passedCount",
                    color = NeonEmerald,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (corruptCount > 0) {
                    Text(
                        text = "FAIL: $corruptCount",
                        color = CrimsonAlert,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (blocks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO TEST BLOCKS GENERATED YET",
                    color = TextMutedDark,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 28.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp)
            ) {
                items(blocks) { block ->
                    val tileColor = when (block.state) {
                        BlockState.PENDING -> SurfaceDark
                        BlockState.WRITING -> CyberCyan
                        BlockState.VERIFYING -> HazardAmber
                        BlockState.PASSED -> NeonEmerald
                        BlockState.CORRUPTED -> CrimsonAlert
                    }

                    val borderColor = when (block.state) {
                        BlockState.WRITING -> CyberCyan
                        BlockState.CORRUPTED -> CrimsonAlert
                        else -> CardBorderDark
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(tileColor)
                            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                            .clickable { selectedBlock = block },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${block.index + 1}",
                            color = if (block.state == BlockState.PENDING) TextMutedDark else ObsidianDark,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }

    // Selected Block Dialog Inspector
    selectedBlock?.let { block ->
        AlertDialog(
            onDismissRequest = { selectedBlock = null },
            title = {
                Text(
                    text = "BLOCK #${block.index + 1} INSPECTION",
                    color = CyberCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("サイズ: ${block.sizeMb} MB", color = TextPrimaryDark)
                    Text(
                        "ステータス: ${
                            when (block.state) {
                                BlockState.PASSED -> "合格 (PASSED)"
                                BlockState.CORRUPTED -> "破損 / 偽装検出 (CORRUPTED)"
                                BlockState.WRITING -> "書き込み中 (WRITING)"
                                BlockState.VERIFYING -> "検証中 (VERIFYING)"
                                else -> "待機中 (PENDING)"
                            }
                        }",
                        color = when (block.state) {
                            BlockState.PASSED -> NeonEmerald
                            BlockState.CORRUPTED -> CrimsonAlert
                            else -> TextSecondaryDark
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text("書き込み速度: ${if (block.writeSpeedMbps > 0) String.format("%.1f MB/s", block.writeSpeedMbps) else "--"}", color = TextSecondaryDark)
                    Text("読み出し速度: ${if (block.readSpeedMbps > 0) String.format("%.1f MB/s", block.readSpeedMbps) else "--"}", color = TextSecondaryDark)
                    if (block.corruptBytes > 0) {
                        Text("破損バイト数: ${block.corruptBytes} Bytes", color = CrimsonAlert, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedBlock = null }) {
                    Text("閉じる", color = CyberCyan)
                }
            },
            containerColor = CardSurfaceDark
        )
    }
}
