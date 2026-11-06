package com.jondude.stardustranch.game

import kotlin.math.hypot
import kotlin.math.max

/**
 * Hand-authored pixel art for the HUD and menus: a wooden 9-slice panel frame with brass rivets and a parchment
 * face, bevelled buttons recoloured through 5-tone hue-shifted ramps, recessed inventory slots, list cards,
 * small status icons (coin, heart, bolt, weather, seasons, buffs) and round touch buttons.
 * Light comes from the top-left. The renderer stretches only the uniform middle strips, so corners stay crisp.
 * Original art for Stardust Ranch.
 */
object Hud {
    private fun c(v: Long) = v.toInt()

    /** Corner sizes (in source pixels) for the 9-slice pieces. */
    const val PANEL_K = 8; const val BUTTON_K = 5; const val SLOT_K = 5; const val ROW_K = 4

    val PANEL = listOf(
        "..OOOOOOOOOOOOOOOOOOOO..",
        ".OhhhhhhhhhhhhhhhhhhhhO.",
        "OhlbbllllllllllllllbblDO",
        "OhbYBbmmmmmmmmmmmmbYBbDO",
        "OhbBbDmmmmmmmmmmmmbBbDDO",
        "OhlbDddddddddddddddbDdDO",
        "OhlmmdOOOOOOOOOOOOlmmdDO",
        "OhlmmdOssssssssssOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOspppppppppOlmmdDO",
        "OhlmmdOOOOOOOOOOOOlmmdDO",
        "OhlbbdlllllllllllllbbdDO",
        "OhbYBbmmmmmmmmmmmmbYBbDO",
        "OhbBbDmmmmmmmmmmmmbBbDDO",
        "OhlbDddddddddddddddbDdDO",
        ".ODDDDDDDDDDDDDDDDDDDDO.",
        "..OOOOOOOOOOOOOOOOOOOO.."
    )
    val PANEL_PAL = mapOf('O' to c(0xFF3A1E1C), 'D' to c(0xFF5E3024), 'd' to c(0xFF7E4428), 'm' to c(0xFFA2622F), 'l' to c(0xFFC78545), 'h' to c(0xFFE8B464), 'b' to c(0xFF6A4214), 'B' to c(0xFFD49A2E), 'Y' to c(0xFFFFE8A0), 'S' to c(0xFFC9A472), 's' to c(0xFFE6CF9E), 'p' to c(0xFFF6E7C4))
    val BUTTON = listOf(
        "..oooooooooooo..",
        ".ohhhhhhhhhhhho.",
        "ohhllllllllllldo",
        "ohllllllllllllmo",
        "ohlmmmmmmmmmmmdo",
        "olmmmmmmmmmmmmdo",
        "olmmmmmmmmmmmmdo",
        "olmmmmmmmmmmmmdo",
        "olmmmmmmmmmmmmdo",
        "olmmmmmmmmmmmmdo",
        "olmmmmmmmmmmmmdo",
        "olmmmmmmmmmmmddo",
        "odmmmmmmmmmmdddo",
        "oddddddddddddddo",
        ".oDDDDDDDDDDDDo.",
        "..oooooooooooo.."
    )
    val SLOT = listOf(
        ".eeeeeeeeeeeeee.",
        "eSSSSSSSSSSSSSSe",
        "eSsssssssssssswe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSsqqqqqqqqqqqwe",
        "eSwwwwwwwwwwwwwe",
        ".eeeeeeeeeeeeee."
    )
    val SLOT_PAL = mapOf('e' to c(0xFF7A5A3C), 'S' to c(0xFFB4935F), 's' to c(0xFFD2B783), 'q' to c(0xFFE9D5A6), 'w' to c(0xFFFAEDCC))
    val SLOT_SEL = listOf(
        ".eeeeeeeeeeeeee.",
        "eSSSSSSSSSSSSSSe",
        "eSssssssssssssse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsqqqqqqqqqqSse",
        "eSsSSSSSSSSSSSse",
        "eSssssssssssssse",
        ".eeeeeeeeeeeeee."
    )
    val SLOT_SEL_PAL = mapOf('e' to c(0xFF8A3A0E), 'S' to c(0xFFFFE27A), 's' to c(0xFFF2B640), 'q' to c(0xFFFFF0C4), 'w' to c(0xFFFFFFFF), 'r' to c(0xFFD0502A))
    val ROW = listOf(
        ".eeeeeeeeee.",
        "ewwwwwwwwwwe",
        "ewqqqqqqqqse",
        "ewqqqqqqqqse",
        "ewqqqqqqqqse",
        "ewqqqqqqqqse",
        "ewqqqqqqqqse",
        "ewqqqqqqqqse",
        "ewqqqqqqqqse",
        "ewqqqqqqqqse",
        "ewssssssssse",
        ".eeeeeeeeee."
    )
    val ROW_PAL = mapOf('e' to c(0xFFA67E4E), 'w' to c(0xFFFBF0D8), 'q' to c(0xFFEBD7A8), 's' to c(0xFFD8BE8A))
    val ROW_SEL_PAL = mapOf('e' to c(0xFFC07A1E), 'w' to c(0xFFFFFBE0), 'q' to c(0xFFFFE7A0), 's' to c(0xFFF2C866))
    val COIN = listOf(
        "..ooooo..",
        ".ohYYYBo.",
        "ohYYbYBBo",
        "oYYbhYBbo",
        "oYYbYBBbo",
        "oYBbBBBbo",
        "oBBBBBbbo",
        ".obbbbbo.",
        "..ooooo.."
    )
    val COIN_PAL = mapOf('o' to c(0xFF6A3408), 'b' to c(0xFFB8741A), 'B' to c(0xFFE8A82A), 'Y' to c(0xFFFFD860), 'h' to c(0xFFFFF6C8))
    val HEART = listOf(
        ".ooo.ooo.",
        "ohhmommmo",
        "ohlmmmmdo",
        "olmmmmmdo",
        ".ommmmdo.",
        "..ommdo..",
        "...odo...",
        "....o...."
    )
    val HEART_PAL = mapOf('o' to c(0xFF5A0E22), 'd' to c(0xFFA8203A), 'm' to c(0xFFE2483A), 'l' to c(0xFFF4806A), 'h' to c(0xFFFFD0C0))
    val BOLT = listOf(
        "....oooo",
        "...ohYbo",
        "..ohYbo.",
        ".ohYYooo",
        "ohYYYYbo",
        "oooohbo.",
        "...ohbo.",
        "..ohbo..",
        "..obo...",
        "..oo...."
    )
    val BOLT_PAL = mapOf('o' to c(0xFF5A2E0A), 'b' to c(0xFFD08A1A), 'Y' to c(0xFFFFE04A), 'h' to c(0xFFFFF8C8))
    val SUN = listOf(
        ".....B.....",
        ".B.......B.",
        "...ooooo...",
        "..ohhYYBo..",
        "..ohYYYBo..",
        "B.oYYYBbo.B",
        "..oYYBBbo..",
        "..oBBbbbo..",
        "...ooooo...",
        ".B.......B.",
        ".....B....."
    )
    val SUN_PAL = mapOf('o' to c(0xFFA8401A), 'B' to c(0xFFF0A02A), 'Y' to c(0xFFFFD84A), 'h' to c(0xFFFFF6C0), 'b' to c(0xFFE07A1A))
    val RAIN = listOf(
        "...oooo...",
        "..occcCoo.",
        ".occccCCCo",
        "occcccCCko",
        "oCCCCCkkko",
        ".oooooooo.",
        "..R..R..R.",
        ".r..r..r..",
        ".........."
    )
    val RAIN_PAL = mapOf('o' to c(0xFF3A4466), 'c' to c(0xFFF4F7FB), 'C' to c(0xFFC4D0E2), 'k' to c(0xFF8A98B8), 'r' to c(0xFF3A8AE0), 'R' to c(0xFF9AD0FF))
    val FLAKE = listOf(
        "....b....",
        ".b..b..b.",
        "..b.w.b..",
        "...bwb...",
        "bbwwowwbb",
        "...bwb...",
        "..b.w.b..",
        ".b..b..b.",
        "....b...."
    )
    val FLAKE_PAL = mapOf('o' to c(0xFF2A5A8A), 'b' to c(0xFF5AA8E8), 'w' to c(0xFFE8F8FF))
    val SPROUT = listOf(
        ".nn...nn.",
        "nLGn.nGgn",
        "nYLGnGgn.",
        ".nLGGgn..",
        "..nnGnn..",
        "....Gn...",
        "....gn...",
        "..qWWWq..",
        ".qwwwwwq."
    )
    val SPROUT_PAL = mapOf('n' to c(0xFF1C4430), 'g' to c(0xFF2C6E3C), 'G' to c(0xFF4A9A40), 'L' to c(0xFF7CC452), 'Y' to c(0xFFC6E87C), 'q' to c(0xFF3A2218), 'w' to c(0xFF6A3E28), 'W' to c(0xFF96603A))
    val FALL = listOf(
        "......oo.",
        "....oohlo",
        "..oohllmo",
        ".ohllmmdo",
        ".olmmqmdo",
        "olmmqmddo",
        "olmqmddo.",
        "oqqoddo..",
        "qo.ooo..."
    )
    val FALL_PAL = mapOf('o' to c(0xFF5A1E0A), 'd' to c(0xFFA8401A), 'm' to c(0xFFE07A2A), 'l' to c(0xFFF4A84A), 'h' to c(0xFFFFD890), 'q' to c(0xFF6A2A10))
    val FEATHER = listOf(
        ".......oo",
        ".....oolo",
        "....ohlmo",
        "...ohlmdo",
        "..ohlmdo.",
        ".ohlmdo..",
        ".olmdo...",
        "oodoo....",
        "o........"
    )
    val FEATHER_PAL = mapOf('o' to c(0xFF1E3A6A), 'd' to c(0xFF3A6AB0), 'm' to c(0xFF5AA0E0), 'l' to c(0xFF9AD0F4), 'h' to c(0xFFE0F4FF))
    val CLOVER = listOf(
        ".nn...nn.",
        "nLGn.nGgn",
        "nGGgnGggn",
        ".nngGgnn.",
        "nLGgnGggn",
        "nGgn.ngGn",
        ".nn.n.nn.",
        "....n....",
        "...n....."
    )
    val CLOVER_PAL = mapOf('n' to c(0xFF1C4430), 'g' to c(0xFF2C6E3C), 'G' to c(0xFF4A9A40), 'L' to c(0xFF7CC452), 'Y' to c(0xFFC6E87C), 'q' to c(0xFF3A2218), 'w' to c(0xFF6A3E28), 'W' to c(0xFF96603A))
    val PICK = listOf(
        "..kkkkk..",
        ".kSHHSsk.",
        "kSkkvkkKk",
        "kk.kvk.kk",
        "...kWk...",
        "...kWk...",
        "...kwk...",
        "...kwk...",
        "....k...."
    )
    val PICK_PAL = mapOf('k' to c(0xFF2A2C3E), 'K' to c(0xFF545E76), 's' to c(0xFF8A98AE), 'S' to c(0xFFC0CCDA), 'H' to c(0xFFF4F8FC), 'w' to c(0xFF6A3E28), 'W' to c(0xFF96603A), 'v' to c(0xFFC08A50))
    val XMARK = listOf(
        "oo.....oo",
        "owo...owo",
        ".owo.owo.",
        "..owowo..",
        "...owo...",
        "..owowo..",
        ".owo.owo.",
        "owo...owo",
        "oo.....oo"
    )
    val XMARK_PAL = mapOf('o' to c(0xFF5A1010), 'w' to c(0xFFFFF4E8))
    val ARROW = listOf(
        "ooooooo",
        "ohYYYbo",
        ".oYYbo.",
        "..obo..",
        "...o..."
    )
    val ARROW_PAL = mapOf('o' to c(0xFF6A2A10), 'h' to c(0xFFFFF0B0), 'Y' to c(0xFFF2C042), 'b' to c(0xFFC07A1A))
    val HEART_EMPTY_PAL = mapOf('o' to c(0xFF8A6A4A), 'd' to c(0xFFB89A72), 'm' to c(0xFFCDB28A), 'l' to c(0xFFDCC49E), 'h' to c(0xFFEBD9B8))
    val BOLT_GREEN_PAL = mapOf('o' to c(0xFF1E4A1A), 'b' to c(0xFF3E9A3A), 'Y' to c(0xFF8CE07A), 'h' to c(0xFFE4FFD8))
    val STOVE = listOf(
        "..........kSsk..",
        "..........kSsk..",
        "..........kSsk..",
        "..........kSsk..",
        ".........kkkkkk.",
        ".........kHSssk.",
        "...a......kSsk..",
        "....a.....kSsk..",
        "...a.cc...kSsk..",
        "...cPPpCc.kSsk..",
        "..cPpppCCckSsk..",
        "..cppCCCCcKSsk..",
        "kkkcccccckkkkkkk",
        "kWHHHHHHHHHHHHSk",
        "kSSSSSSSSSSSSSsk",
        "kKKKKKKKKKKKKKKk",
        "kSskkkkkkkkkksKk",
        "kSkrRRyYyRRrkkKk",
        "kSkRyYYYYYyRkkKk",
        "kSkrRyyyyyRrkkKk",
        "kSskkkkkkkkkksKk",
        "kSssHsssssHsssKk",
        "kSsssssssssssKKk",
        "kKKKKKKKKKKKKKKk",
        "kkkkkkkkkkkkkkkk",
        ".kSk........kSk.",
        ".kkk........kkk."
    )
    val STOVE_PAL = mapOf('k' to c(0xFF1C1824), 'K' to c(0xFF2E2A3A), 's' to c(0xFF484458), 'S' to c(0xFF6C6880), 'H' to c(0xFFA29EB6), 'W' to c(0xFFD8D6E4), 'r' to c(0xFFA8281A), 'R' to c(0xFFE85A22), 'y' to c(0xFFFFB03A), 'Y' to c(0xFFFFF0A0), 'c' to c(0xFF5A2210), 'C' to c(0xFF9A4822), 'p' to c(0xFFD07A3A), 'P' to c(0xFFF6B878), 'a' to c(0xFFF0F0F6), 'b' to c(0xFF6A3A20), 'B' to c(0xFFB07040))

