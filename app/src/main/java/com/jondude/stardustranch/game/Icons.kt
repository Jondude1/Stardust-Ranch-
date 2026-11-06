package com.jondude.stardustranch.game

/**
 * Hand-authored 16x16 item icons (palette-indexed text grids, one char per pixel, '.' = clear).
 * Light comes from the top-left: 'h' highlight, 'l' light, 'm' mid, 'd' shadow, 'o' coloured outline.
 * Shadows are hue-shifted cool, lights warm. Secondary materials use their own ramps
 * (leaf n/g/G/L/Y, wood q/w/W/v/V, metal k/K/s/S/H). Original art for Stardust Ranch.
 */
object Icons {
    private fun c(v: Long) = v.toInt()
    private fun r5(o: Long, d: Long, m: Long, l: Long, h: Long) = mapOf('o' to c(o), 'd' to c(d), 'm' to c(m), 'l' to c(l), 'h' to c(h))

    val LEAF = mapOf('n' to c(0xFF1C4430), 'g' to c(0xFF2C6E3C), 'G' to c(0xFF4A9A40), 'L' to c(0xFF7CC452), 'Y' to c(0xFFC6E87C))
    val WOOD = mapOf('q' to c(0xFF3A2218), 'w' to c(0xFF6A3E28), 'W' to c(0xFF96603A), 'v' to c(0xFFC08A50), 'V' to c(0xFFE2B878))
    val METAL = mapOf('k' to c(0xFF2A2C3E), 'K' to c(0xFF545E76), 's' to c(0xFF8A98AE), 'S' to c(0xFFC0CCDA), 'H' to c(0xFFF4F8FC))
    private val BASE = LEAF + WOOD + METAL

    /** Builds a 5-tone hue-shifted ramp from one base colour: shadows drift toward blue-violet, lights toward warm yellow. */
    fun auto(base: Int): Map<Char, Int> {
        val (h, s, v) = hsv(base)
        fun tone(dh: Float, ds: Float, dv: Float) = fromHsv(h + dh, (s + ds).coerceIn(0f, 1f), (v * dv).coerceIn(0f, 1f))
        val cool = if (h in 60f..240f) 1f else -1f   // which way is "toward violet"
        val warm = -cool
        return mapOf(
            'o' to tone(cool * 22f, 0.12f, 0.38f), 'd' to tone(cool * 12f, 0.08f, 0.68f), 'm' to base,
            'l' to tone(warm * 8f, -0.14f, 1.18f), 'h' to tone(warm * 14f, -0.34f, 1.36f)
        )
    }

    private fun hsv(c: Int): Triple<Float, Float, Float> {
        val r = (c shr 16 and 255) / 255f; val g = (c shr 8 and 255) / 255f; val b = (c and 255) / 255f
        val mx = maxOf(r, g, b); val mn = minOf(r, g, b); val d = mx - mn
        val h = when { d == 0f -> 0f; mx == r -> 60f * (((g - b) / d) % 6f); mx == g -> 60f * ((b - r) / d + 2f); else -> 60f * ((r - g) / d + 4f) }
        return Triple((h + 360f) % 360f, if (mx == 0f) 0f else d / mx, mx)
    }

