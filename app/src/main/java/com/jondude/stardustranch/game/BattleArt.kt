package com.jondude.stardustranch.game

import kotlin.math.abs
import kotlin.math.sin

/**
 * Battle scenery: 192x108 pixel backdrops per biome (sky bands with dithered seams, clouds or stars,
 * layered far hills / peaks / sea, a tree line, textured ground) assembled from hand-authored pieces,
 * plus 80x22 lit platforms the creatures stand on. Light from the top-left; far layers fade toward the sky.
 * Original art for Stardust Ranch.
 */
object BattleArt {
    const val FOREST = 0; const val BEACH = 1; const val MOUNTAIN = 2; const val MINE = 3
    const val BW = 192; const val BH = 108
    private const val HZ = 46 // horizon row
    private fun c(v: Long) = v.toInt()
    private val S = Px.Companion

    // ---- hand-authored pieces ----
    private val CLOUD_A = listOf(
        "..........hhhh..........",
        "......hhhhllllhh........",
        "....hhlllllllllllhh.....",
        "..hhllllllllllllllllhh..",
        ".hlllllllllllllllmmmmlh.",
        "hllllllllllmmmmmmmmmmmmh",
        "mmmmmmmmmmmmmmmmmmmmddd.",
        "..ddddddddddddddddd.....")
    private val CLOUD_B = listOf(
        ".....hhhh.......",
        "...hhllllhh.hh..",
        ".hhlllllllhhllh.",
        "hlllllllllllllmh",
        "mmmmmmmmmmmmmmdd",
        "..dddddddddddd..")
    private val PINE = listOf(
        "....d....",
        "...dmd...",
        "...dmd...",
        "..ddmmd..",
        "..dmmmd..",
        ".ddmmmdd.",
        "..dmmmd..",
        ".ddmmmmd.",
        "ddmmmmmdd",
        ".dmmmmmd.",
        "ddmmmmmmd",
        "dmmmmmmmd",
        "....t....",
        "....t....")
    private val ROUND = listOf(
        "....dddd....",
        "..ddmmmmdd..",
        ".dmmlmmmmmd.",
        "dmmllmmmmmmd",
        "dmmlmmmmmmmd",
        "dmmmmmmmmmdd",
        ".ddmmmmmmdd.",
        "..dddtdddd..",
        ".....t......",
        ".....t......")
    private val TUFT = listOf(".m.m.", "mlmlm")
    private val TUFT2 = listOf("..m..", ".mlm.", "mlmlm")
    private val PEBBLE = listOf(".ll.", "lmmd", ".dd.")
    private val STALACTITE = listOf("dmmmd", "dmmd.", ".dmd.", ".dm..", "..d..", "..d..")
    private val CRYSTAL = listOf(
        "....h......",
        "...hlm.....",
        "...hlm..h..",
        "...hlm.hlm.",
        "h..hlm.hlm.",
        "hm.hlmmhlm.",
        "hlmhlmmhlmm",
        "hlmhlmmhlmm",
        "hlmhlmmhlmm",
        ".lmhlmmhlm.",
        "ddddddddddd")
    private val SHELL = listOf(".lhl.", "lhlhl", ".mmm.")

    private fun stamp(p: Px, grid: List<String>, x: Int, y: Int, pal: Map<Char, Int>) {
        for ((yy, row) in grid.withIndex()) for ((xx, ch) in row.withIndex()) { val col = pal[ch] ?: continue; p.set(x + xx, y + yy, col) }
    }

    /** Ordered 2x2 dither between two colours: t in 0..1 */
    private fun dith(x: Int, y: Int, a: Int, b: Int, t: Float): Int {
        val th = floatArrayOf(0.125f, 0.625f, 0.875f, 0.375f)[(y and 1) * 2 + (x and 1)]
        return if (t > th) b else a
    }

    private fun bands(p: Px, y0: Int, y1: Int, cols: IntArray) {
        val n = cols.size - 1
        for (y in y0 until y1) {
            val f = (y - y0).toFloat() / (y1 - y0) * n
            val i = f.toInt().coerceAtMost(n - 1); val t = f - i
            for (x in 0 until p.w) p.set(x, y, dith(x, y, cols[i], cols[i + 1], ((t - 0.6f) / 0.4f).coerceIn(0f, 1f)))
        }
    }

