package com.sauravupd.twitchbubbles

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {

    var running: Boolean = false
    private val targetFPS = 60
    private val targetTime = 1000L / targetFPS

    override fun run() {
        var lastTime = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            val dt = ((now - lastTime) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
            lastTime = now

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.updateGame(dt)
                        gameView.drawGame(canvas)
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    canvas?.let { surfaceHolder.unlockCanvasAndPost(it) }
                } catch (_: Exception) {
                }
            }

            val elapsed = (System.nanoTime() - now) / 1_000_000
            val sleepTime = targetTime - elapsed
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime)
                } catch (_: InterruptedException) {
                }
            }
        }
    }
}
