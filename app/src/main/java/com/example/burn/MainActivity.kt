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
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.burn.databinding.ActivityMainBinding
import android.media.MediaPlayer
import kotlin.random.Random
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide


class MainActivity : Activity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var heartRateTextView: TextView
    private lateinit var runButton: Button
    private lateinit var targetHeartRateTextView: TextView
    private lateinit var messageTextView: TextView
    private lateinit var phaseStateView: TextView
    private var age: Int = 0
    private var maxHeartRate: Int = 0
    private var currentPhase: Int = 0
    private val phases = arrayOf("Warm-up", "Running", "Rest", "Running", "Rest", "Running", "Rest")
    private val phaseDurations = intArrayOf(5, 5, 5, 5, 5, 5, 5)
    private val targetHeartRates = arrayOf(0.5, 0.85, 0.65, 0.85, 0.65, 0.85, 0.65)
    private var heartRate: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private lateinit var customProgressBar: CustomProgressBar
    private val musicProbability = 0.05
//    private val runningTime = 480
    private val runningTime = 35
    private val gifHandler = Handler()
    private lateinit var scrollViewBackground: ImageView

    // Declare MediaPlayer variable
    private var mediaPlayer: MediaPlayer? = null

    // List of music resource IDs
    private val musicFiles = listOf(R.raw.run1, R.raw.run2, R.raw.run3, R.raw.run4)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        heartRateTextView = binding.heartRateTextView
        targetHeartRateTextView = binding.targetHeartRateTextView
        runButton = binding.runButton
        messageTextView = binding.messageTextView
        customProgressBar = binding.customProgressBar  // Reference to the custom progress bar
        phaseStateView = binding.phaseStatusTextView

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        scrollViewBackground = findViewById(R.id.gifTextureView)


        mediaPlayer = MediaPlayer.create(this, R.raw.run1)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.BODY_SENSORS), PERMISSION_REQUEST_CODE)
        }

        runButton.setOnClickListener {
            if (isRunning) {
                stopRunning()
            } else {
                startRunning()
            }
        }

        loadAndLoopGif()

        // Start the initial GIF loop
        startGifLoop()
    }

    private fun loadAndLoopGif() {
        Glide.with(this)
                .asGif()
            .load(R.raw.runbackground) // Replace with your GIF resource
            .into(scrollViewBackground)
    }

    private fun startGifLoop() {
        gifHandler.postDelayed(object : Runnable {
            override fun run() {
                // Restart the GIF animation
                scrollViewBackground.visibility = View.GONE
                scrollViewBackground.visibility = View.VISIBLE
                gifHandler.postDelayed(this, 3000) // Restart the GIF every 3 seconds
            }
        }, 3000)
    }

    override fun onDestroy() {
        // Remove the GIF looping callback when the activity is destroyed
        gifHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sensorManager.registerListener(this, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
            } else {
                // Handle permission denied
            }
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
            heartRate = event.values[0].toInt()
            heartRateTextView.text = "Heart Rate: $heartRate BPM"
//            updateTargetHeartRate(heartRate)
            customProgressBar.setProgress(heartRate)  // Update custom progress bar
        }
    }

    private fun updateTargetHeartRate(heartRate: Int) {
        val targetRate = (maxHeartRate * targetHeartRates[currentPhase]).toInt()

        Log.d("updateTargetHeartRate", "Target Rate: $targetRate BPM")
        if (heartRate < targetRate * 0.9) {
            messageTextView.text = "Run faster!"
        } else if (heartRate > targetRate * 1.1) {
            messageTextView.text = "Slow down!"
        } else {
            messageTextView.text = ""
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
    }

    private fun playMusic() {
        // Stop any currently playing music
        stopMusic()

        // Randomly select a music file from the list
        val randomIndex = Random.nextInt(musicFiles.size)
        val musicResource = musicFiles[randomIndex]

        // Initialize the MediaPlayer with the selected music file
        mediaPlayer = MediaPlayer.create(this, musicResource)
        mediaPlayer?.isLooping = false // Set looping if needed

        // Start playing the selected music
        mediaPlayer?.start()
    }


    // Function to stop music (if needed)
    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun stopRunning() {
        // Add logic to stop the running phase if needed

        // Change the button label and running state
        runButton.text = "Start Running"
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        // Optionally, show a message to indicate that the run is complete
        messageTextView.text = "Run Completed"

        // Allow the user to start another run
        runButton.isEnabled = true
        currentPhase = 0
        customProgressBar.resetProgress()  // Reset custom progress bar
    }

    private fun startRunning() {
        // Add logic to prepare for a new run, if needed

        age = 30 // You should implement age input logic
        maxHeartRate = calculateMaxHeartRate(age)
        currentPhase = 0
        val targetRate = (maxHeartRate * targetHeartRates[currentPhase]).toInt()
        targetHeartRateTextView.text = "Target Heart Rate: $targetRate BPM"

        // Start the running phase
        val currentTime = System.currentTimeMillis()

        // Change the button label and running state
        runButton.text = "Stop Running"
        isRunning = true

        // Disable the button to prevent starting another run while one is in progress
        runButton.isEnabled = false

        customProgressBar.setMaxProgress(runningTime)

        // Animate the progress bar
        customProgressBar.animateProgress(runningTime) {
            // This callback is called when the animation is complete
            // Change the button label and running state
            runButton.text = "Stop Running"
            isRunning = true

            // Disable the button to prevent starting another run while one is in progress
            runButton.isEnabled = false

            // Start the running phase after progress animation
            runPhase(currentPhase, currentTime)
        }
    }

    private fun calculateMaxHeartRate(age: Int): Int {
        return 220 - age
    }

    private fun runPhase(phase: Int, startTimeMillis: Long) {
        if (phase >= phases.size) {
            // All phases are completed, exit the function
            phaseStateView.text = "NO RUNNING"
            stopRunning()
            return
        }

        currentPhase = phase

        val targetRate = (maxHeartRate * targetHeartRates[phase]).toInt()
        Log.d("YourTag", "Target Rate: $targetRate BPM")
        targetHeartRateTextView.text = "Target Heart Rate: $targetRate BPM"
        updateTargetHeartRate(targetRate)

        // Inside the updateTargetHeartRate function
        if (heartRate < targetRate * 0.9) {
            messageTextView.text = "Run faster!"

            // Check if music should be played
            if (Random.nextDouble(0.0, 1.0) <= musicProbability) {
                // Play music here
                playMusic()
            }
        } else if (heartRate > targetRate * 1.1) {
            messageTextView.text = "Slow down!"
        } else {
            messageTextView.text = ""
        }

        val currentTimeMillis = System.currentTimeMillis()
        val elapsedTime = currentTimeMillis - startTimeMillis
        val phaseDurationMillis = phaseDurations[phase] * 1000L

        if (elapsedTime >= phaseDurationMillis) {
            // Current phase time has elapsed, proceed to the next phase
            val nextPhase = phase + 1
            Log.d("nextPhase", "Next Phase: $nextPhase")

            if (nextPhase >= phases.size) {
                // All phases are completed, exit the function
                phaseStateView.text = "NO RUNNING"
                stopRunning()
                return
            }

            val nextTargetRate = (maxHeartRate * targetHeartRates[nextPhase]).toInt()
            Log.d("YourTag", "Next Phase: $nextPhase, Target Rate: $nextTargetRate BPM")
            targetHeartRateTextView.text = "Target Heart Rate: $nextTargetRate BPM"

            // Proceed to the next phase
            phaseStateView.text = "Phase : ${phases[nextPhase]}"
            runPhase(nextPhase, System.currentTimeMillis())
        } else {
            // Phase time has not elapsed, wait and continue checking
            handler.postDelayed({
                Log.d("YourTag", "Delayed Phase: $phase")
                runPhase(phase, startTimeMillis) // Continue with the current phase
            }, 1000L)
        }
    }
}