package com.jondude.stardustranch.game

/**
 * Hand-authored in-field crop sprites (original designs). Every stage of every crop is assembled from
 * palette-indexed grids: a shared seeded mound and sprout, then a plant "family" (rosette roots, wrapped
 * head, berry bush, sprawling vine, staked climber, corn stalk, star sprout) in that crop's own leaf ramp,
 * and finally hand-drawn produce laid over the mature plant. Light comes from the top-left: leaves carry a
 * warm highlight on their upper-left lobe and a cool, violet-leaning shadow at the stem.
 *
 * Palette keys:  n g G L Y   leaf outline / shadow / mid / light / highlight (from the crop's leaf colour)
 *                o d m l h   produce outline / shadow / mid / light / highlight (from the crop's colour)
 *                x X         accent dark / light (shoulders, flowers, kernels, frost)
 *                q w W v V   wood + soil ramp (mounds, stakes, stems)   s = soft ground shadow
 * Short crops are 16x16 with the base on row 14; tall crops (stage 2+) are 16x32 with the base on row 30.
 */
object CropArt {
    private fun c(v: Long) = v.toInt()
    private fun rows(s: String) = s.trimIndent().lines().filter { it.isNotEmpty() }

    private const val ROOT = 0; private const val HEAD = 1; private const val BUSH = 2; private const val VINE = 3
    private const val TOMATO = 4; private const val CORN = 5; private const val STAR = 6

    private fun family(d: CropDef) = when (d.name) {
        "Turnip", "Potato", "Snowroot" -> ROOT
        "Cauliflower" -> HEAD
        "Melon", "Pumpkin" -> VINE
        "Tomato" -> TOMATO
        "Corn" -> CORN
        "Starfruit" -> STAR
        else -> BUSH
    }

    private fun pal(d: CropDef): Map<Char, Int> {
        val leaf = Icons.auto(d.leaf)
        val prodBase = when (d.name) { "Melon" -> c(0xFF6EAE44); "Starfruit" -> c(0xFFFFCE3A); else -> d.color }
        val prod = Icons.auto(prodBase)
        val acc = when (d.name) {
            "Turnip" -> c(0xFFB84A94) to c(0xFFE07ABE)
            "Snowroot" -> c(0xFF6A7ED0) to c(0xFFE8F8FF)
            "Potato" -> c(0xFFA884D8) to c(0xFFF4ECFA)
            "Corn" -> c(0xFFC89A2A) to c(0xFFF8E27A)
            "Starfruit" -> c(0xFFFFE89A) to c(0xFFFFFFF0)
            "Melon" -> c(0xFFE8C64A) to c(0xFFFFF2A0)
            "Pumpkin" -> c(0xFFE8C64A) to c(0xFFFFF2A0)
            "Tomato" -> c(0xFFE8C64A) to c(0xFFFFF2A0)
            else -> c(0xFFF2F2E6) to c(0xFFFFFFFF)
        }
        return mapOf(
            'n' to leaf['o']!!, 'g' to leaf['d']!!, 'G' to leaf['m']!!, 'L' to leaf['l']!!, 'Y' to leaf['h']!!,
            'o' to prod['o']!!, 'd' to prod['d']!!, 'm' to prod['m']!!, 'l' to prod['l']!!, 'h' to prod['h']!!,
            'x' to acc.first, 'X' to acc.second, 's' to c(0x40101820)
        ) + Icons.WOOD
    }

    private fun put(p: Px, g: List<String>, x: Int, bottom: Int, pal: Map<Char, Int>, flip: Boolean = false) {
        val src = Px.grid(g, pal)
        p.blit(if (flip) src.flipH() else src, x, bottom - g.size + 1)
    }

    fun crop(d: CropDef, stage: Int): Px {
        val f = family(d)
        val tall = (f == TOMATO || f == CORN) && stage >= 2
        val p = Px(16, if (tall) 32 else 16)
        val pl = pal(d)
        val base = p.h - 2
        when {
            stage == 0 -> put(p, SEEDED, 3, base, pl)
            stage == 1 -> put(p, if (f == STAR) STAR_SPROUT else SPROUT, 3, base, pl)
            else -> when (f) {
                ROOT -> root(p, d, stage, pl, base)
                HEAD -> {
                    put(p, if (stage == 2) HEAD2 else HEAD3, if (stage == 2) 2 else 0, base, pl)
                    if (stage == 4) put(p, CURD, 4, base - 3, pl)
                }
                BUSH -> bush(p, d, stage, pl, base)
                VINE -> vine(p, d, stage, pl, base)
                TOMATO -> tomato(p, stage, pl, base)
                CORN -> corn(p, stage, pl, base)
                else -> star(p, stage, pl, base)
            }
        }
        return p
    }

