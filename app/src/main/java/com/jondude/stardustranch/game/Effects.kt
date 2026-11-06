package com.jondude.stardustranch.game

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Effects and atmosphere for Stardust Ranch (original pixel art):
 *  - hand-authored particle sprites (wood chips, stone shards, leaves/petals in seasonal ramps, soil clods,
 *    water drops, gel drops, thorn twigs) and animated sprites (4-frame dust puff, 4-frame water splash,
 *    4-frame twinkle, 3-frame rain ripple), raindrop streaks, three snowflakes, a firefly glow;
 *  - the field-ability obstacles: a thorn bramble (3 season palettes) and a cracked crag;
 *  - [Particle], a tiny physics particle (ground position + height, so chips arc and bounce);
 *  - [Light], a pixel lightmap (4 cells per tile, world aligned) for night, dusk, rain and the mines: each
 *    light source falls off in dithered bands, warm sources tint their pool amber, cool ones (crystals) violet.
 * Grids: one char per pixel, '.' transparent, light from the top-left, outlines a dark tone of the local colour.
 */
object Effects {
    private fun c(v: Long) = v.toInt()

    // ---- particle kinds ----
    const val CHIP = 0; const val SHARD = 1; const val LEAF = 2; const val CLOD = 3; const val DROP = 4; const val GEL = 5; const val TWIG = 6
    // animated, stationary kinds (frame = progress)
    const val PUFF = 10; const val SPLASH = 11; const val TWINKLE = 12; const val RIPPLE = 13; const val STEP = 14

    // leaf tints for LEAF particles
    const val T_SPRING = 0; const val T_SUMMER = 1; const val T_FALL = 2; const val T_WINTER = 3; const val T_GRASS = 4; const val T_PETAL = 5

    // ------------------------------------------------------------------ grids

    private val CHIP_G = listOf(
        listOf("oo..", "oLBo", ".oMo", "..o."),
        listOf(".oo.", "oLBo", "oBMo", ".oo."),
        listOf("....", "oooo", "oLBM", "oooo")
    )
    private val SHARD_G = listOf(
        listOf(".o..", "olo.", "ombo", ".oo."),
        listOf("oo..", "olmo", ".obo", "..o."),
        listOf("...", ".o.", "olo", "omo")
    )
    private val LEAF_G = listOf(
        listOf("...o", ".olo", "oLDo", ".oo."),
        listOf("oo..", "oLlo", ".oDo", "..o."),
        listOf("....", ".oo.", "oLDo", ".oo.")
    )
    private val CLOD_G = listOf(
        listOf(".oo.", "oLBo", "oBDo", ".oo."),
        listOf("oo.", "oLo", ".oo"),
        listOf("...", ".o.", "oBo")
    )
    private val DROP_G = listOf(
        listOf(".h.", "hVv", ".v."),
        listOf("h.", "vw"),
        listOf(".v.", "vVv", ".w.")
    )
    private val TWIG_G = listOf(
        listOf("t...", "oSo.", ".oSo", "..t."),
        listOf("..t", "oSo", "So.", "t.."),
        listOf("t.t", "oSo", ".o.")
    )

    /** 4-frame dust puff (12x10): a lumpy cloud that billows up, then thins out and drifts apart. */
    private val PUFF_G = listOf(
        listOf(
            "............",
            "............",
            "............",
            "............",
            "....oo......",
            "...ohho.....",
            "..ohhlloo...",
            "..olllmmo...",
            "...ommmo....",
            "....ooo....."),
        listOf(
            "............",
            "............",
            "....oo......",
            "...ohhoo....",
            "..ohhllho...",
            ".ohhllllmo..",
            ".ollllmmmo..",
            ".olmmmmmdo..",
            "..ommddoo...",
            "...ooo......"),
        listOf(
            "............",
            "...oo..oo...",
            "..ohho.ohho.",
            ".ohllmoollmo",
            ".ollmmo.omdo",
            "..ommdo..oo.",
            "...oo.......",
            ".......oo...",
            "......olmo..",
            ".......oo..."),
        listOf(
            "..oo........",
            ".olmo....o..",
            "..oo....omo.",
            ".........o..",
            "............",
            "..o.........",
            ".omo........",
            "..o.....o...",
            "............",
            "............")
    )

