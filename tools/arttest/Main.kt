import android.graphics.Bitmap
import com.jondude.stardustranch.game.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class Sheet(val w: Int, val h: Int) {
    val img = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    fun fill(c: Int) { for (y in 0 until h) for (x in 0 until w) img.setRGB(x, y, c) }
    fun draw(b: Bitmap, ox: Int, oy: Int, s: Int = 1) {
        for (y in 0 until b.height) for (x in 0 until b.width) {
            val c = b.getPixel(x, y); val al = c ushr 24; if (al == 0) continue
            for (j in 0 until s) for (i in 0 until s) {
                val X = ox + x * s + i; val Y = oy + y * s + j; if (X !in 0 until w || Y !in 0 until h) continue
                if (al == 255) { img.setRGB(X, Y, c); continue }
                val d = img.getRGB(X, Y); val t = al / 255f
                fun ch(sh: Int) = (((c shr sh) and 255) * t + ((d shr sh) and 255) * (1 - t)).toInt()
                img.setRGB(X, Y, (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0))
            }
        }
    }
    fun save(p: String) = ImageIO.write(img, "png", File(p))
}

fun renderArea(art: Art, a: Area, season: Int, x0: Int, y0: Int, tw: Int, th: Int, s: Int, out: String, g: Game? = null) {
    val T = 16
    val sh = Sheet(tw * T * s, th * T * s); sh.fill(0xFF000000.toInt())
    for (y in y0 until y0 + th) for (x in x0 until x0 + tw) {
        if (!a.inb(x, y)) continue
        val v = (x * 7 + y * 13) and 3
        val b = when (a.ground[a.idx(x, y)]) {
            Gr.GRASS -> art.grass[season][v]; Gr.DIRT -> art.dirt[v]; Gr.TILLED -> if (a.watered[a.idx(x,y)]) art.tilledWet else art.tilledDry
            Gr.WATER -> art.water[0]; Gr.OCEAN -> art.ocean[0]; Gr.SAND -> art.sand[v]; Gr.CAVE -> art.cave[v]; Gr.WOOD -> art.woodFloor
            Gr.WALL -> art.caveWall; Gr.COBBLE -> art.cobble[v]; Gr.CLIFF -> art.cliff; Gr.INWALL -> art.inWall; Gr.BRIDGE -> art.bridge[0]
            else -> null
        }
        if (b != null) sh.draw(b, (x - x0) * T * s, (y - y0) * T * s, s)
    }
    for (y in y0 until y0 + th + 6) {
        for (x in x0 until x0 + tw) {
            if (!a.inb(x, y)) continue
            val i = a.idx(x, y)
            a.crops[i]?.let { val b = art.crop(it.type, it.stage); sh.draw(b, (x - x0) * T * s, ((y - y0 + 1) * T - b.height) * s, s) }
            val o = a.obj[i]
            val b: Bitmap? = when (o) {
                Ob.TREE -> art.tree[season]; Ob.PINE -> art.pine[season]; Ob.ROCK -> art.rock; Ob.BOULDER -> art.boulder
                Ob.COPPER -> art.copperRock; Ob.IRON -> art.ironRock; Ob.GOLD -> art.goldRock; Ob.GEM -> art.gemRock; Ob.COALROCK -> art.coalRock
                Ob.WEED -> art.weed[season]; Ob.STICK -> art.stick; Ob.BUSH -> art.bush[season]; Ob.TALLGRASS -> art.tallGrass[season]
                Ob.FORAGE -> art.item(a.aux[i]); Ob.FENCE -> art.fence; Ob.BIN -> art.bin; Ob.BOX -> art.box; Ob.COUNTER -> art.counter
                Ob.HEALER -> art.healer; Ob.ELEVATOR -> art.elevator; Ob.LADDER -> art.ladder; Ob.LADDERUP -> art.ladderUp
                Ob.FLOWER -> art.flower[a.aux[i] % 3]; Ob.TABLE -> art.table; Ob.ANVIL -> art.anvil; Ob.SHELF -> art.shelf; Ob.CHEST -> art.chest
                Ob.BED -> if (a.o(x, y - 1) == Ob.BED) null else art.bed
                Ob.FOUNTAIN -> if (a.o(x - 1, y) == Ob.FOUNTAIN || a.o(x, y + 1) == Ob.FOUNTAIN) null else art.fountain[0]
                else -> null
            }
            if (b != null) {
                val ox = (x - x0) * T + (T - b.width) / 2
                val oy = when (o) { Ob.BED -> (y - y0) * T; Ob.FOUNTAIN -> (y - y0 - 1) * T; else -> (y - y0 + 1) * T - b.height }
                sh.draw(b, (if (o == Ob.BED || o == Ob.FOUNTAIN) (x - x0) * T else ox) * s, oy * s, s)
            }
        }
        for (bd in a.buildings) if (bd.y + bd.th - 1 == y) {
            val bm = if (bd.type == Bld.COTTAGE) art.cottage(bd.x % 3) else art.buildings[bd.type]!!
            sh.draw(bm, ((bd.x - x0) * T + (bd.tw * T - bm.width) / 2) * s, ((bd.y + bd.th - y0) * T - bm.height) * s, s)
        }
        if (g != null) {
            for (w in a.wilds) if (w.y.toInt() == y) sh.draw(art.mon(w.mon.speciesId)[0], ((w.x - x0) * T - 16).toInt() * s, ((w.y - y0) * T - 30).toInt() * s, s)
            for (n in g.npcs) if (n.area == a.id && n.y.toInt() == y) sh.draw(art.npcFrames[n.key]!![0][0], ((n.x - x0) * T - 8).toInt() * s, ((n.y - y0) * T - 22).toInt() * s, s)
            if (g.cur === a && g.py.toInt() == y) {
                sh.draw(art.player[0][0], ((g.px - x0) * T - 8).toInt() * s, ((g.py - y0) * T - 22).toInt() * s, s)
                g.party.firstOrNull()?.let { sh.draw(art.mon(it.speciesId)[0], ((g.px - 1.2f - x0) * T - 16).toInt() * s, ((g.py - y0) * T - 30).toInt() * s, s) }
            }
        }
    }
    sh.save(out)
}

