package com.jondude.stardustranch.game

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Building exteriors for Stardust Ranch (original designs).
 * Each building is assembled from material ramps (5-6 tones, cool hue-shifted shadows, warm highlights),
 * textured surfaces (shingles, siding, boards, stone) and hand-authored detail grids from [Sprites].
 *
 * Every bitmap is (tw*16 + 8) wide so the roof overhangs the footprint by 4px each side, and taller than the
 * footprint so roofs and chimneys rise into the row behind. Callers centre it on the footprint and anchor it
 * at the footprint's bottom edge.
 */
object Buildings {
    private fun c(v: Long) = v.toInt()
    private val COOL = c(0xFF2A1E44)
    private val WARM = c(0xFFFFF2CC)
    private fun mix(a: Int, b: Int, t: Float) = Px.mix(a, b, t)

    /** 0 outline, 1 dark, 2 mid, 3 base, 4 light, 5 highlight. */
    fun ramp(base: Int): IntArray = intArrayOf(
        mix(base, COOL, 0.72f), mix(base, COOL, 0.42f), mix(base, COOL, 0.2f),
        base, mix(base, WARM, 0.3f), mix(base, WARM, 0.58f)
    )

    private fun hash(x: Int, y: Int, s: Int): Int {
        var h = x * 374761393 + y * 668265263 + s * 2147483 + 1013904223
        h = (h xor (h ushr 13)) * 1274126177
        return (h xor (h ushr 16)) and 0x7FFFFFFF
    }

    private fun matPal(r: IntArray) = mapOf('D' to r[1], 'M' to r[2], 'B' to r[3], 'L' to r[4], 'H' to r[5])
    private fun framePal(r: IntArray) = mapOf('o' to r[0], 'F' to r[5], 'f' to r[4], 'k' to r[2])
    private val glass = mapOf('a' to c(0xFF34507A), 'b' to c(0xFF5C8EBC), 'c' to c(0xFF9CCCE8), 'w' to c(0xFFE8F8FF))
    private val brass = mapOf('y' to c(0xFFF6D86A), 'Y' to c(0xFFA8782E))
    private val flowers = mapOf('g' to c(0xFF6CB04A), 'G' to c(0xFF3E7A3E), 'p' to c(0xFFF49AB4), 'P' to c(0xFFD0587E),
        'q' to c(0xFFFCE27A), 'Q' to c(0xFFE0A63A))

    class Style(
        val wall: Int, val roof: Int, val trim: Int,
        val siding: Int = SIDING,           // SIDING, BOARDS, STONE
        val door: Int = c(0xFF8A5634),
        val shutters: Int? = null,
        val chimney: Int? = null,           // chimney x offset from the right edge, or null
        val porch: Boolean = false,
        val flowerBoxes: Boolean = false,
        val awning: Int? = null,            // second awning stripe colour
        val glassDoor: Boolean = false,
        val sign: Int = 0,                  // 0 none, 1 shop, 2 plus, 3 anvil
        val windows: Int = 2,
        val forgeGlow: Boolean = false
    )
    const val SIDING = 0; const val BOARDS = 1; const val STONE = 2

