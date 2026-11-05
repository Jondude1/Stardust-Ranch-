# Stardust Ranch graphics improvement loop

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

## Runs
- 2026-11-05: v1.0.1 baseline — all art procedural, first playable build.
- 2026-11-05: Area 3 (trees/rocks/bushes/debris) — replaced procedural shapes with hand-authored palette grids in Sprites.kt (Px.grid): 48x80 clumped-canopy trees with seasonal palettes (blossom, fruit, amber, bare+snow), 32x64 tiered pines with winter snow rims, 5-tone rocks/ore/boulder/bush/weed/stick with soft ground shadows; canopies fade when the player is behind them; harness adds nature.png. v1.0.3
