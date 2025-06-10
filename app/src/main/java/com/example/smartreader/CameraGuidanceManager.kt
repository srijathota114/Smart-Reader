package com.example.smartreader

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs

class CameraGuidanceManager(
    context: Context,
    private val onGuidance: (String) -> Unit
) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var lastGuidance = 0L
    private val GUIDANCE_DELAY = 2000L // 2 seconds between guidance messages
    
    fun startMonitoring() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }
    
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastGuidance > GUIDANCE_DELAY) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                when {
                    abs(x) > 3 -> onGuidance("Please hold the phone more level")
                    abs(y) > 8 -> onGuidance("Please hold the phone more upright")
                    z < 5 -> onGuidance("Please point the camera towards the text")
                }
                
                lastGuidance = currentTime
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
} 