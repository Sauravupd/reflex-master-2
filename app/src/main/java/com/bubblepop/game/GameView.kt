package com.bubblepop.game

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.*
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private val state = GameState()
    private val bubbles = mutableListOf<Bubble>()
    private val particles = mutableListOf<Particle>()
    private val bgBubbles = mutableListOf<BackgroundBubble>() // decorative
    private lateinit var soundManager: SoundManager
    private var spawnTimer: Float = 0f
    private var screenW: Int = 0
    private var screenH: Int = 0
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bubble_pop_prefs", Context.MODE_PRIVATE)

    // Floating score popups
    private val scorePopups = mutableListOf<ScorePopup>()

    // Colors for bubbles
    private val bubbleColors = intArrayOf(
        Color.parseColor("#FF4757"),
        Color.parseColor("#FF6B35"),
        Color.parseColor("#FFD93D"),
        Color.parseColor("#2ED573"),
        Color.parseColor("#1E90FF"),
        Color.parseColor("#A55EEA"),
        Color.parseColor("#FF6B81"),
        Color.parseColor("#18DCFF"),
        Color.parseColor("#FFC312"),
        Color.parseColor("#ED4C67")
    )

    // Paints
    private val bgPaint = Paint()
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        setShadowLayer(8f, 0f, 2f, Color.argb(100, 0, 0, 0))
    }
    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 72f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 4f, Color.argb(150, 0, 0, 0))
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 110f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(16f, 0f, 6f, Color.argb(180, 100, 50, 255))
    }
    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 255, 255)
        textSize = 40f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    private val gameOverTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4757")
        textSize = 100f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(16f, 0f, 6f, Color.argb(180, 255, 50, 50))
    }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6C5CE7")
    }
    private val buttonTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 46f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val heartPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF4757")
    }
    private val heartEmptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(60, 255, 255, 255)
    }
    private val comboPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFD700")
        textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val levelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(150, 255, 255, 255)
        textSize = 32f
        textAlign = Paint.Align.LEFT
    }
    private val overlayPaint = Paint().apply {
        color = Color.argb(160, 0, 0, 0)
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val popupPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    // Button rectangles
    private var startButtonRect = RectF()
    private var replayButtonRect = RectF()

    // Animation timers
    private var animTime: Float = 0f
    private var shakeOffsetX: Float = 0f
    private var shakeOffsetY: Float = 0f

    // Stars for background
    private val stars = mutableListOf<Star>()

    init {
        holder.addCallback(this)
        isFocusable = true
        state.bestScore = prefs.getInt("best_score", 0)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = width
        screenH = height

        // Initialize background stars
        for (i in 0 until 60) {
            stars.add(
                Star(
                    Random.nextFloat() * screenW,
                    Random.nextFloat() * screenH,
                    Random.nextFloat() * 2f + 0.5f,
                    Random.nextFloat() * 0.6f + 0.2f,
                    Random.nextFloat() * 6.28f
                )
            )
        }

        // Initialize decorative background bubbles
        for (i in 0 until 8) {
            bgBubbles.add(createBackgroundBubble())
        }

        soundManager = SoundManager(context)

        gameThread = GameThread(holder, this)
        gameThread?.running = true
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenW = width
        screenH = height
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.running = false
        try {
            gameThread?.join()
        } catch (_: InterruptedException) {}
        soundManager.release()
    }

    fun updateGame(dt: Float) {
        animTime += dt

        // Update background elements
        updateStars(dt)
        updateBackgroundBubbles(dt)

        if (state.isStartScreen || state.isGameOver) return

        // Update screen shake
        if (state.screenShakeAmount > 0f) {
            state.screenShakeAmount -= dt * 8f
            if (state.screenShakeAmount <= 0f) {
                state.screenShakeAmount = 0f
                shakeOffsetX = 0f
                shakeOffsetY = 0f
            } else {
                shakeOffsetX = (Random.nextFloat() - 0.5f) * state.screenShakeAmount * 10f
                shakeOffsetY = (Random.nextFloat() - 0.5f) * state.screenShakeAmount * 10f
            }
        }

        // Update combo
        state.updateCombo(dt)

        // Update bubbles
        val deadBubbles = mutableListOf<Bubble>()
        for (bubble in bubbles) {
            bubble.update(dt)
            if (!bubble.isAlive) {
                deadBubbles.add(bubble)
            }
        }

        // Handle missed bubbles
        for (bubble in deadBubbles) {
            bubbles.remove(bubble)
            state.loseLife()
            state.comboCount = 0
            state.screenShakeAmount = 1f
            soundManager.playMiss()
            if (state.isGameOver) {
                onGameOver()
                return
            }
        }

        // Update particles
        val deadParticles = mutableListOf<Particle>()
        for (p in particles) {
            p.update(dt)
            if (!p.isAlive()) deadParticles.add(p)
        }
        particles.removeAll(deadParticles.toSet())

        // Update score popups
        val deadPopups = mutableListOf<ScorePopup>()
        for (popup in scorePopups) {
            popup.update(dt)
            if (!popup.isAlive()) deadPopups.add(popup)
        }
        scorePopups.removeAll(deadPopups.toSet())

        // Spawn bubbles
        spawnTimer += dt
        if (spawnTimer >= state.spawnInterval && bubbles.size < state.maxBubbles) {
            spawnBubble()
            spawnTimer = 0f
        }
    }

    fun drawGame(canvas: Canvas) {
        if (screenW == 0 || screenH == 0) return

        canvas.save()
        canvas.translate(shakeOffsetX, shakeOffsetY)

        // Background
        drawBackground(canvas)

        // Draw background bubbles (decorative)
        for (bgBubble in bgBubbles) {
            bgBubble.draw(canvas)
        }

        if (state.isStartScreen) {
            drawStartScreen(canvas)
        } else if (state.isGameOver) {
            // Draw remaining game elements behind overlay
            for (p in particles) drawParticle(canvas, p)
            drawGameOverScreen(canvas)
        } else {
            // Draw bubbles
            for (bubble in bubbles) {
                bubble.draw(canvas)
            }

            // Draw particles
            for (p in particles) {
                drawParticle(canvas, p)
            }

            // Draw score popups
            for (popup in scorePopups) {
                drawScorePopup(canvas, popup)
            }

            // Draw HUD
            drawHUD(canvas)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_DOWN) return true

        val tx = event.x - shakeOffsetX
        val ty = event.y - shakeOffsetY

        if (state.isStartScreen) {
            if (startButtonRect.contains(tx, ty)) {
                state.reset()
                bubbles.clear()
                particles.clear()
                scorePopups.clear()
                spawnTimer = state.spawnInterval  // spawn immediately
            }
            return true
        }

        if (state.isGameOver) {
            if (replayButtonRect.contains(tx, ty)) {
                state.reset()
                bubbles.clear()
                particles.clear()
                scorePopups.clear()
                spawnTimer = state.spawnInterval
            }
            return true
        }

        // Check bubble hits (check smallest first for difficulty, but we want the one on top)
        var hitBubble: Bubble? = null
        for (bubble in bubbles.reversed()) {
            if (bubble.isAlive && bubble.contains(tx, ty)) {
                hitBubble = bubble
                break
            }
        }

        if (hitBubble != null) {
            popBubble(hitBubble)
        }

        return true
    }

    private fun popBubble(bubble: Bubble) {
        bubble.isAlive = false
        bubbles.remove(bubble)

        // Spawn particles
        val particleCount = Random.nextInt(12, 20)
        for (i in 0 until particleCount) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 400f + 100f
            val pColor = if (Random.nextFloat() > 0.3f) bubble.color
            else Bubble.lightenColor(bubble.color, 0.5f)
            particles.add(
                Particle(
                    x = bubble.x,
                    y = bubble.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 100f,
                    radius = Random.nextFloat() * 6f + 3f,
                    color = pColor,
                    decay = Random.nextFloat() * 1.5f + 2f
                )
            )
        }

        // Handle scoring and sounds
        if (bubble is LifeBubble) {
            state.gainLife()
            soundManager.playLifeGain()
            scorePopups.add(ScorePopup(bubble.x, bubble.y, "+♥", Color.parseColor("#00FF88")))
        } else {
            state.addCombo()
            val mult = state.getScoreMultiplier()
            val points = 10 * mult

            // Vary pop pitch based on combo for that addictive feel
            val pitch = 1.0f + (state.comboCount - 1) * 0.05f
            soundManager.playPop(pitch.coerceAtMost(1.8f))

            if (mult > 1) {
                soundManager.playCombo()
            }

            state.score += points
            state.onPop()

            val popupText = if (mult > 1) "+$points (x$mult)" else "+$points"
            val popupColor = if (mult > 1) Color.parseColor("#FFD700") else Color.WHITE
            scorePopups.add(ScorePopup(bubble.x, bubble.y, popupText, popupColor))
        }
    }

    private fun spawnBubble() {
        val isLifeBubble = Random.nextFloat() < state.lifeBubbleChance && state.lives < state.maxLives
        val maxR = state.bubbleMaxRadius + Random.nextFloat() * 20f
        val padding = maxR + 20f
        val x = Random.nextFloat() * (screenW - padding * 2) + padding
        val y = Random.nextFloat() * (screenH * 0.65f - padding) + padding + screenH * 0.12f

        val bubble = if (isLifeBubble) {
            LifeBubble(x, y, maxR, maxR, state.shrinkRate * 0.8f)
        } else {
            val color = bubbleColors[Random.nextInt(bubbleColors.size)]
            Bubble(x, y, maxR, maxR, state.shrinkRate, color)
        }

        bubbles.add(bubble)
    }

    // --- Drawing helpers ---

    private fun drawBackground(canvas: Canvas) {
        // Deep space gradient
        bgPaint.shader = LinearGradient(
            0f, 0f, 0f, screenH.toFloat(),
            intArrayOf(
                Color.parseColor("#0D0D2B"),
                Color.parseColor("#1A1A4E"),
                Color.parseColor("#12123A")
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)

        // Stars
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (star in stars) {
            val twinkle = (sin(animTime * star.twinkleSpeed + star.phase) * 0.3f + 0.7f)
            starPaint.color = Color.argb((star.brightness * twinkle * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(star.x, star.y, star.size, starPaint)
        }
    }

    private fun drawHUD(canvas: Canvas) {
        val topPadding = 60f

        // Score
        scorePaint.textSize = 72f
        canvas.drawText("${state.score}", screenW / 2f, topPadding + 60f, scorePaint)

        // Level indicator
        levelPaint.textSize = 28f
        canvas.drawText("LEVEL ${state.level}", 30f, topPadding + 30f, levelPaint)

        // Hearts (lives)
        val heartSize = 28f
        val heartSpacing = 65f
        val heartsStartX = screenW - 30f - (state.maxLives * heartSpacing)
        val heartsY = topPadding + 20f

        for (i in 0 until state.maxLives) {
            val cx = heartsStartX + i * heartSpacing + heartSize
            val cy = heartsY
            val paint = if (i < state.lives) heartPaint else heartEmptyPaint
            drawHeartShape(canvas, cx, cy, heartSize, paint)
        }

        // Combo indicator
        if (state.comboCount >= 3) {
            val comboAlpha = ((state.comboTimer / state.comboTimeWindow) * 255).toInt().coerceIn(0, 255)
            comboPaint.alpha = comboAlpha
            val comboScale = 1f + sin(animTime * 8f) * 0.1f
            val comboText = "COMBO x${state.getScoreMultiplier()}!"
            comboPaint.textSize = 36f * comboScale
            canvas.drawText(comboText, screenW / 2f, topPadding + 110f, comboPaint)
        }
    }

    private fun drawStartScreen(canvas: Canvas) {
        // Title with pulsing effect
        val pulse = 1f + sin(animTime * 2f) * 0.03f
        titlePaint.textSize = 110f * pulse
        canvas.drawText("BUBBLE", screenW / 2f, screenH * 0.28f, titlePaint)
        titlePaint.textSize = 120f * pulse
        titlePaint.color = Color.parseColor("#FFD700")
        canvas.drawText("POP", screenW / 2f, screenH * 0.28f + 120f, titlePaint)
        titlePaint.color = Color.WHITE

        // Subtitle
        subtitlePaint.alpha = ((sin(animTime * 1.5f) * 0.3f + 0.7f) * 255).toInt()
        canvas.drawText("Tap bubbles before they vanish!", screenW / 2f, screenH * 0.48f, subtitlePaint)

        // Best score
        if (state.bestScore > 0) {
            val bestPaint = Paint(subtitlePaint)
            bestPaint.textSize = 34f
            bestPaint.color = Color.parseColor("#FFD700")
            bestPaint.alpha = 200
            canvas.drawText("BEST: ${state.bestScore}", screenW / 2f, screenH * 0.54f, bestPaint)
        }

        // Start button
        val btnW = 320f
        val btnH = 80f
        val btnX = screenW / 2f - btnW / 2f
        val btnY = screenH * 0.62f
        startButtonRect = RectF(btnX, btnY, btnX + btnW, btnY + btnH)

        // Button glow
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        glowPaint.shader = RadialGradient(
            screenW / 2f, btnY + btnH / 2f, btnW * 0.8f,
            intArrayOf(Color.argb(40, 108, 92, 231), Color.TRANSPARENT),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(btnX - 40f, btnY - 20f, btnX + btnW + 40f, btnY + btnH + 20f), glowPaint)

        // Button background
        buttonPaint.shader = LinearGradient(
            btnX, btnY, btnX + btnW, btnY + btnH,
            Color.parseColor("#6C5CE7"), Color.parseColor("#A55EEA"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(startButtonRect, 40f, 40f, buttonPaint)
        buttonPaint.shader = null

        // Button text
        canvas.drawText("TAP TO START", screenW / 2f, btnY + btnH / 2f + 16f, buttonTextPaint)

        // Instructions
        val instrPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(120, 255, 255, 255)
            textSize = 26f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("💥 Pop bubbles • ♥ Catch life bubbles", screenW / 2f, screenH * 0.82f, instrPaint)
        canvas.drawText("Miss one = lose a life!", screenW / 2f, screenH * 0.86f, instrPaint)
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        // Dark overlay
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), overlayPaint)

        // Game Over title
        val shakeX = sin(animTime * 15f) * max(0f, 1f - animTime) * 5f
        canvas.drawText("GAME OVER", screenW / 2f + shakeX, screenH * 0.3f, gameOverTitlePaint)

        // Score
        val scoreLabelPaint = Paint(subtitlePaint).apply { textSize = 34f; alpha = 180 }
        canvas.drawText("YOUR SCORE", screenW / 2f, screenH * 0.40f, scoreLabelPaint)

        scorePaint.textSize = 96f
        canvas.drawText("${state.score}", screenW / 2f, screenH * 0.48f, scorePaint)

        // Best score
        val isNewBest = state.score > state.bestScore
        if (isNewBest) {
            val newBestPaint = Paint(subtitlePaint).apply {
                textSize = 36f
                color = Color.parseColor("#FFD700")
                alpha = ((sin(animTime * 4f) * 0.3f + 0.7f) * 255).toInt()
            }
            canvas.drawText("★ NEW BEST! ★", screenW / 2f, screenH * 0.54f, newBestPaint)
        } else if (state.bestScore > 0) {
            val bestPaint = Paint(subtitlePaint).apply { textSize = 30f; alpha = 150 }
            canvas.drawText("BEST: ${state.bestScore}", screenW / 2f, screenH * 0.54f, bestPaint)
        }

        // Play again button
        val btnW = 320f
        val btnH = 80f
        val btnX = screenW / 2f - btnW / 2f
        val btnY = screenH * 0.62f
        replayButtonRect = RectF(btnX, btnY, btnX + btnW, btnY + btnH)

        buttonPaint.shader = LinearGradient(
            btnX, btnY, btnX + btnW, btnY + btnH,
            Color.parseColor("#6C5CE7"), Color.parseColor("#A55EEA"),
            Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(replayButtonRect, 40f, 40f, buttonPaint)
        buttonPaint.shader = null
        canvas.drawText("PLAY AGAIN", screenW / 2f, btnY + btnH / 2f + 16f, buttonTextPaint)
    }

    private fun drawParticle(canvas: Canvas, p: Particle) {
        particlePaint.color = p.color
        particlePaint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)
        canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
    }

    private fun drawScorePopup(canvas: Canvas, popup: ScorePopup) {
        popupPaint.color = popup.color
        popupPaint.alpha = (popup.alpha * 255).toInt().coerceIn(0, 255)
        popupPaint.textSize = 40f * popup.scale
        canvas.drawText(popup.text, popup.x, popup.y, popupPaint)
    }

    private fun drawHeartShape(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        val path = Path()
        val s = size
        path.moveTo(cx, cy + s * 0.3f)
        path.cubicTo(cx - s * 1.2f, cy - s * 0.5f, cx - s * 0.5f, cy - s * 1.2f, cx, cy - s * 0.4f)
        path.cubicTo(cx + s * 0.5f, cy - s * 1.2f, cx + s * 1.2f, cy - s * 0.5f, cx, cy + s * 0.3f)
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun onGameOver() {
        soundManager.playGameOver()
        if (state.score > state.bestScore) {
            state.bestScore = state.score
            prefs.edit().putInt("best_score", state.bestScore).apply()
        }
    }

    // --- Background elements ---

    private fun updateStars(dt: Float) {
        // Stars just twinkle, no position update needed
    }

    private fun updateBackgroundBubbles(dt: Float) {
        for (bgBubble in bgBubbles) {
            bgBubble.y -= bgBubble.speed * dt
            bgBubble.x += sin(animTime * bgBubble.wobbleSpeed + bgBubble.phase) * 0.5f
            bgBubble.alpha = (sin(animTime * 0.5f + bgBubble.phase) * 0.3f + 0.5f).coerceIn(0.1f, 0.6f)
            if (bgBubble.y < -bgBubble.radius * 2) {
                bgBubble.y = screenH.toFloat() + bgBubble.radius
                bgBubble.x = Random.nextFloat() * screenW
            }
        }
    }

    private fun createBackgroundBubble(): BackgroundBubble {
        return BackgroundBubble(
            x = Random.nextFloat() * max(screenW, 1080),
            y = Random.nextFloat() * max(screenH, 2400),
            radius = Random.nextFloat() * 40f + 15f,
            speed = Random.nextFloat() * 30f + 10f,
            alpha = Random.nextFloat() * 0.3f + 0.1f,
            color = bubbleColors[Random.nextInt(bubbleColors.size)],
            wobbleSpeed = Random.nextFloat() * 2f + 0.5f,
            phase = Random.nextFloat() * 6.28f
        )
    }

    // --- Inner data classes ---

    data class Star(val x: Float, val y: Float, val size: Float, val brightness: Float, val phase: Float, val twinkleSpeed: Float = 2f + Random.nextFloat() * 3f)

    data class BackgroundBubble(
        var x: Float, var y: Float, var radius: Float,
        val speed: Float, var alpha: Float, val color: Int,
        val wobbleSpeed: Float, val phase: Float
    ) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun draw(canvas: Canvas) {
            paint.color = color
            paint.alpha = (alpha * 60).toInt().coerceIn(0, 255)
            canvas.drawCircle(x, y, radius, paint)
        }
    }

    data class ScorePopup(
        var x: Float, var y: Float,
        val text: String, val color: Int,
        var alpha: Float = 1f,
        var scale: Float = 1.5f,
        var life: Float = 1f
    ) {
        fun update(dt: Float) {
            y -= 80f * dt
            life -= dt * 1.2f
            alpha = life.coerceIn(0f, 1f)
            scale = 1f + (1f - life) * 0.3f
        }
        fun isAlive() = life > 0f
    }
}
