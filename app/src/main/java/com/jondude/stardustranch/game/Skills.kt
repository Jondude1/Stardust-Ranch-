package com.jondude.stardustranch.game

/**
 * Skills: Farming, Foraging, Mining, Fishing and Taming, levels 0-10 earned with XP from everyday work.
 * Every level gives a small passive bonus; at levels 5 and 10 you choose one of two perks.
 * Pixel icons for each skill are hand-authored 12x12 grids. Original design for Stardust Ranch.
 */
object Skill {
    const val FARM = 0; const val FORAGE = 1; const val MINE = 2; const val FISH = 3; const val TAME = 4
    const val COUNT = 5
    val names = arrayOf("Farming", "Foraging", "Mining", "Fishing", "Taming")
    val colors = intArrayOf(0xFF5DA342.toInt(), 0xFF8A9A3A.toInt(), 0xFF7A7A90.toInt(), 0xFF3F86C8.toInt(), 0xFFD0784A.toInt())
    /** What every level adds (shown on the level-up screen and the Skills tab). */
    val perLevel = arrayOf(
        "+3% chance per level of an extra crop at harvest",
        "+4% chance per level of extra forage",
        "+4% chance per level of extra ore",
        "Reel zone a little wider each level",
        "+2% catch chance per level"
    )
    val howToEarn = arrayOf(
        "Harvest crops, till soil and water",
        "Pick forage and chop trees and sticks",
        "Break rocks, ore and gems, defeat gloops",
        "Land fish (harder fish give more)",
        "Win battles and catch creatures"
    )
}

class Perk(val key: String, val skill: Int, val tier: Int, val name: String, val desc: String)

object Skills {
    const val MAX = 10
    /** Total XP needed to reach each level (index = level). */
    val XP = intArrayOf(0, 100, 250, 450, 700, 1000, 1350, 1750, 2200, 2700, 3300)

    fun levelFor(xp: Int): Int { var l = 0; while (l < MAX && xp >= XP[l + 1]) l++; return l }

    val perks = listOf(
        Perk("tiller", Skill.FARM, 5, "Tiller", "Hoe and watering can use half energy"),
        Perk("rancher", Skill.FARM, 5, "Rancher", "Ranch creatures make goods 15% more often"),
        Perk("greenthumb", Skill.FARM, 10, "Green Thumb", "Watered crops sometimes grow an extra day overnight"),
        Perk("market", Skill.FARM, 10, "Market Gardener", "Crops sell and ship for 20% more"),
        Perk("gatherer", Skill.FORAGE, 5, "Gatherer", "20% chance of a bonus forage item"),
        Perk("lumberjack", Skill.FORAGE, 5, "Lumberjack", "Axe +1 power, trees drop 2 more wood"),
        Perk("botanist", Skill.FORAGE, 10, "Botanist", "Forage sells and ships for 30% more"),
        Perk("trailblazer", Skill.FORAGE, 10, "Trailblazer", "Walk 10% faster everywhere"),
        Perk("prospector", Skill.MINE, 5, "Prospector", "Ore rocks always give 1 more ore"),
        Perk("geologist", Skill.MINE, 5, "Geologist", "Gem rocks give rarer gems"),
        Perk("excavator", Skill.MINE, 10, "Excavator", "Pickaxe +1 power"),
        Perk("spelunker", Skill.MINE, 10, "Spelunker", "Ladders down appear twice as often"),
        Perk("angler", Skill.FISH, 5, "Angler", "Fish sell and ship for 25% more"),
        Perk("steady", Skill.FISH, 5, "Steady Hands", "Reel zone is much wider"),
        Perk("luremaster", Skill.FISH, 10, "Lure Master", "Fish bite 40% sooner"),
        Perk("quickreel", Skill.FISH, 10, "Quick Reel", "Every fish needs one less hit to land"),
        Perk("gentle", Skill.TAME, 5, "Gentle Hands", "Orbs are 15% more likely to catch"),
        Perk("trainer", Skill.TAME, 5, "Trainer", "Your creatures earn 25% more battle XP"),
        Perk("bondkeeper", Skill.TAME, 10, "Bond Keeper", "Creature bond grows twice as fast"),
        Perk("champion", Skill.TAME, 10, "Champion", "Your creatures deal 10% more damage")
    )
    val byKey = perks.associateBy { it.key }
    fun choices(skill: Int, tier: Int) = perks.filter { it.skill == skill && it.tier == tier }

    // ---------------------------------------------------------------- icons (12x12)

    private fun c(v: Long) = v.toInt()
    private val ICONS = listOf(
        // Farming: a sprouting seedling in a soil mound
        listOf(
            "......oo....",
            ".....oLLo...",
            "..oo.oLlo...",
            ".oLLoolo....",
            ".oLlLLlo....",
            "..oolLo.....",
            "....olo.....",
            "....olo.....",
            "..oodddoo...",
            ".odDDDDDdo..",
            "oDDDDDDDDdo.",
            ".oooooooooo."),
        // Foraging: a capped mushroom beside a leaf
        listOf(
            "...oooo.....",
            "..oRwRRo....",
            ".oRRRRwRo...",
            "oRwRRRRRro..",
            "orrrrrrrro..",
            ".ooSSSoo..o.",
            "...SSso..oLo",
            "...SSso.oLLo",
            "...SSso.oLlo",
            "..oSSsso.lo.",
            "..oooooo.o..",
            "............"),
        // Mining: a pickaxe
        listOf(
            "..oooooo....",
            ".oMMMMMMoo..",
            "oMMmmmmMMMo.",
            "omo.oHo.oMMo",
            "oo..oHo..omo",
            ".....oHo..o.",
            ".....oHo....",
            "......oHo...",
            "......oHo...",
            ".......oHo..",
            ".......oho..",
            "........o..."),
        // Fishing: a fish with a raised dorsal fin
        listOf(
            "............",
            "....ooo.....",
            "...oBbBo..oo",
            ".ooBBBBBoobo",
            "oBwoBBBBBobo",
            "oBBBBBBBBbbo",
            "oSSBBBBbbobo",
            ".oSSSbbboobo",
            "..oooooo..oo",
            "....obo.....",
            ".....o......",
            "............"),
        // Taming: a paw print
        listOf(
            "...oo..oo...",
            "..oPPooPPo..",
            "..oPpooPpo..",
            ".o.oo..oo.o.",
            "oPo......oPo",
            "oPo.oooo.oPo",
            ".o.oPPPPo.o.",
            "..oPPPPPPo..",
            ".oPPPPPPPPo.",
            ".oPPPPPPppo.",
            "..oppppppo..",
            "...oooooo...")
    )
    private val ICON_PAL = mapOf(
        'o' to c(0xFF2E2024), 'L' to c(0xFF7CC05A), 'l' to c(0xFF3E8A3E), 'd' to c(0xFF8E5C36), 'D' to c(0xFFB07A48),
        'R' to c(0xFFD8504A), 'r' to c(0xFF9A3040), 'w' to c(0xFFFFF0E0), 'S' to c(0xFFF0E2C4), 's' to c(0xFFC4AE8A),
        'M' to c(0xFFC8CCD8), 'm' to c(0xFF8A8EA2), 'H' to c(0xFFB07A48), 'h' to c(0xFF7A4E30),
        'B' to c(0xFF5AA0E0), 'b' to c(0xFF3466A8), 'P' to c(0xFFE8A070), 'p' to c(0xFFB86A44))

    fun icon(skill: Int): Px = Px.grid(ICONS[skill], ICON_PAL)
    val iconRows: List<List<String>> get() = ICONS
}
