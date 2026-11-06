package com.jondude.stardustranch.game

import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** A creature you own or meet. */
class Mon(var speciesId: Int, var level: Int, var nick: String? = null) {
    var xp = 0
    var hp = 0
    var bond = 0 // grows when it helps on the farm or battles beside you

    init { hp = maxHp }

    val sp: Species get() = Dex[speciesId]
    val name: String get() = nick ?: sp.name
    val maxHp: Int get() = sp.hp * level / 25 + level + 12
    val atk: Int get() = sp.atk * level / 30 + 5
    val def: Int get() = sp.def * level / 30 + 5
    val spd: Int get() = sp.spd * level / 30 + 5
    val xpToNext: Int get() = level * level * 4 + 20
    val fainted: Boolean get() = hp <= 0
    val jobPower: Int get() = 1 + level / 6 + bond / 40

    fun moves(): List<Move> {
        val own = Moves.byType[sp.type]!!.filter { it.level <= level }
        val base = if (sp.type != EType.WILD) listOf(Moves.byType[EType.WILD]!![0]) else emptyList()
        return (base + own).takeLast(4)
    }

    fun heal(amount: Int) { hp = min(maxHp, hp + amount) }
    fun fullHeal() { hp = maxHp }

    /** Returns messages for level ups and evolution. */
    fun gainXp(amount: Int): List<String> {
        val out = ArrayList<String>()
        xp += amount
        while (xp >= xpToNext && level < 60) {
            xp -= xpToNext
            val oldMax = maxHp
            level++
            hp += maxHp - oldMax
            out.add("$name grew to level $level!")
            val learned = Moves.byType[sp.type]!!.firstOrNull { it.level == level }
            if (learned != null) out.add("$name learned ${learned.name}!")
            if (sp.evolveTo >= 0 && level >= sp.evolveLevel) {
                val old = name
                val wasNick = nick != null
                speciesId = sp.evolveTo
                hp = maxHp
                out.add("$old evolved into ${if (wasNick) "$name the ${sp.name}" else sp.name}!")
            }
        }
        return out
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("s", speciesId); put("l", level); put("x", xp); put("h", hp); put("b", bond)
        nick?.let { put("n", it) }
    }

    companion object {
        fun fromJson(o: JSONObject): Mon {
            val m = Mon(o.getInt("s"), o.getInt("l"), if (o.has("n")) o.getString("n") else null)
            m.xp = o.optInt("x"); m.hp = o.optInt("h", m.maxHp); m.bond = o.optInt("b")
            return m
        }
        private val namePool = listOf("Pip", "Juniper", "Bramble", "Clover", "Sunny", "Mochi", "Rusty", "Bean", "Willow",
            "Pepper", "Ziggy", "Hazel", "Biscuit", "Nova", "Pebble", "Basil", "Ember", "Misty", "Sprocket", "Fern",
            "Tumble", "Echo", "Marigold", "Dot", "Comet", "Toffee", "Sage", "Rook", "Indigo", "Maple")
        fun randomNick(rng: Random) = namePool[rng.nextInt(namePool.size)]
    }
}

/** Turn-based battle against a wild creature. */
class Battle(val game: Game, val wild: Mon, val boss: Boolean = false) {
    val msgs = ArrayDeque<String>()
    var activeIdx = game.party.indexOfFirst { !it.fainted }.coerceAtLeast(0)
    val active: Mon get() = game.party[activeIdx]
    var over = false
    var caught = false
    var fled = false
    var lost = false
    var menu = 0 // 0 main, 1 moves, 2 orbs, 3 swap
    var shake = 0f
    var flashWild = 0f
    var flashMine = 0f
    var orbAnim = 0f
    val rng = Random(System.nanoTime())
    var participants = HashSet<Int>().apply { add(activeIdx) }

    init {
        game.dexSeen.add(wild.speciesId)
        msgs.add(if (boss) "The legendary ${wild.name} appears!" else "A wild ${wild.name} (Lv ${wild.level}) appeared!")
        msgs.add("Go, ${active.name}!")
    }

    val waiting: Boolean get() = msgs.isNotEmpty()

    private fun damage(att: Mon, def: Mon, mv: Move): Pair<Int, Float> {
        val mult = TypeChart.mult(mv.type, def.sp.type)
        val stab = if (mv.type == att.sp.type) 1.25f else 1f
        val base = ((2f * att.level / 5f + 2f) * mv.power * att.atk / max(1, def.def)) / 40f + 2f
        val champ = if (att !== wild && game.has("champion")) 1.1f else 1f
        val dmg = (base * mult * stab * champ * (0.85f + rng.nextFloat() * 0.15f)).toInt()
        return max(1, dmg) to mult
    }

    private fun effText(m: Float) = when {
        m > 1f -> "It's super effective!"
        m < 1f -> "It's not very effective..."
        else -> null
    }

    private fun doMove(att: Mon, def: Mon, mv: Move, mine: Boolean) {
        msgs.add("${if (mine) "" else "Wild "}${att.name} used ${mv.name}!")
        if (mv.heal) {
            val amt = att.maxHp / 2
            att.heal(amt)
            msgs.add("${att.name} recovered health!")
            return
        }
        if (rng.nextFloat() < 0.06f) { msgs.add("It missed!"); return }
        val (d, m) = damage(att, def, mv)
        def.hp = max(0, def.hp - d)
        if (mine) flashWild = 0.4f else flashMine = 0.4f
        effText(m)?.let { msgs.add(it) }
    }

