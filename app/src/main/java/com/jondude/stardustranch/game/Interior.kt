package com.jondude.stardustranch.game

import android.graphics.Bitmap

/**
 * Interiors (farmhouse, store, healer's centre): hand-authored wallpaper, wainscot, windows, plank floors, rugs and
 * furniture. Each room has its own theme (palette + wallpaper + rug).
 *
 * Interior walls are read from the map by role:
 *  - upper wall face (two rows above the floor): crown moulding, wallpaper, windows with sills;
 *  - lower wall face (directly above the floor): wallpaper, chair rail, beadboard wainscot, baseboard;
 *  - everything else is the top of a wall (dark cap) with a lit moulding on the edges facing the room.
 * Floors are staggered planks whose seams follow world coordinates, with soft shadows under walls, a themed rug,
 * and a doormat on the door tile. Light comes from the top-left; ramps shift cool in shadow and warm in light.
 * Original art for Stardust Ranch.
 */
object Interior {
    private fun c(v: Long) = v.toInt()
    private val SHADE = c(0xFF1E1636)

    class Theme(
        val paper: Map<Char, Int>, val paperTex: List<String>,
        val wains: Map<Char, Int>,    // o d m b l h: beadboard/rails/baseboard ramp
        val cap: Map<Char, Int>,      // o d b l h: wall-top cap + moulding
        val floor: Map<Char, Int>,    // o d m b B l h: planks
        val trim: Map<Char, Int>,     // F f s: window frame
        val windows: Set<Int>,
        val rug: Rug?
    )

    /** An elliptical (round) or rectangular rug in floor pixel coordinates with ring colours from the outside in. */
    class Rug(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val round: Boolean, val rings: IntArray, val shades: IntArray)

    private fun ramp(vararg kv: Pair<Char, Long>) = kv.associate { it.first to c(it.second) }

    // ---------------------------------------------------------------- wallpapers (16x16, '.' = base)

    /** Farmhouse: cream paper, faint pin stripes and little rose sprigs. */
    val PAPER_HOUSE = listOf(
        "s.......s.......",
        "s...a...s.......",
        "s..aba..s.......",
        "s...a...s.......",
        "s...g...s.......",
        "s..g.g..s.......",
        "s.......s.......",
        "s.......s.......",
        "s.......s...a...",
        "s.......s..aba..",
        "s.......s...a...",
        "s.......s...g...",
        "s.......s..g.g..",
        "s.......s.......",
        "s.......s.......",
        "s.......s......."
    )
    /** Store: broad sage stripes with a fine line and small diamonds. */
    val PAPER_STORE = listOf(
        "sssa......asss..",
        "sssa......asss..",
        "sssa..b...asss..",
        "sssa.bgb..asss..",
        "sssa..b...asss..",
        "sssa......asss..",
        "sssa......asss..",
        "sssa......asss..",
        "sssa......asss..",
        "sssa......asss..",
        "sssa......asss..",
        "sssa..b...asss..",
        "sssa.bgb..asss..",
        "sssa..b...asss..",
        "sssa......asss..",
        "sssa......asss.."
    )
    /** Healer's centre: pale sky paper with tiny twinkling stars. */
    val PAPER_CENTER = listOf(
        "................",
        "...a............",
        "..aba.......s...",
        "...a............",
        "................",
        "..........a.....",
        "........s.......",
        "................",
        "............a...",
        "...........aba..",
        "....s.......a...",
        "................",
        "................",
        "......a.........",
        ".....aba.....s..",
        "......a........."
    )

    // ---------------------------------------------------------------- window (15x12), placed on the upper wall face

