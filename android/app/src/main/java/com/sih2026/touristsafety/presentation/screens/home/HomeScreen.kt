package com.sih2026.touristsafety.presentation.screens.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.sih2026.touristsafety.presentation.components.BottomNavBar
import com.sih2026.touristsafety.presentation.components.SOSButton
import com.sih2026.touristsafety.presentation.components.bounceClick
import com.sih2026.touristsafety.presentation.navigation.Screen

import androidx.hilt.navigation.compose.hiltViewModel
import com.sih2026.touristsafety.presentation.screens.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    // =========================================================================
    // Permission Handling & System Service Checks
    // =========================================================================
    var showLocationRationale by remember { mutableStateOf(false) }
    var showMicRationale by remember { mutableStateOf(false) }
    var isLocationServiceEnabled by remember { mutableStateOf(true) }
    
    val isHazardActive by viewModel.isHazardActive.collectAsState()
    
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val requiredPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locationGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val micGranted = results[Manifest.permission.RECORD_AUDIO] == true

        showLocationRationale = !locationGranted
        showMicRationale = !micGranted
    }

    // Re-check permissions and services every time the screen resumes
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                val locationGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                
                if (!locationGranted || !micGranted) {
                    permissionLauncher.launch(requiredPermissions)
                }

                val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                isLocationServiceEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                                           locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 1. Check if GPS is off at system level
    if (!isLocationServiceEnabled) {
        AlertDialog(
            onDismissRequest = { /* Force action */ },
            icon = { Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Location Services Disabled") },
            text = { Text("Your device's location (GPS) is turned off. TouristSafety requires location services to keep you safe and provide accurate maps. Please enable it in Settings.") },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }) { Text("Turn On") }
            },
            dismissButton = {
                TextButton(onClick = { isLocationServiceEnabled = true }) { Text("Cancel") }
            }
        )
    }

    // 2. Permission rationales (if GPS is on but permission denied)
    if (showLocationRationale && isLocationServiceEnabled) {
        AlertDialog(
            onDismissRequest = { showLocationRationale = false },
            icon = { Icon(Icons.Default.LocationOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Location Permission Required") },
            text = {
                Text(
                    "Location access is critical for your safety:\n\n" +
                    "• SOS alerts include your GPS coordinates\n" +
                    "• Geofencing warns you about danger zones\n" +
                    "• Inactivity monitoring detects if you're stationary too long\n\n" +
                    "Please allow location permissions in App Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLocationRationale = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationRationale = false }) { Text("Later") }
            }
        )
    }

    if (showMicRationale) {
        AlertDialog(
            onDismissRequest = { showMicRationale = false },
            icon = { Icon(Icons.Default.MicOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Microphone Access Required") },
            text = {
                Text(
                    "Microphone access enables safety features:\n\n" +
                    "• SOS audio recording captures evidence\n" +
                    "• Scream detection triggers automatic alerts\n\n" +
                    "Please enable microphone permissions, and ensure the global microphone toggle is on in your device's Quick Settings.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showMicRationale = false
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = { showMicRationale = false }) { Text("Later") }
            }
        )
    }

    if (isHazardActive) {
        AlertDialog(
            onDismissRequest = { /* Force action */ },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text("Hazard Alarm is Active") },
            text = { Text("The physical siren and flashlight SOS are currently active. Tap below to turn them off.") },
            confirmButton = {
                Button(
                    onClick = { 
                        val activity = context as? android.app.Activity
                        viewModel.toggleHazard(activity?.window?.attributes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("Turn Off Alarm")
                }
            }
        )
    }

    var showEmergencyDialog by remember { mutableStateOf(false) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }
    
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingCallNumber?.let { num ->
                val intent = android.content.Intent(android.content.Intent.ACTION_CALL, android.net.Uri.parse("tel:$num"))
                context.startActivity(intent)
            }
        } else {
            pendingCallNumber?.let { num ->
                val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$num"))
                context.startActivity(intent)
            }
        }
        pendingCallNumber = null
        showEmergencyDialog = false
    }

    if (showEmergencyDialog) {
        EmergencyNumbersDialog(
            onDismiss = { showEmergencyDialog = false },
            onCall = { number ->
                val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                if (hasPermission) {
                    val intent = android.content.Intent(android.content.Intent.ACTION_CALL, android.net.Uri.parse("tel:$number"))
                    context.startActivity(intent)
                    showEmergencyDialog = false
                } else {
                    pendingCallNumber = number
                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                }
            }
        )
    }


    var showHazardCountdown by remember { mutableStateOf(false) }
    var countdownTicks by remember { mutableStateOf(3) }

    if (showHazardCountdown) {
        LaunchedEffect(countdownTicks) {
            if (countdownTicks > 0) {
                kotlinx.coroutines.delay(1000)
                countdownTicks--
            } else {
                showHazardCountdown = false
                val activity = context as? android.app.Activity
                viewModel.toggleHazard(activity?.window?.attributes)
                
                val hasCallPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.CALL_PHONE
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                if (hasCallPermission) {
                    try {
                        val callIntent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                            data = android.net.Uri.parse("tel:112")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(callIntent)
                    } catch (e: SecurityException) {
                        val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                            data = android.net.Uri.parse("tel:112")
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(dialIntent)
                    }
                } else {
                    val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse("tel:112")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showHazardCountdown = false },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F)) },
            title = { Text("Activating Hazard Alarm") },
            text = { 
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Starting loud siren and flashlight in:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "$countdownTicks",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD32F2F)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHazardCountdown = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // =========================================================================
    // Main UI
    // =========================================================================
    Scaffold(
        bottomBar = {
            BottomNavBar(
                currentRoute = Screen.Home.route,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onHazardClick = {
                    if (!viewModel.isHazardActive.value) {
                        countdownTicks = 3
                        showHazardCountdown = true
                    } else {
                        val activity = context as? android.app.Activity
                        viewModel.toggleHazard(activity?.window?.attributes)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showEmergencyDialog = true },
                containerColor = Color(0xFFD32F2F),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Call, contentDescription = "Emergency Call", tint = Color.White)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Welcome Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Hello, Tourist",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Stay safe and explore",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }
                
                IconButton(onClick = { navController.navigate(Screen.EProfile.route) }) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // SOS Button Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                SOSButton(
                    onSOSTriggered = { navController.navigate(Screen.SOS.route) },
                    modifier = Modifier.size(160.dp)
                )
            }

            // Quick Actions Grid
            val quickActions = listOf(
                QuickActionItem("Emergency", Icons.Default.Phone, Screen.EmergencyContacts.route),
                QuickActionItem("AR Safe Route", Icons.Default.Navigation, Screen.ARNavigation.route),
                QuickActionItem("Report", Icons.Default.CameraAlt, Screen.IncidentReport.route),
                QuickActionItem("Assistant", Icons.Default.Chat, Screen.Chatbot.route),
                QuickActionItem("Connect", Icons.Default.People, Screen.Connect.route),
                QuickActionItem("Start Journey", Icons.Default.Security, Screen.StartJourney.route),
                QuickActionItem("Translate", Icons.Default.Translate, Screen.Translator.route),
                QuickActionItem("Track", Icons.Default.Map, Screen.Track.route),
                QuickActionItem("Nearby SOS", Icons.Default.Bluetooth, Screen.NearbySOS.route)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                gridItems(quickActions) { action ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .bounceClick { navController.navigate(action.route) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.title,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = action.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

        }
    }
}

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun EmergencyNumbersDialog(
    onDismiss: () -> Unit,
    onCall: (String) -> Unit
) {
    val emergencyNumbers = listOf(
        "112" to "All in one Emergency Number",
        "100" to "Police",
        "101" to "Fire",
        "102" to "Ambulance",
        "103" to "Traffic Police",
        "104" to "State level Helpline for Health",
        "108" to "Disaster Management / Medical",
        "1072" to "Train accident",
        "1090" to "Anti terror Helpline/Alert All India",
        "1096" to "Natural Disaster Control Room",
        "1099" to "Central Accident and Trauma Services"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Emergency Helplines",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn {
                items(emergencyNumbers) { (number, description) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onCall(number) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(description, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
