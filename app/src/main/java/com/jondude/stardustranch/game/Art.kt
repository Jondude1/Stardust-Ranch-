package com.jondude.stardustranch.game

import android.graphics.Bitmap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Every sprite in the game, painted pixel by pixel in code.
 * Original art for Stardust Ranch.
 */
class Art {
    private val S = Px.Companion
    private fun c(v: Long) = v.toInt()

    // ---- palette ----
    private val OUT = c(0xFF2A2030)
    private val grassPal = arrayOf(
        intArrayOf(c(0xFF6CB84A), c(0xFF5AA23E), c(0xFF86CE5E)),
        intArrayOf(c(0xFF55A83B), c(0xFF448F34), c(0xFF6EBF4E)),
        intArrayOf(c(0xFFBFA444), c(0xFFA68836), c(0xFFD6BC5C)),
        intArrayOf(c(0xFFE8EFF4), c(0xFFD2DDE6), c(0xFFF8FBFD))
    )
    private val leafPal = arrayOf(c(0xFF4FA040), c(0xFF3C8C34), c(0xFFD9822A), c(0xFF8A6A4A))
    // Foliage / stone palettes for the hand-authored grids in Sprites.kt (5-tone ramps, cool shadows, warm lights).
    private val SHADOW = c(0x46101820)
    private val barkPal = mapOf('O' to c(0xFF2E1E26), 'D' to c(0xFF4A3034), 'M' to c(0xFF6A4634), 'B' to c(0xFF8A5E3E), 'L' to c(0xFFAE8052), 's' to SHADOW)
    private fun ramp(o: Long, d: Long, m: Long, b: Long, l: Long, h: Long) =
        mapOf('o' to c(o), 'd' to c(d), 'm' to c(m), 'b' to c(b), 'l' to c(l), 'h' to c(h))
    private val leafRamps = arrayOf(
        ramp(0xFF20402F, 0xFF2F6A3A, 0xFF46913E, 0xFF68B248, 0xFF94D05A, 0xFFCAEA7C), // spring: fresh
        ramp(0xFF1E3A34, 0xFF2D5A3C, 0xFF3F7A3E, 0xFF5A9A42, 0xFF82BA4C, 0xFFB8DA66), // summer: deep
        ramp(0xFF4A2228, 0xFF7A3226, 0xFFAA4E2A, 0xFFD27A30, 0xFFEAA442, 0xFFF8D46E), // fall: amber
        ramp(0xFF2A3440, 0xFF3E5058, 0xFF566E70, 0xFF708A84, 0xFF94AAA2, 0xFFC4D2CC)  // winter: frosted
    )
    private val SNOW = c(0xFFF2F6FA)

    private val stonePal = ramp(0xFF3A3444, 0xFF5A5466, 0xFF7C7884, 0xFF9C98A0, 0xFFC0BCBA, 0xFFE2DED4) +
        mapOf('O' to c(0xFF4A4452), 's' to SHADOW)


    // ---- ground tiles [season][variant] ----
    val grass = Array(4) { s -> Array(4) { v -> grassTile(s, v) } }
    val dirt = Array(4) { v -> speckle(c(0xFFB58C5C), c(0xFF9E7748), c(0xFFC9A270), v + 10, 22) }
    val tilledDry = tilled(false)
    val tilledWet = tilled(true)
    val water = Array(3) { f -> waterTile(c(0xFF3F86D6), f) }
    val ocean = Array(3) { f -> waterTile(c(0xFF2C68B8), f) }
    val sand = Array(4) { v -> speckle(c(0xFFEAD49C), c(0xFFD9BF84), c(0xFFF6E6BC), v + 20, 18) }
    val cave = Array(4) { v -> speckle(c(0xFF6E5E52), c(0xFF5E5046), c(0xFF7E6E60), v + 30, 26) }
    val caveWall = caveWallTile()
    val woodFloor = woodFloorTile()
    val inWall = inWallTile()
    val cobble = Array(4) { v -> cobbleTile(v) }
    val cliff = cliffTile()
    val bridge = Array(3) { f -> bridgeTile(f) }

    // ---- objects ----
    val tree = Array(4) { s -> treeSprite(s) }
    val pine = Array(4) { s -> pineSprite(s) }
    val rock = rockSprite(c(0xFF9C9690), 0, 0)
    val boulder = boulderSprite()
    val copperRock = rockSprite(c(0xFF8E8680), c(0xFFE0874A), 1)
    val ironRock = rockSprite(c(0xFF8A847E), c(0xFFD8DCE6), 2)
    val goldRock = rockSprite(c(0xFF8A847E), c(0xFFF5C842), 3)
    val gemRock = rockSprite(c(0xFF7E7290), c(0xFFC07AF0), 4)
    val coalRock = rockSprite(c(0xFF7E7872), c(0xFF26262C), 5)
    val weed = Array(4) { s -> weedSprite(s) }
    val stick = stickSprite()
    val bush = Array(4) { s -> bushSprite(s) }
    val tallGrass = Array(4) { s -> tallGrassSprite(s) }
    val fence = fenceSprite()
    val bed = bedSprite()
    val bin = binSprite()
    val box = boxSprite()
    val counter = counterSprite()
    val healer = healerSprite()
    val elevator = elevatorSprite()
    val ladder = ladderSprite(false)
    val ladderUp = ladderSprite(true)
    val flower = Array(3) { i -> flowerSprite(i) }
    val table = tableSprite()
    val anvil = anvilSprite()
    val fountain = Array(3) { f -> fountainSprite(f) }
    val shelf = shelfSprite()
    val chest = chestSprite()

    val buildings = HashMap<Int, Bitmap>()

    // ---- characters ----
    val player = personFrames(c(0xFFF2C8A0), c(0xFF6B3E22), c(0xFF3F7BC8), c(0xFF4A3A5A), 0)
    val npcFrames = HashMap<String, Array<Array<Bitmap>>>()

    private val monCache = HashMap<Int, Array<Bitmap>>()
    private val itemCache = HashMap<Int, Bitmap>()
    private val cropCache = HashMap<Int, Bitmap>()
    private val gloopCache = HashMap<Int, Array<Bitmap>>()

    init {
        val B = Buildings
        buildings[Bld.HOUSE] = B.hipped(6, 5, 3, Buildings.Style(c(0xFFEAD9B4), c(0xFFC0503E), c(0xFFF4EEE0),
            shutters = c(0xFF4E7A5A), chimney = 14, porch = true, flowerBoxes = true)).bmp()
        buildings[Bld.BARN] = B.barn(6, 5, 3).bmp()
        buildings[Bld.STORE] = B.hipped(6, 5, 3, Buildings.Style(c(0xFFD9B884), c(0xFF3E8E5A), c(0xFFF4EEE0),
            siding = Buildings.BOARDS, door = c(0xFF3E7A4E), awning = c(0xFF3E8E5A), sign = 1)).bmp()
        buildings[Bld.CENTER] = B.hipped(6, 5, 3, Buildings.Style(c(0xFFF2EEE6), c(0xFFE0607A), c(0xFFFAF6F0),
            glassDoor = true, sign = 2, flowerBoxes = true)).bmp()
        buildings[Bld.SMITHY] = B.hipped(5, 4, 2, Buildings.Style(c(0xFF9A948C), c(0xFF4E4A5C), c(0xFF8A6446),
            siding = Buildings.STONE, door = c(0xFF6A4634), chimney = 4, sign = 3, windows = 0, forgeGlow = true)).bmp()
        buildings[Bld.CAVE] = B.cave(5, 4).bmp()
        npcFrames["hollis"] = personFrames(c(0xFFE8B890), c(0xFFD8D8D8), c(0xFF6A3E8E), c(0xFF3A3A3A), 1)
        npcFrames["pia"] = personFrames(c(0xFFF4D2B4), c(0xFFE07A3A), c(0xFF3E9A6A), c(0xFF5A4A3A), 2)
        npcFrames["wren"] = personFrames(c(0xFFC89068), c(0xFF2A2A2A), c(0xFFE0B23A), c(0xFF3E5A3A), 3)
        npcFrames["gus"] = personFrames(c(0xFFE0A884), c(0xFF8A4A2A), c(0xFF6A6A72), c(0xFF3A3028), 4)
        npcFrames["mira"] = personFrames(c(0xFFAD7A58), c(0xFF4A2A6A), c(0xFF4AB4D0), c(0xFFE8D6B0), 5)
    }

