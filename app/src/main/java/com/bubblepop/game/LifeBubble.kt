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

    override fun update(dt: Float) {
        super.update(dt)
        pulsePhase += dt * 6f
    }

    override fun draw(canvas: Canvas) {
        if (!isAlive) return

        // Draw base bubble (includes the shrinking ring from parent)
        super.draw(canvas)

        // Draw heart icon in center
        drawHeart(canvas, x, y, radius * 0.35f)
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val path = Path()
        val s = size

        path.moveTo(cx, cy + s * 0.3f)
        path.cubicTo(cx - s * 1.2f, cy - s * 0.5f, cx - s * 0.5f, cy - s * 1.2f, cx, cy - s * 0.4f)
        path.cubicTo(cx + s * 0.5f, cy - s * 1.2f, cx + s * 1.2f, cy - s * 0.5f, cx, cy + s * 0.3f)
        path.close()

        heartPaint.alpha = 220
        canvas.drawPath(path, heartPaint)
    }
}