    // =============================================================================================
    // Hip-roofed building (house, store, healer centre, smithy, cottages)
    // =============================================================================================
    fun hipped(tw: Int, th: Int, doorX: Int, st: Style): Px {
        val fw = tw * 16; val W = fw + 8; val ox = 4
        val rise = if (th >= 5) 18 else 14
        val H = th * 16 + rise
        val p = Px(W, H)
        val fh = 4
        val wallH = if (th >= 5) 40 else 32
        val eave = H - fh - wallH          // first wall row
        val ridge = if (st.chimney != null) 7 else 3
        val R = ramp(st.roof); val Wl = ramp(st.wall); val T = ramp(st.trim)

        // ---- walls ----
        wallFill(p, ox, eave, fw, wallH, Wl, st.siding, tw * 31 + th)
        // corner posts
        for (y in eave until eave + wallH) {
            p.set(ox, y, T[3]); p.set(ox + 1, y, T[5]); p.set(ox + 2, y, T[4])
            p.set(ox + fw - 3, y, T[3]); p.set(ox + fw - 2, y, T[2]); p.set(ox + fw - 1, y, T[1])
        }
        // shadow cast by the eave (cool, two steps)
        for (x in ox until ox + fw) {
            p.set(x, eave, mix(p.get(x, eave), COOL, 0.55f)); p.set(x, eave + 1, mix(p.get(x, eave + 1), COOL, 0.4f))
            p.set(x, eave + 2, mix(p.get(x, eave + 2), COOL, 0.22f))
        }
        // foundation
        foundation(p, ox - 1, H - fh, fw + 2, fh, tw * 7 + th)

        // ---- door ----
        val dX = ox + doorX * 16 + 8
        val doorW = if (st.glassDoor) 18 else 14
        val dLeft = dX - doorW / 2
        val doorTop = H - fh - 24
        if (st.glassDoor) {
            stamp(p, Sprites.GLASS_DOOR, dLeft, doorTop, framePal(T) + glass + brass + ('M' to Wl[1]))
        } else {
            stamp(p, Sprites.DOOR, dLeft, doorTop, framePal(T) + matPal(ramp(st.door)) + brass)
        }
        // stone step
        for (x in dLeft - 2 until dLeft + doorW + 2) for (y in H - fh until H) {
            val e = x == dLeft - 2 || x == dLeft + doorW + 1 || y == H - 1
            p.set(x, y, if (y == H - fh) c(0xFFC8C0B4) else if (e) c(0xFF6E6476) else c(0xFFA49C9C))
        }

        // ---- windows (+ shutters, flower boxes) ----
        val winW = 14
        val winY = eave + 6 + (if (th >= 5) 2 else 0)
        val slots = ArrayList<Int>()
        if (st.windows >= 2) { slots.add(ox + 10); slots.add(ox + fw - 10 - winW) }
        else if (st.windows == 1) slots.add(if (doorX * 16 + 8 > fw / 2) ox + 9 else ox + fw - 9 - winW)
        for (wx in slots) {
            if (abs(wx + winW / 2 - dX) < winW / 2 + doorW / 2 + 4) continue
            stamp(p, Sprites.WINDOW, wx, winY, framePal(T) + glass)
            st.shutters?.let { sc ->
                val sp = matPal(ramp(sc)) + ('o' to ramp(sc)[0])
                stamp(p, Sprites.SHUTTER, wx - 4, winY + 1, sp); stamp(p, Sprites.SHUTTER, wx + winW, winY + 1, sp)
            }
            if (st.flowerBoxes) stamp(p, Sprites.FLOWERBOX, wx, winY + 14, flowers + matPal(ramp(st.door)) + ('o' to ramp(st.door)[0]))
        }

        // ---- roof ----
        roofHip(p, W, ridge, eave, R, st.roof)
        // fascia board under the roof edge
        for (x in 1 until W - 1) { p.set(x, eave - 2, T[4]); p.set(x, eave - 1, T[2]) }
        p.set(0, eave - 2, T[1]); p.set(W - 1, eave - 2, T[0]); p.set(0, eave - 1, T[0]); p.set(W - 1, eave - 1, T[0])

        // ---- porch roof over the door ----
        if (st.porch) {
            val pl = dLeft - 5; val pr = dLeft + doorW + 5; val py = doorTop - 5
            for (y in py until py + 4) for (x in pl + (py + 3 - y) / 2 until pr - (py + 3 - y) / 2) {
                val r = y - py
                p.set(x, y, if (r == 0) R[4] else if (r == 3) R[1] else if ((x + r) % 4 == 0) R[2] else R[3])
            }
            for (x in pl until pr) { p.set(x, py + 4, T[4]); p.set(x, py + 5, T[1]) }
            for (y in py + 6 until H - fh) {
                p.set(pl + 1, y, T[4]); p.set(pl + 2, y, T[2]); p.set(pr - 3, y, T[4]); p.set(pr - 2, y, T[2])
            }
            for (x in pl + 3 until pr - 3) p.set(x, py + 6, mix(p.get(x, py + 6), COOL, 0.35f))
        }

        // ---- shop awning ----
        st.awning?.let { a2 ->
            val A = ramp(st.trim); val B = ramp(a2)
            val top = eave; val ah = 7
            for (x in ox + 2 until ox + fw - 2) {
                val stripe = ((x - ox - 2) / 6) % 2 == 0
                val rr = if (stripe) A else B
                val scallop = (x - ox - 2) % 6
                val depth = if (scallop == 0 || scallop == 5) ah - 1 else ah
                for (y in 0 until depth + 1) {
                    val col = when {
                        y == 0 -> rr[5]
                        y == depth -> rr[1]
                        y >= depth - 1 -> rr[2]
                        scallop == 0 -> rr[2]
                        else -> rr[if (y < 3) 4 else 3]
                    }
                    p.set(x, top + y, col)
                }
                p.set(x, top + depth + 1, mix(p.get(x, top + depth + 1), COOL, 0.45f))
            }
        }

        // ---- chimney ----
        st.chimney?.let { off ->
            val cx = W - off - 9; val cw = 9
            val cr = ramp(c(0xFFA2564A))
            val bottom = roofYAt(W, ridge, eave, cx) + 8
            for (y in 2 until bottom) for (x in cx until cx + cw) {
                val row = (y - 2) / 3; val sh = if (row % 2 == 0) 0 else 2
                val mortar = (y - 2) % 3 == 2 || (x - cx + sh) % 4 == 0
                var col = if (mortar) cr[2] else if (hash(x, y, 9) % 5 == 0) cr[4] else cr[3]
                if (x == cx) col = cr[4]
                if (x >= cx + cw - 2) col = if (mortar) cr[1] else cr[2]
                p.set(x, y, col)
            }
            for (x in cx - 1 until cx + cw + 1) { p.set(x, 0, c(0xFF8C8698)); p.set(x, 1, c(0xFF5E566E)) }
            for (x in cx + 1 until cx + cw - 1) p.set(x, 2, c(0xFF2A2030))
        }

        // ---- signs ----
        when (st.sign) {
            1 -> { val g = Sprites.SIGN_SHOP; stamp(p, g, W / 2 - g[0].length / 2, eave - 22, framePal(T) + matPal(ramp(c(0xFFC89A5A))) + flowers) }
            2 -> { val g = Sprites.SIGN_PLUS; stamp(p, g, W / 2 - g[0].length / 2, eave - 19, mapOf('o' to R[0]) + matPal(ramp(c(0xFFF6F2EA))) + mapOf('p' to c(0xFFF07090), 'P' to c(0xFFC0405E))) }
            3 -> { val g = Sprites.SIGN_ANVIL; stamp(p, g, W / 2 - g[0].length / 2, eave - 18, mapOf('o' to c(0xFF3A2A2A)) + matPal(ramp(c(0xFF9A6A42))) +
                    mapOf('c' to c(0xFF3A3448), 'b' to c(0xFF6A6478), 'q' to c(0xFFF6B040), 'Q' to c(0xFFE0602A))) }
        }

        // ---- forge glow window (smithy) ----
        if (st.forgeGlow) {
            val gx = ox + fw - 30; val gy = eave + 10
            for (y in 0 until 14) for (x in 0 until 16) {
                val edge = x == 0 || y == 0 || x == 15 || y == 13
                val inner = c(if (y > 8) 0xFFF6A040 else if (y > 4) 0xFFD8602E else 0xFF7A2E2A)
                p.set(gx + x, gy + y, if (edge) T[0] else if (x == 1 || y == 1) T[4] else if ((x + y) % 7 == 0 && y > 9) c(0xFFFFE08A) else inner)
            }
            for (x in gx - 1 until gx + 17) p.set(x, gy + 14, T[4])
        }

        insideOutline(p)
        return p
    }

