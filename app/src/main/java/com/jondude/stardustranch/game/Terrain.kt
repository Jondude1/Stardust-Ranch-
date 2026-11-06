package com.jondude.stardustranch.game

import android.graphics.Bitmap

/**
 * Ground renderer. Each 16x16 cell is composed from the hand-authored textures in [Sprites] (GRASS_TEX, SOIL_TEX,
 * ...) and blended with its eight neighbours:
 *  - higher materials overhang lower ones with a ragged lip, a coloured outline and a soft cast shadow
 *    (grass > cobble > dirt > sand > tilled soil > cave floor > water);
 *  - land meeting water gets an earth bank face, a foam line, and lighter shallows;
 *  - cave walls and cliffs show a lit rock face where they meet open ground, and cast shadows on the floor.
 * Composed tiles are cached per neighbourhood, so drawing stays a single bitmap lookup per cell.
 */
class Terrain(private val art: Art) {
    private fun c(v: Long) = v.toInt()
    private fun pal(o: Long, d: Long, m: Long, b: Long, l: Long, h: Long) =
        mapOf('o' to c(o), 'd' to c(d), 'm' to c(m), '.' to c(b), 'l' to c(l), 'h' to c(h))

    // 6-tone ramps: outline (hue-shifted dark), shadow, mid-shadow, base, light, highlight.
    private val grassPal = arrayOf(
        pal(0xFF2C5E3E, 0xFF438A3E, 0xFF58A443, 0xFF6CB84A, 0xFF88CC5C, 0xFFB2E07A), // spring
        pal(0xFF26523C, 0xFF387838, 0xFF4A9439, 0xFF57A63D, 0xFF73BD4E, 0xFF9AD566), // summer
        pal(0xFF5E4430, 0xFF8A7032, 0xFFA58A38, 0xFFB99E44, 0xFFD1B85A, 0xFFE6D27C), // fall: golden
        pal(0xFF7A8AB0, 0xFFAEBED8, 0xFFCAD6E8, 0xFFE4ECF4, 0xFFF4F8FC, 0xFFFFFFFF)  // winter: snow
    )
    private val dirtPal = pal(0xFF664232, 0xFF8A6244, 0xFF9E7650, 0xFFB58C5C, 0xFFC8A26E, 0xFFDAB884)
    private val sandPal = pal(0xFFAA8A60, 0xFFCFB078, 0xFFDDC38A, 0xFFEAD49C, 0xFFF4E4B6, 0xFFFBF0D0)
    private val cavePal = pal(0xFF342A2E, 0xFF4C4042, 0xFF5E5048, 0xFF6E5E52, 0xFF807060, 0xFF948272)
    private val tillDry = pal(0xFF4A3024, 0xFF644230, 0xFF7A5434, 0xFF8C6238, 0xFFA2774A, 0xFFB98E5E)
    private val tillWet = pal(0xFF2A1C1C, 0xFF3A2820, 0xFF4A3224, 0xFF5A3C28, 0xFF6C4C32, 0xFF80603E)
    private val cobblePal = pal(0xFF6A6270, 0xFF847C82, 0xFF948C88, 0xFFA49C90, 0xFFBAB2A4, 0xFFD2CABA) + ('g' to c(0xFF6E6670))
    private val waterPal = pal(0xFF2A5AA0, 0xFF3474C4, 0xFF3A7ECE, 0xFF3F86D6, 0xFF6AA8E8, 0xFFC0E4FA)
    private val oceanPal = pal(0xFF1E4C8E, 0xFF2558A6, 0xFF2960B0, 0xFF2C68B8, 0xFF4E8CCE, 0xFFB0D8F6)
    private val caveTopPal = pal(0xFF1C1618, 0xFF2A2224, 0xFF3A302E, 0xFF443834, 0xFF574940, 0xFF6A5A4C)
    private val caveFacePal = pal(0xFF1E1819, 0xFF362A2A, 0xFF4C3E38, 0xFF5C4C44, 0xFF726050, 0xFF887460)
    private val cliffTopPal = pal(0xFF3C3442, 0xFF585060, 0xFF6C6464, 0xFF7C726A, 0xFF948878, 0xFFAEA08A)
    private val cliffFacePal = pal(0xFF2C2632, 0xFF4C4450, 0xFF605652, 0xFF70645C, 0xFF887A6A, 0xFFA0927E)
    private val FOAM = c(0xFFE8F6FC)
    private val SHADE = c(0xFF1E1636)

