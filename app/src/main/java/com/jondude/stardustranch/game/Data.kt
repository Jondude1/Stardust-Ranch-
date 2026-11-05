package com.jondude.stardustranch.game

// ---------------------------------------------------------------------------
// Static game data: items, crops, creature types, moves and species.
// Everything here is original to Stardust Ranch.
// ---------------------------------------------------------------------------

enum class Kind { TOOL, SEED, CROP, FORAGE, RESOURCE, ORB, FOOD, PLACE, GEM }

data class ItemDef(
    val id: Int, val name: String, val kind: Kind, val price: Int,
    val c1: Int, val c2: Int, val desc: String,
    val crop: Int = -1, val energy: Int = 0, val heal: Int = 0, val orbPower: Float = 0f
) {
    val sell: Int get() = if (kind == Kind.SEED) price / 2 else price
    val stackable: Boolean get() = kind != Kind.TOOL
}

object Tool { const val HOE = 0; const val CAN = 1; const val PICK = 2; const val AXE = 3; const val SCYTHE = 4; const val SWORD = 5 }

object Season {
    val names = arrayOf("Spring", "Summer", "Fall", "Winter")
    const val SP = 1; const val SU = 2; const val FA = 4; const val WI = 8
}

data class CropDef(
    val idx: Int, val name: String, val seasons: Int, val days: Int, val regrow: Int,
    val seedPrice: Int, val sell: Int, val color: Int, val leaf: Int, val yield: Int = 1, val tall: Boolean = false
)

object Crops {
    val list = listOf(
        CropDef(0, "Turnip", Season.SP, 4, 0, 20, 35, 0xFFF2E6EE.toInt(), 0xFF5DB04A.toInt()),
        CropDef(1, "Potato", Season.SP, 6, 0, 50, 80, 0xFFC9A066.toInt(), 0xFF4E9E3D.toInt()),
        CropDef(2, "Cauliflower", Season.SP, 10, 0, 80, 175, 0xFFF4F1DC.toInt(), 0xFF6CB458.toInt()),
        CropDef(3, "Strawberry", Season.SP, 8, 4, 100, 120, 0xFFE8333F.toInt(), 0xFF3F9A3A.toInt()),
        CropDef(4, "Tomato", Season.SU, 9, 4, 50, 60, 0xFFE5462F.toInt(), 0xFF3C8E36.toInt(), tall = true),
        CropDef(5, "Blueberry", Season.SU, 11, 4, 80, 50, 0xFF4560D6.toInt(), 0xFF3F8C3F.toInt(), yield = 3),
        CropDef(6, "Melon", Season.SU, 12, 0, 80, 250, 0xFFF08A9A.toInt(), 0xFF4FA03C.toInt()),
        CropDef(7, "Corn", Season.SU or Season.FA, 12, 4, 150, 50, 0xFFF5D547.toInt(), 0xFF6DAA3A.toInt(), tall = true),
        CropDef(8, "Pumpkin", Season.FA, 13, 0, 100, 320, 0xFFE58A22.toInt(), 0xFF5E9A35.toInt()),
        CropDef(9, "Cranberry", Season.FA, 7, 5, 240, 75, 0xFFB4223A.toInt(), 0xFF6B8A33.toInt(), yield = 2),
        CropDef(10, "Eggplant", Season.FA, 5, 5, 20, 60, 0xFF6C3B8E.toInt(), 0xFF4D8F39.toInt()),
        CropDef(11, "Snowroot", Season.WI, 7, 0, 120, 140, 0xFFBFE6F7.toInt(), 0xFF6FA7B8.toInt()),
        CropDef(12, "Starfruit", Season.SP or Season.SU or Season.FA, 13, 0, 400, 750, 0xFFFFE04A.toInt(), 0xFF48A35A.toInt())
    )
}

object Items {
    // id ranges: tools 0-9, seeds 10-29, produce 30-49, forage 50-69, resources 70-89, orbs 90-99, food 100-109, placeables 110+
    val all = HashMap<Int, ItemDef>()
    private fun add(d: ItemDef) { all[d.id] = d }

