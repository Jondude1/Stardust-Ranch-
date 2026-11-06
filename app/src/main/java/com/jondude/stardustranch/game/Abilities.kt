package com.jondude.stardustranch.game

/**
 * Overworld creature abilities. Any healthy creature in your party lends its type's field ability:
 *  - CUT    (Leaf, Gale): slices through thorn brambles.
 *  - SMASH  (Stone): breaks cracked crags that no pickaxe can dent.
 *  - CROSS  (Tide, Frost): carries you across a pond, lake or river to the far bank.
 *  - GLOW   (Flame, Spark, Star): lights up the mines and shows which rock hides the ladder down.
 * Each gates a hidden spot that restocks every morning (brambles and crags grow back overnight):
 * Thornwood Hollow (forest, south-west), the Crag Nook (mountain, north-east) and Lake Isle (mountain lake).
 */
enum class Field(val label: String, val verb: String, val types: Set<EType>, val desc: String) {
    CUT("Cut", "cuts through the thorns", setOf(EType.LEAF, EType.GALE), "Leaf and Gale creatures cut through thorn brambles."),
    SMASH("Smash", "smashes the crag apart", setOf(EType.STONE), "Stone creatures smash cracked crags."),
    CROSS("Cross", "carries you across the water", setOf(EType.TIDE, EType.FROST), "Tide and Frost creatures carry you across ponds, lakes and rivers."),
    GLOW("Glow", "lights up the cave", setOf(EType.FLAME, EType.SPARK, EType.STAR), "Flame, Spark and Star creatures light the mines and reveal the rock hiding the ladder.");

    companion object {
        fun of(t: EType): List<Field> = values().filter { t in it.types }
        /** Toast colour for each ability. */
        fun color(f: Field): Int = when (f) { CUT -> 0xFF8CE07A.toInt(); SMASH -> 0xFFE8C08A.toInt(); CROSS -> 0xFF9AD6FF.toInt(); GLOW -> 0xFFFFE07A.toInt() }
    }
}

object Abilities {
    /** Longest stretch of water a creature will carry you over. */
    const val MAX_CROSS = 8

    /** Forest: Thornwood Hollow, ringed by brambles. (x, y, w, h) of the ring; the inside is (x+1, y+1, w-2, h-2). */
    val HOLLOW = intArrayOf(3, 31, 9, 7)
    /** Mountain: the Crag Nook, an alcove walled by cliff and closed by one crag. */
    val NOOK = intArrayOf(30, 4, 7, 5)
    val NOOK_CRAG = 33 to 8
    /** Mountain lake island tiles. */
    val ISLE = intArrayOf(7, 11, 3, 2)

    /** Cache chests in the secret spots (area id to tile). */
    val caches = mapOf("forest" to (7 to 33), "mountain-nook" to (35 to 5), "mountain-isle" to (8 to 11))

    fun carveForest(a: Area) {
        val (x, y, w, h) = HOLLOW.toList()
        for (yy in y until y + h) for (xx in x until x + w) {
            a.setG(xx, yy, Gr.GRASS)
            val edge = xx == x || yy == y || xx == x + w - 1 || yy == y + h - 1
            a.setO(xx, yy, if (edge) Ob.THORN else Ob.NONE)
        }
        // a dirt trail from the main forest path to the east side of the ring
        for (yy in y + 2..y + 7) for (xx in x + w..21) if (a.o(xx, yy) == Ob.TREE || a.o(xx, yy) == Ob.PINE) a.setO(xx, yy, Ob.NONE)
        for (xx in x + w..24) { a.setO(xx, y + 4, Ob.NONE); if (a.g(xx, y + 4) != Gr.BRIDGE && !Gr.isWater(a.g(xx, y + 4))) a.setG(xx, y + 4, Gr.DIRT) }
        // no tall trees right below the ring hiding its south side
        for (yy in y + h..minOf(y + h + 1, a.h - 3)) for (xx in x..x + w - 1) if (a.o(xx, yy) == Ob.TREE || a.o(xx, yy) == Ob.PINE) a.setO(xx, yy, Ob.NONE)
        // a few flowers and the old cache
        a.setO(x + 2, y + 4, Ob.FLOWER, 2); a.setO(x + 6, y + 5, Ob.FLOWER, 0)
        val (cx, cy) = caches["forest"]!!
        a.setO(cx, cy, Ob.CHEST)
        // keep a walkable apron around the ring so the brambles can be reached from every side
        for (yy in y - 1..y + h) for (xx in x - 1..x + w) {
            if (xx == x - 1 || yy == y - 1 || xx == x + w || yy == y + h) {
                if (a.inb(xx, yy) && xx >= 2 && yy >= 2 && yy < a.h - 2) { a.setO(xx, yy, Ob.NONE); if (Gr.solid(a.g(xx, yy))) a.setG(xx, yy, Gr.GRASS) }
            }
        }
    }