    private fun roofYAt(W: Int, ridge: Int, eave: Int, x: Int): Int {
        val inset = ridgeInset(W)
        for (y in ridge until eave) {
            val t = (y - ridge).toFloat() / (eave - 2 - ridge)
            val l = (inset * (1 - t)).roundToInt()
            if (x >= l && x < W - l) return y
        }
        return eave
    }

    private fun ridgeInset(W: Int) = min(24, W / 4)

    private fun roofHip(p: Px, W: Int, ridge: Int, eave: Int, R: IntArray, base: Int) {
        val inset = ridgeInset(W)
        val bot = eave - 2
        val span = (bot - ridge).toFloat()
        for (y in ridge until bot) {
            val t = (y - ridge) / span
            val outer = (inset * (1 - t)).roundToInt()
            val inner = outer + (9 * t).roundToInt() + 1
            // light falls from the top-left: rows near the ridge are a touch warmer
            val warmth = (1 - t) * 0.12f
            for (x in outer until W - outer) {
                val col: Int
                if (x < inner) {           // left hip plane (lit)
                    val r = (y - ridge) % 4
                    col = if (r == 3) R[3] else if (x == outer) R[4] else R[5]
                } else if (x >= W - inner) { // right hip plane (shade)
                    val r = (y - ridge) % 4
                    col = if (r == 3) R[0] else if (x == W - outer - 1) R[0] else R[1]
                } else {
                    // front plane: staggered shingles 6px wide, 4px rows
                    val row = (y - ridge) / 4; val r = (y - ridge) % 4
                    val sx = (x + (if (row % 2 == 0) 0 else 3)) % 6
                    val v = hash((x + (if (row % 2 == 0) 0 else 3)) / 6, row, 3) % 6
                    var cc = when {
                        r == 3 -> R[1]
                        sx == 5 -> R[2]
                        r == 0 && sx == 0 -> R[5]
                        r == 0 -> R[4]
                        v == 0 -> R[2]
                        v == 1 -> R[4]
                        else -> R[3]
                    }
                    if (warmth > 0 && r != 3) cc = mix(cc, WARM, warmth)
                    col = cc
                }
                p.set(x, y, col)
            }
            // hip lines
            p.set(inner, y, R[2]); p.set(W - inner - 1, y, R[1])
        }
        // ridge cap
        for (x in inset - 1 until W - inset + 1) {
            p.set(x, ridge - 2, R[5]); p.set(x, ridge - 1, if ((x - inset) % 5 == 0) R[3] else R[4])
            p.set(x, ridge, R[2])
        }
        // drip edge: darker last shingle row, slightly flared
        for (x in 0 until W) { p.set(x, bot - 1, if (x % 6 == 0) R[1] else R[2]) }
    }