    /** 4-frame splash (16x12): a crown of water rises, peaks, and falls back into a ring. */
    private val SPLASH_G = listOf(
        listOf(
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "......h..h......",
            ".....hVhhVh.....",
            "....vVvwwvVv....",
            "...vwwwwwwwwv...",
            "....vvvvvvvv....",
            "................"),
        listOf(
            "................",
            "................",
            ".......h........",
            "....h..V..h.....",
            "....V..v..V.....",
            "...hv.hVh.vh....",
            "...Vv.vVv.vV....",
            "..hVvhVwVhvVh...",
            "..vVwwwwwwwVv...",
            ".vwwwwwwwwwwwv..",
            "..vvvwwwwwvvv...",
            "................"),
        listOf(
            "..h..........h..",
            "..v...h..h...v..",
            "......V..V......",
            ".h...........h..",
            ".v...h....h..v..",
            "................",
            "....h......h....",
            "...hV......Vh...",
            "..hVw......wVh..",
            ".vVw........wVv.",
            "..vvv......vvv..",
            "................"),
        listOf(
            "................",
            "................",
            "................",
            "h..............h",
            "................",
            "..h..........h..",
            "................",
            "................",
            "...hhh....hhh...",
            "..h..........h..",
            "...vvv....vvv...",
            "................")
    )

    /** 4-frame twinkle (9x9): a four-point star blooms with a hot centre and fades. */
    private val TWINKLE_G = listOf(
        listOf(".........", ".........", ".........", "....y....", "...yHy...", "....y....", ".........", ".........", "........."),
        listOf(".........", ".........", "....y....", "....h....", "..yhWhy..", "....h....", "....y....", ".........", "........."),
        listOf("....y....", "....h....", "..y.h.y..", "...hWh...", "yhhWWWhhy", "...hWh...", "..y.h.y..", "....h....", "....y...."),
        listOf(".........", ".........", "....y....", ".........", "..y.h.y..", ".........", "....y....", ".........", ".........")
    )

    /** 3-frame rain ripple (9x4) where a drop lands. */
    private val RIPPLE_G = listOf(
        listOf(".........", "....h....", "...v.v...", "........."),
        listOf(".........", "...hhh...", "..v...v..", "...www..."),
        listOf("..hhhhh..", ".v.....v.", "..w...w..", "........."))

    /** 3-frame footstep dust (8x5). */
    private val STEP_G = listOf(
        listOf("........", "...oo...", "..ohlo..", ".olmmdo.", "..oooo.."),
        listOf("........", ".oo..oo.", "ohlo.olo", ".omo.mdo", "..o...o."),
        listOf("o......o", ".......o", "........", "o.......", "........"))

    /** Raindrop streak (4x10), slanting down-left with the wind; bright head at the bottom. */
    private val RAIN_G = listOf("...w", "...w", "..w.", "..v.", "..v.", ".v..", ".v..", ".V..", "h...", "h...")

    private val SNOW_G = listOf(
        listOf("h"),
        listOf(".s.", "sWs", ".s."),
        listOf("..s..", "s.h.s", ".hWh.", "s.h.s", "..s.."))

    /** Firefly: a lime core with a soft halo (drawn over the night overlay so it glows). */
    private val FIREFLY_G = listOf(".....", "..g..", ".gYg.", "..g..", ".....")

    // ------------------------------------------------------------------ palettes