    fun cottage(variant: Int): Bitmap = buildings.getOrPut(100 + variant) {
        val v = variant % 3
        val walls = arrayOf(c(0xFFC4D6E6), c(0xFFEACCC4), c(0xFFD6E4BC))
        val roofs = arrayOf(c(0xFF7A5A44), c(0xFF4A6A9A), c(0xFF9A6234))
        val shut = arrayOf(c(0xFF4A6A9A), c(0xFF8A4A5A), c(0xFF5A7A3E))
        Buildings.hipped(4, 4, 1 + variant % 2, Buildings.Style(walls[v], roofs[v], c(0xFFF4EEE0),
            shutters = shut[v], chimney = if (v == 1) null else 6, flowerBoxes = v != 2, porch = v == 2, windows = 1,
            door = arrayOf(c(0xFF8A5634), c(0xFF5A6A8A), c(0xFF7A4A2A))[v])).bmp()
    }

    // =====================================================================
    // Ground
    // =====================================================================

    private fun grassTile(s: Int, v: Int): Bitmap {
        val pal = grassPal[s]; val r = Random(s * 31 + v)
        val p = Px(16, 16); p.fill(pal[0])
        repeat(20) { p.set(r.nextInt(16), r.nextInt(16), pal[1]) }
        repeat(5) {
            val x = r.nextInt(1, 15); val y = r.nextInt(2, 15)
            p.set(x, y, pal[2]); p.set(x, y - 1, pal[2]); p.set(x + 1, y, pal[1])
        }
        if (s == 3) repeat(3) { p.set(r.nextInt(16), r.nextInt(16), c(0xFFC4D2DE)) }
        if (s == 0 && v == 3) { p.set(5, 6, c(0xFFF4F0A0)); p.set(11, 11, c(0xFFF0A0C0)) }
        return p.bmp()
    }

    private fun speckle(base: Int, dark: Int, light: Int, seed: Int, n: Int): Bitmap {
        val r = Random(seed); val p = Px(16, 16); p.fill(base)
        repeat(n) { p.set(r.nextInt(16), r.nextInt(16), if (r.nextBoolean()) dark else light) }
        return p.bmp()
    }

    private fun tilled(wet: Boolean): Bitmap {
        val base = if (wet) c(0xFF5C3E26) else c(0xFF8C6238)
        val dark = if (wet) c(0xFF40291A) else c(0xFF6E4A2A)
        val light = if (wet) c(0xFF6E4C30) else c(0xFFA0764A)
        val p = Px(16, 16); p.fill(base)
        for (y in 0 until 16 step 4) for (x in 0 until 16) { p.set(x, y + 3, dark); if ((x + y) % 5 == 0) p.set(x, y + 1, light) }
        for (x in 0 until 16) { p.set(x, 0, dark) }
        for (y in 0 until 16) { p.set(0, y, dark) }
        return p.bmp()
    }

    private fun waterTile(base: Int, f: Int): Bitmap {
        val p = Px(16, 16); p.fill(base)
        val hi = S.lighten(base, 0.35f); val dk = S.shade(base, 0.85f)
        val r = Random(7)
        repeat(10) { p.set(r.nextInt(16), r.nextInt(16), dk) }
        for (i in 0 until 3) {
            val y = (i * 5 + f * 2) % 16; val x = (i * 6 + f * 3) % 12
            p.set(x, y, hi); p.set(x + 1, y, hi); p.set(x + 2, y, hi); p.set(x + 3, y + 1, hi)
        }
        return p.bmp()
    }

    private fun caveWallTile(): Bitmap {
        val p = Px(16, 16); p.fill(c(0xFF3E342E)); val r = Random(41)
        repeat(8) {
            val x = r.nextInt(16); val y = r.nextInt(16)
            p.ell(x.toFloat(), y.toFloat(), 3f, 2f, c(0xFF4C403A)); p.set(x - 1, y - 1, c(0xFF5E5048))
        }
        repeat(14) { p.set(r.nextInt(16), r.nextInt(16), c(0xFF2A221E)) }
        return p.bmp()
    }

    private fun woodFloorTile(): Bitmap {
        val p = Px(16, 16); p.fill(c(0xFFB4834E))
        for (y in 0 until 16 step 4) for (x in 0 until 16) p.set(x, y, c(0xFF8E6238))
        p.set(5, 1, c(0xFF8E6238)); p.set(5, 2, c(0xFF8E6238)); p.set(12, 5, c(0xFF8E6238)); p.set(12, 6, c(0xFF8E6238))
        p.set(3, 9, c(0xFF8E6238)); p.set(3, 10, c(0xFF8E6238)); p.set(10, 13, c(0xFF8E6238)); p.set(10, 14, c(0xFF8E6238))
        for (x in 0 until 16 step 3) p.set(x, 2, c(0xFFC4955E))
        return p.bmp()
    }

    private fun inWallTile(): Bitmap {
        val p = Px(16, 16); p.fill(c(0xFFD9C4A0))
        for (x in 0 until 16 step 4) for (y in 0 until 13) p.set(x, y, c(0xFFCBB48E))
        p.rect(0, 13, 16, 3, c(0xFF8E6238)); p.rect(0, 12, 16, 1, c(0xFF6E4A2A))
        return p.bmp()
    }

    private fun cobbleTile(v: Int): Bitmap {
        val p = Px(16, 16); p.fill(c(0xFF847C72)); val r = Random(50 + v)
        val stones = listOf(intArrayOf(1, 1, 6, 5), intArrayOf(8, 0, 7, 6), intArrayOf(0, 7, 5, 4), intArrayOf(6, 7, 6, 4), intArrayOf(12, 7, 4, 4), intArrayOf(2, 12, 7, 4), intArrayOf(10, 12, 6, 4))
        for (s in stones) {
            val col = if (r.nextBoolean()) c(0xFFA49C90) else c(0xFF9A9286)
            p.rect(s[0], s[1], s[2] - 1, s[3] - 1, col)
            p.rect(s[0], s[1], s[2] - 1, 1, S.lighten(col, 0.25f))
        }
        return p.bmp()
    }

    private fun cliffTile(): Bitmap {
        val p = Px(16, 16); p.fill(c(0xFF8A7A6A))
        p.rect(0, 0, 16, 3, c(0xFF6CAA4A)); p.rect(0, 3, 16, 1, c(0xFF4E8838))
        for (x in listOf(3, 9, 14)) for (y in 5 until 16) if ((x + y) % 4 != 0) p.set(x, y, c(0xFF6A5C50))
        for (x in listOf(4, 10)) for (y in 6 until 15 step 2) p.set(x, y, c(0xFFA0907E))
        return p.bmp()
    }

