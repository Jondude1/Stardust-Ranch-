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

private var terrainCache: Terrain? = null

fun renderArea(art: Art, a: Area, season: Int, x0: Int, y0: Int, tw: Int, th: Int, s: Int, out: String, g: Game? = null): Sheet {
    val T = 16
    val terrain = terrainCache ?: Terrain(art).also { terrainCache = it }
    val sh = Sheet(tw * T * s, th * T * s); sh.fill(0xFF000000.toInt())
    for (y in y0 until y0 + th) for (x in x0 until x0 + tw) {
        if (!a.inb(x, y)) continue
        val b = terrain.tile(a, x, y, season, 0)
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
                Ob.FORAGE, Ob.GOODS -> art.item(a.aux[i]); Ob.FENCE -> art.fence; Ob.BIN -> art.bin; Ob.BOX -> art.box; Ob.COUNTER -> art.counter
                Ob.HEALER -> art.healer; Ob.ELEVATOR -> art.elevator; Ob.LADDER -> art.ladder; Ob.LADDERUP -> art.ladderUp
                Ob.FLOWER -> art.flower[a.aux[i] % 3]; Ob.TABLE -> art.table; Ob.ANVIL -> art.anvil; Ob.SHELF -> art.shelf; Ob.CHEST -> art.chest; Ob.STOVE -> art.stove
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
            for (n in g.npcs) if (n.area == a.id && n.y.toInt() == y) sh.draw(People.npc[n.key]!![0][0], ((n.x - x0) * T - 8).toInt() * s, ((n.y - y0) * T - 29).toInt() * s, s)
            if (g.cur === a && g.py.toInt() == y) {
                sh.draw(People.player[0][0], ((g.px - x0) * T - 8).toInt() * s, ((g.py - y0) * T - 29).toInt() * s, s)
                g.party.firstOrNull()?.let { sh.draw(art.mon(it.speciesId)[0], ((g.px - 1.2f - x0) * T - 16).toInt() * s, ((g.py - y0) * T - 30).toInt() * s, s) }
            }
        }
    }
    sh.save(out)
    return sh
}

