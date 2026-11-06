package com.jondude.stardustranch.game

/**
 * Hand-authored decor objects for Stardust Ranch (original designs): the town fountain (3 animation frames),
 * shipping bin, bulletin board with pinned notes, rail fence, mine treasure chest, smithy anvil on a stump,
 * wildflower patches (palette swaps), the gift box, and the paper note + pin used by the board screen.
 * One char per pixel, '.' transparent, decoded with [Px.grid]. Light comes from the top-left; every material
 * has a 5-tone ramp with cooler (violet) shadows and warmer lights, and outlines are a dark tone of the local colour.
 */
object Decor {
    private fun c(v: Long) = v.toInt()
    private val SH = c(0x46101820)

    // ---- palettes ----
    private val stone = mapOf('o' to c(0xFF3A3448), 'k' to c(0xFF4A4058), 'd' to c(0xFF625A70), 'm' to c(0xFF7E7888), 'b' to c(0xFF9E98A2),
        'l' to c(0xFFC2BCB8), 'h' to c(0xFFE4DED2), 'w' to c(0xFF2E5E9E), 'W' to c(0xFF3F86D6), 'v' to c(0xFF8CC8F4), 'V' to c(0xFFEAF8FF),
        'c' to c(0xFFF0C850), 'C' to c(0xFFA87A2A), 's' to SH)
    private val wood = mapOf('o' to c(0xFF3A2228), 'D' to c(0xFF5E3A32), 'M' to c(0xFF84543A), 'B' to c(0xFFA8703F), 'L' to c(0xFFCC9452),
        'H' to c(0xFFE8BC72), 'i' to c(0xFF3E3E50), 'I' to c(0xFF6E7088), 'J' to c(0xFFA4A8BC), 'y' to c(0xFFF0C850), 'Y' to c(0xFFB07A2A),
        'Z' to c(0xFFFFF0A0), 'x' to c(0xFF2A2030), 's' to SH)
    private val redWood = wood + mapOf('H' to c(0xFFE88A5A), 'L' to c(0xFFC8603E), 'B' to c(0xFFA4483A), 'M' to c(0xFF803636),
        'D' to c(0xFF5E2A34), 'o' to c(0xFF34182A))
    private val giftPal = mapOf('o' to c(0xFF2A3A2E), 'H' to c(0xFFA8E08A), 'L' to c(0xFF7CC062), 'B' to c(0xFF5EA24A), 'M' to c(0xFF46843E),
        'D' to c(0xFF34643A), 'y' to c(0xFFF0C850), 'Y' to c(0xFFC08A30), 'Z' to c(0xFFFFF0A0), 's' to SH)
    private val boardPal = wood + mapOf('R' to c(0xFFC85A44), 'r' to c(0xFFA8443A), 'Q' to c(0xFF6E2C3A), 'c' to c(0xFFB88654),
        'C' to c(0xFFD4A46C), 'k' to c(0xFF946638), 'K' to c(0xFF7A5030), 'p' to c(0xFFF4ECD8), 'P' to c(0xFFCDBFA6), 'n' to c(0xFF9A8E9E),
        'x' to c(0xFFE04848), 'X' to c(0xFF902838), '3' to c(0xFFF2DC8A), '4' to c(0xFFC8A85A), '2' to c(0xFFC8E0F0), '5' to c(0xFF98B0C8))
    /** Wildflower colourways: rose, marigold, violet. */
    private val flowerPals = listOf(
        mapOf('o' to c(0xFF5A2A4A), 'P' to c(0xFFD8507A), 'p' to c(0xFFF890B0), 'c' to c(0xFFFFE07A)),
        mapOf('o' to c(0xFF6A4A1E), 'P' to c(0xFFE0A22A), 'p' to c(0xFFFFE070), 'c' to c(0xFFB05A2A)),
        mapOf('o' to c(0xFF3A2E6A), 'P' to c(0xFF7A68D8), 'p' to c(0xFFB4A8F8), 'c' to c(0xFFFFF4C0))
    ).map { it + mapOf('g' to c(0xFF6AB04A), 'G' to c(0xFF3E7E3A), 's' to c(0x3C101820)) }
    private val notePal = mapOf('o' to c(0xFF5A4636), 'h' to c(0xFFFFFAEE), 'p' to c(0xFFF4ECD8), 'P' to c(0xFFD2C2A4),
        'x' to c(0xFFE04848), 'X' to c(0xFF902838), 'Z' to c(0xFFFFB0A0), 'q' to c(0xFF8A8A98))

