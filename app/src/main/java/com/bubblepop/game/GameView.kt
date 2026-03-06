package com.bubblepop.game

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
    private val bgBubbles = mutableListOf<BackgroundBubble>()
    private lateinit var soundManager: SoundManager
    private var soundManagerInitialized = false
    private var spawnTimer: Float = 0f
    private var screenW: Int = 0
    private var screenH: Int = 0
    private val prefs: SharedPreferences =
        context.getSharedPreferences("bubble_pop_prefs", Context.MODE_PRIVATE)

    private val scorePopups = mutableListOf<ScorePopup>()

    // Haptic feedback
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        mgr.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

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
    private val flashPaint = Paint()

    // Button rectangles
    private var startButtonRect = RectF()
    private var settingsButtonRect = RectF()
    private var instructionsButtonRect = RectF()
    private var replayButtonRect = RectF()
    private var menuButtonRect = RectF()
    private var backButtonRect = RectF()
    private var speakerButtonRect = RectF()
    
    // Custom Paints for Screenshot Match
    private val mainTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 120f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        textAlign = Paint.Align.CENTER
    }
    private val digitalScorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        typeface = Typeface.MONOSPACE
        textAlign = Paint.Align.LEFT
    }
    private val criticalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#747D8C")
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        letterSpacing = 0.1f
    }
    private val statsCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 255, 255, 255)
        style = Paint.Style.FILL
    }
    private val statsStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(40, 255, 255, 255)
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    
    // Settings toggles (stored in prefs)
    private var musicEnabled = true
    private var hapticsEnabled = true
    private var sfxEnabled = true
    
    // UI States
    private var isSettingsOpen = false
    private var isInstructionsOpen = false

    // Animation timers
    private var animTime: Float = 0f
    private var shakeOffsetX: Float = 0f
    private var shakeOffsetY: Float = 0f

    // Game over message
    private var gameOverMessage: String = ""
    private var gameOverScoreDisplay = 0
    private var gameOverScoreTimer = 0f

    // --- Dopamine effects ---
    // Screen flash on pop
    private var flashAlpha: Float = 0f
    private var flashColor: Int = Color.WHITE

    // Streak text shown during gameplay
    private var streakText: String = ""
    private var streakTimer: Float = 0f
    private val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 48f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(12f, 0f, 4f, Color.argb(200, 0, 0, 0))
    }

    // Level up flash
    private var levelUpTimer: Float = 0f
    private var lastLevel: Int = 1

    // Score counter animation (displayed score climbs toward actual score)
    private var displayedScore: Float = 0f

    // Stars for background
    private val stars = mutableListOf<Star>()

    init {
        holder.addCallback(this)
        isFocusable = true
        state.bestScore = prefs.getInt("best_score", 0)
        musicEnabled = prefs.getBoolean("music_enabled", true)
        hapticsEnabled = prefs.getBoolean("haptics_enabled", true)
        sfxEnabled = prefs.getBoolean("sfx_enabled", true)
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
        soundManager.musicEnabled = musicEnabled
        soundManager.sfxEnabled = sfxEnabled
        soundManagerInitialized = true
        if (musicEnabled) soundManager.startMusic()

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
        soundManagerInitialized = false
    }

    /** Called by MainActivity.onPause */
    fun onPauseGame() {
        if (soundManagerInitialized) {
            soundManager.pauseMusic()
        }
    }

    /** Called by MainActivity.onResume */
    fun onResumeGame() {
        if (soundManagerInitialized) {
            soundManager.startMusic()
        }
    }

    fun updateGame(dt: Float) {
        animTime += dt

        // Update background elements
        updateStars(dt)
        updateBackgroundBubbles(dt)

        // Animate displayed score toward actual score (satisfying counter climb)
        if (displayedScore < state.score) {
            val diff = state.score - displayedScore
            displayedScore += (diff * 8f * dt).coerceAtLeast(1f)
            if (displayedScore > state.score) displayedScore = state.score.toFloat()
        }

        // Game Over score count-up
        if (state.isGameOver && gameOverScoreDisplay < state.score) {
            gameOverScoreTimer += dt
            if (gameOverScoreTimer > 0.02f) {
                gameOverScoreDisplay = (gameOverScoreDisplay + (state.score / 40).coerceAtLeast(1)).coerceAtMost(state.score)
                gameOverScoreTimer = 0f
                if (gameOverScoreDisplay % 10 == 0) soundManager.playPop(1.5f) // subtle tick
            }
        }

        // Update screen flash
        if (flashAlpha > 0f) {
            flashAlpha -= dt * 6f
            if (flashAlpha < 0f) flashAlpha = 0f
        }

        // Update streak text
        if (streakTimer > 0f) {
            streakTimer -= dt
        }

        // Update level up flash
        if (levelUpTimer > 0f) {
            levelUpTimer -= dt
        }

        if (state.isStartScreen || state.isGameOver) return

        // Check for level up
        if (state.level != lastLevel) {
            lastLevel = state.level
            levelUpTimer = 2.0f
            streakText = "⚡ LEVEL ${state.level} ⚡"
            streakTimer = 2.0f
            // Screen flash for level up
            flashColor = Color.parseColor("#6C5CE7")
            flashAlpha = 0.4f
            buzzHaptic(40)
        }

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
            // Red flash on miss
            flashColor = Color.parseColor("#FF4757")
            flashAlpha = 0.3f
            buzzHaptic(80)
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
            val deficit = state.maxBubbles - bubbles.size
            val spawnCount = if (deficit >= 3) 2 else 1
            for (i in 0 until spawnCount) {
                if (bubbles.size < state.maxBubbles) {
                    spawnBubble()
                }
            }
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

        if (isSettingsOpen) {
            drawSettings(canvas)
        } else if (isInstructionsOpen) {
            drawInstructions(canvas)
        } else if (state.isStartScreen) {
            drawStartScreen(canvas)
        } else if (state.isGameOver) {
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

            // Draw streak text (combo announcements, level ups)
            if (streakTimer > 0f) {
                drawStreakText(canvas)
            }
        }

        // Draw screen flash overlay (on top of everything)
        if (flashAlpha > 0f) {
            flashPaint.color = flashColor
            flashPaint.alpha = (flashAlpha * 255).toInt().coerceIn(0, 180)
            canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), flashPaint)
        }

        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        
        // Settings and Instructions are single-touch for simplicity
        if (isSettingsOpen || isInstructionsOpen || state.isStartScreen || state.isGameOver) {
            if (action == MotionEvent.ACTION_DOWN) {
                handleUIPress(event.x, event.y)
            }
            return true
        }

        // Gameplay supports Multi-Touch
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val tx = event.getX(pointerIndex) - shakeOffsetX
                val ty = event.getY(pointerIndex) - shakeOffsetY
                
                // Check speaker toggle first (more sensitive area for touch)
                if (speakerButtonRect.contains(tx, ty + (if (state.isStartScreen) 0f else -shakeOffsetY))) {
                    musicEnabled = !musicEnabled
                    soundManager.musicEnabled = musicEnabled
                    prefs.edit().putBoolean("music_enabled", musicEnabled).apply()
                    buzzHaptic(10)
                    return true
                }
                
                checkBubbleHit(tx, ty)
            }
        }

        return true
    }

    private fun handleUIPress(tx: Float, ty: Float) {
        // Speaker button check for all screens
        if (speakerButtonRect.contains(tx, ty)) {
            musicEnabled = !musicEnabled
            soundManager.musicEnabled = musicEnabled
            prefs.edit().putBoolean("music_enabled", musicEnabled).apply()
            buzzHaptic(10)
            return
        }

        if (isSettingsOpen) {
            // Check back button
            if (backButtonRect.contains(tx, ty)) {
                isSettingsOpen = false
                return
            }
            // Check toggles (Music, Haptics, SFX)
            val centerX = screenW / 2f
            val centerY = screenH / 2f
            if (RectF(centerX - 100f, centerY - 60f, centerX + 100f, centerY - 20f).contains(tx, ty)) {
                musicEnabled = !musicEnabled
                soundManager.musicEnabled = musicEnabled
                prefs.edit().putBoolean("music_enabled", musicEnabled).apply()
                buzzHaptic(10)
            }
            if (RectF(centerX - 100f, centerY + 10f, centerX + 100f, centerY + 50f).contains(tx, ty)) {
                hapticsEnabled = !hapticsEnabled
                prefs.edit().putBoolean("haptics_enabled", hapticsEnabled).apply()
                if (hapticsEnabled) buzzHaptic(20)
            }
            if (RectF(centerX - 100f, centerY + 80f, centerX + 100f, centerY + 120f).contains(tx, ty)) {
                sfxEnabled = !sfxEnabled
                soundManager.sfxEnabled = sfxEnabled
                prefs.edit().putBoolean("sfx_enabled", sfxEnabled).apply()
                if (sfxEnabled) soundManager.playPop()
            }
            return
        }

        if (isInstructionsOpen) {
            if (backButtonRect.contains(tx, ty)) {
                isInstructionsOpen = false
            }
            return
        }

        if (state.isStartScreen) {
            if (startButtonRect.contains(tx, ty)) {
                startGame()
            } else if (instructionsButtonRect.contains(tx, ty)) {
                isInstructionsOpen = true
            } else if (settingsButtonRect.contains(tx, ty)) {
                // We'll hide settings on home screen in the new UI per screenshot, 
                // but let's keep logic if button exists
                isSettingsOpen = true
            }
            return
        }

        if (state.isGameOver) {
            if (replayButtonRect.contains(tx, ty)) {
                startGame()
            } else if (menuButtonRect.contains(tx, ty)) {
                state.isStartScreen = true
                state.isGameOver = false
            }
            return
        }
    }

    private fun startGame() {
        state.reset()
        lastLevel = 1
        displayedScore = 0f
        bubbles.clear()
        particles.clear()
        scorePopups.clear()
        spawnTimer = state.spawnInterval
    }

    private fun checkBubbleHit(tx: Float, ty: Float) {
        var hitBubble: Bubble? = null
        // Synchronization or copy to avoid concurrent modification if multi-touch happens exactly same time
        synchronized(bubbles) {
            for (bubble in bubbles.reversed()) {
                if (bubble.isAlive && bubble.contains(tx, ty)) {
                    hitBubble = bubble
                    break
                }
            }
        }

        if (hitBubble != null) {
            popBubble(hitBubble!!)
        }
    }

    private fun popBubble(bubble: Bubble) {
        bubble.isAlive = false
        bubbles.remove(bubble)

        // --- DOPAMINE: More particles, more dramatic ---
        val mult = state.getScoreMultiplier()
        val particleCount = Random.nextInt(16, 28) + (mult * 4)  // more particles on combos
        for (i in 0 until particleCount) {
            val angle = Random.nextFloat() * 2f * PI.toFloat()
            val speed = Random.nextFloat() * 500f + 150f  // faster particles
            val pColor = if (Random.nextFloat() > 0.25f) bubble.color
            else Bubble.lightenColor(bubble.color, 0.6f)
            particles.add(
                Particle(
                    x = bubble.x,
                    y = bubble.y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 150f,
                    radius = Random.nextFloat() * 8f + 3f,  // bigger particles
                    color = pColor,
                    decay = Random.nextFloat() * 1.5f + 2f
                )
            )
        }

        // --- Sparkle ring: extra ring of small white particles ---
        for (i in 0 until 8) {
            val angle = (i / 8f) * 2f * PI.toFloat()
            particles.add(
                Particle(
                    x = bubble.x + cos(angle) * bubble.maxRadius * 0.8f,
                    y = bubble.y + sin(angle) * bubble.maxRadius * 0.8f,
                    vx = cos(angle) * 200f,
                    vy = sin(angle) * 200f,
                    radius = 3f,
                    color = Color.WHITE,
                    decay = 3f
                )
            )
        }

        // --- Screen flash: brief white flash on pop ---
        flashColor = Bubble.lightenColor(bubble.color, 0.7f)
        flashAlpha = if (mult > 1) 0.25f else 0.12f  // stronger flash on combos

        // --- Haptic: satisfying click ---
        buzzHaptic(if (mult > 1) 20 else 10)

        // Handle scoring and sounds
        if (bubble is LifeBubble) {
            state.gainLife()
            soundManager.playLifeGain()
            scorePopups.add(ScorePopup(bubble.x, bubble.y, "+♥", Color.parseColor("#00FF88"), scale = 2f))
            // Green flash for life gain
            flashColor = Color.parseColor("#00FF88")
            flashAlpha = 0.2f
            buzzHaptic(30)
        } else {
            state.addCombo()
            val newMult = state.getScoreMultiplier()
            val points = 10 * newMult

            val pitch = 1.0f + (state.comboCount - 1) * 0.06f
            soundManager.playPop(pitch.coerceAtMost(2.0f))

            if (newMult > 1) {
                soundManager.playCombo()
            }

            state.score += points
            state.onPop()

            // Score popup — bigger text for combos
            val popupText = if (newMult > 1) "+$points (x$newMult)" else "+$points"
            val popupColor = when {
                newMult >= 4 -> Color.parseColor("#FF4757")   // red for 4x
                newMult >= 3 -> Color.parseColor("#FF6B35")   // orange for 3x
                newMult >= 2 -> Color.parseColor("#FFD700")   // gold for 2x
                else -> Color.WHITE
            }
            val popupScale = 1.5f + (newMult - 1) * 0.3f  // bigger popup on combos
            scorePopups.add(ScorePopup(bubble.x, bubble.y, popupText, popupColor, scale = popupScale))

            // --- Streak announcements ---
            when {
                state.comboCount == 5 -> {
                    streakText = "🔥 ON FIRE!"
                    streakTimer = 1.5f
                }
                state.comboCount == 10 -> {
                    streakText = "⚡ UNSTOPPABLE!"
                    streakTimer = 1.5f
                }
                state.comboCount == 15 -> {
                    streakText = "💀 GODLIKE!"
                    streakTimer = 2.0f
                }
                state.comboCount == 20 -> {
                    streakText = "🌟 LEGENDARY!"
                    streakTimer = 2.0f
                }
            }

            // Milestone pops
            when (state.totalPops) {
                25 -> { streakText = "25 POPS! 🎯"; streakTimer = 1.5f }
                50 -> { streakText = "50 POPS! 💪"; streakTimer = 1.5f }
                100 -> { streakText = "100 POPS! 🏆"; streakTimer = 2.0f }
                200 -> { streakText = "200 POPS! 👑"; streakTimer = 2.0f }
            }
        }
    }

    private fun buzzHaptic(ms: Int) {
        if (!hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(ms.toLong())
            }
        } catch (_: Exception) {}
    }

    private fun spawnBubble() {
        val isLifeBubble = Random.nextFloat() < state.lifeBubbleChance && state.lives < state.maxLives
        val maxR = state.bubbleMaxRadius + Random.nextFloat() * 15f
        val padding = maxR * 1.5f + 20f
        
        // --- TWO-THUMB OPTIMIZATION: Left and Right Lanes ---
        // Bubbles spawn primarily in the left 40% or right 40% of the screen
        val laneWidth = screenW * 0.4f
        val isLeftLane = Random.nextBoolean()
        val startX = if (isLeftLane) padding else (screenW - laneWidth + padding)
        val endX = if (isLeftLane) (laneWidth - padding) else (screenW - padding)
        
        val x = if (endX > startX) {
            Random.nextFloat() * (endX - startX) + startX
        } else {
            Random.nextFloat() * (screenW - padding * 2) + padding
        }
        
        val y = Random.nextFloat() * (screenH - padding * 2 - 120f) + padding + 80f

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

        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (star in stars) {
            val twinkle = (sin(animTime * star.twinkleSpeed + star.phase) * 0.3f + 0.7f)
            starPaint.color = Color.argb((star.brightness * twinkle * 255).toInt().coerceIn(0, 255), 255, 255, 255)
            canvas.drawCircle(star.x, star.y, star.size, starPaint)
        }
    }

    private fun drawHUD(canvas: Canvas) {
        // Very generous padding for notches and rounded corners
        val topPadding = 140f
        val sidePadding = 120f

        // Score: Digital style (0000000) - Top Left
        val formattedScore = String.format("%07d", displayedScore.toInt())
        digitalScorePaint.textSize = 42f
        canvas.drawText(formattedScore, sidePadding, topPadding + 40f, digitalScorePaint)
        
        // "SCORE" Label
        criticalLabelPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("SCORE", sidePadding, topPadding, criticalLabelPaint)

        // "CRITICAL MISSES" - Top Right (Moved further left)
        criticalLabelPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("CRITICAL MISSES", screenW - sidePadding - 180f, topPadding, criticalLabelPaint)
        
        // Life indicators (dots) - NOW BELOW label to prevent horizontal overlap
        val dotRadius = 8f
        val dotGap = 35f
        val dotsStartY = topPadding + 35f
        for (i in 0 until state.maxLives) {
            val cx = screenW - sidePadding - 280f + (i * dotGap)
            val cy = dotsStartY
            val dotPaint = if (i < state.lives) {
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FF4757") }
            } else {
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 255, 255, 255); style = Paint.Style.STROKE; strokeWidth = 2f }
            }
            canvas.drawCircle(cx, cy, dotRadius, dotPaint)
        }

        // Speaker Icon - Top Right (Moved in and down)
        drawSpeakerIcon(canvas, screenW - sidePadding, topPadding - 10f)

        // Combo indicator
        if (state.comboCount >= 2) {
            val comboAlpha = ((state.comboTimer / state.comboTimeWindow) * 255).toInt().coerceIn(0, 255)
            comboPaint.alpha = comboAlpha
            val pulseSpeed = 6f + state.comboCount * 0.5f
            val comboScale = 1f + sin(animTime * pulseSpeed) * 0.15f
            val comboText = "COMBO x${state.getScoreMultiplier()}!"
            val baseSize = 32f + (state.comboCount.coerceAtMost(15)) * 1.5f
            comboPaint.textSize = baseSize * comboScale
            comboPaint.color = when {
                state.comboCount >= 15 -> Color.parseColor("#FF4757")
                state.comboCount >= 10 -> Color.parseColor("#FF6B35")
                state.comboCount >= 5 -> Color.parseColor("#FFD93D")
                else -> Color.parseColor("#FFD700")
            }
            comboPaint.alpha = comboAlpha
            canvas.drawText(comboText, screenW / 2f, topPadding + 85f, comboPaint)
        }
    }

    private fun drawSpeakerIcon(canvas: Canvas, cx: Float, cy: Float) {
        val size = 45f
        speakerButtonRect = RectF(cx - size, cy - size, cx + size, cy + size)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 255, 255, 255)
            style = Paint.Style.FILL
        }
        canvas.drawCircle(cx, cy, size, paint)
        
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        
        // Simplified Speaker Path
        val path = Path()
        path.moveTo(cx - 15f, cy - 8f)
        path.lineTo(cx - 5f, cy - 8f)
        path.lineTo(cx + 5f, cy - 18f)
        path.lineTo(cx + 5f, cy + 18f)
        path.lineTo(cx - 5f, cy + 8f)
        path.lineTo(cx - 15f, cy + 8f)
        path.close()
        canvas.drawPath(path, iconPaint)
        
        if (musicEnabled) {
            // Sound waves
            canvas.drawArc(cx - 10f, cy - 10f, cx + 15f, cy + 10f, -45f, 90f, false, iconPaint)
            canvas.drawArc(cx - 15f, cy - 15f, cx + 25f, cy + 15f, -45f, 90f, false, iconPaint)
        } else {
            // X mark
            canvas.drawLine(cx + 10f, cy - 8f, cx + 20f, cy + 8f, iconPaint)
            canvas.drawLine(cx + 20f, cy - 8f, cx + 10f, cy + 8f, iconPaint)
        }
    }

    private fun drawStreakText(canvas: Canvas) {
        val progress = (streakTimer / 2.0f).coerceIn(0f, 1f)
        val alpha = (progress * 255).toInt().coerceIn(0, 255)
        val scale = 1f + (1f - progress) * 0.3f + sin(animTime * 10f) * 0.05f

        streakPaint.alpha = alpha
        streakPaint.textSize = 48f * scale
        streakPaint.color = Color.parseColor("#FFD700")
        streakPaint.alpha = alpha

        val y = screenH * 0.45f - (1f - progress) * 30f  // float upward
        canvas.drawText(streakText, screenW / 2f, y, streakPaint)
    }

    private fun drawStartScreen(canvas: Canvas) {
        val centerX = screenW / 2f
        val centerY = screenH / 2f

        // TITLE: "TWITCH" (Gradient + Glow) - CENTER
        mainTitlePaint.shader = LinearGradient(0f, centerY - 150f, 0f, centerY - 50f,
            Color.parseColor("#74B9FF"), Color.parseColor("#A55EEA"), Shader.TileMode.CLAMP)
        mainTitlePaint.setShadowLayer(40f, 0f, 0f, Color.argb(120, 108, 92, 231))
        canvas.drawText("TWITCH", centerX, centerY - 50f, mainTitlePaint)
        mainTitlePaint.shader = null
        mainTitlePaint.setShadowLayer(0f, 0f, 0f, 0)

        // SUBTITLE
        subtitlePaint.textSize = 24f
        subtitlePaint.color = Color.parseColor("#747D8C")
        subtitlePaint.letterSpacing = 0.4f
        canvas.drawText("MASTER REFLEX EDITION", centerX, centerY, subtitlePaint)

        // BUTTONS: CENTERED
        val btnW = 450f
        val btnH = 90f
        
        // START GAME (Large White Button)
        startButtonRect = RectF(centerX - btnW/2f, centerY + 80f, centerX + btnW/2f, centerY + 80f + btnH)
        val startBtnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(startButtonRect, btnH/2f, btnH/2f, startBtnPaint)
        
        buttonTextPaint.color = Color.BLACK
        buttonTextPaint.textSize = 34f
        canvas.drawText("START GAME", centerX, startButtonRect.centerY() + 12f, buttonTextPaint)

        // Small Secondary Buttons
        val subBtnW = 220f
        val subBtnH = 70f
        val gap = 30f
        
        // HOW TO PLAY
        instructionsButtonRect = RectF(centerX - subBtnW - gap/2f, centerY + 190f, centerX - gap/2f, centerY + 190f + subBtnH)
        canvas.drawRoundRect(instructionsButtonRect, subBtnH/2f, subBtnH/2f, statsCardPaint)
        
        buttonTextPaint.color = Color.WHITE
        buttonTextPaint.textSize = 24f
        canvas.drawText("HOW TO PLAY", instructionsButtonRect.centerX(), instructionsButtonRect.centerY() + 10f, buttonTextPaint)

        // SETTINGS (Replaces Audio Ready)
        settingsButtonRect = RectF(centerX + gap/2f, centerY + 190f, centerX + subBtnW + gap/2f, centerY + 190f + subBtnH)
        canvas.drawRoundRect(settingsButtonRect, subBtnH/2f, subBtnH/2f, statsCardPaint)
        
        buttonTextPaint.color = Color.WHITE
        canvas.drawText("SETTINGS", settingsButtonRect.centerX(), settingsButtonRect.centerY() + 10f, buttonTextPaint)

        // Best score discreetly at bottom
        if (state.bestScore > 0) {
            subtitlePaint.textSize = 22f
            subtitlePaint.color = Color.argb(100, 255, 255, 255)
            canvas.drawText("TOP SCORE: ${state.bestScore}", centerX, screenH - 40f, subtitlePaint)
        }

        // Speaker Icon - Top Right (Safely away from corner)
        drawSpeakerIcon(canvas, screenW - 120f, 120f)
    }

    private fun drawElegantButton(canvas: Canvas, rect: RectF, text: String, color1: Int, color2: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(rect.left, rect.top, rect.right, rect.bottom, color1, color2, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, 40f, 40f, paint)
        
        buttonTextPaint.textSize = 40f
        canvas.drawText(text, rect.centerX(), rect.centerY() + 14f, buttonTextPaint)
    }

    private fun drawGameOverScreen(canvas: Canvas) {
        val centerX = screenW / 2f
        val centerY = screenH / 2f

        canvas.drawColor(Color.parseColor("#050510"))

        // "FAILED" TITLE
        mainTitlePaint.shader = LinearGradient(0f, 60f, 0f, 160f,
            Color.parseColor("#FF4757"), Color.parseColor("#ED4C67"), Shader.TileMode.CLAMP)
        mainTitlePaint.setShadowLayer(40f, 0f, 0f, Color.argb(160, 237, 76, 103))
        canvas.drawText("FAILED", centerX, 150f, mainTitlePaint)
        mainTitlePaint.shader = null

        // GRID LAYOUT FOR STATS (More spaced out)
        val startX = 100f
        val startY = 240f
        val boxW = 300f
        val boxH = 145f
        val hGap = 45f
        val vGap = 40f

        fun drawStatBox(label: String, value: String, x: Float, y: Float, valColor: Int) {
            val rect = RectF(x, y, x + boxW, y + boxH)
            canvas.drawRoundRect(rect, 20f, 20f, statsCardPaint)
            
            val labelP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#747D8C"); textSize = 26f; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            val valueP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = valColor; textSize = 48f; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            
            canvas.drawText(label, rect.centerX(), rect.top + 50f, labelP)
            canvas.drawText(value, rect.centerX(), rect.bottom - 40f, valueP)
        }

        drawStatBox("SCORE", "${state.score}", startX, startY, Color.WHITE)
        drawStatBox("POPPED", "${state.totalPops}", startX + boxW + hGap, startY, Color.parseColor("#74B9FF"))
        drawStatBox("TOTAL MISSED", "${state.totalMissed}", startX, startY + boxH + vGap, Color.parseColor("#747D8C"))
        drawStatBox("COMBO", "${state.maxCombo}", startX + boxW + hGap, startY + boxH + vGap, Color.parseColor("#FFD700"))

        // BUTTONS below grid
        val btnW = 260f
        val btnH = 90f
        
        // RETRY
        replayButtonRect = RectF(startX, startY + (boxH + vGap) * 2f + 30f, startX + btnW, startY + (boxH + vGap) * 2f + btnH + 30f)
        val retryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(replayButtonRect, btnH/2f, btnH/2f, retryPaint)
        buttonTextPaint.color = Color.BLACK
        buttonTextPaint.textSize = 32f
        canvas.drawText("RETRY", replayButtonRect.centerX(), replayButtonRect.centerY() + 10f, buttonTextPaint)

        // MENU
        menuButtonRect = RectF(startX + btnW + hGap, startY + (boxH + vGap) * 2f + 30f, startX + btnW * 2f + hGap, startY + (boxH + vGap) * 2f + btnH + 30f)
        canvas.drawRoundRect(menuButtonRect, btnH/2f, btnH/2f, statsCardPaint)
        buttonTextPaint.color = Color.WHITE
        canvas.drawText("MENU", menuButtonRect.centerX(), menuButtonRect.centerY() + 10f, buttonTextPaint)

        // PERFORMANCE REPORT BOX (Right Side - Expanded to fill)
        val reportLeft = startX + (boxW + hGap) * 2f + 40f
        val reportRect = RectF(reportLeft, startY, screenW - 80f, startY + (boxH + vGap) * 2f + btnH + 30f)
        val reportPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(25, 116, 185, 255); style = Paint.Style.FILL }
        canvas.drawRoundRect(reportRect, 30f, 30f, reportPaint)
        
        statsStrokePaint.color = Color.argb(100, 116, 185, 255)
        canvas.drawRoundRect(reportRect, 30f, 30f, statsStrokePaint)
        
        criticalLabelPaint.textAlign = Paint.Align.LEFT
        criticalLabelPaint.textSize = 30f
        canvas.drawText("PERFORMANCE REPORT", reportRect.left + 50f, reportRect.top + 70f, criticalLabelPaint)
        
        val msgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.LEFT
        }
        
        val msg = "\"$gameOverMessage\""
        canvas.drawText(msg, reportRect.left + 50f, reportRect.centerY(), msgPaint)
    }

    private fun drawSettings(canvas: Canvas) {
        val centerX = screenW / 2f
        val centerY = screenH / 2f
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), overlayPaint)

        titlePaint.textSize = 80f
        canvas.drawText("SETTINGS", centerX, centerY - 150f, titlePaint)

        val togglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 40f
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }

        fun drawToggle(label: String, enabled: Boolean, y: Float) {
            val color = if (enabled) Color.parseColor("#2ED573") else Color.parseColor("#FF4757")
            val status = if (enabled) "ON" else "OFF"
            
            // Draw box
            val boxW = 200f
            val boxH = 60f
            val boxRect = RectF(centerX - 100f, y - 40f, centerX + 100f, y)
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                alpha = 180
            }
            canvas.drawRoundRect(boxRect, 30f, 30f, p)
            
            canvas.drawText("$label: $status", centerX, y - 10f, togglePaint)
        }

        drawToggle("MUSIC", musicEnabled, centerY - 20f)
        drawToggle("HAPTICS", hapticsEnabled, centerY + 50f)
        drawToggle("SOUND FX", sfxEnabled, centerY + 120f)

        // Back button
        val btnW = 200f
        val btnH = 70f
        backButtonRect = RectF(centerX - btnW/2f, screenH - btnH - 40f, centerX + btnW/2f, screenH - 40f)
        drawElegantButton(canvas, backButtonRect, "BACK", Color.parseColor("#636E72"), Color.parseColor("#2D3436"))
    }

    private fun drawInstructions(canvas: Canvas) {
        val centerX = screenW / 2f
        val centerY = screenH / 2f
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), overlayPaint)

        titlePaint.textSize = 80f
        canvas.drawText("HOW TO PLAY", centerX, centerY - 120f, titlePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
        }

        canvas.drawText("🫧 Tap bubbles before they shrink!", centerX, centerY - 40f, textPaint)
        canvas.drawText("♥ Catch life bubbles to survive.", centerX, centerY + 10f, textPaint)
        canvas.drawText("⚡ Chain pops for massive COMBOs.", centerX, centerY + 60f, textPaint)
        canvas.drawText("👍 PRO TIP: Use both thumbs!", centerX, centerY + 110f, textPaint)

        // Back button
        val btnW = 200f
        val btnH = 70f
        backButtonRect = RectF(centerX - btnW/2f, screenH - btnH - 40f, centerX + btnW/2f, screenH - 40f)
        drawElegantButton(canvas, backButtonRect, "BACK", Color.parseColor("#0984E3"), Color.parseColor("#74B9FF"))
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
        gameOverMessage = Messages.getGameOverMessage(state.score, state.level)
        gameOverScoreDisplay = 0
        gameOverScoreTimer = 0f
        buzzHaptic(150)  // long buzz on death
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
            x = Random.nextFloat() * max(screenW, 1920),
            y = Random.nextFloat() * max(screenH, 1080),
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
            y -= 100f * dt  // float up faster
            life -= dt * 1.0f  // last a bit longer
            alpha = life.coerceIn(0f, 1f)
            scale = scale + (1f - life) * 0.2f  // grow as they fade
        }
        fun isAlive() = life > 0f
    }
}