    private fun tex(rows: List<String>, p: Map<Char, Int>, flip: Boolean = false): IntArray {
        val base = p.getValue('.'); val o = IntArray(256) { base }
        for (y in 0 until 16) for (x in 0 until 16) {
            val ch = rows[y][if (flip) 15 - x else x]
            if (ch != '.') o[y * 16 + x] = p[ch] ?: error("terrain palette has no '$ch'")
        }
        return o
    }
    /** Water frame: upper waves drift right, lower waves drift left (seamless: the texture is periodic). */
    private fun waves(p: Map<Char, Int>, f: Int): IntArray {
        val t = tex(Sprites.WATER_TEX, p); val o = IntArray(256)
        for (y in 0 until 16) for (x in 0 until 16) {
            val sx = if (y < 8) (x - f + 16) % 16 else (x + f) % 16
            o[y * 16 + x] = t[y * 16 + sx]
        }
        return o
    }

    private val grassT = Array(4) { s -> Array(4) { v -> tex(Sprites.GRASS_TEX[v], grassPal[s]) } }
    private val dirtT = Array(4) { v -> tex(Sprites.SOIL_TEX[v], dirtPal) }
    private val caveT = Array(4) { v -> tex(Sprites.SOIL_TEX[(v + 2) and 3], cavePal, flip = true) }
    private val sandT = Array(4) { v -> tex(Sprites.SAND_TEX[v], sandPal) }
    private val tilledT = arrayOf(tex(Sprites.TILLED_TEX, tillDry), tex(Sprites.TILLED_TEX, tillWet))
    private val cobbleT = Array(2) { v -> tex(Sprites.COBBLE_TEX[v], cobblePal) }
    private val waterT = Array(3) { f -> waves(waterPal, f) }
    private val oceanT = Array(3) { f -> waves(oceanPal, f) }
    private val caveTopT = Array(2) { v -> tex(Sprites.ROCK_TOP_TEX, caveTopPal, flip = v == 1) }
    private val cliffTopT = Array(2) { v -> tex(Sprites.ROCK_TOP_TEX, cliffTopPal, flip = v == 1) }

    private fun pri(g: Int) = when (g) {
        Gr.WATER, Gr.OCEAN, Gr.BRIDGE -> 0; Gr.CAVE -> 1; Gr.TILLED -> 2; Gr.SAND -> 3
        Gr.DIRT -> 4; Gr.COBBLE -> 5; Gr.GRASS -> 6; else -> -1
    }
    private fun watery(g: Int) = g == Gr.WATER || g == Gr.OCEAN || g == Gr.BRIDGE
    private fun rocky(g: Int) = g == Gr.WALL || g == Gr.CLIFF
    private fun open(g: Int) = !rocky(g) && g != Gr.VOID
    private fun casts(g: Int) = rocky(g) || g == Gr.INWALL

    private fun baseTex(g: Int, v: Int, s: Int, f: Int, wet: Boolean): IntArray = when (g) {
        Gr.GRASS -> grassT[s][v]; Gr.DIRT -> dirtT[v]; Gr.SAND -> sandT[v]; Gr.CAVE -> caveT[v]
        Gr.TILLED -> tilledT[if (wet) 1 else 0]; Gr.COBBLE -> cobbleT[v and 1]
        Gr.OCEAN -> oceanT[f]; else -> waterT[f]
    }
    private fun palOf(g: Int, s: Int): Map<Char, Int> = when (g) {
        Gr.GRASS -> grassPal[s]; Gr.DIRT -> dirtPal; Gr.SAND -> sandPal; Gr.CAVE -> cavePal
        Gr.TILLED -> tillDry; Gr.COBBLE -> cobblePal; else -> waterPal
    }
    /** Colours of the exposed bank between a land material and the water below it (lit top, shaded bottom). */
    private fun bank(g: Int): IntArray = when (g) {
        Gr.SAND -> intArrayOf(c(0xFFC4A06A), c(0xFF967650))
        Gr.COBBLE -> intArrayOf(c(0xFF746C70), c(0xFF4E4852))
        Gr.CAVE -> intArrayOf(c(0xFF4C4042), c(0xFF30282C))
        else -> intArrayOf(c(0xFF8A5E3C), c(0xFF5A3C2E))
    }