    private fun bridgeTile(f: Int): Bitmap {
        val p = Px(16, 16)
        val w = waterTile(c(0xFF3F86D6), f)
        val tmp = IntArray(256); w.getPixels(tmp, 0, 16, 0, 0, 16, 16); System.arraycopy(tmp, 0, p.p, 0, 256)
        for (y in 0 until 16) {
            if (y % 4 == 3) continue
            for (x in 1 until 15) p.set(x, y, if (y % 4 == 0) c(0xFFC4955E) else c(0xFFA87848))
        }
        for (y in 0 until 16) { p.set(0, y, c(0xFF6E4A2A)); p.set(15, y, c(0xFF6E4A2A)) }
        return p.bmp()
    }

    // =====================================================================
    // Objects
    // =====================================================================

    private fun treeSprite(s: Int): Bitmap {
        if (s == 3) return Px.grid(Sprites.TREE_BARE, barkPal + ('w' to SNOW)).bmp()
        val leaf = leafRamps[s]
        val accents = when (s) {
            0 -> mapOf('f' to c(0xFFF6C2D2), 'F' to c(0xFFD884A6))   // blossom
            1 -> mapOf('f' to c(0xFFE0484A), 'F' to c(0xFF9E2A3C))   // fruit
            else -> mapOf('f' to leaf['l']!!, 'F' to leaf['b']!!)
        }
        return Px.grid(Sprites.TREE, barkPal + leaf + accents).bmp()
    }

    private fun pineSprite(s: Int): Bitmap {
        val base = ramp(0xFF142E2C, 0xFF1F4A3A, 0xFF2C6440, 0xFF3E7E46, if (s == 2) 0xFF6A9A4E else 0xFF5E9E52, 0xFF9CC870)
        val needle = if (s == 3) base.mapValues { (_, v) -> S.mix(v, c(0xFF7A8EA0), 0.22f) } else base
        // 'S' marks the upward-facing rims of each tier: a snow cap in winter, empty otherwise
        return Px.grid(Sprites.PINE, barkPal + needle + mapOf('S' to (if (s == 3) SNOW else 0), 'W' to (if (s == 3) SNOW else needle['d']!!))).bmp()
    }

    private fun rockSprite(base: Int, ore: Int, kind: Int): Bitmap {
        // tint the stone ramp toward the requested base colour (gem rocks are violet, coal rocks darker)
        val stone = stonePal.mapValues { (k, v) -> if (k == 's') v else S.mix(v, base, if (k in "oO") 0.1f else 0.22f) }
        val fleck = if (ore == 0) mapOf('x' to stone['b']!!, 'X' to stone['m']!!, 'y' to stone['l']!!)
        else mapOf('x' to ore, 'X' to S.shade(ore, 0.62f), 'y' to S.lighten(ore, 0.5f))
        return Px.grid(Sprites.ROCK, stone + fleck).bmp()
    }

    private fun boulderSprite(): Bitmap = Px.grid(Sprites.BOULDER, stonePal.mapValues { (k, v) -> if (k == 's') v else S.shade(v, 0.92f) }).bmp()

    private fun weedSprite(s: Int): Bitmap = Px.grid(Sprites.WEED, leafRamps[s] + ('s' to SHADOW)).bmp()

    private fun stickSprite(): Bitmap =
        Px.grid(Sprites.STICK, ramp(0xFF2E1E26, 0xFF4A3034, 0xFF6A4634, 0xFF8A5E3E, 0xFFAE8052, 0xFFD0A070) + ('s' to SHADOW)).bmp()

    private fun bushSprite(s: Int): Bitmap {
        val leaf = leafRamps[s]
        val extra = mapOf(
            'S' to if (s == 3) SNOW else leaf['l']!!,
            'f' to when (s) { 1 -> c(0xFF4A5AD8); 0 -> c(0xFFF6C2D2); else -> leaf['b']!! },
            's' to SHADOW
        )
        return Px.grid(Sprites.BUSH, leaf + extra).bmp()
    }

    private fun tallGrassSprite(s: Int): Bitmap {
        val p = Px(16, 16); val pal = grassPal[s]
        val col = if (s == 3) c(0xFF8EA2B0) else S.shade(pal[0], 0.62f); val lt = if (s == 3) c(0xFFD6E2EA) else S.lighten(pal[2], 0.15f)
        for (x in 0 until 16 step 2) {
            val h = 8 + (x * 7) % 6
            p.line(x, 15, x + 1, 15 - h, col); p.line(x + 1, 15, x + 1, 15 - h + 2, S.shade(col, 0.85f)); p.set(x + 1, 15 - h, lt)
        }
        return p.bmp()
    }

    private fun fenceSprite(): Bitmap {
        val p = Px(16, 16); val w = c(0xFFA8784A); val d = c(0xFF6E4A2A)
        p.rect(0, 6, 16, 2, w); p.rect(0, 11, 16, 2, w); p.rect(0, 8, 16, 1, d); p.rect(0, 13, 16, 1, d)
        p.rect(6, 3, 4, 13, w); p.rect(9, 3, 1, 13, d); p.rect(6, 3, 4, 1, S.lighten(w, 0.3f))
        return p.bmp()
    }

    private fun bedSprite(): Bitmap {
        val p = Px(16, 32)
        p.rect(1, 2, 14, 29, c(0xFF8E5A30)); p.rect(1, 2, 14, 3, c(0xFF6E4422))
        p.rect(3, 5, 10, 6, c(0xFFF4F4F4)); p.rect(2, 11, 12, 18, c(0xFF3F7BC8))
        for (y in 13 until 29 step 3) p.rect(2, y, 12, 1, c(0xFF5A94DC))
        p.outline(OUT); return p.bmp()
    }

    private fun binSprite(): Bitmap {
        val p = Px(16, 16)
        p.rect(1, 5, 14, 10, c(0xFF8E5A30)); p.rect(1, 3, 14, 3, c(0xFFB0783E))
        for (x in 3 until 15 step 4) p.rect(x, 6, 1, 9, c(0xFF6E4422))
        p.rect(6, 4, 4, 1, c(0xFFF2D24A))
        p.outline(OUT); return p.bmp()
    }

    private fun boxSprite(): Bitmap {
        val p = Px(16, 16)
        p.rect(1, 6, 14, 9, c(0xFF5A8E4A)); p.rect(1, 3, 14, 4, c(0xFF6EAA5A))
        p.rect(1, 6, 14, 1, c(0xFF3E6A32)); p.rect(7, 5, 2, 4, c(0xFFF2D24A))
        p.set(8, 10, c(0xFFFFE07A)); p.set(7, 11, c(0xFFFFE07A)); p.set(9, 11, c(0xFFFFE07A)); p.set(8, 12, c(0xFFFFE07A))
        p.outline(OUT); return p.bmp()
    }

    private fun counterSprite(): Bitmap {
        val p = Px(16, 16)
        p.rect(0, 3, 16, 4, c(0xFFC4955E)); p.rect(0, 7, 16, 9, c(0xFF8E5A30))
        p.rect(0, 3, 16, 1, c(0xFFDCAE72)); for (x in 0 until 16 step 4) p.rect(x, 8, 1, 8, c(0xFF6E4422))
        return p.bmp()
    }

    private fun healerSprite(): Bitmap {
        val p = Px(16, 32)
        p.rect(3, 18, 10, 13, c(0xFFC8C4D0)); p.rect(2, 16, 12, 3, c(0xFFE4E0EA)); p.rect(4, 29, 8, 2, c(0xFF9A96A4))
        p.ell(8f, 10f, 6f, 6f, c(0xFFF07AA8)); p.ellOn(6f, 8f, 2.5f, 2.5f, c(0xFFFFD0E4))
        p.rect(7, 22, 2, 5, c(0xFFF07AA8)); p.rect(5, 24, 6, 2, c(0xFFF07AA8))
        p.outline(OUT); return p.bmp()
    }

