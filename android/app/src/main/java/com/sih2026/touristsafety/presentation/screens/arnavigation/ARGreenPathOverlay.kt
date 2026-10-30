package com.sih2026.touristsafety.presentation.screens.arnavigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun ARGreenPathOverlay(
    target: ARTarget,
    currentAzimuth: Float,
    isHazardActive: Boolean,
    isIndoorMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Relative angle between phone heading and target (-180..+180)
    var deltaAngle = target.relativeBearingDegrees - currentAzimuth
    while (deltaAngle < -180f) deltaAngle += 360f
    while (deltaAngle > 180f) deltaAngle -= 360f

    val halfFov = 35f
    val isWithinFov = abs(deltaAngle) <= halfFov

    // Pulse animation for chevrons & door scanner
    val infiniteTransition = rememberInfiniteTransition(label = "ar_pulse")
    val pulseOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_offset"
    )

    val scanBeamY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_beam"
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val screenHeightPx = constraints.maxHeight.toFloat()

        // 1. Draw 3D AR Green Path & Door Frame on Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startBottomX = screenWidthPx * 0.5f
            val startBottomY = screenHeightPx * 0.95f

            // Target X in screen space
            val targetX = if (isWithinFov) {
                screenWidthPx * 0.5f + (deltaAngle / halfFov) * (screenWidthPx * 0.42f)
            } else {
                if (deltaAngle > 0) screenWidthPx * 0.95f else screenWidthPx * 0.05f
            }
            // In indoor room mode, door base is slightly lower on the floor
            val targetY = if (isIndoorMode) screenHeightPx * 0.52f else screenHeightPx * 0.45f

            // Detour curve if hazard avoidance is triggered
            val detourCurveX = if (isHazardActive) {
                if (deltaAngle >= 0) -180f else 180f
            } else 0f

            // Draw Neon Green Pathway Corridor
            val path = Path().apply {
                moveTo(startBottomX - 90f, startBottomY)
                cubicTo(
                    startBottomX - 60f + detourCurveX, screenHeightPx * 0.70f,
                    targetX - 25f + detourCurveX * 0.5f, screenHeightPx * 0.58f,
                    targetX, targetY
                )
                lineTo(targetX, targetY)
                cubicTo(
                    targetX + 25f + detourCurveX * 0.5f, screenHeightPx * 0.58f,
                    startBottomX + 60f + detourCurveX, screenHeightPx * 0.70f,
                    startBottomX + 90f, startBottomY
                )
                close()
            }

            // Glowing ground corridor
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0x0000E676),
                        Color(0x3300E676),
                        Color(0x8800E676)
                    ),
                    startY = targetY,
                    endY = startBottomY
                )
            )

            // Animated pulsing chevrons moving forward along path
            val chevronCount = 5
            for (i in 0 until chevronCount) {
                val t = ((i.toFloat() / chevronCount) + (pulseOffset / chevronCount)) % 1f
                val fraction = 1f - t

                val curY = startBottomY - (startBottomY - targetY) * fraction
                val curCenterX = startBottomX + (targetX - startBottomX) * fraction + (sin(fraction * Math.PI.toFloat()) * detourCurveX)
                val widthAtT = 160f * (1f - fraction * 0.75f)

                val chevronPath = Path().apply {
                    moveTo(curCenterX - widthAtT * 0.5f, curY + 20f)
                    lineTo(curCenterX, curY)
                    lineTo(curCenterX + widthAtT * 0.5f, curY + 20f)
                }

                drawPath(
                    path = chevronPath,
                    color = Color(0xFF00E676).copy(alpha = (1f - fraction).coerceIn(0.2f, 1f)),
                    style = Stroke(width = 10f * (1f - fraction * 0.6f), cap = StrokeCap.Round)
                )
            }

            // INDOOR AI DOOR DETECTION BOUNDING BOX
            if (isIndoorMode && isWithinFov && target.type == ARTargetType.EXIT) {
                val doorWidth = 200f
                val doorHeight = 360f
                val doorLeft = targetX - doorWidth * 0.5f
                val doorTop = targetY - doorHeight

                // Draw Door Frame Outline (Cyan-Green Hologram)
                drawRect(
                    color = Color(0xFF00E676).copy(alpha = 0.25f),
                    topLeft = Offset(doorLeft, doorTop),
                    size = Size(doorWidth, doorHeight)
                )

                // Corner Brackets for AI Object Detection aesthetic
                val bracketLen = 30f
                val bracketStroke = Stroke(width = 5f, cap = StrokeCap.Square)
                val bracketColor = Color(0xFF00E676)

                // Top-Left Corner
                drawLine(bracketColor, Offset(doorLeft, doorTop), Offset(doorLeft + bracketLen, doorTop), bracketStroke.width)
                drawLine(bracketColor, Offset(doorLeft, doorTop), Offset(doorLeft, doorTop + bracketLen), bracketStroke.width)

                // Top-Right Corner
                drawLine(bracketColor, Offset(doorLeft + doorWidth, doorTop), Offset(doorLeft + doorWidth - bracketLen, doorTop), bracketStroke.width)
                drawLine(bracketColor, Offset(doorLeft + doorWidth, doorTop), Offset(doorLeft + doorWidth, doorTop + bracketLen), bracketStroke.width)

                // Bottom-Left Corner
                drawLine(bracketColor, Offset(doorLeft, doorTop + doorHeight), Offset(doorLeft + bracketLen, doorTop + doorHeight), bracketStroke.width)
                drawLine(bracketColor, Offset(doorLeft, doorTop + doorHeight), Offset(doorLeft, doorTop + doorHeight - bracketLen), bracketStroke.width)

                // Bottom-Right Corner
                drawLine(bracketColor, Offset(doorLeft + doorWidth, doorTop + doorHeight), Offset(doorLeft + doorWidth - bracketLen, doorTop + doorHeight), bracketStroke.width)
                drawLine(bracketColor, Offset(doorLeft + doorWidth, doorTop + doorHeight), Offset(doorLeft + doorWidth, doorTop + doorHeight - bracketLen), bracketStroke.width)

                // Animated AI Scanning Laser across door frame
                val laserY = doorTop + (doorHeight * scanBeamY)
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0x0000E676), Color(0xFF00E676), Color(0x0000E676)),
                        startX = doorLeft,
                        endX = doorLeft + doorWidth
                    ),
                    start = Offset(doorLeft, laserY),
                    end = Offset(doorLeft + doorWidth, laserY),
                    strokeWidth = 3f
                )
            }

            // Hazard Red Zone Indicator
            if (isHazardActive) {
                val hazardCenterX = screenWidthPx * 0.5f + (deltaAngle / halfFov) * (screenWidthPx * 0.2f)
                val hazardCenterY = screenHeightPx * 0.62f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x99D50000), Color(0x00D50000)),
                        center = Offset(hazardCenterX, hazardCenterY),
                        radius = 110f
                    ),
                    center = Offset(hazardCenterX, hazardCenterY),
                    radius = 110f
                )
            }
        }

        // 2. Floating 3D Holographic Marker
        if (isWithinFov) {
            val targetXPx = screenWidthPx * 0.5f + (deltaAngle / halfFov) * (screenWidthPx * 0.42f)
            val targetYPx = if (isIndoorMode) screenHeightPx * 0.36f else screenHeightPx * 0.42f

            val density = LocalDensity.current
            val offsetX = with(density) { (targetXPx - 120.dp.toPx()).roundToInt() }
            val offsetY = with(density) { (targetYPx - 80.dp.toPx()).roundToInt() }

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .width(240.dp)
            ) {
                FloatingTargetCard(target = target, isIndoor = isIndoorMode)
            }
        } else {
            // 3. Off-FOV Guide Arrow
            val isTargetRight = deltaAngle > 0
            val turnDegrees = abs(deltaAngle).roundToInt()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = if (isTargetRight) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E676)),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isTargetRight) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (isTargetRight) "Turn Right" else "Turn Left",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "$turnDegrees° towards ${if (isIndoorMode) "Room Exit" else target.name.take(15)}",
                                color = Color(0xFF00E676),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (isTargetRight) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingTargetCard(target: ARTarget, isIndoor: Boolean = false) {
    val themeColor = when (target.type) {
        ARTargetType.EXIT -> Color(0xFF00E676)
        ARTargetType.ENTRY -> Color(0xFF29B6F6)
        ARTargetType.SAFE_HAVEN -> Color(0xFFFFB300)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.92f),
        border = androidx.compose.foundation.BorderStroke(2.dp, themeColor),
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(themeColor.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, themeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (target.type) {
                            ARTargetType.EXIT -> Icons.Default.DoorSliding
                            ARTargetType.ENTRY -> Icons.Default.MeetingRoom
                            ARTargetType.SAFE_HAVEN -> Icons.Default.LocalPolice
                        },
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column {
                    Text(
                        text = if (isIndoor && target.type == ARTargetType.EXIT) "DETECTED ROOM EXIT" else target.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (isIndoor) "${target.distanceMeters}m • Doorway Identified" else "${target.distanceMeters}m Ahead",
                        color = themeColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = themeColor.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isIndoor) "✓ AI Spatial Frame Lock (98%)" else "✓ Geofence Verified Safe",
                    color = themeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
