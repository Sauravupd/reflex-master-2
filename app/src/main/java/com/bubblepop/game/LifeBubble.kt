package com.bubblepop.game

import android.graphics.*

class LifeBubble(
    x: Float,
    y: Float,
    radius: Float,
    maxRadius: Float,
    shrinkRate: Float
) : Bubble(x, y, radius, maxRadius, shrinkRate, Color.parseColor("#00FF88")) {

    private var pulsePhase: Float = 0f
    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val glowRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    override fun update(dt: Float) {
        super.update(dt)
        pulsePhase += dt * 6f  // pulsing speed
    }

    override fun draw(canvas: Canvas) {
        if (!isAlive) return

        // Pulsing glow ring
        val pulseScale = 1f + 0.15f * kotlin.math.sin(pulsePhase)
        val glowRadius = radius * 1.3f * pulseScale
        glowRingPaint.shader = RadialGradient(
            x, y, glowRadius,
            intArrayOf(
                Color.argb(80, 0, 255, 136),
                Color.argb(30, 0, 255, 136),
                Color.TRANSPARENT
            ),
            floatArrayOf(0.6f, 0.8f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, glowRadius, glowRingPaint)

        // Draw base bubble
        super.draw(canvas)

        // Draw heart icon in center
        drawHeart(canvas, x, y, radius * 0.35f)
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val path = Path()
        val s = size

        // Heart shape using cubic bezier curves
        path.moveTo(cx, cy + s * 0.3f)
        path.cubicTo(cx - s * 1.2f, cy - s * 0.5f, cx - s * 0.5f, cy - s * 1.2f, cx, cy - s * 0.4f)
        path.cubicTo(cx + s * 0.5f, cy - s * 1.2f, cx + s * 1.2f, cy - s * 0.5f, cx, cy + s * 0.3f)
        path.close()

        heartPaint.alpha = 220
        canvas.drawPath(path, heartPaint)
    }
}
