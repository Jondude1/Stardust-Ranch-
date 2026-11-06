package com.jondude.stardustranch.game

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

object Gr {
    const val GRASS = 0; const val DIRT = 1; const val TILLED = 2; const val WATER = 3; const val SAND = 4
    const val CAVE = 5; const val WOOD = 6; const val WALL = 7; const val COBBLE = 8; const val VOID = 9
    const val CLIFF = 10; const val OCEAN = 11; const val INWALL = 12; const val BRIDGE = 13
    fun solid(g: Int) = g == WATER || g == WALL || g == VOID || g == CLIFF || g == OCEAN || g == INWALL
    fun isWater(g: Int) = g == WATER || g == OCEAN
}

object Ob {
    const val NONE = 0; const val TREE = 1; const val PINE = 2; const val ROCK = 3; const val COPPER = 4; const val IRON = 5
    const val GOLD = 6; const val GEM = 7; const val WEED = 8; const val STICK = 9; const val BUSH = 10; const val TALLGRASS = 11
    const val FORAGE = 12; const val FENCE = 13; const val BLOCK = 14; const val BED = 15; const val BIN = 16; const val BOX = 17
    const val COUNTER = 19; const val HEALER = 20; const val ELEVATOR = 21; const val LADDER = 22; const val LADDERUP = 23
    const val SPRINKLER = 24; const val QSPRINKLER = 25; const val FLOWER = 26; const val TABLE = 27; const val BOULDER = 28
    const val ANVIL = 29; const val FOUNTAIN = 30; const val SHELF = 31; const val CHEST = 32; const val COALROCK = 33
    const val GOODS = 34 // ranch goods lying in the pen; aux = item id
    fun walkable(o: Int) = o == NONE || o == TALLGRASS || o == FORAGE || o == FLOWER || o == LADDER || o == LADDERUP || o == GOODS
}

object Bld { const val HOUSE = 0; const val BARN = 1; const val STORE = 2; const val CENTER = 3; const val COTTAGE = 4; const val SMITHY = 5; const val CAVE = 6 }

class Crop(val type: Int) {
    var days = 0
    val def: CropDef get() = Crops.list[type]
    val ripe: Boolean get() = days >= def.days
    /** 0 seed .. 4 ripe */
    val stage: Int get() = if (ripe) 4 else (days * 4 / def.days).coerceIn(0, 3)
}

data class Warp(val x: Int, val y: Int, val w: Int, val h: Int, val to: String, val tx: Int, val ty: Int, val dir: Int)
data class Building(val type: Int, val x: Int, val y: Int, val tw: Int, val th: Int, val doorX: Int)

class WildMon(val mon: Mon, var x: Float, var y: Float) {
    var tx = x; var ty = y; var timer = 0f; var flip = false; var anim = 0f; var cooldown = 0f
}

class Gloop(var x: Float, var y: Float, var hp: Int, val color: Int) {
    var hurt = 0f; var anim = 0f; var vx = 0f; var vy = 0f
}

class Area(val id: String, val w: Int, val h: Int) {
    val ground = IntArray(w * h)
    val obj = IntArray(w * h)
    val aux = IntArray(w * h)
    val watered = BooleanArray(w * h)
    val crops = HashMap<Int, Crop>()
    val buildings = ArrayList<Building>()
    val warps = ArrayList<Warp>()
    var spawns = intArrayOf()
    var nightSpawns = intArrayOf()
    var minLv = 2; var maxLv = 5
    var maxWild = 0
    var indoor = false
    var dark = false
    var mineFloor = 0
    var beach = false
    var forageSpots = 0
    val wilds = ArrayList<WildMon>()
    val gloops = ArrayList<Gloop>()
    val name: String get() = when {
        mineFloor > 0 -> "Mines - Floor $mineFloor"
        else -> id.replaceFirstChar { it.uppercase() }
    }

