package com.sauravupd.twitchbubbles

data class GameState(
    var score: Int = 0,
    var lives: Int = 3,
    var maxLives: Int = 5,
    var level: Int = 1,
    var popsThisLevel: Int = 0,
    var popsPerLevel: Int = 12,
    var bestScore: Int = 0,
    var isGameOver: Boolean = false,
    var isStartScreen: Boolean = true,
    var comboCount: Int = 0,
    var comboTimer: Float = 0f,
    var comboTimeWindow: Float = 2.0f,
    var screenShakeAmount: Float = 0f,
    var totalPops: Int = 0,
    var totalMissed: Int = 0,
    var maxCombo: Int = 0
) {
    // Start with 2 bubbles, ramp up to 8
    val maxBubbles: Int get() = (2 + (level - 1)).coerceAtMost(8)

    // Easier start: slower shrink at level 1, ramps harder
    val shrinkRate: Float get() = 35f + (level - 1) * 8f

    // Spawn interval: gentler start, still gets intense
    val spawnInterval: Float get() = (1.2f - (level - 1) * 0.08f).coerceAtLeast(0.35f)

    val lifeBubbleChance: Float
        get() = when {
            level <= 1 -> 0.05f    // small chance even at level 1 for early reward
            level <= 3 -> 0.12f
            level <= 5 -> 0.14f
            else -> 0.16f
        }

    // BIG bubbles at start, shrink with level
    val bubbleMaxRadius: Float get() = (95f - (level - 1) * 5f).coerceAtLeast(50f)

    fun onPop() {
        popsThisLevel++
        totalPops++
        if (popsThisLevel >= popsPerLevel) {
            level++
            popsThisLevel = 0
        }
    }

    fun loseLife() {
        lives--
        totalMissed++
        if (lives <= 0) {
            lives = 0
            isGameOver = true
        }
    }

    fun gainLife() {
        if (lives < maxLives) {
            lives++
        }
    }

    fun reset() {
        score = 0
        lives = 3
        level = 1
        popsThisLevel = 0
        totalPops = 0
        totalMissed = 0
        maxCombo = 0
        isGameOver = false
        isStartScreen = false
        comboCount = 0
        comboTimer = 0f
        screenShakeAmount = 0f
    }

    fun updateCombo(dt: Float) {
        if (comboTimer > 0) {
            comboTimer -= dt
            if (comboTimer <= 0f) {
                comboCount = 0
            }
        }
    }

    fun addCombo() {
        comboCount++
        comboTimer = comboTimeWindow
        if (comboCount > maxCombo) {
            maxCombo = comboCount
        }
    }

    fun getScoreMultiplier(): Int {
        return when {
            comboCount >= 10 -> 4
            comboCount >= 5 -> 3
            comboCount >= 3 -> 2
            else -> 1
        }
    }
}