    private fun elevatorSprite(): Bitmap {
        val p = Px(16, 16)
        p.rect(1, 4, 14, 8, c(0xFF7A7A84)); p.rect(1, 4, 14, 2, c(0xFF9A9AA4)); p.rect(3, 6, 10, 2, c(0xFF4A4448))
        p.ell(4f, 13f, 2f, 2f, c(0xFF3A3A3E)); p.ell(12f, 13f, 2f, 2f, c(0xFF3A3A3E))
        p.set(5, 5, c(0xFFE0874A)); p.set(9, 5, c(0xFFD8DCE6)); p.set(11, 5, c(0xFFF5C842))
        p.outline(OUT); return p.bmp()
    }

    private fun ladderSprite(up: Boolean): Bitmap {
        val p = Px(16, 16)
        if (!up) { p.ell(8f, 9f, 7f, 6f, c(0xFF120E0C)); p.ellOn(8f, 6f, 6f, 2f, c(0xFF2A2220)) }
        val w = c(0xFFB0783E)
        p.rect(4, 1, 2, 14, w); p.rect(10, 1, 2, 14, w)
        for (y in 3 until 15 step 3) p.rect(4, y, 8, 1, c(0xFFD0985A))
        if (up) p.outline(OUT)
        return p.bmp()
    }

    private fun flowerSprite(i: Int): Bitmap {
        val p = Px(16, 16); val cols = intArrayOf(c(0xFFF2D24A), c(0xFFF07AA8), c(0xFF8E7CE0))
        for ((x, y) in listOf(4 to 9, 11 to 6, 8 to 12)) {
            p.line(x, y + 1, x, y + 4, c(0xFF3E8C3A))
            p.set(x - 1, y, cols[i]); p.set(x + 1, y, cols[i]); p.set(x, y - 1, cols[i]); p.set(x, y + 1, cols[i]); p.set(x, y, c(0xFFFFF4C0))
        }
        return p.bmp()
    }

    private fun tableSprite(): Bitmap {
        val p = Px(16, 16)
        p.rect(1, 4, 14, 6, c(0xFFB0783E)); p.rect(1, 4, 14, 1, c(0xFFD0985A)); p.rect(2, 10, 2, 5, c(0xFF6E4422)); p.rect(12, 10, 2, 5, c(0xFF6E4422))
        p.rect(6, 2, 4, 3, c(0xFFF4F4F4)); p.rect(7, 1, 2, 1, c(0xFFE0605A))
        p.outline(OUT); return p.bmp()
    }

    private fun anvilSprite(): Bitmap {
        val p = Px(16, 16)
        p.rect(2, 5, 12, 3, c(0xFF5A5A62)); p.rect(5, 8, 6, 3, c(0xFF4A4A52)); p.rect(3, 11, 10, 3, c(0xFF5A5A62))
        p.rect(0, 5, 3, 2, c(0xFF5A5A62)); p.rect(2, 5, 12, 1, c(0xFF8A8A94))
        p.outline(OUT); return p.bmp()
    }

    private fun fountainSprite(f: Int): Bitmap {
        val p = Px(32, 32)
        p.ell(16f, 20f, 15f, 10f, c(0xFFB4AEA6)); p.ell(16f, 20f, 12f, 7.5f, c(0xFF3F86D6))
        p.rect(14, 6, 4, 14, c(0xFFC8C2BA)); p.ell(16f, 7f, 5f, 2.5f, c(0xFFC8C2BA))
        val hi = c(0xFFBFE4FF)
        for (i in 0 until 4) { val y = 2 + (i * 3 + f) % 8; p.set(13 - i % 2, y, hi); p.set(19 + i % 2, y, hi) }
        p.set(10 + f * 3, 21, hi); p.set(20 - f * 2, 23, hi)
        p.outline(OUT); return p.bmp()
    }

    private fun shelfSprite(): Bitmap {
        val p = Px(16, 32)
        p.rect(1, 2, 14, 29, c(0xFF8E5A30)); p.rect(2, 3, 12, 27, c(0xFF5A3A22))
        val cols = intArrayOf(c(0xFFE0605A), c(0xFF5A94DC), c(0xFFF2D24A), c(0xFF6EAA5A), c(0xFFB46BE0))
        for (row in 0 until 3) {
            val y = 3 + row * 9
            p.rect(2, y + 7, 12, 2, c(0xFF8E5A30))
            for (k in 0 until 4) p.rect(3 + k * 3, y + 2 + (k % 2), 2, 5 - (k % 2), cols[(row + k) % 5])
        }
        p.outline(OUT); return p.bmp()
    }

    private fun chestSprite(): Bitmap {
        val p = Px(16, 16)
        p.rect(1, 6, 14, 9, c(0xFF8E4A2A)); p.rect(1, 2, 14, 5, c(0xFFAE5E34))
        p.rect(1, 6, 14, 1, c(0xFFF2C842)); p.rect(1, 2, 1, 13, c(0xFFF2C842)); p.rect(14, 2, 1, 13, c(0xFFF2C842))
        p.rect(7, 5, 2, 4, c(0xFFF2C842))
        p.outline(OUT); return p.bmp()
    }

    // =====================================================================
    // People: frames[dir][frame], dir 0 down,1 up,2 left,3 right
    // =====================================================================

    private fun personFrames(skin: Int, hair: Int, shirt: Int, pants: Int, style: Int): Array<Array<Bitmap>> {
        return Array(4) { dir ->
            Array(3) { f ->
                if (dir == 3) person(skin, hair, shirt, pants, style, 2, f).flipH().bmp()
                else person(skin, hair, shirt, pants, style, dir, f).bmp()
            }
        }
    }

