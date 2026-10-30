package com.sih2026.touristsafety.presentation.screens.arnavigation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARNavigationScreen(
    onNavigateBack: () -> Unit
) {
    var arMode by remember { mutableStateOf(ARMode.INDOOR) } // Default to Indoor for immediate room testing!
    var isHazardSimulated by remember { mutableStateOf(false) }

    // Dynamic indoor exit target that can calibrate/anchor to the room's real door
    var indoorExitBearing by remember { mutableFloatStateOf(0f) }
    var indoorExitDistance by remember { mutableIntStateOf(4) } // 4 meters
    var isScanningRoom by remember { mutableStateOf(true) }

    val orientation by rememberAROrientation()

    // Simulate initial room scanner calibration
    LaunchedEffect(arMode) {
        if (arMode == ARMode.INDOOR) {
            isScanningRoom = true
            delay(1200) // Realistic scanning delay
            // Auto-anchor to user's initial heading or forward sector
            indoorExitBearing = orientation.azimuth
            isScanningRoom = false
        }
    }

    val dynamicIndoorTargets = remember(indoorExitBearing, indoorExitDistance) {
        listOf(
            ARTarget(
                id = "room_detected_exit",
                name = "Room Exit Doorway",
                type = ARTargetType.EXIT,
                subtitle = "Spatial Egress • AI Identified",
                relativeBearingDegrees = indoorExitBearing,
                distanceMeters = indoorExitDistance
            ),
            ARTarget(
                id = "indoor_entry",
                name = "Main Entrance / Checkpoint",
                type = ARTargetType.ENTRY,
                subtitle = "Security & Scanner Corridor",
                relativeBearingDegrees = (indoorExitBearing + 180f) % 360f,
                distanceMeters = 16
            ),
            ARTarget(
                id = "indoor_haven",
                name = "Safe Haven: First-Aid Desk",
                type = ARTargetType.SAFE_HAVEN,
                subtitle = "Medical & Tourist Help Point",
                relativeBearingDegrees = (indoorExitBearing + 90f) % 360f,
                distanceMeters = 8
            )
        )
    }

    val targetsList = if (arMode == ARMode.OUTDOOR) {
        DwarkaARData.outdoorTargets
    } else {
        dynamicIndoorTargets
    }

    var selectedTarget by remember(arMode, targetsList) { mutableStateOf(targetsList.first()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (arMode == ARMode.OUTDOOR) "AR Safe Route • Dwarka" else "AR Indoor Room Exit Navigator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (arMode == ARMode.INDOOR) {
                                "AI Room Egress • Heading: ${orientation.azimuth.toInt()}°"
                            } else {
                                "Heading: ${orientation.azimuth.toInt()}° • Dwarka Safe Corridor"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00E676)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Indoor / Outdoor Mode Switch
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable {
                                arMode = if (arMode == ARMode.OUTDOOR) ARMode.INDOOR else ARMode.OUTDOOR
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = if (arMode == ARMode.OUTDOOR) Icons.Default.Place else Icons.Default.Home,
                                contentDescription = null,
                                tint = Color(0xFF00E676),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (arMode == ARMode.OUTDOOR) "Outdoor" else "Indoor",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.70f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Live Camera Preview Background
            ARCameraPreview()

            // 2. AR Green Path & 3D Spatial Doorway Overlay
            ARGreenPathOverlay(
                target = selectedTarget,
                currentAzimuth = orientation.azimuth,
                isHazardActive = isHazardSimulated,
                isIndoorMode = (arMode == ARMode.INDOOR)
            )

            // 3. Top AI Room Scanner Status Banner
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (arMode == ARMode.INDOOR) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A).copy(alpha = 0.90f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isScanningRoom) Color(0xFF29B6F6) else Color(0xFF00E676)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isScanningRoom) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF29B6F6)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Scanning room boundaries & doorways...",
                                        color = Color(0xFF29B6F6),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF00E676), CircleShape)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Exit Doorway Locked • Floor Path Active",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            // Quick Calibration Anchor Button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF00E676).copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E676)),
                                modifier = Modifier.clickable {
                                    // Instantly anchor the room door to whatever direction user is facing!
                                    indoorExitBearing = orientation.azimuth
                                    indoorExitDistance = 3
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Adjust,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "Anchor Here",
                                        color = Color(0xFF00E676),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Hazard Warning Alert Banner
                AnimatedVisibility(
                    visible = isHazardSimulated,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFD50000).copy(alpha = 0.92f),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (arMode == ARMode.INDOOR) "Indoor Corridor Hazard Detected" else "Hazard Geofence in Sector",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "AR Green Path diverted +35° into safe corridor.",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Bottom Controls (Target Selector & Hazard Toggle)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))
                        )
                    )
                    .padding(bottom = 20.dp, start = 16.dp, end = 16.dp, top = 20.dp)
            ) {
                // Hazard Avoidance Simulator Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (isHazardSimulated) Color(0xFFFF5252) else Color(0xFF00E676),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isHazardSimulated) "Hazard Avoidance: ACTIVE" else "Dynamic Hazard Avoidance",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Switch(
                        checked = isHazardSimulated,
                        onCheckedChange = { isHazardSimulated = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFFFF5252),
                            checkedTrackColor = Color(0xFFFF5252).copy(alpha = 0.4f)
                        )
                    )
                }

                // Destination Selector Carousel
                Text(
                    text = if (arMode == ARMode.INDOOR) "ROOM EGRESS DESTINATION:" else "DWARKA DESTINATION:",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    targetsList.forEach { target ->
                        val isSelected = target.id == selectedTarget.id
                        val badgeColor = when (target.type) {
                            ARTargetType.EXIT -> Color(0xFF00E676)
                            ARTargetType.ENTRY -> Color(0xFF29B6F6)
                            ARTargetType.SAFE_HAVEN -> Color(0xFFFFB300)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) badgeColor.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) badgeColor else Color.White.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.clickable { selectedTarget = target }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(badgeColor, CircleShape)
                                )
                                Spacer(Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = target.name,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${target.distanceMeters}m • ${target.subtitle.take(18)}",
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