    // Ragged lip depths (2..4 px). Every pattern starts and ends at the same depth so neighbours always join.
    private val JAG = arrayOf(
        intArrayOf(1, 1, 2, 2, 1, 0, 0, 1, 2, 2, 1, 1, 0, 0, 1, 1),
        intArrayOf(1, 2, 2, 1, 1, 1, 0, 0, 1, 1, 2, 2, 2, 1, 0, 1),
        intArrayOf(1, 0, 0, 1, 2, 2, 2, 1, 1, 0, 1, 2, 1, 1, 2, 1)
    )
    // Neighbour order: N, NE, E, SE, S, SW, W, NW
    private val DX = intArrayOf(0, 1, 1, 1, 0, -1, -1, -1)
    private val DY = intArrayOf(-1, -1, 0, 1, 1, 1, 0, -1)
    private fun dirOf(tx: Int, ty: Int): Int {
        for (i in 0 until 8) if (DX[i] == tx && DY[i] == ty) return i
        return 0
    }

    private val cache = HashMap<Long, Bitmap>()

    /** The finished ground bitmap for cell (x, y), or null for empty void. [wf] is the water frame (0..2). */
    fun tile(a: Area, x: Int, y: Int, season: Int, wf: Int): Bitmap? {
        val i = a.idx(x, y); val gr = a.ground[i]
        if (gr == Gr.VOID) return null
        if (a.indoor && (gr == Gr.INWALL || gr == Gr.WOOD)) return Interior.tile(a, x, y)
        val v = (x * 7 + y * 13) and 3
        val wet = gr == Gr.TILLED && a.watered[i]
        val f = if (watery(gr)) ((wf % 3) + 3) % 3 else 0
        val jag = ((x * 5 + y * 3) % 3 + 3) % 3
        var key = gr.toLong() or (v.toLong() shl 4) or ((season and 3).toLong() shl 6) or
            ((if (wet) 1L else 0L) shl 8) or (f.toLong() shl 9) or (jag.toLong() shl 11)
        val n = IntArray(8)
        for (k in 0 until 8) {
            val nx = x + DX[k]; val ny = y + DY[k]
            n[k] = if (a.inb(nx, ny)) a.ground[a.idx(nx, ny)] else gr
            key = key or ((n[k] and 15).toLong() shl (13 + k * 4))
        }
        return cache.getOrPut(key) {
            Bitmap.createBitmap(compose(gr, v, season and 3, f, wet, n, jag), 16, 16, Bitmap.Config.ARGB_8888)
        }
    }

    private fun pixels(b: Bitmap): IntArray { val o = IntArray(256); b.getPixels(o, 0, 16, 0, 0, 16, 16); return o }

    private fun compose(gr: Int, v: Int, s: Int, f: Int, wet: Boolean, n: IntArray, jag: Int): IntArray {
        val out = when {
            rocky(gr) -> rock(gr, v, n)
            gr == Gr.WOOD -> pixels(art.woodFloor)
            gr == Gr.INWALL -> pixels(art.inWall)
            else -> baseTex(gr, v, s, f, wet).copyOf()
        }
        val myP = pri(gr)
        if (myP >= 0) {
            val mats = n.filter { pri(it) > myP }.distinct().sortedBy { pri(it) }
            for (m in mats) overlay(out, gr, m, n, v, s, f, jag)
        }
        if (!rocky(gr) && gr != Gr.INWALL) castShadows(out, n)
        if (gr == Gr.BRIDGE) planks(out, n)
        return out
    }

    private fun mixInto(out: IntArray, i: Int, col: Int, t: Float) { out[i] = Px.mix(out[i], col, t) }

