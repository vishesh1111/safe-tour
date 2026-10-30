package com.sih2026.touristsafety.presentation.screens.arnavigation

enum class ARMode {
    INDOOR,
    OUTDOOR
}

enum class ARTargetType {
    EXIT,
    ENTRY,
    SAFE_HAVEN
}

data class ARTarget(
    val id: String,
    val name: String,
    val type: ARTargetType,
    val subtitle: String,
    val relativeBearingDegrees: Float, // 0..360 deg relative to North
    val distanceMeters: Int,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isWithin1500m: Boolean = true // Enforces 1.5 km Proximity Constraint
)

object DwarkaARData {
    // Outdoor Prototype Locations - Centered around Dwarka Sector 21 / Vegas Mall Sector 14, New Delhi
    val outdoorTargets = listOf(
        ARTarget(
            id = "dwarka_exit_1",
            name = "Exit Gate 1 (Terminal Road)",
            type = ARTargetType.EXIT,
            subtitle = "Smart Exit • Dwarka Sec 21 Metro",
            relativeBearingDegrees = 45f,
            distanceMeters = 85,
            latitude = 28.5535,
            longitude = 77.0595
        ),
        ARTarget(
            id = "dwarka_exit_2",
            name = "Exit Gate 2 (Palam Vihar Rd)",
            type = ARTargetType.EXIT,
            subtitle = "Smart Exit • Proximity Verified",
            relativeBearingDegrees = 135f,
            distanceMeters = 190,
            latitude = 28.5510,
            longitude = 77.0570
        ),
        ARTarget(
            id = "dwarka_entry_main",
            name = "Main Entry (Gate A)",
            type = ARTargetType.ENTRY,
            subtitle = "Verified Entry Point • Security Check",
            relativeBearingDegrees = 320f,
            distanceMeters = 120,
            latitude = 28.5522,
            longitude = 77.0583
        ),
        ARTarget(
            id = "dwarka_police_haven",
            name = "Safe Haven: Sec 23 Police",
            type = ARTargetType.SAFE_HAVEN,
            subtitle = "Verified Police Post • 24/7 Responders",
            relativeBearingDegrees = 200f,
            distanceMeters = 420,
            latitude = 28.5630,
            longitude = 77.0510
        ),
        ARTarget(
            id = "dwarka_hospital_haven",
            name = "Safe Haven: Manipal Hospital",
            type = ARTargetType.SAFE_HAVEN,
            subtitle = "Emergency Trauma Care • Verified",
            relativeBearingDegrees = 75f,
            distanceMeters = 680,
            latitude = 28.5878,
            longitude = 77.0601
        )
    )

    // Indoor Prototype Locations - Perfect for testing inside demo room/hall/station
    val indoorTargets = listOf(
        ARTarget(
            id = "indoor_exit_1",
            name = "Emergency Exit (Gate 1)",
            type = ARTargetType.EXIT,
            subtitle = "North Corridor • Fire Safe Route",
            relativeBearingDegrees = 0f, // Directly North
            distanceMeters = 18
        ),
        ARTarget(
            id = "indoor_exit_2",
            name = "Exit Gate 2 (West Lobby)",
            type = ARTargetType.EXIT,
            subtitle = "Ground Level Exit • Wheelchair Access",
            relativeBearingDegrees = 270f, // West
            distanceMeters = 35
        ),
        ARTarget(
            id = "indoor_entry",
            name = "Main Entrance / Checkpoint",
            type = ARTargetType.ENTRY,
            subtitle = "Security & Baggage Scanner",
            relativeBearingDegrees = 180f, // South
            distanceMeters = 24
        ),
        ARTarget(
            id = "indoor_haven",
            name = "Safe Haven: First-Aid Desk",
            type = ARTargetType.SAFE_HAVEN,
            subtitle = "Medical & Tourist Help Point",
            relativeBearingDegrees = 90f, // East
            distanceMeters = 12
        )
    )
}