    private val woodP = mapOf('o' to c(0xFF4A2A2A), 'M' to c(0xFF8A5634), 'B' to c(0xFFB57A44), 'L' to c(0xFFE0B070))
    private val stoneP = mapOf('o' to c(0xFF3E3A4C), 'm' to c(0xFF7C7884), 'b' to c(0xFF9E9AA2), 'l' to c(0xFFD6D2CA))
    private val dirtP = mapOf('o' to c(0xFF3E2626), 'D' to c(0xFF6E4430), 'B' to c(0xFF946038), 'L' to c(0xFFC08A54))
    private val leafP = listOf(
        mapOf('o' to c(0xFF22472E), 'D' to c(0xFF3F8A3C), 'L' to c(0xFF7CC352), 'l' to c(0xFFC2E87A)),
        mapOf('o' to c(0xFF1E3A30), 'D' to c(0xFF33703A), 'L' to c(0xFF5EA244), 'l' to c(0xFFA6D460)),
        mapOf('o' to c(0xFF5A2228), 'D' to c(0xFFB44E28), 'L' to c(0xFFE8902E), 'l' to c(0xFFFAD06A)),
        mapOf('o' to c(0xFF3A2A2A), 'D' to c(0xFF6E5038), 'L' to c(0xFF9C7A52), 'l' to c(0xFFC8AA7A)),
        mapOf('o' to c(0xFF2A4A2A), 'D' to c(0xFF4E9A3A), 'L' to c(0xFF8ED45A), 'l' to c(0xFFD2F08A)),
        mapOf('o' to c(0xFF7A3050), 'D' to c(0xFFE07AA0), 'L' to c(0xFFFAB4CC), 'l' to c(0xFFFFE6F0)))
    private val waterP = mapOf('w' to c(0xCC3F7FC8), 'v' to c(0xE07CC0F0), 'V' to c(0xF0B8E4FF), 'h' to c(0xFFF4FCFF))
    private val twigP = mapOf('o' to c(0xFF2E1A2A), 'S' to c(0xFF6E4452), 't' to c(0xFFE0D2B8))
    private val dustP = mapOf('o' to c(0x8C8A7058), 'd' to c(0xA0907458), 'm' to c(0xB0B09474), 'l' to c(0xC0D0B898), 'h' to c(0xD0EADCC0))
    private val caveDustP = mapOf('o' to c(0x8C4A3E40), 'd' to c(0xA0645450), 'm' to c(0xB07E6E66), 'l' to c(0xC09E8E84), 'h' to c(0xD0C2B4A8))
    private val twinkleP = mapOf('y' to c(0xC0F2C842), 'h' to c(0xFFFFEC9A), 'H' to c(0xFFFFF6D0), 'W' to c(0xFFFFFFFF))
    private val rainP = mapOf('w' to c(0x40A8C8F0), 'v' to c(0x70B8D8FA), 'V' to c(0x90D0E8FF), 'h' to c(0xB0F0F8FF))
    private val rippleP = mapOf('h' to c(0x90E8F4FF), 'v' to c(0x60B8D8FA), 'w' to c(0x40A0C0E8))
    private val snowP = mapOf('h' to c(0xF0FFFFFF), 'W' to c(0xFFFFFFFF), 's' to c(0x90DCEAF8))
    private val fireflyP = mapOf('g' to c(0x70C8F070), 'Y' to c(0xFFF4FFB0))

    private fun g(rows: List<String>, pal: Map<Char, Int>) = Px.grid(rows, pal)

    // ------------------------------------------------------------------ sprites

    /** Debris bits for a particle kind; [tint] picks the leaf/petal palette or overrides a gel/shard colour. */
    fun bits(kind: Int, tint: Int = 0): List<Px> = when (kind) {
        CHIP -> CHIP_G.map { g(it, woodP) }
        SHARD -> if (tint == 0) SHARD_G.map { g(it, stoneP) } else SHARD_G.map { g(it, oreP(tint)) }
        LEAF -> LEAF_G.map { g(it, leafP[tint.coerceIn(0, leafP.size - 1)]) }
        CLOD -> CLOD_G.map { g(it, dirtP) }
        DROP -> DROP_G.map { g(it, waterP) }
        GEL -> DROP_G.map { g(it, gelP(tint)) }
        TWIG -> TWIG_G.map { g(it, twigP) }
        else -> emptyList()
    }

    private fun oreP(col: Int) = mapOf('o' to Px.shade(col, 0.35f), 'm' to Px.shade(col, 0.7f), 'b' to col, 'l' to Px.lighten(col, 0.5f))
    private fun gelP(col: Int) = mapOf('w' to (Px.shade(col, 0.6f) and 0xDDFFFFFF.toInt()), 'v' to col, 'V' to Px.lighten(col, 0.4f), 'h' to Px.lighten(col, 0.8f))

    fun puff(cave: Boolean = false): List<Px> = PUFF_G.map { g(it, if (cave) caveDustP else dustP) }
    fun step(cave: Boolean = false): List<Px> = STEP_G.map { g(it, if (cave) caveDustP else dustP) }
    /** Animated kinds whose sprite sits on the ground (bottom-anchored) rather than centred on the point. */
    fun grounded(kind: Int) = kind == PUFF || kind == SPLASH || kind == STEP
    fun splash(): List<Px> = SPLASH_G.map { g(it, waterP) }
    fun twinkle(): List<Px> = TWINKLE_G.map { g(it, twinkleP) }
    fun ripple(): List<Px> = RIPPLE_G.map { g(it, rippleP) }
    fun rain(): Px = g(RAIN_G, rainP)
    fun snow(): List<Px> = SNOW_G.map { g(it, snowP) }
    fun firefly(): Px = g(FIREFLY_G, fireflyP)

    // ------------------------------------------------------------------ obstacles