    /** Paints material [m] (from neighbouring cells) over this cell with a ragged lip, rim and shadows. */
    private fun overlay(out: IntArray, gr: Int, m: Int, n: IntArray, v: Int, s: Int, f: Int, jag: Int) {
        val mask = BooleanArray(256)
        fun put(x: Int, y: Int) { if (x in 0..15 && y in 0..15) mask[y * 16 + x] = true }
        val nN = n[0] == m; val nE = n[2] == m; val nS = n[4] == m; val nW = n[6] == m
        val jn = JAG[jag]; val je = JAG[(jag + 1) % 3]; val js = JAG[(jag + 2) % 3]; val jw = JAG[(jag + 1) % 3].reversedArray()
        for (i in 0 until 16) {
            if (nN) for (k in 0 until 2 + jn[i]) put(i, k)
            if (nS) for (k in 0 until 2 + js[i]) put(i, 15 - k)
            if (nW) for (k in 0 until 2 + jw[i]) put(k, i)
            if (nE) for (k in 0 until 2 + je[i]) put(15 - k, i)
        }
        // outer corners: a rounded nub where only the diagonal neighbour is [m]
        fun disc(cx: Float, cy: Float) {
            for (yy in 0..15) for (xx in 0..15) {
                val dx = xx + 0.5f - cx; val dy = yy + 0.5f - cy
                if (dx * dx + dy * dy < 10f) put(xx, yy)
            }
        }
        if (n[7] == m && !nN && !nW) disc(0f, 0f)
        if (n[1] == m && !nN && !nE) disc(16f, 0f)
        if (n[3] == m && !nS && !nE) disc(16f, 16f)
        if (n[5] == m && !nS && !nW) disc(0f, 16f)
        // inner corners: fill the sharp elbow into a curve
        fun elbow(fx: Boolean, fy: Boolean) {
            for (yy in 0..5) for (xx in 0..5) {
                val dx = xx + 0.5f - 6f; val dy = yy + 0.5f - 6f
                if (dx * dx + dy * dy > 9f) put(if (fx) 15 - xx else xx, if (fy) 15 - yy else yy)
            }
        }
        if (nN && nW) elbow(false, false)
        if (nN && nE) elbow(true, false)
        if (nS && nE) elbow(true, true)
        if (nS && nW) elbow(false, true)

        fun inside(x: Int, y: Int): Boolean {
            if (x in 0..15 && y in 0..15) return mask[y * 16 + x]
            val tx = if (x < 0) -1 else if (x > 15) 1 else 0
            val ty = if (y < 0) -1 else if (y > 15) 1 else 0
            if (n[dirOf(tx, ty)] == m) return true
            return mask[y.coerceIn(0, 15) * 16 + x.coerceIn(0, 15)]   // assume the lip carries on across the seam
        }
        fun near(x: Int, y: Int, r: Int): Boolean {
            for (dy in -r..r) for (dx in -r..r) if (Math.abs(dx) + Math.abs(dy) <= r && inside(x + dx, y + dy)) return true
            return false
        }

        val mt = baseTex(m, v, s, f, false); val mp = palOf(m, s)
        val rimDark = mp.getValue('o'); val rimMid = mp.getValue('d')
        val water = watery(gr); val bk = bank(m)
        for (y in 0..15) for (x in 0..15) {
            val i = y * 16 + x
            if (mask[i]) {
                val up = !inside(x, y - 1); val dn = !inside(x, y + 1)
                val lf = !inside(x - 1, y); val rt = !inside(x + 1, y)
                out[i] = when {
                    dn || rt -> rimDark          // shaded side of the lip
                    up || lf -> rimMid           // lit side, still outlined
                    else -> mt[i]
                }
            } else if (water) {
                when {
                    inside(x, y - 1) -> out[i] = bk[0]                        // bank face, lit top
                    inside(x, y - 2) -> out[i] = bk[1]                        // bank face, shaded
                    inside(x, y - 3) -> out[i] = if ((x + f) % 5 == 0) Px.lighten(out[i], 0.3f) else FOAM
                    inside(x, y - 4) -> mixInto(out, i, SHADE, 0.22f)          // the bank's shadow on the water
                    near(x, y, 1) -> out[i] = if ((x * 3 + y + f) % 7 == 0) Px.lighten(out[i], 0.35f) else FOAM
                    near(x, y, 2) -> out[i] = if ((x + y * 2 + f * 2) % 4 == 0) FOAM else Px.lighten(out[i], 0.28f)
                    near(x, y, 3) -> out[i] = Px.lighten(out[i], 0.14f)
                }
            } else {
                when {
                    inside(x, y - 1) -> mixInto(out, i, SHADE, 0.30f)   // cast shadow under the lip
                    inside(x, y - 2) -> mixInto(out, i, SHADE, 0.14f)
                    inside(x + 1, y) -> mixInto(out, i, SHADE, 0.12f)
                }
            }
        }
    }

