package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorCore(
    modifier: Modifier = Modifier,
    size: Dp = 180.dp,
    isActive: Boolean = true,
    energyOutput: String = "100%"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_reactor")
    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_outer"
    )

    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_inner"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = this.size.minDimension / 2f

            // Outer Glow Disk
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        JarvisCyan.copy(alpha = if (isActive) 0.25f else 0.08f),
                        JarvisDarkBg.copy(alpha = 0f)
                    ),
                    center = center,
                    radius = maxR
                ),
                radius = maxR,
                center = center
            )

            // Outer Ring 1
            drawCircle(
                color = JarvisBorder,
                radius = maxR * 0.92f,
                center = center,
                style = Stroke(width = 2f)
            )

            // Segmented Outer Arc (Rotating Clockwise)
            val outerSegments = 8
            for (i in 0 until outerSegments) {
                val startAngle = rotationOuter + (i * (360f / outerSegments))
                drawArc(
                    color = JarvisCyan.copy(alpha = 0.85f),
                    startAngle = startAngle,
                    sweepAngle = 24f,
                    useCenter = false,
                    topLeft = Offset(center.x - maxR * 0.85f, center.y - maxR * 0.85f),
                    size = Size(maxR * 1.7f, maxR * 1.7f),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
            }

            // Inner Ring (Rotating Counter-Clockwise) with Gold Accents
            val innerSegments = 6
            for (i in 0 until innerSegments) {
                val startAngle = rotationInner + (i * (360f / innerSegments))
                drawArc(
                    color = JarvisGold.copy(alpha = 0.75f),
                    startAngle = startAngle,
                    sweepAngle = 32f,
                    useCenter = false,
                    topLeft = Offset(center.x - maxR * 0.65f, center.y - maxR * 0.65f),
                    size = Size(maxR * 1.3f, maxR * 1.3f),
                    style = Stroke(width = 2.5f)
                )
            }

            // Radial spokes
            val spokes = 12
            for (i in 0 until spokes) {
                val angleRad = Math.toRadians((i * (360.0 / spokes) + rotationOuter).toDouble())
                val p1 = Offset(
                    (center.x + (maxR * 0.35f) * cos(angleRad)).toFloat(),
                    (center.y + (maxR * 0.35f) * sin(angleRad)).toFloat()
                )
                val p2 = Offset(
                    (center.x + (maxR * 0.58f) * cos(angleRad)).toFloat(),
                    (center.y + (maxR * 0.58f) * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = JarvisCyan.copy(alpha = 0.4f),
                    start = p1,
                    end = p2,
                    strokeWidth = 1.5f
                )
            }

            // Center Core Pulsing
            val coreR = maxR * 0.28f * pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        JarvisCyan,
                        JarvisCyan.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreR
                ),
                radius = coreR,
                center = center
            )
        }

        // Center Text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "JARVIS",
                color = JarvisCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = energyOutput,
                color = Color.White,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    modifier: Modifier = Modifier,
    amplitude: Float = 0f,
    barCount: Int = 16,
    color: Color = JarvisCyan
) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val wave = (sin(phase + (i * 0.5f)) * 0.5f + 0.5f)
            val normalizedAmp = if (amplitude > 0.05f) {
                ((wave * amplitude * 24) + 4).dp
            } else {
                3.dp
            }

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(normalizedAmp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(color.copy(alpha = if (amplitude > 0.05f) 0.9f else 0.3f))
            )
        }
    }
}

@Composable
fun StatusTelemetryBadge(
    label: String,
    statusText: String,
    modifier: Modifier = Modifier,
    color: Color = JarvisCyan
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(JarvisDarkSurfaceVariant)
            .border(1.dp, JarvisBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label.uppercase(),
            color = JarvisOnSurfaceVariant,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = statusText,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun TacticalProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = JarvisCyan
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(JarvisDarkSurfaceVariant)
                .border(0.5.dp, JarvisBorder, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(activeColor.copy(alpha = 0.6f), activeColor)
                        )
                    )
            )
        }
    }
}
