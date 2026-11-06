package com.jondude.stardustranch.game

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class Slot(var id: Int, var n: Int)

class Inventory(val size: Int) {
    val slots = arrayOfNulls<Slot>(size)

    /** Returns how many could NOT be added. */
    fun add(id: Int, n: Int): Int {
        var left = n
        val def = Items[id]
        if (def.stackable) for (s in slots) if (s != null && s.id == id) { s.n += left; return 0 }
        for (i in slots.indices) if (slots[i] == null && left > 0) {
            if (def.stackable) { slots[i] = Slot(id, left); return 0 }
            slots[i] = Slot(id, 1); left--
        }
        return left
    }
    fun count(id: Int) = slots.sumOf { if (it != null && it.id == id) it.n else 0 }
    fun remove(id: Int, n: Int): Boolean {
        if (count(id) < n) return false
        var left = n
        for (i in slots.indices) {
            val s = slots[i] ?: continue
            if (s.id != id) continue
            val take = min(left, s.n); s.n -= take; left -= take
            if (s.n <= 0) slots[i] = null
            if (left == 0) break
        }
        return true
    }
    fun removeAt(i: Int, n: Int) { val s = slots[i] ?: return; s.n -= n; if (s.n <= 0) slots[i] = null }
    fun isEmpty() = slots.all { it == null }
    fun toJson(): JSONArray = JSONArray().apply { for (s in slots) put(if (s == null) JSONObject.NULL else JSONObject().put("i", s.id).put("n", s.n)) }
    fun load(a: JSONArray) { for (i in 0 until min(size, a.length())) { val o = a.optJSONObject(i); slots[i] = if (o == null) null else Slot(o.getInt("i"), o.getInt("n")) } }
}

class Npc(
    val key: String, val name: String, val area: String, var x: Float, var y: Float,
    val loves: Set<Int>, val likes: Set<Int>, val lines: List<String>, val gift: Pair<Int, Int>
) { var dir = 0; var timer = 0f; val hx = x; val hy = y }

class Toast(val text: String, var x: Float, var y: Float, var t: Float, val color: Int)

class Dialog(val speaker: String?, val pages: List<String>, val options: List<Pair<String, () -> Unit>> = emptyList(), val onClose: (() -> Unit)? = null) {
    var page = 0
}

class PenMon(val mon: Mon, var x: Float, var y: Float) { var tx = x; var ty = y; var t = 0f; var flip = false }

enum class Mode { PLAY, DIALOG, MENU, SHOP, SMITH, RANCH, ELEVATOR, BATTLE, STARTER, REPORT, FISH, COOK }

/** One cast of the fishing rod: wait for a bite, hook it, then the reel dial minigame. */
class FishingState(val loc: Int, val bx: Int, val by: Int) {
    var phase = 0           // 0 waiting for a bite, 1 biting (tap to hook), 2 reeling
    var t = 0f; var anim = 0f
    var fish: FishDef? = null
    var angle = 0f; var speed = 0f; var spin = 1f   // needle, degrees (0 = 3 o'clock, clockwise)
    var zone = 0f; var zoneW = 0f                    // green catch arc centre and width, degrees
    var hits = 0; var need = 3; var misses = 0
    var flash = 0f; var lastHit = false
    fun inZone(): Boolean { val d = ((angle - zone) % 360f + 540f) % 360f - 180f; return abs(d) <= zoneW / 2f }
}

class Game(val saveFile: File) {
    val rng = Random(System.nanoTime())
    var seed = rng.nextInt()

    // world
    val areas = HashMap<String, Area>()
    lateinit var cur: Area
    var px = 11.5f; var py = 10.5f; var dir = 0; var walkT = 0f; var moving = false
    val trail = ArrayDeque<Pair<Float, Float>>()
    var fx = px; var fy = py; var fflip = false
    var hurtT = 0f
    var swingT = 0f; var swingTool = -1
    var mineFloor = 0

    // time
    var day = 1; var season = 0; var year = 1
    var time = 360f // minutes since midnight
    var raining = false; var rainTomorrow = false
    var clockAcc = 0f

    // player
    var gold = 500
    var maxEnergy = 270; var energy = 270f
    var maxHp = 100; var hp = 100
    var water = 40
    val inv = Inventory(24)
    var sel = 0
    val toolLevel = IntArray(7)
    val party = ArrayList<Mon>()
    val ranch = ArrayList<Mon>()
    val ranchBox = Inventory(36)
    val dexSeen = HashSet<Int>()
    val dexCaught = HashSet<Int>()
    var deepest = 0
    var astralCaught = false
    val shipped = ArrayList<Slot>()
    val friendship = HashMap<String, Int>()
    val talkedToday = HashSet<String>()
    val giftedToday = HashSet<String>()
    val heartGifts = HashSet<String>()
    var started = false
    val fishCaught = HashMap<Int, Int>()
    var fishIntro = false
    var goodsIntro = false
    // cooking
    var buff: Buff? = null; var buffUntil = 0f
    var cookTip = false      // morning-report announcement shown
    var cookIntro = false    // first-use explanation at the stove shown
    var dishesCooked = 0
    var fishing: FishingState? = null

    // ui
    var mode = Mode.STARTER
    var dialog: Dialog? = null
    var battle: Battle? = null
    var menuTab = 0
    var menuSel = -1
    var shopSell = false
    var report: List<String> = emptyList()
    val toasts = ArrayList<Toast>()
    var banner: String? = null; var bannerT = 0f
    val npcs = ArrayList<Npc>()
    val pen = ArrayList<PenMon>()
    var frame = 0f

    val waterMax: Int get() = 40 + toolLevel[Tool.CAN] * 25
    val lead: Mon? get() = party.firstOrNull { !it.fainted }
    val leadJob: Job? get() = party.firstOrNull()?.let { if (it.fainted) null else it.sp.job }
    val held: Slot? get() = inv.slots[sel]

    init {
        buildWorld()
        if (!load()) newGame()
    }

    // ======================================================================
    // Setup
    // ======================================================================

    private fun buildWorld() {
        val r = Random(seed)
        areas["farm"] = World.makeFarm(r)
        areas["house"] = World.makeHouse()
        rebuildPublicAreas()
        cur = areas["farm"]!!
        npcs.clear()
        npcs.add(Npc("hollis", "Mayor Hollis", "town", 26.5f, 19.5f, setOf(Items.produce(8), Items.produce(12), Items.DIAMOND), setOf(Items.produce(2), Items.produce(6), 54),
            listOf("Welcome to Stardust Valley! Your grandmother's ranch has been empty far too long.",
                "Creatures used to help on every farm around here. It's good to see that coming back.",
                "If you find anything shiny in the mines, I'm always happy to see it.",
                "The forest to the south of your farm is full of friendly little creatures.",
                "A good farmer waters every morning. A great one has creatures do it!"), Items.QSPRINKLER to 2))
        npcs.add(Npc("pia", "Pia", "store", 5.5f, 3.5f, setOf(Items.produce(3), 53, Items.AMETHYST), setOf(51, Items.produce(5), Items.produce(9)),
            listOf("Seeds change with the seasons, so check back often!", "Capture Orbs are cheaper by the dozen. Well, they would be if I gave discounts.",
                "Did you know creatures love helping? Leave some at your ranch and they'll work while you sleep.",
                "Strawberries keep producing after the first harvest. Very good value!"), Items.SUPER_ORB to 5))
        npcs.add(Npc("wren", "Ranger Wren", "center", 3.5f, 4.5f, setOf(Items.produce(5), 61, Items.RUBY), setOf(50, 52, 55),
            listOf("Weaken a wild creature before throwing an orb. It works much better.",
                "Type matters! Leaf beats Tide, Tide beats Flame, Flame beats Leaf.",
                "Each creature has a farm job. Waterers, gardeners, diggers, foragers... find a team you like.",
                "The creature at the front of your party follows you and lends a hand.",
                "Some creatures only come out at night. Try the forest after 8pm.",
                "The fountain here will heal your whole party for free, any time.",
                "Creatures living in your ranch pen make little goods overnight when they're happy. The stronger your bond, the more you'll find."), Items.STAR_ORB to 1))
        npcs.add(Npc("gus", "Gus", "town", 30.5f, 7.5f, setOf(Items.GOLD, Items.produce(7)), setOf(Items.COPPER, Items.IRON, Items.COAL),
            listOf("Bring me ore and coin and I'll make your tools sing.", "Copper first, then iron, then gold. That's the way of the forge.",
                "The mines go a hundred floors deep. Nobody I know has seen the bottom.",
                "Gloops are harmless alone. In groups they'll wear you down."), Items.IRON to 10))
        npcs.add(Npc("mira", "Mira", "beach", 30.5f, 8.5f, setOf(60, 59, Items.produce(6), 122, 124), setOf(58, Items.produce(4), 120, 121),
            listOf("The tide brings new shells every morning.", "Shellby love to dig in the sand right around here.",
                "Have you ever seen a Tsunamaw? My grandfather swore one brought the rain.",
                "Beach creatures get feisty in the evening.",
                "The sea fish change with the seasons. Lantern Eels only come out on autumn and winter nights.",
                "When it storms, Storm Rays rise right up to the pier. Worth getting wet for!"), 60 to 3))
    }