    fun panel(): Px = Px.grid(PANEL, PANEL_PAL)
    fun slot(sel: Boolean): Px = if (sel) Px.grid(SLOT_SEL, SLOT_SEL_PAL) else Px.grid(SLOT, SLOT_PAL)
    fun row(sel: Boolean): Px = Px.grid(ROW, if (sel) ROW_SEL_PAL else ROW_PAL)

    /** Button in any colour: the base colour becomes a 5-tone ramp (cool shadows, warm lights) plus a darker lip. */
    fun button(base: Int): Px = Px.grid(BUTTON, ramp(base))

    fun ramp(base: Int): Map<Char, Int> {
        val r = Icons.auto(base)
        return r + mapOf('D' to Px.mix(r['o']!!, r['d']!!, 0.45f))
    }

    // icons
    val coin: Px get() = Px.grid(COIN, COIN_PAL)
    fun heart(full: Boolean): Px = Px.grid(HEART, if (full) HEART_PAL else HEART_EMPTY_PAL)
    fun bolt(green: Boolean = false): Px = Px.grid(BOLT, if (green) BOLT_GREEN_PAL else BOLT_PAL)
    val sun: Px get() = Px.grid(SUN, SUN_PAL)
    val rain: Px get() = Px.grid(RAIN, RAIN_PAL)
    val flake: Px get() = Px.grid(FLAKE, FLAKE_PAL)
    val xmark: Px get() = Px.grid(XMARK, XMARK_PAL)
    val arrow: Px get() = Px.grid(ARROW, ARROW_PAL)
    val stove: Px get() = Px.grid(STOVE, STOVE_PAL)

