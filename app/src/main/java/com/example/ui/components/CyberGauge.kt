package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TestPhase
import com.example.data.model.TestProgressState
import com.example.ui.theme.*

@Composable
fun CyberGauge(
    progressState: TestProgressState,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progressState.overallProgressRatio,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "GaugeProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "GaugePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "RotationAngle"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 14.dp.toPx()
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            val startAngle = 135f
            val sweepAngle = 270f

            // Background Track
            drawArc(
                color = CardBorderDark,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Dynamic Active Gradient
            val activeGradient = when (progressState.phase) {
                TestPhase.WRITING -> Brush.sweepGradient(
                    0.0f to CyberCyan,
                    1.0f to LaserPurple
                )
                TestPhase.VERIFYING -> Brush.sweepGradient(
                    0.0f to NeonEmerald,
                    1.0f to CyberCyan
                )
                TestPhase.COMPLETED -> if (progressState.corruptBlockCount > 0) {
                    Brush.sweepGradient(0.0f to CrimsonAlert, 1.0f to HazardAmber)
                } else {
                    Brush.sweepGradient(0.0f to NeonEmerald, 1.0f to CyberCyan)
                }
                TestPhase.PAUSED -> Brush.sweepGradient(0.0f to HazardAmber, 1.0f to CardBorderDark)
                else -> Brush.sweepGradient(0.0f to CyberCyanDim, 1.0f to CardBorderDark)
            }

            // Progress Fill Arc
            val currentSweep = sweepAngle * animatedProgress.coerceIn(0f, 1f)
            if (currentSweep > 0f) {
                drawArc(
                    brush = activeGradient,
                    startAngle = startAngle,
                    sweepAngle = currentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Outer Tech Dash Ring
            if (progressState.phase == TestPhase.WRITING || progressState.phase == TestPhase.VERIFYING) {
                val outerRadius = (size.width / 2) - 2.dp.toPx()
                val dashCount = 36
                val center = Offset(size.width / 2, size.height / 2)
                for (i in 0 until dashCount) {
                    val angleRad = Math.toRadians((i * (360.0 / dashCount) + rotationAngle)).toFloat()
                    val startX = center.x + (outerRadius - 8.dp.toPx()) * Math.cos(angleRad.toDouble()).toFloat()
                    val startY = center.y + (outerRadius - 8.dp.toPx()) * Math.sin(angleRad.toDouble()).toFloat()
                    val endX = center.x + outerRadius * Math.cos(angleRad.toDouble()).toFloat()
                    val endY = center.y + outerRadius * Math.sin(angleRad.toDouble()).toFloat()
                    drawLine(
                        color = CyberCyan.copy(alpha = pulseAlpha * 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Inner Information Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val percentInt = (animatedProgress * 100).toInt()

            Text(
                text = when (progressState.phase) {
                    TestPhase.WRITING -> "WRITE PHASE"
                    TestPhase.VERIFYING -> "VERIFY PHASE"
                    TestPhase.COMPLETED -> if (progressState.corruptBlockCount > 0) "CORRUPTED" else "VERIFIED"
                    TestPhase.PAUSED -> "PAUSED"
                    TestPhase.PREPARING -> "PREPARING"
                    else -> "READY"
                },
                color = when (progressState.phase) {
                    TestPhase.WRITING -> CyberCyan
                    TestPhase.VERIFYING -> NeonEmerald
                    TestPhase.COMPLETED -> if (progressState.corruptBlockCount > 0) CrimsonAlert else NeonEmerald
                    TestPhase.PAUSED -> HazardAmber
                    else -> TextSecondaryDark
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "$percentInt%",
                color = TextPrimaryDark,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Current Speed Display
            val activeSpeed = if (progressState.phase == TestPhase.VERIFYING) {
                progressState.currentReadSpeedMbps
            } else {
                progressState.currentWriteSpeedMbps
            }

            Text(
                text = if (activeSpeed > 0) String.format("%.1f MB/s", activeSpeed) else "-- MB/s",
                color = if (progressState.phase == TestPhase.VERIFYING) NeonEmerald else CyberCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