    const val WOOD = 70; const val STONE = 71; const val FIBER = 72; const val COPPER = 73; const val IRON = 74
    const val GOLD = 75; const val QUARTZ = 76; const val AMETHYST = 77; const val RUBY = 78; const val DIAMOND = 79
    const val GEL = 80; const val COAL = 81
    const val ORB = 90; const val SUPER_ORB = 91; const val STAR_ORB = 92
    const val SNACK = 100; const val TONIC = 101; const val FEAST = 102
    const val SPRINKLER = 110; const val QSPRINKLER = 111

    fun seed(crop: Int) = 10 + crop
    fun produce(crop: Int) = 30 + crop

    init {
        add(ItemDef(Tool.HOE, "Hoe", Kind.TOOL, 0, 0xFF9B6B3F.toInt(), 0xFFB8C4CC.toInt(), "Tills soil for planting."))
        add(ItemDef(Tool.CAN, "Watering Can", Kind.TOOL, 0, 0xFF6E8FB0.toInt(), 0xFF9FC2E0.toInt(), "Waters crops. Refill at any water."))
        add(ItemDef(Tool.PICK, "Pickaxe", Kind.TOOL, 0, 0xFF9B6B3F.toInt(), 0xFFB8C4CC.toInt(), "Breaks rocks and ore."))
        add(ItemDef(Tool.AXE, "Axe", Kind.TOOL, 0, 0xFF9B6B3F.toInt(), 0xFFB8C4CC.toInt(), "Chops trees and sticks."))
        add(ItemDef(Tool.SCYTHE, "Scythe", Kind.TOOL, 0, 0xFF9B6B3F.toInt(), 0xFFD8DEE2.toInt(), "Cuts grass and weeds, harvests crops."))
        add(ItemDef(Tool.SWORD, "Rusty Sword", Kind.TOOL, 0, 0xFF7A5230.toInt(), 0xFFC8D0D8.toInt(), "Fends off gloops in the mines."))

        for (c in Crops.list) {
            add(ItemDef(seed(c.idx), "${c.name} Seeds", Kind.SEED, c.seedPrice, 0xFFC7A876.toInt(), c.color,
                "Plant in ${seasonText(c.seasons)}. ${c.days} days" + (if (c.regrow > 0) ", keeps producing." else "."), crop = c.idx))
            add(ItemDef(produce(c.idx), c.name, Kind.CROP, c.sell, c.color, c.leaf, "Fresh from the farm.", energy = 15 + c.sell / 12))
        }

        add(ItemDef(50, "Wild Leek", Kind.FORAGE, 60, 0xFFE9F2D8.toInt(), 0xFF6DB04F.toInt(), "Spring forage.", energy = 25))
        add(ItemDef(51, "Daffodil", Kind.FORAGE, 30, 0xFFF7DF3C.toInt(), 0xFF5DA548.toInt(), "Spring forage."))
        add(ItemDef(52, "Spice Berry", Kind.FORAGE, 80, 0xFFE24B2A.toInt(), 0xFF4A8E3A.toInt(), "Summer forage.", energy = 25))
        add(ItemDef(53, "Sweet Pea", Kind.FORAGE, 50, 0xFFE58BD8.toInt(), 0xFF5AA046.toInt(), "Summer forage."))
        add(ItemDef(54, "Hazelnut", Kind.FORAGE, 90, 0xFFA8733D.toInt(), 0xFF6B4524.toInt(), "Fall forage.", energy = 30))
        add(ItemDef(55, "Wild Plum", Kind.FORAGE, 80, 0xFF7A3E9D.toInt(), 0xFF4A8E3A.toInt(), "Fall forage.", energy = 25))
        add(ItemDef(56, "Crystal Fruit", Kind.FORAGE, 150, 0xFFA8E4F4.toInt(), 0xFFFFFFFF.toInt(), "Winter forage.", energy = 40))
        add(ItemDef(57, "Snow Yam", Kind.FORAGE, 100, 0xFFD9C4A3.toInt(), 0xFF8F6E4A.toInt(), "Winter forage.", energy = 30))
        add(ItemDef(58, "Seashell", Kind.FORAGE, 40, 0xFFF2D4C2.toInt(), 0xFFD69A82.toInt(), "Found on the beach."))
        add(ItemDef(59, "Coral", Kind.FORAGE, 80, 0xFFF06B5E.toInt(), 0xFFFFB3A6.toInt(), "Found on the beach."))
        add(ItemDef(60, "Sea Urchin", Kind.FORAGE, 160, 0xFF4B3A6E.toInt(), 0xFF8E7CC3.toInt(), "Spiky, valuable."))
        add(ItemDef(61, "Common Mushroom", Kind.FORAGE, 40, 0xFFD9B98A.toInt(), 0xFFF2E6D0.toInt(), "Grows in the forest.", energy = 20))

        add(ItemDef(WOOD, "Wood", Kind.RESOURCE, 2, 0xFFA0703F.toInt(), 0xFF6E4A28.toInt(), "Building material."))
        add(ItemDef(STONE, "Stone", Kind.RESOURCE, 2, 0xFF9A9A9A.toInt(), 0xFF6E6E6E.toInt(), "Building material."))
        add(ItemDef(FIBER, "Fiber", Kind.RESOURCE, 1, 0xFF7DBB4A.toInt(), 0xFF4E8A30.toInt(), "Plant fiber for crafting."))
        add(ItemDef(COPPER, "Copper Ore", Kind.RESOURCE, 5, 0xFFD9824A.toInt(), 0xFF8A8A8A.toInt(), "Mined in the upper caves."))
        add(ItemDef(IRON, "Iron Ore", Kind.RESOURCE, 10, 0xFFC8CCD6.toInt(), 0xFF7A7A7A.toInt(), "Mined from floor 30 down."))
        add(ItemDef(GOLD, "Gold Ore", Kind.RESOURCE, 25, 0xFFF5C842.toInt(), 0xFF7A7A7A.toInt(), "Mined from floor 60 down."))
        add(ItemDef(QUARTZ, "Quartz", Kind.GEM, 25, 0xFFEFF4F7.toInt(), 0xFFBFD3DD.toInt(), "A clear crystal."))
        add(ItemDef(AMETHYST, "Amethyst", Kind.GEM, 100, 0xFFB46BE0.toInt(), 0xFF7B3EAA.toInt(), "A purple gem."))
        add(ItemDef(RUBY, "Ruby", Kind.GEM, 250, 0xFFE8304A.toInt(), 0xFF98142A.toInt(), "A red gem."))
        add(ItemDef(DIAMOND, "Diamond", Kind.GEM, 750, 0xFFE8FBFF.toInt(), 0xFF8FD8EE.toInt(), "A rare and precious gem."))
        add(ItemDef(GEL, "Gloop Gel", Kind.RESOURCE, 5, 0xFF7DDB6A.toInt(), 0xFF3E9A40.toInt(), "Sticky goo from a gloop."))
        add(ItemDef(COAL, "Coal", Kind.RESOURCE, 15, 0xFF2E2E34.toInt(), 0xFF5A5A62.toInt(), "Burns hot."))

        add(ItemDef(ORB, "Capture Orb", Kind.ORB, 100, 0xFF4FC8B8.toInt(), 0xFFF2C842.toInt(), "Catches wild creatures.", orbPower = 1f))
        add(ItemDef(SUPER_ORB, "Super Orb", Kind.ORB, 350, 0xFF9A6BE0.toInt(), 0xFFD8DCE6.toInt(), "Better catch rate.", orbPower = 1.8f))
        add(ItemDef(STAR_ORB, "Star Orb", Kind.ORB, 1500, 0xFF2A3A8A.toInt(), 0xFFFFE07A.toInt(), "Great catch rate.", orbPower = 3f))

        add(ItemDef(SNACK, "Field Snack", Kind.FOOD, 60, 0xFFD9A35A.toInt(), 0xFF8A5A2A.toInt(), "+40 energy, heals lead creature 30.", energy = 40, heal = 30))
        add(ItemDef(TONIC, "Berry Tonic", Kind.FOOD, 150, 0xFFD94A8A.toInt(), 0xFFFFD0E4.toInt(), "Heals your whole party 60.", energy = 10, heal = 60))
        add(ItemDef(FEAST, "Ranch Feast", Kind.FOOD, 300, 0xFFF2B44A.toInt(), 0xFF8A3A2A.toInt(), "+120 energy and full party heal.", energy = 120, heal = 999))

        add(ItemDef(SPRINKLER, "Sprinkler", Kind.PLACE, 300, 0xFF8FA6B8.toInt(), 0xFF4A8AD9.toInt(), "Waters the 4 tiles around it each morning."))
        add(ItemDef(QSPRINKLER, "Quality Sprinkler", Kind.PLACE, 900, 0xFFF5C842.toInt(), 0xFF4A8AD9.toInt(), "Waters the 8 tiles around it each morning."))
    }