    /**
     * Thorn bramble (16x22): overlapping leafy lobes (each lit on its top-left), thorny purple canes arching out
     * over the top and down the sides, and hips/berries tucked in the leaves. The bottom rows fill the full tile
     * width so neighbouring brambles join into one hedge. g/G/L/l leaf ramp, o leaf outline, S/s cane, t thorn,
     * r/R/q berry, _ ground shadow.
     */
    private val THORN_G = listOf(
        "....tt..........",
        "...sSSs.....tt..",
        "..sS..Ss...sSSs.",
        ".tS....Ss.sS..Ss",
        "..s..ooSsSoo..oS",
        ".t.oolLLsSLLoolt",
        "..olLLGGoSGGolLo",
        ".olLGGGgoSgglLGo",
        "olLGrRggosolLGGo",
        "oLGGqrgolLLGGggo",
        "oGGgggolLLGGgrRo",
        "oggolLLsGGgggqro",
        "oglLLGGSsgggolLo",
        "olLGGGgSsggolLGo",
        "oLGrRgggSsolLGgo",
        "oGgqrggooSsLGggo",
        "oggggolLLGSsgggo",
        "ogggolLGGgSsggo.",
        ".oggoGGggggSsgo.",
        "..oooooooooooo..",
        "_______________.",
        ".______________.")

    private fun thornPal(season: Int): Map<Char, Int> {
        val leaves = when (season) {
            2 -> listOf(c(0xFF4A1E2C), c(0xFF7A3428), c(0xFFAE5A2C), c(0xFFDC9442), c(0xFFF4C870))
            3 -> listOf(c(0xFF3A3A50), c(0xFF6A7488), c(0xFFA6B6C8), c(0xFFDAE4EE), c(0xFFFFFFFF))
            1 -> listOf(c(0xFF1A3230), c(0xFF26503A), c(0xFF357040), c(0xFF52924A), c(0xFF8ABE60))
            else -> listOf(c(0xFF1C3830), c(0xFF2A5C3A), c(0xFF3D8040), c(0xFF60A44A), c(0xFF9CCE68))
        }
        return mapOf('o' to leaves[0], 'g' to leaves[1], 'G' to leaves[2], 'L' to leaves[3], 'l' to leaves[4],
            'S' to c(0xFF4E2A44), 's' to c(0xFF925A70), 'C' to c(0xFF26142A), 't' to c(0xFFEEE2C8),
            'r' to c(0xFFC02C4A), 'R' to c(0xFFF47A8E), 'q' to c(0xFF62183A), '_' to c(0x46101820))
    }

    /** Thorn bramble for a season; [flip] mirrors it so a hedge of them doesn't repeat. */
    fun thorn(season: Int, flip: Boolean = false): Px {
        val pal = thornPal(season)
        val o = g(THORN_G, pal)
        // canes outside the leaf mass get a dark rim so they read against the grass
        val src = o.p.copyOf()
        val cane = setOf(pal['S'], pal['s'], pal['t'])
        for (y in 0 until 19) for (x in 0 until 16) {
            if (src[y * 16 + x] != 0) continue
            var n = false
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val xx = x + dx; val yy = y + dy
                if (xx in 0 until 16 && yy in 0 until 19 && src[yy * 16 + xx] in cane && src[yy * 16 + xx] != pal['t']) n = true
            }
            if (n) o.set(x, y, pal['C']!!)
        }
        return if (flip) o.flipH() else o
    }

    /** Cracked crag (16x20): an angular sandstone block split by a deep fissure (smashed by a Stone creature). */
    private val CRAG_G = listOf(
        "................",
        "................",
        ".....oooooo.....",
        "....ohhhhbmo....",
        "...ohhhhbbmmo...",
        "..ohhhbbbcbmdo..",
        "..ohhbbbbcmmdo..",
        ".ohhbbbbcbmmddo.",
        ".ohbbbbbcmmmddo.",
        "ohhbbbbcbmmmdddo",
        "ohbbbmmcCcmmdddo",
        "obbbmmmcmmcmdkdo",
        "obbmmmcmmmmcdkko",
        "obmmmdcmmmmmcdko",
        "odmmmcmmmmmddcko",
        "odmmddcddmmddkko",
        ".odddkcdddddkkko",
        ".okkkkckkkkkkko.",
        "..oooooooooooo..",
        "..ssssssssssss..")
    private val cragP = mapOf('o' to c(0xFF2E2234), 'k' to c(0xFF54404E), 'd' to c(0xFF7A5E5E), 'm' to c(0xFFA6806A),
        'b' to c(0xFFC8A47E), 'h' to c(0xFFEACCA0), 'c' to c(0xFF1E1426), 'C' to c(0xFF3A2A3E), 's' to c(0x46101820))

    fun crag(): Px {
        val p = g(CRAG_G, cragP)
        // a lit lip on the lower-left side of every crack pixel, so the fissure reads as cut into the rock
        val src = p.p.copyOf()
        for (y in 0 until p.h) for (x in 0 until p.w) if (src[y * p.w + x] == cragP['c']) {
            val l = x - 1
            if (l >= 0 && src[y * p.w + l] == cragP['b'] || l >= 0 && src[y * p.w + l] == cragP['m']) p.set(l, y, Px.lighten(src[y * p.w + l], 0.35f))
        }
        // a few pebbles at the foot
        p.set(1, 18, cragP['m']!!); p.set(2, 18, cragP['d']!!); p.set(14, 18, cragP['d']!!)
        return p
    }
}