    val WINDOW = listOf(
        ".FFFFFFFFFFFFF.",
        ".FfffffffffffF.",
        ".FfkkkkfkkkwfF.",
        ".FfkwkKfkkwkfF.",
        ".FfkKKKfkKKKfF.",
        ".FfffffffffffF.",
        ".FfKKKKfKKKKfF.",
        ".FfKKKKfKgKKfF.",
        ".FfgKgGfGggGfF.",
        ".FfffffffffffF.",
        "SSSSSSSSSSSSSSS",
        "sssssssssssssss"
    )
    private val SKY = ramp('k' to 0xFFD4EEFA, 'K' to 0xFF9ECFF0, 'w' to 0xFFFFFFFF, 'g' to 0xFF7DB85A, 'G' to 0xFF5A9448)

    /** Door mat (16x16) on the door tile. */
    val MAT = listOf(
        "................",
        "................",
        "................",
        "..MMMMMMMMMMMM..",
        "..MnnnnnnnnnnM..",
        "..MnmmmmmmmmnM..",
        "..MnmhmhmhmmnM..",
        "..MnmmhmhmhmnM..",
        "..MnmmmmmmmmnM..",
        "..MnnnnnnnnnnM..",
        "..MMMMMMMMMMMM..",
        "...xxxxxxxxxxxx.",
        "................",
        "................",
        "................",
        "................"
    )
    private val MAT_PAL = ramp('M' to 0xFF7A3A36, 'n' to 0xFFA8563E, 'm' to 0xFFC49A5E, 'h' to 0xFFDAB676)

    // ---------------------------------------------------------------- themes

    val HOUSE = Theme(
        paper = ramp('.' to 0xFFEAD7AE, 's' to 0xFFDCC498, 'a' to 0xFFD27C70, 'b' to 0xFFF2C46A, 'g' to 0xFF7FA05A),
        paperTex = PAPER_HOUSE,
        wains = ramp('o' to 0xFF3E2422, 'd' to 0xFF5E3A2C, 'm' to 0xFF7C4E34, 'b' to 0xFF95633E, 'l' to 0xFFB37E4E, 'h' to 0xFFCE9A62),
        cap = ramp('o' to 0xFF231618, 'd' to 0xFF3A2626, 'b' to 0xFF4A3230, 'l' to 0xFF8E6448, 'h' to 0xFFB8875C),
        floor = ramp('o' to 0xFF5A3424, 'd' to 0xFF8E5C36, 'm' to 0xFFA56C3E, 'b' to 0xFFB47C46, 'B' to 0xFFBC854C, 'l' to 0xFFCB9558, 'h' to 0xFFDDAC6E),
        trim = ramp('F' to 0xFF7E6656, 'f' to 0xFFF2EADA, 'S' to 0xFFF6EEDE, 's' to 0xFFB8A894),
        windows = setOf(3, 7),
        rug = Rug(96f, 90f, 36f, 20f, true,
            intArrayOf(c(0xFF5A2A2E), c(0xFFB8483E), c(0xFFE8D2A0), c(0xFF6E944A), c(0xFFB8483E), c(0xFFE8D2A0), c(0xFFB8483E)),
            intArrayOf(c(0xFF3E1C24), c(0xFF8E3434), c(0xFFC8AE7E), c(0xFF4E7038), c(0xFF8E3434), c(0xFFC8AE7E), c(0xFF8E3434)))
    )
    val STORE = Theme(
        paper = ramp('.' to 0xFFC6D4A4, 's' to 0xFFADC08C, 'a' to 0xFF95A878, 'b' to 0xFFE8E2B8, 'g' to 0xFFC85A4A),
        paperTex = PAPER_STORE,
        wains = ramp('o' to 0xFF3A2620, 'd' to 0xFF64422C, 'm' to 0xFF845A38, 'b' to 0xFFA27244, 'l' to 0xFFC08C56, 'h' to 0xFFDAA86C),
        cap = ramp('o' to 0xFF1E1A1A, 'd' to 0xFF2E2A28, 'b' to 0xFF3C3632, 'l' to 0xFF7A6248, 'h' to 0xFFA88A62),
        floor = ramp('o' to 0xFF4A2C22, 'd' to 0xFF7A4C30, 'm' to 0xFF8E5A36, 'b' to 0xFF9C643C, 'B' to 0xFFA56C40, 'l' to 0xFFB67C4C, 'h' to 0xFFC89462),
        trim = ramp('F' to 0xFF5A3E2E, 'f' to 0xFFB07A48, 'S' to 0xFFC08A56, 's' to 0xFF7E5232),
        windows = setOf(6),
        rug = Rug(96f, 92f, 50f, 13f, false,
            intArrayOf(c(0xFF3A2A4A), c(0xFF5A4E8E), c(0xFFE0C468), c(0xFF8E4A4A)),
            intArrayOf(c(0xFF2A1E3A), c(0xFF40386E), c(0xFFB89A48), c(0xFF6E3438)))
    )
    val CENTER = Theme(
        paper = ramp('.' to 0xFFD2E2F0, 's' to 0xFFB4CCE4, 'a' to 0xFFF8FBFF, 'b' to 0xFFF6D88A, 'g' to 0xFFB4CCE4),
        paperTex = PAPER_CENTER,
        wains = ramp('o' to 0xFF5C6688, 'd' to 0xFF98A4C0, 'm' to 0xFFBCC6DA, 'b' to 0xFFD8E0EC, 'l' to 0xFFEEF2F8, 'h' to 0xFFFFFFFF),
        cap = ramp('o' to 0xFF1C1E2E, 'd' to 0xFF2C3044, 'b' to 0xFF383E56, 'l' to 0xFF7A88AC, 'h' to 0xFFA8B6D4),
        floor = ramp('o' to 0xFF7A5A3E, 'd' to 0xFFB48E62, 'm' to 0xFFC49E70, 'b' to 0xFFD2AE7E, 'B' to 0xFFD8B484, 'l' to 0xFFE2C292, 'h' to 0xFFEED6AA),
        trim = ramp('F' to 0xFF6A7494, 'f' to 0xFFF4F6FA, 'S' to 0xFFF8FAFC, 's' to 0xFFB0B8CC),
        windows = setOf(3, 9),
        rug = Rug(104f, 88f, 32f, 19f, true,
            intArrayOf(c(0xFF3C4C7A), c(0xFF6C86C4), c(0xFFF4F6FA), c(0xFFF07AA8), c(0xFFF4F6FA), c(0xFF6C86C4)),
            intArrayOf(c(0xFF2A3458), c(0xFF4C64A0), c(0xFFC8D0E2), c(0xFFC0507E), c(0xFFC8D0E2), c(0xFF4C64A0)))
    )