    private fun rebuildPublicAreas() {
        val r = Random(seed + 7)
        areas["town"] = World.makeTown(r)
        areas["store"] = World.makeStore()
        areas["center"] = World.makeCenter()
        areas["forest"] = World.makeForest(r)
        areas["beach"] = World.makeBeach(r)
        areas["mountain"] = World.makeMountain(r)
        for (id in listOf("forest", "beach", "mountain")) refreshForage(areas[id]!!)
    }

    private fun newGame() {
        inv.add(Tool.HOE, 1); inv.add(Tool.CAN, 1); inv.add(Tool.PICK, 1); inv.add(Tool.AXE, 1)
        inv.add(Tool.SCYTHE, 1); inv.add(Tool.SWORD, 1); inv.add(Items.seed(0), 15); inv.add(Items.ORB, 5)
        cur = areas["house"]!!; px = 3.5f; py = 4.5f; dir = 0
        mode = Mode.STARTER
    }

    fun chooseStarter(id: Int) {
        val m = Mon(id, 5, Mon.randomNick(rng)); m.bond = 30
        party.add(m); dexSeen.add(id); dexCaught.add(id)
        started = true
        mode = Mode.PLAY
        say("Ranger Wren", "Welcome to Stardust Ranch! I've brought ${m.nick} the ${m.sp.name} to help you get started.",
            "${m.nick} will follow you and help with chores. Its job is ${m.sp.job.label}: ${m.sp.job.desc}",
            "Wild creatures roam the forest, the beach, the mountain and the mines. Battle them, then toss a Capture Orb!",
            "Creatures you leave at your ranch pen will work the farm every morning while you sleep.",
            "Clear some space, plant those turnip seeds, and water them every day. Sleep in your bed to end the day. Good luck!")
        save()
    }

    // ======================================================================
    // Messages
    // ======================================================================

    fun say(speaker: String?, vararg pages: String, options: List<Pair<String, () -> Unit>> = emptyList(), onClose: (() -> Unit)? = null) {
        dialog = Dialog(speaker, pages.toList(), options, onClose); mode = Mode.DIALOG
    }

    fun toast(text: String, color: Int = 0xFFFFFFFF.toInt()) { toasts.add(Toast(text, px, py - 1f, 1.4f, color)) }
    fun note(text: String) { banner = text; bannerT = 2.6f }

    fun give(id: Int, n: Int, silent: Boolean = false): Boolean {
        if (n <= 0) return true
        val left = inv.add(id, n)
        if (!silent && n - left > 0) toast("+${n - left} ${Items[id].name}")
        if (left > 0) { ranchBox.add(id, left); note("Inventory full - sent ${Items[id].name} to the Ranch Box") }
        return left == 0
    }

    // ======================================================================
    // Update
    // ======================================================================

    fun update(dt: Float, jx: Float, jy: Float) {
        frame += dt
        for (t in toasts) { t.t -= dt; t.y -= dt * 0.8f }
        toasts.removeAll { it.t <= 0 }
        if (bannerT > 0) { bannerT -= dt; if (bannerT <= 0) banner = null }
        battle?.update(dt)
        if (mode == Mode.FISH) { moving = false; if (tickClock(dt)) updateFishing(dt); return }
        if (mode != Mode.PLAY) { moving = false; return }

        if (!tickClock(dt)) return
        hurtT = max(0f, hurtT - dt)
        swingT = max(0f, swingT - dt)

        // movement
        val mag = sqrt(jx * jx + jy * jy)
        moving = mag > 0.15f
        if (moving) {
            var speed = 4.4f
            if (energy <= 0) speed *= 0.6f
            if (leadJob == Job.HARVEST) speed *= 1.1f
            if (buff == Buff.SPEED) speed *= 1.2f
            val nx = jx / max(mag, 1f); val ny = jy / max(mag, 1f)
            dir = if (abs(nx) > abs(ny)) (if (nx > 0) 3 else 2) else (if (ny > 0) 0 else 1)
            tryMove(nx * speed * dt, 0f); tryMove(0f, ny * speed * dt)
            walkT += dt * speed
            val last = trail.lastOrNull()
            if (last == null || hypot(last.first - px, last.second - py) > 0.12f) {
                trail.addLast(px to py); if (trail.size > 40) trail.removeFirst()
            }
            checkWarps()
        }
        // follower
        if (trail.size > 8) {
            val target = trail[max(0, trail.size - 9)]
            val dx = target.first - fx; val dy = target.second - fy
            if (abs(dx) > 0.02f) fflip = dx < 0
            fx += dx * min(1f, dt * 8f); fy += dy * min(1f, dt * 8f)
        }

        updateNpcs(dt)
        updateWilds(dt)
        updateGloops(dt)
        if (cur.id == "farm") updatePen(dt)
    }

    /** Clock: 1 game minute every 0.7 seconds. Returns false if the day ended. */
    private fun tickClock(dt: Float): Boolean {
        clockAcc += dt
        if (clockAcc >= 0.7f) {
            clockAcc -= 0.7f
            time += 1f
            if (buff != null && time >= buffUntil) { note("Your ${buff!!.label} buff wore off."); buff = null }
            if (time.toInt() % 60 == 0 && time >= 1500f && time < 1501f) note("It's getting late... you should head to bed.")
            if (time >= 1560f) { fishing = null; passOut(); return false }
        }
        return true
    }

    private fun tryMove(dx: Float, dy: Float) {
        val nx = px + dx; val ny = py + dy
        val r = 0.28f
        val pts = arrayOf(nx - r to ny - 0.1f, nx + r to ny - 0.1f, nx - r to ny + 0.25f, nx + r to ny + 0.25f)
        for ((x, y) in pts) if (cur.solid(floor(x).toInt(), floor(y).toInt())) return
        for (n in npcs) if (n.area == cur.id && abs(n.x - nx) < 0.6f && abs(n.y - ny) < 0.5f) return
        px = nx; py = ny
    }

    private fun checkWarps() {
        val tx = floor(px).toInt(); val ty = floor(py).toInt()
        for (w in cur.warps) {
            if (tx in w.x until w.x + w.w && ty in w.y until w.y + w.h) {
                // only trigger when walking in the warp's direction for edge exits
                if (w.to == "mine") { enterMine(1); return }
                val offX = if (w.w > 1) tx - w.x else 0; val offY = if (w.h > 1) ty - w.y else 0
                warp(w.to, w.tx + (if (w.w > 1 && w.to != "house") offX - w.w / 2 else 0) + 0.5f,
                    w.ty + (if (w.h > 1) offY - w.h / 2 else 0) + 0.5f, w.dir)
                return
            }
        }
    }

    fun warp(to: String, x: Float, y: Float, d: Int) {
        val a = areas[to] ?: return
        cur = a; px = x; py = y; dir = d
        // nudge onto a free tile
        if (cur.solid(floor(px).toInt(), floor(py).toInt())) {
            loop@ for (r in 1..4) for (oy in -r..r) for (ox in -r..r) if (!cur.solid(floor(px).toInt() + ox, floor(py).toInt() + oy)) { px += ox; py += oy; break@loop }
        }
        trail.clear(); fx = px; fy = py
        if (a.id != "mine") mineFloor = 0
        spawnWildsInitial()
        if (a.id == "farm") buildPen()
        note(a.name)
        if (a.id == "beach" && !fishIntro) {
            fishIntro = true
            say(null, "A hand-painted sign by the pier reads: \"FISHING! Rods for sale - ask Mira.\"",
                "\"Face any water and use the rod to cast. Wait for the float to dip and a ! to appear, then tap to hook the fish.\"",
                "\"Then tap whenever the spinning needle crosses the green. Different fish live in the sea, the forest river, your farm pond and the mountain lake - and they change with the season, the hour and the rain.\"")
        }
    }