    fun idx(x: Int, y: Int) = y * w + x
    fun inb(x: Int, y: Int) = x in 0 until w && y in 0 until h
    fun g(x: Int, y: Int) = if (inb(x, y)) ground[idx(x, y)] else Gr.VOID
    fun o(x: Int, y: Int) = if (inb(x, y)) obj[idx(x, y)] else Ob.BLOCK
    fun solid(x: Int, y: Int): Boolean = !inb(x, y) || Gr.solid(ground[idx(x, y)]) || !Ob.walkable(obj[idx(x, y)])
    fun setO(x: Int, y: Int, o: Int, a: Int = 0) { if (inb(x, y)) { obj[idx(x, y)] = o; aux[idx(x, y)] = a } }
    fun setG(x: Int, y: Int, gr: Int) { if (inb(x, y)) ground[idx(x, y)] = gr }
    fun fillG(x: Int, y: Int, rw: Int, rh: Int, gr: Int) { for (yy in y until y + rh) for (xx in x until x + rw) setG(xx, yy, gr) }
    fun fillO(x: Int, y: Int, rw: Int, rh: Int, o: Int) { for (yy in y until y + rh) for (xx in x until x + rw) setO(xx, yy, o) }

    fun addBuilding(type: Int, x: Int, y: Int, tw: Int, th: Int, doorX: Int, warpTo: String?, tx: Int, ty: Int) {
        buildings.add(Building(type, x, y, tw, th, doorX))
        fillO(x, y, tw, th, Ob.BLOCK)
        if (warpTo != null) {
            setO(x + doorX, y + th - 1, Ob.NONE)
            warps.add(Warp(x + doorX, y + th - 1, 1, 1, warpTo, tx, ty, 1))
        }
    }

    fun forageCount(): Int = obj.count { it == Ob.FORAGE }
}

object World {

    fun makeFarm(rng: Random): Area {
        val a = Area("farm", 48, 36)
        a.fillG(0, 0, a.w, a.h, Gr.GRASS)
        border(a, Ob.TREE)
        // exits
        a.fillO(46, 16, 2, 4, Ob.NONE); a.warps.add(Warp(47, 16, 1, 4, "town", 1, 18, 3))
        a.fillO(21, 34, 4, 2, Ob.NONE); a.warps.add(Warp(21, 35, 4, 1, "forest", 23, 1, 0))
        // paths
        a.fillG(11, 9, 2, 9, Gr.DIRT); a.fillG(11, 17, 37, 2, Gr.DIRT); a.fillG(22, 17, 2, 19, Gr.DIRT)
        // pond
        for (y in 22..31) for (x in 34..44) if (hypot((x - 39.5) / 5.5, (y - 26.5) / 4.5) < 1.0) a.setG(x, y, Gr.WATER)
        // debris everywhere else
        for (y in 2 until a.h - 2) for (x in 2 until a.w - 2) {
            if (a.g(x, y) != Gr.GRASS || a.o(x, y) != Ob.NONE) continue
            if (x in 6..17 && y in 2..16) continue      // yard + starter field
            if (x in 26..43 && y in 1..17) continue     // barn and pen
            val r = rng.nextFloat()
            when {
                r < 0.10f -> a.setO(x, y, Ob.WEED)
                r < 0.15f -> a.setO(x, y, Ob.STICK)
                r < 0.20f -> a.setO(x, y, Ob.ROCK, 1)
                r < 0.23f -> a.setO(x, y, Ob.TREE, 6)
                r < 0.235f -> a.setO(x, y, Ob.BOULDER, 6)
                r < 0.27f -> a.setO(x, y, Ob.TALLGRASS)
            }
        }
        a.addBuilding(Bld.HOUSE, 8, 4, 6, 5, 3, "house", 6, 7)
        a.setO(15, 8, Ob.BIN)
        a.addBuilding(Bld.BARN, 30, 2, 6, 5, 3, null, 0, 0)
        a.setO(37, 6, Ob.BOX)
        // pen
        for (x in 28..41) { a.setO(x, 9, Ob.FENCE); a.setO(x, 16, Ob.FENCE) }
        for (y in 9..16) { a.setO(28, y, Ob.FENCE); a.setO(41, y, Ob.FENCE) }
        a.setO(33, 9, Ob.NONE); a.setO(34, 16, Ob.NONE)
        for (i in 0 until 6) a.setO(7 + i * 2, 3, Ob.FLOWER, rng.nextInt(3))
        return a
    }