    private fun ridge(x: Int, seed: Int, amp: Float, base: Int, f1: Float, f2: Float): Int =
        (base - amp * (0.55f * sin(x * f1 + seed) + 0.3f * sin(x * f2 + seed * 2.3f) + 0.15f * sin(x * 0.37f + seed * 0.7f))).toInt()

    /** Fills a silhouette layer under a ridge line, with a lit rim on slopes that face the top-left light. */
    private fun layer(p: Px, seed: Int, amp: Float, base: Int, f1: Float, f2: Float, body: Int, rim: Int, shade: Int, bottom: Int) {
        for (x in 0 until p.w) {
            val top = ridge(x, seed, amp, base, f1, f2); val prev = ridge(x - 1, seed, amp, base, f1, f2)
            for (y in top until bottom) p.set(x, y, body)
            if (top <= prev) { p.set(x, top, rim); p.set(x, top + 1, rim) } else { p.set(x, top, shade); p.set(x, top + 1, shade) }
        }
    }

    private fun sky(p: Px, night: Boolean, rnd: kotlin.random.Random) {
        if (night) {
            bands(p, 0, HZ, intArrayOf(c(0xFF141838), c(0xFF1E2650), c(0xFF2C3A6A), c(0xFF4A5888)))
            repeat(46) { val x = rnd.nextInt(BW); val y = rnd.nextInt(HZ - 8); p.set(x, y, if (rnd.nextInt(4) == 0) c(0xFFFFF4C8) else c(0xFFB8C4E8)) }
            repeat(5) { val x = rnd.nextInt(8, BW - 8); val y = rnd.nextInt(3, HZ - 14)
                p.set(x, y, c(0xFFFFFFFF)); p.set(x - 1, y, c(0xFF8A9AD0)); p.set(x + 1, y, c(0xFF8A9AD0)); p.set(x, y - 1, c(0xFF8A9AD0)); p.set(x, y + 1, c(0xFF8A9AD0)) }
            // moon with a soft crescent shadow
            val mx = 150; val my = 12
            for (y in -6..6) for (x in -6..6) if (x * x + y * y <= 36) p.set(mx + x, my + y, if ((x - 2) * (x - 2) + (y + 1) * (y + 1) <= 18) c(0xFFFFF6D8) else c(0xFFD8D4C0))
            p.set(mx - 2, my + 2, c(0xFFC4C0AC)); p.set(mx - 3, my + 1, c(0xFFC4C0AC))
        } else {
            bands(p, 0, HZ, intArrayOf(c(0xFF4A92E0), c(0xFF6AAAEA), c(0xFF8EC4F0), c(0xFFB8DCF4), c(0xFFDCEEF6)))
            val cp = mapOf('h' to c(0xFFFFFFFF), 'l' to c(0xFFF2F6FA), 'm' to c(0xFFD2E2F0), 'd' to c(0xFFAEC6E0))
            stamp(p, CLOUD_A, 18, 8, cp); stamp(p, CLOUD_B, 92, 4, cp); stamp(p, CLOUD_A, 136, 18, cp); stamp(p, CLOUD_B, 60, 22, cp)
        }
    }

