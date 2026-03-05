package com.bubblepop.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val popSoundId: Int
    private val lifeSoundId: Int
    private val missSoundId: Int
    private val gameOverSoundId: Int
    private val comboSoundId: Int

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        popSoundId = soundPool.load(context, R.raw.pop, 1)
        lifeSoundId = soundPool.load(context, R.raw.life_gain, 1)
        missSoundId = soundPool.load(context, R.raw.miss, 1)
        gameOverSoundId = soundPool.load(context, R.raw.game_over, 1)
        comboSoundId = soundPool.load(context, R.raw.combo, 1)
    }

    fun playPop(pitch: Float = 1.0f) {
        soundPool.play(popSoundId, 0.8f, 0.8f, 1, 0, pitch.coerceIn(0.5f, 2.0f))
    }

    fun playLifeGain() {
        soundPool.play(lifeSoundId, 0.9f, 0.9f, 1, 0, 1.0f)
    }

    fun playMiss() {
        soundPool.play(missSoundId, 0.6f, 0.6f, 1, 0, 1.0f)
    }

    fun playGameOver() {
        soundPool.play(gameOverSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    fun playCombo() {
        soundPool.play(comboSoundId, 0.7f, 0.7f, 1, 0, 1.2f)
    }

    fun release() {
        soundPool.release()
    }
}