    fun enterMine(floorN: Int) {
        mineFloor = floorN
        if (floorN > deepest) deepest = floorN
        val m = World.makeMine(floorN, Random(rng.nextInt()))
        areas["mine"] = m
        cur = m; px = 4.5f; py = m.h / 2 + 0.5f
        trail.clear(); fx = px; fy = py; dir = 3
        spawnWildsInitial()
        if (floorN == 100 && !astralCaught) {
            m.wilds.add(WildMon(Mon(35, 55), 20.5f, m.h / 2 + 0.5f).apply { cooldown = 1f })
            note("Something ancient stirs on this floor...")
        } else note("Mines - Floor $floorN")
    }

    // ======================================================================
    // NPCs, wild creatures, gloops, pen
    // ======================================================================

    private fun updateNpcs(dt: Float) {
        for (n in npcs) {
            if (n.area != cur.id) continue
            n.timer -= dt
            if (n.timer <= 0) {
                n.timer = 2f + rng.nextFloat() * 3f
                if (n.area == "town" || n.area == "beach") {
                    val tx = n.hx + rng.nextInt(-2, 3); val ty = n.hy + rng.nextInt(-1, 2)
                    if (!cur.solid(tx.toInt(), ty.toInt()) && hypot(tx - px, ty - py) > 1f) { n.dir = if (tx > n.x) 3 else if (tx < n.x) 2 else 0; n.x = tx; n.y = ty }
                }
            }
        }
    }

    private fun pickSpecies(a: Area): Int {
        val night = time >= 1200 && a.nightSpawns.isNotEmpty()
        val pool = if (night && rng.nextFloat() < 0.6f) a.nightSpawns else a.spawns
        if (a.mineFloor > 0 && rng.nextFloat() < 0.04f) return 25
        return pool[rng.nextInt(pool.size)]
    }

    private fun spawnTile(a: Area): Pair<Int, Int>? {
        repeat(40) {
            val x = rng.nextInt(a.w); val y = rng.nextInt(a.h)
            if (a.solid(x, y)) return@repeat
            if (hypot(x + 0.5f - px, y + 0.5f - py) < 5f) return@repeat
            val o = a.o(x, y); val g = a.g(x, y)
            val ok = when {
                a.mineFloor > 0 -> g == Gr.CAVE
                a.beach -> g == Gr.SAND
                else -> o == Ob.TALLGRASS
            }
            if (ok) return x to y
        }
        return null
    }

    private fun spawnWild(a: Area) {
        val t = spawnTile(a) ?: return
        val lv = rng.nextInt(a.minLv, a.maxLv + 1)
        a.wilds.add(WildMon(Mon(pickSpecies(a), lv), t.first + 0.5f, t.second + 0.5f))
    }

    private fun spawnWildsInitial() {
        val a = cur
        if (a.mineFloor == 0) a.wilds.clear()
        while (a.wilds.size < a.maxWild) { val before = a.wilds.size; spawnWild(a); if (a.wilds.size == before) break }
    }

    private var spawnAcc = 0f
    private fun updateWilds(dt: Float) {
        val a = cur
        spawnAcc += dt
        if (spawnAcc > 6f) { spawnAcc = 0f; if (a.wilds.size < a.maxWild) spawnWild(a) }
        val it = a.wilds.iterator()
        while (it.hasNext()) {
            val w = it.next()
            w.anim += dt; w.cooldown = max(0f, w.cooldown - dt); w.timer -= dt
            if (w.timer <= 0) {
                w.timer = 1.5f + rng.nextFloat() * 2.5f
                val tx = w.x + rng.nextInt(-3, 4); val ty = w.y + rng.nextInt(-3, 4)
                if (!a.solid(floor(tx).toInt(), floor(ty).toInt())) { w.tx = tx; w.ty = ty }
            }
            val dx = w.tx - w.x; val dy = w.ty - w.y; val d = hypot(dx, dy)
            if (d > 0.05f) {
                val sp = 1.4f * dt
                val nx = w.x + dx / d * min(sp, d); val ny = w.y + dy / d * min(sp, d)
                if (!a.solid(floor(nx).toInt(), floor(ny).toInt())) { w.x = nx; w.y = ny } else { w.tx = w.x; w.ty = w.y }
                w.flip = dx < 0
            }
            if (w.cooldown <= 0 && hypot(w.x - px, w.y - py) < 0.75f) {
                startBattle(w); return
            }
        }
    }

    fun startBattle(w: WildMon) {
        if (party.none { !it.fainted }) {
            w.cooldown = 3f
            note(if (party.isEmpty()) "You need a creature partner to battle!" else "Your creatures are too tired. Heal them first!")
            return
        }
        battle = Battle(this, w.mon, w.mon.speciesId == 35)
        battleSource = w
        mode = Mode.BATTLE
    }
    private var battleSource: WildMon? = null

    fun endBattle() {
        val b = battle ?: return
        val w = battleSource
        battle = null; battleSource = null
        mode = Mode.PLAY
        if (w != null) {
            if (b.caught || b.over && !b.fled && !b.lost) cur.wilds.remove(w) else w.cooldown = 3f
            if (b.caught && w.mon.speciesId == 35) astralCaught = true
            if (!b.caught && !b.fled && !b.lost && w.mon.speciesId == 35) astralCaught = true
        }
        if (b.lost) blackout("Your creatures were overwhelmed...")
        else if (cur.mineFloor > 0 && b.over && !b.fled && rng.nextFloat() < 0.15f) revealLadderNear()
    }

    private fun updateGloops(dt: Float) {
        val a = cur
        for (g in a.gloops) {
            g.anim += dt; g.hurt = max(0f, g.hurt - dt)
            val dx = px - g.x; val dy = py - g.y; val d = hypot(dx, dy)
            var mx = g.vx * dt; var my = g.vy * dt
            g.vx *= 0.9f; g.vy *= 0.9f
            if (d < 7f && d > 0.3f && g.hurt <= 0) { val sp = 1.5f + a.mineFloor / 60f; mx += dx / d * sp * dt; my += dy / d * sp * dt }
            if (!a.solid(floor(g.x + mx).toInt(), floor(g.y).toInt())) g.x += mx
            if (!a.solid(floor(g.x).toInt(), floor(g.y + my).toInt())) g.y += my
            if (d < 0.6f && hurtT <= 0) {
                val dmg = 5 + a.mineFloor / 5
                hp -= dmg; hurtT = 1f
                toast("-$dmg", 0xFFFF6060.toInt())
                g.vx = -dx / max(d, 0.1f) * 4f; g.vy = -dy / max(d, 0.1f) * 4f
                if (hp <= 0) { blackout("You were knocked out in the mines!"); return }
            }
        }
    }

    private fun buildPen() {
        pen.clear()
        for (m in ranch) pen.add(PenMon(m, 29.5f + rng.nextFloat() * 11f, 10.5f + rng.nextFloat() * 5f))
    }

    private fun updatePen(dt: Float) {
        for (p in pen) {
            p.t -= dt
            if (p.t <= 0) { p.t = 2f + rng.nextFloat() * 3f; p.tx = 29.5f + rng.nextFloat() * 11f; p.ty = 10.5f + rng.nextFloat() * 5f }
            val dx = p.tx - p.x; val dy = p.ty - p.y; val d = hypot(dx, dy)
            if (d > 0.05f) { p.x += dx / d * min(d, 1f * dt); p.y += dy / d * min(d, 1f * dt); p.flip = dx < 0 }
        }
    }

    // ======================================================================
    // Actions
    // ======================================================================

    fun facing(): Pair<Int, Int> {
        val tx = floor(px).toInt(); val ty = floor(py).toInt()
        return when (dir) { 0 -> tx to ty + 1; 1 -> tx to ty - 1; 2 -> tx - 1 to ty; else -> tx + 1 to ty }
    }

    private fun dirVec() = when (dir) { 0 -> 0 to 1; 1 -> 0 to -1; 2 -> -1 to 0; else -> 1 to 0 }