    private fun wildMove(): Move {
        val ms = wild.moves()
        val healer = ms.firstOrNull { it.heal }
        if (healer != null && wild.hp < wild.maxHp / 3 && rng.nextFloat() < 0.4f) return healer
        val atks = ms.filter { !it.heal }
        return atks[rng.nextInt(atks.size)]
    }

    fun chooseMove(mv: Move) {
        menu = 0
        val mineFirst = active.spd >= wild.spd || rng.nextFloat() < 0.1f
        if (mineFirst) {
            doMove(active, wild, mv, true)
            if (checkWildFaint()) return
            doMove(wild, active, wildMove(), false)
            checkMyFaint()
        } else {
            doMove(wild, active, wildMove(), false)
            if (checkMyFaint()) return
            doMove(active, wild, mv, true)
            checkWildFaint()
        }
    }

    private fun checkWildFaint(): Boolean {
        if (!wild.fainted) return false
        msgs.add("Wild ${wild.name} fainted!")
        val xp = (wild.level * (if (boss) 40 else 14) * (1f + wild.sp.hp / 100f) * (if (game.has("trainer")) 1.25f else 1f)).toInt()
        game.addXp(Skill.TAME, 6 + wild.level / 2 + (if (boss) 40 else 0))
        for (i in participants) {
            val m = game.party.getOrNull(i) ?: continue
            if (m.fainted) continue
            val share = if (i == activeIdx) xp else xp / 2
            msgs.add("${m.name} gained $share XP.")
            m.bond += game.bondGain(2)
            msgs.addAll(m.gainXp(share))
            game.dexSeen.add(m.speciesId); game.dexCaught.add(m.speciesId)
        }
        over = true
        return true
    }

    private fun checkMyFaint(): Boolean {
        if (!active.fainted) return false
        msgs.add("${active.name} fainted!")
        val next = game.party.indexOfFirst { !it.fainted }
        if (next < 0) {
            msgs.add("You have no creatures left to fight...")
            lost = true; over = true
        } else {
            activeIdx = next
            participants.add(next)
            msgs.add("Go, ${active.name}!")
        }
        return true
    }

    fun swap(i: Int) {
        menu = 0
        if (i == activeIdx || game.party[i].fainted) return
        msgs.add("Come back, ${active.name}!")
        activeIdx = i
        participants.add(i)
        msgs.add("Go, ${active.name}!")
        doMove(wild, active, wildMove(), false)
        checkMyFaint()
    }

    fun throwOrb(orbId: Int) {
        menu = 0
        if (boss && orbId != Items.STAR_ORB && wild.hp > wild.maxHp / 4) {
            game.inv.remove(orbId, 1)
            msgs.add("You threw a ${Items[orbId].name}...")
            msgs.add("${wild.name} swatted it away! Weaken it first.")
            doMove(wild, active, wildMove(), false); checkMyFaint(); return
        }
        if (!game.inv.remove(orbId, 1)) return
        val power = Items[orbId].orbPower
        val hpFactor = 1f - 0.75f * wild.hp.toFloat() / wild.maxHp
        var chance = wild.sp.catchRate * power * (0.35f + hpFactor * 1.4f)
        chance *= (1f + game.skillLv(Skill.TAME) * 0.02f) * (if (game.has("gentle")) 1.15f else 1f)
        if (game.party.size + game.ranch.size == 0) chance = 1f
        chance = chance.coerceIn(0.02f, 0.98f)
        orbAnim = 1.6f
        msgs.add("You threw a ${Items[orbId].name}!")
        val shakes = if (rng.nextFloat() < chance) 3 else rng.nextInt(0, 3)
        repeat(shakes) { msgs.add("...wobble...") }
        if (shakes == 3) {
            caught = true; over = true
            wild.nick = Mon.randomNick(rng)
            wild.bond = 10
            msgs.add("Gotcha! ${wild.sp.name} was caught!")
            msgs.add("You named it ${wild.nick}.")
            game.dexCaught.add(wild.speciesId)
            game.addXp(Skill.TAME, 20 + wild.level)
            if (game.party.size < 6) {
                game.party.add(wild); msgs.add("${wild.name} joined your party.")
            } else {
                game.ranch.add(wild); msgs.add("${wild.name} was sent to your ranch.")
            }
        } else {
            msgs.add("Oh no! It broke free!")
            doMove(wild, active, wildMove(), false)
            checkMyFaint()
        }
    }

    fun run() {
        menu = 0
        if (boss) { msgs.add("You can't run from this one!"); return }
        val chance = 0.6f + (active.spd - wild.spd) * 0.02f
        if (rng.nextFloat() < chance) {
            msgs.add("Got away safely!")
            fled = true; over = true
        } else {
            msgs.add("Couldn't get away!")
            doMove(wild, active, wildMove(), false)
            checkMyFaint()
        }
    }

    fun update(dt: Float) {
        shake = max(0f, shake - dt)
        flashWild = max(0f, flashWild - dt)
        flashMine = max(0f, flashMine - dt)
        orbAnim = max(0f, orbAnim - dt)
    }
}