fun main(args: Array<String>) {
    val out = args[0]
    val art = Art()
    // creature sheet
    val cs = Sheet(12 * 34 * 3, 3 * 34 * 3); cs.fill(0xFFE8F0D8.toInt())
    for (sp in Dex.list) cs.draw(art.mon(sp.id)[0], (sp.id % 12) * 34 * 3, (sp.id / 12) * 34 * 3, 3)
    cs.save("$out/creatures.png")
    // items + crops + people sheet
    val ids = Items.all.keys.sorted()
    val isz = 18 * 3
    val is_ = Sheet(16 * isz, ((ids.size + 15) / 16 + 6) * isz + 4 * 26 * 3); is_.fill(0xFFF6E7C4.toInt())
    for ((k, id) in ids.withIndex()) is_.draw(art.item(id), (k % 16) * isz, (k / 16) * isz, 3)
    var oy = ((ids.size + 15) / 16) * isz + 10
    for (cI in Crops.list.indices) for (st in 0..4) { val b = art.crop(cI, st); is_.draw(b, (cI % 3 * 5 + st) * isz, oy + (cI / 3) * 34 * 3 - (b.height - 16) * 3 + 16 * 3, 3) }
    oy += 5 * 34 * 3 + 10
    val people = listOf(art.player) + art.npcFrames.values
    for ((pi, fr) in people.withIndex()) for (d in 0..3) is_.draw(fr[d][1], (pi * 4 + d) * 17 * 3, oy, 3)
    is_.save("$out/items.png")
    // buildings
    val bs = Sheet(1400, 420); bs.fill(0xFF6CB84A.toInt()); var bx = 4
    for (t in listOf(Bld.HOUSE, Bld.BARN, Bld.STORE, Bld.CENTER)) { val b = art.buildings[t]!!; bs.draw(b, bx, 210 - b.height * 2, 2); bx += b.width * 2 + 8 }
    bx = 4
    for (b in listOf(art.buildings[Bld.SMITHY]!!, art.buildings[Bld.CAVE]!!, art.cottage(0), art.cottage(1), art.cottage(2))) { bs.draw(b, bx, 416 - b.height * 2, 2); bx += b.width * 2 + 8 }
    bs.save("$out/buildings.png")

    // nature sheet: trees/pines/bushes/weeds per season + stones
    val ns = Sheet(4 * 52 * 3 + 4 * 36 * 3, 84 * 3 + 20 * 3); ns.fill(0xFF6CB84A.toInt())
    for (s in 0..3) { ns.draw(art.tree[s], s * 52 * 3, 2 * 3, 3); ns.draw(art.pine[s], (4 * 52 + s * 36) * 3, 18 * 3, 3) }
    val small = listOf(art.rock, art.copperRock, art.ironRock, art.goldRock, art.gemRock, art.coalRock, art.boulder, art.stick) +
        (0..3).map { art.bush[it] } + (0..3).map { art.weed[it] }
    for ((k, b) in small.withIndex()) ns.draw(b, k * 20 * 3, 84 * 3, 3)
    ns.save("$out/nature.png")

    // game sim
    val f = File("$out/save.json"); f.delete()
    val g = Game(f)
    check(g.mode == Mode.STARTER)
    g.chooseStarter(0)
    println("mode after starter=${g.mode} dialog pages=${g.dialog?.pages?.size} party=${g.party.map { it.name + ":" + it.sp.name }}")
    g.dialog = null; g.mode = Mode.PLAY
    // walk out of house to the door (down)
    var steps = 0
    while (g.cur.id == "house" && steps < 400) { if (g.px < 6.4f) g.update(0.05f, 1f, 0f) else if (g.px > 6.6f) g.update(0.05f, -1f, 0f) else g.update(0.05f, 0f, 1f); steps++ }
    println("after walking: area=${g.cur.id} pos=${g.px},${g.py}")
    // till + plant + water on farm at (12,13)
    g.px = 12.5f; g.py = 12.5f; g.dir = 0
    g.sel = g.inv.slots.indexOfFirst { it?.id == Tool.HOE }; g.action()
    println("tilled? ground=${g.cur.g(12,13)} energy=${g.energy}")
    g.sel = g.inv.slots.indexOfFirst { it?.id == Items.seed(0) }; g.action()
    println("crop planted=${g.cur.crops[g.cur.idx(12,13)]?.def?.name}")
    g.sel = g.inv.slots.indexOfFirst { it?.id == Tool.CAN }; g.action()
    println("watered=${g.cur.watered[g.cur.idx(12,13)]}")
    for (d in 0 until 5) { g.sel = g.inv.slots.indexOfFirst { it?.id == Tool.CAN }; val farm = g.areas["farm"]!!; g.cur = farm; g.px = 12.5f; g.py = 12.5f; g.dir = 0; g.action(); g.endDay(false); g.mode = Mode.PLAY }
    val farm = g.areas["farm"]!!
    println("after 5 days crop ripe=${farm.crops[farm.idx(12,13)]?.ripe} day=${g.day} report=${g.report}")
    g.cur = farm; g.px = 12.5f; g.py = 12.5f; g.dir = 0; g.action()
    println("turnips=${g.inv.count(Items.produce(0))}")
    // battle sim in forest
    g.warp("forest", 23.5f, 3.5f, 0)
    println("forest wilds=${g.cur.wilds.size} species=${g.cur.wilds.map { it.mon.sp.name + it.mon.level }}")
    val w = g.cur.wilds.firstOrNull()
    if (w != null) {
        g.startBattle(w)
        val b = g.battle!!
        var turns = 0
        while (!b.over && turns < 60) {
            while (b.msgs.isNotEmpty()) println("  > " + b.msgs.removeFirst())
            if (b.over) break
            if (b.wild.hp < b.wild.maxHp / 2 && g.inv.count(Items.ORB) > 0) b.throwOrb(Items.ORB) else b.chooseMove(b.active.moves().first { !it.heal })
            turns++
        }
        while (b.msgs.isNotEmpty()) println("  > " + b.msgs.removeFirst())
        g.endBattle()
    }
    println("party now=${g.party.map { it.name + " " + it.sp.name + " Lv" + it.level }}  mode=${g.mode}")
    // mines
    g.enterMine(1)
    println("mine rocks=${g.cur.obj.count { it == Ob.ROCK }} gloops=${g.cur.gloops.size} wild=${g.cur.wilds.size}")
    renderArea(art, g.cur, 0, 0, 0, 30, 22, 2, "$out/mine.png", g)
    g.enterMine(45); renderArea(art, g.cur, 0, 0, 0, 30, 22, 2, "$out/mine45.png", g)
    // save/load roundtrip
    g.save()
    val g2 = Game(f)
    println("loaded: day=${g2.day} gold=${g2.gold} party=${g2.party.size} mode=${g2.mode} crops=${g2.areas["farm"]!!.crops.size}")
    // renders
    renderArea(art, farm, 0, 0, 0, 48, 36, 2, "$out/farm.png")
    val town = g.areas["town"]!!
    g.cur = town
    renderArea(art, town, 0, 0, 0, 48, 36, 2, "$out/town.png", g)
    renderArea(art, g.areas["forest"]!!, 2, 0, 0, 50, 42, 2, "$out/forest_fall.png")
    renderArea(art, g.areas["beach"]!!, 1, 0, 0, 44, 28, 2, "$out/beach.png", g)
    renderArea(art, g.areas["mountain"]!!, 3, 0, 0, 42, 30, 2, "$out/mountain_winter.png")
    renderArea(art, g.areas["house"]!!, 0, 0, 0, 12, 9, 4, "$out/house.png")
    renderArea(art, g.areas["store"]!!, 0, 0, 0, 12, 9, 4, "$out/store.png", g)
    // close-up near the house at gameplay zoom
    g.cur = farm; g.px = 12.5f; g.py = 10.5f
    renderArea(art, farm, 0, 4, 2, 20, 12, 5, "$out/farm_close.png", g)
    println("DONE")
}