    /** The A button. */
    fun action() {
        if (mode != Mode.PLAY) return
        val (fxT, fyT) = facing()
        val (dvx, dvy) = dirVec()
        // NPCs
        for (n in npcs) if (n.area == cur.id && hypot(n.x - (px + dvx * 0.8f), n.y - (py + dvy * 0.8f)) < 1.0f) { talk(n); return }
        // shopkeeper behind counter
        if (cur.id == "store" && cur.o(fxT, fyT) == Ob.COUNTER) { npcs.firstOrNull { it.key == "pia" }?.let { talk(it) }; return }
        // wild creature in front
        for (w in cur.wilds) if (hypot(w.x - (px + dvx * 0.8f), w.y - (py + dvy * 0.8f)) < 0.9f) { startBattle(w); return }
        // objects
        val o = cur.o(fxT, fyT)
        when (o) {
            Ob.BED -> { say(null, "Go to sleep for the night?", options = listOf("Sleep" to { endDay(false) }, "Not yet" to {})); return }
            Ob.BIN -> { shipHeld(); return }
            Ob.BOX -> { collectBox(); return }
            Ob.HEALER -> { for (m in party) m.fullHeal(); say(null, "The fountain glows softly. Your creatures are fully healed!"); return }
            Ob.ELEVATOR -> { mode = Mode.ELEVATOR; return }
            Ob.LADDER -> { enterMine(mineFloor + 1); return }
            Ob.LADDERUP -> { warp("mountain", 20.5f, 6.5f, 0); return }
            Ob.ANVIL -> { mode = Mode.SMITH; return }
            Ob.FORAGE -> { pickForage(fxT, fyT); return }
            Ob.GOODS -> { pickGoods(fxT, fyT); return }
            Ob.CHEST -> { openChest(fxT, fyT); return }
            Ob.FOUNTAIN -> { say(null, "Coins glitter at the bottom of the fountain. Someone wished for rain."); return }
            Ob.STOVE -> { openStove(); return }
            Ob.SHELF -> { say(null, if (cur.id == "house") "Grandma's old farming almanacs. One page is dog-eared: 'Creatures are family, not tools.'" else "Shelves packed with goods."); return }
        }
        if (cur.id == "farm" && fyT == 6 && fxT in 32..34) { mode = Mode.RANCH; menuSel = -1; return }
        // ripe crop
        val crop = if (cur.inb(fxT, fyT)) cur.crops[cur.idx(fxT, fyT)] else null
        if (crop != null && crop.ripe) { harvest(fxT, fyT); return }
        // water tile with can handled by tool
        useHeld()
    }

    private fun talk(n: Npc) {
        n.dir = when { abs(px - n.x) > abs(py - n.y) -> if (px > n.x) 3 else 2; else -> if (py > n.y) 0 else 1 }
        val h = held
        val pts = friendship[n.key] ?: 0
        if (h != null && Items[h.id].kind in setOf(Kind.CROP, Kind.FORAGE, Kind.GEM, Kind.FOOD, Kind.FISH, Kind.GOODS) && n.key !in giftedToday) {
            val add = when { h.id in n.loves -> 80; h.id in n.likes -> 45; else -> 20 }
            val name = Items[h.id].name
            inv.removeAt(sel, 1)
            giftedToday.add(n.key)
            friendship[n.key] = min(2500, pts + add)
            val line = when (add) { 80 -> "Oh, a $name! I love these! Thank you!"; 45 -> "A $name? That's really thoughtful, thanks."; else -> "Thanks for the $name." }
            say(n.name, line, onClose = { checkHeartGift(n) })
            return
        }
        if (n.key !in talkedToday) { talkedToday.add(n.key); friendship[n.key] = min(2500, pts + 20) }
        val line = n.lines[(day + season * 7 + n.key.length) % n.lines.size]
        val opts = when (n.key) {
            "pia" -> listOf("Shop" to { mode = Mode.SHOP; shopSell = false; menuSel = -1 }, "Bye" to {})
            "gus" -> listOf("Upgrade tools" to { mode = Mode.SMITH }, "Bye" to {})
            "wren" -> listOf("Heal my creatures" to { for (m in party) m.fullHeal(); note("Your party is fully healed!") }, "Bye" to {})
            "mira" -> if (!hasRod()) listOf("Buy rod (500g)" to { buyRod() }, "Bye" to {}) else emptyList()
            else -> emptyList()
        }
        say(n.name, line, options = opts, onClose = { checkHeartGift(n) })
    }

    private fun checkHeartGift(n: Npc) {
        val hearts = (friendship[n.key] ?: 0) / 250
        if (hearts >= 3 && n.key !in heartGifts) {
            heartGifts.add(n.key)
            give(n.gift.first, n.gift.second, true)
            say(n.name, "We've really become friends, haven't we? Here, I want you to have this.", "Received ${n.gift.second} ${Items[n.gift.first].name}!")
        }
    }

    private fun shipHeld() {
        val h = held
        if (h == null || Items[h.id].kind == Kind.TOOL) { val total = shipped.sumOf { Items[it.id].sell * it.n }; say(null, "Shipping bin. Hold an item and press Use to ship it.\nShipped today: ${total}g"); return }
        val d = Items[h.id]
        shipped.add(Slot(h.id, h.n))
        toast("Shipped ${h.n} ${d.name}", 0xFFFFE07A.toInt())
        inv.slots[sel] = null
    }

    private fun collectBox() {
        if (ranchBox.isEmpty()) { say(null, "The Ranch Box is empty. Your ranch creatures put what they find in here each morning."); return }
        var moved = 0
        for (i in ranchBox.slots.indices) {
            val s = ranchBox.slots[i] ?: continue
            val left = inv.add(s.id, s.n)
            moved += s.n - left
            if (left == 0) ranchBox.slots[i] = null else s.n = left
        }
        if (moved > 0) toast("Collected $moved items") else note("Your inventory is full!")
    }

    private fun pickForage(x: Int, y: Int) {
        val id = cur.aux[cur.idx(x, y)]
        cur.setO(x, y, Ob.NONE)
        var n = if (leadJob == Job.FORAGE) 2 else 1
        if (buff == Buff.LUCK && rng.nextFloat() < 0.5f) n++
        give(id, n)
        lead?.let { it.bond++ }
    }

    private fun pickGoods(x: Int, y: Int) {
        val id = cur.aux[cur.idx(x, y)]
        cur.setO(x, y, Ob.NONE)
        give(id, 1)
    }

    private fun openChest(x: Int, y: Int) {
        cur.setO(x, y, Ob.NONE)
        val f = cur.mineFloor
        val loot = when {
            f >= 90 -> listOf(Items.DIAMOND to 2, Items.STAR_ORB to 1)
            f >= 60 -> listOf(Items.RUBY to 2, Items.SUPER_ORB to 3)
            f >= 30 -> listOf(Items.AMETHYST to 2, Items.SUPER_ORB to 2)
            else -> listOf(Items.QUARTZ to 2, Items.ORB to 4)
        }
        for ((id, n) in loot) give(id, n)
        gold += f * 25; toast("+${f * 25}g", 0xFFFFE07A.toInt())
    }

    private fun harvest(x: Int, y: Int) {
        val i = cur.idx(x, y); val c = cur.crops[i] ?: return
        val d = c.def
        var n = d.yield
        if (leadJob == Job.HARVEST && rng.nextFloat() < 0.35f) n++
        if (!give(Items.produce(d.idx), n)) {}
        if (d.regrow > 0) c.days = d.days - d.regrow else cur.crops.remove(i)
        lead?.let { it.bond++ }
    }

    fun useHeld() {
        val h = held
        if (h == null) return
        val d = Items[h.id]
        when (d.kind) {
            Kind.TOOL -> useTool(h.id)
            Kind.SEED -> plant(h)
            Kind.FOOD, Kind.CROP, Kind.FORAGE, Kind.FISH, Kind.GOODS -> {
                if (d.energy <= 0 && d.heal <= 0) { note("You can't eat that."); return }
                say(null, "Eat the ${d.name}?", options = listOf("Eat" to { eat(h.id) }, "No" to {}))
            }
            Kind.PLACE -> place(h)
            Kind.ORB -> note("Bump into a wild creature to battle, then throw orbs in battle.")
            else -> note("Hold a tool, seeds or food to use them.")
        }
    }

    private fun eat(id: Int) {
        val d = Items[id]
        if (!inv.remove(id, 1)) return
        energy = min(maxEnergy.toFloat(), energy + d.energy)
        hp = min(maxHp, hp + d.energy / 2)
        if (d.heal > 0) { if (id == Items.SNACK) lead?.heal(d.heal) else for (m in party) m.heal(d.heal) }
        toast("+${d.energy} energy", 0xFF8CE07A.toInt())
        Cooking.byId[id]?.let { dish ->
            buff = dish.buff; buffUntil = time + dish.minutes
            note("${dish.buff.label} for ${dish.minutes / 60}h: ${dish.buff.effect}!")
        }
    }