    fun makeHouse(): Area {
        val a = Area("house", 12, 9); a.indoor = true
        a.fillG(0, 0, 12, 9, Gr.WOOD)
        a.fillG(0, 0, 12, 2, Gr.INWALL)
        for (y in 0 until 9) { a.setG(0, y, Gr.INWALL); a.setG(11, y, Gr.INWALL) }
        a.fillG(0, 8, 12, 1, Gr.INWALL); a.setG(6, 8, Gr.WOOD)
        a.setO(2, 2, Ob.BED); a.setO(2, 3, Ob.BED)
        a.setO(8, 3, Ob.TABLE); a.setO(9, 2, Ob.SHELF); a.setO(5, 2, Ob.SHELF)
        a.warps.add(Warp(6, 8, 1, 1, "farm", 11, 9, 0))
        return a
    }

    fun makeTown(rng: Random): Area {
        val a = Area("town", 48, 36)
        a.fillG(0, 0, a.w, a.h, Gr.GRASS)
        border(a, Ob.TREE)
        a.fillG(14, 14, 21, 9, Gr.COBBLE)
        a.fillG(0, 17, 15, 3, Gr.COBBLE)
        a.fillG(23, 0, 3, 15, Gr.COBBLE)
        a.fillG(23, 22, 3, 14, Gr.COBBLE)
        a.fillG(11, 12, 2, 3, Gr.COBBLE); a.fillG(33, 12, 2, 3, Gr.COBBLE)
        a.fillG(26, 6, 6, 2, Gr.COBBLE)
        a.fillO(0, 17, 2, 3, Ob.NONE); a.warps.add(Warp(0, 17, 1, 3, "farm", 46, 18, 2))
        a.fillO(23, 0, 3, 2, Ob.NONE); a.warps.add(Warp(23, 0, 3, 1, "mountain", 20, 27, 1))
        a.fillO(23, 34, 3, 2, Ob.NONE); a.warps.add(Warp(23, 35, 3, 1, "beach", 20, 1, 0))
        a.addBuilding(Bld.STORE, 8, 8, 6, 5, 3, "store", 6, 7)
        a.addBuilding(Bld.CENTER, 30, 8, 6, 5, 3, "center", 6, 7)
        a.addBuilding(Bld.SMITHY, 32, 2, 5, 4, 2, null, 0, 0)
        a.setO(31, 6, Ob.ANVIL)
        a.addBuilding(Bld.COTTAGE, 5, 25, 4, 4, 1, null, 0, 0)
        a.addBuilding(Bld.COTTAGE, 38, 25, 4, 4, 2, null, 0, 0)
        a.addBuilding(Bld.COTTAGE, 14, 27, 4, 4, 1, null, 0, 0)
        a.fillO(18, 16, 2, 2, Ob.FOUNTAIN)
        for (i in 0 until 40) {
            val x = rng.nextInt(2, a.w - 2); val y = rng.nextInt(2, a.h - 2)
            if (a.g(x, y) == Gr.GRASS && a.o(x, y) == Ob.NONE) a.setO(x, y, if (rng.nextFloat() < 0.6f) Ob.FLOWER else Ob.BUSH, rng.nextInt(3))
        }
        for (i in 0 until 14) {
            val x = rng.nextInt(2, a.w - 2); val y = rng.nextInt(2, a.h - 2)
            if (a.g(x, y) == Gr.GRASS && a.o(x, y) == Ob.NONE && a.o(x, y + 1) == Ob.NONE) a.setO(x, y, Ob.TREE, 99)
        }
        return a
    }

    fun makeStore(): Area {
        val a = interior("store", "town", 11, 13)
        for (x in 3..8) a.setO(x, 4, Ob.COUNTER)
        for (x in 2..9) if (x != 6) a.setO(x, 2, Ob.SHELF)
        a.setO(1, 5, Ob.SHELF); a.setO(10, 5, Ob.SHELF)
        return a
    }

    fun makeCenter(): Area {
        val a = interior("center", "town", 33, 13)
        a.setO(6, 3, Ob.HEALER)
        a.setO(2, 2, Ob.SHELF); a.setO(9, 2, Ob.SHELF); a.setO(9, 5, Ob.TABLE)
        return a
    }