    /** Soft shadows cast onto open ground by walls, cliffs and interior walls to the north / west / east. */
    private fun castShadows(out: IntArray, n: IntArray) {
        if (casts(n[0])) for (x in 0..15) {
            mixInto(out, x, SHADE, 0.42f); mixInto(out, 16 + x, SHADE, 0.26f); mixInto(out, 32 + x, SHADE, 0.12f)
        } else if (casts(n[7]) || casts(n[1])) {
            val x0 = if (casts(n[7])) 0 else 12
            for (x in x0 until x0 + 4) { mixInto(out, x, SHADE, 0.22f); mixInto(out, 16 + x, SHADE, 0.10f) }
        }
        if (casts(n[6])) for (y in 0..15) { mixInto(out, y * 16, SHADE, 0.28f); mixInto(out, y * 16 + 1, SHADE, 0.12f) }
        if (casts(n[2])) for (y in 0..15) mixInto(out, y * 16 + 15, SHADE, 0.16f)
    }

    /** Cave wall / cliff cell: rubbly rock top, lit rims, and a rock face wherever open ground lies to the south. */
    private fun rock(gr: Int, v: Int, n: IntArray): IntArray {
        val cave = gr == Gr.WALL
        val tp = if (cave) caveTopPal else cliffTopPal
        val fp = if (cave) caveFacePal else cliffFacePal
        val out = (if (cave) caveTopT else cliffTopT)[v and 1].copyOf()
        if (open(n[0])) for (x in 0..15) { out[x] = tp.getValue('h'); out[16 + x] = tp.getValue('l') }
        if (open(n[6])) for (y in 0..15) out[y * 16] = tp.getValue('l')
        if (open(n[2])) for (y in 0..15) out[y * 16 + 15] = tp.getValue('o')
        if (open(n[4])) {
            val face = Sprites.ROCK_FACE_TEX
            val y0 = 16 - face.size
            for (yy in face.indices) for (x in 0..15) {
                val ch = face[yy][if (v and 1 == 1) 15 - x else x]
                out[(y0 + yy) * 16 + x] = fp.getValue(ch)
            }
            for (x in 0..15) out[(y0 - 1) * 16 + x] = tp.getValue('o')     // dark crease above the ledge
            if (open(n[6])) for (yy in y0..15) out[yy * 16] = fp.getValue('l')
            if (open(n[2])) for (yy in y0..15) out[yy * 16 + 15] = fp.getValue('o')
        }
        return out
    }

    /** Wooden planks laid across water: lit plank tops, grain, dark gaps and side rails. */
    private fun planks(out: IntArray, n: IntArray) {
        val top = c(0xFFCC9E64); val body = c(0xFFAE7E4C); val grain = c(0xFF946840); val gap = c(0xFF5A3A2A)
        val rail = c(0xFF6A462E); val railHi = c(0xFF8E6440)
        for (y in 0..15) for (x in 0..15) {
            out[y * 16 + x] = when (y % 4) {
                0 -> top; 3 -> gap
                else -> if ((x * 5 + y * 3) % 11 == 0) grain else body
            }
        }
        if (n[6] != Gr.BRIDGE) for (y in 0..15) { out[y * 16] = rail; out[y * 16 + 1] = railHi }
        if (n[2] != Gr.BRIDGE) for (y in 0..15) { out[y * 16 + 15] = rail; out[y * 16 + 14] = grain }
    }
}