    fun themeFor(id: String): Theme = when (id) { "store" -> STORE; "center" -> CENTER; else -> HOUSE }

    // ---------------------------------------------------------------- composition

    private const val CAP = 0; private const val FACE_HI = 1; private const val FACE_LO = 2

    private fun open(a: Area, x: Int, y: Int) = a.inb(x, y) && a.g(x, y) != Gr.INWALL
    private fun wallRole(a: Area, x: Int, y: Int): Int {
        if (open(a, x, y + 1)) return FACE_LO
        if (a.inb(x, y + 1) && a.g(x, y + 1) == Gr.INWALL && open(a, x, y + 2)) return FACE_HI
        return CAP
    }
    /** True when (x, y) shows to the viewer as part of the room (floor or a wall face), i.e. not a wall top. */
    private fun roomy(a: Area, x: Int, y: Int): Boolean {
        if (!a.inb(x, y)) return false
        if (a.g(x, y) != Gr.INWALL) return true
        return wallRole(a, x, y) != CAP
    }

    private fun paperAt(t: Theme, gx: Int, gy: Int): Int = t.paper.getValue(t.paperTex[gy and 15][gx and 15])

    private fun cap(a: Area, t: Theme, x: Int, y: Int, out: IntArray) {
        val p = t.cap
        for (yy in 0..15) for (xx in 0..15) {
            val gy = y * 16 + yy; val gx = x * 16 + xx
            out[yy * 16 + xx] = when {
                gy % 8 == 7 -> p.getValue('d')
                (gx * 3 + gy * 5) % 29 == 0 -> p.getValue('d')
                else -> p.getValue('b')
            }
        }
        // lit moulding on edges that face the room: outline at the very edge, then light, then highlight
        val n = roomy(a, x, y - 1); val s = roomy(a, x, y + 1); val w = roomy(a, x - 1, y); val e = roomy(a, x + 1, y)
        fun put(xx: Int, yy: Int, ch: Char) { out[yy * 16 + xx] = p.getValue(ch) }
        for (i in 0..15) {
            if (s) { put(i, 15, 'o'); put(i, 14, 'l'); put(i, 13, 'h') }
            if (n) { put(i, 0, 'o'); put(i, 1, 'h'); put(i, 2, 'l') }
            if (e) { put(15, i, 'o'); put(14, i, 'l'); put(13, i, 'h') }
            if (w) { put(0, i, 'o'); put(1, i, 'h'); put(2, i, 'l') }
        }
        // inner corners (room only diagonally adjacent)
        val corners = listOf(Triple(1, 1, Pair(15, 15)), Triple(-1, 1, Pair(0, 15)), Triple(1, -1, Pair(15, 0)), Triple(-1, -1, Pair(0, 0)))
        for ((dx, dy, at) in corners) {
            if (roomy(a, x + dx, y + dy) && !roomy(a, x + dx, y) && !roomy(a, x, y + dy)) {
                val (cx, cy) = at
                val sx = if (dx > 0) -1 else 1; val sy = if (dy > 0) -1 else 1
                for (k in 0..2) for (j in 0..2) {
                    val ch = when (maxOf(k, j)) { 0 -> 'o'; 1 -> 'l'; else -> 'h' }
                    if (k == 2 && j == 2) continue
                    put(cx + sx * k, cy + sy * j, ch)
                }
            }
        }
    }