// ---- HUD mock helpers (mirror Ui.nine / Ui.bar so the sheet matches the game) ----
fun Sheet.blend(X: Int, Y: Int, c: Int) {
    val al = c ushr 24; if (al == 0 || X !in 0 until w || Y !in 0 until h) return
    if (al == 255) { img.setRGB(X, Y, c); return }
    val d = img.getRGB(X, Y); val t = al / 255f
    fun ch(sh: Int) = (((c shr sh) and 255) * t + ((d shr sh) and 255) * (1 - t)).toInt()
    img.setRGB(X, Y, (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0))
}
fun Sheet.nine(p: Px, k: Int, l: Int, t: Int, r: Int, b: Int, s: Int, shadow: Boolean = true) {
    if (shadow) nineRaw(p, k, l + s, t + 2 * s, r + s, b + 2 * s, s, 0x58000000)
    nineRaw(p, k, l, t, r, b, s, 0)
}
fun Sheet.nineRaw(p: Px, k: Int, l: Int, t: Int, r: Int, b: Int, s: Int, tint: Int) {
    val kx = minOf(k * s, (r - l) / 2); val ky = minOf(k * s, (b - t) / 2)
    val xs = intArrayOf(l, l + kx, r - kx, r); val ys = intArrayOf(t, t + ky, b - ky, b)
    val sx = intArrayOf(0, k, p.w - k, p.w); val sy = intArrayOf(0, k, p.h - k, p.h)
    for (j in 0..2) for (i in 0..2) {
        val dw = xs[i + 1] - xs[i]; val dh = ys[j + 1] - ys[j]; if (dw <= 0 || dh <= 0) continue
        for (Y in 0 until dh) for (X in 0 until dw) {
            val c = p.get(sx[i] + X * (sx[i + 1] - sx[i]) / dw, sy[j] + Y * (sy[j + 1] - sy[j]) / dh)
            if (c ushr 24 == 0) continue
            blend(xs[i] + X, ys[j] + Y, if (tint != 0) tint else c)
        }
    }
}
fun Sheet.px(p: Px, x: Int, y: Int, s: Int) = draw(p.bmp(), x, y, s)
fun Sheet.text(str: String, x: Int, y: Int, size: Int, col: Int, center: Boolean = false, shadow: Boolean = false) {
    val gr = img.createGraphics()
    gr.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    gr.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, size)
    val xx = if (center) x - gr.fontMetrics.stringWidth(str) / 2 else x
    if (shadow) { gr.color = java.awt.Color(0, 0, 0, 160); gr.drawString(str, xx + 2, y + 2) }
    gr.color = java.awt.Color(col, true); gr.drawString(str, xx, y); gr.dispose()
}
fun Sheet.rect(l: Int, t: Int, r: Int, b: Int, c: Int) { for (y in t until b) for (x in l until r) blend(x, y, c) }
fun Sheet.bar(l: Int, t: Int, r: Int, b: Int, f: Float, col: Int, s: Int, vertical: Boolean = false) {
    rect(l, t, r, b, 0xFF2A1A16.toInt()); rect(l + s, t + s, r - s, b - s, 0xFF5A463C.toInt())
    val rp = Hud.barRamp(col)
    if (!vertical) {
        val fw = ((r - l - 2 * s) * f).toInt() / s * s; if (fw <= 0) return
        rect(l + s, t + s, l + s + fw, b - s, rp[2]); rect(l + s, t + 2 * s, l + s + fw, t + 3 * s, rp[1])
        rect(l + s, t + s, l + s + fw, t + 2 * s, rp[0]); rect(l + s, b - 2 * s, l + s + fw, b - s, rp[4])
    } else {
        val fh = ((b - t - 2 * s) * f).toInt() / s * s; if (fh <= 0) return
        val ft = b - s - fh
        rect(l + s, ft, r - s, b - s, rp[2]); rect(l + 2 * s, ft, l + 3 * s, b - s, rp[1])
        rect(l + s, ft, l + 2 * s, b - s, rp[0]); rect(r - 2 * s, ft, r - s, b - s, rp[4]); rect(l + s, ft, r - s, ft + s, rp[0])
    }
}

