package com.bubblepop.game

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

    // Paint objects
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    open fun update(dt: Float) {
        age += dt
        radius -= shrinkRate * dt
        if (radius <= 8f) {
            isAlive = false
        }
    }

    open fun draw(canvas: Canvas) {
        if (!isAlive) return

        val alpha = ((radius / maxRadius) * 255).toInt().coerceIn(60, 255)

        // Outer glow
        glowPaint.shader = RadialGradient(
            x, y, radius * 1.5f,
            intArrayOf(Color.argb(40, Color.red(color), Color.green(color), Color.blue(color)), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x, y, radius * 1.5f, glowPaint)

        // Main bubble fill with radial gradient
        val lighterColor = lightenColor(color, 0.4f)
        fillPaint.shader = RadialGradient(
            x - radius * 0.3f, y - radius * 0.3f, radius * 1.2f,
            intArrayOf(lighterColor, color, darkenColor(color, 0.3f)),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        fillPaint.alpha = alpha
        canvas.drawCircle(x, y, radius, fillPaint)

        // Specular highlight
        highlightPaint.shader = RadialGradient(
            x - radius * 0.25f, y - radius * 0.3f, radius * 0.5f,
            intArrayOf(Color.argb(180, 255, 255, 255), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(x - radius * 0.25f, y - radius * 0.3f, radius * 0.5f, highlightPaint)

        // Subtle outline
        outlinePaint.color = Color.argb(50, 255, 255, 255)
        canvas.drawCircle(x, y, radius, outlinePaint)
    }

    fun contains(tx: Float, ty: Float): Boolean {
        val dx = tx - x
        val dy = ty - y
        return dx * dx + dy * dy <= radius * radius
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
