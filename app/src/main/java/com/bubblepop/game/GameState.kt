package com.bubblepop.game

data class GameState(
    var score: Int = 0,
    var lives: Int = 3,
    var maxLives: Int = 5,
    var level: Int = 1,
    var popsThisLevel: Int = 0,
    var popsPerLevel: Int = 10,
    var bestScore: Int = 0,
    var isGameOver: Boolean = false,
    var isStartScreen: Boolean = true,
    var comboCount: Int = 0,
    var comboTimer: Float = 0f,
    var comboTimeWindow: Float = 1.5f,
    var screenShakeAmount: Float = 0f
) {
    // Difficulty parameters derived from level
    val maxBubbles: Int get() = (level).coerceAtMost(5)

    val shrinkRate: Float get() = 30f + (level - 1) * 8f

    val spawnInterval: Float get() = (2.0f - (level - 1) * 0.15f).coerceAtLeast(0.6f)

    val lifeBubbleChance: Float
        get() = when {
            level <= 1 -> 0f
            level <= 3 -> 0.10f
            level <= 5 -> 0.12f
            else -> 0.15f
        }

    val bubbleMaxRadius: Float get() = (90f - (level - 1) * 5f).coerceAtLeast(50f)

    fun onPop() {
        popsThisLevel++
        if (popsThisLevel >= popsPerLevel) {
            level++
            popsThisLevel = 0
        }
    }

    fun loseLife() {
        lives--
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