    /** Ground palette per season: far (pale), mid, near, tuft light, tuft dark. */
    private fun groundPal(season: Int): IntArray = when (season) {
        0 -> intArrayOf(c(0xFF9CCC6A), c(0xFF7CBA52), c(0xFF5EA040), c(0xFFB4E07A), c(0xFF3E7E34))
        1 -> intArrayOf(c(0xFF86BC5C), c(0xFF68A646), c(0xFF4C8C38), c(0xFF9ED06A), c(0xFF2E6A30))
        2 -> intArrayOf(c(0xFFD2B05A), c(0xFFC09444), c(0xFFA4782E), c(0xFFE8C870), c(0xFF7A5226))
        else -> intArrayOf(c(0xFFF2F6FA), c(0xFFE0E8F0), c(0xFFC8D6E4), c(0xFFFFFFFF), c(0xFF9AAEC8))
    }
    private fun treePal(season: Int, far: Boolean): Map<Char, Int> {
        val base = when (season) {
            0 -> intArrayOf(c(0xFF2E6A4A), c(0xFF46885A), c(0xFF6AAA6A))
            1 -> intArrayOf(c(0xFF245A42), c(0xFF367A50), c(0xFF5A9A5E))
            2 -> intArrayOf(c(0xFF7A3A2E), c(0xFFB0602E), c(0xFFDA9440))
            else -> intArrayOf(c(0xFF4A5E6E), c(0xFF6E8494), c(0xFFDCE6EE))
        }
        val haze = c(0xFFB8D4E4)
        fun h(v: Int) = if (far) S.mix(v, haze, 0.45f) else v
        return mapOf('d' to h(base[0]), 'm' to h(base[1]), 'l' to h(base[2]), 't' to h(c(0xFF4A3428)))
    }

    private fun groundFill(p: Px, top: Int, g: IntArray, rnd: kotlin.random.Random, tufts: Boolean) {
        bands(p, top, BH, intArrayOf(g[0], g[1], g[2], g[2]))
        if (!tufts) return
        val tp = mapOf('m' to g[4], 'l' to g[3])
        repeat(34) {
            val y = rnd.nextInt(top + 3, BH - 2); val x = rnd.nextInt(-2, BW)
            val near = (y - top).toFloat() / (BH - top)
            stamp(p, if (near > 0.5f) TUFT2 else TUFT, x, y, if (near < 0.3f) mapOf('m' to S.mix(g[4], g[1], 0.5f), 'l' to g[0]) else tp)
        }
    }