    private fun interior(id: String, back: String, bx: Int, by: Int): Area {
        val a = Area(id, 12, 9); a.indoor = true
        a.fillG(0, 0, 12, 9, Gr.WOOD)
        a.fillG(0, 0, 12, 2, Gr.INWALL)
        for (y in 0 until 9) { a.setG(0, y, Gr.INWALL); a.setG(11, y, Gr.INWALL) }
        a.fillG(0, 8, 12, 1, Gr.INWALL); a.setG(6, 8, Gr.WOOD)
        a.warps.add(Warp(6, 8, 1, 1, back, bx, by, 0))
        return a
    }

    fun makeForest(rng: Random): Area {
        val a = Area("forest", 50, 42)
        a.fillG(0, 0, a.w, a.h, Gr.GRASS)
        // river
        var ry = 24.0
        for (x in 0 until a.w) {
            ry += rng.nextDouble(-0.6, 0.6); ry = ry.coerceIn(21.0, 28.0)
            for (dy in 0..2) a.setG(x, ry.toInt() + dy, Gr.WATER)
        }
        for (y in 0 until a.h) if (Gr.isWater(a.g(24, y))) { a.setG(23, y, Gr.BRIDGE); a.setG(24, y, Gr.BRIDGE); a.setG(25, y, Gr.BRIDGE) }
        // winding path
        var px = 24.0
        for (y in 0 until a.h) {
            px += rng.nextDouble(-0.5, 0.5); px = px.coerceIn(20.0, 28.0)
            for (dx in -1..1) if (!Gr.isWater(a.g(px.toInt() + dx, y)) && a.g(px.toInt() + dx, y) != Gr.BRIDGE) a.setG(px.toInt() + dx, y, Gr.DIRT)
        }
        // groves
        repeat(30) {
            val cx = rng.nextInt(a.w); val cy = rng.nextInt(a.h); val r = rng.nextInt(2, 6)
            for (y in cy - r..cy + r) for (x in cx - r..cx + r)
                if (hypot((x - cx).toDouble(), (y - cy).toDouble()) <= r && a.g(x, y) == Gr.GRASS && rng.nextFloat() < 0.55f)
                    a.setO(x, y, if (rng.nextBoolean()) Ob.PINE else Ob.TREE, 6)
        }
        // tall grass meadows
        repeat(14) {
            val cx = rng.nextInt(a.w); val cy = rng.nextInt(a.h); val r = rng.nextInt(2, 5)
            for (y in cy - r..cy + r) for (x in cx - r..cx + r)
                if (hypot((x - cx).toDouble(), (y - cy).toDouble()) <= r && a.g(x, y) == Gr.GRASS && a.o(x, y) == Ob.NONE)
                    a.setO(x, y, Ob.TALLGRASS)
        }
        for (i in 0 until 120) {
            val x = rng.nextInt(a.w); val y = rng.nextInt(a.h)
            if (a.g(x, y) == Gr.GRASS && a.o(x, y) == Ob.NONE) a.setO(x, y, listOf(Ob.BUSH, Ob.STICK, Ob.WEED, Ob.FLOWER, Ob.ROCK)[rng.nextInt(5)], 1)
        }
        border(a, Ob.PINE)
        a.fillO(22, 0, 5, 3, Ob.NONE); a.fillG(22, 0, 5, 3, Gr.DIRT)
        a.warps.add(Warp(22, 0, 5, 1, "farm", 23, 33, 1))
        a.spawns = intArrayOf(21, 21, 15, 15, 13, 13, 33, 33, 26, 0)
        a.nightSpawns = intArrayOf(17, 17, 28, 21, 15)
        a.minLv = 2; a.maxLv = 8; a.maxWild = 9; a.forageSpots = 12
        return a
    }