    private fun faceLo(t: Theme, x: Int, y: Int, out: IntArray) {
        val p = t.wains
        for (yy in 0..15) for (xx in 0..15) {
            val gx = x * 16 + xx; val b = gx % 4
            out[yy * 16 + xx] = when (yy) {
                in 0..4 -> paperAt(t, gx, y * 16 + yy)
                5 -> p.getValue('h'); 6 -> p.getValue('l'); 7 -> p.getValue('o')
                in 8..12 -> when (b) {
                    0 -> p.getValue('d'); 1 -> p.getValue(if (yy == 8) 'm' else 'l')
                    else -> if (yy == 8) p.getValue('d') else if ((gx * 7 + yy * 3) % 13 == 0) p.getValue('m') else p.getValue('b')
                }
                13 -> p.getValue('l'); 14 -> p.getValue('m'); else -> p.getValue('o')
            }
        }
        // the wallpaper darkens slightly just above the rail (rail casts no shadow upward, but paper meets wood)
        for (xx in 0..15) out[4 * 16 + xx] = Px.mix(out[4 * 16 + xx], SHADE, 0.10f)
    }

    private fun faceHi(t: Theme, x: Int, y: Int, out: IntArray) {
        val p = t.wains
        for (yy in 0..15) for (xx in 0..15) out[yy * 16 + xx] = paperAt(t, x * 16 + xx, y * 16 + yy)
        for (xx in 0..15) {
            out[xx] = t.cap.getValue('o'); out[16 + xx] = p.getValue('h'); out[32 + xx] = p.getValue('b')
            out[48 + xx] = p.getValue('d'); out[64 + xx] = Px.mix(out[64 + xx], SHADE, 0.22f); out[80 + xx] = Px.mix(out[80 + xx], SHADE, 0.10f)
        }
        // crown dentils
        for (xx in 0..15 step 4) out[32 + xx] = p.getValue('m')
        if (x in t.windows) {
            val pal = t.trim + SKY
            for ((ry, row) in WINDOW.withIndex()) for ((rx, ch) in row.withIndex()) {
                if (ch == '.') continue
                val oy = 4 + ry; val ox = rx
                if (oy in 0..15) out[oy * 16 + ox] = pal.getValue(ch)
            }
            // sill shadow on the paper below is off-tile; soft shade beside the frame on the right
            for (ry in 1..10) if (4 + ry <= 15) out[(4 + ry) * 16 + 14] = Px.mix(out[(4 + ry) * 16 + 14], SHADE, 0.25f)
        }
    }