    /** Season icon: sprout, sun, falling leaf, snowflake. */
    fun season(s: Int): Px = when (s) { 0 -> Px.grid(SPROUT, SPROUT_PAL); 1 -> sun; 2 -> Px.grid(FALL, FALL_PAL); else -> flake }

    /** Buff icons, in Buff order: Swift (feather), Lucky (clover), Miner (pickaxe), Hearty (green bolt). */
    fun buff(b: Buff): Px = when (b) {
        Buff.SPEED -> Px.grid(FEATHER, FEATHER_PAL)
        Buff.LUCK -> Px.grid(CLOVER, CLOVER_PAL)
        Buff.MINING -> Px.grid(PICK, PICK_PAL)
        Buff.STAMINA -> bolt(true)
    }

    /**
     * Round bevelled touch button of diameter d (source pixels): coloured outline, a bright rim on the lit
     * upper-left, banded body shading toward the lower-right and a thick shaded lip at the bottom.
     * pressed = drawn sunk (lip gone, darker).
     */
    fun round(d: Int, base: Int, pressed: Boolean = false, alpha: Int = 255): Px {
        val r = ramp(base)
        val p = Px(d, d + 2)
        val R = d / 2f; val cx = d / 2f; val cy = d / 2f
        val lip = if (pressed) 0 else 2
        val top = if (pressed) 2 else 0
        fun a(col: Int) = (alpha shl 24) or (col and 0xFFFFFF)
        // lip (shadow under the button)
        for (y in 0 until d + 2) for (x in 0 until d) {
            val dl = hypot(x + 0.5f - cx, y + 0.5f - (cy + lip + top))
            if (dl <= R) p.set(x, y, a(r['D']!!))
        }
        for (y in 0 until d) for (x in 0 until d) {
            val dx = x + 0.5f - cx; val dy = y + 0.5f - cy
            val dist = hypot(dx, dy)
            if (dist > R) continue
            val yy = y + top
            val col = when {
                dist > R - 1f -> r['o']!!
                dist > R - 2.2f && dx + dy < -R * 0.35f -> r['h']!!
                else -> {
                    val k = (dx * 0.55f + dy * 0.85f) / R    // -1 lit .. +1 shade
                    when { k < -0.55f -> r['l']!!; k < 0.35f -> r['m']!!; else -> r['d']!! }
                }
            }
            p.set(x, yy, a(if (pressed) Px.mix(col, r['o']!!, 0.18f) else col))
        }
        // small specular glint
        val gx = (cx - R * 0.45f).toInt(); val gy = (cy - R * 0.5f).toInt() + top
        if (!pressed && d >= 16) { p.set(gx, gy, a(0xFFFFFFFF.toInt())); p.set(gx + 1, gy, a(r['h']!!)); p.set(gx, gy + 1, a(r['h']!!)) }
        return p
    }

    /** Joystick base: a translucent recessed ring. */
    fun ring(d: Int): Px {
        val p = Px(d, d); val R = d / 2f
        for (y in 0 until d) for (x in 0 until d) {
            val dx = x + 0.5f - R; val dy = y + 0.5f - R; val dist = hypot(dx, dy)
            if (dist > R) continue
            val col = when {
                dist > R - 1f -> 0xC0281C18.toInt()
                dist > R - 3f -> if (dx + dy < 0) 0x90FFF2D0.toInt() else 0x90A08060.toInt()
                dist > R - 4f -> 0x70281C18
                else -> 0x38FFFFFF
            }
            p.set(x, y, col)
        }
        return p
    }

    /** 1-pixel-wide vertical/horizontal bar fill bands for a colour: highlight, light, mid, mid, shade. */
    fun barRamp(base: Int): IntArray { val r = ramp(base); return intArrayOf(r['h']!!, r['l']!!, r['m']!!, r['m']!!, r['d']!!) }

    /** Scales used by Ui: pixel size of HUD art on a screen of height h. */
    fun scaleFor(u: Float): Int = max(2, Math.round(u * 1.5f))
}