    private fun fromHsv(h0: Float, s: Float, v: Float): Int {
        val h = ((h0 % 360f) + 360f) % 360f
        val cc = v * s; val x = cc * (1 - kotlin.math.abs((h / 60f) % 2f - 1)); val m = v - cc
        val (r, g, b) = when ((h / 60f).toInt()) { 0 -> Triple(cc, x, 0f); 1 -> Triple(x, cc, 0f); 2 -> Triple(0f, cc, x); 3 -> Triple(0f, x, cc); 4 -> Triple(x, 0f, cc); else -> Triple(cc, 0f, x) }
        fun ch(f: Float) = ((f + m) * 255f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (ch(r) shl 16) or (ch(g) shl 8) or ch(b)
    }

    private fun sized(p: Px): Px { if (p.w == 16 && p.h == 16) return p; val o = Px(16, 16); o.blit(p, 0, 0); return o }
    private fun g(rows: List<String>, pal: Map<Char, Int>) = sized(Px.grid(rows, BASE + pal))

    /** Returns the hand-authored icon for an item, or null to use the procedural fallback. */
    fun icon(d: ItemDef): Px? {
        if (d.kind == Kind.SEED) return seedPacket(d)
        Fish.byId[d.id]?.let { return fish(it) }
        val grid = GRIDS[d.id] ?: return null
        val pal = PALS[d.id] ?: auto(d.c1)
        return g(grid, pal)
    }

    // =====================================================================
    // Seeds: one paper packet, tinted label + seed cluster in the crop's colour
    // =====================================================================
    private val PACKET = listOf(
        "................",
        "...pppppppppp...",
        "..pPPPPPPPPPPp..",
        "..pVVVVVVVVVvp..",
        "..pccccccccccp..",
        "..pVVVVVVVVVvp..",
        "..pVVVlhVVVVvp..",
        "..pVVlmmmhVVvp..",
        "..pVVhmddmVVvp..",
        "..pVVmmlmdlVvp..",
        "..pVVVdmdmmVvp..",
        "..pVVVVVddVVvp..",
        "..pVVVVVVVVVvp..",
        "..pvvvvvvvvvvp..",
        "...pppppppppp...",
        "................"
    )
    private fun seedPacket(d: ItemDef): Px {
        val crop = Crops.list[d.crop]
        val r = auto(crop.color)
        val pal = mapOf(
            'p' to c(0xFF5A3A24), 'P' to c(0xFFB08454), 'V' to c(0xFFE8CF9C), 'v' to c(0xFFC9A876),
            'c' to (r['d'] ?: crop.color)
        ) + r
        return g(PACKET, pal)
    }

    // =====================================================================
    // Fish: shared hand-drawn body shapes, each species gets its own palette and markings ('x')
    // =====================================================================
    private val FISH_SHAPES = arrayOf(
        // 0: slim river fish
        listOf(
            "................",
            "................",
            "................",
            "......ofFo......",
            ".....oFFFfo..oo.",
            "...oooooooooofFo",
            "..ohhlllmxmmmoFo",
            ".ohwellmmxmmdoFo",
            ".olllmmmxmmmdoof",
            ".ddBBBBmmmmddofo",
            "..odBBBBdddooofo",
            "...ooofFooooo.oo",
            "......oo........",
            "................",
            "................",
            "................"
        ),
        // 1: deep-bodied perch / bream
        listOf(
            "................",
            "................",
            ".......ofo......",
            "......ofFfo.....",
            ".....oooooooo...",
            "....ohhllxmmmo.o",
            "...ohllmmxmmmdoF",
            "..ohwelmmxmmmdfF",
            ".olleemmmxmmmdfo",
            ".olllmmmmxmmddoF",
            "..odBBBBmxmmddfF",
            "...odBBBBdddoo.o",
            "....ooofFoooo...",
            ".......oo.......",
            "................",
            "................"
        ),
        // 2: long eel
        listOf(
            "................",
            "................",
            "................",
            "................",
            "..........ooooo.",
            "..ooooo..ofFFFfo",
            ".ohhllmoohlmmmdo",
            "ohwellmmmlmmxddo",
            "olleemmxmmmxddo.",
            "oddBBBBmmmxddo..",
            ".oodBBBddddoo...",
            "...ooooooo......",
            "................",
            "................",
            "................",
            "................"
        ),
        // 3: round puffer
        listOf(
            "................",
            "................",
            ".....o..o..o....",
            "....oxooxooxo...",
            "...ohhllmmmmdo..",
            "..ohhllxmmxmmdoo",
            ".ohwelmmmmmmmdFo",
            ".olleemxmmxmmdfo",
            ".olllmmmmmmmddFo",
            ".oddBBBxBBxmddoo",
            "..odBBBBBBBdddo.",
            "...oxdBBBBddxo..",
            "....oxooxooxo...",
            ".....o..o..o....",
            "................",
            "................"
        ),
        // 4: flat ray
        listOf(
            "................",
            "................",
            ".......oo.......",
            "......ohlo......",
            ".....ohllmo.....",
            "...oohlxmmmoo...",
            ".oohllmmmxmmdoo.",
            "ohhllxmmmmmmmddo",
            ".oolmmmmxmmmdoo.",
            "...oodmmmmddoo..",
            ".....odmmddo....",
            "......odddo.....",
            ".......ofo......",
            "........of......",
            ".........fo.....",
            "..........o....."
        ),
        // 5: whiskered bottom-feeder (catfish / sturgeon)
        listOf(
            "................",
            "................",
            "................",
            ".......ofFo.....",
            "....ooooooooo.oo",
            "...ohhllmmmmmofo",
            "..ohwellmxmxmdoF",
            ".ohleelmmmmmmmdF",
            "oolllmmxmxmmmdoF",
            "f.oBBBBmmmmmddof",
            ".f.odBBBdddddo.o",
            "..f.ooofFoooo...",
            "........o.......",
            "................",
            "................",
            "................"
        ),
        // 6: ornamental koi with flowing fins
        listOf(
            "................",
            "................",
            "......oFFo......",
            ".....oFfFFo.....",
            "...oooooooooo.oo",
            "..ohhlxxlmmmmoFF",
            ".ohwelxxxmmmmdFf",
            ".olleemxmmxxmdfF",
            ".olllmmmmxxxddoF",
            "..oBBBBmmmxddoFF",
            "...odBBBdddoo.oF",
            "....ooFfoFfo....",
            ".....oFFooFFo...",
            "......ooo..oo...",
            "................",
            "................"
        )
    )

    private fun fish(f: FishDef): Px {
        val body = auto(f.body)
        val fin = auto(f.fin)
        val pal = body + mapOf(
            'B' to f.belly, 'x' to f.mark, 'f' to (fin['d'] ?: f.fin), 'F' to (fin['l'] ?: f.fin),
            'e' to c(0xFF16121E), 'w' to c(0xFFF8F8F0)
        )
        return g(FISH_SHAPES[f.shape], pal)
    }

    // =====================================================================
    // Bobber shown on the water while fishing (8x8)
    // =====================================================================
    val BOBBER = listOf(
        "...kk...",
        "...kk...",
        "..oRRo..",
        ".oRhRRo.",
        ".oWWWWo.",
        ".oWWWwo.",
        "..owwo..",
        "...oo..."
    )
    val BOBBER_PAL = mapOf('k' to c(0xFF2A2C3E), 'o' to c(0xFF4A1A22), 'R' to c(0xFFE0403A), 'h' to c(0xFFFFB0A0),
        'W' to c(0xFFF4F0E6), 'w' to c(0xFFB8B4C8))

    // =====================================================================
    // Per-item grids
    // =====================================================================
    private var BERRY: List<String> = emptyList()
    private val GRIDS = HashMap<Int, List<String>>()
    private val PALS = HashMap<Int, Map<Char, Int>>()
    private fun put(id: Int, pal: Map<Char, Int>?, vararg rows: String) { GRIDS[id] = rows.toList(); if (pal != null) PALS[id] = pal }

    init {
        // ---- tools ----
        put(Tool.HOE, null,
            "................",
            "........kkkkk...",
            ".......kHSSSsk..",
            "........kkqkSsk.",
            "..........qvkSsk",
            ".........qvWkSsk",
            "........qvWqkSKk",
            ".......qvWq.kSKk",
            "......qvWq..kKKk",
            ".....qvWq....kk.",
            "....qvWq........",
            "...qvWq.........",
            "..qvWq..........",
            ".qvWq...........",
            ".qWq............",
            "..q.............")
        put(Tool.CAN, null,
            "................",
            "................",
            "....kkkkk.......",
            "...k.....k......",
            "..k.kkkkkkk.....",
            "..kkHSSSSsKk..kk",
            "..kHSSSSsssKkkSk",
            ".kKkSSssssKKkSk.",
            ".kKkSsssssKKSk..",
            ".kKkSsssssKKk...",
            "..kkSsssssKKk...",
            "...kSsssKKKKk...",
            "...kKKKKKKKKk...",
            "....kkkkkkkk....",
            "................",
            "................")
        put(Tool.PICK, null,
            "................",
            "......kkkk......",
            "....kkSHHSkk....",
            "...kSHkqqkSskk..",
            "..kSkk.qvq.kKsk.",
            "..kk..qvWq..kKk.",
            "..k..qvWq....kk.",
            ".....qvWq.....k.",
            "....qvWq........",
            "...qvWq.........",
            "...qvWq.........",
            "..qvWq..........",
            "..qvWq..........",
            ".qvWq...........",
            ".qWq............",
            "..q.............")
        put(Tool.AXE, null,
            "................",
            ".........kkk....",
            "........kSHSk...",
            ".......kSHSssk..",
            "......kqkSsssKk.",
            ".....qvqkssKKKk.",
            "....qvWq.kKKkk..",
            "...qvWq...kk....",
            "...qvWq.........",
            "..qvWq..........",
            "..qvWq..........",
            ".qvWq...........",
            ".qvWq...........",
            ".qWq............",
            "..q.............",
            "................")
        put(Tool.SCYTHE, null,
            "................",
            "..kkkkkkkk......",
            ".kSHHHHSSskk....",
            "kSHkkkkkkkKsqk..",
            "kSk.......kqvq..",
            "kk........qvWq..",
            "k.........qvq...",
            "..........qvWq..",
            "..........qvq...",
            ".........qvWq...",
            ".........qvq....",
            "........qvWq....",
            "........qvq.....",
            ".......qvWq.....",
            ".......qWq......",
            "........q.......")
        put(Tool.SWORD, mapOf('r' to c(0xFF8A5A2A), 'R' to c(0xFFB88A3A)),
            "................",
            "............kk..",
            "...........kHSk.",
            "..........kHSsk.",
            ".........kHSsk..",
            "........kHSsk...",
            ".......kHSsk....",
            "......kHSsk.....",
            "..kk.kHSsk......",
            "..kRkSSsk.......",
            "...kRRsk........",
            "....qRRk........",
            "...qvqkRk.......",
            "..qvq..kk.......",
            "..qq............",
            "................")
        put(Tool.ROD, mapOf('R' to c(0xFFC0302A), 'r' to c(0xFF7A1A1E), 't' to c(0xFF9A96B0), 'o' to c(0xFF4A1A22), 'b' to c(0xFFF4F0E6)),
            "............qq..",
            "...........qvqt.",
            "..........qvq.t.",
            ".........qWq..t.",
            "........qWq...t.",
            ".......qrRq...t.",
            "......qWq.....t.",
            ".....qWq......t.",
            "..kkqWq.......t.",
            ".kSHkq........t.",
            ".kSsSk........t.",
            ".kKsKk.......oRo",
            "..kkkq.......obo",
            "....qVq.......o.",
            "...qVq..........",
            "...qq...........")

        // ---- produce ----
        put(Items.produce(0), mapOf('o' to c(0xFF4A2C48), 'd' to c(0xFFA88AA8), 'm' to c(0xFFE6DCE6), 'l' to c(0xFFF4EEF2), 'h' to c(0xFFFFFFFF),
            'p' to c(0xFF9E3E80), 'P' to c(0xFFD06AA8), 'r' to c(0xFFF0A4CC)),
            "................",
            "....nn....nn....",
            "...nLGn..nLGn...",
            "...nGLGnnGLgn...",
            "....ngGGGGgn....",
            ".....nngGnn.....",
            "....oopPPpoo....",
            "...orrPPPPPpo...",
            "..orPPPPPPPPpo..",
            "..ohlmmmmmmmdo..",
            "..ohlmmmmmmmdo..",
            "..olmmmmmmmmdo..",
            "...olmmmmmmdo...",
            "....odmmmddo....",
            ".....oddddo.....",
            ".......oo.......")
        put(Items.produce(1), r5(0xFF4A2E22, 0xFF8A5E3A, 0xFFB88A56, 0xFFD6AE74, 0xFFEED29A),
            "................",
            "................",
            "................",
            "......oooo......",
            "....oollmmoo....",
            "...olhhlmmmdo...",
            "..olhllmmdmmdo..",
            "..olllmmmmmmdo..",
            ".olmmdmmmmmmmdo.",
            ".olmmmmmmmdmmdo.",
            ".odmmmmmmmmmddo.",
            "..odmmmdmmmmddo.",
            "..oddmmmmmmddo..",
            "...ooddddddoo...",
            ".....oooooo.....",
            "................")
        put(Items.produce(2), r5(0xFF5E5444, 0xFFC8BC98, 0xFFEDE6CC, 0xFFF8F4E4, 0xFFFFFFFF),
            "................",
            "................",
            "......oooo......",
            "....oohllmoo....",
            "...ohllmhllmo...",
            "..ohlmmllmmmdo..",
            "..olmdmmlmmddo..",
            ".nolmmmdmmmddon.",
            "nGnoddmmmdddonGn",
            "nLGnoodddooonGgn",
            ".nLGGnooooonGGgn",
            ".nGLGGGgGGGGGgn.",
            "..nGGLGgGgGGgn..",
            "...nngGGgGGnn...",
            ".....nnggnn.....",
            "................")
        put(Items.produce(3), r5(0xFF5A1424, 0xFFA01E30, 0xFFE0333F, 0xFFF2626A, 0xFFFFB0B0) + mapOf('y' to c(0xFFF6E27A)),
            "................",
            "........n.......",
            ".......nGn......",
            "....nnnGLnnn....",
            "...nLGGgGGLGn...",
            "..ooonGgGGnooo..",
            ".olhlmonnomlmdo.",
            ".ohlmymmmmymmdo.",
            ".olmmmmmymmmmdo.",
            ".olmymmmmmmymdo.",
            "..olmmmymmmmdo..",
            "..odmmmmmymddo..",
            "...odmymmmddo...",
            "....oddmmddo....",
            ".....oddddo.....",
            "......oooo......")
        put(Items.produce(4), r5(0xFF5A1A14, 0xFFA8301E, 0xFFE5462F, 0xFFF47A52, 0xFFFFC8A0),
            "................",
            "................",
            ".......nn.......",
            "......nLn.......",
            "....onnGnnoo....",
            "...onGLgGLnmo...",
            "..olhonGnodmmo..",
            ".olhhlmnmmmmmdo.",
            ".olhlmmmmmmmmdo.",
            ".olllmmmmmmmmdo.",
            ".olmmmmmmmmmddo.",
            ".odmmmmmmmmmddo.",
            "..odmmmmmmmddo..",
            "...oddmmmdddo...",
            "....ooddddoo....",
            "......oooo......")
        val berry = listOf("..ooo..", ".ohlmo.", "ohlmmdo", "olmcmdo", "odmmddo", ".odddo.", "..ooo..")
        BERRY = berry
        put(Items.produce(6), r5(0xFF1E3A22, 0xFF3E7A34, 0xFF6AAE48, 0xFF9AD06A, 0xFFD2EEA0) + mapOf('x' to c(0xFF2E5E2A)),
            "................",
            ".......q........",
            "......qv........",
            ".....oooooo.....",
            "...oohlxmlxoo...",
            "..ohlxlmxmmxdo..",
            ".ohlxhlmxmmxddo.",
            ".olxllmxmmxmmdo.",
            "olxlmmmxmmxmmddo",
            "olxlmmxmmmxmmddo",
            "oxlmmmxmmmxmdxdo",
            ".odxmmxmmmxmddo.",
            ".oddxmxmmxmdddo.",
            "..oddxdmmxddoo..",
            "...ooddddddoo...",
            ".....oooooo.....")
        put(Items.produce(7), r5(0xFF6A4A10, 0xFFC89A20, 0xFFF5D547, 0xFFFFE98A, 0xFFFFF8D0),
            "......oo........",
            ".....ohlo.......",
            ".....ohmdo......",
            "....ohlmmdo.....",
            "....olhmdmo.....",
            "....ohlmmdo.....",
            "....olmdmdo.....",
            "...nolhmmdon....",
            "..nLnolmdmonGn..",
            "..nGLnomdmonGgn.",
            "..nGLGnodonGGgn.",
            "...nGLGnonGGgn..",
            "...nGGLGnGGgn...",
            "....nGGGgGgn....",
            ".....nnGgGn.....",
            ".......nnn......")
        put(Items.produce(8), r5(0xFF5A2410, 0xFFB4521A, 0xFFE58A22, 0xFFF5AE4A, 0xFFFFD88A),
            "................",
            ".......nn.......",
            "......nGgn......",
            "...ooooGgoooo...",
            "..ohlmodomlmdo..",
            ".ohhlmdomdmmmdo.",
            ".ohlmmdmmdmmmdo.",
            "ohllmmdmmdmmmddo",
            "ohlmmmdmmdmmmddo",
            "olmmmmdmmdmmmddo",
            "olmmmmdmmdmmmddo",
            "odmmmmdmmdmmdddo",
            ".odmmmdmmdmmddo.",
            ".oddmmddddmmddo.",
            "..ooddoooodddoo.",
            "....oooo..oooo..")
        put(Items.produce(10), r5(0xFF2A1438, 0xFF4A2466, 0xFF6C3B8E, 0xFF9A64B8, 0xFFD2A8E6),
            "................",
            "...........nn...",
            "..........nGn...",
            ".......nnnGLnn..",
            "......nGLGgGGn..",
            ".....onGGgGnno..",
            "....ohlnnnnmdo..",
            "...ohlmmmmmmdo..",
            "..ohlmmmmmmmdo..",
            "..olmmmmmmmddo..",
            ".ohlmmmmmmmddo..",
            ".olmmmmmmmddo...",
            ".olmmmmmmdddo...",
            ".odmmmmmddoo....",
            "..oddddddo......",
            "...oooooo.......")
        put(Items.produce(11), r5(0xFF2A4A6A, 0xFF6AA6C8, 0xFFA8DAF0, 0xFFD4F0FA, 0xFFFFFFFF) +
            mapOf('n' to c(0xFF1E4A54), 'g' to c(0xFF3A7A84), 'G' to c(0xFF5AA6AE), 'L' to c(0xFF8ED2D6)),
            "................",
            "....nn....nn....",
            "....nLn..nLn....",
            ".....nGnnGn.nn..",
            "..nn.nGLGn.nLn..",
            "..nLnnGgGnnGn...",
            "...nGGgGgGGn....",
            "....oohlmmoo....",
            "...ohlllmmmdo...",
            "...ohlmmmmmdo...",
            "....olmmmmdo....",
            "....olmmmddo....",
            ".....olmmdo.....",
            ".....odmddo.....",
            "......oddo......",
            ".......oo.......")
        put(Items.produce(12), r5(0xFF6A4A10, 0xFFD4A020, 0xFFFFE04A, 0xFFFFF08A, 0xFFFFFFE0),
            ".......oo.......",
            "......ohlo......",
            "......ohmo......",
            ".....ohlmdo.....",
            ".....ohlmdo.....",
            "ooooohlmmmdooooo",
            "ohhllllmmmmmmddo",
            ".ohllmmmmmmmddo.",
            "..ohlmmmmmmddo..",
            "...olmmmmmmdo...",
            "...olmmmmmmdo...",
            "..olmmmddmmmdo..",
            "..olmmdoodmmdo..",
            ".olmddo..oddmdo.",
            ".oddoo....ooddo.",
            ".ooo........ooo.")

        // ---- forage ----
        put(50, r5(0xFF3E4A34, 0xFFB8C49E, 0xFFE4ECD4, 0xFFF4F8EC, 0xFFFFFFFF),
            "........nn......",
            ".......nLn..nn..",
            "...nn..nGn.nLn..",
            "...nLn.nGnnGn...",
            "....nGnnGLGn....",
            ".....nGnGGn.....",
            "......nGGgn.....",
            "......ngGgn.....",
            "......ohlmo.....",
            "......ohlmo.....",
            "......olmdo.....",
            "......olmdo.....",
            ".....oolmdoo....",
            "....ohlmmmddo...",
            "...o.oddddo.o...",
            "..o..o.o.o..o...")
        put(51, r5(0xFF7A4A08, 0xFFD8A418, 0xFFF7DF3C, 0xFFFFF07A, 0xFFFFFCD8) + mapOf('t' to c(0xFFE0782A), 'T' to c(0xFFF6A040)),
            "................",
            "....oo....oo....",
            "...ohlo..olmo...",
            "...olmmoomlmo...",
            "....olmoTTomo...",
            ".oooolotTTtoooo.",
            "ohhllmoTttoomddo",
            ".oolmmotttommoo.",
            "...oommottomoo..",
            "....olmoooomo...",
            "...olmdo..omdo..",
            "...ooo.nn..ooo..",
            ".......nGn......",
            "...nn..nGn......",
            "....nGnnGn......",
            ".....nnGgn......")
        put(52, r5(0xFF5A1414, 0xFFA82A20, 0xFFE24B2A, 0xFFF47E52, 0xFFFFC4A0),
            ".........nn.....",
            "........nGLn....",
            ".......nGLGn....",
            "...nnnnGgGn.....",
            "..nLGGGgnn......",
            "...nnoogn.......",
            ".....ohoon.oo...",
            "....olmmgnohlo..",
            "....odmdognlmdo.",
            ".....odoonodddo.",
            "....oo..gn.ooo..",
            "...ohlo.gn......",
            "...olmdoonoo....",
            "...oddooohlo....",
            "....oo..olmdo...",
            ".........ooo....")
        put(53, r5(0xFF5A1E58, 0xFFB04AA8, 0xFFE58BD8, 0xFFF4B8EA, 0xFFFFE4FA),
            "................",
            ".....oooo.......",
            "....ohhlmo.oo...",
            "...ohllmmooldo..",
            "...olmmdmolmdo..",
            "....odmdomlmdo..",
            "....oooolmmdo...",
            "...ohlomdddoo...",
            "...olmoooooo....",
            "....odo.n.......",
            ".....oo.nGn.....",
            "........nGn.nn..",
            "..nn....nGnnLn..",
            "...nLnn.nGnGn...",
            "....nnGnnGgn....",
            "......nnngn.....")
        put(54, r5(0xFF3A2010, 0xFF7A4A24, 0xFFA8733D, 0xFFCC9A5E, 0xFFEAC48C) + mapOf('t' to c(0xFF5A3A1E), 'T' to c(0xFF8A6438)),
            "................",
            ".......qq.......",
            "......qWq.......",
            "....tttttttt....",
            "...tTTVTTTTTt...",
            "..tTVTTTTTTTTt..",
            "..ttTtTtTtTttt..",
            "..oollmmmmmddo..",
            "..ohhlmmmmmddo..",
            "..ohllmmmmmddo..",
            "..olmmmmmmmddo..",
            "...olmmmmmddo...",
            "...odmmmmmddo...",
            "....oddmmddo....",
            ".....oodddo.....",
            ".......ooo......")
        put(55, r5(0xFF2A1438, 0xFF52286E, 0xFF7A3E9D, 0xFFA868C4, 0xFFDCB0EE),
            "................",
            "........q..nn...",
            "........qnnLGn..",
            ".......qnGLGgn..",
            ".......qnnGgnn..",
            "....oooooonn....",
            "...ohhlmmmmoo...",
            "..ohhlmmmmmddo..",
            "..ohlmmmmmmddo..",
            ".ohlmmmmmmmmddo.",
            ".olmmmmmmmmmddo.",
            ".olmmmmmmmmdddo.",
            "..odmmmmmmdddo..",
            "..oddmmmmdddoo..",
            "...ooddddddo....",
            ".....oooooo.....")
        put(56, r5(0xFF2A4A78, 0xFF5A9AD0, 0xFFA8E4F4, 0xFFD8F6FC, 0xFFFFFFFF),
            "................",
            ".......o........",
            "......ohoo......",
            ".....ohhlmoo....",
            "....ohhlmlmmo...",
            "...ohhlmhlmmdo..",
            "..ohhllmlmmmddo.",
            "..ohlllmlmmmddo.",
            ".ohllmmhmmmmddo.",
            ".olllmmlmmmdddo.",
            ".olmmmmlmmmddo..",
            "..olmmmlmmddoo..",
            "..oddmmlmdddo...",
            "...ooddlddoo....",
            ".....ooooo......",
            "................")
        put(57, r5(0xFF4A3424, 0xFF9A7A58, 0xFFD9C4A3, 0xFFEEDEC2, 0xFFFFF6E4),
            "................",
            "................",
            "..ww............",
            "...wooo.........",
            "...ohhlooo......",
            "..ohhllmmmoo....",
            "..olllmmdmmmoo..",
            "..olmmmmmmmmmdo.",
            "..olmmdmmmmmmddo",
            "...olmmmmmmdmmdo",
            "...odmmmmmmmmddo",
            "....oddmmmdmmdo.",
            ".....ooddddddoww",
            ".......oooooo..w",
            "................",
            "................")
        put(58, r5(0xFF6A3A2E, 0xFFC9866E, 0xFFF0C4AE, 0xFFF8DECE, 0xFFFFF4EA),
            "................",
            "................",
            "......ooooo.....",
            "....oohlmlmoo...",
            "...ohldhldmmdo..",
            "..ohlldhldmmddo.",
            "..ohlmdlmdmddmo.",
            ".ohlmdlmmdmdmmdo",
            ".olmmdlmmdmdmmdo",
            ".olmdlmmmdmmdmdo",
            "..odmdmmdmmddoo.",
            "...oodmmdmddo...",
            ".....odmmddo....",
            "....odddddddo...",
            "....oooooooooo..",
            "................")
        put(59, r5(0xFF6A1A22, 0xFFC0383A, 0xFFF06B5E, 0xFFFF9E88, 0xFFFFD2C4),
            "................",
            "..oo......oo....",
            ".ohlo....ohlo...",
            ".olmo.oo.olmo...",
            "..olmohlooldo...",
            "..olmolmoldo.oo.",
            "...olmlmldo.ohlo",
            "....olmmldoolmdo",
            ".oo.olmmdolmdo..",
            "ohlooolmddmdo...",
            "olmdmolmdddo....",
            ".oddmmlmddo.....",
            "...oodmmdo......",
            ".....odmdo......",
            "....oodddoo.....",
            "....ooooooo.....")
        put(60, r5(0xFF1E1430, 0xFF3A2A5A, 0xFF4B3A6E, 0xFF7A68A8, 0xFFB8A8E0) + mapOf('s' to c(0xFF8E7CC3), 'S' to c(0xFFD2C4F4)),
            "................",
            ".S.....S.....S..",
            "..s....s....s...",
            "...s.ooooo.s....",
            "....ohhlmmoo....",
            ".S.ohlmmmmmdoS..",
            "..sohlmmmmmdos..",
            "Sssolmmmmmmddoss",
            "..sodmmmmmmddos.",
            ".s.odmmmmmdddo.s",
            "....oddmmdddo...",
            "...s.oooooo.s...",
            "..s....s....s...",
            ".S.....S.....S..",
            "................",
            "................")
        put(61, r5(0xFF4A2E1A, 0xFF9A6A3E, 0xFFD9B98A, 0xFFEAD2A8, 0xFFFFF0D2) + mapOf('t' to c(0xFFC8BCA4), 'T' to c(0xFFF2EAD8), 'u' to c(0xFF7A6A58)),
            "................",
            "................",
            ".....oooooo.....",
            "...oohhllmmoo...",
            "..ohhlhlmmmmdo..",
            ".ohhllmmmmmmmdo.",
            ".ohllmmmmmmmddo.",
            ".olmmmmmmmmdddo.",
            "..oddddddddddo..",
            "...oouTTTtuoo...",
            ".....uTTttu.....",
            ".....uTTttu.....",
            ".....uTttttu....",
            "....uTTtttu.....",
            ".....uuuuuu.....",
            "................")

        // ---- resources ----
        put(Items.WOOD, null,
            "................",
            "................",
            "................",
            "................",
            "....qq..........",
            "..qqWqqqqqqqq...",
            ".qvVVVvvvvWWqqq.",
            ".qvvvvWWvvvqVVVq",
            ".qWWvvvvWWWqVwVq",
            ".qwWWWWwwWWqwVwq",
            ".qwwwwWWwwwqVwVq",
            ".qwwwwwwwwwqVVVq",
            "..qqqqqqqqqqqqq.",
            "................",
            "................",
            "................")
        put(Items.STONE, r5(0xFF34303E, 0xFF5E5A68, 0xFF8A8690, 0xFFB2AEAE, 0xFFDCD8D0),
            "................",
            "................",
            "................",
            "......oooo......",
            "....oohhllmo....",
            "...ohhlllmmmo...",
            "..ohllmmmmdmmo..",
            "..ollmmmmmmdmdo.",
            ".ohlmmmmdmmmddo.",
            ".olmmmmmmmmdddo.",
            ".odmmmdmmmddddo.",
            "..oddmmmmddddo..",
            "...ooddddddoo...",
            ".....oooooo.....",
            "................",
            "................")
        put(Items.FIBER, null,
            "................",
            "....n.....n.....",
            "....Ln...nL..n..",
            "...nLn..nLn.nL..",
            "...nGLn.nGn.nLn.",
            "....nGn.nGnnGn..",
            "....nGLnnGLnGn..",
            ".....nGnnGnGn...",
            ".....nGGnGGGn...",
            "....qWWWWWWWWq..",
            "....qvVVvvvvWq..",
            ".....nGnnGnGn...",
            ".....nGnnGnGn...",
            "....nGn.nGn.Gn..",
            "....nn..nn..nn..",
            "................")
        val ore = listOf(
            "................",
            "................",
            "................",
            "......oooo......",
            "....oohhllmo....",
            "...ohhxXlmmmo...",
            "..ohllXxmmdmmo..",
            "..ollmmmmXxdmdo.",
            ".ohlXxmmdmxXddo.",
            ".olmxXmmmmmdddo.",
            ".odmmmdmXxddddo.",
            "..oddmmmxXdddo..",
            "...ooddddddoo...",
            ".....oooooo.....",
            "................",
            "................")
        val rock = r5(0xFF34303E, 0xFF5E5A68, 0xFF8A8690, 0xFFB2AEAE, 0xFFDCD8D0)
        put(Items.COPPER, rock + mapOf('x' to c(0xFFB8582A), 'X' to c(0xFFF2A066)), *ore.toTypedArray())
        put(Items.IRON, rock + mapOf('x' to c(0xFF9AA4B8), 'X' to c(0xFFEEF2F8)), *ore.toTypedArray())
        put(Items.GOLD, rock + mapOf('x' to c(0xFFD49A1A), 'X' to c(0xFFFFE680)), *ore.toTypedArray())
        put(Items.COAL, r5(0xFF0E0C14, 0xFF1E1C26, 0xFF32303C, 0xFF52505E, 0xFF8A8898),
            "................",
            "................",
            "................",
            ".......ooo......",
            ".....oohlmoo....",
            "....ohhlmmmdo...",
            "...ohllmdmmddo..",
            "..ohlmmhmmdddo..",
            "..olmmhlmmmddoo.",
            ".olmmmlmmmddddo.",
            ".odmmmmmmdddddo.",
            "..oddmmmddddoo..",
            "...ooddddddo....",
            ".....oooooo.....",
            "................",
            "................")
        val gem = listOf(
            "................",
            "................",
            "................",
            "....oooooooo....",
            "...ohhlhlmmdo...",
            "..ohhllhlmmmdo..",
            ".oddddddddddddo.",
            ".ohlllmmmmmdddo.",
            "..ohllmmmmddo...",
            "...ohlmmmddo....",
            "....ohlmddo.....",
            ".....olmdo......",
            "......odo.......",
            ".......o........",
            "................",
            "................")
        put(Items.RUBY, r5(0xFF4A0A1A, 0xFF98142A, 0xFFE8304A, 0xFFFF6E7A, 0xFFFFD0D4), *gem.toTypedArray())
        put(Items.DIAMOND, r5(0xFF2A5A7A, 0xFF7ABCD8, 0xFFC8F0FA, 0xFFE8FBFF, 0xFFFFFFFF), *gem.toTypedArray())
        val crystal = listOf(
            "................",
            ".......oo.......",
            "......ohlo......",
            "......ohmo...o..",
            "..o..ohlmdo.olo.",
            ".olo.ohlmdo.olo.",
            ".ohlooohlmdoohdo",
            ".ohmdoohlmdoolmo",
            ".olmdoohlmddolmo",
            ".olmdoolmmddoldo",
            "..olmdolmmdolddo",
            "..oolmdlmddolmo.",
            "...oooooooooooo.",
            "..oKsSSSssKKkko.",
            "...kkkkkkkkkkk..",
            "................")
        put(Items.QUARTZ, r5(0xFF5A6A7A, 0xFFB4C8D4, 0xFFE4EEF2, 0xFFF4F8FA, 0xFFFFFFFF), *crystal.toTypedArray())
        put(Items.AMETHYST, r5(0xFF2E1048, 0xFF6A2E9A, 0xFFB46BE0, 0xFFD4A0F2, 0xFFF4E0FF), *crystal.toTypedArray())
        put(Items.GEL, r5(0xFF1A4A26, 0xFF3E9A40, 0xFF7DDB6A, 0xFFAEF08E, 0xFFEAFFD8),
            "................",
            "................",
            "................",
            "................",
            ".......oo.......",
            "......ohlo......",
            ".....ohhlmo.....",
            "....ohhlmmmo....",
            "...ohhllmmmdo...",
            "..ohlllmmmmmdo..",
            ".ohllmmmmmmmddo.",
            ".olmmmmmmmmmddo.",
            ".odmmmmmmmmdddo.",
            "..oodddddddddo..",
            "....oooooooo....",
            "................")

        // ---- food ----
        put(Items.SNACK, r5(0xFF4A2A14, 0xFF8A5A2A, 0xFFD9A35A, 0xFFEEC27E, 0xFFFFE4B0) + mapOf('r' to c(0xFFC0303A), 'R' to c(0xFFF06070), 'y' to c(0xFFF2D04A)),
            "................",
            "................",
            "................",
            "....oooooooo....",
            "...ohhllllmmo...",
            "..ohlmmmmmmmdo..",
            "..ooooooooooooo.",
            "..oGLGGRrRyyGGo.",
            "..oogogoooogogo.",
            "..ohhlllmmmmmdo.",
            "..olmmmmmmmmddo.",
            "..odmmmmmmmdddo.",
            "...ooooooooooo..",
            "................",
            "................",
            "................")
        put(Items.TONIC, r5(0xFF4A0E2E, 0xFF98205A, 0xFFD94A8A, 0xFFF080B4, 0xFFFFD0E4) + mapOf('c' to c(0xFF6A4628), 'C' to c(0xFFA87848), 'g' to c(0xFF9AB4C4), 'G' to c(0xFFE8F4FA)),
            "................",
            "......cccc......",
            "......cCCc......",
            "......gGGg......",
            "......gGgg......",
            ".....gGGggg.....",
            "....gGGggggg....",
            "...gGoooooogg...",
            "...ghlmmmmmdg...",
            "...ghllmmmmdg...",
            "...glhmmmmddg...",
            "...glmmmmmddg...",
            "...gdmmmmdddg...",
            "....gggggggg....",
            "................",
            "................")
        put(Items.FEAST, r5(0xFF5A2A14, 0xFFA8581E, 0xFFE0903A, 0xFFF2B860, 0xFFFFE0A0) + mapOf('p' to c(0xFF8A8AA0), 'P' to c(0xFFE8E8F0), 'W' to c(0xFFFFFFFF), 'r' to c(0xFFC0303A)),
            "................",
            "................",
            "................",
            ".......Wn.......",
            ".....ooonoo.....",
            "....ohhlmmmo....",
            "...ohhlmmmmdo...",
            "...ohllmmmmdo.G.",
            "..rolmmmmmddoLGn",
            "..rodmmmmmddonGn",
            ".pPoodddddoooPPp",
            "pPPPPPPPPPPPPPPp",
            ".pPPPPPPPPPPPPp.",
            "..pppppppppppp..",
            "................",
            "................")

        // ---- placeables ----
        val spr = listOf(
            "................",
            "......kkkk......",
            ".....kSHSsk.....",
            "....kSkkkksk....",
            "....kSkxxkKk....",
            "..wwkskxxkKkww..",
            ".w..kkKkkKkk..w.",
            "......kSsk......",
            "....kkkSskkk....",
            "...kSHSSssKKk...",
            "..kSSssssssKKk..",
            "..kSssssssKKKk..",
            "..kKKKKKKKKKKk..",
            "...kkkkkkkkkk...",
            "................",
            "................")
        put(Items.SPRINKLER, mapOf('x' to c(0xFF3A7ADA), 'w' to c(0xFF9AD6FF)), *spr.toTypedArray())
        put(Items.QSPRINKLER, mapOf('x' to c(0xFF3A7ADA), 'w' to c(0xFF9AD6FF), 'k' to c(0xFF5A3A0A), 'K' to c(0xFFB4801A), 's' to c(0xFFE8B83A), 'S' to c(0xFFFFE07A), 'H' to c(0xFFFFFAD8)), *spr.toTypedArray())
    }


    /** Berry clusters are assembled from one hand-drawn berry so each berry keeps clean shading. */
    fun cluster(d: ItemDef): Px? {
        val crop = when (d.id) { Items.produce(5) -> 5; Items.produce(9) -> 9; else -> return null }
        val pal = BASE + (if (crop == 5) r5(0xFF1C1E48, 0xFF2C3C8C, 0xFF4560D6, 0xFF7A94EE, 0xFFC8D6FF) + mapOf('c' to c(0xFF1C1E48))
                          else r5(0xFF4A0A1A, 0xFF8A1428, 0xFFB4223A, 0xFFE0506A, 0xFFFFB0BC) + mapOf('c' to c(0xFFB4223A)))
        val p = Px(16, 16)
        val leaf = Px.grid(listOf("..nn....", ".nLGn...", "nLGgn...", ".nGgnn..", "..nnGn..", "....nn.."), BASE)
        p.blit(leaf, 6, 0)
        val b = Px.grid(BERRY, pal)
        val spots = if (crop == 5) listOf(1 to 3, 8 to 4, 4 to 8) else listOf(2 to 4, 8 to 3, 1 to 9, 7 to 9)
        for ((x, y) in spots) p.blit(b, x, y)
        return p
    }
}

// =====================================================================
// Fish data (original species)
// =====================================================================

object Loc { const val OCEAN = 1; const val RIVER = 2; const val POND = 4; const val LAKE = 8 }

data class FishDef(
    val id: Int, val name: String, val loc: Int, val seasons: Int, val from: Int, val to: Int, val rainOnly: Boolean,
    val diff: Float, val price: Int, val weight: Int, val shape: Int,
    val body: Int, val belly: Int, val fin: Int, val mark: Int, val desc: String
)

object Fish {
    private fun c(v: Long) = v.toInt()
    private const val ALL = 15
    val list = listOf(
        FishDef(120, "Sand Sardel", Loc.OCEAN, ALL, 360, 1560, false, 0.10f, 25, 12, 0, c(0xFF7E9CB8), c(0xFFE8EEF4), c(0xFF9AB4CC), c(0xFF5A7A9A), "A little silver schooling fish."),
        FishDef(121, "Bluefin Skipper", Loc.OCEAN, Season.SP or Season.SU, 360, 1140, false, 0.35f, 65, 8, 0, c(0xFF3E78C8), c(0xFFE4EEF8), c(0xFF2A5AB0), c(0xFF2A4A9A), "Skips across the waves on summer mornings."),
        FishDef(122, "Coral Puffer", Loc.OCEAN, Season.SU, 600, 1080, false, 0.45f, 110, 5, 3, c(0xFFF2A04A), c(0xFFFFF0D0), c(0xFFE07A3A), c(0xFFC0503A), "Puffs up into a spiky ball when caught."),
        FishDef(123, "Lantern Eel", Loc.OCEAN, Season.FA or Season.WI, 1080, 1560, false, 0.65f, 160, 4, 2, c(0xFF3A4A7A), c(0xFF9AE0D0), c(0xFF5AE0C0), c(0xFF8AF0D8), "Its spots glow softly in the dark sea."),
        FishDef(124, "Storm Ray", Loc.OCEAN, Season.SP or Season.SU or Season.FA, 360, 1560, true, 0.75f, 230, 4, 4, c(0xFF5A6478), c(0xFFDDE2EA), c(0xFF3E4658), c(0xFFF2D04A), "Only rises to the surface when it rains."),
        FishDef(125, "River Minnet", Loc.RIVER, ALL, 360, 1560, false, 0.08f, 20, 12, 0, c(0xFF8AA06A), c(0xFFEAF0D0), c(0xFFA8B884), c(0xFF6A7E4E), "Darts around the shallows in every season."),
        FishDef(126, "Speckled Trout", Loc.RIVER, Season.SP or Season.FA, 360, 1200, false, 0.40f, 75, 7, 0, c(0xFF6E8A5A), c(0xFFF2D4C4), c(0xFFC07A6A), c(0xFF2E3A28), "Freckled with dark spots and a rosy side."),
        FishDef(127, "Sunfin Perch", Loc.RIVER, Season.SU, 360, 1140, false, 0.25f, 55, 9, 1, c(0xFFC8B23A), c(0xFFFFF0B0), c(0xFFF07A2A), c(0xFF5A6A2A), "Its fins blaze orange in the summer sun."),
        FishDef(128, "Mossback Catfish", Loc.RIVER, Season.SP or Season.SU or Season.FA, 360, 1560, true, 0.70f, 170, 4, 5, c(0xFF5A6A3A), c(0xFFD8D0A8), c(0xFF46562E), c(0xFF34421E), "Grows a coat of moss. Comes out in the rain."),
        FishDef(129, "Frostgill Pike", Loc.RIVER, Season.WI, 360, 1560, false, 0.55f, 125, 7, 0, c(0xFF7AA4B4), c(0xFFF0FAFF), c(0xFFB8E0F0), c(0xFF4A7A8E), "A sleek hunter of icy winter rivers."),
        FishDef(130, "Mudcarp", Loc.POND, ALL, 360, 1560, false, 0.15f, 35, 12, 1, c(0xFF9A8458), c(0xFFE8DCB4), c(0xFF7A6440), c(0xFF6A5838), "Happy in any muddy pond."),
        FishDef(131, "Lily Koi", Loc.POND, Season.SP or Season.SU, 480, 1140, false, 0.50f, 145, 5, 6, c(0xFFF4EEE4), c(0xFFFFFFFF), c(0xFFF2B4A0), c(0xFFE0502E), "A graceful pond fish painted orange and white."),
        FishDef(132, "Bramble Bream", Loc.LAKE, Season.SP or Season.SU or Season.FA, 360, 1560, false, 0.20f, 45, 10, 1, c(0xFF8A7AA0), c(0xFFEDE4F2), c(0xFF6A5A8A), c(0xFF5A4A78), "A plump violet bream from the mountain lake."),
        FishDef(133, "Glacier Char", Loc.LAKE, Season.FA or Season.WI, 360, 1560, false, 0.45f, 105, 7, 0, c(0xFF5A6E8A), c(0xFFF2A07A), c(0xFF8AA4C4), c(0xFFE8F2FA), "Cold-water fish with a sunset belly."),
        FishDef(134, "Starlit Sturgeon", Loc.LAKE, Season.SU or Season.FA, 1200, 1560, false, 0.90f, 420, 2, 5, c(0xFF2E3458), c(0xFFB8BCE0), c(0xFF4A5290), c(0xFFFFE07A), "Ancient and rare. Said to swim up to look at the stars."),
        FishDef(135, "Dawn Goby", Loc.POND or Loc.RIVER, ALL, 360, 600, false, 0.30f, 60, 6, 1, c(0xFFE08AA0), c(0xFFFFE8EE), c(0xFFF2C04A), c(0xFFB04A6E), "Only bites in the first hours after sunrise.")
    )
    val byId: Map<Int, FishDef> = list.associateBy { it.id }
}