    private fun floor(a: Area, t: Theme, x: Int, y: Int, out: IntArray) {
        val p = t.floor
        for (yy in 0..15) for (xx in 0..15) {
            val gx = x * 16 + xx; val gy = y * 16 + yy
            val board = gy / 4; val r = gy % 4
            val seamOff = (board * 13) % 40
            val seg = (gx + seamOff) / 40
            val atSeam = (gx + seamOff) % 40 == 0
            val tone = if (((board * 5 + seg * 3) % 7) < 3) 'B' else 'b'
            out[yy * 16 + xx] = when {
                r == 3 -> p.getValue('o')
                atSeam -> p.getValue('d')
                (gx + seamOff) % 40 == 1 -> p.getValue(if (r == 0) 'h' else 'l')
                r == 0 -> p.getValue('l')
                (gx * 3 + board * 7) % 17 == 0 && r == 2 -> p.getValue('m')
                (gx * 5 + board * 3) % 23 in 0..2 && r == 1 -> p.getValue('m')
                else -> p.getValue(tone)
            }
        }
        // rug
        t.rug?.let { rug ->
            for (yy in 0..15) for (xx in 0..15) {
                val gx = x * 16 + xx + 0.5f; val gy = y * 16 + yy + 0.5f
                val d = if (rug.round) {
                    val dx = (gx - rug.cx) / rug.rx; val dy = (gy - rug.cy) / rug.ry
                    kotlin.math.sqrt(dx * dx + dy * dy)
                } else maxOf(kotlin.math.abs(gx - rug.cx) / rug.rx, kotlin.math.abs(gy - rug.cy) / rug.ry)
                if (d >= 1f) {
                    // soft contact shadow just outside the bottom/right edge
                    if (d < 1.08f && gy > rug.cy) out[yy * 16 + xx] = Px.mix(out[yy * 16 + xx], SHADE, 0.22f)
                    continue
                }
                val bandW = if (rug.round) 1f / (rug.rings.size + 1.5f) else 0f
                val ring = if (rug.round) ((1f - d) / bandW).toInt().coerceIn(0, rug.rings.size - 1)
                    else { val inset = minOf(rug.rx - kotlin.math.abs(gx - rug.cx), rug.ry - kotlin.math.abs(gy - rug.cy)).toInt(); if (inset < 1) 0 else if (inset < 3) 1 else if (inset < 4) 2 else 3 }
                val ix = gx.toInt(); val iy = gy.toInt()
                // braid: alternate lit/shaded stitches; the lower-right of the rug is in shade
                val braid = if (rug.round) (ix * 3 + iy * 5) % 7 == 0 || ((ix + iy) % 4 == 0 && ring % 2 == 1) else (ix * 3 + iy * 5) % 11 == 0
                val lowerRight = (gx - rug.cx) / rug.rx + (gy - rug.cy) / rug.ry > 0.7f
                val col = if (ring == 0 || braid || lowerRight) rug.shades[ring] else rug.rings[ring]
                out[yy * 16 + xx] = if (ring == 0) rug.rings[0] else col
                if (!rug.round && ring == 3 && ((kotlin.math.abs(ix - rug.cx.toInt()) + kotlin.math.abs(iy - rug.cy.toInt()) * 2) % 12 == 0)) out[yy * 16 + xx] = rug.rings[2]
            }
        }
        // door mat on the door tile in the bottom wall
        if (y == a.h - 1) {
            for ((ry, row) in MAT.withIndex()) for ((rx, ch) in row.withIndex()) {
                if (ch == '.') continue
                out[ry * 16 + rx] = if (ch == 'x') Px.mix(out[ry * 16 + rx], SHADE, 0.3f) else MAT_PAL.getValue(ch)
            }
        }
        // shadows from walls to the north / west / east
        fun wall(dx: Int, dy: Int) = a.inb(x + dx, y + dy) && a.g(x + dx, y + dy) == Gr.INWALL
        if (wall(0, -1)) for (xx in 0..15) {
            out[xx] = Px.mix(out[xx], SHADE, 0.46f); out[16 + xx] = Px.mix(out[16 + xx], SHADE, 0.30f)
            out[32 + xx] = Px.mix(out[32 + xx], SHADE, 0.16f); out[48 + xx] = Px.mix(out[48 + xx], SHADE, 0.06f)
        }
        if (wall(-1, 0)) for (yy in 0..15) {
            out[yy * 16] = Px.mix(out[yy * 16], SHADE, 0.36f); out[yy * 16 + 1] = Px.mix(out[yy * 16 + 1], SHADE, 0.2f)
            out[yy * 16 + 2] = Px.mix(out[yy * 16 + 2], SHADE, 0.08f)
        }
        if (wall(1, 0)) for (yy in 0..15) out[yy * 16 + 15] = Px.mix(out[yy * 16 + 15], SHADE, 0.16f)
        if (!wall(0, -1) && wall(-1, -1)) for (xx in 0..3) { out[xx] = Px.mix(out[xx], SHADE, 0.28f); out[16 + xx] = Px.mix(out[16 + xx], SHADE, 0.12f) }
    }

