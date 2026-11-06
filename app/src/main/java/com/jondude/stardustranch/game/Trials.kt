package com.jondude.stardustranch.game

import kotlin.math.hypot

/** One ranger trial: a tamer with a fixed team who awards a badge (and its perk) the first time you beat them. */
class RangerDef(
    val key: String, val name: String, val area: String, val place: String,
    val badge: String, val badgeName: String, val perk: String,
    /** species id to level */
    val team: List<Pair<Int, Int>>, val gold: Int,
    val hello: String, val win: String, val lose: String, val after: String
) {
    fun teamMons(): List<Mon> = team.map { (s, l) -> Mon(s, l) }
    val topLevel: Int get() = team.maxOf { it.second }
}

/**
 * Ranger trials: three field rangers (forest, beach, mountain) and head ranger Wren in the healer's centre.
 * Beat a ranger's whole team in a multi-creature battle (no orbs, no running) to earn their badge; each badge
 * is a small permanent perk. Wren only accepts challengers holding the other three badges.
 * Rematches pay a fifth of the prize, once per ranger per day.
 */
object Trials {
    const val GROVE = "grove"; const val TIDE = "tide"; const val SUMMIT = "summit"; const val STAR = "star"

    val list = listOf(
        RangerDef("juniper", "Ranger Juniper", "forest", "the forest, north of the river", GROVE, "Grove Badge",
            "Your creatures earn 10% more battle XP.",
            listOf(21 to 7, 15 to 8, 0 to 9), 400,
            "I keep watch over the forest trail. Show me you and your creatures work as one!",
            "Your team moves like the wind through leaves. The Grove Badge is yours!",
            "Rest up and try again. The forest isn't going anywhere.",
            "Train in the tall grass - creatures there grow strong on sunlight."),
        RangerDef("marlo", "Ranger Marlo", "beach", "the beach, west of the pier", TIDE, "Tide Badge",
            "Capture Orbs work 10% better.",
            listOf(23 to 11, 6 to 12, 26 to 12, 7 to 13), 700,
            "Waves wear down even stone. Let's see if your team can weather mine!",
            "Ha! You rode that one out. Take the Tide Badge - orbs will feel lighter in your hand.",
            "The tide always comes back in. So can you.",
            "Tide creatures hate Leaf moves. Remember that when you face the sea."),
        RangerDef("flint", "Ranger Flint", "mountain", "the mountain, east of the mine entrance", SUMMIT, "Summit Badge",
            "After each battle you win, your party recovers 15% health.",
            listOf(29 to 15, 9 to 16, 19 to 16, 31 to 17), 1000,
            "Up here the air is thin and the creatures are tough. Ready for the climb?",
            "You reached the top. The Summit Badge is yours - your team will catch its breath after every win.",
            "The mountain humbles everyone once. Come back stronger.",
            "Flame melts Frost and Stone shrugs off Spark. Pack a mixed team."),
        RangerDef("wren", "Ranger Wren", "center", "the healer's centre in town", STAR, "Star Badge",
            "Your creatures deal 10% more battle damage.",
            listOf(12 to 22, 4 to 23, 14 to 23, 1 to 24), 1500,
            "So you've earned all three field badges. Then let's see it - give me everything you've got!",
            "Incredible... You're a true ranger now. The Star Badge is yours. Grandma would be so proud.",
            "Not bad at all! Heal up at the fountain and come back.",
            "Every creature in the valley seems a little braver since you arrived.")
    )
    val byKey = list.associateBy { it.key }
    val fieldRangers = list.filter { it.key != "wren" }

    /** Where each field ranger stands (tile centre), cleared of obstacles by World. */
    val spots = mapOf("forest" to (26 to 6), "beach" to (14 to 9), "mountain" to (26 to 9))

    fun rematchGold(d: RangerDef) = d.gold / 5

    // ------------------------------------------------------------------ badge art

    private fun c(v: Long) = v.toInt()