    private fun person(skin: Int, hair: Int, shirt: Int, pants: Int, style: Int, dir: Int, f: Int): Px {
        val p = Px(16, 24)
        val bob = if (f == 0) 0 else 1
        val shoe = c(0xFF3A2A22)
        // legs
        val lA = if (f == 1) -1 else if (f == 2) 1 else 0
        if (dir == 2) {
            p.rect(6 + lA, 19, 3, 4, pants); p.rect(6 + lA, 22, 3, 2, shoe)
            p.rect(8 - lA, 19, 2, 4, S.shade(pants, 0.8f)); p.rect(8 - lA, 22, 2, 2, shoe)
        } else {
            p.rect(5, 19 + (if (f == 1) -1 else 0), 3, 4, pants); p.rect(9, 19 + (if (f == 2) -1 else 0), 3, 4, pants)
            p.rect(5, 22, 3, 2, shoe); p.rect(9, 22, 3, 2, shoe)
        }
        // body
        p.rect(4, 12 + bob, 9, 8, shirt)
        p.rect(4, 18 + bob, 9, 1, S.shade(shirt, 0.75f))
        if (style == 1) p.rect(8, 12 + bob, 1, 6, c(0xFFF2D24A))
        if (style == 3) p.rect(4, 15 + bob, 9, 1, c(0xFF6E4422))
        if (style == 4) { p.rect(5, 13 + bob, 7, 7, c(0xFF8E5A30)) }
        // arms
        val aS = if (f == 1) 1 else if (f == 2) -1 else 0
        if (dir == 2) {
            p.rect(7, 13 + bob + aS, 3, 5, S.shade(shirt, 0.85f)); p.rect(7, 18 + bob + aS, 3, 1, skin)
        } else {
            p.rect(2, 13 + bob + aS, 2, 5, shirt); p.rect(13, 13 + bob - aS, 2, 5, shirt)
            p.rect(2, 18 + bob + aS, 2, 1, skin); p.rect(13, 18 + bob - aS, 2, 1, skin)
        }
        // head
        val hy = 2 + bob
        p.rect(4, hy + 1, 9, 9, skin); p.rect(5, hy, 7, 1, skin)
        when (dir) {
            0 -> {
                p.rect(4, hy, 9, 3, hair); p.rect(4, hy, 1, 6, hair); p.rect(12, hy, 1, 6, hair)
                if (style == 2) { p.rect(3, hy + 2, 1, 8, hair); p.rect(13, hy + 2, 1, 8, hair) }
                if (style == 5) { p.rect(3, hy + 1, 11, 2, hair) }
                p.set(6, hy + 5, OUT); p.set(10, hy + 5, OUT); p.set(6, hy + 4, OUT); p.set(10, hy + 4, OUT)
                p.set(5, hy + 7, S.shade(skin, 0.85f)); p.set(11, hy + 7, S.shade(skin, 0.85f))
                p.rect(7, hy + 8, 3, 1, S.shade(skin, 0.7f))
                if (style == 1) { p.rect(5, hy + 8, 7, 2, hair) }
            }
            1 -> { p.rect(4, hy, 9, 10, hair); if (style == 2) p.rect(4, hy + 9, 9, 3, hair) }
            2 -> {
                p.rect(4, hy, 9, 3, hair); p.rect(9, hy, 4, 8, hair)
                if (style == 2) p.rect(10, hy, 4, 11, hair)
                p.set(5, hy + 4, OUT); p.set(5, hy + 5, OUT); p.set(3, hy + 6, skin)
                if (style == 1) p.rect(3, hy + 8, 5, 2, hair)
            }
        }
        if (style == 0) { p.rect(3, hy, 11, 2, c(0xFFE0B04A)); p.rect(5, hy - 2, 7, 3, c(0xFFE0B04A)); p.rect(5, hy, 7, 1, c(0xFFB43A2E)) }
        if (style == 3) { p.rect(3, hy, 11, 2, c(0xFF3E5A3A)); p.rect(5, hy - 1, 7, 2, c(0xFF3E5A3A)) }
        p.outline(OUT)
        return p
    }

    // =====================================================================
    // Creatures: procedural sprites from species traits. frames[0..1] facing right, [2..3] facing left
    // =====================================================================

    fun mon(id: Int): Array<Bitmap> = monCache.getOrPut(id) {
        Creatures.frames(Dex[id])?.let { f -> return@getOrPut Array(4) { f[it].bmp() } }
        val a = monPx(Dex[id], 0); val b = monPx(Dex[id], 1)
        arrayOf(a.bmp(), b.bmp(), a.flipH().bmp(), b.flipH().bmp())
    }