    private fun spend(cost: Float): Boolean {
        if (energy <= 0f) { note("You're exhausted. Eat something or go to bed."); return false }
        energy = max(0f, energy - (if (buff == Buff.STAMINA) cost * 0.5f else cost)); return true
    }

    private fun useTool(t: Int) {
        if (t == Tool.ROD) { castRod(); return }
        swingT = 0.25f; swingTool = t
        val (fx0, fy0) = facing()
        val (dvx, dvy) = dirVec()
        val lvl = toolLevel[t]
        when (t) {
            Tool.HOE -> {
                if (!spend(2f)) return
                for (k in 0..lvl) {
                    val x = fx0 + dvx * k; val y = fy0 + dvy * k
                    if (!cur.inb(x, y)) continue
                    val o = cur.o(x, y)
                    if (o == Ob.WEED) { cur.setO(x, y, Ob.NONE); continue }
                    if (cur.id != "farm") { if (k == 0) note("You can only till soil on your farm."); continue }
                    val g = cur.g(x, y)
                    if ((g == Gr.GRASS || g == Gr.DIRT) && (o == Ob.NONE || o == Ob.TALLGRASS)) {
                        cur.setO(x, y, Ob.NONE); cur.setG(x, y, Gr.TILLED); if (raining) cur.watered[cur.idx(x, y)] = true
                    }
                }
            }
            Tool.CAN -> {
                if (Gr.isWater(cur.g(fx0, fy0))) { water = waterMax; toast("Refilled!", 0xFF9AD6FF.toInt()); return }
                if (water <= 0) { note("The watering can is empty. Refill at a pond or river."); return }
                val freeWater = leadJob == Job.WATER
                if (!freeWater && !spend(2f)) return
                water--
                val wide = freeWater
                for (k in 0..lvl) for (s in (if (wide) -1..1 else 0..0)) {
                    val x = fx0 + dvx * k + (if (dvx == 0) s else 0); val y = fy0 + dvy * k + (if (dvy == 0) s else 0)
                    if (cur.inb(x, y) && cur.g(x, y) == Gr.TILLED) cur.watered[cur.idx(x, y)] = true
                }
            }
            Tool.PICK -> {
                if (!spend(2f)) return
                val x = fx0; val y = fy0
                val o = cur.o(x, y)
                val i = cur.idx(x, y).coerceAtLeast(0)
                when (o) {
                    Ob.ROCK, Ob.COPPER, Ob.IRON, Ob.GOLD, Ob.GEM, Ob.COALROCK, Ob.BOULDER -> {
                        if (o == Ob.BOULDER && lvl < 1) { note("This boulder is too tough. Upgrade your pickaxe."); return }
                        val power = 1 + lvl + (if (leadJob == Job.MINE) 1 else 0) + (if (buff == Buff.MINING) 1 else 0)
                        cur.aux[i] -= power
                        if (cur.aux[i] <= 0) breakRock(x, y, o)
                    }
                    Ob.WEED, Ob.STICK -> cur.setO(x, y, Ob.NONE)
                    Ob.SPRINKLER, Ob.QSPRINKLER -> { cur.setO(x, y, Ob.NONE); give(if (o == Ob.SPRINKLER) Items.SPRINKLER else Items.QSPRINKLER, 1) }
                    Ob.NONE -> if (cur.g(x, y) == Gr.TILLED && cur.crops[i] == null) { cur.setG(x, y, Gr.DIRT); cur.watered[i] = false }
                }
            }
            Tool.AXE -> {
                if (!spend(2f)) return
                val x = fx0; val y = fy0; val o = cur.o(x, y); val i = cur.idx(x, y).coerceAtLeast(0)
                when (o) {
                    Ob.TREE, Ob.PINE -> {
                        if (cur.aux[i] >= 99) { note("This tree is far too big to chop."); return }
                        cur.aux[i] -= 1 + lvl + (if (leadJob == Job.CLEAR) 1 else 0)
                        if (cur.aux[i] <= 0) { cur.setO(x, y, Ob.NONE); give(Items.WOOD, rng.nextInt(5, 9)); if (rng.nextFloat() < 0.3f) give(Items.FIBER, 2) }
                    }
                    Ob.STICK -> { cur.setO(x, y, Ob.NONE); give(Items.WOOD, rng.nextInt(1, 3)) }
                    Ob.WEED, Ob.BUSH -> if (o == Ob.WEED) cur.setO(x, y, Ob.NONE)
                }
            }
            Tool.SCYTHE, Tool.SWORD -> {
                val cells = ArrayList<Pair<Int, Int>>()
                cells.add(fx0 to fy0)
                if (t == Tool.SCYTHE) { if (dvx == 0) { cells.add(fx0 - 1 to fy0); cells.add(fx0 + 1 to fy0) } else { cells.add(fx0 to fy0 - 1); cells.add(fx0 to fy0 + 1) } }
                for ((x, y) in cells) {
                    val o = cur.o(x, y)
                    if (o == Ob.WEED) { cur.setO(x, y, Ob.NONE); if (rng.nextFloat() < 0.5f) give(Items.FIBER, 1) }
                    else if (o == Ob.TALLGRASS && t == Tool.SCYTHE) { cur.setO(x, y, Ob.NONE); if (rng.nextFloat() < 0.4f) give(Items.FIBER, 1) }
                    val c = if (cur.inb(x, y)) cur.crops[cur.idx(x, y)] else null
                    if (c != null && c.ripe && t == Tool.SCYTHE) harvest(x, y)
                }
                // hit gloops
                val hx = px + dvx * 0.9f; val hy = py + dvy * 0.9f
                for (g in cur.gloops) if (hypot(g.x - hx, g.y - hy) < 1.1f && g.hurt <= 0f) {
                    g.hp -= if (t == Tool.SWORD) 3 else 1
                    g.hurt = 0.35f; g.vx = dvx * 6f; g.vy = dvy * 6f
                }
                val dead = cur.gloops.filter { it.hp <= 0 }
                for (g in dead) {
                    cur.gloops.remove(g)
                    give(Items.GEL, rng.nextInt(1, 3))
                    if (rng.nextFloat() < 0.12f) revealLadderAt(floor(g.x).toInt(), floor(g.y).toInt())
                }
            }
        }
    }

    private fun breakRock(x: Int, y: Int, o: Int) {
        cur.setO(x, y, Ob.NONE)
        val luck = leadJob == Job.LUCK || leadJob == Job.MINE || buff == Buff.LUCK
        when (o) {
            Ob.ROCK -> { give(Items.STONE, rng.nextInt(1, 3)); if (cur.mineFloor > 0 && rng.nextFloat() < 0.04f) give(Items.COAL, 1) }
            Ob.BOULDER -> give(Items.STONE, rng.nextInt(10, 16))
            Ob.COPPER -> give(Items.COPPER, rng.nextInt(1, 4) + (if (luck) 1 else 0))
            Ob.IRON -> give(Items.IRON, rng.nextInt(1, 4) + (if (luck) 1 else 0))
            Ob.GOLD -> give(Items.GOLD, rng.nextInt(1, 3) + (if (luck) 1 else 0))
            Ob.COALROCK -> give(Items.COAL, rng.nextInt(1, 3))
            Ob.GEM -> {
                val r = rng.nextFloat() - (if (luck) 0.1f else 0f) - cur.mineFloor / 400f
                give(when { r < 0.05f -> Items.DIAMOND; r < 0.2f -> Items.RUBY; r < 0.5f -> Items.AMETHYST; else -> Items.QUARTZ }, 1)
            }
        }
        if (cur.mineFloor > 0) {
            val rocksLeft = cur.obj.count { it in setOf(Ob.ROCK, Ob.COPPER, Ob.IRON, Ob.GOLD, Ob.GEM, Ob.COALROCK) }
            val chance = 0.03f + 1.5f / max(rocksLeft, 1) + (if (luck) 0.02f else 0f)
            if (!cur.obj.contains(Ob.LADDER) && rng.nextFloat() < chance) revealLadderAt(x, y)
        }
    }

    private fun revealLadderAt(x: Int, y: Int) {
        if (cur.mineFloor <= 0 || cur.mineFloor >= 100 || cur.obj.contains(Ob.LADDER)) return
        if (cur.o(x, y) == Ob.NONE) { cur.setO(x, y, Ob.LADDER); note("A ladder down appeared!") }
    }