    // ------------------------------------------------------------------ families

    private fun root(p: Px, d: CropDef, stage: Int, pl: Map<Char, Int>, base: Int) {
        if (stage == 2) { put(p, ROOT2, 2, base, pl); return }
        if (stage == 3) { put(p, ROOT3, 1, base, pl); return }
        when (d.name) {
            "Potato" -> {
                put(p, ROOT3, 1, base - 1, pl)
                put(p, POTATO_FLOWER, 3, base - 7, pl); put(p, POTATO_FLOWER, 9, base - 8, pl)
                put(p, SPUD, 0, base + 1, pl); put(p, SPUD, 10, base + 1, pl, flip = true)
            }
            "Snowroot" -> {
                put(p, ROOT3, 1, base - 3, pl)
                put(p, BULB, 4, base + 1, pl)
                p.set(3, base - 9, pl['X']!!); p.set(12, base - 6, pl['X']!!); p.set(8, base - 12, pl['X']!!)
            }
            else -> { put(p, ROOT3, 1, base - 3, pl); put(p, BULB, 4, base + 1, pl) }
        }
    }

    private fun bush(p: Px, d: CropDef, stage: Int, pl: Map<Char, Int>, base: Int) {
        if (stage == 2) { put(p, BUSH2, 3, base, pl); return }
        put(p, BUSH3, 1, base, pl)
        if (stage < 4) return
        when (d.name) {
            "Strawberry" -> { put(p, STRAWBERRY, 1, base - 1, pl); put(p, STRAWBERRY, 10, base, pl); put(p, STRAWBERRY, 6, base - 5, pl) }
            "Blueberry" -> { put(p, BLUEBERRIES, 2, base - 2, pl); put(p, BLUEBERRIES, 9, base - 1, pl); put(p, BLUEBERRIES, 6, base - 6, pl) }
            "Eggplant" -> { put(p, EGGPLANT, 2, base, pl); put(p, EGGPLANT, 10, base - 1, pl) }
            else -> { // cranberries and any other berry
                for ((x, y) in listOf(2 to 3, 6 to 1, 10 to 3, 4 to 6, 9 to 7, 12 to 1)) put(p, CRANBERRY, x, base - y, pl)
            }
        }
    }

    private fun vine(p: Px, d: CropDef, stage: Int, pl: Map<Char, Int>, base: Int) {
        if (stage == 2) { put(p, VINE2, 2, base, pl); return }
        put(p, VINE3, 0, base - (if (stage == 4) 3 else 0), pl)
        if (stage == 4) {
            if (d.name == "Pumpkin") put(p, PUMPKIN, 2, base + 1, pl) else put(p, MELON, 2, base + 1, pl)
        } else put(p, FLOWER_BUD, 9, base - 4, pl)
    }

    private fun tomato(p: Px, stage: Int, pl: Map<Char, Int>, base: Int) {
        val top = if (stage == 2) base - 12 else base - 23
        // stake: lit left edge, shaded right, pointed top, sunk into the soil
        for (y in top - 1..base) { p.set(11, y, pl['v']!!); p.set(12, y, pl['w']!!) }
        p.set(11, top - 2, pl['W']!!); p.set(12, top - 2, pl['q']!!)
        p.set(11, base, pl['q']!!); p.set(12, base, pl['q']!!)
        stem(p, 7, base, top + 2, pl)
        var y = base - 3; var right = false
        while (y > top + 2) {
            if (right) put(p, SPRIG, 8, y, pl) else put(p, SPRIG, 2, y, pl, flip = true)
            right = !right; y -= 3
        }
        put(p, TUFT, 5, top + 3, pl)
        // twine ties from stem to stake
        for (ty in listOf(base - 6, base - 15)) if (ty > top + 3) { p.set(9, ty, pl['V']!!); p.set(10, ty, pl['v']!!) }
        if (stage == 4) {
            put(p, TOMATO_F, 2, base - 4, pl); put(p, TOMATO_F, 8, base - 9, pl); put(p, TOMATO_F, 2, base - 13, pl)
            put(p, TOMATO_F, 9, base - 17, pl)
        } else if (stage == 3) { put(p, FLOWER_BUD, 2, base - 9, pl); put(p, FLOWER_BUD, 9, base - 15, pl) }
    }