    operator fun get(id: Int): ItemDef = all[id] ?: all[STONE]!!

    fun seasonText(mask: Int): String {
        val s = ArrayList<String>()
        for (i in 0..3) if (mask and (1 shl i) != 0) s.add(Season.names[i])
        return s.joinToString("/")
    }

    fun forageFor(season: Int, beach: Boolean): IntArray = if (beach) intArrayOf(58, 58, 59, 60) else when (season) {
        0 -> intArrayOf(50, 51, 61)
        1 -> intArrayOf(52, 53, 61)
        2 -> intArrayOf(54, 55, 61)
        else -> intArrayOf(56, 57)
    }
}

data class Recipe(val out: Int, val count: Int, val needs: List<Pair<Int, Int>>)

object Recipes {
    val list = listOf(
        Recipe(Items.ORB, 1, listOf(Items.STONE to 3, Items.COPPER to 1)),
        Recipe(Items.SUPER_ORB, 1, listOf(Items.IRON to 1, Items.QUARTZ to 1)),
        Recipe(Items.STAR_ORB, 1, listOf(Items.GOLD to 2, Items.AMETHYST to 1)),
        Recipe(Items.SNACK, 1, listOf(Items.FIBER to 3, 61 to 1)),
        Recipe(Items.TONIC, 1, listOf(Items.GEL to 3, Items.FIBER to 2)),
        Recipe(Items.SPRINKLER, 1, listOf(Items.COPPER to 2, Items.IRON to 1)),
        Recipe(Items.QSPRINKLER, 1, listOf(Items.IRON to 2, Items.GOLD to 1, Items.COAL to 1))
    )
}