    /** 32 wide stone fountain: tiered bowl with a water jet, falling sheets, a block-walled basin with coins (frame 0). */
    val FOUNTAIN_0 = """
        ..............vvvv..............
        .............v.VV.v.............
        ............v..vV..v............
        ...............vv...............
        ...............Vv...............
        ...............vV...............
        ..............ovvo..............
        ..........oooolVvloooo..........
        .........ohhlWWWWWWbbbo.........
        ........ohhWwwwwwwwwWbbo........
        ........ohhWWWWWWWWWWbbo........
        .......vVohhlllllllbbboVv.......
        ........v.oobbllllddoo.v........
        ........v...oobbmdoo...v........
        ........V...oolbmdoo...V........
        .......vv...oolbmdoo...vv.......
        ........v...oolbmdoo...v........
        ........V.oooolbmdoooo.V........
        ......oovohllolbmdolllovoo......
        ....oohhhhhllolbmdollllllloo....
        ...ohhhhbbmmmolbmdommmmmllllo...
        ..ohhhbVwVwwwolbmdowwwVwmmlllo..
        .ohhhbwwwwwwwoooooowwwwwwwmlllo.
        olllbwwwWWWWohllbmdoWWWWwwwmmmbo
        olllwwVWWWWWWlbbmmdWWWWWWWwwbbmo
        omllhWWWWWvWooooooooWvWWWWWmmmdo
        obmllhWWVWWvWWWWWWWWvWWWcWlbbddo
        ombdllhhhcCWWWWWWWWWWWWmdmmmkdko
        olmmmmlllhhhhhhhlmlllllbmldddkdo
        obbmbbmmmdllllllmdmmmmdkkkddkddo
        obbbmmbbbmkdddddmdmmmmmdkdkkkddo
        .obbbbmmmddmmmmmmdmmmmdkkkddkdo.
        ..obbbbmlmddddddddddddmddddddoss
        ..sobbbmbmmmmmdmmmmmmdmdddddosss
        ...soobbbmmmmmdmmmmmmdmdddoossss
        ......oooommmmmmmmmmmmooooss....
        ..........oooooooooooo..........
    """.trimIndent().lines()

    val FOUNTAIN_1 = """
        ..............vvvv..............
        .............v.VV.v.............
        ............v..vV..v............
        ...............vv...............
        ...............Vv...............
        ...............vV...............
        ..............ovvo..............
        ..........oooolVvloooo..........
        .........ohhlWWWWWWbbbo.........
        ........ohhWwwwwwwwwWbbo........
        ........ohhWWWWWWWWWWbbo........
        ........vohhlllllllbbbov........
        ........v.oobbllllddoo.v........
        ........V...oobbmdoo...V........
        .......vv...oolbmdoo...vv.......
        ........v...oolbmdoo...v........
        ........V...oolbmdoo...V........
        ........v.oooolbmdoooo.v........
        ......ovvohllolbmdolllovvo......
        ....oohhhhhllolbmdollllllloo....
        ...ohhVhbbmmmolbmdommmmmlVllo...
        ..ohhhbbwwwwwolbmdowwwwwmmlllo..
        .ohhhbwwwwwwwoooooowwwwwwwmlllo.
        olllbwwwWWWWohllbmdoWWWWwVwmmmbo
        olllwwWWWWWWWlbbmmdWWWWWWWwwbbmo
        omllhWWWWWWWooooooooWWWWWWWmmmdo
        obmllhWWvWWWWWWWWWWWWWWvcWlbbddo
        ombdllhhhcCWWWWWWWWWWWvmdmmmkdko
        olmmmmlllhhhhhhhlmlllllbmldddkdo
        obbmbbmmmdllllllmdmmmmdkkkddkddo
        obbbmmbbbmkdddddmdmmmmmdkdkkkddo
        .obbbbmmmddmmmmmmdmmmmdkkkddkdo.
        ..obbbbmlmddddddddddddmddddddoss
        ..sobbbmbmmmmmdmmmmmmdmdddddosss
        ...soobbbmmmmmdmmmmmmdmdddoossss
        ......oooommmmmmmmmmmmooooss....
        ..........oooooooooooo..........
    """.trimIndent().lines()