    /** The backdrop behind a battle. kind = FOREST/BEACH/MOUNTAIN/MINE. */
    fun backdrop(kind: Int, season: Int, night: Boolean): Px {
        val p = Px(BW, BH)
        val rnd = kotlin.random.Random(kind * 31 + season * 7)
        val g = groundPal(season)
        when (kind) {
            MINE -> {
                bands(p, 0, BH, intArrayOf(c(0xFF1A1418), c(0xFF2A2024), c(0xFF3A2E2C), c(0xFF4A3C34)))
                // irregular rock wall: cells around scattered seeds, lit on their top-left, cracks between
                val wall = intArrayOf(c(0xFF1E181C), c(0xFF34282A), c(0xFF463830), c(0xFF5C4A3E), c(0xFF76604C))
                val seeds = List(46) { Pair(rnd.nextInt(-8, BW + 8).toFloat(), rnd.nextInt(-4, 66).toFloat()) }
                for (y in 0 until 64) for (x in 0 until BW) {
                    var d1 = 1e9f; var d2 = 1e9f; var sx = 0f; var sy = 0f
                    for ((qx, qy) in seeds) { val dd = (x - qx) * (x - qx) * 0.6f + (y - qy) * (y - qy); if (dd < d1) { d2 = d1; d1 = dd; sx = qx; sy = qy } else if (dd < d2) d2 = dd }
                    val edge = kotlin.math.sqrt(d2) - kotlin.math.sqrt(d1)
                    val lit = -((x - sx) * 0.5f + (y - sy)) / 9f
                    val depth = (1f - y / 64f) * 0.5f
                    var col = when { edge < 1.1f -> wall[0]; edge < 2.1f && lit > 0f -> wall[4]; lit > 0.5f -> wall[3]; lit > -0.4f -> wall[2]; else -> wall[1] }
                    col = S.mix(col, wall[0], depth)
                    p.set(x, y, col)
                }
                val stp = mapOf('d' to c(0xFF2A2024), 'm' to c(0xFF5A4A40))
                for (i in 0 until 9) stamp(p, STALACTITE, 6 + i * 22 + rnd.nextInt(6), 0, stp)
                // floor
                bands(p, 62, BH, intArrayOf(c(0xFF4A3E36), c(0xFF5E5046), c(0xFF6E5E52), c(0xFF7A6A5C)))
                for (x in 0 until BW) { p.set(x, 62, c(0xFF2A2024)); if (x % 7 < 4) p.set(x, 63, c(0xFF3A3030)) }
                val crys = mapOf('h' to c(0xFFF4E0FF), 'l' to c(0xFFC08AF0), 'm' to c(0xFF8A52C8), 'd' to c(0xFF3A2A4A))
                val crys2 = mapOf('h' to c(0xFFE0FFF8), 'l' to c(0xFF8AE8D8), 'm' to c(0xFF3EB0A8), 'd' to c(0xFF1E3A40))
                for ((x, y, k) in listOf(Triple(12, 53, 0), Triple(72, 54, 1), Triple(120, 52, 0), Triple(172, 53, 1))) {
                    // glow halo first
                    for (yy in -10..10) for (xx in -13..13) if (xx * xx + yy * yy * 2 < 150) { val cur = p.get(x + 5 + xx, y + 5 + yy); if (cur != 0) p.set(x + 5 + xx, y + 5 + yy, S.mix(cur, if (k == 0) c(0xFF8A52C8) else c(0xFF3EB0A8), 0.22f)) }
                    stamp(p, CRYSTAL, x, y, if (k == 0) crys else crys2)
                }
                val pp = mapOf('l' to c(0xFF8A7A6A), 'm' to c(0xFF5E5046), 'd' to c(0xFF3A3030))
                repeat(22) { stamp(p, PEBBLE, rnd.nextInt(BW), rnd.nextInt(66, BH - 3), pp) }
                return p
            }
            BEACH -> {
                sky(p, night, rnd)
                // distant island with a lit western slope
                for (x in 44..124) {
                    val k = (x - 44) / 80f
                    val top = (HZ - 1 - sin(k * Math.PI.toFloat()) * 9f - sin(x * 0.3f) * 1.2f).toInt()
                    val prev = (HZ - 1 - sin((x - 45) / 80f * Math.PI.toFloat()) * 9f - sin((x - 1) * 0.3f) * 1.2f).toInt()
                    for (y in top until HZ) p.set(x, y, if (k < 0.45f) c(0xFF86AEBA) else c(0xFF7098AA))
                    p.set(x, top, if (top <= prev) c(0xFFAACCD0) else c(0xFF6A90A2))
                }
                // sea
                bands(p, HZ, 74, intArrayOf(c(0xFF6AA8DC), c(0xFF4A8CD0), c(0xFF3A78C4), c(0xFF3070BC)))
                for (y in HZ until 70) { val n = 3 + (y - HZ) / 3
                    for (i in 0 until n) { val x = (rnd.nextInt(BW)); val l = 2 + (y - HZ) / 6
                        for (k in 0 until l) p.set(x + k, y, if (y < HZ + 6) c(0xFFE6F2FA) else c(0xFF8EC4EE)) } }
                for (x in 0 until BW) p.set(x, HZ, c(0xFFC8E4F6))
                // foam line and wet sand
                for (x in 0 until BW) { val fy = 70 + (sin(x * 0.18f) * 1.5f).toInt(); p.set(x, fy, c(0xFFF4FAFC)); p.set(x, fy + 1, c(0xFFD4ECF4))
                    for (y in fy + 2 until 77) p.set(x, y, c(0xFFC8B07C)) }
                val sand = intArrayOf(c(0xFFE2C890), c(0xFFEAD49C), c(0xFFF2DEAA), c(0xFFF4E2B4), c(0xFFC8AC74))
                for (y in 75 until BH) for (x in 0 until BW) p.set(x, y, dith(x, y, sand[0], sand[2], (y - 75) / 33f))
                repeat(40) { val x = rnd.nextInt(BW); val y = rnd.nextInt(76, BH); p.set(x, y, sand[4]); p.set(x + 1, y, sand[4]); p.set(x + 1, y - 1, sand[3]) }
                val sp = mapOf('h' to c(0xFFFFF4EA), 'l' to c(0xFFF0C4AE), 'm' to c(0xFFC9866E))
                repeat(5) { stamp(p, SHELL, rnd.nextInt(4, BW - 8), rnd.nextInt(80, BH - 4), sp) }
            }
            MOUNTAIN -> {
                sky(p, night, rnd)
                // snowy peaks
                val peak = c(0xFF8A9ABE); val peakL = c(0xFFAAB8D4); val peakD = c(0xFF6E7CA0)
                for (x in 0 until BW) {
                    val t1 = (HZ - 30 + abs(((x + 10) % 64) - 32) * 0.9f + sin(x * 0.4f) * 1.2f).toInt()
                    for (y in t1 until HZ) {
                        val left = ((x + 10) % 64) < 32
                        val snow = y < t1 + 5 + (sin(x * 0.9f) * 1.5f).toInt()
                        p.set(x, y, if (snow) (if (left) c(0xFFF8FAFC) else c(0xFFD2DCEA)) else if (left) peakL else peakD)
                    }
                    p.set(x, t1, if (((x + 10) % 64) < 32) c(0xFFFFFFFF) else c(0xFFB8C6DA))
                }
                for (x in 0 until BW) for (y in HZ - 12 until HZ) if (p.get(x, y) == peakD || p.get(x, y) == peakL) p.set(x, y, S.mix(p.get(x, y), peak, 0.6f))
                layer(p, 3, 4f, HZ + 2, 0.12f, 0.3f, S.mix(groundPal(season)[2], c(0xFF8AA4B4), 0.5f), S.mix(groundPal(season)[0], c(0xFFC8D8E0), 0.4f), S.mix(groundPal(season)[2], c(0xFF5A6A8A), 0.5f), HZ + 10)
                val tp = treePal(season, true)
                var x = -4; while (x < BW) { stamp(p, PINE, x, HZ - 6 + rnd.nextInt(3), tp); x += 7 + rnd.nextInt(9) }
                groundFill(p, HZ + 8, g, rnd, season != 3)
                val rp = mapOf('l' to c(0xFFC0BCBA), 'm' to c(0xFF8A8690), 'd' to c(0xFF5E5A68))
                repeat(10) { stamp(p, PEBBLE, rnd.nextInt(BW), rnd.nextInt(HZ + 12, BH - 3), rp) }
            }
            else -> {
                sky(p, night, rnd)
                layer(p, 1, 7f, HZ - 6, 0.05f, 0.13f, c(0xFFA8CCD4), c(0xFFC4E0E4), c(0xFF94BAC8), HZ + 2)
                val far = treePal(season, true); val near = treePal(season, false)
                var x = -6; while (x < BW) { stamp(p, if (rnd.nextBoolean()) PINE else ROUND, x, HZ - 10 + rnd.nextInt(3), far); x += 6 + rnd.nextInt(6) }
                x = -8; while (x < BW) { val pine = rnd.nextInt(3) == 0; stamp(p, if (pine) PINE else ROUND, x, HZ - (if (pine) 6 else 3) + rnd.nextInt(3), near); x += 9 + rnd.nextInt(8) }
                groundFill(p, HZ + 5, g, rnd, season != 3)
                if (season == 3) repeat(30) { val xx = rnd.nextInt(BW); val y = rnd.nextInt(HZ + 8, BH); p.set(xx, y, g[4]); p.set(xx + 1, y, g[2]) }
                if (season < 2) { val fl = intArrayOf(c(0xFFF2E05A), c(0xFFF4F4F4), c(0xFFE07AB0)); repeat(18) { val xx = rnd.nextInt(BW); val y = rnd.nextInt(HZ + 14, BH); p.set(xx, y, fl[rnd.nextInt(3)]) } }
            }
        }
        if (night) for (i in p.p.indices) { val y = i / BW; if (y >= HZ - 14) p.p[i] = S.mix(p.p[i], c(0xFF1A2244), 0.5f) }
        return p
    }

