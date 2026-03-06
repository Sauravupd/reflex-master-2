package com.sauravupd.twitchbubbles

import android.graphics.*

open class Bubble(
    var x: Float,
    var y: Float,
    var radius: Float,
    val maxRadius: Float,
    val shrinkRate: Float,
    val color: Int
) {
    var isAlive: Boolean = true
    var spawnTime: Float = 0f
    var age: Float = 0f

    // The ring starts at this radius and shrinks toward the bubble
    var ringRadius: Float = maxRadius * 1.45f
    val ringStartRadius: Float = maxRadius * 1.45f
    val ringEndRadius: Float = maxRadius * 1.05f  // gap between ring and bubble

    // Paint objects
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
    }

    open fun update(dt: Float) {
        age += dt
        radius -= shrinkRate * dt

        // Shrink the ring toward the bubble edge
        val totalLife = (maxRadius - 8f) / shrinkRate  // total time bubble lives
        val lifeProgress = (age / totalLife).coerceIn(0f, 1f)
        ringRadius = ringStartRadius + (ringEndRadius - ringRadius) * lifeProgress
        // Actually: linear interpolation
        ringRadius = ringStartRadius * (1f - lifeProgress) + ringEndRadius * lifeProgress

        if (radius <= 8f) {
            isAlive = false
        }
    }

    open fun draw(canvas: Canvas) {
        if (!isAlive) return

        val alpha = ((radius / maxRadius) * 255).toInt().coerceIn(60, 255)

        // Outer glow (subtle)
        glowPaint.shader = RadialGradient(
            x, y, radius * 1.6f,
            intArrayOf(Color.argb(30, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius * 1.6f, glowPaint)

        // Draw the shrinking timer ring (sharper look)
        drawTimerRing(canvas)

        // Main bubble fill (Glassy/Deep Gradient)
        val lighterColor = lightenColor(color, 0.5f)
        val darkerColor = darkenColor(color, 0.4f)
        
        // Inner depth gradient
        fillPaint.shader = RadialGradient(
            x + radius * 0.2f, y + radius * 0.2f, radius * 1.0f,
            intArrayOf(color, color, darkerColor),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        fillPaint.alpha = alpha
        canvas.drawCircle(x, y, radius, fillPaint)

        // Inner "Glassy" Highlight / Flare
        highlightPaint.shader = RadialGradient(
            x - radius * 0.35f, y - radius * 0.35f, radius * 0.7f,
            intArrayOf(Color.argb(200, 255, 255, 255), Color.argb(40, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius * 0.9f, highlightPaint)

        // Sharp Inner Rim
        outlinePaint.color = Color.argb(120, 255, 255, 255)
        outlinePaint.strokeWidth = 2f
        canvas.drawCircle(x, y, radius - 2f, outlinePaint)
    }

    private fun drawTimerRing(canvas: Canvas) {
        val totalLife = (maxRadius - 8f) / shrinkRate
        val lifeProgress = (age / totalLife).coerceIn(0f, 1f)

        val sweepAngle = 360f * (1f - lifeProgress)
        if (sweepAngle <= 0f) return

        val ringColor = when {
            lifeProgress < 0.4f -> interpolateColor(Color.parseColor("#00FF88"), Color.parseColor("#FFD93D"), lifeProgress / 0.4f)
            lifeProgress < 0.7f -> interpolateColor(Color.parseColor("#FFD93D"), Color.parseColor("#FF6B35"), (lifeProgress - 0.4f) / 0.3f)
            else -> interpolateColor(Color.parseColor("#FF6B35"), Color.parseColor("#FF4757"), (lifeProgress - 0.7f) / 0.3f)
        }

        ringPaint.color = ringColor
        ringPaint.alpha = 240
        ringPaint.strokeWidth = 3f

        val rect = RectF(x - ringRadius, y - ringRadius, x + ringRadius, y + ringRadius)
        canvas.drawArc(rect, -90f, sweepAngle, false, ringPaint)
    }

    private fun interpolateColor(from: Int, to: Int, fraction: Float): Int {
        val f = fraction.coerceIn(0f, 1f)
        val r = (Color.red(from) + (Color.red(to) - Color.red(from)) * f).toInt()
        val g = (Color.green(from) + (Color.green(to) - Color.green(from)) * f).toInt()
        val b = (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * f).toInt()
        return Color.rgb(r, g, b)
    }

    fun contains(tx: Float, ty: Float): Boolean {
        val dx = tx - x
        val dy = ty - y
        val hitRadius = radius * 1.35f
        return dx * dx + dy * dy <= hitRadius * hitRadius
    }

    companion object {
        fun lightenColor(color: Int, factor: Float): Int {
            val r = (Color.red(color) + (255 - Color.red(color)) * factor).toInt().coerceIn(0, 255)
            val g = (Color.green(color) + (255 - Color.green(color)) * factor).toInt().coerceIn(0, 255)
            val b = (Color.blue(color) + (255 - Color.blue(color)) * factor).toInt().coerceIn(0, 255)
            return Color.argb(Color.alpha(color), r, g, b)
        }

        fun darkenColor(color: Int, factor: Float): Int {
            val r = (Color.red(color) * (1 - factor)).toInt().coerceIn(0, 255)
            val g = (Color.green(color) * (1 - factor)).toInt().coerceIn(0, 255)
            val b = (Color.blue(color) * (1 - factor)).toInt().coerceIn(0, 255)
            return Color.argb(Color.alpha(color), r, g, b)
        }
    }
}