// ---------------------------------------------------------------------------
// Creatures
// ---------------------------------------------------------------------------

enum class EType(val label: String, val color: Int) {
    WILD("Wild", 0xFFB5A58A.toInt()), LEAF("Leaf", 0xFF5DBB4A.toInt()), FLAME("Flame", 0xFFF06A2A.toInt()),
    TIDE("Tide", 0xFF3F8BE0.toInt()), STONE("Stone", 0xFFA88A5E.toInt()), SPARK("Spark", 0xFFF2CE2E.toInt()),
    GALE("Gale", 0xFF8FD6E0.toInt()), SHADE("Shade", 0xFF6B4A9E.toInt()), FROST("Frost", 0xFFAEE6F5.toInt()),
    STAR("Star", 0xFFFFD86A.toInt());
}

object TypeChart {
    private val strong = mapOf(
        EType.LEAF to setOf(EType.TIDE, EType.STONE),
        EType.FLAME to setOf(EType.LEAF, EType.FROST),
        EType.TIDE to setOf(EType.FLAME, EType.STONE),
        EType.STONE to setOf(EType.FLAME, EType.SPARK, EType.FROST),
        EType.SPARK to setOf(EType.TIDE, EType.GALE),
        EType.GALE to setOf(EType.LEAF, EType.WILD),
        EType.SHADE to setOf(EType.STAR, EType.SHADE),
        EType.FROST to setOf(EType.GALE, EType.LEAF),
        EType.STAR to setOf(EType.SHADE),
        EType.WILD to emptySet()
    )
    fun mult(atk: EType, def: EType): Float = when {
        strong[atk]?.contains(def) == true -> 2f
        strong[def]?.contains(atk) == true -> 0.5f
        else -> 1f
    }
}