    private fun revealLadderNear() {
        val tx = floor(px).toInt(); val ty = floor(py).toInt()
        for (r in 1..3) for (oy in -r..r) for (ox in -r..r) if (cur.g(tx + ox, ty + oy) == Gr.CAVE && cur.o(tx + ox, ty + oy) == Ob.NONE) { revealLadderAt(tx + ox, ty + oy); return }
    }

    private fun plant(h: Slot) {
        val (x, y) = facing()
        if (cur.g(x, y) != Gr.TILLED || cur.o(x, y) != Ob.NONE) { note("Plant seeds in tilled soil."); return }
        val i = cur.idx(x, y)
        if (cur.crops[i] != null) return
        val cd = Crops.list[Items[h.id].crop]
        if (cd.seasons and (1 shl season) == 0) { note("${cd.name} won't grow in ${Season.names[season]}."); return }
        val c = Crop(cd.idx)
        if (leadJob == Job.GROW) c.days = 1
        cur.crops[i] = c
        inv.removeAt(sel, 1)
    }

    private fun place(h: Slot) {
        val (x, y) = facing()
        if (cur.id != "farm") { note("You can only place that on your farm."); return }
        if (cur.solid(x, y) || cur.o(x, y) != Ob.NONE || cur.crops[cur.idx(x, y)] != null) { note("Can't place that there."); return }
        cur.setO(x, y, if (h.id == Items.SPRINKLER) Ob.SPRINKLER else Ob.QSPRINKLER)
        if (cur.g(x, y) == Gr.TILLED) cur.setG(x, y, Gr.DIRT)
        inv.removeAt(sel, 1)
    }

    fun craft(r: Recipe) {
        if (r.needs.any { inv.count(it.first) < it.second }) { note("Missing materials."); return }
        for ((id, n) in r.needs) inv.remove(id, n)
        give(r.out, r.count)
    }

    fun buy(id: Int, n: Int) {
        val cost = Items[id].price * n
        if (gold < cost) { note("Not enough gold."); return }
        gold -= cost
        give(id, n)
    }

    fun sellSlot(i: Int) {
        val s = inv.slots[i] ?: return
        val d = Items[s.id]
        if (d.kind == Kind.TOOL) return
        gold += d.sell * s.n
        toast("+${d.sell * s.n}g", 0xFFFFE07A.toInt())
        inv.slots[i] = null
    }

    fun upgradeCost(t: Int): Triple<Int, Int, Int>? = when (toolLevel[t]) {
        0 -> Triple(1000, Items.COPPER, 5)
        1 -> Triple(3000, Items.IRON, 5)
        2 -> Triple(8000, Items.GOLD, 5)
        else -> null
    }

    fun upgrade(t: Int) {
        val c = upgradeCost(t) ?: return
        if (gold < c.first || inv.count(c.second) < c.third) { note("You need ${c.first}g and ${c.third} ${Items[c.second].name}."); return }
        gold -= c.first; inv.remove(c.second, c.third)
        toolLevel[t]++
        if (t == Tool.CAN) water = waterMax
        say("Gus", "There you go! Your ${Items[t].name} is now ${levelName(toolLevel[t])}-grade.")
    }

    fun levelName(l: Int) = arrayOf("Basic", "Copper", "Iron", "Gold")[l.coerceIn(0, 3)]

    fun moveToRanch(i: Int) {
        if (party.size <= 1) { note("Keep at least one creature in your party."); return }
        ranch.add(party.removeAt(i)); buildPen()
    }
    fun moveToParty(i: Int) {
        if (party.size >= 6) { note("Your party is full (6)."); return }
        party.add(ranch.removeAt(i)); buildPen()
    }

    // ======================================================================
    // Fishing
    // ======================================================================

    // ---------------------------------------------------------------- cooking

    private fun openStove() {
        if (!cookIntro) {
            cookIntro = true
            say(null, "Grandma's old wood stove crackles to life. A recipe card is tucked behind the kettle.",
                "Pick a dish, check you're carrying the ingredients, then tap Cook. Dishes give lots of energy plus a buff for a few hours: Swift (walk faster), Lucky (faster bites, extra forage and ore), Miner (+1 pickaxe power) or Hearty (tools use half energy).",
                "Eat a dish from your hotbar or the Items menu. Only one buff lasts at a time, and buffs end when you sleep.",
                onClose = { mode = Mode.COOK; menuSel = -1 })
            return
        }
        mode = Mode.COOK; menuSel = -1
    }

    /** How many of an ingredient you carry (ANY_FISH counts every fish). */
    fun ingCount(id: Int): Int = if (id == Cooking.ANY_FISH) inv.slots.sumOf { if (it != null && Items[it.id].kind == Kind.FISH) it.n else 0 } else inv.count(id)

    fun canCook(d: Dish) = d.needs.all { (id, n) -> ingCount(id) >= n }

    fun cook(d: Dish): Boolean {
        if (!canCook(d)) { note("You're missing ingredients."); return false }
        for ((id, n) in d.needs) {
            if (id == Cooking.ANY_FISH) {
                repeat(n) {
                    val fishId = inv.slots.filterNotNull().filter { Items[it.id].kind == Kind.FISH }.minByOrNull { Items[it.id].sell }!!.id
                    inv.remove(fishId, 1)
                }
            } else inv.remove(id, n)
        }
        give(d.id, 1)
        dishesCooked++
        toast("Cooked ${d.name}!", 0xFFFFE07A.toInt())
        return true
    }

    fun hasRod() = inv.count(Tool.ROD) + ranchBox.count(Tool.ROD) > 0

    fun buyRod() {
        if (hasRod()) return
        if (gold < Items[Tool.ROD].price) { say("Mira", "A rod is ${Items[Tool.ROD].price}g. Come back when you've saved up!"); return }
        gold -= Items[Tool.ROD].price
        give(Tool.ROD, 1, true)
        say("Mira", "Here's your very own Fishing Rod! It was my grandfather's design.",
            "Stand facing any water and use the rod to cast. Watch your float - when it dips and you see a !, tap quickly to hook the fish.",
            "Then a reel dial appears. Tap anywhere when the spinning needle is inside the green. Miss three times and the line snaps!",
            "Sea fish live here, river fish in the forest, pond fish on your farm, and the mountain lake has its own. Some only bite at night or in the rain. Tide creatures leading your party make the green zone wider!")
    }

    fun fishLoc(a: Area): Int? = when (a.id) { "beach" -> Loc.OCEAN; "forest" -> Loc.RIVER; "farm" -> Loc.POND; "mountain" -> Loc.LAKE; else -> null }

    /** True when the player faces water they could cast into. */
    fun canCast(): Boolean = castTarget() != null

    private fun castTarget(): Pair<Int, Int>? {
        if (fishLoc(cur) == null) return null
        val (dvx, dvy) = dirVec(); val tx = floor(px).toInt(); val ty = floor(py).toInt()
        for (k in 1..3) {
            val x = tx + dvx * k; val y = ty + dvy * k
            if (Gr.isWater(cur.g(x, y))) return if (Gr.isWater(cur.g(x + dvx, y + dvy))) (x + dvx) to (y + dvy) else x to y
            if (k == 1 && cur.solid(x, y) && cur.g(x, y) != Gr.BRIDGE) return null
        }
        return null
    }

    private fun castRod() {
        val loc = fishLoc(cur)
        if (loc == null) { note("There's nowhere to fish here."); return }
        val t = castTarget()
        if (t == null) { note("Face open water to cast your line."); return }
        if (!spend(4f)) return
        swingT = 0.25f; swingTool = Tool.ROD
        fishing = FishingState(loc, t.first, t.second).apply { this.t = biteWait() }
        mode = Mode.FISH
    }

    private fun biteWait(): Float = (1.4f + rng.nextFloat() * 3.6f) * (if (lead?.sp?.type == EType.TIDE) 0.7f else 1f) * (if (buff == Buff.LUCK) 0.7f else 1f)

    /** Picks a fish that lives here right now (location, season, hour, weather), weighted by rarity. */
    fun rollFish(loc: Int): FishDef {
        val t = time.toInt()
        val ok = Fish.list.filter { it.loc and loc != 0 && it.seasons and (1 shl season) != 0 && t >= it.from && t < it.to && (!it.rainOnly || raining) }
        val pool = ok.ifEmpty { Fish.list.filter { it.loc and loc != 0 }.sortedByDescending { it.weight }.take(1) }
        var r = rng.nextInt(pool.sumOf { it.weight })
        for (f in pool) { r -= f.weight; if (r < 0) return f }
        return pool.last()
    }