    private fun wallFill(p: Px, x0: Int, y0: Int, w: Int, h: Int, r: IntArray, kind: Int, seed: Int) {
        for (y in y0 until y0 + h) for (x in x0 until x0 + w) {
            val ly = y - y0; val lx = x - x0
            val col = when (kind) {
                BOARDS -> {
                    val b = lx % 5; val board = lx / 5
                    val n = hash(board, ly / 3, seed) % 9
                    when {
                        b == 4 -> r[1]
                        b == 0 -> r[4]
                        n == 0 && b == 2 -> r[2]
                        else -> r[3]
                    }
                }
                STONE -> {
                    val row = ly / 6; val sh = if (row % 2 == 0) 0 else 5
                    val bx = (lx + sh) % 10; val by = ly % 6
                    val v = hash((lx + sh) / 10, row, seed) % 4
                    when {
                        by == 5 || bx == 9 -> r[1]
                        by == 0 || bx == 0 -> r[5 - (if (v == 0) 1 else 0)]
                        by == 4 || bx == 8 -> r[2]
                        v == 1 -> r[4]
                        v == 2 -> r[2]
                        else -> r[3]
                    }
                }
                else -> {
                    val by = ly % 5
                    val n = hash(lx / 9, ly / 5, seed) % 11
                    when {
                        by == 0 -> r[5]
                        by == 4 -> r[1]
                        by == 3 -> r[2]
                        n == 0 && lx % 9 == 4 -> r[2]
                        else -> r[3]
                    }
                }
            }
            // gentle falloff toward the shaded right side
            val f = lx.toFloat() / w
            p.set(x, y, if (f > 0.82f) mix(col, COOL, (f - 0.82f) * 0.9f) else col)
        }
    }

