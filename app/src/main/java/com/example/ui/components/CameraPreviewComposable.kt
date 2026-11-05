package com.example.ui.components

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*
import com.example.vision.CameraManager

@Composable
fun CameraPreviewComposable(
    cameraManager: CameraManager,
    onCaptureClick: () -> Unit,
    onSwitchLensClick: () -> Unit,
    onToggleTorchClick: () -> Unit,
    modifier: Modifier = Modifier,
    isCapturing: Boolean = false,
    hasCameraPermission: Boolean = true
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(hasCameraPermission, lifecycleOwner, previewViewRef) {
        val pv = previewViewRef
        if (hasCameraPermission && pv != null) {
            cameraManager.startCamera(lifecycleOwner, pv, cameraManager.isFrontCamera)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hud_scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .border(1.5.dp, JarvisBorder, RoundedCornerShape(16.dp))
    ) {
        if (hasCameraPermission) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewViewRef = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view")
            )

            // Holographic HUD Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Scan line
                val currentScanY = h * scanY
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, JarvisCyan, JarvisCyan, Color.Transparent)
                    ),
                    start = Offset(0f, currentScanY),
                    end = Offset(w, currentScanY),
                    strokeWidth = 2f
                )

                // Central Targeting Reticle
                val centerX = w / 2f
                val centerY = h / 2f
                val boxSize = minOf(w, h) * 0.45f
                val half = boxSize / 2f

                // Outer Target Rect with Corner Brackets
                val bracketLen = 30f
                // Top-Left
                drawLine(JarvisCyan, Offset(centerX - half, centerY - half), Offset(centerX - half + bracketLen, centerY - half), 3f)
                drawLine(JarvisCyan, Offset(centerX - half, centerY - half), Offset(centerX - half, centerY - half + bracketLen), 3f)
                // Top-Right
                drawLine(JarvisCyan, Offset(centerX + half, centerY - half), Offset(centerX + half - bracketLen, centerY - half), 3f)
                drawLine(JarvisCyan, Offset(centerX + half, centerY - half), Offset(centerX + half, centerY - half + bracketLen), 3f)
                // Bottom-Left
                drawLine(JarvisCyan, Offset(centerX - half, centerY + half), Offset(centerX - half + bracketLen, centerY + half), 3f)
                drawLine(JarvisCyan, Offset(centerX - half, centerY + half), Offset(centerX - half, centerY + half - bracketLen), 3f)
                // Bottom-Right
                drawLine(JarvisCyan, Offset(centerX + half, centerY + half), Offset(centerX + half - bracketLen, centerY + half), 3f)
                drawLine(JarvisCyan, Offset(centerX + half, centerY + half), Offset(centerX + half, centerY + half - bracketLen), 3f)

                // Center crosshair
                drawLine(JarvisCyan.copy(alpha = 0.5f), Offset(centerX - 12f, centerY), Offset(centerX + 12f, centerY), 1.5f)
                drawLine(JarvisCyan.copy(alpha = 0.5f), Offset(centerX, centerY - 12f), Offset(centerX, centerY + 12f), 1.5f)
            }
        } else {
            // Permission missing placeholder
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Camera Permission Required",
                    tint = JarvisCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "OPTICAL SENSORS OFFLINE",
                    color = JarvisCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Camera permission is required for live visual perception and OCR extraction, sir.",
                    color = JarvisOnSurfaceVariant,
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Top Telemetry Badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, JarvisBorder, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (hasCameraPermission) JarvisEmerald else JarvisCrimson)
                )
                Text(
                    text = if (cameraManager.isFrontCamera) "CAM: FRONT (USER)" else "CAM: REAR (OPTICAL)",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            // Quick Hardware Controls: Torch & Switch
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!cameraManager.isFrontCamera) {
                    IconButton(
                        onClick = onToggleTorchClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (cameraManager.isTorchOn) JarvisGold else Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, JarvisBorder, CircleShape)
                            .testTag("torch_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (cameraManager.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Torch",
                            tint = if (cameraManager.isTorchOn) Color.Black else Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val pv = previewViewRef
                        if (pv != null) {
                            cameraManager.switchCamera(lifecycleOwner, pv)
                        }
                        onSwitchLensClick()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, JarvisBorder, CircleShape)
                        .testTag("switch_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera Lens",
                        tint = JarvisCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Shutter Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .clip(CircleShape)
                    .background(JarvisDarkBg.copy(alpha = 0.8f))
                    .border(2.dp, JarvisCyan, CircleShape)
                    .clickable(enabled = !isCapturing && hasCameraPermission) {
                        onCaptureClick()
                    }
                    .testTag("camera_shutter_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) JarvisGold else JarvisCyan)
                )
            }
        }
    }
}
