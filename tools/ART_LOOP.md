# Stardust Ranch improvement loop (graphics + gameplay)

A scheduled task runs every 10 minutes. Each run makes ONE focused, visible graphics upgrade, checks it with
`tools/arttest/run.sh` (renders creatures.png, items.png, buildings.png and map PNGs), pushes, and logs a line below.

## Quality bar (Jon's requirement)
Every sprite must be at least as polished as Stardew Valley's own sprites — that is the bar for craft, NOT a look to copy.
Never trace, copy or imitate specific Stardew Valley or Pokémon sprites, characters, items or tiles. Designs stay original.

What "Stardew-level" means in practice:
- Hand-authored pixel art, not geometric shapes. Move sprites out of procedural shape code into palette-indexed
  text grids (one char per pixel) in `game/Sprites.kt`, decoded by a small helper in Px.kt. Keep procedural code
  only as a fallback.
- Proper sizes: 16x16 ground tiles; characters 16x32 with 4-frame walk cycles per direction and tool-use poses;
  trees about 48x80 with a separate trunk; buildings with real depth (roof overhang, shadowed eaves, trim, porches).
- Color ramps of 4-5 tones per material with hue-shifted shadows (cooler/purpler) and warmer highlights; no flat fills.
- Selective outlines (darker tone of the local color, not pure black everywhere), clean pixel clusters, no stray
  single pixels or pillow shading, consistent top-left light source.
- Ground tiles with variation and edge/transition tiles (grass to dirt, grass to water with shoreline, tilled soil
  that joins neighbours), animated water, seasonal palettes.
- Creatures with expressive faces, readable silhouettes, idle/bounce animation, and front-facing battle sprites.

## Areas (rotate; never repeat the area of either of the last two runs)
1. Creatures (procedural sprites in Art.monPx: shape, shading, faces, per-species details, battle poses)
2. Terrain tiles (grass/dirt/water/sand/cave/cobble, edge transitions, animated water, seasonal variety)
3. Trees, rocks, bushes, debris and decor objects
4. Buildings and interiors (house, barn, store, center, smithy, cottages, cave, furniture)
5. Player and villager sprites (detail, walk cycles, tool-use poses)
6. Crops and item icons
7. Effects and atmosphere (particles, tool impacts, splash, day/night lighting, weather, mine light)
8. Battle scene (backgrounds, platforms, attack animations, catch animation)
9. HUD and menus (panels, fonts, icons, hotbar, dialog boxes)

## Gameplay backlog (since 2026-11-06 every run also ships ONE gameplay feature)
Pick the highest item not yet done (mark done items with [x] and the release tag). Jon may reorder this list.
- [ ] Fishing: rod sold at the beach, simple timing/reel minigame, 15+ original fish by location, season, time and weather
- [ ] Creature produce: ranch creatures make goods over time (e.g. wool, honeydew, glowstones) collected from the pen, scaled by bond
- [ ] Cooking: kitchen in the farmhouse, recipes from crops/forage/fish that give buffs (speed, luck, mining, energy)
- [ ] Skills: Farming, Foraging, Mining, Fishing, Taming levels 1-10 with XP, level-up screen and a perk choice at 5 and 10
- [ ] Bulletin board quests in town: daily villager requests with gold, friendship and item rewards
- [ ] Ranger trials: 4 tamer challenges (one per season area) with multi-creature trainer battles and badges that unlock perks
- [ ] Overworld creature abilities: creatures of a type let you cross water, smash boulders, cut thorn hedges, light the mines; gate new spots behind them
- [ ] Farm buildings via a carpenter: coop-style creature barn upgrade, greenhouse (all-season crops), silo, shed; fruit trees
- [ ] Artisan machines: preserves jar, keg, cheese-style press, furnace for ore bars; crafted and placed on the farm
- [ ] Mine bosses every 25 floors, more enemy types with attack patterns, weapon tiers sold by Gus
- [ ] Seasonal festivals on fixed dates (e.g. Spring flower fair, Summer lantern night, Fall harvest show, Winter star fest) with mini-games
- [ ] Villager daily schedules, heart events at 2/4/6 hearts, and a collection museum for gems, fish and creatures
- [ ] Creature eggs and breeding at the ranch, held items, and a creature that can ride/carry you faster
- [ ] Sound effects and simple chiptune music (generated in code, no outside audio), with a volume option
- [ ] Quality of life: storage chests, map screen, journal of goals, tool charge-up, inventory sort, larger inventory upgrade

Gameplay rules: keep it fun and balanced (no huge gold/XP jumps), old saves must keep loading (new save fields optional
with defaults), every new feature must be reachable in play and explained once in-game (dialog, sign or note), and the
gameplay simulation in tools/arttest/Main.kt must be extended to exercise the new feature and print a check line.

## Runs
- 2026-11-05: v1.0.1 baseline — all art procedural, first playable build.
- 2026-11-05: Area 3 (trees/rocks/bushes/debris) — replaced procedural shapes with hand-authored palette grids in Sprites.kt (Px.grid): 48x80 clumped-canopy trees with seasonal palettes (blossom, fruit, amber, bare+snow), 32x64 tiered pines with winter snow rims, 5-tone rocks/ore/boulder/bush/weed/stick with soft ground shadows; canopies fade when the player is behind them; harness adds nature.png. v1.0.3
- 2026-11-05: Area 4 (buildings) — new Buildings.kt assembles every exterior from 6-tone ramps (cool shadows, warm lights): hip roofs with staggered shingles, lit/shaded hip planes, ridge caps and fascia; plank/board/stone walls with eave shadow and corner trim; hand-authored windows, shutters, flower boxes, panel + glass doors and shop/healer/smithy signs in Sprites.kt; gambrel barn with braced doors + hayloft; chiselled-rock cave with timber frame; 3 cottage variants. Roofs overhang 4px and rise into the row behind. Harness buildings sheet includes cottages; battle sim tolerates empty forest. v1.0.5
- 2026-11-05: Area 5 (player + villagers) — new People.kt builds every person from palette-indexed layer grids (legs, torso/arms, outfit, head, hair/hat): 16x32 bodies (were 16x24 boxes) with 4-tone ramps, cool shadows, coloured outlines; per-character straw hat, bald crown + white beard + cardigan buttons, long swept ginger hair, green cap + belt, tousled hair + beard + leather apron, plum bob + collar; 5 frames per direction (idle + 4-step walk: lifted foot + bob front/back, stride + arm swing in profile). Dialog/friends use head-and-shoulders portraits; harness adds people.png. (Old procedural person code in Art.kt is now unused.) v1.0.7
- 2026-11-06: Area 2 (terrain) — new Terrain.kt composes every ground cell from hand-authored 16x16 textures in Sprites.kt (grass tufts per season incl. snow, pebbled dirt/cave floor, rippled sand, clod-textured tilled soil, running-bond cobbles, drifting water waves, rubble rock tops + rock faces) and blends it with its 8 neighbours: ragged grass/snow lips with coloured outlines and cast shadows over paths and cobbles, earth bank + foam + shallows along every shoreline, lit rock faces and floor shadows where mine walls and mountain cliffs meet open ground, railed bridge planks. Cached per neighbourhood; harness renders through Terrain too. v1.0.9
