package com.jondude.stardustranch.game

import android.content.Context
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

/** Owns the game thread: update, render, and touch processing all happen on it. */
class GameView(ctx: Context, host: Ui.Host) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {
    val game = Game(File(ctx.filesDir, "stardust_save.json"))
    private val art = Art()
    val ui = Ui(game, art, host)
    private val events = ConcurrentLinkedQueue<TouchEv>()
    @Volatile private var running = false
    private var thread: Thread? = null
    @Volatile private var backPressed = false

    init { holder.addCallback(this); isFocusable = true }

    override fun surfaceCreated(h: SurfaceHolder) { start() }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) { synchronized(this) { ui.layout(w, hh) } }
    override fun surfaceDestroyed(h: SurfaceHolder) { stop() }

    fun start() {
        if (running) return
        running = true
        thread = Thread(this, "game").also { it.start() }
    }

    fun stop() {
        running = false
        try { thread?.join(500) } catch (_: InterruptedException) {}
        thread = null
    }

    fun onBack() { backPressed = true }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val am = e.actionMasked
        when (am) {
            MotionEvent.ACTION_MOVE -> for (i in 0 until e.pointerCount) events.add(TouchEv(2, e.getPointerId(i), e.getX(i), e.getY(i)))
            else -> {
                val i = e.actionIndex
                events.add(TouchEv(am, e.getPointerId(i), e.getX(i), e.getY(i)))
            }
        }
        return true
    }

    override fun run() {
        var last = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            val dt = ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            synchronized(this) {
                while (true) { val e = events.poll() ?: break; ui.touch(e) }
                if (backPressed) { backPressed = false; ui.back() }
                ui.tick(dt)
                game.update(dt, ui.joyX, ui.joyY)
            }
            val h = holder
            val c = try { h.lockCanvas() } catch (_: Exception) { null }
            if (c != null) {
                try { synchronized(this) { ui.draw(c) } } finally { try { h.unlockCanvasAndPost(c) } catch (_: Exception) {} }
            }
            val spent = (System.nanoTime() - now) / 1_000_000
            if (spent < 16) try { Thread.sleep(16 - spent) } catch (_: InterruptedException) {}
        }
    }
}