    fun makeBeach(rng: Random): Area {
        val a = Area("beach", 44, 28); a.beach = true
        a.fillG(0, 0, a.w, a.h, Gr.SAND)
        for (x in 0 until a.w) {
            val shore = 17 + (Math.sin(x * 0.35) * 1.5).toInt()
            for (y in shore until a.h) a.setG(x, y, Gr.OCEAN)
        }
        a.fillG(10, 12, 2, 13, Gr.BRIDGE)
        a.fillG(30, 14, 4, 3, Gr.WATER)
        for (x in 0 until a.w) { a.setO(x, 0, Ob.PINE); a.setO(x, 1, if (rng.nextFloat() < 0.5f) Ob.BUSH else Ob.PINE) }
        for (y in 0 until 18) { a.setO(0, y, Ob.PINE); a.setO(a.w - 1, y, Ob.PINE) }
        a.fillO(19, 0, 3, 2, Ob.NONE)
        a.warps.add(Warp(19, 0, 3, 1, "town", 24, 34, 1))
        for (i in 0 until 30) {
            val x = rng.nextInt(2, a.w - 2); val y = rng.nextInt(2, 8)
            if (a.o(x, y) == Ob.NONE) a.setO(x, y, Ob.TALLGRASS)
        }
        a.spawns = intArrayOf(23, 23, 23, 13, 26, 6)
        a.nightSpawns = intArrayOf(23, 28, 6)
        a.minLv = 4; a.maxLv = 10; a.maxWild = 6; a.forageSpots = 8
        return a
    }

    fun makeMountain(rng: Random): Area {
        val a = Area("mountain", 42, 30)
        a.fillG(0, 0, a.w, a.h, Gr.GRASS)
        a.fillG(0, 0, a.w, 3, Gr.CLIFF)
        for (x in 0 until a.w) if (rng.nextFloat() < 0.5f) a.setG(x, 3, Gr.CLIFF)
        for (y in 9..15) for (x in 4..13) if (hypot((x - 8.5) / 5.0, (y - 12.0) / 3.5) < 1.0) a.setG(x, y, Gr.WATER)
        a.fillG(19, 5, 3, 25, Gr.DIRT)
        border(a, Ob.PINE)
        a.addBuilding(Bld.CAVE, 18, 2, 5, 4, 2, null, 0, 0)
        a.setO(20, 5, Ob.NONE)
        a.warps.add(Warp(20, 5, 1, 1, "mine", 0, 0, 0))
        a.setO(23, 6, Ob.ELEVATOR)
        a.fillO(19, 28, 3, 2, Ob.NONE)
        a.warps.add(Warp(19, 29, 3, 1, "town", 24, 1, 0))
        for (i in 0 until 90) {
            val x = rng.nextInt(2, a.w - 2); val y = rng.nextInt(5, a.h - 2)
            if (a.g(x, y) == Gr.GRASS && a.o(x, y) == Ob.NONE)
                a.setO(x, y, when (rng.nextInt(6)) { 0, 1 -> Ob.ROCK; 2 -> Ob.BOULDER; 3 -> Ob.PINE; else -> Ob.TALLGRASS }, if (rng.nextInt(6) == 2) 6 else 1)
        }
        repeat(6) {
            val cx = rng.nextInt(26, 39); val cy = rng.nextInt(8, 26); val r = rng.nextInt(2, 4)
            for (y in cy - r..cy + r) for (x in cx - r..cx + r)
                if (a.g(x, y) == Gr.GRASS && a.o(x, y) == Ob.NONE) a.setO(x, y, Ob.TALLGRASS)
        }
        a.spawns = intArrayOf(9, 9, 11, 11, 15, 29, 3)
        a.nightSpawns = intArrayOf(17, 9, 11)
        a.minLv = 5; a.maxLv = 12; a.maxWild = 6; a.forageSpots = 4
        return a
    }