    private fun monPx(sp: Species, frame: Int): Px {
        val p = Px(32, 32)
        val s = sp.size
        val c1 = sp.c1; val c2 = sp.c2
        val dark = S.shade(c1, 0.72f); val light = S.lighten(c1, 0.3f)
        val f = sp.feats
        val bob = frame.toFloat()
        val gy = 30f // ground line
        var hx: Float; var hy: Float; var hr: Float
        var tailX: Float; var tailY: Float; var backX: Float; var backY: Float

        fun has(x: Int) = f and x != 0

        when (sp.body) {
            Body.QUAD -> {
                val bw = 9.5f * s; val bh = 5.5f * s
                val by = gy - 4f * s - bh + bob * 0.5f
                // legs
                for (lx in listOf(-0.6f, -0.25f, 0.3f, 0.65f)) {
                    val x = (16 + lx * bw).toInt(); val lh = (5 * s).toInt().coerceAtLeast(2)
                    p.rect(x - 1, (by + bh * 0.4f).toInt(), 3, lh + 2, if (lx < 0) dark else c1)
                }
                hr = 5.5f * s; hx = 16 + bw * 0.75f; hy = by - bh * 0.6f
                tailX = 16 - bw; tailY = by - bh * 0.3f; backX = 16f; backY = by - bh
                if (has(Feat.TAIL)) { for (i in 0..6) p.ell(tailX - i * 0.9f * s, tailY - i * 0.8f * s + (i * i) * 0.08f, 2f * s, 1.6f * s, c1) }
                p.ell(16f, by, bw, bh, c1)
                if (has(Feat.FLUFF)) for (i in 0 until 8) { val a = i * PI / 7; p.ell((16 + cos(a) * bw * 0.85f).toFloat(), (by - sin(a) * bh * 0.85f).toFloat(), 3f * s, 2.5f * s, light) }
                p.ell(hx, hy, hr, hr * 0.9f, c1)
                p.ell(hx + hr * 0.6f, hy + hr * 0.35f, hr * 0.55f, hr * 0.4f, S.lighten(c1, 0.15f))
                p.ellOn(16f, by + bh * 0.55f, bw * 0.75f, bh * 0.4f, if (has(Feat.FLUFF)) S.shade(light, 0.9f) else dark)
            }
            Body.BIRD -> {
                val br = 7.5f * s; val by = gy - 3f - br + bob
                p.line(14, (by + br).toInt(), 13, gy.toInt(), c(0xFFE0A03A)); p.line(18, (by + br).toInt(), 19, gy.toInt(), c(0xFFE0A03A))
                hr = 5f * s; hx = 16 + br * 0.5f; hy = by - br * 0.75f
                tailX = 16 - br; tailY = by; backX = 16f; backY = by - br
                p.tri(tailX + 2, tailY - 2, tailX - 5 * s, tailY - 5 * s, tailX - 4 * s, tailY + 3 * s, c2)
                p.ell(16f, by, br, br * 0.9f, c1)
                p.ell(hx, hy, hr, hr, c1)
                p.ellOn(17f, by + br * 0.35f, br * 0.6f, br * 0.45f, S.lighten(c1, 0.35f))
                p.tri(hx + hr - 1, hy, hx + hr + 4 * s, hy + 1.5f, hx + hr - 1, hy + 2.5f, c(0xFFF2B23A))
            }
            Body.SERPENT -> {
                val segs = 7
                hr = 5.5f * s; hx = 22f + 2 * s; hy = gy - 15f * s + bob
                tailX = 4f; tailY = gy - 3f; backX = 14f; backY = gy - 9f * s
                for (i in 0 until segs) {
                    val t = i / (segs - 1f)
                    val x = 5f + t * 17f
                    val y = gy - 3f - (sin(t * PI * 1.4).toFloat() * 6f * s) - t * 6f * s + bob * t
                    p.ell(x, y, (2f + t * 3f) * s, (2f + t * 2.5f) * s, if (i % 2 == 0) c1 else S.shade(c1, 0.9f))
                }
                p.ell(hx, hy, hr, hr * 0.85f, c1)
                p.ell(hx + hr * 0.5f, hy + hr * 0.3f, hr * 0.6f, hr * 0.45f, S.lighten(c1, 0.15f))
            }
            Body.TALL -> {
                val bw = 7f * s; val bh = 8.5f * s
                val by = gy - 5f * s - bh + bob
                p.rect((16 - bw * 0.6f).toInt(), (by + bh * 0.6f).toInt(), (3 * s).toInt() + 1, (6 * s).toInt() + 1, dark)
                p.rect((16 + bw * 0.25f).toInt(), (by + bh * 0.6f).toInt(), (3 * s).toInt() + 1, (6 * s).toInt() + 1, dark)
                hr = 5.5f * s; hx = 17f; hy = by - bh - hr * 0.4f
                tailX = 16 - bw; tailY = by + bh * 0.4f; backX = 14f; backY = by - bh * 0.4f
                if (has(Feat.TAIL)) for (i in 0..5) p.ell(tailX - i * 0.8f, tailY + i * 0.5f, 2.4f * s, 2f * s, c1)
                p.ell(16f, by, bw, bh, c1)
                p.ellOn(17.5f, by + bh * 0.2f, bw * 0.6f, bh * 0.6f, S.lighten(c1, 0.3f))
                // arms
                p.ell(16 + bw * 0.95f, by + 1f, 2.2f * s, 3.5f * s, dark); p.ell(16 - bw * 0.95f, by + 1f, 2.2f * s, 3.5f * s, dark)
                p.ell(hx, hy, hr, hr, c1)
            }
            else -> { // ROUND
                val br = 9f * s; val by = gy - br * 0.9f + bob * 0.6f
                p.ell(16 - br * 0.5f, gy - 1.5f, 2.5f * s, 1.5f * s, dark); p.ell(16 + br * 0.5f, gy - 1.5f, 2.5f * s, 1.5f * s, dark)
                hr = br; hx = 16f; hy = by
                tailX = 16 - br; tailY = by + 2; backX = 16f - br * 0.2f; backY = by - br
                p.ell(16f, by, br, br * 0.9f, c1)
                p.ellOn(18f, by + br * 0.4f, br * 0.6f, br * 0.4f, S.lighten(c1, 0.3f))
            }
        }

        // ---- features ----
        val top = hy - hr * 0.85f
        if (has(Feat.MANE)) for (i in 0 until 9) {
            val a = -PI * 0.15 + i * PI * 1.3 / 8
            p.ell((hx - cos(a) * hr * 0.9f).toFloat(), (hy - sin(a) * hr * 0.9f).toFloat(), 2.6f * s, 2.6f * s, c2)
        }
        if (has(Feat.SHELL)) { p.ell(backX - 1, backY + 3 * s, 7.5f * s, 5.5f * s, c2); p.ellOn(backX - 3, backY + 1, 3f * s, 2f * s, S.lighten(c2, 0.35f)); p.line((backX - 6 * s).toInt(), (backY + 3 * s).toInt(), (backX + 5 * s).toInt(), (backY + 3 * s).toInt(), S.shade(c2, 0.7f)) }
        if (has(Feat.SPIKES)) for (i in 0 until 4) { val x = backX - 6 * s + i * 3.5f * s; p.tri(x - 1.5f * s, backY + 2, x + 1.5f * s, backY + 2, x, backY - 3.5f * s, c2) }
        if (has(Feat.CRYSTAL)) for (i in 0 until 3) { val x = backX - 4 * s + i * 4 * s; val hgt = (5 + (i % 2) * 3) * s; p.tri(x - 1.8f * s, backY + 2, x + 1.8f * s, backY + 2, x, backY - hgt, if (i == 1) c(0xFFE8FBFF) else c2); p.set(x.toInt(), (backY - hgt * 0.5f).toInt(), c(0xFFFFFFFF)) }
        if (has(Feat.FIN)) p.tri(backX - 4 * s, backY + 2, backX + 2 * s, backY + 2, backX - 3 * s, backY - 6 * s, c2)
        if (has(Feat.WINGS)) { p.ell(backX - 1, backY + 5 * s, 6f * s, 3.5f * s, c2); p.ellOn(backX - 3, backY + 4 * s, 3f * s, 1.5f * s, S.lighten(c2, 0.3f)) }
        if (has(Feat.FLAME)) {
            val fx = if (sp.body == Body.ROUND) hx else tailX - 1; val fy = if (sp.body == Body.ROUND) top - 1 else tailY - 2
            p.ell(fx, fy - 2 * s + bob * 0.5f, 3.2f * s, 4.2f * s, c(0xFFF06A2A)); p.ell(fx, fy - 1 * s, 2f * s, 2.6f * s, c(0xFFFFD34A)); p.set(fx.toInt(), (fy - 6 * s).toInt(), c(0xFFF06A2A))
        }
        if (has(Feat.EARS)) {
            p.tri(hx - hr * 0.75f, top + 2, hx - hr * 0.1f, top + 1, hx - hr * 0.55f, top - 5 * s, c1)
            p.tri(hx + hr * 0.05f, top + 1, hx + hr * 0.75f, top + 2, hx + hr * 0.5f, top - 5 * s, c1)
            p.tri(hx - hr * 0.6f, top + 1, hx - hr * 0.25f, top + 1, hx - hr * 0.5f, top - 3 * s, c2)
            p.tri(hx + hr * 0.2f, top + 1, hx + hr * 0.6f, top + 1, hx + hr * 0.45f, top - 3 * s, c2)
        }
        if (has(Feat.HORN)) p.tri(hx + hr * 0.1f, top + 2, hx + hr * 0.5f, top + 2, hx + hr * 0.45f, top - 6 * s, c(0xFFF4ECD6))
        if (has(Feat.LEAF)) {
            p.line(hx.toInt(), top.toInt(), hx.toInt(), (top - 3 * s).toInt(), c(0xFF3E8C3A))
            p.ell(hx - 2.5f * s, top - 3.5f * s, 2.8f * s, 1.6f * s, c(0xFF5DBB4A)); p.ell(hx + 2.5f * s, top - 4f * s, 2.8f * s, 1.6f * s, c(0xFF4FA040))
            if (sp.c2 != c(0xFF3E8C3A)) p.ell(hx, top - 5 * s, 1.6f * s, 1.6f * s, c2)
        }
        if (has(Feat.ANTENNA)) {
            p.line((hx - 2).toInt(), top.toInt(), (hx - 4 * s).toInt(), (top - 5 * s).toInt(), OUT); p.line((hx + 2).toInt(), top.toInt(), (hx + 4 * s).toInt(), (top - 5 * s).toInt(), OUT)
            p.ell(hx - 4 * s, top - 5 * s, 1.5f, 1.5f, c2); p.ell(hx + 4 * s, top - 5 * s, 1.5f, 1.5f, c2)
        }
        if (has(Feat.STAR)) { val sx = hx; val sy = hy - hr * 0.35f; p.set(sx.toInt(), sy.toInt() - 1, c(0xFFFFF4A0)); p.set(sx.toInt() - 1, sy.toInt(), c(0xFFFFF4A0)); p.set(sx.toInt(), sy.toInt(), c(0xFFFFE07A)); p.set(sx.toInt() + 1, sy.toInt(), c(0xFFFFF4A0)); p.set(sx.toInt(), sy.toInt() + 1, c(0xFFFFF4A0)) }
        if (sp.body == Body.ROUND && !has(Feat.SHELL)) p.ellOn(16f, hy + hr * 0.45f, hr * 0.55f, hr * 0.35f, S.lighten(c2, 0.1f))

        // ---- face ----
        val eyeY = (hy - hr * 0.05f).toInt()
        val ex1 = (hx + hr * 0.05f).toInt(); val ex2 = (hx + hr * 0.55f).toInt()
        val eyeCol = if (sp.type == EType.SHADE) c(0xFFF5D547) else OUT
        for (ex in listOf(ex1, ex2)) {
            p.set(ex, eyeY, eyeCol); p.set(ex, eyeY + 1, eyeCol)
            if (s > 0.8f) p.set(ex + 1, eyeY + 1, eyeCol)
            p.set(ex, eyeY - 1, c(0xFFFFFFFF))
        }
        p.set((hx + hr * 0.3f).toInt(), eyeY + 3, S.shade(c1, 0.5f))
        // cheeks
        p.set(ex2 + 1, eyeY + 2, c(0xFFF09AA8))

        p.outline(OUT)
        return p
    }