    private val cache = HashMap<String, Bitmap>()

    /** The finished ground bitmap for an interior wall or floor cell. */
    fun tile(a: Area, x: Int, y: Int): Bitmap = cache.getOrPut("${a.id}/$x/$y/${a.w}x${a.h}") {
        val t = themeFor(a.id)
        val out = IntArray(256)
        if (a.g(x, y) == Gr.INWALL) when (wallRole(a, x, y)) {
            FACE_LO -> faceLo(t, x, y, out)
            FACE_HI -> faceHi(t, x, y, out)
            else -> cap(a, t, x, y, out)
        } else floor(a, t, x, y, out)
        Bitmap.createBitmap(out, 16, 16, Bitmap.Config.ARGB_8888)
    }

    // ---------------------------------------------------------------- furniture

    private val WOOD = ramp('O' to 0xFF2E1A1A, 'W' to 0xFF5A3628, 'w' to 0xFF7E4E34, 'v' to 0xFFA46C44, 'h' to 0xFFC8925C)

    val BED = listOf(
        "..OOOOOOOOOOOO..",
        ".OhhvvvvvvvvvvO.",
        "OhvvwwwwwwwwwwWO",
        "OvwWOOOOOOOOWwWO",
        "OvwOvvvvvvvvOwWO",
        "OvwOvwwwwwwWOwWO",
        "OvwOWWWWWWWWOwWO",
        "OvwWOOOOOOOOWwWO",
        "OWWWWWWWWWWWWWWO",
        "OOSSSSSSSSSSSSOO",
        ".OSSPPPPPPPPpSO.",
        ".OSPPPPPPPPPPpO.",
        ".OSPPPPPPPPPppO.",
        ".OSSppppppppsSO.",
        ".OSSSSSSSSSSSsO.",
        ".OYYYYYYYYYYYyO.",
        ".OyyyyyyyyyyyyO.",
        ".ObQQQRRRRbQQQO.",
        ".OQQQqRRRrQQQqO.",
        ".OQQQqRRRrQQQqO.",
        ".OqqqqrrrrqqqqO.",
        ".ORRRRYYYYRRRRO.",
        ".ORRRrYYYyRRRrO.",
        ".ORRRrYYYyRRRrO.",
        ".OrrrryyyyrrrrO.",
        ".ObQQQRRRRbQQQO.",
        ".OQQQqRRRrQQQqO.",
        ".OQQQqRRRrQQQqO.",
        ".OqqqqrrrrqqqqO.",
        "OvwwwwwwwwwwwwWO",
        "OWWWWWWWWWWWWWWO",
        ".OO..........OO."
    )
    private val BED_PAL = WOOD + ramp(
        'P' to 0xFFF8F4EC, 'p' to 0xFFD6CCDA, 'S' to 0xFFEDE6DA, 's' to 0xFFC4B8C2,
        'Q' to 0xFF4F7FC0, 'q' to 0xFF34528E, 'b' to 0xFF7AA8DC, 'R' to 0xFFC8564A, 'r' to 0xFF8E3440,
        'Y' to 0xFFEFD9A4, 'y' to 0xFFC6A880)

