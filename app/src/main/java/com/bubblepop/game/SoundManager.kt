package com.bubblepop.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import kotlin.random.Random

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val popSoundId: Int
    private val lifeSoundId: Int
    private val missSoundId: Int
    private val gameOverSoundId: Int
    private val comboSoundId: Int
    
    // Programmatic music IDs (using existing sounds or variants if possible)
    // We'll use miss for kick (lowered pitch), pop for hi-hat (higher pitch)
    private val kickId: Int
    private val hiHatId: Int
    private val bassId: Int

    // Music Loop State
    private val handler = Handler(Looper.getMainLooper())
    private var beatTask: Runnable? = null
    private var beatCount = 0
    private var isMusicRunning = false

    // Settings
    var musicEnabled: Boolean = true
        set(value) {
            field = value
            if (value) startMusic() else stopMusic()
        }
    var sfxEnabled: Boolean = true

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(15) // Bumped for programmatic loop
            .setAudioAttributes(audioAttributes)
            .build()

        popSoundId = soundPool.load(context, R.raw.pop, 1)
        lifeSoundId = soundPool.load(context, R.raw.life_gain, 1)
        missSoundId = soundPool.load(context, R.raw.miss, 1)
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1)
        comboSoundId = soundPool.load(context, R.raw.combo, 1)
        
        // Music components
        kickId = soundPool.load(context, R.raw.miss, 1)
        hiHatId = soundPool.load(context, R.raw.pop, 1)
        bassId = soundPool.load(context, R.raw.combo, 1)
    }

    fun playPop(pitch: Float = 1.0f) {
        if (!sfxEnabled) return
        soundPool.play(popSoundId, 0.8f, 0.8f, 1, 0, pitch.coerceIn(0.5f, 2.0f))
    }

    fun playLifeGain() {
        if (!sfxEnabled) return
        soundPool.play(lifeSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
    }

    fun playMiss() {
        if (!sfxEnabled) return
        soundPool.play(missSoundId, 0.6f, 0.6f, 1, 0, 1.0f)
    }

    fun playGameOver() {
        if (!sfxEnabled) return
        soundPool.play(gameOverSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun playCombo() {
        if (!sfxEnabled) return
        soundPool.play(comboSoundId, 0.7f, 0.7f, 1, 0, 1.2f)
    }

    fun startMusic() {
        if (!musicEnabled || isMusicRunning) return
        isMusicRunning = true
        
        val tempo = 125
        val beatDuration = (60000 / tempo / 2).toLong() // 8th notes

        beatTask = object : Runnable {
            override fun run() {
                if (!isMusicRunning) return

                val time = System.currentTimeMillis()
                
                // Kick on every 1st and 3rd 8th note (4/4)
                if (beatCount % 2 == 0) {
                    playKick()
                }
                
                // Hi-hat on offbeats
                if (beatCount % 2 == 1) {
                    playHiHat()
                }
                
                // Bassline logic from audioService.ts
                if (beatCount % 4 == 0 || beatCount % 16 == 7) {
                    val steps = listOf(0.5f, 0.5f, 0.5f, 0.45f, 0.5f, 0.55f, 0.5f, 0.6f)
                    val pitch = steps[(beatCount / 4) % steps.size]
                    playBass(pitch)
                }

                beatCount = (beatCount + 1) % 32
                handler.postDelayed(this, beatDuration)
            }
        }
        handler.post(beatTask!!)
    }

    private fun playKick() {
        if (!musicEnabled) return
        soundPool.play(kickId, 0.35f, 0.35f, 0, 0, 0.5f) // Low pitched miss
    }

    private fun playHiHat() {
        if (!musicEnabled) return
        soundPool.play(hiHatId, 0.08f, 0.08f, 0, 0, 2.0f) // Very high pop
    }

    private fun playBass(pitch: Float) {
        if (!musicEnabled) return
        soundPool.play(bassId, 0.12f, 0.12f, 0, 0, pitch)
    }

    fun stopMusic() {
        isMusicRunning = false
        beatTask?.let { handler.removeCallbacks(it) }
        beatTask = null
    }

    fun pauseMusic() = stopMusic()

    fun release() {
        stopMusic()
        soundPool.release()
    }
}