data class Move(val name: String, val type: EType, val power: Int, val level: Int, val heal: Boolean = false)

object Moves {
    val byType: Map<EType, List<Move>> = mapOf(
        EType.WILD to listOf(Move("Tackle", EType.WILD, 35, 1), Move("Headbutt", EType.WILD, 55, 8), Move("Rally Rush", EType.WILD, 80, 20)),
        EType.LEAF to listOf(Move("Vine Lash", EType.LEAF, 40, 1), Move("Seed Burst", EType.LEAF, 60, 9), Move("Sunsoak", EType.LEAF, 0, 14, heal = true), Move("Bloom Storm", EType.LEAF, 90, 24)),
        EType.FLAME to listOf(Move("Ember Snap", EType.FLAME, 40, 1), Move("Cinder Claw", EType.FLAME, 60, 9), Move("Kiln Blast", EType.FLAME, 90, 24)),
        EType.TIDE to listOf(Move("Bubble Jet", EType.TIDE, 40, 1), Move("Rip Current", EType.TIDE, 60, 9), Move("Mist Mend", EType.TIDE, 0, 15, heal = true), Move("Tidal Crash", EType.TIDE, 90, 24)),
        EType.STONE to listOf(Move("Pebble Toss", EType.STONE, 40, 1), Move("Rockslide", EType.STONE, 65, 10), Move("Quake Slam", EType.STONE, 90, 25)),
        EType.SPARK to listOf(Move("Static Nip", EType.SPARK, 40, 1), Move("Volt Arc", EType.SPARK, 60, 9), Move("Storm Coil", EType.SPARK, 90, 24)),
        EType.GALE to listOf(Move("Gust", EType.GALE, 40, 1), Move("Feather Cut", EType.GALE, 60, 9), Move("Cyclone", EType.GALE, 88, 24)),
        EType.SHADE to listOf(Move("Night Nip", EType.SHADE, 40, 1), Move("Hex Wisp", EType.SHADE, 62, 10), Move("Eclipse", EType.SHADE, 92, 26)),
        EType.FROST to listOf(Move("Frost Bite", EType.FROST, 40, 1), Move("Ice Shard", EType.FROST, 60, 9), Move("Blizzard", EType.FROST, 90, 25)),
        EType.STAR to listOf(Move("Starlight", EType.STAR, 50, 1), Move("Comet Dive", EType.STAR, 75, 10), Move("Supernova", EType.STAR, 110, 30))
    )
}

/** What a creature does for the farm. */
enum class Job(val label: String, val desc: String) {
    WATER("Waterer", "Waters crops each morning. Following you: waters a wider area."),
    GROW("Gardener", "Helps crops grow faster each morning."),
    CLEAR("Clearer", "Clears weeds, sticks and rocks on the farm."),
    MINE("Digger", "Brings stone and ore. Following you: rocks break faster."),
    FORAGE("Forager", "Brings wild forage. Following you: double forage."),
    HARVEST("Harvester", "Harvests ripe crops into the Ranch Box."),
    LUCK("Lucky", "Finds coins and gems. Following you: better drops.")
}

// Body shapes for the procedural sprite generator.
object Body { const val ROUND = 0; const val QUAD = 1; const val BIRD = 2; const val SERPENT = 3; const val TALL = 4 }
object Feat {
    const val EARS = 1; const val HORN = 2; const val LEAF = 4; const val FLAME = 8; const val FIN = 16; const val WINGS = 32
    const val TAIL = 64; const val SPIKES = 128; const val CRYSTAL = 256; const val ANTENNA = 512; const val SHELL = 1024; const val MANE = 2048
    const val FLUFF = 4096; const val STAR = 8192
}

data class Species(
    val id: Int, val name: String, val type: EType, val hp: Int, val atk: Int, val def: Int, val spd: Int,
    val catchRate: Float, val evolveTo: Int, val evolveLevel: Int, val job: Job,
    val body: Int, val c1: Int, val c2: Int, val feats: Int, val size: Float, val dex: String
)