    // =====================================================================
    // Crops: key = crop*10+stage. Tall crops are 16x32, others 16x16 (anchored to bottom).
    // =====================================================================

    fun crop(type: Int, stage: Int): Bitmap = cropCache.getOrPut(type * 10 + stage) { cropPx(Crops.list[type], stage).bmp() }

    private fun cropPx(d: CropDef, stage: Int): Px {
        val tall = d.tall && stage >= 2
        val p = Px(16, if (tall) 32 else 16)
        val by = p.h - 3
        val leaf = d.leaf; val leafD = S.shade(leaf, 0.75f)
        when (stage) {
            0 -> { p.set(6, by, c(0xFF4A3020)); p.set(9, by - 1, c(0xFF4A3020)); p.set(8, by + 1, c(0xFF4A3020)) }
            1 -> { p.line(8, by, 8, by - 3, leaf); p.set(7, by - 3, leaf); p.set(6, by - 4, leaf); p.set(9, by - 3, leaf); p.set(10, by - 4, leaf) }
            else -> {
                val hgt = if (tall) (if (stage == 2) 14 else 24) else (if (stage == 2) 6 else 9)
                p.line(8, by, 8, by - hgt, leafD)
                if (d.tall && d.name == "Tomato") { p.line(12, by, 12, by - hgt - 2, c(0xFFA8784A)) }
                for (i in 0 until hgt / 3) {
                    val y = by - 2 - i * 3; val side = if (i % 2 == 0) -1 else 1
                    p.ell(8f + side * 2.5f, y.toFloat(), 2.6f, 1.4f, if (i % 2 == 0) leaf else leafD)
                }
                if (!d.tall) { p.ell(8f, (by - hgt + 1).toFloat(), 4f, 2.5f, leaf); p.ellOn(7f, (by - hgt).toFloat(), 2f, 1f, S.lighten(leaf, 0.3f)) }
                if (d.name == "Corn") for (i in 0 until 3) p.line(8, by - hgt + 2, 4 + i * 4, by - hgt - 3, leaf)
                if (stage == 4) produceOn(p, d, by, hgt)
            }
        }
        p.outline(c(0x00000000))
        val q = Px(p.w, p.h); q.blit(p, 0, 0); q.outline(S.shade(leafD, 0.6f))
        return q
    }

    private fun produceOn(p: Px, d: CropDef, by: Int, hgt: Int) {
        val col = d.color; val hi = S.lighten(col, 0.4f)
        when (d.name) {
            "Turnip", "Snowroot" -> { p.ell(8f, (by - 1).toFloat(), 4f, 3.5f, col); p.ellOn(8f, (by - 3).toFloat(), 3f, 1.4f, if (d.name == "Turnip") c(0xFFD86AA8) else c(0xFF8FD8EE)) }
            "Potato" -> { p.ell(5f, by.toFloat(), 2.5f, 1.8f, col); p.ell(11f, by.toFloat(), 2.5f, 1.8f, col) }
            "Cauliflower" -> { p.ell(8f, (by - 4).toFloat(), 5.5f, 4.5f, col); for (i in 0 until 5) p.set(5 + i, by - 6 + i % 2, c(0xFFDCD8C0)) }
            "Melon" -> { p.ell(8f, (by - 3).toFloat(), 6f, 4.5f, c(0xFF7AB848)); for (x in 4..12 step 3) p.line(x, by - 7, x, by, c(0xFF4E8A30)) }
            "Pumpkin" -> { p.ell(8f, (by - 3).toFloat(), 6.5f, 4.5f, col); p.line(6, by - 7, 6, by, S.shade(col, 0.8f)); p.line(10, by - 7, 10, by, S.shade(col, 0.8f)); p.rect(7, by - 9, 2, 3, c(0xFF5E7A2A)) }
            "Eggplant" -> { p.ell(5f, (by - 4).toFloat(), 1.8f, 3.2f, col); p.ell(11f, (by - 5).toFloat(), 1.8f, 3.2f, col) }
            "Corn" -> { p.ell(10f, (by - hgt / 2).toFloat(), 1.8f, 4.5f, col); p.ell(6f, (by - hgt / 2 - 4).toFloat(), 1.6f, 4f, col) }
            "Starfruit" -> { val cx = 8f; val cy = (by - hgt).toFloat(); p.tri(cx, cy - 4, cx - 4, cy + 3, cx + 4, cy + 3, col); p.tri(cx, cy + 4, cx - 4, cy - 2, cx + 4, cy - 2, col) }
            else -> { // berries & fruit clusters
                val spots = listOf(-3 to -2, 3 to -4, 0 to -6, -2 to -8, 3 to -9, 1 to -1)
                for ((dx, dy) in spots) { val y = by + dy * hgt / 10; p.ell(8f + dx, y.toFloat(), 1.6f, 1.6f, col); p.set(8 + dx - 1, y - 1, hi) }
            }
        }
    }

    // =====================================================================
    // Item icons 16x16
    // =====================================================================

    fun item(id: Int): Bitmap = itemCache.getOrPut(id) { val d = Items[id]; (Icons.cluster(d) ?: Icons.icon(d) ?: itemPx(d)).bmp() }
    /** Red-and-white float drawn on the water while fishing. */
    val bobber: Bitmap by lazy { Px.grid(Icons.BOBBER, Icons.BOBBER_PAL).bmp() }

