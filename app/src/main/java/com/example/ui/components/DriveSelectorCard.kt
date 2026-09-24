package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StorageDriveInfo
import com.example.ui.theme.*

@Composable
fun DriveSelectorCard(
    driveInfo: StorageDriveInfo,
    onSelectUsbClick: () -> Unit,
    onSelectSandboxClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                border = BorderStroke(
                    width = 1.5.dp,
                    brush = if (driveInfo.isSelected) {
                        if (driveInfo.isUsbOtg) Brush.horizontalGradient(listOf(CyberCyan, NeonEmerald))
                        else Brush.horizontalGradient(listOf(LaserPurple, CyberCyan))
                    } else {
                        Brush.horizontalGradient(listOf(CardBorderDark, HazardAmber.copy(alpha = 0.5f)))
                    }
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            if (!driveInfo.isSelected) {
                // UNSELECTED DRIVE STATE
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(HazardAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "USB STORAGE DISCONNECTED",
                                color = HazardAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                        }

                        Surface(
                            color = HazardAmber.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, HazardAmber.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "未選択",
                                color = HazardAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // USB Hub Visual Hero Icon
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(CyberCyan.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                            .border(1.5.dp, CyberCyan.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = "USB Drive",
                            tint = CyberCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "検証するUSBメモリを選択してください",
                        color = TextPrimaryDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "OTGアダプタでスマホにUSBメモリを接続し、フォルダのアクセス許可を与えてください。",
                        color = TextSecondaryDark,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Main Action CTA Button
                    Button(
                        onClick = onSelectUsbClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = ObsidianDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Usb,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔌 USBメモリのフォルダを選択 (SAF)",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onSelectSandboxClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryDark),
                        border = BorderStroke(1.dp, CardBorderDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = LaserPurple
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "📱 アプリ内サンドボックスで動作テスト",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // SELECTED DRIVE STATE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (driveInfo.isUsbOtg) Icons.Default.Usb else Icons.Default.Folder,
                            contentDescription = "Drive Icon",
                            tint = if (driveInfo.isUsbOtg) CyberCyan else LaserPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (driveInfo.isUsbOtg) "USB OTG MOUNTED" else "INTERNAL SANDBOX STORAGE",
                            color = if (driveInfo.isUsbOtg) CyberCyan else LaserPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        color = if (driveInfo.isWritable) NeonEmerald.copy(alpha = 0.15f) else HazardAmber.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, if (driveInfo.isWritable) NeonEmerald else HazardAmber)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (driveInfo.isWritable) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (driveInfo.isWritable) NeonEmerald else HazardAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (driveInfo.isWritable) "WRITABLE" else "READ-ONLY",
                                color = if (driveInfo.isWritable) NeonEmerald else HazardAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = driveInfo.name,
                    color = TextPrimaryDark,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Storage Statistics Grid
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
                        Text("AVAILABLE FREE", color = TextMutedDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(driveInfo.freeGbFormatted, color = CyberCyan, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("USED", color = TextMutedDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(driveInfo.usedGbFormatted, color = TextSecondaryDark, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("TOTAL CAPACITY", color = TextMutedDark, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        Text(driveInfo.totalGbFormatted, color = TextPrimaryDark, fontSize = 16.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Storage Visual Fill Bar
                LinearProgressIndicator(
                    progress = { driveInfo.usedRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (driveInfo.isUsbOtg) CyberCyan else LaserPurple,
                    trackColor = SurfaceDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Change / Re-select Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectUsbClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        border = BorderStroke(1.dp, CyberCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("USBを変更", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (driveInfo.isUsbOtg) {
                        OutlinedButton(
                            onClick = onSelectSandboxClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LaserPurple),
                            border = BorderStroke(1.dp, CardBorderDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("サンドボックス切替", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