    private fun foundation(p: Px, x0: Int, y0: Int, w: Int, h: Int, seed: Int) {
        val s = ramp(c(0xFF9A928E))
        for (y in y0 until y0 + h) for (x in x0 until x0 + w) {
            val ly = y - y0; val sh = if (ly / 2 % 2 == 0) 0 else 3
            val bx = (x - x0 + sh) % 7
            val v = hash((x - x0 + sh) / 7, ly / 2, seed) % 3
            p.set(x, y, when {
                ly == 0 -> s[1]
                ly == h - 1 -> s[0]
                bx == 6 -> s[1]
                ly % 2 == 1 -> s[2]
                bx == 0 -> s[5]
                v == 0 -> s[4]
                else -> s[3]
            })
        }
    }

    // =============================================================================================
    // Barn: gambrel gable facing the viewer, vertical boards, big braced double doors and a hayloft.
    // =============================================================================================
    fun barn(tw: Int, th: Int, doorX: Int): Px {
        val fw = tw * 16; val W = fw + 8; val ox = 4
        val H = th * 16 + 20
        val p = Px(W, H)
        val R = ramp(c(0xFF5E5466)); val Wd = ramp(c(0xFFB83A30)); val T = ramp(c(0xFFF2EADA))
        val fh = 4
        val eaveY = H - fh - 36              // where the gambrel meets the side walls
        val cx = W / 2f
        val knee = 0.36f                      // fraction of half-width where the slope breaks
        fun half(y: Int, top: Int): Float {   // half-width of the gambrel profile at row y
            val ht = eaveY - top
            val kneeY = top + ht * 0.55f
            return if (y >= kneeY) {
                val t = (y - kneeY) / (eaveY - kneeY)
                (W / 2f) * (knee + (1 - knee) * (0.72f + 0.28f * t)).coerceAtMost(1f) * (0.86f + 0.14f * t)
            } else {
                val t = (y - top) / (kneeY - top)
                (W / 2f) * (0.18f + (knee + 0.4f - 0.18f) * t)
            }
        }
        val top = 2
        // roof band (outer) then the gable wall inset 5px
        for (y in top until eaveY + 2) {
            val hw = half(y, top)
            for (x in 0 until W) {
                val d = abs(x + 0.5f - cx)
                if (d > hw) continue
                val e = hw - d
                val innerHw = if (y >= top + 6) half(y - 6, top) - 4 else -1f
                if (d <= innerHw && y < eaveY + 2) continue
                val r = (y + (x / 5)) % 3
                val left = x < cx
                p.set(x, y, when {
                    e < 1.2f -> R[0]
                    e < 2.4f -> if (left) R[5] else R[2]
                    r == 0 -> if (left) R[3] else R[1]
                    else -> if (left) R[4] else R[2]
                })
            }
        }
        // gable wall boards
        for (y in top + 6 until eaveY + 2) {
            val hw = half(y - 6, top) - 4
            for (x in 0 until W) {
                val d = abs(x + 0.5f - cx); if (d > hw) continue
                val b = x % 5
                p.set(x, y, when { b == 4 -> Wd[1]; b == 0 -> Wd[4]; hash(x / 5, y / 4, 5) % 8 == 0 -> Wd[2]; else -> Wd[3] })
            }
            // shadow under the roof band
            for (x in 0 until W) {
                val d = abs(x + 0.5f - cx)
                if (d <= hw && d > hw - 2) p.set(x, y, mix(p.get(x, y), COOL, 0.45f))
            }
        }
        // lower walls
        wallFill(p, ox, eaveY + 2, fw, H - fh - eaveY - 2, Wd, BOARDS, 77)
        for (x in ox until ox + fw) for (k in 0..1) p.set(x, eaveY + 2 + k, mix(p.get(x, eaveY + 2 + k), COOL, if (k == 0) 0.5f else 0.28f))
        // white corner trim
        for (y in eaveY + 2 until H - fh) {
            p.set(ox, y, T[3]); p.set(ox + 1, y, T[5]); p.set(ox + 2, y, T[4])
            p.set(ox + fw - 3, y, T[3]); p.set(ox + fw - 2, y, T[2]); p.set(ox + fw - 1, y, T[1])
        }
        // eave line (roof meeting walls) - short overhang ledges
        for (x in 0 until W) {
            val d = abs(x + 0.5f - cx); val hw = half(eaveY + 1, top)
            if (d <= hw && d > half(eaveY - 5, top) - 4) { p.set(x, eaveY + 1, R[1]) }
        }
        foundation(p, ox - 1, H - fh, fw + 2, fh, 31)

        // hayloft door in the gable
        run {
            val hx = (cx - 8).toInt(); val hy = top + 14
            for (y in 0 until 16) for (x in 0 until 16) {
                val edge = x < 2 || x > 13 || y < 2 || y > 13
                val col = if (edge) (if (x < 2 || y < 2) T[4] else T[2]) else {
                    val dd = abs(x - y) <= 0 || abs(x + y - 15) <= 0
                    if (dd) T[3] else if (x % 4 == 0) Wd[1] else Wd[2]
                }
                p.set(hx + x, hy + y, col)
            }
            for (x in hx until hx + 16) p.set(x, hy + 16, T[1])
            // pulley beam
            for (x in hx + 5 until hx + 11) { p.set(x, hy - 4, c(0xFF6A4634)); p.set(x, hy - 3, c(0xFF4A3034)) }
            p.set(hx + 8, hy - 2, c(0xFF2E1E26)); p.set(hx + 8, hy - 1, c(0xFF2E1E26))
        }

        // big braced double doors
        val dX = ox + doorX * 16 + 8
        val dw = 30; val dh = 28
        val dl = dX - dw / 2; val dt = H - fh - dh
        for (y in dt - 2 until H - fh) for (x in dl - 2 until dl + dw + 2) {
            val ly = y - dt; val lx = x - dl
            val frame = ly < 0 || lx < 0 || lx >= dw
            val col = if (frame) (if (ly == -2 || lx == -2) T[5] else if (lx >= dw) T[2] else T[3]) else {
                val leafX = if (lx < dw / 2) lx else lx - dw / 2
                val lw = dw / 2
                val border = leafX < 2 || leafX >= lw - 2 || ly < 2 || ly >= dh - 2
                val diag = abs(leafX * dh / lw - ly) <= 1 || abs((lw - 1 - leafX) * dh / lw - ly) <= 1
                when {
                    border -> if (leafX < 1 || ly < 1) T[5] else if (leafX >= lw - 1 || ly >= dh - 1) T[1] else T[3]
                    diag -> T[4]
                    else -> if (lx % 4 == 3) Wd[0] else if (lx % 4 == 0) Wd[3] else Wd[2]
                }
            }
            p.set(x, y, col)
        }
        for (y in dt until H - fh) p.set(dX, y, T[0])
        p.set(dX - 2, dt + dh / 2, c(0xFF2E2838)); p.set(dX + 1, dt + dh / 2, c(0xFF2E2838))
        // small windows either side
        for (wx in listOf(ox + 7, ox + fw - 7 - 10)) {
            if (abs(wx + 5 - dX) < dw / 2 + 8) continue
            val wy = eaveY + 8
            for (y in 0 until 10) for (x in 0 until 10) {
                val edge = x == 0 || y == 0 || x == 9 || y == 9
                p.set(wx + x, wy + y, if (edge) (if (x == 0 || y == 0) T[4] else T[2]) else if (x == 5 || y == 5) T[3]
                    else if (x + y < 7) c(0xFF9CCCE8) else c(0xFF5C8EBC))
            }
        }
        // weathervane on the ridge
        val vx = cx.toInt()
        for (y in 0 until top + 1) p.set(vx, y, c(0xFF4A4458))
        p.set(vx - 2, 0, c(0xFF4A4458)); p.set(vx - 1, 0, c(0xFF4A4458)); p.set(vx + 1, 0, c(0xFFE0A63A)); p.set(vx + 2, 0, c(0xFFE0A63A))
        insideOutline(p)
        return p
    }