/** A world particle: (x, y) on the ground in tiles, z = height above it in tiles. */
class Particle(val kind: Int, var x: Float, var y: Float, var z: Float, var vx: Float, var vy: Float, var vz: Float,
               val life: Float, val variant: Int, val tint: Int = 0) {
    var t = 0f
    var bounced = false
    val done: Boolean get() = t >= life
    val progress: Float get() = (t / life).coerceIn(0f, 0.999f)

    fun update(dt: Float) {
        t += dt
        if (kind >= Effects.PUFF) { x += vx * dt; y += vy * dt; return }
        vz -= 14f * dt
        x += vx * dt; y += vy * dt; z += vz * dt
        if (z <= 0f) {
            z = 0f
            if (!bounced && vz < -1.5f) { vz = -vz * 0.35f; vx *= 0.5f; vy *= 0.5f; bounced = true } else { vz = 0f; vx *= 0.8f; vy *= 0.8f }
        }
    }
}

/** What the light overlay should draw this frame: base darkness, shade colour and sources. */
class Lighting(val base: Float, val shade: Int, val srcs: List<LightSrc>)

/** A light source in tile coordinates. [col] = RGB tint of its pool, [str] 0..1. */
class LightSrc(val x: Float, val y: Float, val r: Float, val col: Int, val str: Float = 1f)

/**
 * Pixel lightmap: one ARGB cell per quarter tile, world aligned so it never shimmers as the camera moves.
 * Darkness is a base level (night / dusk / rain / mine) cut away by light sources; each source's falloff is
 * quantised into bands with a 2x2 ordered dither on the band edges, and its colour tints the pool.
 */
object Light {
    const val CELLS = 4
    const val WARM = 0xFF9A3C
    const val CANDLE = 0xFFD890
    const val CRYSTAL = 0xB070F0
    const val GLOW = 0x70E0FF
    private val bayer = floatArrayOf(0f, 0.5f, 0.75f, 0.25f)

    /** Base darkness (0..1) and shade colour for an outdoor scene at [time] minutes. */
    fun outdoor(time: Float, raining: Boolean): Pair<Float, Int> {
        val n = ((time - 1080f) / 240f).coerceIn(0f, 1f)            // 18:00 -> 22:00
        val dusk = ((time - 1000f) / 140f).coerceIn(0f, 1f) * (1f - n) // golden hour before dark
        val nn = n * n * (3 - 2 * n)
        var base = nn * 0.66f + dusk * 0.28f
        if (raining) base += 0.2f * (1f - nn) + 0.06f
        val col = when {
            raining && nn < 0.3f -> 0xFF26304A.toInt()
            nn > 0.01f -> Px.mix(0xFF8A3A30.toInt(), 0xFF0C1232.toInt(), min(1f, nn * 1.6f))
            else -> 0xFFD8702A.toInt()
        }
        return base.coerceIn(0f, 0.9f) to col
    }

    const val MINE_BASE = 0.94f
    const val MINE_COL = 0xFF0A0610.toInt()