    private fun updateFishing(dt: Float) {
        val f = fishing ?: run { mode = Mode.PLAY; return }
        f.anim += dt; f.flash = max(0f, f.flash - dt)
        when (f.phase) {
            0 -> { f.t -= dt; if (f.t <= 0f) { f.phase = 1; f.t = 1.0f } }
            1 -> { f.t -= dt; if (f.t <= 0f) endFishing("It got away... tap as soon as you see the !") }
            2 -> f.angle = ((f.angle + f.speed * f.spin * dt) % 360f + 360f) % 360f
        }
    }

    /** A tap anywhere while fishing. */
    fun fishTap() {
        val f = fishing ?: return
        when (f.phase) {
            0 -> endFishing("Too early! Wait for the float to dip.")
            1 -> {
                val fish = rollFish(f.loc)
                f.fish = fish; f.phase = 2
                f.speed = 150f + fish.diff * 190f
                f.zoneW = 74f - fish.diff * 38f + (if (lead?.sp?.type == EType.TIDE) 12f else 0f)
                f.need = 3 + (fish.diff * 2.6f).toInt()
                f.zone = rng.nextFloat() * 360f
                f.angle = (f.zone + 180f) % 360f
            }
            2 -> {
                f.flash = 0.25f
                if (f.inZone()) {
                    f.lastHit = true; f.hits++
                    if (f.hits >= f.need) { landFish(f); return }
                    f.zone = (f.zone + 100f + rng.nextFloat() * 160f) % 360f
                    f.speed *= 1.07f
                    if (f.fish!!.diff > 0.5f && rng.nextFloat() < 0.5f) f.spin = -f.spin
                } else {
                    f.lastHit = false; f.misses++
                    if (f.misses >= 3) endFishing("The line snapped! The ${f.fish!!.name} escaped.")
                }
            }
        }
    }

    fun fishCancel() { if (fishing != null) endFishing(if (fishing?.phase == 2) "You let it go." else "You reeled in your line.") }

    private fun endFishing(msg: String) { fishing = null; mode = Mode.PLAY; note(msg) }

    private fun landFish(f: FishingState) {
        val fish = f.fish ?: return
        fishing = null; mode = Mode.PLAY
        give(fish.id, 1, true)
        val first = (fishCaught[fish.id] ?: 0) == 0
        fishCaught[fish.id] = (fishCaught[fish.id] ?: 0) + 1
        lead?.let { it.bond++ }
        toast("+1 ${fish.name}", 0xFF9AD6FF.toInt())
        if (first) say(null, "You caught a ${fish.name}! It's your first one.", "${fish.desc} Sells for ${fish.price}g. (Fish species found: ${fishCaught.size}/${Fish.list.size})")
        else note("Caught a ${fish.name}!")
    }

    // ======================================================================
    // Day cycle
    // ======================================================================

    private fun passOut() {
        val lost = min(1000, gold / 10)
        gold -= lost
        endDay(true, if (lost > 0) "You passed out from exhaustion! Someone carried you home. (-${lost}g)" else "You passed out from exhaustion! Someone carried you home.")
    }

    fun blackout(msg: String) {
        val lost = min(500, gold / 20)
        gold -= lost
        for (m in party) if (m.hp <= 0) m.hp = 1
        hp = maxHp / 2
        energy = energy / 2
        time = min(time + 120f, 1500f)
        warp("house", 3.5f, 4.5f, 0)
        say(null, msg, "You wake up at home. Ranger Wren must have helped you back." + (if (lost > 0) " (-${lost}g)" else ""))
    }

    fun endDay(passedOut: Boolean, extra: String? = null) {
        val lines = ArrayList<String>()
        extra?.let { lines.add(it) }
        // shipping
        var total = 0
        for (s in shipped) total += Items[s.id].sell * s.n
        shipped.clear()
        if (total > 0) { gold += total; lines.add("Shipping earned ${total}g.") }

        val farm = areas["farm"]!!
        // crop growth
        val newSeason = day == 28
        for ((i, c) in farm.crops.entries) {
            if (farm.watered[i] || raining) c.days++
        }
        // advance date
        day++
        if (day > 28) {
            day = 1; season = (season + 1) % 4; if (season == 0) year++
            lines.add("It's now ${Season.names[season]}!")
            val dead = farm.crops.filter { it.value.def.seasons and (1 shl season) == 0 }.keys
            if (dead.isNotEmpty()) lines.add("${dead.size} out-of-season crops withered.")
            for (k in dead) farm.crops.remove(k)
        }
        // weather
        raining = rainTomorrow
        rainTomorrow = season != 3 && rng.nextFloat() < 0.22f
        for (i in farm.watered.indices) farm.watered[i] = raining && farm.ground[i] == Gr.TILLED
        // sprinklers
        for (y in 0 until farm.h) for (x in 0 until farm.w) {
            val o = farm.o(x, y)
            if (o == Ob.SPRINKLER || o == Ob.QSPRINKLER) for (dy in -1..1) for (dx in -1..1) {
                if (o == Ob.SPRINKLER && abs(dx) + abs(dy) != 1) continue
                if (farm.g(x + dx, y + dy) == Gr.TILLED) farm.watered[farm.idx(x + dx, y + dy)] = true
            }
        }
        // ranch helpers work the morning (+ lead creature at half power)
        val helpers = ranch.map { it to it.jobPower } + listOfNotNull(lead?.let { it to max(1, it.jobPower / 2) })
        for ((m, p) in helpers) {
            val r = doJob(farm, m, p)
            m.bond++
            if (r != null) lines.add(r)
        }
        makeGoods(farm, lines)
        buff = null
        if (!cookTip) { cookTip = true; lines.add("NEW: Grandma's old stove in the farmhouse kitchen works again! Face it and press Cook to turn crops, forage and fish into dishes that give energy and a buff.") }
        // weeds creep back
        repeat(4) {
            val x = rng.nextInt(3, farm.w - 3); val y = rng.nextInt(3, farm.h - 3)
            if (farm.g(x, y) == Gr.GRASS && farm.o(x, y) == Ob.NONE && !(x in 6..17 && y in 2..16) && !(x in 26..43 && y in 1..17)) farm.setO(x, y, Ob.WEED)
        }
        // public areas reset
        rebuildPublicAreas()
        // restore
        energy = if (passedOut) maxEnergy * 0.6f else maxEnergy.toFloat()
        hp = maxHp
        for (m in party) m.fullHeal()
        water = waterMax
        talkedToday.clear(); giftedToday.clear()
        time = 360f
        cur = areas["house"]!!; px = 3.5f; py = 4.5f; dir = 0; trail.clear(); fx = px; fy = py
        lines.add(if (raining) "It's raining today. Your crops are watered!" else if (season == 3) "A crisp winter morning." else "A sunny morning.")
        report = lines
        mode = Mode.REPORT
        save()
    }

    /** Ranch creatures leave goods of their type in the pen overnight; higher bond = more often, sometimes two. */
    private fun makeGoods(farm: Area, lines: MutableList<String>) {
        if (ranch.isEmpty()) return
        var n = 0
        val existing = farm.obj.count { it == Ob.GOODS }
        for (m in ranch) {
            if (existing + n >= 24) break
            val chance = (0.35f + m.bond / 250f).coerceAtMost(0.9f)
            if (rng.nextFloat() >= chance) continue
            val count = if (m.bond >= 120 && rng.nextFloat() < 0.4f) 2 else 1
            repeat(count) { if (placeGoods(farm, Items.goodsFor(m.sp.type))) n++ }
        }
        if (n == 0) return
        lines.add("Your ranch creatures left $n ${if (n == 1) "gift" else "gifts"} in the pen.")
        if (!goodsIntro) {
            goodsIntro = true
            lines.add("NEW: Happy ranch creatures now make goods - fleece, pearls, glowstones and more, one kind per creature type. The higher their bond, the more often they make them. Walk into the pen and press Pick to collect, then sell or gift them!")
        }
    }

    private fun placeGoods(farm: Area, id: Int): Boolean {
        repeat(40) {
            val x = rng.nextInt(29, 41); val y = rng.nextInt(10, 16)
            if (farm.o(x, y) == Ob.NONE && !Gr.solid(farm.g(x, y))) { farm.setO(x, y, Ob.GOODS, id); return true }
        }
        return false
    }