    private fun itemPx(d: ItemDef): Px {
        val p = Px(16, 16)
        val c1 = d.c1; val c2 = d.c2
        val handle = c(0xFF9B6B3F); val metal = c(0xFFB8C4CC)
        when (d.kind) {
            Kind.TOOL -> when (d.id) {
                Tool.HOE -> { p.line(3, 14, 11, 3, handle); p.line(4, 14, 12, 3, handle); p.rect(10, 2, 5, 3, metal); p.rect(13, 2, 2, 5, metal) }
                Tool.CAN -> { p.ell(7f, 10f, 5f, 4f, c1); p.rect(2, 6, 10, 2, S.lighten(c1, 0.2f)); p.line(11, 9, 15, 5, c1); p.line(11, 10, 15, 6, c1); p.rect(1, 7, 2, 4, S.shade(c1, 0.7f)) }
                Tool.PICK -> { p.line(4, 14, 10, 4, handle); p.line(5, 14, 11, 4, handle); p.line(3, 5, 8, 2, metal); p.line(8, 2, 14, 7, metal); p.line(3, 6, 8, 3, metal) }
                Tool.AXE -> { p.line(4, 14, 10, 3, handle); p.line(5, 14, 11, 3, handle); p.ell(12f, 5f, 3.5f, 3.5f, metal); p.rect(9, 2, 3, 6, metal) }
                Tool.SCYTHE -> { p.line(4, 14, 8, 2, handle); p.line(8, 2, 14, 4, c2); p.line(8, 3, 14, 5, c2); p.line(14, 4, 14, 7, c2) }
                Tool.SWORD -> { p.line(3, 13, 12, 4, c2); p.line(4, 13, 13, 4, c2); p.line(12, 3, 13, 3, c(0xFFFFFFFF)); p.line(2, 10, 6, 14, c(0xFF8E6A3A)); p.line(2, 14, 4, 12, c1) }
            }
            Kind.SEED -> {
                p.rect(3, 2, 10, 13, c1); p.rect(3, 2, 10, 2, S.shade(c1, 0.8f))
                p.ell(8f, 9f, 3f, 3f, c2); p.ellOn(7f, 8f, 1f, 1f, S.lighten(c2, 0.5f))
            }
            Kind.CROP -> {
                val crop = Crops.list[d.id - 30]
                when (crop.name) {
                    "Corn" -> { p.ell(8f, 8f, 3f, 6f, c1); for (y in 3..13 step 2) p.line(6, y, 10, y, S.shade(c1, 0.85f)); p.tri(4f, 15f, 8f, 4f, 6f, 15f, c2) }
                    "Eggplant" -> { p.ell(8f, 9f, 4f, 6f, c1); p.rect(6, 2, 5, 3, c2) }
                    "Melon" -> { p.ell(8f, 9f, 6.5f, 5.5f, c(0xFF7AB848)); for (x in 4..12 step 3) p.line(x, 4, x, 14, c(0xFF4E8A30)) }
                    "Starfruit" -> { p.tri(8f, 1f, 2f, 12f, 14f, 12f, c1); p.tri(8f, 15f, 2f, 5f, 14f, 5f, c1) }
                    "Blueberry", "Cranberry" -> { for ((x, y) in listOf(5 to 7, 10 to 7, 8 to 11, 5 to 12, 11 to 12)) { p.ell(x.toFloat(), y.toFloat(), 2.4f, 2.4f, c1); p.set(x - 1, y - 1, S.lighten(c1, 0.5f)) } }
                    else -> {
                        p.ell(8f, 10f, 5.5f, 5f, c1)
                        p.ellOn(6f, 8f, 2f, 1.6f, S.lighten(c1, 0.45f))
                        p.ell(8f, 4f, 3f, 1.6f, c2); p.line(8, 2, 8, 5, c2)
                        if (crop.name == "Pumpkin") { p.line(6, 6, 6, 14, S.shade(c1, 0.8f)); p.line(10, 6, 10, 14, S.shade(c1, 0.8f)) }
                    }
                }
            }
            Kind.ORB -> {
                // glass lantern-orb: tinted glass sphere, glowing spark inside, metal clasp on top
                p.ell(8f, 9.5f, 6f, 6f, S.lighten(c1, 0.2f))
                p.ellOn(9f, 10.5f, 4.5f, 4f, c1)
                p.ellOn(10f, 12f, 3.5f, 2f, S.shade(c1, 0.8f))
                p.set(8, 9, c2); p.set(7, 10, c2); p.set(9, 10, c2); p.set(8, 11, c2); p.set(8, 10, c(0xFFFFFFFF))
                p.ellOn(5.5f, 6.5f, 1.8f, 1.4f, c(0xFFF4FCFF))
                p.rect(6, 1, 4, 3, c2); p.rect(5, 3, 6, 1, S.shade(c2, 0.7f)); p.set(7, 1, S.lighten(c2, 0.5f))
            }
            Kind.GEM -> { p.tri(8f, 2f, 3f, 7f, 13f, 7f, S.lighten(c1, 0.3f)); p.tri(3f, 7f, 13f, 7f, 8f, 14f, c1); p.line(3, 7, 13, 7, c2); p.set(6, 5, c(0xFFFFFFFF)) }
            Kind.FOOD -> when (d.id) {
                Items.TONIC -> { p.rect(5, 6, 6, 8, c1); p.rect(6, 3, 4, 3, c(0xFFE8F0F4)); p.rect(6, 2, 4, 1, c(0xFF8E5A30)); p.rect(6, 8, 2, 4, c2) }
                Items.FEAST -> { p.ell(8f, 11f, 7f, 3f, c(0xFFF4F4F4)); p.ell(8f, 8f, 5f, 3.5f, c1); p.ellOn(6f, 7f, 2f, 1f, S.lighten(c1, 0.4f)); p.set(9, 6, c2); p.set(5, 9, c(0xFF5DBB4A)) }
                else -> { p.rect(2, 5, 12, 7, c1); for (x in 3..12 step 3) p.rect(x, 6, 1, 5, c2); p.rect(2, 5, 12, 1, S.lighten(c1, 0.3f)) }
            }
            Kind.PLACE -> { p.rect(5, 9, 6, 5, c1); p.rect(7, 4, 2, 6, c1); p.ell(8f, 4f, 2.5f, 1.5f, c2); p.set(4, 3, c2); p.set(12, 3, c2); p.set(3, 5, c2); p.set(13, 5, c2) }
            else -> when (d.id) {
                Items.WOOD -> { p.rect(2, 5, 12, 6, c1); p.ell(13f, 8f, 2f, 3f, c(0xFFD0A070)); p.set(13, 8, c2); p.rect(2, 7, 10, 1, c2) }
                Items.FIBER -> { for (x in 4..11 step 2) p.line(x, 14, x + 1, 3, c1); p.line(3, 9, 12, 9, c2) }
                Items.GEL -> { p.ell(8f, 10f, 6f, 4.5f, c1); p.ellOn(6f, 8f, 2f, 1.5f, S.lighten(c1, 0.5f)) }
                Items.STONE, Items.COAL -> { p.ell(8f, 9f, 6f, 5f, c1); p.ellOn(6f, 7f, 2.5f, 1.5f, S.lighten(c1, 0.3f)); p.ellOn(10f, 11f, 3f, 2f, S.shade(c1, 0.8f)) }
                Items.COPPER, Items.IRON, Items.GOLD -> { p.ell(8f, 9f, 6f, 5f, c2); for ((x, y) in listOf(6 to 7, 9 to 10, 10 to 6, 5 to 11)) { p.rect(x, y, 2, 2, c1); p.set(x, y, S.lighten(c1, 0.5f)) } }
                58, 59 -> { p.ell(8f, 10f, 6f, 4.5f, c1); for (i in 0..4) p.line(8, 14, 3 + i * 2, 6, c2) }
                60 -> { p.ell(8f, 9f, 4.5f, 4.5f, c1); for (i in 0 until 8) { val a = i * PI / 4; p.line(8, 9, (8 + cos(a) * 7).toInt(), (9 + sin(a) * 7).toInt(), c2) } }
                61 -> { p.ell(8f, 7f, 6f, 3.5f, c1); p.rect(6, 9, 4, 5, c2); p.set(6, 6, S.lighten(c1, 0.4f)) }
                51, 53 -> { p.line(8, 15, 8, 8, c(0xFF3E8C3A)); for (i in 0 until 6) { val a = i * PI / 3; p.ell((8 + cos(a) * 3).toFloat(), (5 + sin(a) * 3).toFloat(), 2f, 2f, c1) }; p.set(8, 5, c(0xFFFFF4C0)) }
                else -> { p.ell(8f, 9f, 5f, 5f, c1); p.ellOn(6f, 7f, 2f, 1.5f, S.lighten(c1, 0.45f)); p.line(8, 4, 10, 1, c2); p.ell(11f, 3f, 2f, 1f, c2) }
            }
        }
        p.outline(OUT)
        return p
    }

    // =====================================================================
    // Gloops (mine enemies)
    // =====================================================================

    fun gloop(color: Int): Array<Bitmap> = gloopCache.getOrPut(color) {
        Array(2) { f ->
            val p = Px(16, 16)
            val ry = if (f == 0) 5.5f else 4.5f; val rx = if (f == 0) 6.5f else 7.5f
            p.ell(8f, 15f - ry, rx, ry, color)
            p.ellOn(6f, 15f - ry * 1.5f, 2.5f, 1.5f, S.lighten(color, 0.5f))
            p.ellOn(10f, 14f, 5f, 2f, S.shade(color, 0.75f))
            p.set(6, (15 - ry).toInt(), OUT); p.set(10, (15 - ry).toInt(), OUT); p.set(6, (14 - ry).toInt(), OUT); p.set(10, (14 - ry).toInt(), OUT)
            p.outline(OUT); p.bmp()
        }
    }
}
