package com.sih2026.touristsafety.presentation.screens.arnavigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

data class AROrientation(
    val azimuth: Float = 0f, // 0..360 degrees
    val pitch: Float = 0f     // Device tilt
)

@Composable
fun rememberAROrientation(): State<AROrientation> {
    val context = LocalContext.current
    val orientationState = remember { mutableStateOf(AROrientation()) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        val listener = object : SensorEventListener {
            private val rotationMatrix = FloatArray(9)
            private val orientationValues = FloatArray(3)

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    var azimuthDeg = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                    if (azimuthDeg < 0) azimuthDeg += 360f

                    val pitchDeg = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                    orientationState.value = AROrientation(azimuth = azimuthDeg, pitch = pitchDeg)
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    var azimuthDeg = event.values[0]
                    if (azimuthDeg < 0) azimuthDeg += 360f
                    orientationState.value = AROrientation(azimuth = azimuthDeg, pitch = event.values[1])
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager?.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    return orientationState
}