object Dex {
    private fun c(v: Long) = v.toInt()
    val list: List<Species> = listOf(
        Species(0, "Sproutle", EType.LEAF, 45, 49, 49, 45, 0.45f, 1, 16, Job.GROW, Body.ROUND, c(0xFF7ED36B), c(0xFF3E8C3A), Feat.LEAF, 0.75f, "A sprout grows on its head. It naps in sunny furrows."),
        Species(1, "Bloomhorn", EType.LEAF, 62, 62, 63, 60, 0.3f, 2, 32, Job.GROW, Body.QUAD, c(0xFF6CC45A), c(0xFFE07AB0), Feat.LEAF or Feat.HORN, 0.9f, "Its horn blossoms each spring. Fields it walks bloom early."),
        Species(2, "Verdantaur", EType.LEAF, 82, 84, 85, 70, 0.1f, -1, 0, Job.GROW, Body.QUAD, c(0xFF4FA845), c(0xFFF2D34A), Feat.LEAF or Feat.HORN or Feat.MANE, 1.05f, "A great grove-beast. Old farmers call it the harvest king."),
        Species(3, "Emberkit", EType.FLAME, 39, 52, 43, 65, 0.45f, 4, 16, Job.CLEAR, Body.QUAD, c(0xFFF07A3A), c(0xFFFFD34A), Feat.EARS or Feat.FLAME, 0.75f, "Its tail-flame flickers when it is happy. Loves burning brush."),
        Species(4, "Cindervix", EType.FLAME, 58, 64, 58, 80, 0.3f, 5, 32, Job.CLEAR, Body.QUAD, c(0xFFE35A2A), c(0xFFFFC23A), Feat.EARS or Feat.FLAME or Feat.TAIL, 0.9f, "Leaves smoldering pawprints. Clears a field in an afternoon."),
        Species(5, "Pyrolynx", EType.FLAME, 78, 84, 78, 100, 0.1f, -1, 0, Job.CLEAR, Body.QUAD, c(0xFFC8401E), c(0xFFFFE07A), Feat.EARS or Feat.FLAME or Feat.MANE, 1.05f, "Its mane burns white-hot. Wildfires bow before it."),
        Species(6, "Puddlit", EType.TIDE, 44, 48, 65, 43, 0.45f, 7, 16, Job.WATER, Body.ROUND, c(0xFF5AA8F0), c(0xFFBFE4FF), Feat.FIN, 0.75f, "Hops between puddles. Sprays crops when it sneezes."),
        Species(7, "Torrentoad", EType.TIDE, 59, 63, 80, 58, 0.3f, 8, 32, Job.WATER, Body.ROUND, c(0xFF3F86D6), c(0xFF9AD6FF), Feat.FIN or Feat.SPIKES, 0.95f, "Can water a whole field with one mighty croak."),
        Species(8, "Tsunamaw", EType.TIDE, 79, 83, 100, 78, 0.1f, -1, 0, Job.WATER, Body.SERPENT, c(0xFF2A62B8), c(0xFF8FE0F0), Feat.FIN or Feat.HORN, 1.1f, "Rides the tide in from the deep. Its roar brings rain."),
        Species(9, "Pebblet", EType.STONE, 40, 55, 70, 30, 0.5f, 10, 20, Job.MINE, Body.ROUND, c(0xFFA89A86), c(0xFF7A6E60), Feat.SPIKES, 0.7f, "Rolls through mine tunnels and nibbles on gravel."),
        Species(10, "Bouldrum", EType.STONE, 75, 95, 110, 35, 0.2f, -1, 0, Job.MINE, Body.TALL, c(0xFF8E7E68), c(0xFFD9824A), Feat.SPIKES or Feat.CRYSTAL, 1.05f, "Drums its stone fists to crack open ore veins."),
        Species(11, "Zappip", EType.SPARK, 35, 55, 40, 90, 0.45f, 12, 22, Job.HARVEST, Body.QUAD, c(0xFFF5D547), c(0xFF4A3A2A), Feat.EARS or Feat.TAIL, 0.7f, "Quick little paws. Picks berries faster than you can blink."),
        Species(12, "Voltail", EType.SPARK, 60, 85, 55, 110, 0.2f, -1, 0, Job.HARVEST, Body.QUAD, c(0xFFF2C12E), c(0xFF5A3A9A), Feat.EARS or Feat.TAIL or Feat.ANTENNA, 0.95f, "Its tail stores lightning from summer storms."),
        Species(13, "Fluffinch", EType.GALE, 40, 45, 40, 70, 0.5f, 14, 18, Job.FORAGE, Body.BIRD, c(0xFFF2E6C8), c(0xFF8FC6E8), Feat.WINGS or Feat.FLUFF, 0.65f, "A puffball bird that brings shiny things home."),
        Species(14, "Galewing", EType.GALE, 70, 75, 65, 100, 0.25f, -1, 0, Job.FORAGE, Body.BIRD, c(0xFF8FC6E8), c(0xFFF2F2F2), Feat.WINGS or Feat.TAIL, 1.0f, "Scouts the valley from above and returns with treasures."),
        Species(15, "Burrowbun", EType.WILD, 45, 50, 45, 60, 0.55f, 16, 18, Job.HARVEST, Body.QUAD, c(0xFFD9B88A), c(0xFFF4E4CC), Feat.EARS, 0.65f, "Lives under hedgerows. Loves to help at harvest time."),
        Species(16, "Thumperoo", EType.WILD, 75, 80, 65, 85, 0.25f, -1, 0, Job.HARVEST, Body.TALL, c(0xFFC49A6A), c(0xFFF4E4CC), Feat.EARS or Feat.TAIL, 1.0f, "Stomps so hard the ripe fruit falls right into the basket."),
        Species(17, "Duskit", EType.SHADE, 40, 55, 40, 70, 0.4f, 18, 24, Job.LUCK, Body.QUAD, c(0xFF4A3A6E), c(0xFFF5D547), Feat.EARS or Feat.TAIL, 0.7f, "Only seen after dark. Its eyes glow like lanterns."),
        Species(18, "Umbrawl", EType.SHADE, 70, 90, 65, 95, 0.15f, -1, 0, Job.LUCK, Body.QUAD, c(0xFF2E2448), c(0xFFB46BE0), Feat.EARS or Feat.MANE or Feat.TAIL, 1.0f, "A shadow beast that hoards lost coins."),
        Species(19, "Frostling", EType.FROST, 45, 45, 55, 50, 0.45f, 20, 28, Job.WATER, Body.ROUND, c(0xFFD6F2FF), c(0xFF7FC8E8), Feat.CRYSTAL, 0.7f, "Melts a little each morning to water the plants nearby."),
        Species(20, "Glacieron", EType.FROST, 80, 85, 95, 55, 0.12f, -1, 0, Job.WATER, Body.TALL, c(0xFFAEE0F5), c(0xFF4A8EB8), Feat.CRYSTAL or Feat.SPIKES, 1.1f, "A walking glacier. Its footsteps leave fresh springs."),
        Species(21, "Budbug", EType.LEAF, 38, 40, 45, 50, 0.6f, 22, 14, Job.GROW, Body.ROUND, c(0xFF9AD65A), c(0xFFF2A0C8), Feat.ANTENNA, 0.55f, "Snoozes inside flower buds and pollinates them."),
        Species(22, "Petalwing", EType.LEAF, 60, 60, 55, 85, 0.3f, -1, 0, Job.GROW, Body.BIRD, c(0xFFF2A0C8), c(0xFF9AD65A), Feat.WINGS or Feat.ANTENNA, 0.85f, "Its wingbeats scatter pollen across whole fields."),
        Species(23, "Shellby", EType.TIDE, 45, 50, 75, 30, 0.5f, 24, 22, Job.FORAGE, Body.ROUND, c(0xFFF2B49A), c(0xFF7AC4D9), Feat.SHELL, 0.7f, "Combs the beach for shells to decorate its own."),
        Species(24, "Crustadon", EType.TIDE, 75, 85, 110, 40, 0.18f, -1, 0, Job.FORAGE, Body.QUAD, c(0xFFE07A5A), c(0xFF5AA8C8), Feat.SHELL or Feat.SPIKES, 1.05f, "An armored beach giant. Brings treasures from the reef."),
        Species(25, "Gemmit", EType.STONE, 50, 50, 90, 80, 0.08f, -1, 0, Job.LUCK, Body.ROUND, c(0xFFB46BE0), c(0xFFE8FBFF), Feat.CRYSTAL or Feat.STAR, 0.65f, "Rare. Its body is a living geode full of gems."),
        Species(26, "Breezel", EType.GALE, 42, 50, 40, 80, 0.45f, 27, 26, Job.CLEAR, Body.SERPENT, c(0xFFBFEFE6), c(0xFF5AC4B4), Feat.TAIL or Feat.FLUFF, 0.7f, "Whirls through the grass, sweeping up debris."),
        Species(27, "Cyclonix", EType.GALE, 72, 82, 62, 110, 0.15f, -1, 0, Job.CLEAR, Body.SERPENT, c(0xFF5AC4B4), c(0xFFF2F2F2), Feat.TAIL or Feat.WINGS, 1.05f, "A living whirlwind. Clears a field in one spin."),
        Species(28, "Wispurr", EType.SHADE, 50, 65, 45, 95, 0.2f, -1, 0, Job.LUCK, Body.ROUND, c(0xFF8E7CC3), c(0xFFE8E0FF), Feat.EARS or Feat.FLUFF, 0.65f, "A ghostly cat. Purrs near buried treasure."),
        Species(29, "Snowpup", EType.FROST, 45, 55, 45, 65, 0.45f, 30, 25, Job.MINE, Body.QUAD, c(0xFFF4F8FC), c(0xFF7FB8E0), Feat.EARS or Feat.FLUFF, 0.7f, "Digs snow tunnels and finds ore underneath."),
        Species(30, "Blizzhound", EType.FROST, 75, 90, 70, 90, 0.15f, -1, 0, Job.MINE, Body.QUAD, c(0xFFDDEEF8), c(0xFF3F7BB8), Feat.EARS or Feat.MANE or Feat.SPIKES, 1.05f, "Its howl freezes cave walls so they crumble."),
        Species(31, "Magmole", EType.FLAME, 50, 60, 55, 40, 0.45f, 32, 24, Job.MINE, Body.ROUND, c(0xFF8A4A3A), c(0xFFF06A2A), Feat.SPIKES, 0.7f, "Tunnels through warm rock deep in the mines."),
        Species(32, "Volcamole", EType.FLAME, 80, 95, 80, 45, 0.15f, -1, 0, Job.MINE, Body.TALL, c(0xFF6E3A2E), c(0xFFFF8A2A), Feat.SPIKES or Feat.FLAME, 1.05f, "Its back is a tiny volcano. Smelts ore it digs up."),
        Species(33, "Woolamb", EType.WILD, 55, 45, 55, 40, 0.55f, 34, 20, Job.HARVEST, Body.QUAD, c(0xFFF4F0E6), c(0xFF5A4A3A), Feat.FLUFF, 0.7f, "So fluffy it gets stuck in hedges. Gentle and helpful."),
        Species(34, "Ramble", EType.WILD, 80, 80, 80, 55, 0.2f, -1, 0, Job.HARVEST, Body.QUAD, c(0xFFE6DCC8), c(0xFF8A6A4A), Feat.FLUFF or Feat.HORN, 1.0f, "Headbutts trees so fruit tumbles down for the farmer."),
        Species(35, "Astralynx", EType.STAR, 100, 110, 100, 110, 0.03f, -1, 0, Job.LUCK, Body.QUAD, c(0xFF2A2A5A), c(0xFFFFE07A), Feat.EARS or Feat.STAR or Feat.MANE or Feat.TAIL, 1.15f, "Said to fall from the sky into the deepest cave once an age.")
    )

    operator fun get(id: Int) = list[id]
    val starters = intArrayOf(0, 3, 6)
}
