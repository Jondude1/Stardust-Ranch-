package com.jondude.stardustranch.game

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.roundToInt

/** Tiny pixel canvas used to paint every sprite in code. */
class Px(val w: Int, val h: Int) {
    val p = IntArray(w * h)

    fun set(x: Int, y: Int, c: Int) { if (x in 0 until w && y in 0 until h) p[y * w + x] = c }
    fun get(x: Int, y: Int): Int = if (x in 0 until w && y in 0 until h) p[y * w + x] else 0
    fun fill(c: Int) { p.fill(c) }

    fun rect(x: Int, y: Int, rw: Int, rh: Int, c: Int) {
        for (yy in y until y + rh) for (xx in x until x + rw) set(xx, yy, c)
    }

    fun ell(cx: Float, cy: Float, rx: Float, ry: Float, c: Int) {
        if (rx <= 0 || ry <= 0) return
        val x0 = (cx - rx - 1).toInt(); val x1 = (cx + rx + 1).toInt()
        val y0 = (cy - ry - 1).toInt(); val y1 = (cy + ry + 1).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            val dx = (x + 0.5f - cx) / rx; val dy = (y + 0.5f - cy) / ry
            if (dx * dx + dy * dy <= 1f) set(x, y, c)
        }
    }

    /** Only paints where something is already drawn (for shading). */
    fun ellOn(cx: Float, cy: Float, rx: Float, ry: Float, c: Int, onlyColor: Int? = null) {
        val x0 = (cx - rx - 1).toInt(); val x1 = (cx + rx + 1).toInt()
        val y0 = (cy - ry - 1).toInt(); val y1 = (cy + ry + 1).toInt()
        for (y in y0..y1) for (x in x0..x1) {
            val dx = (x + 0.5f - cx) / rx; val dy = (y + 0.5f - cy) / ry
            if (dx * dx + dy * dy <= 1f) {
                val cur = get(x, y)
                if (cur != 0 && (onlyColor == null || cur == onlyColor)) set(x, y, c)
            }
        }
    }

    fun line(x0: Int, y0: Int, x1: Int, y1: Int, c: Int) {
        var x = x0; var y = y0
        val dx = abs(x1 - x0); val dy = -abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1; val sy = if (y0 < y1) 1 else -1
        var err = dx + dy
        while (true) {
            set(x, y, c)
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 >= dy) { err += dy; x += sx }
            if (e2 <= dx) { err += dx; y += sy }
        }
    }

    fun tri(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float, c: Int) {
        val minX = minOf(ax, bx, cx).toInt(); val maxX = maxOf(ax, bx, cx).roundToInt()
        val minY = minOf(ay, by, cy).toInt(); val maxY = maxOf(ay, by, cy).roundToInt()
        for (y in minY..maxY) for (x in minX..maxX) {
            val px = x + 0.5f; val py = y + 0.5f
            val d1 = (px - bx) * (ay - by) - (ax - bx) * (py - by)
            val d2 = (px - cx) * (by - cy) - (bx - cx) * (py - cy)
            val d3 = (px - ax) * (cy - ay) - (cx - ax) * (py - ay)
            val neg = d1 < 0 || d2 < 0 || d3 < 0
            val pos = d1 > 0 || d2 > 0 || d3 > 0
            if (!(neg && pos)) set(x, y, c)
        }
    }

    fun outline(c: Int) {
        val src = p.copyOf()
        for (y in 0 until h) for (x in 0 until w) {
            if (src[y * w + x] != 0) continue
            var n = false
            if (x > 0 && src[y * w + x - 1] != 0) n = true
            else if (x < w - 1 && src[y * w + x + 1] != 0) n = true
            else if (y > 0 && src[(y - 1) * w + x] != 0) n = true
            else if (y < h - 1 && src[(y + 1) * w + x] != 0) n = true
            if (n) p[y * w + x] = c
        }
    }

    fun flipH(): Px {
        val o = Px(w, h)
        for (y in 0 until h) for (x in 0 until w) o.p[y * w + (w - 1 - x)] = p[y * w + x]
        return o
    }

    fun blit(src: Px, ox: Int, oy: Int) {
        for (y in 0 until src.h) for (x in 0 until src.w) {
            val c = src.p[y * src.w + x]
            if (c != 0) set(x + ox, y + oy, c)
        }
    }

    fun bmp(): Bitmap = Bitmap.createBitmap(p, w, h, Bitmap.Config.ARGB_8888)

    companion object {
        fun shade(c: Int, f: Float): Int {
            val a = c ushr 24 and 0xFF
            val r = ((c shr 16 and 0xFF) * f).toInt().coerceIn(0, 255)
            val g = ((c shr 8 and 0xFF) * f).toInt().coerceIn(0, 255)
            val b = ((c and 0xFF) * f).toInt().coerceIn(0, 255)
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
        fun lighten(c: Int, f: Float): Int {
            val a = c ushr 24 and 0xFF
            val r = c shr 16 and 0xFF; val g = c shr 8 and 0xFF; val b = c and 0xFF
            return (a shl 24) or ((r + (255 - r) * f).toInt() shl 16) or ((g + (255 - g) * f).toInt() shl 8) or (b + (255 - b) * f).toInt()
        }
        fun mix(a: Int, b: Int, t: Float): Int {
            val r = ((a shr 16 and 0xFF) * (1 - t) + (b shr 16 and 0xFF) * t).toInt()
            val g = ((a shr 8 and 0xFF) * (1 - t) + (b shr 8 and 0xFF) * t).toInt()
            val bl = ((a and 0xFF) * (1 - t) + (b and 0xFF) * t).toInt()
            return (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
        }
    }
}