    /**
     * Fills [out] (cw x ch cells) for the cells starting at world cell (cx0, cy0).
     * Returns nothing; cells are ARGB, transparent where fully lit.
     */
    fun render(out: IntArray, cw: Int, ch: Int, cx0: Int, cy0: Int, base: Float, shade: Int, srcs: List<LightSrc>) {
        val sr = shade shr 16 and 255; val sg = shade shr 8 and 255; val sb = shade and 255
        val bands = 5f
        for (j in 0 until ch) {
            val wy = (cy0 + j + 0.5f) / CELLS
            for (i in 0 until cw) {
                val wx = (cx0 + i + 0.5f) / CELLS
                var lit = 0f; var tr = 0f; var tg = 0f; var tb = 0f; var tw = 0f
                for (s in srcs) {
                    val dx = wx - s.x; val dy = (wy - s.y) * 1.15f
                    val d2 = dx * dx + dy * dy; val r2 = s.r * s.r
                    if (d2 >= r2) continue
                    val f = (1f - sqrt(d2) / s.r)
                    val v = min(1f, f * 1.6f) * s.str
                    lit = max(lit, v) + (1f - max(lit, v)) * min(lit, v) * 0.5f
                    val w = f * f * s.str
                    tr += (s.col shr 16 and 255) * w; tg += (s.col shr 8 and 255) * w; tb += (s.col and 255) * w; tw += w
                }
                // quantise the lit amount into bands, dithering the band edges
                val q = lit.coerceIn(0f, 1f) * bands
                val fl = floor(q)
                val k = ((cx0 + i) and 1) + (((cy0 + j) and 1) shl 1)
                val band = (fl + if (q - fl > bayer[k] && q < bands) 1f else 0f) / bands * 0.9f
                val dark = base * (1f - band)
                var a = dark
                var r = sr.toFloat(); var gg = sg.toFloat(); var b = sb.toFloat()
                if (tw > 0.001f) {
                    // warm/cool pool tint: strongest mid-falloff, fades to the edge and the hot centre
                    val warm = min(0.75f, tw) * (if (base > 0.3f) 1f else base / 0.3f)
                    val cr = tr / tw; val cg = tg / tw; val cb = tb / tw
                    val m = (warm * 0.9f).coerceIn(0f, 1f)
                    r = r * (1 - m) + cr * m; gg = gg * (1 - m) + cg * m; b = b * (1 - m) + cb * m
                    a = max(a, warm * 0.5f * band)
                }
                val alpha = (a * 255f).toInt().coerceIn(0, 255)
                out[j * cw + i] = if (alpha < 3) 0 else (alpha shl 24) or (r.toInt().coerceIn(0, 255) shl 16) or (gg.toInt().coerceIn(0, 255) shl 8) or b.toInt().coerceIn(0, 255)
            }
        }
    }

    /** Night light sources of an outdoor area: lit windows/doors of buildings (warm), plus [extra]. */
    fun buildingLights(a: Area): List<LightSrc> = a.buildings.mapNotNull { b ->
        when (b.type) {
            Bld.HOUSE, Bld.STORE, Bld.CENTER, Bld.COTTAGE -> LightSrc(b.x + b.tw / 2f, b.y + b.th - 0.2f, b.tw * 0.5f + 1.4f, WARM, 0.68f)
            Bld.SMITHY -> LightSrc(b.x + b.tw / 2f, b.y + b.th - 0.2f, 3.4f, 0xFF8A40, 0.9f)
            Bld.BARN -> LightSrc(b.x + b.tw / 2f, b.y + b.th - 0.3f, 2.4f, CANDLE, 0.55f)
            else -> null
        }
    }

    /** Glowing objects in a dark area: gem and crystal rocks (violet), chests (gold), ladders (pale). */
    fun mineLights(a: Area, x0: Int, y0: Int, x1: Int, y1: Int): List<LightSrc> {
        val out = ArrayList<LightSrc>()
        for (y in max(0, y0)..min(a.h - 1, y1)) for (x in max(0, x0)..min(a.w - 1, x1)) when (a.o(x, y)) {
            Ob.GEM -> out.add(LightSrc(x + 0.5f, y + 0.5f, 1.9f, CRYSTAL, 0.55f))
            Ob.GOLD -> out.add(LightSrc(x + 0.5f, y + 0.5f, 1.4f, 0xFFD050, 0.35f))
            Ob.CHEST -> out.add(LightSrc(x + 0.5f, y + 0.5f, 1.8f, 0xFFD050, 0.5f))
            Ob.LADDER, Ob.LADDERUP -> out.add(LightSrc(x + 0.5f, y + 0.5f, 1.6f, CANDLE, 0.45f))
        }
        return out
    }

    /** Average darkness in a lightmap (0..1), for checks. */
    fun meanAlpha(out: IntArray): Float = if (out.isEmpty()) 0f else out.sumOf { (it ushr 24).toLong() }.toFloat() / out.size / 255f

}
