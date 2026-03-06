package com.sauravupd.twitchbubbles

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var radius: Float,
    var color: Int,
    var alpha: Float = 1f,
    var life: Float = 1f,
    var decay: Float = 2.5f
) {
    fun update(dt: Float) {
        x += vx * dt
        y += vy * dt
        vy += 200f * dt  // gravity
        life -= decay * dt
        alpha = life.coerceIn(0f, 1f)
        radius *= (1f - 0.5f * dt)
    }

    fun isAlive(): Boolean = life > 0f
}