    val SHELF = listOf(
        "OOOOOOOOOOOOOOOO",
        "OhhhhhhhhhhhhhvO",
        "OvwwwwwwwwwwwwWO",
        "OvkkkkkkkkkkkkWO",
        "OvKRrKKKKKLlKKWO",
        "OvKRrBbKKLLLlKWO",
        "OvKRrBbGglLlKKWO",
        "OvKyyBbGgTTtKKWO",
        "OvKRrByGgTTtKKWO",
        "OvKRrBbGgTttKKWO",
        "OvKRrBbGgKKKKKWO",
        "OvhhhhhhhhhhhhWO",
        "OvWWWWWWWWWWWWWO",
        "OvKKPpKKKKKKKKWO",
        "OvKKPpKRrKKYyKWO",
        "OvYyPpKRrBbYyKWO",
        "OvYyPpGRrBbYyKWO",
        "OvyyPyGRrBbyyKWO",
        "OvYyPpGRrBbYyKWO",
        "OvYyPpGRrBbYyKWO",
        "OvhhhhhhhhhhhhWO",
        "OvWWWWWWWWWWWWWO",
        "OvKKKKKKKKKKKKWO",
        "OvKKKKBbKKKKKKWO",
        "OvKeeeBbKGgRrKWO",
        "OvKEEEBbKGgRrKWO",
        "OvKeeeByKGgRrKWO",
        "OvKEEEBbKGgRrKWO",
        "OvKOOOBbKGgRrKWO",
        "OvhhhhhhhhhhhhWO",
        "OWWWWWWWWWWWWWWO",
        ".OO..........OO."
    )
    private val SHELF_PAL = WOOD + ramp(
        'K' to 0xFF3A2424, 'k' to 0xFF2A1A1C,
        'R' to 0xFFC8564A, 'r' to 0xFF8E3440, 'B' to 0xFF5A86C8, 'b' to 0xFF3A5A96, 'G' to 0xFF6EA850, 'g' to 0xFF447A3C,
        'Y' to 0xFFE8C258, 'y' to 0xFFB08A34, 'P' to 0xFF9A6ACC, 'p' to 0xFF6A4496,
        'L' to 0xFF7CC05A, 'l' to 0xFF3E7A3E, 'T' to 0xFFD0784A, 't' to 0xFF94482E, 'E' to 0xFFF0E6CE, 'e' to 0xFFC8BCA2)

    val TABLE = listOf(
        "......Ff.F......",
        ".....FfFFfF.....",
        "......lFLF......",
        "......LVVuL.....",
        "..OOOOOVVuOOOO..",
        ".OhhhhhVVuvvvvO.",
        "OhvvvvvWWWvvvwwO",
        "OvvvvvvvvvvvwwWO",
        "OwwwwwwwwwwwwWWO",
        ".OWWWWWWWWWWWWO.",
        ".OOwWOOOOOOwWOO.",
        "..OwWO....OwWO..",
        "..OwWO....OwWO..",
        "..OwWO....OwWO..",
        "..OWWO....OWWO..",
        "..OOOO....OOOO.."
    )
    private val TABLE_PAL = WOOD + ramp('V' to 0xFF7AA6DA, 'u' to 0xFF46689E, 'F' to 0xFFF07A8A, 'f' to 0xFFFFD6DC, 'L' to 0xFF7CC05A, 'l' to 0xFF3E7A3E)

