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

class MainActivity : Activity(), SensorEventListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sensorManager: SensorManager
    private var heartRateSensor: Sensor? = null
    private val PERMISSION_REQUEST_CODE = 1
    private lateinit var heartRateTextView: TextView
    private lateinit var runButton: Button
    private lateinit var targetHeartRateTextView: TextView
    private lateinit var messageTextView: TextView
    private var age: Int = 0
    private var maxHeartRate: Int = 0
    private var currentPhase: Int = 0
    private val phases = arrayOf("Warm-up", "Running", "Rest", "Running", "Rest", "Running", "Rest")
    private val phaseDurations = intArrayOf(10, 10, 10, 10, 10, 10, 10)
    private val targetHeartRates = arrayOf(0.5, 0.85, 0.65, 0.85, 0.65, 0.85, 0.65)
    private var heartRate: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        heartRateTextView = binding.heartRateTextView
        targetHeartRateTextView = binding.targetHeartRateTextView
        runButton = binding.runButton
        messageTextView = binding.messageTextView

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)

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
            updateTargetHeartRate(heartRate)
        }
    }

    private fun updateTargetHeartRate(heartRate: Int) {
        val targetRate = (maxHeartRate * targetHeartRates[currentPhase]).toInt()

//        targetHeartRateTextView.text = "Target Heart Rate: $targetRate BPM"
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

    private fun startRunning() {
        age = 30 // You should implement age input logic
        maxHeartRate = calculateMaxHeartRate(age)
        currentPhase = 0
        val targetRate = (maxHeartRate * targetHeartRates[currentPhase]).toInt()
        targetHeartRateTextView.text = "Target Heart Rate: $targetRate BPM"
        val curruntTime = System.currentTimeMillis()
        runPhase(currentPhase, curruntTime)
        runButton.text = "Stop Running" // 버튼 레이블 변경
        isRunning = true // 달리기 중 상태로 설정
    }

    private fun stopRunning() {
        // 달리기를 멈추는 동작 추가 (필요한 로직 추가)
        // 예를 들어, 현재 단계 실행을 중지하고 버튼 레이블을 다시 "Start Running"으로 설정
        handler.removeCallbacksAndMessages(null) // 핸들러에서 예약된 작업 모두 제거
        runButton.text = "Start Running" // 버튼 레이블 변경
        isRunning = false // 달리기 종료 상태로 설정
    }

    private fun calculateMaxHeartRate(age: Int): Int {
        return 220 - age
    }

    private fun runPhase(phase: Int, startTimeMillis: Long) {
        if (phase < phases.size) {
            val targetRate = (maxHeartRate * targetHeartRates[phase]).toInt()
            Log.d("YourTag", "Target Rate: $targetRate BPM")

            if (heartRate < targetRate * 0.9) {
                messageTextView.text = "Run faster!"
            } else if (heartRate > targetRate * 1.1) {
                messageTextView.text = "Slow down!"
            } else {
                messageTextView.text = ""
            }

            val currentTimeMillis = System.currentTimeMillis()
            val elapsedTime = currentTimeMillis - startTimeMillis
            val phaseDurationMillis = phaseDurations[phase] * 1000L

            val currentTimeTextView = findViewById<TextView>(R.id.currentTimeTextView)
            val elapsedTimeTextView = findViewById<TextView>(R.id.elapsedTimeTextView)

            currentTimeTextView.text = "Current Time: $currentTimeMillis"
            elapsedTimeTextView.text = "Elapsed Time: $elapsedTime ms"

            if (elapsedTime >= phaseDurationMillis) {
                // 현재 단계의 시간이 다 지났으므로 다음 단계로 진행
                val nextPhase = (phase + 1)
                val targetRate = (maxHeartRate * targetHeartRates[nextPhase]).toInt()
                Log.d("YourTag", "Next Phase: $nextPhase, Target Rate: $targetRate BPM")
                targetHeartRateTextView.text = "Target Heart Rate: $targetRate BPM"

                // 다음 페이즈로 넘어갈 때 현재 페이즈가 중복으로 호출되지 않도록 수정
                if (nextPhase != phase) {
                    runPhase(nextPhase, System.currentTimeMillis())
                }
            } else {
                // 아직 단계의 시간이 다 지나지 않았으므로 기다립니다.
                // 예를 들어, 다음 확인을 1초마다 하도록 설정할 수 있습니다.
                handler.postDelayed({
                    Log.d("YourTag", "Delayed Phase: $phase")
                    runPhase(phase, startTimeMillis) // 현재 단계 시작 시간을 그대로 유지
                }, 1000L)
            }

            if (phase == phases.size - 1 && elapsedTime >= phaseDurationMillis) {
                // 달리기가 모든 단계를 완료했을 때
                stopRunning()
            }
        }
    }


}