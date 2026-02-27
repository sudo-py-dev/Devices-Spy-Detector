package com.spydetect.edapps.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.google.android.material.color.MaterialColors
import kotlin.math.cos
import kotlin.math.sin

class RadarView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
  View(context, attrs, defStyleAttr) {

  companion object {
    private const val ANIMATION_START_ANGLE = 0f
    private const val ANIMATION_END_ANGLE = 360f
    private const val ANIMATION_DURATION_MS = 2000L
    private const val RING_ALPHA = 50
    private const val SWEEP_ALPHA = 100
    private const val RADIUS_SCALE = 0.9f
    private const val INNER_RING_SCALE_1 = 0.66f
    private const val INNER_RING_SCALE_2 = 0.33f
    private const val BLIP_RADIUS_SMALL = 8f
    private const val BLIP_RADIUS_LARGE = 16f
    private const val BLIP_ALPHA_FULL = 255
    private const val BLIP_ALPHA_DIM = 100
    private const val SWEEP_ANGLE_START = -45f
    private const val SWEEP_ANGLE_END = 45f
    private const val DETECTION_THRESHOLD = 30f
    private const val DETECTION_THRESHOLD_UPPER = 330f
    private const val BLIP_1_DISTANCE = 0.7f
    private const val BLIP_2_DISTANCE = 0.4f
    private const val BLIP_1_ANGLE = 45f
    private const val BLIP_2_ANGLE = 210f
    private const val BLIP_1_ALPHA = 0.8f
    private const val BLIP_2_ALPHA = 0.5f
  }

  private val ringPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 2f
    }

  private val sweepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

  private val pointPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
      color = Color.CYAN
    }

  private var rotationAngle = 0f
  private var isScanning = false
  private var animator: ValueAnimator? = null

  private val primaryColor by lazy {
    MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
  }

  init {
    ringPaint.color = primaryColor
    sweepPaint.color = primaryColor
  }

  fun setScanning(scanning: Boolean) {
    if (isScanning == scanning) return
    isScanning = scanning
    if (scanning) {
      startAnimation()
    } else {
      stopAnimation()
    }
  }

  private fun startAnimation() {
    animator?.cancel()
    animator =
      ValueAnimator.ofFloat(ANIMATION_START_ANGLE, ANIMATION_END_ANGLE).apply {
        duration = ANIMATION_DURATION_MS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
          rotationAngle = it.animatedValue as Float
          invalidate()
        }
        start()
      }
  }

  private fun stopAnimation() {
    animator?.cancel()
    animator = null
    rotationAngle = 0f
    invalidate()
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    val centerX = width / 2f
    val centerY = height / 2f
    val radius = (width.coerceAtMost(height) / 2f) * RADIUS_SCALE

    // Draw rings
    ringPaint.alpha = RING_ALPHA
    canvas.drawCircle(centerX, centerY, radius, ringPaint)
    canvas.drawCircle(centerX, centerY, radius * INNER_RING_SCALE_1, ringPaint)
    canvas.drawCircle(centerX, centerY, radius * INNER_RING_SCALE_2, ringPaint)

    // Draw Crosshair
    canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, ringPaint)
    canvas.drawLine(centerX, centerY - radius, centerX, centerY + radius, ringPaint)

    if (isScanning) {

      canvas.save()
      canvas.rotate(rotationAngle, centerX, centerY)

      sweepPaint.alpha = SWEEP_ALPHA
      canvas.drawArc(
        centerX - radius,
        centerY - radius,
        centerX + radius,
        centerY + radius,
        SWEEP_ANGLE_START,
        SWEEP_ANGLE_END,
        true,
        sweepPaint
      )

      canvas.restore()

      // Draw pseudo-detections (blips)
      drawBlip(
        BlipParams(canvas, centerX, centerY, radius * BLIP_1_DISTANCE, BLIP_1_ANGLE, BLIP_1_ALPHA)
      )
      drawBlip(
        BlipParams(canvas, centerX, centerY, radius * BLIP_2_DISTANCE, BLIP_2_ANGLE, BLIP_2_ALPHA)
      )
    }
  }

  private data class BlipParams(
    val canvas: Canvas,
    val centerX: Float,
    val centerY: Float,
    val radius: Float,
    val angle: Float,
    val alphaMultiplier: Float
  )

  private fun drawBlip(params: BlipParams) {
    val rad = Math.toRadians(params.angle.toDouble()).toFloat()
    val x = params.centerX + params.radius * cos(rad)
    val y = params.centerY + params.radius * sin(rad)

    // Flicker effect based on rotation
    val dist = Math.abs(rotationAngle - params.angle)
    if (dist < DETECTION_THRESHOLD || dist > DETECTION_THRESHOLD_UPPER) {
      pointPaint.alpha = (BLIP_ALPHA_FULL * params.alphaMultiplier).toInt()
      params.canvas.drawCircle(x, y, BLIP_RADIUS_SMALL, pointPaint)

      pointPaint.alpha = (BLIP_ALPHA_DIM * params.alphaMultiplier).toInt()
      params.canvas.drawCircle(x, y, BLIP_RADIUS_LARGE, pointPaint)
    }
  }
}
