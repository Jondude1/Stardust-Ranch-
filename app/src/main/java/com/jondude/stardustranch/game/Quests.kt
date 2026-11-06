package com.jondude.stardustranch.game

import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Town bulletin board requests: villagers pin up a note asking for a few items.
 * Accept a note on the board, then bring the items to that villager and talk to them.
 */
class Quest(
    val npc: String, val item: Int, val qty: Int, val gold: Int, val friend: Int,
    val bonus: Int, val bonusN: Int, var daysLeft: Int, var accepted: Boolean = false, val line: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().put("npc", npc).put("i", item).put("q", qty).put("g", gold).put("f", friend)
        .put("b", bonus).put("bn", bonusN).put("d", daysLeft).put("a", accepted).put("l", line)

    companion object {
        fun fromJson(o: JSONObject): Quest? {
            val npc = o.optString("npc", ""); val item = o.optInt("i", -1)
            if (npc !in Quests.who || Items.all[item] == null) return null
            return Quest(npc, item, max(1, o.optInt("q", 1)), o.optInt("g", 0), o.optInt("f", 0), o.optInt("b", -1), o.optInt("bn", 0),
                o.optInt("d", 1), o.optBoolean("a", false), o.optInt("l", 0))
        }
    }
}

object Quests {
    const val MAX_POSTED = 3     // open notes on the board
    const val MAX_ACTIVE = 3     // accepted, not yet delivered
    const val DAYS = 3           // days a note stays up / an accepted request lasts
    const val FRIEND = 60        // friendship points for a delivered request (a quarter heart)

    /** Villagers who post requests, with the speaker name on their notes. */
    val who = linkedMapOf("hollis" to "Mayor Hollis", "pia" to "Pia", "wren" to "Ranger Wren", "gus" to "Gus", "mira" to "Mira")

    /** Short request texts, one picked per note ("%s" = item name). */
    private val lines = mapOf(
        "hollis" to listOf("The town hall could use some %s for the welcome basket.", "I'm hosting the council dinner. Could you spare some %s?", "Polishing up the town display. I need %s!"),
        "pia" to listOf("Customers keep asking for %s and my shelves are empty!", "I'm out of %s. Could you restock me?", "Looking for fresh %s for a special order."),
        "wren" to listOf("The creatures at the centre would love some %s.", "I'm studying what wild creatures eat. Could you bring %s?", "Running low on %s for my patrol pack."),
        "gus" to listOf("The forge is hungry. Bring me %s.", "Got a big order in. I need %s, quick.", "Short on %s this week. Help an old smith out?"),
        "mira" to listOf("I promised Grandpa a plate of %s.", "Want to help with my tide-pool survey? I need %s.", "The pier cats are restless. Some %s would calm them.")
    )

    fun text(q: Quest): String {
        val l = lines[q.npc] ?: return "Wanted: %s"
        val name = if (q.qty > 1) "${q.qty} ${Items[q.item].name}" else Items[q.item].name
        return l[q.line % l.size].replace("%s", name)
    }

    /** Items a villager might ask for this season (everything here can be found, grown, mined or caught). */
    fun pool(npc: String, season: Int): List<Int> {
        val crops = Crops.list.filter { it.seasons and (1 shl season) != 0 && it.idx != 12 }.map { Items.produce(it.idx) }
        val forage = Items.forageFor(season, false).toList()
        val beach = listOf(58, 59)
        val fish = Fish.list.filter { it.seasons and (1 shl season) != 0 && !it.rainOnly && it.diff <= 0.45f }.map { it.id }
        return when (npc) {
            "hollis" -> crops + listOf(Items.QUARTZ, Items.AMETHYST)
            "pia" -> crops + forage
            "wren" -> forage + listOf(Items.GEL) + fish.take(2)
            "gus" -> listOf(Items.COPPER, Items.COPPER, Items.STONE, Items.WOOD, Items.COAL, Items.IRON)
            else -> fish + beach
        }
    }

    /** How many of an item a request asks for: lots of cheap things, a single pricey thing. */
    fun qtyFor(id: Int, r: Random): Int {
        val p = Items[id].price
        return when {
            p <= 3 -> 15 + r.nextInt(3) * 5        // wood, stone: 15-25
            p <= 15 -> 5 + r.nextInt(6)            // ore, coal, gel: 5-10
            p <= 60 -> 3 + r.nextInt(3)            // cheap crops/forage/fish: 3-5
            p <= 150 -> 1 + r.nextInt(3)           // 1-3
            else -> 1
        }
    }

    /** Pays a bit over the items' sale value plus a flat delivery fee, rounded to 5g. Never a huge jump. */
    fun goldFor(id: Int, qty: Int): Int {
        val value = Items[id].price * qty
        val g = value * 1.5f + 40 + min(60, value / 4)
        return min(1500, ((g / 5f).roundToInt() * 5))
    }

    /** A small thank-you item some villagers add (null = none). */
    fun bonusFor(npc: String, season: Int, r: Random): Pair<Int, Int>? {
        if (r.nextFloat() >= 0.4f) return null
        return when (npc) {
            "hollis" -> Items.QUARTZ to 1
            "pia" -> {
                val c = Crops.list.filter { it.seasons and (1 shl season) != 0 && it.idx != 12 }
                Items.seed(c[r.nextInt(c.size)].idx) to 3
            }
            "wren" -> Items.ORB to 2
            "gus" -> Items.COAL to 3
            else -> Items.SNACK to 1
        }
    }

    fun make(npc: String, season: Int, r: Random): Quest {
        val p = pool(npc, season)
        val id = p[r.nextInt(p.size)]
        val q = qtyFor(id, r)
        val b = bonusFor(npc, season, r)
        return Quest(npc, id, q, goldFor(id, q), FRIEND, b?.first ?: -1, b?.second ?: 0, DAYS, false, r.nextInt(3))
    }

    /**
     * Morning board refresh: expire old notes and requests, then pin 1-2 new notes (up to [MAX_POSTED] open).
     * Never two open notes from the same villager, and never a note from someone you already owe.
     * Returns how many requests expired while accepted.
     */
    fun refresh(list: MutableList<Quest>, season: Int, r: Random, age: Boolean = true): Int {
        var lapsed = 0
        if (age) {
            val it = list.iterator()
            while (it.hasNext()) { val q = it.next(); q.daysLeft--; if (q.daysLeft <= 0) { if (q.accepted) lapsed++; it.remove() } }
        }
        var add = 1 + r.nextInt(2)
        while (add > 0 && list.count { !it.accepted } < MAX_POSTED) {
            val free = who.keys.filter { k -> list.none { it.npc == k } }
            if (free.isEmpty()) break
            list.add(make(free[r.nextInt(free.size)], season, r))
            add--
        }
        return lapsed
    }
}