    // =============================================================================================
    // Cave mouth: layered rock with a timber frame.
    // =============================================================================================
    fun cave(tw: Int, th: Int): Px {
        val W = tw * 16 + 8; val H = th * 16 + 6
        val p = Px(W, H)
        val R = ramp(c(0xFF8A7A6C)); val M = ramp(c(0xFF6E8A4A))
        val cx = W / 2f
        // boulder mass built from overlapping lumps, each shaded top-left
        val lumps = listOf(
            floatArrayOf(cx, H * 0.62f, W * 0.5f, H * 0.5f),
            floatArrayOf(cx - W * 0.26f, H * 0.42f, W * 0.24f, H * 0.34f),
            floatArrayOf(cx + W * 0.24f, H * 0.4f, W * 0.26f, H * 0.36f),
            floatArrayOf(cx - 2f, H * 0.26f, W * 0.24f, H * 0.24f),
            floatArrayOf(cx - W * 0.4f, H * 0.78f, W * 0.14f, H * 0.24f),
            floatArrayOf(cx + W * 0.4f, H * 0.76f, W * 0.15f, H * 0.26f)
        )
        val mask = BooleanArray(W * H)
        for (l in lumps) for (y in 0 until H) for (x in 0 until W) {
            val dx = (x + 0.5f - l[0]) / l[2]; val dy = (y + 0.5f - l[1]) / l[3]
            if (dx * dx + dy * dy <= 1f + (hash(x / 2, y / 2, 4) % 10) / 90f) mask[y * W + x] = true
        }
        // chiselled facets: nearest-seed cells, each with its own facing toward or away from the top-left light
        val seeds = ArrayList<FloatArray>()
        var k = 0
        for (gy in 0 until H step 9) for (gx in 0 until W step 11) {
            seeds.add(floatArrayOf(gx + (hash(gx, gy, 11) % 11).toFloat(), gy + (hash(gx, gy, 12) % 9).toFloat(), k++.toFloat()))
        }
        val owner = IntArray(W * H) { -1 }
        for (y in 0 until H) for (x in 0 until W) {
            if (!mask[y * W + x]) continue
            var best = 1e9f; var bi = 0
            for ((i, sd) in seeds.withIndex()) { val dx = x - sd[0]; val dy = (y - sd[1]) * 1.3f; val d = dx * dx + dy * dy; if (d < best) { best = d; bi = i } }
            owner[y * W + x] = bi
        }
        for (y in 0 until H) for (x in 0 until W) {
            val o = owner[y * W + x]; if (o < 0) continue
            val sd = seeds[o]
            var lit = -1f
            for (l in lumps) {
                val dx = (sd[0] - l[0]) / l[2]; val dy = (sd[1] - l[1]) / l[3]
                if (dx * dx + dy * dy <= 1.1f) lit = max(lit, -dx * 0.7f - dy * 0.9f)
            }
            lit += ((hash(o, 0, 21) % 100) - 50) / 160f - (sd[1] / H) * 0.35f
            var t = when { lit > 0.75f -> 5; lit > 0.3f -> 4; lit > -0.15f -> 3; lit > -0.55f -> 2; else -> 1 }
            val right = if (x + 1 < W) owner[y * W + x + 1] else -1
            val below = if (y + 1 < H) owner[(y + 1) * W + x] else -1
            val above = if (y > 0) owner[(y - 1) * W + x] else -1
            if ((right >= 0 && right != o) || (below >= 0 && below != o)) t = max(1, t - 2)
            else if (above >= 0 && above != o && t < 5) t += 1
            p.set(x, y, R[t])
        }
        // cracks
        for (k in 0 until 9) {
            var x = (hash(k, 1, 7) % W); var y = (hash(k, 2, 7) % (H - 10)) + 4
            repeat(5 + k % 3) { if (p.get(x, y) != 0) p.set(x, y, R[1]); x += if (hash(k, it, 8) % 2 == 0) 1 else -1; y++ }
        }
        // moss tufts on top-left lit areas
        for (y in 1 until H - 6) for (x in 0 until W) {
            if (p.get(x, y) == 0 || p.get(x, y - 1) != 0) continue
            if (hash(x / 4, 0, 2) % 3 == 0) continue
            p.set(x, y, M[4]); p.set(x, y + 1, M[3]); if (x % 3 == 0) p.set(x, y + 2, M[2])
        }
        // the dark mouth
        val mw = 26; val mh = 34
        val mx = (cx - mw / 2).toInt(); val my = H - mh
        for (y in my until H) for (x in mx until mx + mw) {
            val dx = (x + 0.5f - cx) / (mw / 2f); val dy = (y + 0.5f - (my + mw / 2f)) / (mw / 2f)
            if (y < my + mw / 2 && dx * dx + dy * dy > 1f) continue
            val depth = (y - my).toFloat() / mh
            p.set(x, y, mix(c(0xFF0C0A12), c(0xFF2A2234), (1 - depth) * 0.3f + 0.05f * (abs(dx))))
        }
        // timber frame
        val wood = ramp(c(0xFF9A6A42))
        for (y in my + 6 until H) for (k in 0 until 4) {
            val l = mx - 3 + k; val r = mx + mw - 1 + k
            p.set(l, y, wood[if (k == 0) 0 else if (k == 1) 4 else if (k == 2) 3 else 1])
            p.set(r, y, wood[if (k == 0) 0 else if (k == 1) 3 else if (k == 2) 2 else 0])
        }
        for (x in mx - 5 until mx + mw + 5) for (k in 0 until 4) {
            p.set(x, my + 4 + k, wood[if (k == 0) 5 else if (k == 3) 1 else if (x % 7 == 0) 2 else 3])
        }
        for (x in mx - 1 until mx + mw + 1) p.set(x, my + 8, c(0xFF0C0A12))
        // lantern
        val lx = mx + mw + 5; val ly = my + 12
        p.set(lx, ly - 1, c(0xFF3A3448)); p.set(lx - 1, ly, c(0xFF3A3448)); p.set(lx, ly, c(0xFFFCE27A)); p.set(lx + 1, ly, c(0xFF3A3448))
        p.set(lx, ly + 1, c(0xFFF6A040)); p.set(lx - 1, ly + 1, c(0xFF3A3448)); p.set(lx + 1, ly + 1, c(0xFF3A3448)); p.set(lx, ly + 2, c(0xFF3A3448))
        insideOutline(p)
        return p
    }

    // ---------------------------------------------------------------------------------------------
    private fun stamp(p: Px, rows: List<String>, x: Int, y: Int, pal: Map<Char, Int>) = p.blit(Px.grid(rows, pal), x, y)

    /** Darkens every opaque pixel touching transparency into a hue-shifted rim (selective outline). */
    private fun insideOutline(p: Px) {
        val src = p.p.copyOf()
        for (y in 0 until p.h) for (x in 0 until p.w) {
            val v = src[y * p.w + x]; if (v == 0) continue
            var edge = false
            if (x == 0 || src[y * p.w + x - 1] == 0) edge = true
            else if (x == p.w - 1 || src[y * p.w + x + 1] == 0) edge = true
            else if (y == 0 || src[(y - 1) * p.w + x] == 0) edge = true
            if (edge) p.p[y * p.w + x] = mix(v, c(0xFF1E1428), 0.62f)
        }
    }

    @Suppress("unused") private fun clamp(v: Int, a: Int, b: Int) = max(a, min(b, v))
}