    private fun doJob(farm: Area, m: Mon, p: Int): String? {
        val name = m.name
        when (m.sp.job) {
            Job.WATER -> {
                var n = 0
                for ((i, c) in farm.crops) if (!farm.watered[i] && !c.ripe && n < 6 * p) { farm.watered[i] = true; n++ }
                return if (n > 0) "$name watered $n crops overnight." else null
            }
            Job.GROW -> {
                var n = 0
                for (c in farm.crops.values) if (!c.ripe && n < 3 * p && rng.nextFloat() < 0.6f) { c.days++; n++ }
                return if (n > 0) "$name helped $n crops grow faster." else null
            }
            Job.CLEAR -> {
                var n = 0
                for (i in farm.obj.indices) {
                    if (n >= 4 * p) break
                    val o = farm.obj[i]
                    if (o == Ob.WEED || o == Ob.STICK || o == Ob.ROCK) {
                        farm.obj[i] = Ob.NONE; n++
                        ranchBox.add(when (o) { Ob.STICK -> Items.WOOD; Ob.ROCK -> Items.STONE; else -> Items.FIBER }, 1)
                    }
                }
                return if (n > 0) "$name cleared $n bits of debris (Ranch Box)." else null
            }
            Job.MINE -> {
                ranchBox.add(Items.STONE, 3 * p)
                val ore = when { m.level >= 35 -> Items.GOLD; m.level >= 20 -> Items.IRON; else -> Items.COPPER }
                ranchBox.add(ore, p)
                return "$name dug up stone and ${Items[ore].name} (Ranch Box)."
            }
            Job.FORAGE -> {
                val pool = Items.forageFor(season, false)
                val n = 1 + p / 2
                repeat(n) { ranchBox.add(pool[rng.nextInt(pool.size)], 1) }
                return "$name brought back $n forage (Ranch Box)."
            }
            Job.HARVEST -> {
                var n = 0
                for ((i, c) in farm.crops.entries.toList()) {
                    if (n >= 4 * p) break
                    if (c.ripe) {
                        ranchBox.add(Items.produce(c.type), c.def.yield); n++
                        if (c.def.regrow > 0) c.days = c.def.days - c.def.regrow else farm.crops.remove(i)
                    }
                }
                return if (n > 0) "$name harvested $n crops (Ranch Box)." else null
            }
            Job.LUCK -> {
                val g = rng.nextInt(20, 61) * p
                gold += g
                if (rng.nextFloat() < 0.08f * p) ranchBox.add(listOf(Items.QUARTZ, Items.AMETHYST, Items.RUBY)[rng.nextInt(3)], 1)
                return "$name found ${g}g lying around."
            }
        }
    }

    private fun refreshForage(a: Area) {
        for (i in a.obj.indices) if (a.obj[i] == Ob.FORAGE) a.obj[i] = Ob.NONE
        val pool = Items.forageFor(season, a.beach)
        var placed = 0; var tries = 0
        while (placed < a.forageSpots && tries < 400) {
            tries++
            val x = rng.nextInt(2, a.w - 2); val y = rng.nextInt(2, a.h - 2)
            val g = a.g(x, y)
            if ((g == Gr.GRASS || g == Gr.SAND || g == Gr.DIRT) && a.o(x, y) == Ob.NONE) { a.setO(x, y, Ob.FORAGE, pool[rng.nextInt(pool.size)]); placed++ }
        }
    }

    // ======================================================================
    // Save / load
    // ======================================================================

    fun save() {
        if (!started) return
        try {
            val o = JSONObject()
            o.put("v", 1); o.put("seed", seed)
            o.put("day", day); o.put("season", season); o.put("year", year)
            o.put("gold", gold); o.put("raining", raining); o.put("rainT", rainTomorrow)
            o.put("inv", inv.toJson()); o.put("box", ranchBox.toJson())
            o.put("tools", JSONArray(toolLevel.toList()))
            o.put("party", JSONArray().apply { party.forEach { put(it.toJson()) } })
            o.put("ranch", JSONArray().apply { ranch.forEach { put(it.toJson()) } })
            o.put("seen", JSONArray(dexSeen.toList())); o.put("caught", JSONArray(dexCaught.toList()))
            o.put("deepest", deepest); o.put("astral", astralCaught)
            o.put("friends", JSONObject(friendship as Map<*, *>)); o.put("hgifts", JSONArray(heartGifts.toList()))
            o.put("fish", JSONObject().apply { for ((k, v) in fishCaught) put(k.toString(), v) }); o.put("fishTip", fishIntro); o.put("goodsTip", goodsIntro)
            o.put("cookTip", cookTip); o.put("cookIntro", cookIntro); o.put("cooked", dishesCooked)
            val f = areas["farm"]!!
            o.put("fg", intsToStr(f.ground)); o.put("fo", intsToStr(f.obj)); o.put("fa", intsToStr(f.aux))
            o.put("fw", f.watered.joinToString("") { if (it) "1" else "0" })
            o.put("crops", JSONArray().apply { for ((i, c) in f.crops) put(JSONArray(listOf(i, c.type, c.days))) })
            val tmp = File(saveFile.parentFile, saveFile.name + ".tmp")
            tmp.writeText(o.toString())
            tmp.renameTo(saveFile)
        } catch (_: Exception) {}
    }

    private fun intsToStr(a: IntArray) = a.joinToString(",")
    private fun strToInts(s: String, into: IntArray) { val parts = s.split(","); for (i in into.indices) into[i] = parts.getOrNull(i)?.toIntOrNull() ?: 0 }

    private fun load(): Boolean {
        if (!saveFile.exists()) return false
        return try {
            val o = JSONObject(saveFile.readText())
            seed = o.getInt("seed")
            buildWorld()
            day = o.getInt("day"); season = o.getInt("season"); year = o.getInt("year")
            gold = o.getInt("gold"); raining = o.optBoolean("raining"); rainTomorrow = o.optBoolean("rainT")
            inv.load(o.getJSONArray("inv")); ranchBox.load(o.getJSONArray("box"))
            val tl = o.getJSONArray("tools"); for (i in 0 until min(toolLevel.size, tl.length())) toolLevel[i] = tl.getInt(i)
            val pa = o.getJSONArray("party"); for (i in 0 until pa.length()) party.add(Mon.fromJson(pa.getJSONObject(i)))
            val ra = o.getJSONArray("ranch"); for (i in 0 until ra.length()) ranch.add(Mon.fromJson(ra.getJSONObject(i)))
            val se = o.getJSONArray("seen"); for (i in 0 until se.length()) dexSeen.add(se.getInt(i))
            val ca = o.getJSONArray("caught"); for (i in 0 until ca.length()) dexCaught.add(ca.getInt(i))
            deepest = o.optInt("deepest"); astralCaught = o.optBoolean("astral")
            val fr = o.optJSONObject("friends"); if (fr != null) for (k in fr.keys()) friendship[k] = fr.getInt(k)
            val hg = o.optJSONArray("hgifts"); if (hg != null) for (i in 0 until hg.length()) heartGifts.add(hg.getString(i))
            val fc = o.optJSONObject("fish"); if (fc != null) for (k in fc.keys()) k.toIntOrNull()?.let { fishCaught[it] = fc.optInt(k) }
            fishIntro = o.optBoolean("fishTip", false)
            goodsIntro = o.optBoolean("goodsTip", false)
            cookTip = o.optBoolean("cookTip", false); cookIntro = o.optBoolean("cookIntro", false); dishesCooked = o.optInt("cooked", 0)
            val f = areas["farm"]!!
            strToInts(o.getString("fg"), f.ground); strToInts(o.getString("fo"), f.obj); strToInts(o.getString("fa"), f.aux)
            val fw = o.getString("fw"); for (i in f.watered.indices) f.watered[i] = fw.getOrNull(i) == '1'
            val cr = o.getJSONArray("crops")
            for (i in 0 until cr.length()) { val a = cr.getJSONArray(i); f.crops[a.getInt(0)] = Crop(a.getInt(1)).apply { days = a.getInt(2) } }
            started = party.isNotEmpty()
            time = 360f
            cur = areas["house"]!!; px = 3.5f; py = 4.5f; dir = 0; fx = px; fy = py
            water = waterMax
            mode = if (started) Mode.PLAY else Mode.STARTER
            if (!started) { inv.slots.fill(null); newGame() }
            true
        } catch (e: Exception) {
            false
        }
    }
}
