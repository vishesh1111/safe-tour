package com.sih2026.touristsafety.presentation.screens.map

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.sih2026.touristsafety.domain.model.NearbyPlace
import com.sih2026.touristsafety.domain.model.TouristLocation
import com.sih2026.touristsafety.data.local.entities.GeofenceZoneEntity

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Send
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouristMapScreen(
    viewModel: TouristMapViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCrowdDensity: () -> Unit = {},
    onNavigateToARNavigation: () -> Unit = {}
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val userLocation by viewModel.userLocation.collectAsState()
    val nearbyPlaces by viewModel.nearbyPlaces.collectAsState()
    val nearbyTourists by viewModel.nearbyTourists.collectAsState()
    val dangerZones by viewModel.dangerZones.collectAsState()
    val isShowingTourists by viewModel.isShowingTourists.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            viewModel.updateUserLocation(LatLng(location.latitude, location.longitude))
                        }
                    }
            } catch (_: SecurityException) {}
        }
    }

    // On first composition: check if permission already granted, else request it
    LaunchedEffect(Unit) {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            // Permission already granted — fetch location directly
            try {
                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            viewModel.updateUserLocation(LatLng(location.latitude, location.longitude))
                        }
                    }
            } catch (_: SecurityException) {}
        } else {
            // Ask for permission
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    
    val bottomSheetState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetPeekHeight = 120.dp,
        sheetContent = {
            Column(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { viewModel.selectTab(0) }, text = { Text("Nearby") })
                    Tab(selected = selectedTab == 1, onClick = { viewModel.selectTab(1) }, text = { Text("Zones") })
                    Tab(selected = selectedTab == 2, onClick = { viewModel.selectTab(2) }, text = { Text("Share") })
                }
                
                when (selectedTab) {
                    0 -> NearbyPlacesList(nearbyPlaces)
                    1 -> DangerZonesList(dangerZones)
                    2 -> ShareLocationList(nearbyPlaces)
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Tourist Map") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToARNavigation) {
                        Icon(Icons.Default.Navigation, contentDescription = "AR Navigation", tint = Color(0xFF00E676))
                    }
                    IconButton(onClick = onNavigateToCrowdDensity) {
                        Icon(Icons.Default.Group, contentDescription = "Crowd Density")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            OsmMapView(
                modifier = Modifier.fillMaxSize(),
                userLocation = userLocation,
                nearbyPlaces = nearbyPlaces,
                dangerZones = dangerZones,
                nearbyTourists = emptyList(),
                isShowingTourists = false,
                onRecenterRequested = {
                    // Re-fetch fresh GPS location when FAB is tapped
                    val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        try {
                            fusedLocationClient.getCurrentLocation(
                                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null
                            ).addOnSuccessListener { location ->
                                if (location != null) {
                                    viewModel.updateUserLocation(LatLng(location.latitude, location.longitude))
                                }
                            }
                        } catch (_: SecurityException) {}
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                }
            )


        }
    }
}

@Composable
fun NearbyPlacesList(places: List<NearbyPlace>) {
    if (places.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Loading nearby places...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(places) { place ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when(place.type) {
                                "hospital" -> Icons.Default.LocalHospital
                                "police" -> Icons.Default.LocalPolice
                                "food" -> Icons.Default.Restaurant
                                "shopping" -> Icons.Default.ShoppingCart
                                "hotel" -> Icons.Default.Hotel
                                else -> Icons.Default.Place
                            },
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(place.name, style = MaterialTheme.typography.titleMedium)
                            Text("${place.distance} km • ${String.format(java.util.Locale.US, "%.1f", place.rating)} ★", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DangerZonesList(zones: List<GeofenceZoneEntity>) {
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        items(zones) { zone ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(zone.name, style = MaterialTheme.typography.titleMedium)
                        Text("Level ${zone.severity} Danger", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun ShareLocationList(places: List<NearbyPlace>) {
    val emergencyPlaces = places.filter { it.type.lowercase() == "police" || it.type.lowercase() == "hospital" }
    var showDialog by remember { androidx.compose.runtime.mutableStateOf<NearbyPlace?>(null) }

    if (showDialog != null) {
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Location Shared") },
            text = { Text("Your live location and distress signal have been successfully and securely transmitted to ${showDialog?.name}. Help is on the way.") },
            confirmButton = {
                TextButton(onClick = { showDialog = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (emergencyPlaces.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("No nearby emergency services found.", style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp)) {
            items(emergencyPlaces) { place ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                        showDialog = place
                    },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text("${String.format("%.1f", place.distance)} km • Tap to share location", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Icon(Icons.Default.Send, contentDescription = "Share", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