    val FOUNTAIN_2 = """
        ..............vvvv..............
        .............v.VV.v.............
        ............v..vV..v............
        ...............vv...............
        ...............Vv...............
        ...............vV...............
        ..............ovvo..............
        ..........oooolVvloooo..........
        .........ohhlWWWWWWbbbo.........
        ........ohhWwwwwwwwwWbbo........
        ........ohhWWWWWWWWWWbbo........
        ........vohhlllllllbbbov........
        ........V.oobbllllddoo.V........
        .......vv...oobbmdoo...vv.......
        ........v...oolbmdoo...v........
        ........V...oolbmdoo...V........
        ........v...oolbmdoo...v........
        .......vv.oooolbmdoooo.vv.......
        ......ooVohllolbmdollloVoo......
        ....oohhhhhllolbmdollllllloo....
        ...ohhhhbbmmmolbmdommmmmllllo...
        ..ohhhbVwVwwwolbmdowwwVwmmlllo..
        .ohhhbwwwwwwwoooooowwwwwwwmlllo.
        olllbwwwWWWWohllbmdoWWWWwwwmmmbo
        olllwwWWWWWWWlbbmmdWWWWWWWwwbbmo
        omllhWWWWWVWooooooooWWWWWWWmmmdo
        obmllhWWWWWVWWWWWWWWWWWWcWlbbddo
        ombdllhhhcCWVWWWWWWWWWWmdmmmkdko
        olmmmmlllhhhhhhhlmlllllbmldddkdo
        obbmbbmmmdllllllmdmmmmdkkkddkddo
        obbbmmbbbmkdddddmdmmmmmdkdkkkddo
        .obbbbmmmddmmmmmmdmmmmdkkkddkdo.
        ..obbbbmlmddddddddddddmddddddoss
        ..sobbbmbmmmmmdmmmmmmdmdddddosss
        ...soobbbmmmmmdmmmmmmdmdddoossss
        ......oooommmmmmmmmmmmooooss....
        ..........oooooooooooo..........
    """.trimIndent().lines()

    /** Shipping bin: plank lid, iron band with rivets, vertical-plank front and a brass label plate. */
    val BIN = """
        ................
        ..oooooooooooo..
        .oHHHHHHHLLLLBo.
        .oLLLLLLLLLLBBo.
        .oMMMMMMMMMMMMo.
        .oHLLLLLLLLLBMo.
        oiJJJJJJJJJJJJio
        oiIIIIIIIIIIIIio
        oJoHLDLBDLBDBoIo
        oIoLBDyyyyBDMoio
        oJoLBDYYYYBDMoIo
        oIoLBDLBDLBDMoio
        oJoBMDBMDBMDMoIo
        oIoMMDMMDMMDDoio
        oiiDDDDDDDDDDiio
        .oooooooooooooo.
        ..ssssssssssss..
    """.trimIndent().lines()

    /** Town bulletin board (32 wide): shingled cap, framed cork panel, two posts. Notes are stamped on separately. */
    val BOARD = """
        ................................
        .....oooooooooooooooooooooo.....
        ...ooRRRRRRRRRRRRRRRRRRRRRRoo...
        ..oRRrrRRrrRRrrRRrrRRrrRRrrRQo..
        .oRrrrrrrrrrrrrrrrrrrrrrrrrrrQo.
        oRRrQrrrQrrrQrrrQrrrQrrrQrrrQQQo
        oQQQQQQQQQQQQQQQQQQQQQQQQQQQQQQo
        oooooooooooooooooooooooooooooooo
        .oDDDDDDDDDDDDDDDDDDDDDDDDDDDDo.
        .oHLooooooooooooooooooooooooBMo.
        .oHLokkkkkkkkkkkkkkkkkkkkkkoBMo.
        .oLLokcCcccccckcccccCccckckoBMo.
        .oLLocCccckcccccccCcccccccKoBMo.
        .oLLoccckcccccCccckcccccckcoBMo.
        .oLLokccCcccckcccccccCcckckoBMo.
        .oLLocCccccCccccckccccccccKoBMo.
        .oLLoccckccccckcccCcckccccKoBMo.
        .oLLokcccCccccccckccccCcckcoBMo.
        .oLLocCcckccccCccccccckcccKoBMo.
        .oLLoccccccckccccCcccccckccoBMo.
        .oLLockcCccccccckccccCcccckoBMo.
        .oLLoKKKKKKKKKKKKKKKKKKKKKKoBMo.
        .oLLooooooooooooooooooooooooBMo.
        .oLBDDDDDDDDDDDDDDDDDDDDDDDDBMo.
        .oooooooooooooooooooooooooooooo.
        ...oLBo..................oLBo...
        ...oLBo..................oLBo...
        ...oLBo..................oLBo...
        ...oLMo..................oLMo...
        ...oBMo..................oBMo...
        ..ssoooss...............ssooos..
        ....ssss..................sss...
    """.trimIndent().lines()

    val NOTE_WHITE = """
        ..xX...
        ppxpppP
        pnnnnpP
        pppppPP
        pnnnpPP
        ppppPP.
        PPPPP..
    """.trimIndent().lines()

    val NOTE_YELLOW = """
        ...xX..
        3333334
        3nnnn34
        3333334
        3nnn334
        3333344
        4444444
    """.trimIndent().lines()

    val NOTE_BLUE = """
        ..xX....
        22222225
        2nnnnn25
        22222225
        2nnn2225
        55555555
    """.trimIndent().lines()