/** Mock of the in-game HUD (Ui.drawHud) at u = 2, hs = 3 over a farm render, plus the kitchen screen. */
fun renderHud(art: Art, g: Game, out: String) {
    val farm = g.areas["farm"]!!
    val bg = renderArea(art, farm, 0, 4, 2, 20, 12, 4, "$out/hud_bg.png", g)
    java.io.File("$out/hud_bg.png").delete()
    val sh = Sheet(1280, 720); sh.img.graphics.drawImage(bg.img, 0, -24, null)
    val u = 2; val hs = Hud.scaleFor(2f)
    val INK = 0xFF3A2416.toInt()
    // date plaque
    val l = 8 * u; val t = 8 * u; val pw = 158 * u; val ph = 76 * u
    sh.nine(Hud.panel(), Hud.PANEL_K, l, t, l + pw, t + ph, hs)
    val ix = l + 12 * u
    sh.px(Hud.season(0), ix, t + 11 * u, hs); sh.text("Spring 12 · Y1", ix + 9 * hs + 5 * u, t + 23 * u, 26, INK)
    sh.text("9:40 am", ix, t + 45 * u, 32, INK); sh.px(Hud.sun, l + pw - 12 * u - 11 * hs, t + 46 * u - 11 * hs, hs)
    sh.rect(ix, t + 50 * u, l + pw - 12 * u, t + 50 * u + 1, 0xFFD8BE8A.toInt())
    sh.px(Hud.coin, ix, t + 53 * u, hs); sh.text("1,250g", ix + 9 * hs + 5 * u, t + 65 * u, 26, 0xFF8A6A12.toInt())
    sh.text("Farm", l + pw - 12 * u - 30, t + 65 * u, 20, 0xFF6E5A48.toInt())
    var py = t + ph + 5 * u
    sh.nine(Hud.panel(), Hud.PANEL_K, l, py, l + pw, py + 44 * u, hs)
    sh.draw(art.mon(g.party[0].speciesId)[0], l + 8 * u, py + 6 * u, u)
    sh.text("${g.party[0].name} Lv${g.party[0].level}", l + 44 * u, py + 21 * u, 21, INK)
    sh.bar(l + 44 * u, py + 25 * u, l + pw - 12 * u, py + 33 * u, 0.7f, 0xFF5DD45A.toInt(), 2)
    py += 49 * u
    sh.nine(Hud.panel(), Hud.PANEL_K, l, py, l + pw, py + 36 * u, hs)
    sh.px(Hud.buff(Buff.SPEED), l + 12 * u, py + 18 * u - 9 * hs / 2, hs); sh.text("Swift  2h 35m", l + 12 * u + 9 * hs + 6 * u, py + 23 * u, 24, 0xFF3E7A2A.toInt())
    // energy / health
    val W = 1280; val H = 720
    val el = W - 52 * u; val et = 8 * u; val eb = 164 * u
    sh.nine(Hud.panel(), Hud.PANEL_K, el, et, W - 8 * u, eb, hs)
    val cx1 = el + 16 * u; val cx2 = el + 30 * u
    sh.px(Hud.bolt(), cx1 - 8 * hs / 2, et + 11 * u, hs); sh.px(Hud.heart(true), cx2 - 9 * hs / 2, et + 12 * u, hs)
    sh.bar(cx1 - 5 * u, et + 32 * u, cx1 + 5 * u, eb - 11 * u, 0.8f, 0xFF5DC44A.toInt(), 2, true)
    sh.bar(cx2 - 5 * u, et + 32 * u, cx2 + 5 * u, eb - 11 * u, 1f, 0xFFE2483A.toInt(), 2, true)
    // hotbar
    val slot = 38 * u; val n = 8
    val hx = W / 2 - slot * n / 2 - 18 * u; val hy = H - slot - 12 * u
    sh.nine(Hud.panel(), Hud.PANEL_K, hx - 10 * u, hy - 10 * u, hx + slot * n + 38 * u, hy + slot + 10 * u, hs)
    for (i in 0 until n) {
        val sx = hx + i * slot
        sh.nine(Hud.slot(i == 2), Hud.SLOT_K, sx + u, hy + u, sx + slot - u, hy + slot - u, hs, false)
        val s = g.inv.slots[i] ?: continue
        sh.draw(art.item(s.id), sx + 5 * u + 3, hy + 4 * u + 3, 4)
        if (s.n > 1) sh.text("${s.n}", sx + slot - 22, hy + slot - 8, 20, 0xFFFFFFFF.toInt(), shadow = true)
    }
    val rx = hx + slot * n + 4 * u
    sh.nine(Hud.button(0xFFB0783E.toInt()), Hud.BUTTON_K, rx, hy + 3 * u, rx + 24 * u, hy + slot - 3 * u, hs, false)
    sh.text("1", rx + 12 * u, hy + slot / 2 + 8, 28, 0xFFFFFFFF.toInt(), true, true)
    // A / Menu buttons
    val ad = Math.round(62f * u / hs)
    sh.px(Hud.round(ad, 0xFF5DA342.toInt()), W - 62 * u - 18 * u, H - 62 * u - 34 * u, hs)
    sh.text("Use", W - 49 * u, H - 61 * u, 28, 0xFFFFFFFF.toInt(), true, true)
    val bd = Math.round(62f * 0.62f * u / hs)
    sh.px(Hud.round(bd, 0xFFB0783E.toInt()), W - (62 * 0.62f).toInt() * u - 26 * u - 62 * u, H - (62 * 0.62f).toInt() * u - 72 * u, hs)
    // dialog sample
    val dl = 40 * u; val dr = W - 40 * u; val dt = H - 134 * u - 60 * u; val db = H - 12 * u - 60 * u
    sh.nine(Hud.panel(), Hud.PANEL_K, dl, dt, 640, db, hs)
    sh.nine(Hud.button(0xFF9A5E30.toInt()), Hud.BUTTON_K, dl + 14 * u, dt - 14 * u, dl + 84 * u, dt + 14 * u, hs, false)
    sh.text("Wren", dl + 49 * u, dt + 4 * u, 26, 0xFFFFF0D0.toInt(), true, true)
    sh.text("Morning! The stove in your kitchen", dl + 18 * u, dt + 36 * u, 28, INK)
    sh.text("works again, you know.", dl + 18 * u, dt + 55 * u, 28, INK)
    sh.px(Hud.arrow, 640 - 22 * u - 7 * hs / 2, db - 22 * u, hs)
    for ((k, h) in listOf(true, true, true, false, false).withIndex()) sh.px(Hud.heart(h), 700 + k * (9 * hs + 8), 520, hs)
    sh.save("$out/hud.png")

    // kitchen screen
    val ks = Sheet(1280, 720); ks.img.graphics.drawImage(bg.img, 0, -24, null); ks.rect(0, 0, 1280, 720, 0x80000000.toInt())
    val ow = (1280 * 0.86f).toInt(); val oh = (720 * 0.86f).toInt(); val ol = (1280 - ow) / 2; val ot = (720 - oh) / 2
    ks.nine(Hud.panel(), Hud.PANEL_K, ol, ot, ol + ow, ot + oh, hs)
    ks.text("Kitchen", ol + 20 * u, ot + 32 * u, 36, 0xFF8E3A1E.toInt())
    ks.nine(Hud.button(0xFFC0503A.toInt()), Hud.BUTTON_K, ol + ow - 50 * u, ot + 12 * u, ol + ow - 14 * u, ot + 44 * u, hs, false)
    ks.px(Hud.xmark, ol + ow - 32 * u - 9 * hs / 2, ot + 27 * u - 9 * hs / 2 - hs, hs)
    val al = ol + 18 * u; val at = ot + 54 * u; val ab = ot + oh - 30 * u
    val size = minOf((ab - at) / 3, ((ow - 36 * u) * 0.42f / 3).toInt())
    for ((i, d) in Cooking.list.withIndex()) {
        val x = al + (i % 3) * size; val y = at + (i / 3) * size
        ks.nine(Hud.slot(i == 4), Hud.SLOT_K, x + 2 * u, y + 2 * u, x + size - 2 * u, y + size - 2 * u, hs, false)
        val isc = (size - 16 * u) / 16; ks.draw(art.item(d.id), x + (size - 16 * isc) / 2, y + 6 * u, isc)
        ks.px(Hud.buff(d.buff), x + size - 6 * u - 9 * hs, y + size - 6 * u - 9 * hs, hs)
    }
    val d = Cooking.list[4]; val dx = al + size * 3 + 22 * u; val dr2 = ol + ow - 18 * u
    ks.draw(art.item(d.id), dx, at, 6)
    ks.text(d.name, dx + 58 * u, at + 20 * u, 32, INK); ks.text("+${d.energy} energy · sells ${d.price}g", dx + 58 * u, at + 40 * u, 22, 0xFF6E5A48.toInt())
    var y = at + 58 * u
    ks.px(Hud.buff(d.buff), dx, y, hs); ks.text("${d.buff.label} buff · ${d.minutes / 60} hours", dx + 9 * hs + 6 * u, y + 9 * hs - 2 * u, 26, 0xFF3E7A2A.toInt())
    y += 9 * hs + 14 * u; ks.text(d.buff.effect.replaceFirstChar { it.uppercase() }, dx, y, 22, 0xFF6E5A48.toInt()); y += 10 * u
    ks.text("Ingredients", dx, y + 6 * u, 24, 0xFF8E3A1E.toInt()); y += 12 * u
    for ((id, nn) in d.needs) {
        ks.nine(Hud.row(false), Hud.ROW_K, dx, y, dr2, y + 28 * u, hs, false)
        ks.draw(art.item(id), dx + 6 * u + 4, y + 4 * u + 4, 2)
        ks.text(Cooking.ingName(id), dx + 32 * u, y + 19 * u, 24, INK); ks.text("$nn/$nn", dr2 - 30 * u, y + 19 * u, 24, 0xFF3E7A2A.toInt())
        y += 30 * u
    }
    val bt = maxOf(y + 6 * u, ab - 34 * u)
    ks.nine(Hud.button(0xFF5DA342.toInt()), Hud.BUTTON_K, dx, bt, dx + 120 * u, bt + 34 * u, hs, false)
    ks.text("Cook", dx + 60 * u, bt + 21 * u, 28, 0xFFFFFFFF.toInt(), true, true)
    ks.save("$out/kitchen.png")
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
    val people = listOf(People.player) + People.npc.values
    for ((pi, fr) in people.withIndex()) for (d in 0..3) is_.draw(fr[d][1], (pi * 4 + d) * 17 * 3, oy, 3)
    is_.save("$out/items.png")
    // big icon sheet (5x) for judging icon detail: every item id in order, 12 per row
    val ic = Sheet(12 * 18 * 5, ((ids.size + 11) / 12) * 18 * 5); ic.fill(0xFFF6E7C4.toInt())
    for ((k, id) in ids.withIndex()) ic.draw(art.item(id), (k % 12) * 18 * 5 + 5, (k / 12) * 18 * 5 + 5, 5)
    ic.save("$out/icons.png")
    // people sheet: every character, every direction x frame (idle + 4 walk), plus portraits
    val ps = Sheet(20 * 18 * 4, people.size * 34 * 4); ps.fill(0xFF78B45A.toInt())
    for ((pi, fr) in people.withIndex()) for (d in 0..3) for (f in 0..4) ps.draw(fr[d][f], ((d * 5 + f) * 18) * 4, (pi * 34 + 1) * 4, 4)
    ps.save("$out/people.png")
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

    // battle scenes: backdrop + platforms + creatures laid out like Ui.drawBattle (wild back-right, yours front-left)
    val scenes = listOf(Triple(BattleArt.FOREST, 0, false), Triple(BattleArt.FOREST, 2, false), Triple(BattleArt.FOREST, 3, true),
        Triple(BattleArt.BEACH, 1, false), Triple(BattleArt.MOUNTAIN, 3, false), Triple(BattleArt.MINE, 0, false))
    val bsS = 2; val bsh = Sheet(2 * BattleArt.BW * bsS + 6, 3 * BattleArt.BH * bsS + 6); bsh.fill(0xFF000000.toInt())
    for ((k, sc) in scenes.withIndex()) {
        val ox = (k % 2) * (BattleArt.BW * bsS + 6); val oy = (k / 2) * (BattleArt.BH * bsS + 3)
        bsh.draw(BattleArt.backdrop(sc.first, sc.second, sc.third).bmp(), ox, oy, bsS)
        val pl = BattleArt.platform(sc.first, sc.second).bmp()
        bsh.draw(pl, ox + (140 - 40) * bsS, oy + (54 - 9) * bsS, bsS); bsh.draw(pl, ox + (52 - 40) * bsS, oy + (86 - 9) * bsS, bsS)
        bsh.draw(art.mon(listOf(21, 13, 28, 23, 29, 9)[k])[2], ox + (140 - 16) * bsS, oy + (54 - 30) * bsS, bsS)
        bsh.draw(art.mon(listOf(0, 3, 6, 0, 3, 6)[k])[0], ox + (52 - 16) * bsS, oy + (86 - 30) * bsS, bsS)
    }
    bsh.save("$out/battle.png")

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
    // fishing sim: beach sign intro, buy a rod from Mira, cast off the pier, hook the bite, reel with timed taps
    g.gold += 600
    g.warp("beach", 10.5f, 20.5f, 3)
    val introShown = g.mode == Mode.DIALOG && g.fishIntro
    g.dialog = null; g.mode = Mode.PLAY
    val goldBefore = g.gold
    g.buyRod(); val rodTalk = g.dialog?.pages?.size ?: 0; g.dialog = null; g.mode = Mode.PLAY
    println("fishing: intro=$introShown rod=${g.hasRod()} paid=${goldBefore - g.gold} miraPages=$rodTalk")
    val rr = java.util.Random(7)
    var casts = 0; var hooked = 0
    while (casts < 12) {
        casts++
        g.px = 10.5f; g.py = 20.5f; g.dir = 3
        g.sel = g.inv.slots.indexOfFirst { it?.id == Tool.ROD }
        g.energy = g.maxEnergy.toFloat(); g.time = 600f
        g.action()
        if (g.mode != Mode.FISH) { println("fishing: cast failed mode=${g.mode} banner=${g.banner}"); break }
        var guard = 0
        while (g.fishing?.phase == 0 && guard++ < 400) g.update(0.05f, 0f, 0f)
        g.fishTap()
        if (g.fishing?.phase == 2) hooked++
        guard = 0
        while (g.fishing != null && guard++ < 6000) {
            g.update(0.01f, 0f, 0f)
            val fs = g.fishing ?: break
            if (fs.inZone() && rr.nextFloat() < 0.5f) g.fishTap() else if (rr.nextFloat() < 0.0015f) g.fishTap()
        }
        if (g.mode == Mode.DIALOG) { g.dialog = null; g.mode = Mode.PLAY }
    }
    val landed = g.fishCaught.values.sum()
    println("fishing: casts=$casts hooked=$hooked landed=$landed species=${g.fishCaught.keys.map { Items[it].name }} inv=${Fish.list.sumOf { g.inv.count(it.id) }}")
    // fish tables respect place, season, hour and weather
    g.season = 1; g.raining = false; g.time = 700f
    val sumDay = (0 until 300).map { g.rollFish(Loc.OCEAN).name }.toSet()
    g.time = 1300f; g.season = 2
    val lakeNight = (0 until 300).map { g.rollFish(Loc.LAKE).name }.toSet()
    g.raining = true; g.time = 800f; g.season = 0
    val rainRiver = (0 until 300).map { g.rollFish(Loc.RIVER).name }.toSet()
    g.season = 3; g.raining = false
    val winterPond = (0 until 300).map { g.rollFish(Loc.POND).name }.toSet()
    println("fishing tables: summerDayOcean=$sumDay fallNightLake=$lakeNight springRainRiver=$rainRiver winterPond=$winterPond")
    val fishOk = introShown && g.hasRod() && landed > 0 && hooked > 0 && "Lantern Eel" !in sumDay && "Storm Ray" !in sumDay &&
        "Starlit Sturgeon" in lakeNight && "Mossback Catfish" in rainRiver && "Lily Koi" !in winterPond
    println("CHECK fishing: " + if (fishOk) "PASS" else "FAIL")
    g.season = 0; g.raining = false; g.time = 600f
    // creature goods sim: three ranch creatures with low / high / mid bond, six nights, then pick one up
    val farmA = g.areas["farm"]!!
    for (i in farmA.obj.indices) if (farmA.obj[i] == Ob.GOODS) farmA.obj[i] = Ob.NONE
    g.ranch.clear()
    g.ranch.add(Mon(33, 10).apply { bond = 10 }); g.ranch.add(Mon(6, 12).apply { bond = 150 }); g.ranch.add(Mon(9, 12).apply { bond = 60 })
    var introLines = 0; var nightsWithGoods = 0
    repeat(6) { g.endDay(false); g.mode = Mode.PLAY; introLines += g.report.count { it.startsWith("NEW: Happy ranch") }; if (g.report.any { it.contains("in the pen") }) nightsWithGoods++ }
    val goodsTiles = farmA.obj.indices.filter { farmA.obj[it] == Ob.GOODS }
    val kinds = goodsTiles.map { Items[farmA.aux[it]].name }.groupingBy { it }.eachCount()
    val inPen = goodsTiles.all { it % farmA.w in 29..40 && it / farmA.w in 10..15 }
    val t0 = goodsTiles.first(); val gid = farmA.aux[t0]; val have0 = g.inv.count(gid) + g.ranchBox.count(gid)
    g.cur = farmA; g.px = t0 % farmA.w + 0.5f; g.py = t0 / farmA.w - 0.5f; g.dir = 0
    g.action()
    val picked = g.inv.count(gid) + g.ranchBox.count(gid) == have0 + 1 && farmA.obj[t0] == Ob.NONE
    println("goods: nights=$nightsWithGoods/6 intro=$introLines tiles=${goodsTiles.size} inPen=$inPen kinds=$kinds picked=${Items[gid].name}:$picked sell=${Items[gid].sell}g")
    val goodsOk = introLines == 1 && goodsTiles.size >= 6 && inPen && picked && kinds.size == 3
    println("CHECK goods: " + if (goodsOk) "PASS" else "FAIL")
    renderArea(art, farmA, 0, 26, 6, 18, 12, 4, "$out/pen_goods.png")
    // cooking sim: stove in the farmhouse, first-use intro, cook three dishes (one with "any fish"), eat for buffs
    val house = g.areas["house"]!!
    val stoveOk = house.o(10, 2) == Ob.STOVE
    g.cur = house; g.px = 10.5f; g.py = 3.5f; g.dir = 1; g.mode = Mode.PLAY
    val tipShown = g.cookTip
    g.action()
    val introPages = if (g.mode == Mode.DIALOG) g.dialog?.pages?.size ?: 0 else 0
    g.dialog?.let { d -> g.dialog = null; g.mode = Mode.PLAY; d.onClose?.invoke() }
    val opened = g.mode == Mode.COOK
    g.mode = Mode.PLAY; g.action(); val reopenedNoIntro = g.mode == Mode.COOK
    g.give(Items.produce(4), 2, true); g.give(Items.FIBER, 1, true); g.give(Items.produce(8), 1, true); g.give(54, 1, true)
    val fishBefore = Fish.list.sumOf { g.inv.count(it.id) }
    val cheapest = Fish.list.filter { g.inv.count(it.id) > 0 }.minByOrNull { it.price }!!
    val cheapBefore = g.inv.count(cheapest.id)
    val soup = Cooking.byId[163]!!; val gfish = Cooking.byId[162]!!; val pie = Cooking.byId[165]!!; val tart = Cooking.byId[164]!!
    val cooked = listOf(g.cook(soup), g.cook(gfish), g.cook(pie))
    val tartRefused = !g.cook(tart)
    val fishUsed = fishBefore - Fish.list.sumOf { g.inv.count(it.id) } == 1 && g.inv.count(cheapest.id) == cheapBefore - 1
    val dishesInBag = listOf(163, 162, 165).all { g.inv.count(it) == 1 } && g.inv.count(Items.produce(4)) == 0
    // eat the pie from the hotbar: Miner buff, +1 pick power
    g.mode = Mode.PLAY; g.energy = 50f
    g.sel = g.inv.slots.indexOfFirst { it?.id == 165 }; g.useHeld(); g.dialog!!.options.first().second.invoke(); g.dialog = null; g.mode = Mode.PLAY
    val pieBuff = g.buff == Buff.MINING && g.energy >= 50f + pie.energy - 1 && g.inv.count(165) == 0
    g.enterMine(2)
    val rockI = g.cur.obj.indices.first { g.cur.obj[it] == Ob.ROCK && g.cur.obj.getOrNull(it + g.cur.w) == Ob.NONE }
    g.cur.aux[rockI] = 10; g.px = rockI % g.cur.w + 0.5f; g.py = rockI / g.cur.w + 1.5f; g.dir = 1
    g.sel = g.inv.slots.indexOfFirst { it?.id == Tool.PICK }; g.action()
    val minerHit = 10 - g.cur.aux[rockI]   // basic pick 1 + buff 1 (+1 if lead is a miner)
    // stamina halves tool energy; speed makes you walk further
    g.buff = Buff.STAMINA; g.buffUntil = g.time + 60; val e0 = g.energy; g.action(); val stamCost = e0 - g.energy
    g.warp("farm", 20.5f, 20.5f, 0); g.mode = Mode.PLAY
    for (x in 18..28) for (y in 19..22) { g.cur.setO(x, y, Ob.NONE); g.cur.crops.remove(g.cur.idx(x, y)); if (g.cur.g(x, y) != Gr.GRASS) g.cur.setG(x, y, Gr.GRASS) }
    g.buff = null; val x0 = g.px; repeat(10) { g.update(0.05f, 1f, 0f) }; val plain = g.px - x0
    g.px = 20.5f; g.py = 20.5f; g.buff = Buff.SPEED; g.buffUntil = g.time + 60; val x1 = g.px; repeat(10) { g.update(0.05f, 1f, 0f) }; val fast = g.px - x1
    // buff wears off when its time is up, and at bedtime
    g.buffUntil = g.time + 1f; repeat(60) { g.update(0.05f, 0f, 0f) }; val expired = g.buff == null
    g.buff = Buff.LUCK; g.buffUntil = g.time + 300; g.endDay(false); g.mode = Mode.PLAY
    val clearedAtNight = g.buff == null && g.report.none { it.startsWith("NEW: Grandma's old stove") }
    println("cooking: stove=$stoveOk tipInReport=$tipShown intro=$introPages opened=$opened reopen=$reopenedNoIntro cooked=$cooked tartRefused=$tartRefused fishUsed=$fishUsed(${cheapest.name}) bag=$dishesInBag")
    println("cooking: pieBuff=$pieBuff minerHit=$minerHit stamCost=$stamCost speed=${"%.2f".format(plain)}->${"%.2f".format(fast)} expired=$expired night=$clearedAtNight count=${g.dishesCooked}")
    val cookOk = stoveOk && tipShown && introPages == 3 && opened && reopenedNoIntro && cooked.all { it } && tartRefused && fishUsed && dishesInBag &&
        pieBuff && minerHit >= 2 && stamCost in 0.9f..1.1f && fast > plain * 1.15f && expired && clearedAtNight && g.dishesCooked == 3
    println("CHECK cooking: " + if (cookOk) "PASS" else "FAIL")
    // save/load roundtrip
    g.save()
    val g2 = Game(f)
    println("loaded cooking: tip=${g2.cookTip} intro=${g2.cookIntro} cooked=${g2.dishesCooked} stove=${g2.areas["house"]!!.o(10, 2) == Ob.STOVE}")
    println("loaded goods: ${g2.areas["farm"]!!.obj.count { it == Ob.GOODS }} (was ${farmA.obj.count { it == Ob.GOODS }}) tip=${g2.goodsIntro}")
    println("loaded fish: caught=${g2.fishCaught} intro=${g2.fishIntro} rod=${g2.hasRod()} (match=${g2.fishCaught == g.fishCaught})")
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
    renderHud(art, g, out)
    // close-up near the house at gameplay zoom
    g.cur = farm; g.px = 12.5f; g.py = 10.5f
    renderArea(art, farm, 0, 4, 2, 20, 12, 5, "$out/farm_close.png", g)
    println("DONE")
}
