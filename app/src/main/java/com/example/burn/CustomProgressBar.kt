package com.example.burn
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CustomProgressBar(context: Context, attrs: AttributeSet) : View(context, attrs) {
    private var progress: Int = 0
    private var maxProgress: Int = 100

    private val backgroundColor: Int = Color.GRAY
    private val progressColor: Int = Color.BLUE

    private val backgroundPaint = Paint()
    private val progressPaint = Paint()

    init {
        backgroundPaint.color = backgroundColor
        progressPaint.color = progressColor
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width.toFloat()
        val height = height.toFloat()

        // Draw background
        canvas?.drawRect(0f, 0f, width, height, backgroundPaint)

        // Draw progress
        val progressWidth = (progress.toFloat() / maxProgress) * width
        canvas?.drawRect(0f, 0f, progressWidth, height, progressPaint)
    }

    fun setProgress(progress: Int) {
        if (progress in 0..maxProgress) {
            this.progress = progress
            invalidate()
        }
    }

    fun resetProgress() {
        // Set the progress back to 0%
        progress = 0
        // Update the progress bar
        invalidate()
    }
    fun setMaxProgress(maxProgress: Int) {
        this.maxProgress = maxProgress
        invalidate()
    }

    fun animateProgress(seconds: Int, onAnimationComplete: () -> Unit) {
        val targetProgress = maxProgress
        val animationDurationMillis = seconds * 1000L
        val updateIntervalMillis = 1000L // Update the progress every 1 second

        val animationStartTime = System.currentTimeMillis()

        val progressUpdateRunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - animationStartTime

                if (elapsedTime < animationDurationMillis) {
                    // Calculate progress based on elapsed time and duration
                    val animatedProgress = (elapsedTime.toFloat() / animationDurationMillis * targetProgress).toInt()
                    setProgress(animatedProgress)

                    // Schedule the next progress update
                    handler?.postDelayed(this, updateIntervalMillis)
                } else {
                    // Animation is complete
                    setProgress(targetProgress)
                    onAnimationComplete.invoke()
                }
            }
        }

        // Start the progress update runnable
        handler?.post(progressUpdateRunnable)
    }
}