    fun carveMountain(a: Area) {
        // Crag Nook: cliff walls on three sides, a crag in the open south side
        val (x, y, w, h) = NOOK.toList()
        for (yy in y until y + h) for (xx in x until x + w) {
            val wall = xx == x || xx == x + w - 1 || yy == y + h - 1
            a.setG(xx, yy, if (wall) Gr.CLIFF else Gr.DIRT)
            a.setO(xx, yy, Ob.NONE)
        }
        for (xx in x until x + w) a.setG(xx, y - 1, Gr.CLIFF)
        val (cx, cy) = NOOK_CRAG
        a.setG(cx, cy, Gr.DIRT); a.setO(cx, cy, Ob.CRAG, 0)
        // the path up to it, and a trail west to the mine road
        for (yy in cy + 1..cy + 2) for (xx in cx - 1..cx + 1) { a.setO(xx, yy, Ob.NONE); if (a.g(xx, yy) != Gr.DIRT) a.setG(xx, yy, Gr.GRASS) }
        for (xx in 21..cx) { a.setO(xx, cy + 2, Ob.NONE); a.setG(xx, cy + 2, Gr.DIRT) }
        a.setO(x + 1, y + 1, Ob.GEM, 3); a.setO(x + 2, y + 1, Ob.IRON, 3); a.setO(x + 4, y + 2, Ob.GOLD, 3)
        a.setO(x + 1, y + 3, Ob.COALROCK, 2)
        val (nx, ny) = caches["mountain-nook"]!!
        a.setO(nx, ny, Ob.CHEST)
        // Lake Isle in the mountain lake
        val (ix, iy, iw, ih) = ISLE.toList()
        for (yy in iy until iy + ih) for (xx in ix until ix + iw) { a.setG(xx, yy, Gr.GRASS); a.setO(xx, yy, Ob.NONE) }
        val (sx, sy) = caches["mountain-isle"]!!
        a.setO(sx, sy, Ob.CHEST)
        a.setO(ix + 2, iy + 1, Ob.FLOWER, 1)
    }

    /** Loot from a field-ability cache (restocks daily). Returns item pairs and gold. */
    fun cacheLoot(key: String, season: Int, roll: (Int) -> Int): Pair<List<Pair<Int, Int>>, Int> = when (key) {
        "forest" -> {
            val pool = Items.forageFor(season, false)
            listOf(pool[roll(pool.size)] to 2, Items.FIBER to 3, Items.SUPER_ORB to 1) to 80
        }
        "mountain-nook" -> listOf(Items.COAL to 3, (if (roll(4) == 0) Items.RUBY else Items.AMETHYST) to 1) to 120
        "mountain-isle" -> listOf(Items.goodsFor(EType.TIDE) to 1, Items.ORB to 3, Items.QUARTZ to 1) to 100
        else -> emptyList<Pair<Int, Int>>() to 0
    }

    /** Which cache a chest at (x, y) in area [id] is, or null for a mine chest. */
    fun cacheAt(id: String, x: Int, y: Int): String? = caches.entries.firstOrNull { (k, v) -> k.substringBefore('-') == id && v.first == x && v.second == y }?.key

    /**
     * The far bank when crossing from (x, y) in direction (dx, dy): the first non-water tile within [MAX_CROSS]
     * steps that you can stand on, or null if there is none (open sea, a wall, or no water ahead).
     */
    fun crossTarget(a: Area, x: Int, y: Int, dx: Int, dy: Int): Pair<Int, Int>? {
        if (!Gr.isWater(a.g(x + dx, y + dy))) return null
        for (k in 1..MAX_CROSS + 1) {
            val tx = x + dx * k; val ty = y + dy * k
            if (!a.inb(tx, ty)) return null
            val gr = a.g(tx, ty)
            if (Gr.isWater(gr)) { if (gr == Gr.OCEAN) return null; continue }
            return if (!a.solid(tx, ty)) tx to ty else null
        }
        return null
    }
}
