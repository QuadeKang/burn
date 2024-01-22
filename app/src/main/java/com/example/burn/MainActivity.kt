package com.example.burn

import android.app.Activity
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.burn.databinding.ActivityMainBinding

class MainActivity : Activity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var heartRateTextView: TextView
    private lateinit var runButton: Button
    private var age: Int = 0
    private var maxHeartRate: Int = 0
    private var currentPhase: Int = 0
    private val phases = arrayOf("Warm-up", "Running", "Rest")
    private val phaseDurations = intArrayOf(30, 120, 30)
    private val targetHeartRates = arrayOf(0.5, 0.85, 0.65) // 50%, 85%, 65% of max heart rate
    private lateinit var targetHeartRateTextView: TextView
    private lateinit var messageTextView: TextView
    private var heartRate: Int = 0 // Declare heartRate at the class level
    private val handler = Handler(Looper.getMainLooper()) // Declare handler at the class level

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        heartRateTextView = findViewById(R.id.heartRateTextView)
        messageTextView = findViewById(R.id.messageTextView)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        heartRateTextView = findViewById(R.id.heartRateTextView)
        targetHeartRateTextView = findViewById(R.id.targetHeartRateTextView)
        runButton = findViewById(R.id.runButton)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.BODY_SENSORS), PERMISSION_REQUEST_CODE)
        }

        runButton.setOnClickListener {
            startRunning()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start heart rate monitoring
                sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                // Permission denied, handle accordingly
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            val heartRate = event.values[0].toInt()
            heartRateTextView.text = "Heart Rate: $heartRate BPM"
            updateTargetHeartRate(heartRate)
        }
    }
    private fun updateTargetHeartRate(heartRate: Int) {
        // Calculate target heart rate for the current phase
        val targetRate = (maxHeartRate * targetHeartRates[currentPhase]).toInt()

        // Display target heart rate
        targetHeartRateTextView.text = "Target Heart Rate: $targetRate BPM"

        // Check if the current heart rate is within the target range
        if (heartRate < targetRate * 0.9) {
            // Display a message like "Run faster!"
            messageTextView.text = "Run faster!"
        } else if (heartRate > targetRate * 1.1) {
            // Display a message like "Slow down!"
            messageTextView.text = "Slow down!"
        } else {
            // If heart rate is within the target range, clear the message
            messageTextView.text = ""
        }

        // Schedule the next phase...
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun startRunning() {
        // Get user's age and calculate max heart rate
        // You should add UI elements and logic to input the age here
        age = 30 // For example, set age to 30
        maxHeartRate = calculateMaxHeartRate(age)

        // Start the running phase
        currentPhase = 0
        runPhase(currentPhase)
    }

    private fun calculateMaxHeartRate(age: Int): Int {
        return 220 - age
    }

    private fun runPhase(phase: Int) {
        if (phase < phases.size) {
            // Display current phase
            targetHeartRateTextView.text = "Phase: ${phases[phase]}"

            // Calculate target heart rate for the current phase
            val targetRate = (maxHeartRate * targetHeartRates[phase]).toInt()

            // Display target heart rate
            targetHeartRateTextView.text = "Target Heart Rate: $targetRate BPM"

            // Check if the current heart rate is within the target range
            if (heartRate < targetRate * 0.9) {
                // Display a message like "Run faster!"
                messageTextView.text = "Run faster!"
            } else if (heartRate > targetRate * 1.1) {
                // Display a message like "Slow down!"
                messageTextView.text = "Slow down!"
            } else {
                // If heart rate is within the target range, clear the message
                messageTextView.text = ""
            }

            // Schedule the next phase after the current phase duration
            val nextPhase = (phase + 1) % phases.size
            val nextPhaseDuration = phaseDurations[nextPhase] * 1000L // Convert to milliseconds
            handler.postDelayed({
                runPhase(nextPhase)
            }, nextPhaseDuration)
        } else {
            // Running is complete, you can add any necessary actions here
            // For example, display a message or stop monitoring heart rate
            targetHeartRateTextView.text = "Running Complete"
            sensorManager.unregisterListener(this)
        }
    }
}