    private fun field(base: Long): Map<Char, Int> = Icons.auto(c(base)).let { mapOf('a' to it['d']!!, 'b' to it['m']!!, 'c' to it['l']!!, 'e' to it['o']!!) }
    private val RIM = Icons.auto(c(0xFFE0A830))

    private val EMBLEMS = mapOf(
        GROVE to (field(0xFF2E6E52) to """
            .....nn.
            ...nnLYn
            ..nLLYLn
            .nLYLLGn
            .nLLGGn.
            nGLGgnn.
            ngnnn...
            n.......
        """),
        TIDE to (field(0xFF2A4C9A) to """
            ....PPp.
            ..PPpQQp
            .PpQqqQ.
            PpQq..p.
            pQq.....
            QqqQQpPP
            qQQqqQQp
            .qqqqqq.
        """),
        SUMMIT to (field(0xFF6A5490) to """
            ...PP...
            ..PPPp..
            ..PpSps.
            .PSSsps.
            .SSssss.
            SSsSssss
            SssSsssS
            ........
        """),
        STAR to (field(0xFF2A2A62) to """
            ...PP...
            ...Pp...
            PPPPpppp
            .PPPpps.
            ..PPps..
            .PPs.ps.
            .Ps...s.
            ........
        """)
    )
    private val EMBLEM_PAL = mapOf(
        'n' to c(0xFF1C4430), 'g' to c(0xFF2C6E3C), 'G' to c(0xFF4A9A40), 'L' to c(0xFF8CD45A), 'Y' to c(0xFFD8F48A),
        'P' to c(0xFFFFFFFF), 'p' to c(0xFFD6E6F4), 'Q' to c(0xFF6AA8E8), 'q' to c(0xFF3A6EC0),
        'S' to c(0xFFB8B0C8), 's' to c(0xFF7E7298)
    )
    private val STAR_PAL = mapOf('P' to c(0xFFFFF4B0), 'p' to c(0xFFFFD040), 's' to c(0xFFC88A20))

    /** 16x16 medallion: gold rim lit from the top-left, a bevelled coloured field and a hand-drawn emblem. */
    fun badgePx(key: String): Px {
        val (fld, grid) = EMBLEMS[key] ?: EMBLEMS[GROVE]!!
        val p = Px(16, 16)
        for (y in 0 until 16) for (x in 0 until 16) {
            val dx = x + 0.5f - 8f; val dy = y + 0.5f - 8f; val d = hypot(dx, dy)
            if (d > 7.7f) continue
            val lit = if (d == 0f) 0f else (-dx - dy) / (d * 1.414f)
            val col = when {
                d > 6.9f -> RIM['o']!!
                d > 5.2f -> when { lit > 0.45f -> RIM['h']!!; lit > -0.05f -> RIM['l']!!; lit > -0.55f -> RIM['m']!!; else -> RIM['d']!! }
                d > 4.4f -> if (lit > 0.2f) RIM['d']!! else if (lit < -0.4f) RIM['h']!! else RIM['m']!!
                else -> { val t = dx + dy; when { t < -3.2f -> fld['c']!!; t > 3.6f -> fld['a']!!; else -> fld['b']!! } }
            }
            p.set(x, y, col)
        }
        val pal = if (key == STAR) EMBLEM_PAL + STAR_PAL else EMBLEM_PAL
        p.blit(Px.grid(grid.trimIndent().lines().filter { it.isNotEmpty() }, pal), 4, 4)
        return p
    }

    /** A dim, desaturated silhouette for a badge not yet earned. */
    fun lockedPx(key: String): Px {
        val src = badgePx(key); val o = Px(16, 16)
        for (i in src.p.indices) {
            val c = src.p[i]; if (c == 0) continue
            val l = ((c shr 16 and 255) * 30 + (c shr 8 and 255) * 59 + (c and 255) * 11) / 100
            val v = 70 + l / 5
            o.p[i] = (0xFF shl 24) or ((v - 6) shl 16) or ((v - 8) shl 8) or (v + 4)
        }
        return o
    }
}