    fun makeMine(floor: Int, rng: Random): Area {
        val a = Area("mine", 30, 22)
        a.dark = true; a.mineFloor = floor
        a.fillG(0, 0, a.w, a.h, Gr.WALL)
        // cellular cave
        val open = BooleanArray(a.w * a.h)
        for (y in 2 until a.h - 2) for (x in 2 until a.w - 2) open[a.idx(x, y)] = rng.nextFloat() < 0.62f
        repeat(4) {
            val copy = open.copyOf()
            for (y in 2 until a.h - 2) for (x in 2 until a.w - 2) {
                var n = 0
                for (dy in -1..1) for (dx in -1..1) if (copy[a.idx(x + dx, y + dy)]) n++
                open[a.idx(x, y)] = n >= 5
            }
        }
        // carve a guaranteed spine so everything connects
        var cx = 3; var cy = a.h / 2
        while (cx < a.w - 3) {
            for (dy in -1..1) open[a.idx(cx, (cy + dy).coerceIn(2, a.h - 3))] = true
            cx++
            if (rng.nextFloat() < 0.4f) cy = (cy + rng.nextInt(-1, 2)).coerceIn(4, a.h - 5)
        }
        for (y in 2 until a.h - 2) for (x in 2 until a.w - 2) if (open[a.idx(x, y)]) a.setG(x, y, Gr.CAVE)
        // keep only the region connected to the start
        val sx = 3; val sy = a.h / 2
        val seen = BooleanArray(a.w * a.h)
        val q = ArrayDeque<Int>(); q.add(a.idx(sx, sy)); seen[a.idx(sx, sy)] = true
        while (q.isNotEmpty()) {
            val i = q.removeFirst(); val x = i % a.w; val y = i / a.w
            for ((dx, dy) in listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)) {
                val nx = x + dx; val ny = y + dy
                if (a.inb(nx, ny) && !seen[a.idx(nx, ny)] && a.g(nx, ny) == Gr.CAVE) { seen[a.idx(nx, ny)] = true; q.add(a.idx(nx, ny)) }
            }
        }
        for (i in 0 until a.w * a.h) if (a.ground[i] == Gr.CAVE && !seen[i]) a.ground[i] = Gr.WALL
        a.setO(sx, sy, Ob.LADDERUP)
        // rocks and ore
        for (y in 0 until a.h) for (x in 0 until a.w) {
            if (a.g(x, y) != Gr.CAVE || a.o(x, y) != Ob.NONE) continue
            if (abs(x - sx) + abs(y - sy) < 3) continue
            val r = rng.nextFloat()
            if (r < 0.38f) {
                val o = rng.nextFloat()
                val type = when {
                    o < 0.025f -> Ob.GEM
                    floor >= 60 && o < 0.10f -> Ob.GOLD
                    floor >= 30 && o < 0.16f -> Ob.IRON
                    floor >= 20 && o < 0.19f -> Ob.COALROCK
                    o < 0.22f -> Ob.COPPER
                    else -> Ob.ROCK
                }
                a.setO(x, y, type, 1 + floor / 30)
            }
        }
        if (floor % 10 == 0) {
            // treasure room floor
            val spots = (0 until a.w * a.h).filter { a.ground[it] == Gr.CAVE && a.obj[it] == Ob.NONE && abs(it % a.w - sx) > 6 }
            if (spots.isNotEmpty()) { val s = spots[rng.nextInt(spots.size)]; a.obj[s] = Ob.CHEST }
        }
        a.minLv = 5 + floor / 3; a.maxLv = 9 + floor / 3
        a.spawns = when {
            floor < 20 -> intArrayOf(9, 9, 11, 15, 31)
            floor < 40 -> intArrayOf(9, 31, 31, 29, 19, 11)
            floor < 70 -> intArrayOf(19, 29, 31, 17, 28, 9)
            else -> intArrayOf(10, 32, 20, 18, 30, 28)
        }
        a.maxWild = 1 + rng.nextInt(2)
        val gloopColor = when { floor < 30 -> 0xFF6FD45A.toInt(); floor < 60 -> 0xFF5AB4E0.toInt(); floor < 90 -> 0xFFB45AE0.toInt(); else -> 0xFFE0445A.toInt() }
        val nG = (1 + floor / 8).coerceAtMost(8)
        val floors = (0 until a.w * a.h).filter { a.ground[it] == Gr.CAVE && a.obj[it] == Ob.NONE && abs(it % a.w - sx) > 5 }
        repeat(nG) {
            if (floors.isNotEmpty()) {
                val s = floors[rng.nextInt(floors.size)]
                a.gloops.add(Gloop(s % a.w + 0.5f, s / a.w + 0.5f, 3 + floor / 10, gloopColor))
            }
        }
        return a
    }

    private fun border(a: Area, o: Int) {
        for (x in 0 until a.w) { a.setO(x, 0, o, 99); a.setO(x, 1, o, 99); a.setO(x, a.h - 1, o, 99); a.setO(x, a.h - 2, o, 99) }
        for (y in 0 until a.h) { a.setO(0, y, o, 99); a.setO(1, y, o, 99); a.setO(a.w - 1, y, o, 99); a.setO(a.w - 2, y, o, 99) }
    }
}
