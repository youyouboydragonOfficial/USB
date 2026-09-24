package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.TestReportEntity
import com.example.ui.theme.*

@Composable
fun ReportCertificateDialog(
    report: TestReportEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛡️ USB GUARDIAN DIAGNOSIS REPORT",
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Result Banner Badge
                val isGenuine = report.isGenuine && !report.isFakeDrive
                Surface(
                    color = if (isGenuine) NeonEmerald.copy(alpha = 0.15f) else CrimsonAlert.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (isGenuine) NeonEmerald else CrimsonAlert),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (isGenuine) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isGenuine) NeonEmerald else CrimsonAlert,
                            modifier = Modifier.size(40.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isGenuine) "GENUINE SAFE STORAGE" else "FAKE CAPACITY DETECTED!",
                            color = if (isGenuine) NeonEmerald else CrimsonAlert,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = if (isGenuine) "全テストブロックの正常なデータ維持を確認しました。" else "コントローラーの偽装またはセクタ破損が検出されました。",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Health Score Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("HEALTH SCORE", color = TextSecondaryDark, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text(
                            text = "${report.healthScore} / 100",
                            color = if (report.healthScore >= 90) NeonEmerald else if (report.healthScore >= 50) HazardAmber else CrimsonAlert,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    LinearProgressIndicator(
                        progress = { report.healthScore / 100f },
                        modifier = Modifier
                            .width(100.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = if (report.healthScore >= 90) NeonEmerald else if (report.healthScore >= 50) HazardAmber else CrimsonAlert,
                        trackColor = CardBorderDark
                    )
                }

                // Metrics List
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportRow("デバイス名 (Drive Name):", report.driveName)
                    ReportRow("テスト実施容量 (Tested Size):", "${report.testedSizeMb} MB")
                    ReportRow("検証成功容量 (Verified Size):", "${report.verifiedSizeMb} MB", if (report.corruptBlocks > 0) CrimsonAlert else NeonEmerald)
                    ReportRow("全ブロック数 (Total Blocks):", "${report.totalBlocks} 個")
                    ReportRow("破損ブロック数 (Corrupt Blocks):", "${report.corruptBlocks} 個", if (report.corruptBlocks > 0) CrimsonAlert else TextPrimaryDark)
                    ReportRow("最大書き込み速度 (Max Write):", String.format("%.1f MB/s", report.maxWriteSpeedMbps), CyberCyan)
                    ReportRow("最大読み出し速度 (Max Read):", String.format("%.1f MB/s", report.maxReadSpeedMbps), NeonEmerald)
                    ReportRow("所要時間 (Duration):", "${report.durationSeconds} 秒")
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "USB Guard Test Report - ${report.driveName}")
                            putExtra(Intent.EXTRA_TEXT, buildShareText(report))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "レポートを共有"))
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = CyberCyan)
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = ObsidianDark)
                ) {
                    Text("閉じる", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = CardSurfaceDark
    )
}

@Composable
private fun ReportRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = TextPrimaryDark) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondaryDark, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

private fun buildShareText(report: TestReportEntity): String {
    val status = if (report.isGenuine) "【正常容量】" else "【🚨容量偽装検出】"
    return """
        [USB Drive Guard 検査結果レポート]
        判定: $status
        対象ドライブ: ${report.driveName}
        テスト容量: ${report.testedSizeMb} MB
        正常検証容量: ${report.verifiedSizeMb} MB
        破損ブロック: ${report.corruptBlocks} / ${report.totalBlocks}
        最高書き込み速度: ${String.format("%.1f", report.maxWriteSpeedMbps)} MB/s
        最高読み出し速度: ${String.format("%.1f", report.maxReadSpeedMbps)} MB/s
        スコア: ${report.healthScore}/100
        #USBGuard #USBTester #AndroidOTG
    """.trimIndent()
}
