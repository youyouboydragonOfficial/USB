package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LiveSpeedPoint
import com.example.ui.theme.*

@Composable
fun SpeedGraphCanvas(
    speedPoints: List<LiveSpeedPoint>,
    modifier: Modifier = Modifier
) {
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
                text = "⚡ REAL-TIME THROUGHPUT (MB/S)",
                color = TextSecondaryDark,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(CyberCyan, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WRITE", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(NeonEmerald, RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("READ", color = NeonEmerald, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            if (speedPoints.size < 2) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "WAITING FOR THROUGHPUT METRICS...",
                        color = TextMutedDark,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Grid lines
                    val gridCount = 4
                    for (i in 0..gridCount) {
                        val y = h * (i.toFloat() / gridCount)
                        drawLine(
                            color = CardBorderDark.copy(alpha = 0.4f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    val maxWrite = speedPoints.maxOfOrNull { it.writeSpeedMbps } ?: 10.0
                    val maxRead = speedPoints.maxOfOrNull { it.readSpeedMbps } ?: 10.0
                    val maxY = maxOf(maxWrite, maxRead, 10.0).toFloat()

                    // Write Path (Cyber Cyan)
                    val writePath = Path()
                    val readPath = Path()

                    val pointCount = speedPoints.size
                    val stepX = w / (pointCount - 1).coerceAtLeast(1)

                    speedPoints.forEachIndexed { idx, pt ->
                        val x = idx * stepX
                        val yWrite = h - ((pt.writeSpeedMbps.toFloat() / maxY) * h).coerceIn(0f, h)
                        val yRead = h - ((pt.readSpeedMbps.toFloat() / maxY) * h).coerceIn(0f, h)

                        if (idx == 0) {
                            writePath.moveTo(x, yWrite)
                            readPath.moveTo(x, yRead)
                        } else {
                            writePath.lineTo(x, yWrite)
                            readPath.lineTo(x, yRead)
                        }
                    }

                    // Draw Write Line
                    drawPath(
                        path = writePath,
                        color = CyberCyan,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Read Line
                    drawPath(
                        path = readPath,
                        color = NeonEmerald,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