    /** Lit oval platform for a creature to stand on (80x22): grassy / sandy / rocky top, earth front face. */
    fun platform(kind: Int, season: Int): Px {
        val w = 80; val h = 22; val p = Px(w, h)
        val cx = 39.5f; val cy = 8f; val rx = 38.5f; val ry = 7.5f
        val top: IntArray = when (kind) {
            BEACH -> intArrayOf(c(0xFF8A6A42), c(0xFFD2B47C), c(0xFFE6CC94), c(0xFFF2DEAA), c(0xFFFAEECC))
            MINE -> intArrayOf(c(0xFF221A1C), c(0xFF4E4238), c(0xFF64564A), c(0xFF7A6A5C), c(0xFF94846E))
            MOUNTAIN -> if (season == 3) intArrayOf(c(0xFF4A5A7A), c(0xFFB8C8DA), c(0xFFD6E2EE), c(0xFFEEF4FA), c(0xFFFFFFFF))
                        else intArrayOf(c(0xFF2E3A2E), c(0xFF5E7A4A), c(0xFF789A5A), c(0xFF94B46C), c(0xFFB8D088))
            else -> { val g = groundPal(season); intArrayOf(S.mix(g[4], c(0xFF1A2030), 0.5f), g[4], g[2], g[1], g[0]) }
        }
        val side = if (kind == MINE) intArrayOf(c(0xFF1A1416), c(0xFF2E2426), c(0xFF3E3230)) else if (kind == BEACH) intArrayOf(c(0xFF6A4E30), c(0xFFA8865A), c(0xFFC4A272)) else intArrayOf(c(0xFF3A2420), c(0xFF6A4630), c(0xFF8A5E3E))
        // front earth face: the lower half of the oval extruded down 6px
        for (x in 0 until w) {
            val dx = (x + 0.5f - cx) / rx; if (abs(dx) > 1f) continue
            val e = kotlin.math.sqrt(1 - dx * dx) * ry
            val yb = (cy + e).toInt()
            for (y in yb until yb + 6) { val col = when { y == yb + 5 || abs(dx) > 0.97f -> side[0]; dx < -0.4f -> side[2]; else -> side[1] }; p.set(x, y, col) }
            if (x % 6 == 2) p.set(x, yb + 3, side[0])
        }
        // top face, lit from the top-left
        for (y in 0 until h) for (x in 0 until w) {
            val dx = (x + 0.5f - cx) / rx; val dy = (y + 0.5f - cy) / ry
            val d = dx * dx + dy * dy
            if (d > 1f) continue
            val light = -(dx * 0.6f + dy * 0.8f)
            val col = when { d > 0.86f -> if (dy < 0 && dx < 0.3f) top[3] else top[1]; light > 0.45f -> top[4]; light > -0.05f -> top[3]; light > -0.5f -> top[2]; else -> top[1] }
            p.set(x, y, col)
        }
        // texture: tufts / ripples / cracks
        val rnd = kotlin.random.Random(kind * 13 + season)
        repeat(16) {
            val x = rnd.nextInt(8, w - 10); val y = rnd.nextInt(3, 13)
            val dx = (x - cx) / rx; val dy = (y - cy) / ry
            if (dx * dx + dy * dy < 0.6f) when (kind) {
                FOREST -> if (season != 3) stamp(p, TUFT, x, y, mapOf('m' to top[1], 'l' to top[4])) else { p.set(x, y, top[1]); p.set(x + 1, y, top[2]) }
                BEACH -> { p.set(x, y, top[1]); p.set(x + 1, y, top[1]); p.set(x + 2, y, top[2]) }
                else -> { p.set(x, y, top[0]); p.set(x + 1, y + 1, top[0]); p.set(x, y - 1, top[4]) }
            }
        }
        // grass lip hanging over the front edge
        if (kind == FOREST || (kind == MOUNTAIN && season != 3)) for (x in 2 until w - 2 step 3) {
            val dx = (x + 0.5f - cx) / rx; if (abs(dx) > 0.95f) continue
            val yb = (cy + kotlin.math.sqrt(1 - dx * dx) * ry).toInt()
            p.set(x, yb, top[1]); p.set(x, yb + 1, top[0]); if (x % 2 == 0) p.set(x + 1, yb, top[2])
        }
        // coloured outline
        val o = Px(w, h); o.blit(p, 0, 0); o.outline(top[0])
        return o
    }
}