    private fun corn(p: Px, stage: Int, pl: Map<Char, Int>, base: Int) {
        val top = if (stage == 2) base - 13 else base - 25
        for (y in top..base) { p.set(6, y, pl['n']!!); p.set(7, y, pl['L']!!); p.set(8, y, pl['g']!!); p.set(9, y, pl['n']!!) }
        for (y in top + 2..base step 4) { p.set(7, y, pl['Y']!!); p.set(8, y, pl['G']!!) } // nodes
        p.set(5, base, pl['n']!!); p.set(10, base, pl['n']!!)
        var y = base - 2; var right = true
        while (y > top + 3) {
            if (right) put(p, BLADE, 9, y, pl) else put(p, BLADE, -2, y, pl, flip = true)
            right = !right; y -= if (stage == 2) 4 else 5
        }
        if (stage == 2) put(p, TUFT, 5, top + 1, pl)
        else put(p, TASSEL, 4, top + 2, pl)
        if (stage == 4) { put(p, COB, 9, base - 7, pl); put(p, COB, 2, base - 13, pl, flip = true) }
    }

    private fun star(p: Px, stage: Int, pl: Map<Char, Int>, base: Int) {
        if (stage == 2) { put(p, STAR2, 3, base, pl); return }
        put(p, STAR3, 2, base, pl)
        if (stage == 4) {
            put(p, STARFRUIT, 3, base - 7, pl)
            p.set(2, base - 12, pl['X']!!); p.set(13, base - 13, pl['X']!!); p.set(13, base - 3, pl['h']!!)
        } else put(p, STAR_BUD, 6, base - 9, pl)
    }

    private fun stem(p: Px, x: Int, base: Int, top: Int, pl: Map<Char, Int>) {
        for (y in top..base) { val wob = if ((y / 4) % 2 == 0) 0 else 1; p.set(x + wob, y, pl['g']!!); p.set(x + wob - 1, y, pl['G']!!) }
    }

    // ------------------------------------------------------------------ shared stages

    private val SEEDED = rows("""
        ...qqqq...
        .qqwWWwqq.
        qwWvVWwWwq
        sqqwwwwqqs
    """)

    private val SPROUT = rows("""
        .nn...nn.
        nYLn.nLYn
        nGLGnGLGn
        .ngGnGgn.
        ..nngnn..
        ....g....
        ..qwWwq..
    """)

    private val STAR_SPROUT = rows("""
        ....X....
        ...nxn...
        .nnLgnn..
        nYLGnLYn.
        .nGgnGgn.
        ..nngnn..
        ..qwgwq..
    """)

    // ------------------------------------------------------------------ root rosette (turnip, potato, snowroot)

    private val ROOT2 = rows("""
        ...nn.nn...
        ..nYLnLYn..
        .nLGgnGLGn.
        nLGgnLnGgGn
        nGgnLGGngGn
        .nngGLGgnn.
        ...ngGGgn..
        ...snggns..
    """)

    private val ROOT3 = rows("""
        ....nn..nn....
        ...nYLnnLYn...
        ..nLGgnnGLGn..
        .nnLGgnnGgGnn.
        nYLnGgnnGgnLYn
        nGLGnGgGgnGLGn
        .nGgGnGLGnGgn.
        ..nngGnLnGgnn.
        ....nngGgnn...
        .....ngGgn....
        ....snnnnns...
    """)

    private val BULB = rows("""
        ..oxxo..
        .oxXXxo.
        oxXxxxxo
        ohlhmmdo
        olmmmmdo
        .odmmdo.
        ..oddo..
    """)

    private val SPUD = rows("""
        .ooo.
        ohlmo
        omdmo
        .ooo.
    """)

    private val POTATO_FLOWER = rows("""
        .X.
        XxX
        .X.
    """)

    // ------------------------------------------------------------------ head (cauliflower)

    private val HEAD2 = rows("""
        ..nn....nn..
        .nLYn..nYLn.
        nLGGgnnGGLGn
        nGGgnLLngGgn
        .nGgnGGngGn.
        ..nngGGgnn..
        ...snnnns...
    """)

    private val HEAD3 = rows("""
        .....nn..nn.....
        ....nLYnnYLn....
        .nn.nGLgnLGn.nn.
        nYLnnGGgnGgnnLYn
        nGLGnnGgGgnnGLGn
        nGGLGnnGGnnGLGgn
        .nGGgGnGLnGgGgn.
        ..nngGgnGnGgnn..
        ....nngGGgnn....
        ....snnnnnns....
    """)

    private val CURD = rows("""
        .oo.oo..
        ohhohlo.
        ohllhlmo
        olhlmlmo
        .olmmdo.
        ..oddo..
    """)