    val COUNTER = listOf(
        "................",
        "................",
        "OOOOOOOOOOOOOOOO",
        "hhhhhhhhhhhhhhhh",
        "vvvvvvvvvvvvvvvv",
        "vvvvvwvvvvvvvvwv",
        "wwwwwwwwwwwwwwww",
        "OOOOOOOOOOOOOOOO",
        "OWWWWWWWOWWWWWWW",
        "OvwwwwwWOvwwwwwW",
        "Ovwwww wOvwwwwwW".replace(' ', 'w'),
        "OvwwwwwWOvwwwwwW",
        "OvwwwwwWOvwwwwwW",
        "OvvvvvvWOvvvvvvW",
        "WWWWWWWWWWWWWWWW",
        "OOOOOOOOOOOOOOOO"
    )

    /** Healing fountain in the centre: a glowing glass orb in a brass collar on a marble plinth. */
    val HEALER = listOf(
        ".....oooooo.....",
        "...ooPPPPPPoo...",
        "..oPPwwPPPPPpo..",
        ".oPPwwPPPPPPPpo.",
        ".oPPwPPPPPPPPpo.",
        "oPPPPPPPPPPPPppo",
        "oPPPPPPPPPPPPppo",
        "oPPPPPPPPPPPpppo",
        "oPPPPPPPPPPPpppo",
        ".oPPPPPPPPPpppo.",
        ".opPPPPPPPppppo.",
        "..oppppppppppo..",
        "...oopppppppo...",
        "..OOOOOOOOOOOO..",
        ".OhhhhhhhhhhhyO.",
        ".OyyyyyyyyyyyYO.",
        "..OOOOOOOOOOOO..",
        "..OMMMMMMMMMmO..",
        "..OMmmmmmmmmmO..",
        "..OMmmmCCmmmmO..",
        "..OMmmmCCmmmmO..",
        "..OMmCCCCCCmmO..",
        "..OMmCCCCCCmmO..",
        "..OMmmmCCmmmmO..",
        "..OMmmmCCmmmmO..",
        "..OMmmmmmmmmdO..",
        "..OMmmmmmmmmdO..",
        ".OOOOOOOOOOOOOO.",
        ".OMMMMMMMMMMMmO.",
        ".OmmmmmmmmmmmdO.",
        ".OddddddddddddO.",
        "..OOOOOOOOOOOO.."
    )
    private val HEALER_PAL = ramp('o' to 0xFF8E2E5E, 'P' to 0xFFF27AAA, 'p' to 0xFFC84C88, 'w' to 0xFFFFE4F0,
        'O' to 0xFF4A4458, 'h' to 0xFFFFE08A, 'y' to 0xFFD8A23E, 'Y' to 0xFF9A6A2A,
        'M' to 0xFFF4F2F8, 'm' to 0xFFD8D6E4, 'd' to 0xFFAAA6C0, 'C' to 0xFFF07AA8)

    fun healer(): Bitmap = Px.grid(HEALER, HEALER_PAL).bmp()
    fun bed(): Bitmap = Px.grid(BED, BED_PAL).bmp()
    fun shelf(): Bitmap = Px.grid(SHELF, SHELF_PAL).bmp()
    fun table(): Bitmap = Px.grid(TABLE, TABLE_PAL).bmp()
    fun counter(): Bitmap = Px.grid(COUNTER, WOOD).bmp()

    /** All grids (for the harness: every row must be the same width). */
    val grids: Map<String, List<String>> get() = mapOf(
        "paperHouse" to PAPER_HOUSE, "paperStore" to PAPER_STORE, "paperCenter" to PAPER_CENTER, "window" to WINDOW, "mat" to MAT,
        "bed" to BED, "healer" to HEALER, "shelf" to SHELF, "table" to TABLE, "counter" to COUNTER)
}