    /** Rail fence that tiles left-right: lit post, two rails. */
    val FENCE = """
        ................
        ................
        ......oooo......
        .....oHLLMo.....
        .....oLLBMo.....
        ooooooLLBMoooooo
        LLLLLoLLBMoLLLLL
        BBBBBoLLBMoBBBBB
        MMMMMoLLBMoMMMMM
        ooooooLLBMoooooo
        .....oLLBMo.....
        ooooooLLBMoooooo
        LLLLLoLLBMoLLLLL
        MMMMMoLBBDoMMMMM
        ooooooMMDDoooooo
        sssssoooooosssss
    """.trimIndent().lines()

    /** Mine treasure chest: red wood, gold bands and lock plate. */
    val CHEST = """
        ................
        ................
        ...oooooooooo...
        ..oHHLLLLLLLBo..
        .oZHLLLLLLLBBYo.
        .oyLLLLLLLLBMYo.
        .oyDDDDDDDDDDYo.
        oZyyyyyoZZoyyYYo
        oyLLLLoZyYoBBMYo
        oyLBBBoyxYoBMMYo
        oyLBBBBooooBMMYo
        oyBBBBBBBBBMMMYo
        oyMMMMMMMMMMDDYo
        oZyyyyyyyyyyyYYo
        .oooooooooooooo.
        ..ssssssssssss..
    """.trimIndent().lines()

    /** Anvil with horn on a sawn stump. */
    val ANVIL = """
        ................
        ................
        ................
        .ooooooooooooo..
        oJJJJJJJJJJJIio.
        .oIIIIIIIIIIiio.
        ..oooiIIIiiooo..
        ....oiIIiio.....
        ...oJIIIiiio....
        ..oDDDDDDDDDDo..
        .oHLLLLLLBBBMMo.
        .oLLDLBBBDBMMDo.
        .oLBDLBBMDBMMDo.
        .oBMDBMMMDMMDDo.
        ..oooooooooooo..
        ..ssssssssssss..
    """.trimIndent().lines()

    /** A patch of three wildflowers (palette swaps per colourway). */
    val FLOWER = """
        ................
        ................
        ................
        ..........oPPo..
        ...oPPo..oPppPo.
        ..oPppPo.oPpcPo.
        ..oPpcPo..oPPo..
        ...oPPo....gG...
        ....gG..oPo.g...
        ...Ggg.oPcPoG...
        ....gG..oPo.g...
        .....g..gG..gG..
        ...gGgGggGggGg..
        ..ssGgsGgssGgs..
        ...ssssssssss...
        ................
    """.trimIndent().lines()

    /** Gift box with a ribbon and bow. */
    val GIFT = """
        ................
        ................
        .....oo..oo.....
        ....oZyooyZo....
        ....oyYooYyo....
        ..oooooyyooooo..
        .oHHLLLyYLLLBBo.
        .oLLLLLyYLLLBMo.
        .oDDDDDyYDDDDDo.
        .oHLLLLyYLLLBMo.
        .oLLLLLyYLLBBMo.
        .oLLLLLyYLBBMMo.
        .oBBBBByYBBMMMo.
        .oMMMMMyYMMMDDo.
        ..oooooooooooo..
        ..ssssssssssss..
    """.trimIndent().lines()

    /** Paper card for the board screen (9-slice, 3 px corners). */
    val NOTE_UI = """
        oooooooooooo
        ohhhhhhhhhpo
        ohppppppppPo
        ohppppppppPo
        ohppppppppPo
        ohppppppppPo
        ohppppppppPo
        ohppppppppPo
        ohppppppppPo
        ohppppppppPo
        opPPPPPPPPPo
        oooooooooooo
    """.trimIndent().lines()

    const val NOTE_K = 3
    /** Red push-pin for notes on the board screen. */
    val PIN = """
        .ooo.
        oZxXo
        oxxXo
        oXXXo
        .ooo.
        ..q..
        ..q..
    """.trimIndent().lines()

    fun fountain(f: Int): Px = Px.grid(listOf(FOUNTAIN_0, FOUNTAIN_1, FOUNTAIN_2)[f % 3], stone)
    fun bin(): Px = Px.grid(BIN, wood)
    fun board(): Px {
        val p = Px.grid(BOARD, boardPal)
        p.blit(Px.grid(NOTE_WHITE, boardPal), 6, 11)
        p.blit(Px.grid(NOTE_YELLOW, boardPal), 14, 10)
        p.blit(Px.grid(NOTE_BLUE, boardPal), 17, 16)
        return p
    }
    fun fence(): Px = Px.grid(FENCE, wood)
    fun chest(): Px = Px.grid(CHEST, redWood)
    fun anvil(): Px = Px.grid(ANVIL, wood)
    fun flower(i: Int): Px = Px.grid(FLOWER, flowerPals[i % flowerPals.size])
    fun gift(): Px = Px.grid(GIFT, giftPal)
    fun note(): Px = Px.grid(NOTE_UI, notePal)
    fun pin(): Px = Px.grid(PIN, notePal)
}