    // ------------------------------------------------------------------ bush (berries, eggplant)

    private val BUSH2 = rows("""
        ...nn.....
        ..nLYn.nn.
        .nLGgnnLYn
        nLGgnnGLGn
        nGgnLGngGn
        .nngGGgnn.
        ..snggns..
    """)

    private val BUSH3 = rows("""
        ....nnn.......
        ...nLYLnnnn...
        ..nLGGnnLYLn..
        .nnGgnLLGGgnn.
        nLYnnLGGngnLYn
        nGLGnGgGnnLGGn
        nGGgLnngLGnGgn
        .nGgGGnLGGgGn.
        ..nngGgnGgnn..
        ....nnggGnn...
        ...ssnnnnss...
    """)

    private val STRAWBERRY = rows("""
        .LG.
        ohmo
        ohdo
        .md.
        ..o.
    """)

    private val BLUEBERRIES = rows("""
        .oo...
        olho..
        omdooo
        .oolho
        ..omdo
        ...oo.
    """)

    private val CRANBERRY = rows("""
        hm
        md
    """)

    private val EGGPLANT = rows("""
        .nGn.
        oGLGo
        ohlmo
        ohmmo
        olmdo
        omddo
        .odo.
    """)

    // ------------------------------------------------------------------ vine (melon, pumpkin)

    private val VINE2 = rows("""
        .nn......nn.
        nLYn.nn.nYLn
        nGLGnLYnnGLn
        .nGgnGLGgnGn
        ..nngnGgGnn.
        ...snnnnns..
    """)

    private val VINE3 = rows("""
        ...nn.......nn..
        ..nLYn..nn.nYLn.
        .nLGLGnnLYnGLGgn
        nLGgGnnLGLGnGgn.
        nGgnnGnGGgGnnn..
        .nnLYnGgnGgn.nn.
        ..nGLGnnnnnn.nYn
        ..nGgn.......nn.
        ...nn...........
    """)

    private val FLOWER_BUD = rows("""
        .X.
        XxX
        .g.
    """)

    private val MELON = rows("""
        ...ooooo...
        .odhldmmdo.
        ohdhldlmddo
        oldlmdmmddo
        oldmmdmdddo
        omdmddmdddo
        .odddddddo.
        s.ooooooo.s
    """)

    private val PUMPKIN = rows("""
        .....qw.....
        ....qWq.....
        ..oooWoooo..
        .ohlmolhmdo.
        ohlhmolmmddo
        olhmmomlmddo
        omlmdomddddo
        .odmdoddddo.
        s.oooooooo.s
    """)

    // ------------------------------------------------------------------ climbers (tomato, corn)

    private val SPRIG = rows("""
        ...nn.
        .nnLYn
        nLGGLn
        .nggn.
    """)

    private val TUFT = rows("""
        .n.n.
        nYnLn
        nGLGn
        .ngn.
    """)

    private val TOMATO_F = rows("""
        .GLG.
        ohGmo
        ohlmo
        omddo
        .ooo.
    """)

    private val BLADE = rows("""
        ......nn
        ....nnLn
        ..nnLLGn
        .nLGGgn.
        nLGgnn..
        ngn.....
    """)

    private val TASSEL = rows("""
        X..X..X
        .x.X.x.
        .xXXXx.
        ..xXx..
        ...x...
    """)

    private val COB = rows("""
        .ohh.
        ohlmo
        ohlmo
        olmdo
        ohmdo
        nLGgn
        nGLgn
        .nGn.
        ..n..
    """)

    // ------------------------------------------------------------------ starfruit

    private val STAR2 = rows("""
        ...X......
        ..nxn.....
        ..nLn.nn..
        .nLGnnYLn.
        nYLGgnGLn.
        nGgnngGn..
        .nn.ngn...
        ...snns...
    """)

    private val STAR3 = rows("""
        .....nn.....
        ....nLYn....
        .nn.nGLn.nn.
        nYLnnGgnnLYn
        nGLGnggnGLGn
        .nGgngGngGn.
        ..nnnGgnnn..
        .nnLnggnLnn.
        nYLGngGnGLYn
        .nGgngGngGn.
        ..nnnGgnnn..
        ....ngGn....
        ...snnnns...
    """)

    private val STAR_BUD = rows("""
        .x.
        xXx
        .x.
    """)

    private val STARFRUIT = rows("""
        ....o....
        ...oho...
        ..ohlmo..
        oooolmooo
        ohhllmmdo
        .ohlmmdo.
        ..olmmo..
        .olmodmo.
        .odo.odo.
        .oo...oo.
    """)
}
