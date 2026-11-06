package com.jondude.stardustranch.game

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.jondude.stardustranch.update.Updater
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class TouchEv(val action: Int, val id: Int, val x: Float, val y: Float)

/** Draws the world, HUD, menus and battles, and handles touch. Immediate-mode buttons. */
class Ui(val g: Game, val art: Art, val host: Host) {
    private val terrain = Terrain(art)
    interface Host { fun installUpdate(r: Updater.Release); fun checkUpdate() }

    var W = 1; var H = 1
    var sc = 4; var T = 64
    var u = 1f

    private class Hit(val r: RectF, val act: () -> Unit)
    private val hits = ArrayList<Hit>()
    private val pending = ArrayList<Hit>()

    // controls
    var joyId = -1; var joyOx = 0f; var joyOy = 0f; var joyX = 0f; var joyY = 0f
    var aId = -1; var aHold = 0f
    private val aBtn = RectF(); private val bBtn = RectF()
    var updateDismissed = false
    private var listOff = 0
    private var dexPick = -1
    private var hotRow = 0

    // paints
    private val px = Paint().apply { isFilterBitmap = false; isAntiAlias = false }
    /** Tree canopies fade when the player walks behind them so the player is never lost in the foliage. */
    private val canopyFade = Paint().apply { isFilterBitmap = false; isAntiAlias = false; alpha = 120 }
    private val fill = Paint().apply { isAntiAlias = true }
    private val stroke = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
    private val txt = Paint().apply { isAntiAlias = true; typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD) }
    private val sil = Paint().apply { isFilterBitmap = false; colorFilter = ColorMatrixColorFilter(ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 40f, 0f, 0f, 0f, 0f, 40f, 0f, 0f, 0f, 0f, 60f, 0f, 0f, 0f, 1f, 0f))) }
    private val dst = RectF()
    private val tmpR = Rect()

    private val CREAM = 0xFFF6E7C4.toInt()
    private val WOODC = 0xFF6E4422.toInt()
    private val INK = 0xFF3A2416.toInt()
    private val GOLD = 0xFFF2C842.toInt()

    fun layout(w: Int, h: Int) {
        W = w; H = h
        sc = max(2, Math.round(h / (16f * 11f)))
        T = 16 * sc
        u = h / 360f
        val ab = 62 * u
        aBtn.set(W - ab - 18 * u, H - ab - 34 * u, W - 18 * u, H - 34 * u)
        bBtn.set(W - ab * 0.62f - 26 * u - ab, H - ab * 0.62f - 72 * u, W - 26 * u - ab, H - 72 * u)
    }

    // ======================================================================
    // Input
    // ======================================================================

    fun touch(e: TouchEv) {
        val down = e.action == 0 || e.action == 5
        val up = e.action == 1 || e.action == 6 || e.action == 3
        if (g.mode == Mode.FISH) {
            // taps land on press for snappy timing; the "Let go" button still works
            joyId = -1; joyX = 0f; joyY = 0f; aId = -1
            if (down) { if (fishQuit.contains(e.x, e.y)) g.fishCancel() else g.fishTap() }
            return
        }
        if (g.mode != Mode.PLAY) {
            joyId = -1; joyX = 0f; joyY = 0f; aId = -1
            if (up) tapAt(e.x, e.y)
            return
        }
        when {
            down -> {
                if (aBtn.contains(e.x, e.y) || hypot(e.x - aBtn.centerX(), e.y - aBtn.centerY()) < aBtn.width() * 0.7f) {
                    aId = e.id; aHold = 0f; g.action(); return
                }
                // HUD buttons first
                for (h in hits) if (h.r.contains(e.x, e.y)) { h.act(); return }
                if (e.x < W * 0.55f && joyId < 0) { joyId = e.id; joyOx = e.x; joyOy = e.y; joyX = 0f; joyY = 0f }
            }
            e.action == 2 -> {
                if (e.id == joyId) {
                    val r = 46 * u
                    var dx = (e.x - joyOx) / r; var dy = (e.y - joyOy) / r
                    val m = hypot(dx, dy)
                    if (m > 1f) { joyOx += (e.x - joyOx) * (1 - 1 / m) * 0.5f; joyOy += (e.y - joyOy) * (1 - 1 / m) * 0.5f; dx /= m; dy /= m }
                    joyX = dx; joyY = dy
                }
            }
            up -> {
                if (e.id == joyId) { joyId = -1; joyX = 0f; joyY = 0f }
                if (e.id == aId) aId = -1
            }
        }
    }

    private fun tapAt(x: Float, y: Float) {
        for (i in hits.indices.reversed()) if (hits[i].r.contains(x, y)) { hits[i].act(); return }
    }

    fun tick(dt: Float) {
        if (aId >= 0 && g.mode == Mode.PLAY) {
            aHold += dt
            if (aHold > 0.45f) { aHold = 0.2f; val h = g.held; if (h != null && Items[h.id].kind == Kind.TOOL) g.action() }
        }
    }

    fun back() {
        when (g.mode) {
            Mode.PLAY -> { g.mode = Mode.MENU; g.menuSel = -1 }
            Mode.MENU, Mode.SHOP, Mode.SMITH, Mode.RANCH, Mode.ELEVATOR -> g.mode = Mode.PLAY
            Mode.DIALOG -> closeDialog()
            Mode.FISH -> g.fishCancel()
            else -> {}
        }
    }

    private fun btn(l: Float, t: Float, r: Float, b: Float, act: () -> Unit) { pending.add(Hit(RectF(l, t, r, b), act)) }

    // ======================================================================
    // Draw
    // ======================================================================

    fun draw(c: Canvas) {
        pending.clear()
        if (g.mode == Mode.BATTLE && g.battle != null) drawBattle(c)
        else {
            drawWorld(c)
            drawHud(c)
            when (g.mode) {
                Mode.DIALOG -> drawDialog(c)
                Mode.MENU -> drawMenu(c)
                Mode.SHOP -> drawShop(c)
                Mode.SMITH -> drawSmith(c)
                Mode.RANCH -> drawRanch(c)
                Mode.ELEVATOR -> drawElevator(c)
                Mode.STARTER -> drawStarter(c)
                Mode.REPORT -> drawReport(c)
                Mode.FISH -> drawFishing(c)
                else -> {}
            }
        }
        hits.clear(); hits.addAll(pending)
    }

    // ---------------------------------------------------------------- world

    private var camX = 0f; private var camY = 0f

    private fun bmp(c: Canvas, b: Bitmap, x: Float, y: Float, w: Float = b.width * sc.toFloat(), h: Float = b.height * sc.toFloat(), p: Paint = px) {
        dst.set(x, y, x + w, y + h); c.drawBitmap(b, null, dst, p)
    }

    private fun drawWorld(c: Canvas) {
        val a = g.cur
        val mapW = a.w * T; val mapH = a.h * T
        camX = if (mapW <= W) (mapW - W) / 2f else (g.px * T - W / 2f).coerceIn(0f, (mapW - W).toFloat())
        camY = if (mapH <= H) (mapH - H) / 2f else (g.py * T - H / 2f).coerceIn(0f, (mapH - H).toFloat())
        camX = floor(camX); camY = floor(camY)
        c.drawColor(if (a.dark) 0xFF0C0806.toInt() else 0xFF101010.toInt())
        val x0 = max(0, (camX / T).toInt() - 1); val x1 = min(a.w - 1, ((camX + W) / T).toInt() + 1)
        val y0 = max(0, (camY / T).toInt() - 1); val y1 = min(a.h - 1, ((camY + H) / T).toInt() + 1)
        val wf = ((g.frame * 2f).toInt()) % 3
        val season = g.season
        for (y in y0..y1) for (x in x0..x1) {
            val b = terrain.tile(a, x, y, season, wf)
            val sx = x * T - camX; val sy = y * T - camY
            if (b != null) bmp(c, b, sx, sy, T.toFloat(), T.toFloat())
            else { fill.color = Color.BLACK; c.drawRect(sx, sy, sx + T, sy + T, fill) }
        }
        // rows painter
        val rowEnd = min(a.h - 1, y1 + 6)
        for (y in y0..rowEnd) {
            if (y <= y1 + 5) for (x in x0..x1) drawCell(c, a, x, y)
            for ((bi, b) in a.buildings.withIndex()) if (b.y + b.th - 1 == y) {
                val bm = if (b.type == Bld.COTTAGE) art.cottage(bi) else art.buildings[b.type] ?: continue
                val bw = bm.width * sc.toFloat(); val bh = bm.height * sc.toFloat()
                bmp(c, bm, b.x * T - camX + (b.tw * T - bw) / 2, (b.y + b.th) * T - camY - bh)
            }
            drawEntitiesInRow(c, a, y)
        }
        // toasts
        for (t in g.toasts) {
            txt.textSize = 13 * u; txt.textAlign = Paint.Align.CENTER
            val x = t.x * T - camX; val yy = t.y * T - camY
            txt.color = 0xC0000000.toInt(); c.drawText(t.text, x + 1.5f * u, yy + 1.5f * u, txt)
            txt.color = t.color; c.drawText(t.text, x, yy, txt)
        }
        // lighting
        if (a.dark) {
            val cx = g.px * T - camX; val cy = g.py * T - camY
            val r = T * (if (g.leadJob == Job.LUCK || g.lead?.sp?.type == EType.FLAME) 5.5f else 4.2f)
            fill.shader = RadialGradient(cx, cy - T * 0.3f, r, intArrayOf(0x00000000, 0x55000000, 0xF0000000.toInt()), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill); fill.shader = null
        } else if (!a.indoor) {
            val t = ((g.time - 1080f) / 240f).coerceIn(0f, 1f) * 0.6f + (if (g.raining) 0.12f else 0f)
            if (t > 0.01f) { fill.color = Color.argb((t * 255).toInt(), 16, 22, 60); c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill) }
            if (g.raining || g.season == 3) drawWeather(c)
        }
    }

    private fun drawWeather(c: Canvas) {
        val snow = g.season == 3
        val n = if (snow) 70 else 110
        stroke.strokeWidth = 1.5f * u
        for (i in 0 until n) {
            val seed = i * 9973
            val sx = ((seed % W) + g.frame * (if (snow) 20f else -160f) * u + (seed % 37) * 10) % W
            val sy = ((seed * 7 % H) + g.frame * (if (snow) 40f else 600f) * u) % H
            val x = if (sx < 0) sx + W else sx
            if (snow) { fill.color = 0xD0FFFFFF.toInt(); c.drawCircle(x + sin(g.frame + i) * 6 * u, sy, 2f * u, fill) }
            else { stroke.color = 0x90BFD8FF.toInt(); c.drawLine(x, sy, x - 4 * u, sy + 14 * u, stroke) }
        }
    }

    private fun drawCell(c: Canvas, a: Area, x: Int, y: Int) {
        val i = a.idx(x, y)
        val sx = x * T - camX; val sy = y * T - camY
        val crop = a.crops[i]
        if (crop != null) {
            val b = art.crop(crop.type, crop.stage)
            bmp(c, b, sx, sy + T - b.height * sc)
        }
        val o = a.obj[i]
        if (o == Ob.NONE || o == Ob.BLOCK) return
        val s = g.season
        val b: Bitmap = when (o) {
            Ob.TREE -> art.tree[s]; Ob.PINE -> art.pine[s]; Ob.ROCK -> art.rock; Ob.BOULDER -> art.boulder
            Ob.COPPER -> art.copperRock; Ob.IRON -> art.ironRock; Ob.GOLD -> art.goldRock; Ob.GEM -> art.gemRock; Ob.COALROCK -> art.coalRock
            Ob.WEED -> art.weed[s]; Ob.STICK -> art.stick; Ob.BUSH -> art.bush[s]; Ob.TALLGRASS -> art.tallGrass[s]
            Ob.FORAGE -> art.item(a.aux[i]); Ob.FENCE -> art.fence; Ob.BIN -> art.bin; Ob.BOX -> art.box
            Ob.COUNTER -> art.counter; Ob.HEALER -> art.healer; Ob.ELEVATOR -> art.elevator; Ob.LADDER -> art.ladder; Ob.LADDERUP -> art.ladderUp
            Ob.SPRINKLER -> art.item(Items.SPRINKLER); Ob.QSPRINKLER -> art.item(Items.QSPRINKLER); Ob.FLOWER -> art.flower[a.aux[i] % 3]
            Ob.TABLE -> art.table; Ob.ANVIL -> art.anvil; Ob.SHELF -> art.shelf; Ob.CHEST -> art.chest
            Ob.BED -> { if (a.o(x, y - 1) == Ob.BED) return; art.bed }
            Ob.FOUNTAIN -> { if (a.o(x - 1, y) == Ob.FOUNTAIN || a.o(x, y + 1) == Ob.FOUNTAIN) return; art.fountain[((g.frame * 4).toInt()) % 3] }
            else -> return
        }
        val bw = b.width * sc.toFloat(); val bh = b.height * sc.toFloat()
        when (o) {
            Ob.BED -> bmp(c, b, sx, sy)
            Ob.FOUNTAIN -> bmp(c, b, sx, sy - T)
            Ob.FORAGE -> { val bob = sin(g.frame * 3 + x) * sc; bmp(c, b, sx + T * 0.15f, sy + T * 0.1f + bob, T * 0.7f, T * 0.7f) }
            Ob.TREE, Ob.PINE -> {
                val behind = abs(g.px - (x + 0.5f)) < b.width / 32f + 0.3f && g.py < y + 0.2f && g.py > y + 1f - b.height / 16f + 0.5f
                bmp(c, b, sx + (T - bw) / 2, sy + T - bh, p = if (behind) canopyFade else px)
            }
            else -> bmp(c, b, sx + (T - bw) / 2, sy + T - bh)
        }
    }

    private fun drawShadow(c: Canvas, x: Float, y: Float, w: Float) {
        fill.color = 0x40000000; c.drawOval(x - w / 2, y - w * 0.18f, x + w / 2, y + w * 0.18f, fill)
    }

    private fun drawMonWorld(c: Canvas, m: Mon, x: Float, y: Float, flip: Boolean, anim: Float, scale: Float = 0.85f) {
        val f = art.mon(m.speciesId)
        val fr = if ((anim * 3).toInt() % 2 == 0) 0 else 1
        val b = f[fr + if (flip) 2 else 0]
        val w = 32 * sc * scale
        val sx = x * T - camX; val sy = y * T - camY
        drawShadow(c, sx, sy, w * 0.45f)
        bmp(c, b, sx - w / 2, sy - w + 3 * sc, w, w)
    }

    private fun drawEntitiesInRow(c: Canvas, a: Area, row: Int) {
        if (a.id == "farm") for (p in g.pen) if (floor(p.y).toInt() == row) drawMonWorld(c, p.mon, p.x, p.y, p.flip, g.frame + p.x, 0.75f)
        for (n in g.npcs) if (n.area == a.id && floor(n.y).toInt() == row) {
            val fr = People.npc[n.key]!!
            val sx = n.x * T - camX; val sy = n.y * T - camY
            drawShadow(c, sx, sy, T * 0.6f)
            bmp(c, fr[n.dir][0], sx - 8 * sc, sy - 29 * sc)
        }
        for (w in a.wilds) if (floor(w.y).toInt() == row) {
            drawMonWorld(c, w.mon, w.x, w.y, w.flip, w.anim, if (w.mon.speciesId == 35) 1.1f else 0.8f)
            if (w.mon.speciesId !in g.dexCaught) {} else { fill.color = 0xFFE24848.toInt(); c.drawCircle(w.x * T - camX + T * 0.35f, w.y * T - camY - T * 1.1f, 3 * u, fill) }
        }
        for (gl in a.gloops) if (floor(gl.y).toInt() == row) {
            val fr = art.gloop(gl.color)[((gl.anim * 3).toInt()) % 2]
            val sx = gl.x * T - camX; val sy = gl.y * T - camY
            drawShadow(c, sx, sy, T * 0.6f)
            if (gl.hurt > 0 && ((gl.hurt * 20).toInt() % 2 == 0)) continue
            bmp(c, fr, sx - 8 * sc, sy - 14 * sc)
        }
        val lead = g.party.firstOrNull()
        if (lead != null && !lead.fainted && floor(g.fy).toInt() == row && g.cur.id != "store") drawMonWorld(c, lead, g.fx, g.fy, g.fflip, g.frame + g.walkT * 0.3f, 0.7f)
        if (floor(g.py).toInt() == row) drawPlayer(c)
    }

    private fun drawPlayer(c: Canvas) {
        val sx = g.px * T - camX; val sy = g.py * T - camY
        val fr = if (g.moving) 1 + (g.walkT * 4.4f).toInt() % 4 else 0
        drawShadow(c, sx, sy, T * 0.6f)
        if (g.hurtT > 0 && ((g.hurtT * 16).toInt() % 2 == 0)) return
        // tool swing behind when facing up
        if (g.swingT > 0 && g.dir == 1) drawSwing(c, sx, sy)
        bmp(c, People.player[g.dir][fr], sx - 8 * sc, sy - 29 * sc)
        if (g.swingT > 0 && g.dir != 1) drawSwing(c, sx, sy)
        if (g.fishing != null) drawLine(c, sx, sy)
    }

    private fun drawSwing(c: Canvas, sx: Float, sy: Float) {
        val t = g.swingTool; if (t < 0) return
        val b = art.item(t)
        val k = 1f - g.swingT / 0.25f
        val (dx, dy) = when (g.dir) { 0 -> 0f to 0.5f; 1 -> 0f to -0.9f; 2 -> -0.6f to -0.3f; else -> 0.6f to -0.3f }
        c.save()
        val cx = sx + dx * T; val cy = sy + dy * T - T * 0.3f
        c.rotate((k - 0.5f) * 110f * (if (g.dir == 2) -1 else 1), cx, cy)
        bmp(c, b, cx - T * 0.35f, cy - T * 0.35f, T * 0.7f, T * 0.7f)
        c.restore()
    }

    /** Fishing line from the rod tip to the bobbing float. */
    private fun drawLine(c: Canvas, sx: Float, sy: Float) {
        val f = g.fishing ?: return
        val (dx, dy) = when (g.dir) { 0 -> 0.15f to 0.1f; 1 -> 0.15f to -1.3f; 2 -> -0.75f to -1.25f; else -> 0.75f to -1.25f }
        val tipX = sx + dx * T; val tipY = sy + dy * T
        val bob = if (f.phase == 1) 2.5f * sc else sin(f.anim * 3f) * 0.8f * sc
        val bx = (f.bx + 0.5f) * T - camX; val by = (f.by + 0.5f) * T - camY + bob
        // rod
        stroke.color = 0xFF5A3A22.toInt(); stroke.strokeWidth = 2.2f * sc
        c.drawLine(sx + dx * T * 0.3f, sy - T * 0.7f, tipX, tipY, stroke)
        // ripples
        stroke.strokeWidth = 0.8f * sc
        for (k in 0..1) {
            val ph = ((f.anim * 0.8f + k * 0.5f) % 1f)
            stroke.color = Color.argb(((1f - ph) * (if (f.phase == 1) 230 else 140)).toInt(), 230, 245, 255)
            val rw = T * (0.25f + ph * 0.45f); c.drawOval(bx - rw, by + 2 * sc - rw * 0.4f, bx + rw, by + 2 * sc + rw * 0.4f, stroke)
        }
        // line with a gentle sag
        stroke.color = 0xCCEFEFF6.toInt(); stroke.strokeWidth = 0.6f * sc
        val mx = (tipX + bx) / 2; val my = max(tipY, by) + T * 0.25f
        var lx = tipX; var ly = tipY
        for (i in 1..10) {
            val t = i / 10f; val a = 1 - t
            val nx = a * a * tipX + 2 * a * t * mx + t * t * bx; val ny = a * a * tipY + 2 * a * t * my + t * t * (by - 3 * sc)
            c.drawLine(lx, ly, nx, ny, stroke); lx = nx; ly = ny
        }
        bmp(c, art.bobber, bx - 4 * sc, by - 5 * sc)
        if (f.phase == 1) {
            val pop = 1f + sin(f.anim * 30f) * 0.08f
            panel(c, sx - 14 * u * pop, sy - T * 2.4f - 30 * u * pop, sx + 14 * u * pop, sy - T * 2.4f)
            text(c, "!", sx, sy - T * 2.4f - 7 * u, 22f, 0xFFC0302A.toInt(), Paint.Align.CENTER)
        }
    }

    // ---------------------------------------------------------------- fishing overlay

    private val fishQuit = RectF()

    private fun drawFishing(c: Canvas) {
        val f = g.fishing ?: return
        fishQuit.set(W - 110 * u, H - 64 * u, W - 18 * u, H - 30 * u)
        if (f.phase < 2) {
            val w = 300 * u; val l = W / 2f - w / 2; val t = H - 120 * u
            panel(c, l, t, l + w, t + 44 * u)
            text(c, if (f.phase == 0) "Waiting for a bite... (tap the ! when it appears)" else "A bite! TAP NOW!", W / 2f, t + 28 * u,
                if (f.phase == 0) 12f else 16f, if (f.phase == 0) INK else 0xFFC0302A.toInt(), Paint.Align.CENTER)
            button(c, fishQuit.left, fishQuit.top, fishQuit.right, fishQuit.bottom, "Reel in", col = 0xFF9A7A5A.toInt(), size = 12f) {}
            return
        }
        val fish = f.fish ?: return
        fill.color = 0x60000000; c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill)
        val cx = W / 2f; val cy = H * 0.46f; val R = 78 * u
        // dial body
        val shake = if (f.flash > 0 && !f.lastHit) sin(f.flash * 80f) * 4 * u else 0f
        panel(c, cx - R - 30 * u + shake, cy - R - 52 * u, cx + R + 30 * u + shake, cy + R + 64 * u)
        text(c, "Reel it in!", cx + shake, cy - R - 28 * u, 16f, 0xFF8E3A1E.toInt(), Paint.Align.CENTER)
        val ccx = cx + shake
        fill.color = 0xFF3E5A7A.toInt(); c.drawCircle(ccx, cy, R, fill)
        fill.color = 0xFF5A86AE.toInt(); c.drawCircle(ccx, cy, R * 0.86f, fill)
        fill.color = 0x30FFFFFF; c.drawCircle(ccx - R * 0.2f, cy - R * 0.2f, R * 0.6f, fill)
        // track + green zone
        val ring = RectF(ccx - R * 0.93f, cy - R * 0.93f, ccx + R * 0.93f, cy + R * 0.93f)
        stroke.strokeWidth = 12 * u; stroke.color = 0xFF2A3E58.toInt(); c.drawArc(ring, 0f, 360f, false, stroke)
        stroke.color = if (f.flash > 0 && f.lastHit) 0xFFB8F08A.toInt() else 0xFF5DD45A.toInt()
        c.drawArc(ring, f.zone - f.zoneW / 2f, f.zoneW, false, stroke)
        stroke.strokeWidth = 2 * u; stroke.color = 0xFF2E7A2A.toInt()
        c.drawArc(RectF(ring.left - 6 * u, ring.top - 6 * u, ring.right + 6 * u, ring.bottom + 6 * u), f.zone - f.zoneW / 2f, f.zoneW, false, stroke)
        // needle
        val rad = Math.toRadians(f.angle.toDouble())
        val nx = ccx + (Math.cos(rad) * R * 0.98f).toFloat(); val ny = cy + (Math.sin(rad) * R * 0.98f).toFloat()
        stroke.strokeWidth = 4 * u; stroke.color = 0xFFF4EEE0.toInt(); c.drawLine(ccx, cy, nx, ny, stroke)
        stroke.strokeWidth = 2 * u; stroke.color = 0xFFC0302A.toInt(); c.drawLine(ccx, cy, nx, ny, stroke)
        // hub shows the fish's shadow until it's landed
        fill.color = 0xFFE8D2A2.toInt(); c.drawCircle(ccx, cy, 24 * u, fill)
        bmp(c, art.item(fish.id), ccx - 20 * u, cy - 20 * u, 40 * u, 40 * u, sil)
        // progress pips and line strain
        val py = cy + R + 20 * u
        for (i in 0 until f.need) {
            fill.color = if (i < f.hits) 0xFF5DD45A.toInt() else 0xFFB8A888.toInt()
            c.drawCircle(cx - (f.need - 1) * 9 * u + i * 18 * u, py, 6 * u, fill)
        }
        for (i in 0 until 3) text(c, if (i < f.misses) "✕" else "·", cx - 18 * u + i * 18 * u, py + 26 * u, 14f, if (i < f.misses) 0xFFC0302A.toInt() else 0xFF8A7A62.toInt(), Paint.Align.CENTER)
        text(c, "Tap anywhere when the needle is in the green", cx, cy + R + 82 * u, 12f, Color.WHITE, Paint.Align.CENTER, true)
        button(c, fishQuit.left, fishQuit.top, fishQuit.right, fishQuit.bottom, "Let go", col = 0xFF9A7A5A.toInt(), size = 12f) {}
    }

    // ---------------------------------------------------------------- HUD

    private fun panel(c: Canvas, l: Float, t: Float, r: Float, b: Float, col: Int = CREAM) {
        fill.color = 0x60000000; c.drawRoundRect(l + 3 * u, t + 4 * u, r + 3 * u, b + 4 * u, 8 * u, 8 * u, fill)
        fill.color = WOODC; c.drawRoundRect(l, t, r, b, 8 * u, 8 * u, fill)
        fill.color = col; c.drawRoundRect(l + 3 * u, t + 3 * u, r - 3 * u, b - 3 * u, 6 * u, 6 * u, fill)
    }

    private fun text(c: Canvas, s: String, x: Float, y: Float, size: Float, col: Int = INK, align: Paint.Align = Paint.Align.LEFT, shadow: Boolean = false) {
        txt.textSize = size * u; txt.textAlign = align
        if (shadow) { txt.color = 0xA0000000.toInt(); c.drawText(s, x + 1.2f * u, y + 1.2f * u, txt) }
        txt.color = col; c.drawText(s, x, y, txt)
    }

    private fun wrap(s: String, width: Float, size: Float): List<String> {
        txt.textSize = size * u
        val out = ArrayList<String>()
        for (para in s.split("\n")) {
            var line = ""
            for (w in para.split(" ")) {
                val t = if (line.isEmpty()) w else "$line $w"
                if (txt.measureText(t) > width && line.isNotEmpty()) { out.add(line); line = w } else line = t
            }
            out.add(line)
        }
        return out
    }

    private fun button(c: Canvas, l: Float, t: Float, r: Float, b: Float, label: String, enabled: Boolean = true, col: Int = 0xFF5DA342.toInt(), size: Float = 13f, act: () -> Unit) {
        fill.color = 0x50000000; c.drawRoundRect(l + 2 * u, t + 3 * u, r + 2 * u, b + 3 * u, 6 * u, 6 * u, fill)
        fill.color = if (enabled) col else 0xFF9A9088.toInt(); c.drawRoundRect(l, t, r, b, 6 * u, 6 * u, fill)
        fill.color = 0x30FFFFFF; c.drawRoundRect(l + 2 * u, t + 2 * u, r - 2 * u, (t + b) / 2, 5 * u, 5 * u, fill)
        text(c, label, (l + r) / 2, (t + b) / 2 + size * 0.36f * u, size, Color.WHITE, Paint.Align.CENTER, true)
        if (enabled) btn(l, t, r, b, act)
    }

    private fun bar(c: Canvas, l: Float, t: Float, r: Float, b: Float, frac: Float, col: Int) {
        fill.color = 0xFF3A2A22.toInt(); c.drawRoundRect(l, t, r, b, 3 * u, 3 * u, fill)
        fill.color = col; c.drawRoundRect(l + 1.5f * u, t + 1.5f * u, l + 1.5f * u + (r - l - 3 * u) * frac.coerceIn(0f, 1f), b - 1.5f * u, 2 * u, 2 * u, fill)
    }

    private fun hpCol(f: Float) = if (f > 0.5f) 0xFF5DD45A.toInt() else if (f > 0.2f) 0xFFF2C842.toInt() else 0xFFE2483A.toInt()

    fun timeStr(): String {
        val t = g.time.toInt(); var h = (t / 60) % 24; val m = (t % 60) / 10 * 10
        val am = h < 12; h %= 12; if (h == 0) h = 12
        return "$h:${if (m < 10) "0$m" else "$m"} ${if (am) "am" else "pm"}"
    }

    private fun drawHud(c: Canvas) {
        if (g.mode == Mode.STARTER) return
        // clock panel
        val l = 10 * u; val t = 8 * u
        panel(c, l, t, l + 132 * u, t + 62 * u)
        text(c, "${Season.names[g.season]} ${g.day} · Y${g.year}", l + 10 * u, t + 20 * u, 13f)
        text(c, timeStr(), l + 10 * u, t + 38 * u, 15f, if (g.time >= 1440) 0xFFC0302A.toInt() else INK)
        text(c, if (g.raining) "Rain" else if (g.season == 3) "Snow" else "Sunny", l + 122 * u, t + 38 * u, 11f, 0xFF6E5A48.toInt(), Paint.Align.RIGHT)
        text(c, "${g.gold}g", l + 10 * u, t + 55 * u, 13f, 0xFF8A6A12.toInt())
        text(c, g.cur.name, l + 122 * u, t + 55 * u, 10f, 0xFF6E5A48.toInt(), Paint.Align.RIGHT)
        // lead creature
        val lead = g.party.firstOrNull()
        if (lead != null) {
            val ly = t + 68 * u
            panel(c, l, ly, l + 132 * u, ly + 34 * u)
            bmp(c, art.mon(lead.speciesId)[0], l + 4 * u, ly + 1 * u, 32 * u, 32 * u)
            text(c, "${lead.name} Lv${lead.level}", l + 38 * u, ly + 15 * u, 10.5f)
            bar(c, l + 38 * u, ly + 19 * u, l + 124 * u, ly + 27 * u, lead.hp.toFloat() / lead.maxHp, hpCol(lead.hp.toFloat() / lead.maxHp))
        }
        // energy & hp bars (right side, vertical)
        val bx = W - 30 * u; val btop = 12 * u; val bbot = 132 * u
        panel(c, bx - 6 * u, btop - 4 * u, bx + 22 * u, bbot + 18 * u)
        bar(c, bx, btop + 2 * u, bx + 7 * u, bbot, 1f, 0xFF3A2A22.toInt())
        val ef = g.energy / g.maxEnergy
        fill.color = if (ef > 0.25f) 0xFF5DD45A.toInt() else 0xFFE2483A.toInt()
        c.drawRoundRect(bx + 1.5f * u, bbot - (bbot - btop - 2 * u) * ef, bx + 5.5f * u, bbot - 1.5f * u, 2 * u, 2 * u, fill)
        val hf = g.hp.toFloat() / g.maxHp
        bar(c, bx + 9 * u, btop + 2 * u, bx + 16 * u, bbot, 1f, 0xFF3A2A22.toInt())
        fill.color = 0xFFE2483A.toInt(); c.drawRoundRect(bx + 10.5f * u, bbot - (bbot - btop - 2 * u) * hf, bx + 14.5f * u, bbot - 1.5f * u, 2 * u, 2 * u, fill)
        text(c, "E  H", bx + 8 * u, bbot + 12 * u, 9f, INK, Paint.Align.CENTER)

        // hotbar
        val slot = 38 * u; val n = 8
        val hx = W / 2f - slot * n / 2f - 18 * u; val hy = H - slot - 10 * u
        panel(c, hx - 6 * u, hy - 6 * u, hx + slot * n + 6 * u + 26 * u, hy + slot + 6 * u)
        for (i in 0 until n) {
            val si = hotRow * 8 + i
            val sx = hx + i * slot
            fill.color = if (si == g.sel) 0xFFFFE07A.toInt() else 0xFFE8D2A2.toInt()
            c.drawRoundRect(sx + 2 * u, hy + 2 * u, sx + slot - 2 * u, hy + slot - 2 * u, 4 * u, 4 * u, fill)
            if (si == g.sel) { stroke.color = 0xFFC0302A.toInt(); stroke.strokeWidth = 2.5f * u; c.drawRoundRect(sx + 2 * u, hy + 2 * u, sx + slot - 2 * u, hy + slot - 2 * u, 4 * u, 4 * u, stroke) }
            val s = g.inv.slots[si]
            if (s != null) {
                bmp(c, art.item(s.id), sx + 5 * u, hy + 5 * u, slot - 10 * u, slot - 10 * u)
                if (s.n > 1) text(c, "${s.n}", sx + slot - 4 * u, hy + slot - 4 * u, 10f, Color.WHITE, Paint.Align.RIGHT, true)
                if (s.id < 6 && g.toolLevel[s.id] > 0) { fill.color = arrayOf(0, 0xFFD9824A.toInt(), 0xFFC8CCD6.toInt(), GOLD)[g.toolLevel[s.id]]; c.drawCircle(sx + 9 * u, hy + 9 * u, 3.5f * u, fill) }
                if (s.id == Tool.CAN) bar(c, sx + 5 * u, hy + slot - 9 * u, sx + slot - 5 * u, hy + slot - 4 * u, g.water.toFloat() / g.waterMax, 0xFF4A9AE0.toInt())
            }
            btn(sx, hy, sx + slot, hy + slot) { g.sel = si }
        }
        val rx = hx + slot * n + 2 * u
        text(c, "${hotRow + 1}", rx + 12 * u, hy + slot / 2 + 5 * u, 14f, INK, Paint.Align.CENTER)
        btn(rx, hy, rx + 26 * u, hy + slot) { hotRow = (hotRow + 1) % 3; g.sel = hotRow * 8 }
        // held item name
        g.held?.let { text(c, Items[it.id].name, W / 2f, hy - 12 * u, 12f, Color.WHITE, Paint.Align.CENTER, true) }

        // buttons
        if (g.mode == Mode.PLAY) {
            fill.color = 0x70000000; c.drawCircle(aBtn.centerX() + 2 * u, aBtn.centerY() + 3 * u, aBtn.width() / 2, fill)
            fill.color = if (aId >= 0) 0xFF3E8C3A.toInt() else 0xFF5DA342.toInt(); c.drawCircle(aBtn.centerX(), aBtn.centerY(), aBtn.width() / 2, fill)
            stroke.color = 0xFF2E5E2A.toInt(); stroke.strokeWidth = 3 * u; c.drawCircle(aBtn.centerX(), aBtn.centerY(), aBtn.width() / 2, stroke)
            text(c, contextLabel(), aBtn.centerX(), aBtn.centerY() + 5 * u, 14f, Color.WHITE, Paint.Align.CENTER, true)
            fill.color = 0x70000000; c.drawCircle(bBtn.centerX() + 2 * u, bBtn.centerY() + 3 * u, bBtn.width() / 2, fill)
            fill.color = 0xFFB0783E.toInt(); c.drawCircle(bBtn.centerX(), bBtn.centerY(), bBtn.width() / 2, fill)
            text(c, "Menu", bBtn.centerX(), bBtn.centerY() + 4 * u, 11f, Color.WHITE, Paint.Align.CENTER, true)
            btn(bBtn.left, bBtn.top, bBtn.right, bBtn.bottom) { g.mode = Mode.MENU; g.menuSel = -1; listOff = 0 }
            // joystick
            if (joyId >= 0) {
                fill.color = 0x40FFFFFF; c.drawCircle(joyOx, joyOy, 46 * u, fill)
                fill.color = 0x90FFFFFF.toInt(); c.drawCircle(joyOx + joyX * 46 * u, joyOy + joyY * 46 * u, 18 * u, fill)
            }
        }
        // banner
        g.banner?.let {
            val w = max(220 * u, txt.apply { textSize = 13 * u }.measureText(it) + 40 * u)
            panel(c, W / 2f - w / 2, 10 * u, W / 2f + w / 2, 40 * u)
            text(c, it, W / 2f, 30 * u, 13f, INK, Paint.Align.CENTER)
        }
        // update bar
        val up = Updater.available
        if (up != null && !updateDismissed && g.mode == Mode.PLAY) {
            val w = 300 * u; val l2 = W / 2f - w / 2; val t2 = 46 * u
            panel(c, l2, t2, l2 + w, t2 + 40 * u, 0xFFDFF2D0.toInt())
            val msg = if (Updater.busy) "Downloading ${(Updater.progress * 100).toInt().coerceAtLeast(0)}%" else "Update ${up.tag} available"
            text(c, msg, l2 + 12 * u, t2 + 26 * u, 13f)
            if (!Updater.busy) {
                button(c, l2 + w - 140 * u, t2 + 8 * u, l2 + w - 74 * u, t2 + 32 * u, "Update", size = 11f) { host.installUpdate(up) }
                button(c, l2 + w - 68 * u, t2 + 8 * u, l2 + w - 10 * u, t2 + 32 * u, "Later", col = 0xFF9A7A5A.toInt(), size = 11f) { updateDismissed = true }
            }
        }
    }

    private fun contextLabel(): String {
        val (fx, fy) = g.facing()
        val o = g.cur.o(fx, fy)
        val crop = if (g.cur.inb(fx, fy)) g.cur.crops[g.cur.idx(fx, fy)] else null
        val h = g.held
        if (h != null && h.id == Tool.ROD && crop?.ripe != true && o != Ob.FORAGE && g.canCast()) return "Cast"
        return when {
            crop?.ripe == true -> "Pick"
            o == Ob.FORAGE -> "Pick"
            o == Ob.BED -> "Sleep"
            o == Ob.BIN -> "Ship"
            o == Ob.LADDER -> "Down"
            o == Ob.LADDERUP -> "Up"
            o in setOf(Ob.BOX, Ob.CHEST, Ob.HEALER, Ob.ELEVATOR, Ob.ANVIL, Ob.COUNTER) -> "Open"
            else -> "Use"
        }
    }

    // ---------------------------------------------------------------- dialog

    private fun closeDialog() {
        val d = g.dialog ?: return
        g.dialog = null; g.mode = Mode.PLAY
        d.onClose?.invoke()
    }

    private fun drawDialog(c: Canvas) {
        val d = g.dialog ?: return
        val l = 40 * u; val r = W - 40 * u; val t = H - 128 * u; val b = H - 14 * u
        fill.color = 0x50000000; c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill)
        panel(c, l, t, r, b)
        var ty = t + 26 * u
        if (d.speaker != null) {
            val n = d.speaker
            val npc = g.npcs.firstOrNull { it.name == n }
            if (npc != null) {
                panel(c, l, t - 92 * u, l + 86 * u, t - 4 * u)
                bmp(c, People.portraits[npc.key]!!, l + 11 * u, t - 88 * u, 64 * u, 88 * u)
            }
            text(c, n, l + 14 * u, ty, 14f, 0xFF8E3A1E.toInt()); ty += 20 * u
        }
        for (line in wrap(d.pages[d.page], r - l - 40 * u, 14f).take(4)) { text(c, line, l + 14 * u, ty, 14f); ty += 19 * u }
        val last = d.page == d.pages.size - 1
        if (last && d.options.isNotEmpty()) {
            var bx = r - 14 * u
            for ((label, act) in d.options.reversed()) {
                txt.textSize = 13 * u
                val w = txt.measureText(label) + 30 * u
                button(c, bx - w, b - 40 * u, bx, b - 12 * u, label) {
                    g.dialog = null; g.mode = Mode.PLAY
                    act()
                    if (g.mode == Mode.PLAY && g.dialog == null) d.onClose?.invoke()
                }
                bx -= w + 10 * u
            }
        } else {
            text(c, "▼", r - 20 * u, b - 12 * u, 12f, 0xFF8E3A1E.toInt(), Paint.Align.CENTER)
            btn(0f, 0f, W.toFloat(), H.toFloat()) { if (last) closeDialog() else d.page++ }
        }
    }

    // ---------------------------------------------------------------- common overlay frame

    private fun overlay(c: Canvas, title: String, w: Float = W * 0.86f, h: Float = H * 0.86f): RectF {
        fill.color = 0x80000000.toInt(); c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill)
        btn(0f, 0f, W.toFloat(), H.toFloat()) {} // swallow
        val l = (W - w) / 2; val t = (H - h) / 2
        panel(c, l, t, l + w, t + h)
        text(c, title, l + 18 * u, t + 28 * u, 18f, 0xFF8E3A1E.toInt())
        button(c, l + w - 44 * u, t + 10 * u, l + w - 12 * u, t + 40 * u, "X", col = 0xFFC0503A.toInt()) { g.mode = Mode.PLAY; g.menuSel = -1 }
        return RectF(l, t, l + w, t + h)
    }

    // ---------------------------------------------------------------- menu

    private val tabs = listOf("Items", "Party", "Craft", "Dex", "Friends", "Options")

    private fun drawMenu(c: Canvas) {
        val r = overlay(c, "")
        var tx = r.left + 14 * u
        for ((i, name) in tabs.withIndex()) {
            txt.textSize = 13 * u
            val w = txt.measureText(name) + 26 * u
            button(c, tx, r.top + 10 * u, tx + w, r.top + 40 * u, name, col = if (g.menuTab == i) 0xFFB0783E.toInt() else 0xFF8E7A62.toInt()) {
                g.menuTab = i; g.menuSel = -1; listOff = 0; dexPick = -1
            }
            tx += w + 6 * u
        }
        val area = RectF(r.left + 16 * u, r.top + 52 * u, r.right - 16 * u, r.bottom - 14 * u)
        when (g.menuTab) {
            0 -> drawItemsTab(c, area)
            1 -> drawPartyTab(c, area)
            2 -> drawCraftTab(c, area)
            3 -> drawDexTab(c, area)
            4 -> drawFriendsTab(c, area)
            5 -> drawOptionsTab(c, area)
        }
    }

    private fun slotGrid(c: Canvas, inv: Inventory, l: Float, t: Float, size: Float, cols: Int, selected: Int, onTap: (Int) -> Unit) {
        for (i in 0 until inv.size) {
            val x = l + (i % cols) * size; val y = t + (i / cols) * size
            fill.color = if (i == selected) 0xFFFFE07A.toInt() else if (i / cols == 0) 0xFFEBD7A8.toInt() else 0xFFE0C896.toInt()
            c.drawRoundRect(x + 2 * u, y + 2 * u, x + size - 2 * u, y + size - 2 * u, 4 * u, 4 * u, fill)
            val s = inv.slots[i]
            if (s != null) {
                bmp(c, art.item(s.id), x + 5 * u, y + 5 * u, size - 10 * u, size - 10 * u)
                if (s.n > 1) text(c, "${s.n}", x + size - 5 * u, y + size - 5 * u, 10f, Color.WHITE, Paint.Align.RIGHT, true)
            }
            btn(x, y, x + size, y + size) { onTap(i) }
        }
    }

    private fun drawItemsTab(c: Canvas, a: RectF) {
        val size = min(a.width() * 0.6f / 8, a.height() / 3.6f)
        slotGrid(c, g.inv, a.left, a.top, size, 8, g.menuSel) { i ->
            if (g.menuSel < 0) { if (g.inv.slots[i] != null) g.menuSel = i }
            else if (g.menuSel == i) g.menuSel = -1
            else { val t = g.inv.slots[i]; g.inv.slots[i] = g.inv.slots[g.menuSel]; g.inv.slots[g.menuSel] = t; g.menuSel = -1 }
        }
        text(c, "Tap an item, then tap another slot to move it. Top row = hotbar 1.", a.left, a.top + size * 3 + 20 * u, 11f, 0xFF6E5A48.toInt())
        val ix = a.left + size * 8 + 20 * u
        val s = if (g.menuSel >= 0) g.inv.slots[g.menuSel] else null
        if (s != null) {
            val d = Items[s.id]
            bmp(c, art.item(s.id), ix, a.top, 48 * u, 48 * u)
            text(c, d.name, ix + 56 * u, a.top + 20 * u, 15f)
            text(c, if (d.kind == Kind.TOOL) "${g.levelName(g.toolLevel[s.id])} tool" else "Sells for ${d.sell}g", ix + 56 * u, a.top + 40 * u, 11f, 0xFF6E5A48.toInt())
            var y = a.top + 72 * u
            for (line in wrap(d.desc, a.right - ix, 12f)) { text(c, line, ix, y, 12f); y += 17 * u }
            if (d.energy > 0 || d.heal > 0) button(c, ix, y + 6 * u, ix + 90 * u, y + 36 * u, "Eat") {
                val id = s.id; g.menuSel = -1; g.mode = Mode.PLAY; g.sel = g.inv.slots.indexOfFirst { it?.id == id }.coerceAtLeast(0); g.useHeld()
            }
        } else {
            text(c, "Inventory", ix, a.top + 18 * u, 15f)
            text(c, "Gold: ${g.gold}g", ix, a.top + 40 * u, 13f, 0xFF8A6A12.toInt())
            text(c, "Water: ${g.water}/${g.waterMax}", ix, a.top + 60 * u, 12f)
            text(c, "Deepest mine floor: ${g.deepest}", ix, a.top + 80 * u, 12f)
            text(c, "Creatures caught: ${g.dexCaught.size}/${Dex.list.size}", ix, a.top + 100 * u, 12f)
        }
    }

    private fun drawPartyTab(c: Canvas, a: RectF) {
        val rowH = min(a.height() / 6.2f, 46 * u)
        val lw = a.width() * 0.55f
        for ((i, m) in g.party.withIndex()) {
            val y = a.top + i * rowH
            fill.color = if (i == g.menuSel) 0xFFFFE07A.toInt() else 0xFFEBD7A8.toInt()
            c.drawRoundRect(a.left, y + 2 * u, a.left + lw, y + rowH - 2 * u, 5 * u, 5 * u, fill)
            bmp(c, art.mon(m.speciesId)[0], a.left + 4 * u, y + 2 * u, rowH - 4 * u, rowH - 4 * u)
            text(c, "${m.name}${if (i == 0) "  (following)" else ""}", a.left + rowH + 4 * u, y + rowH * 0.42f, 13f)
            text(c, "${m.sp.name} · ${m.sp.type.label} · Lv${m.level} · ${m.sp.job.label}", a.left + rowH + 4 * u, y + rowH * 0.78f, 10f, 0xFF6E5A48.toInt())
            val f = m.hp.toFloat() / m.maxHp
            bar(c, a.left + lw - 96 * u, y + rowH * 0.25f, a.left + lw - 8 * u, y + rowH * 0.45f, f, hpCol(f))
            text(c, "${m.hp}/${m.maxHp}", a.left + lw - 52 * u, y + rowH * 0.78f, 10f, INK, Paint.Align.CENTER)
            btn(a.left, y, a.left + lw, y + rowH) {
                if (g.menuSel < 0) g.menuSel = i
                else if (g.menuSel == i) g.menuSel = -1
                else { val t = g.party[i]; g.party[i] = g.party[g.menuSel]; g.party[g.menuSel] = t; g.menuSel = -1; g.trail.clear(); g.fx = g.px; g.fy = g.py }
            }
        }
        if (g.party.isEmpty()) text(c, "No creatures yet.", a.left, a.top + 20 * u, 13f)
        val dx = a.left + lw + 16 * u
        val m = g.party.getOrNull(if (g.menuSel >= 0) g.menuSel else 0) ?: return
        bmp(c, art.mon(m.speciesId)[0], dx, a.top, 72 * u, 72 * u)
        text(c, m.name, dx + 80 * u, a.top + 22 * u, 16f)
        text(c, "${m.sp.name} Lv${m.level}", dx + 80 * u, a.top + 42 * u, 12f, 0xFF6E5A48.toInt())
        typeChip(c, m.sp.type, dx + 80 * u, a.top + 50 * u)
        var y = a.top + 92 * u
        text(c, "ATK ${m.atk}  DEF ${m.def}  SPD ${m.spd}  Bond ${m.bond}", dx, y, 11f); y += 16 * u
        text(c, "XP ${m.xp}/${m.xpToNext}" + (if (m.sp.evolveTo >= 0) "  · evolves at Lv${m.sp.evolveLevel}" else ""), dx, y, 11f); y += 18 * u
        text(c, "Moves: " + m.moves().joinToString(", ") { it.name }, dx, y, 11f); y += 18 * u
        text(c, "Farm job: ${m.sp.job.label} (power ${m.jobPower})", dx, y, 12f, 0xFF3E7A2A.toInt()); y += 16 * u
        for (line in wrap(m.sp.job.desc, a.right - dx, 11f)) { text(c, line, dx, y, 11f); y += 15 * u }
        text(c, "Tap two creatures to reorder. The first one follows you.", a.left, a.bottom - 4 * u, 10f, 0xFF6E5A48.toInt())
    }

    private fun typeChip(c: Canvas, t: EType, x: Float, y: Float) {
        txt.textSize = 10 * u
        val w = txt.measureText(t.label) + 14 * u
        fill.color = t.color; c.drawRoundRect(x, y, x + w, y + 16 * u, 8 * u, 8 * u, fill)
        text(c, t.label, x + w / 2, y + 12 * u, 10f, Color.WHITE, Paint.Align.CENTER, true)
    }

    private fun drawCraftTab(c: Canvas, a: RectF) {
        val rowH = min(a.height() / Recipes.list.size, 44 * u)
        for ((i, r) in Recipes.list.withIndex()) {
            val y = a.top + i * rowH
            fill.color = 0xFFEBD7A8.toInt(); c.drawRoundRect(a.left, y + 2 * u, a.right, y + rowH - 2 * u, 5 * u, 5 * u, fill)
            bmp(c, art.item(r.out), a.left + 4 * u, y + 4 * u, rowH - 8 * u, rowH - 8 * u)
            text(c, Items[r.out].name, a.left + rowH + 4 * u, y + rowH * 0.62f, 13f)
            var nx = a.left + a.width() * 0.34f
            var ok = true
            for ((id, n) in r.needs) {
                val have = g.inv.count(id)
                if (have < n) ok = false
                bmp(c, art.item(id), nx, y + rowH * 0.2f, rowH * 0.6f, rowH * 0.6f)
                text(c, "$have/$n", nx + rowH * 0.65f, y + rowH * 0.62f, 11f, if (have >= n) 0xFF3E7A2A.toInt() else 0xFFC0302A.toInt())
                nx += rowH * 0.65f + 50 * u
            }
            button(c, a.right - 86 * u, y + 6 * u, a.right - 8 * u, y + rowH - 6 * u, "Craft", ok) { g.craft(r) }
        }
    }

    private fun drawDexTab(c: Canvas, a: RectF) {
        val cols = 12; val rows = 3
        val size = min(a.width() / cols, (a.height() - 50 * u) / rows)
        for ((i, sp) in Dex.list.withIndex()) {
            val x = a.left + (i % cols) * size; val y = a.top + (i / cols) * size
            fill.color = if (i == dexPick) 0xFFFFE07A.toInt() else 0xFFEBD7A8.toInt()
            c.drawRoundRect(x + 2 * u, y + 2 * u, x + size - 2 * u, y + size - 2 * u, 4 * u, 4 * u, fill)
            when (sp.id) {
                in g.dexCaught -> bmp(c, art.mon(sp.id)[0], x + 3 * u, y + 3 * u, size - 6 * u, size - 6 * u)
                in g.dexSeen -> bmp(c, art.mon(sp.id)[0], x + 3 * u, y + 3 * u, size - 6 * u, size - 6 * u, sil)
                else -> text(c, "?", x + size / 2, y + size / 2 + 6 * u, 16f, 0xFFB0987A.toInt(), Paint.Align.CENTER)
            }
            text(c, "${i + 1}", x + 5 * u, y + 12 * u, 8f, 0xFF8E7A62.toInt())
            btn(x, y, x + size, y + size) { dexPick = i }
        }
        val y = a.top + rows * size + 18 * u
        val sp = Dex.list.getOrNull(dexPick)
        if (sp == null) text(c, "Seen ${g.dexSeen.size} · Caught ${g.dexCaught.size} of ${Dex.list.size}. Tap an entry.", a.left, y, 12f)
        else if (sp.id !in g.dexSeen) text(c, "#${sp.id + 1} ???  Not yet seen.", a.left, y, 12f)
        else {
            text(c, "#${sp.id + 1} ${sp.name} · ${sp.type.label} · ${sp.job.label}" + (if (sp.evolveTo >= 0) " · evolves into ${Dex[sp.evolveTo].name} at Lv${sp.evolveLevel}" else ""), a.left, y, 12f)
            text(c, sp.dex, a.left, y + 18 * u, 11f, 0xFF6E5A48.toInt())
        }
    }

    private fun drawFriendsTab(c: Canvas, a: RectF) {
        val rowH = min(a.height() / 5.5f, 48 * u)
        for ((i, n) in g.npcs.withIndex()) {
            val y = a.top + i * rowH
            fill.color = 0xFFEBD7A8.toInt(); c.drawRoundRect(a.left, y + 2 * u, a.right, y + rowH - 2 * u, 5 * u, 5 * u, fill)
            bmp(c, People.portraits[n.key]!!, a.left + 8 * u, y + 3 * u, (rowH - 6 * u) * 16f / 22f, rowH - 6 * u)
            text(c, n.name, a.left + rowH, y + rowH * 0.45f, 14f)
            text(c, "Lives in the ${n.area}" + (if (n.key in g.giftedToday) " · gifted today" else ""), a.left + rowH, y + rowH * 0.8f, 10f, 0xFF6E5A48.toInt())
            val pts = g.friendship[n.key] ?: 0
            for (h in 0 until 10) {
                val hx = a.left + a.width() * 0.5f + h * 22 * u
                fill.color = if (h < pts / 250) 0xFFE2483A.toInt() else 0xFFC8B08A.toInt()
                heart(c, hx, y + rowH / 2, 8 * u)
            }
        }
        text(c, "Talk every day and give gifts to raise friendship. Friends give presents at 3 hearts.", a.left, a.bottom - 4 * u, 10f, 0xFF6E5A48.toInt())
    }

    private fun heart(c: Canvas, x: Float, y: Float, r: Float) {
        c.drawCircle(x - r * 0.45f, y - r * 0.2f, r * 0.55f, fill); c.drawCircle(x + r * 0.45f, y - r * 0.2f, r * 0.55f, fill)
        val p = android.graphics.Path(); p.moveTo(x - r, y); p.lineTo(x + r, y); p.lineTo(x, y + r); p.close(); c.drawPath(p, fill)
    }

    private fun drawOptionsTab(c: Canvas, a: RectF) {
        var y = a.top + 18 * u
        text(c, "Stardust Ranch v${Updater.installedVersion}", a.left, y, 15f); y += 22 * u
        text(c, Updater.status, a.left, y, 12f, 0xFF6E5A48.toInt()); y += 12 * u
        button(c, a.left, y, a.left + 170 * u, y + 32 * u, "Check for updates") { host.checkUpdate() }
        Updater.available?.let { r -> button(c, a.left + 180 * u, y, a.left + 320 * u, y + 32 * u, if (Updater.busy) "Downloading..." else "Install ${r.tag}", !Updater.busy) { host.installUpdate(r) } }
        y += 52 * u
        button(c, a.left, y, a.left + 170 * u, y + 32 * u, "Save game", col = 0xFF3F7BC8.toInt()) { g.save(); g.note("Game saved.") }
        y += 52 * u
        text(c, "Tools: " + (0..5).joinToString("  ") { "${Items[it].name.substringAfterLast(' ')} ${g.levelName(g.toolLevel[it])}" }, a.left, y, 11f); y += 22 * u
        val help = listOf(
            "Left side of screen: drag to walk. Green button: use tool / talk / interact. Hold it to keep swinging.",
            "Tap a hotbar slot to pick an item. The small number switches hotbar rows.",
            "The game saves every time you sleep. Creatures heal overnight and at Wren's fountain in town.",
            "Farm east → Town. Farm south → Forest. Town north → Mountain & Mines. Town south → Beach.")
        for (h in help) for (line in wrap(h, a.width(), 11f)) { text(c, line, a.left, y, 11f, 0xFF6E5A48.toInt()); y += 15 * u }
    }

    // ---------------------------------------------------------------- shop

    private fun shopStock(): List<Int> {
        val seeds = Crops.list.filter { it.seasons and (1 shl g.season) != 0 }.map { Items.seed(it.idx) }
        return seeds + listOf(Items.ORB, Items.SUPER_ORB, Items.STAR_ORB, Items.SNACK, Items.TONIC, Items.FEAST)
    }

    private fun drawShop(c: Canvas) {
        val r = overlay(c, "Pia's General Store")
        button(c, r.left + 230 * u, r.top + 10 * u, r.left + 300 * u, r.top + 40 * u, "Buy", col = if (!g.shopSell) 0xFFB0783E.toInt() else 0xFF8E7A62.toInt()) { g.shopSell = false }
        button(c, r.left + 306 * u, r.top + 10 * u, r.left + 376 * u, r.top + 40 * u, "Sell", col = if (g.shopSell) 0xFFB0783E.toInt() else 0xFF8E7A62.toInt()) { g.shopSell = true }
        text(c, "${g.gold}g", r.right - 56 * u, r.top + 30 * u, 15f, 0xFF8A6A12.toInt(), Paint.Align.RIGHT)
        val a = RectF(r.left + 16 * u, r.top + 52 * u, r.right - 16 * u, r.bottom - 14 * u)
        if (!g.shopSell) {
            val stock = shopStock()
            val cols = 2; val rowsN = (stock.size + 1) / 2
            val rowH = min(a.height() / rowsN, 46 * u); val colW = a.width() / cols
            for ((i, id) in stock.withIndex()) {
                val x = a.left + (i % cols) * colW; val y = a.top + (i / cols) * rowH
                val d = Items[id]
                fill.color = 0xFFEBD7A8.toInt(); c.drawRoundRect(x + 2 * u, y + 2 * u, x + colW - 4 * u, y + rowH - 2 * u, 5 * u, 5 * u, fill)
                bmp(c, art.item(id), x + 6 * u, y + 4 * u, rowH - 8 * u, rowH - 8 * u)
                text(c, d.name, x + rowH + 4 * u, y + rowH * 0.45f, 12f)
                text(c, "${d.price}g", x + rowH + 4 * u, y + rowH * 0.82f, 11f, 0xFF8A6A12.toInt())
                button(c, x + colW - 112 * u, y + 7 * u, x + colW - 66 * u, y + rowH - 7 * u, "x1", g.gold >= d.price, size = 11f) { g.buy(id, 1) }
                button(c, x + colW - 60 * u, y + 7 * u, x + colW - 12 * u, y + rowH - 7 * u, "x5", g.gold >= d.price * 5, size = 11f) { g.buy(id, 5) }
            }
        } else {
            val size = min(a.width() * 0.6f / 8, a.height() / 3.6f)
            slotGrid(c, g.inv, a.left, a.top, size, 8, g.menuSel) { i -> g.menuSel = if (g.inv.slots[i] != null && Items[g.inv.slots[i]!!.id].kind != Kind.TOOL) i else -1 }
            val ix = a.left + size * 8 + 20 * u
            val s = if (g.menuSel >= 0) g.inv.slots[g.menuSel] else null
            if (s != null) {
                val d = Items[s.id]
                text(c, d.name, ix, a.top + 20 * u, 15f)
                text(c, "${s.n} × ${d.sell}g = ${s.n * d.sell}g", ix, a.top + 42 * u, 13f, 0xFF8A6A12.toInt())
                button(c, ix, a.top + 56 * u, ix + 110 * u, a.top + 88 * u, "Sell all") { g.sellSlot(g.menuSel); g.menuSel = -1 }
            } else text(c, "Tap an item to sell it.", ix, a.top + 20 * u, 12f)
        }
    }

    // ---------------------------------------------------------------- blacksmith

    private fun drawSmith(c: Canvas) {
        val r = overlay(c, "Gus's Forge", W * 0.7f, H * 0.7f)
        text(c, "${g.gold}g", r.right - 56 * u, r.top + 30 * u, 15f, 0xFF8A6A12.toInt(), Paint.Align.RIGHT)
        val tools = listOf(Tool.HOE, Tool.CAN, Tool.PICK, Tool.AXE)
        val a = RectF(r.left + 16 * u, r.top + 52 * u, r.right - 16 * u, r.bottom - 30 * u)
        val rowH = a.height() / 4
        for ((i, t) in tools.withIndex()) {
            val y = a.top + i * rowH
            fill.color = 0xFFEBD7A8.toInt(); c.drawRoundRect(a.left, y + 2 * u, a.right, y + rowH - 2 * u, 5 * u, 5 * u, fill)
            bmp(c, art.item(t), a.left + 6 * u, y + 4 * u, rowH - 8 * u, rowH - 8 * u)
            text(c, "${Items[t].name} (${g.levelName(g.toolLevel[t])})", a.left + rowH + 6 * u, y + rowH * 0.45f, 13f)
            val cost = g.upgradeCost(t)
            if (cost == null) text(c, "Fully upgraded!", a.left + rowH + 6 * u, y + rowH * 0.8f, 11f, 0xFF3E7A2A.toInt())
            else {
                text(c, "→ ${g.levelName(g.toolLevel[t] + 1)}: ${cost.first}g + ${cost.third} ${Items[cost.second].name} (have ${g.inv.count(cost.second)})", a.left + rowH + 6 * u, y + rowH * 0.8f, 11f, 0xFF6E5A48.toInt())
                button(c, a.right - 100 * u, y + 8 * u, a.right - 8 * u, y + rowH - 8 * u, "Upgrade", g.gold >= cost.first && g.inv.count(cost.second) >= cost.third) { g.upgrade(t); g.mode = if (g.mode == Mode.DIALOG) Mode.DIALOG else Mode.SMITH }
            }
        }
        text(c, "Upgraded tools work on more tiles at once and break tougher rocks.", r.left + 18 * u, r.bottom - 12 * u, 11f, 0xFF6E5A48.toInt())
    }

    // ---------------------------------------------------------------- ranch

    private fun drawRanch(c: Canvas) {
        val r = overlay(c, "Ranch")
        val a = RectF(r.left + 16 * u, r.top + 52 * u, r.right - 16 * u, r.bottom - 30 * u)
        val half = a.width() / 2 - 8 * u
        text(c, "Party (${g.party.size}/6) - tap to send to ranch", a.left, a.top, 12f, 0xFF8E3A1E.toInt())
        text(c, "Ranch (${g.ranch.size}) - tap to bring along", a.left + half + 16 * u, a.top, 12f, 0xFF8E3A1E.toInt())
        val rowH = min((a.height() - 14 * u) / 6, 44 * u)
        for ((i, m) in g.party.withIndex()) monRow(c, m, a.left, a.top + 8 * u + i * rowH, half, rowH) { g.moveToRanch(i) }
        val perPage = 6
        listOff = listOff.coerceIn(0, max(0, (g.ranch.size - 1) / perPage * perPage))
        for (k in 0 until perPage) {
            val i = listOff + k; val m = g.ranch.getOrNull(i) ?: break
            monRow(c, m, a.left + half + 16 * u, a.top + 8 * u + k * rowH, half, rowH) { g.moveToParty(i) }
        }
        if (g.ranch.size > perPage) {
            button(c, a.right - 130 * u, r.bottom - 34 * u, a.right - 70 * u, r.bottom - 8 * u, "Prev", listOff > 0, size = 11f) { listOff -= perPage }
            button(c, a.right - 64 * u, r.bottom - 34 * u, a.right, r.bottom - 8 * u, "Next", listOff + perPage < g.ranch.size, size = 11f) { listOff += perPage }
        }
        text(c, "Ranch creatures do their farm job every morning (results go to the Ranch Box).", a.left, r.bottom - 12 * u, 10f, 0xFF6E5A48.toInt())
    }

    private fun monRow(c: Canvas, m: Mon, l: Float, t: Float, w: Float, h: Float, act: () -> Unit) {
        fill.color = 0xFFEBD7A8.toInt(); c.drawRoundRect(l, t + 2 * u, l + w, t + h - 2 * u, 5 * u, 5 * u, fill)
        bmp(c, art.mon(m.speciesId)[0], l + 2 * u, t + 2 * u, h - 4 * u, h - 4 * u)
        text(c, "${m.name} Lv${m.level}", l + h + 2 * u, t + h * 0.45f, 12f)
        text(c, "${m.sp.name} · ${m.sp.job.label}", l + h + 2 * u, t + h * 0.8f, 10f, 0xFF3E7A2A.toInt())
        btn(l, t, l + w, t + h, act)
    }

    // ---------------------------------------------------------------- elevator

    private fun drawElevator(c: Canvas) {
        val r = overlay(c, "Mine Cart", W * 0.7f, H * 0.7f)
        val floors = listOf(1) + (5..g.deepest step 5).toList()
        val bw = 74 * u; val bh = 36 * u
        val cols = ((r.width() - 32 * u) / (bw + 8 * u)).toInt().coerceAtLeast(1)
        for ((i, f) in floors.withIndex()) {
            val x = r.left + 16 * u + (i % cols) * (bw + 8 * u); val y = r.top + 56 * u + (i / cols) * (bh + 8 * u)
            button(c, x, y, x + bw, y + bh, "Floor $f", col = 0xFF6E6A84.toInt(), size = 12f) { g.mode = Mode.PLAY; g.enterMine(f) }
        }
        text(c, "Every 5th floor you reach is added here.", r.left + 18 * u, r.bottom - 14 * u, 11f, 0xFF6E5A48.toInt())
    }

    // ---------------------------------------------------------------- starter

    private fun drawStarter(c: Canvas) {
        c.drawColor(0xFF1B2A1F.toInt())
        fill.shader = LinearGradient(0f, 0f, 0f, H.toFloat(), 0xFF2E5E9C.toInt(), 0xFF5DA342.toInt(), Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, W.toFloat(), H.toFloat(), fill); fill.shader = null
        text(c, "Stardust Ranch", W / 2f, 52 * u, 34f, Color.WHITE, Paint.Align.CENTER, true)
        text(c, "Grandma's old ranch is yours now. Choose a creature partner to start your new life.", W / 2f, 80 * u, 13f, 0xFFF4F0E0.toInt(), Paint.Align.CENTER, true)
        val cw = min(W * 0.28f, 210 * u); val ch = H * 0.62f
        val gap = (W - cw * 3) / 4
        for ((i, id) in Dex.starters.withIndex()) {
            val sp = Dex[id]
            val l = gap + i * (cw + gap); val t = H * 0.3f
            panel(c, l, t, l + cw, t + ch)
            val bob = sin(g.frame * 3 + i) * 3 * u
            bmp(c, art.mon(id)[((g.frame * 2).toInt() + i) % 2], l + cw / 2 - 60 * u, t + 4 * u + bob, 120 * u, 120 * u)
            text(c, sp.name, l + cw / 2, t + 140 * u, 18f, INK, Paint.Align.CENTER)
            typeChip(c, sp.type, l + cw / 2 - 22 * u, t + 148 * u)
            text(c, "Job: ${sp.job.label}", l + cw / 2, t + 182 * u, 12f, 0xFF3E7A2A.toInt(), Paint.Align.CENTER)
            var y = t + 200 * u
            for (line in wrap(sp.dex, cw - 20 * u, 10.5f)) { text(c, line, l + cw / 2, y, 10.5f, 0xFF6E5A48.toInt(), Paint.Align.CENTER); y += 14 * u }
            btn(l, t, l + cw, t + ch) { g.chooseStarter(id) }
        }
    }

    // ---------------------------------------------------------------- report

    private fun drawReport(c: Canvas) {
        c.drawColor(0xFF141A2A.toInt())
        val w = min(W * 0.7f, 560 * u); val l = (W - w) / 2
        val h = H * 0.8f; val t = (H - h) / 2
        panel(c, l, t, l + w, t + h)
        text(c, "${Season.names[g.season]} ${g.day}, Year ${g.year}", l + w / 2, t + 34 * u, 20f, 0xFF8E3A1E.toInt(), Paint.Align.CENTER)
        var y = t + 66 * u
        for (line in g.report) for (wl in wrap(line, w - 40 * u, 13f)) { if (y < t + h - 40 * u) text(c, wl, l + 20 * u, y, 13f); y += 19 * u }
        text(c, "Game saved · tap to continue", l + w / 2, t + h - 16 * u, 12f, 0xFF6E5A48.toInt(), Paint.Align.CENTER)
        btn(0f, 0f, W.toFloat(), H.toFloat()) { g.mode = Mode.PLAY; g.note("${Season.names[g.season]} ${g.day}") }
    }

    // ---------------------------------------------------------------- battle

    private fun drawBattle(c: Canvas) {
        val b = g.battle!!
        val kind = when { g.cur.mineFloor > 0 -> BattleArt.MINE; g.cur.beach -> BattleArt.BEACH; g.cur.id == "mountain" -> BattleArt.MOUNTAIN; else -> BattleArt.FOREST }
        val night = kind != BattleArt.MINE && g.time >= 1200f
        // pixel backdrop scaled to cover the screen (nearest-neighbour), centred
        val bg = art.battleBg(kind, g.season, night)
        val s = max(W / bg.width.toFloat(), H / bg.height.toFloat())
        bmp(c, bg, (W - bg.width * s) / 2f, (H - bg.height * s) / 2f, bg.width * s, bg.height * s)
        // lit platforms under each creature; the top face centre sits on the creature's foot line
        val pl = art.platform(kind, g.season)
        val wpw = H * 0.78f; val wph = wpw * pl.height / pl.width
        bmp(c, pl, W * 0.73f - wpw / 2, H * 0.505f - wph * 8f / 22f, wpw, wph)
        val mpw = H * 0.98f; val mph = mpw * pl.height / pl.width
        bmp(c, pl, W * 0.27f - mpw / 2, H * 0.765f - mph * 8f / 22f, mpw, mph)

        val shakeX = if (b.flashWild > 0) sin(b.flashWild * 60) * 6 * u else 0f
        val wildSize = H * 0.36f * (0.6f + b.wild.sp.size * 0.4f)
        if (!b.caught || b.waiting && b.orbAnim > 0) {
            val wb = art.mon(b.wild.speciesId)[2 + ((g.frame * 2).toInt() % 2)]
            if (!(b.orbAnim > 0 && b.orbAnim < 1.2f))
                if (!(b.wild.fainted && b.over && !b.waiting))
                    bmp(c, wb, W * 0.73f - wildSize / 2 + shakeX, H * 0.52f - wildSize, wildSize, wildSize)
        }
        if (b.orbAnim > 0) {
            val k = (1.6f - b.orbAnim) / 0.4f
            val ox = if (k < 1f) W * 0.3f + (W * 0.43f) * k else W * 0.73f
            val oy = if (k < 1f) H * 0.6f - sin(k * Math.PI.toFloat()) * H * 0.35f - H * 0.15f * k else H * 0.47f
            val wob = if (k >= 1f) sin(g.frame * 14) * 8f else 0f
            c.save(); c.rotate(wob, ox, oy)
            bmp(c, art.item(Items.ORB), ox - 16 * u, oy - 16 * u, 32 * u, 32 * u); c.restore()
        }
        val mineSx = if (b.flashMine > 0) sin(b.flashMine * 60) * 6 * u else 0f
        val my = b.active
        val mySize = H * 0.4f * (0.6f + my.sp.size * 0.4f)
        if (!my.fainted) bmp(c, art.mon(my.speciesId)[(g.frame * 2).toInt() % 2], W * 0.27f - mySize / 2 + mineSx, H * 0.78f - mySize, mySize, mySize)

        // info boxes
        infoBox(c, b.wild, 18 * u, 16 * u, false, b.wild.speciesId in g.dexCaught)
        infoBox(c, my, W - 270 * u, H * 0.545f, true, false)

        // bottom panel
        val pt = H * 0.74f
        panel(c, 10 * u, pt, W - 10 * u, H - 8 * u)
        if (b.waiting) {
            var ty = pt + 32 * u
            for (line in wrap(b.msgs.first(), W - 80 * u, 16f)) { text(c, line, 30 * u, ty, 16f); ty += 22 * u }
            text(c, "▼", W - 34 * u, H - 22 * u, 13f, 0xFF8E3A1E.toInt(), Paint.Align.CENTER)
            btn(0f, 0f, W.toFloat(), H.toFloat()) { b.msgs.removeFirst() }
            return
        }
        if (b.over) { g.endBattle(); return }
        val bl = W * 0.42f; val bt = pt + 12 * u; val bw = (W - 22 * u - bl) / 2 - 6 * u; val bh = (H - 20 * u - bt) / 2 - 4 * u
        when (b.menu) {
            0 -> {
                text(c, "What will ${my.name} do?", 30 * u, pt + 38 * u, 16f)
                button(c, bl, bt, bl + bw, bt + bh, "Fight", col = 0xFFC0503A.toInt(), size = 15f) { b.menu = 1 }
                button(c, bl + bw + 8 * u, bt, bl + bw * 2 + 8 * u, bt + bh, "Orb", col = 0xFF3F7BC8.toInt(), size = 15f) { b.menu = 2 }
                button(c, bl, bt + bh + 8 * u, bl + bw, bt + bh * 2 + 8 * u, "Party", col = 0xFF5DA342.toInt(), size = 15f) { b.menu = 3 }
                button(c, bl + bw + 8 * u, bt + bh + 8 * u, bl + bw * 2 + 8 * u, bt + bh * 2 + 8 * u, "Run", col = 0xFF8E7A62.toInt(), size = 15f) { b.run() }
            }
            1 -> {
                val ms = my.moves()
                for ((i, mv) in ms.withIndex()) {
                    val x = 20 * u + (i % 2) * ((W - 140 * u) / 2 + 6 * u); val y = bt + (i / 2) * (bh + 8 * u)
                    val eff = TypeChart.mult(mv.type, b.wild.sp.type)
                    val lbl = mv.name + (if (mv.heal) " (heal)" else " ${mv.power}") + (if (eff > 1f) " ▲" else if (eff < 1f) " ▼" else "")
                    button(c, x, y, x + (W - 140 * u) / 2, y + bh, lbl, col = mv.type.color, size = 13f) { b.chooseMove(mv) }
                }
                button(c, W - 110 * u, bt, W - 22 * u, bt + bh * 2 + 8 * u, "Back", col = 0xFF8E7A62.toInt()) { b.menu = 0 }
            }
            2 -> {
                val orbs = listOf(Items.ORB, Items.SUPER_ORB, Items.STAR_ORB)
                for ((i, id) in orbs.withIndex()) {
                    val x = 20 * u + i * ((W - 140 * u) / 3 + 4 * u)
                    val n = g.inv.count(id)
                    button(c, x, bt, x + (W - 140 * u) / 3, bt + bh * 2 + 8 * u, "${Items[id].name} ×$n", n > 0, col = Items[id].c1, size = 12f) { b.throwOrb(id) }
                }
                button(c, W - 110 * u, bt, W - 22 * u, bt + bh * 2 + 8 * u, "Back", col = 0xFF8E7A62.toInt()) { b.menu = 0 }
            }
            3 -> {
                val w = (W - 140 * u) / 3
                for ((i, m) in g.party.withIndex()) {
                    val x = 20 * u + (i % 3) * (w + 4 * u); val y = bt + (i / 3) * (bh + 8 * u)
                    button(c, x, y, x + w, y + bh, "${m.name} ${m.hp}/${m.maxHp}", !m.fainted && i != b.activeIdx, col = if (i == b.activeIdx) 0xFFB0783E.toInt() else 0xFF5DA342.toInt(), size = 11f) { b.swap(i) }
                }
                button(c, W - 110 * u, bt, W - 22 * u, bt + bh * 2 + 8 * u, "Back", col = 0xFF8E7A62.toInt()) { b.menu = 0 }
            }
        }
    }

    private fun infoBox(c: Canvas, m: Mon, l: Float, t: Float, mine: Boolean, caught: Boolean) {
        val w = 252 * u; val h = if (mine) 70 * u else 56 * u
        panel(c, l, t, l + w, t + h)
        text(c, m.name, l + 14 * u, t + 22 * u, 15f)
        if (caught) { fill.color = 0xFFE24848.toInt(); c.drawCircle(l + 14 * u + txt.apply { textSize = 15 * u }.measureText(m.name) + 10 * u, t + 17 * u, 4 * u, fill) }
        text(c, "Lv${m.level}", l + w - 14 * u, t + 22 * u, 13f, INK, Paint.Align.RIGHT)
        typeChip(c, m.sp.type, l + w - 100 * u, t + 9 * u)
        val f = m.hp.toFloat() / m.maxHp
        bar(c, l + 14 * u, t + 30 * u, l + w - 14 * u, t + 40 * u, f, hpCol(f))
        if (mine) {
            text(c, "${m.hp}/${m.maxHp}", l + w - 14 * u, t + 56 * u, 12f, INK, Paint.Align.RIGHT)
            bar(c, l + 14 * u, t + 48 * u, l + w - 80 * u, t + 53 * u, m.xp.toFloat() / m.xpToNext, 0xFF4A9AE0.toInt())
        }
    }
}
