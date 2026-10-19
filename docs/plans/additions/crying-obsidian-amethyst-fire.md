# Build Plan: Crying Obsidian and Amethyst Fire

## Status

- 2026-10-18 — Done — the owner reviewed the rework in game and passed it: "that
  actually looks like crying obsidian got manufactured into a new form. the fires
  are exactly what i was thinking. the bars are amazing. the top grates look
  great. this is a pass by miles". All checks pass (Rework verification addendum).
- 2026-10-18 — Rework built and checked — waiting on the owner's in-game look
  (Rework verification addendum).
- 2026-10-18 — Rework accepted — after the owner's in-game review: a recoloured
  amethyst flame and particle, and still, varied crying stains (Rework addendum).
- 2026-10-18 — Built — both slices built and checked (Verification addendum). The
  amethyst flame reads blue-violet in game, not purple; the owner decides between
  keeping the tint and drawing an original purple flame (Open question addendum).
- 2026-10-18 — Accepted — the owner picked every recommended option (PRD Content
  addendum; recipe contract amendment). Requested by the owner's friend; part of 1.0.

## Scope

Two additions, built as two slices. Each slice ends with the standard check suite,
the client game tests (`lore/policies/verification.md`), a mutation check of each new
test (test-authoring Rule 1), and screenshots.

## Slice A — Amethyst Fire

**Goal:** a fourth, purple fire type for every brazier, from a new Amethyst Campfire.

- **Fire type:** amethyst. Light 12, item prefix `amethyst_fire_`, no lava pops.
- **Amethyst Campfire:** crafted with an amethyst shard (contract R7). It
  cooks, smokes and damages like vanilla's campfires. It drops itself with Silk
  Touch, otherwise 2 charcoal, like the Copper Campfire.
- **Flame:** vanilla's soul campfire flame tinted purple, on the campfire, on
  braziers in every placement, and on items, as copper fire is done.
- **Wall and hanging flame particle:** a purple flame particle that reuses
  vanilla's soul fire flame sprite, tinted, since vanilla has no purple flame
  particle.
- **Embers:** a purple glowing ember bed.
- **Data:** recipes (contract R2, R4, R7, R8), loot, tags (campfires,
  `mineable/axe`), English names, creative tab.

**Tests:**
- the contract test (79 recipes);
- the flame-tint check (amethyst fire draws the tinted soul flame);
- the fire-particle check (purple flame on wall and hanging braziers, no lava
  pops);
- the art check (every new state has its art).

## Slice B — Crying Obsidian

**Goal:** crying obsidian bars, and a crying variant of the obsidian brazier,
both with tears.

- **Crying obsidian bars:** vanilla bars models with an animated texture of
  purple tear streaks creeping down; they drip tears. Same hardness and mining
  as the other mod bars (stone pickaxe or better).
- **Crying obsidian brazier:** the obsidian tier (explosion-proof, cooking
  1.5×). Its brazier texture carries the same creeping streaks, and it drips
  tears from its slats and basin.
- **Tears:** vanilla's dripping obsidian tear particle, the one the crying
  obsidian block uses. It hangs, falls and splashes.
- **Data:** recipes (contract R5, R6), loot, tags, names, creative tab.

**Tests:**
- the contract test;
- a game test that the crying brazier has obsidian's specials;
- a client check that crying bars and braziers spawn dripping tears and plain
  obsidian doesn't;
- the art check.

## Acceptance

All checks pass. The owner looks at both in game.

> **Addendum (2026-10-18, verification of slices A and B):** Both slices were
> built in one pass, since they share the variant, data and test changes.
>
> - **Check suite** (`lore/policies/verification.md`): datagen leaves the
>   generated files unchanged; `./gradlew clean build` compiles with no
>   warnings, the unit tests pass 8/8 and the game tests 30/30; the client game
>   tests pass; the client log has no mod warnings; the provider-memory and
>   AGENTS.md budget checks pass.
> - **Generated content:** 16 blockstates (14 braziers, 2 mod campfires), 79
>   recipes.
> - **New and extended checks:**
>   - contract test: 79 recipes, from the amendment;
>   - recipe game tests: 79 load; crying bars, the Amethyst Campfire and a
>     crying amethyst-fire brazier craft;
>   - specials, mining and cooking: the crying brazier survives TNT, can't be
>     pushed, mines with stone or better and cooks at obsidian's 1.5x; crying
>     bars mine with stone or better; the Amethyst Campfire drops with anything;
>   - fire particles: amethyst braziers on walls and ceilings show the purple
>     flame particle; the Amethyst Campfire never pops lava;
>   - tears: crying bars, an unlit crying brazier and a hanging one shed
>     vanilla's dripping tears; plain obsidian ones don't; no tear forms inside
>     the ground or the ledge;
>   - art: every new state has its models and textures; amethyst fire is the
>     soul flame tinted `#d080ff` on the campfire and in every placement; the
>     crying bars and brazier draw their own animated textures, plain obsidian's
>     are still.
> - **Mutation checks** (test-authoring Rule 1), each caught:
>   - headless, in one run: the Amethyst Campfire recipe made with a copper
>     nugget (contract test); crying bars made with obsidian (recipe grid); crying
>     bars dropped from the stone-tool tag (mining); the crying brazier given
>     iron's properties (explosion) and iron's tier (cooking, 480 ticks);
>   - client, one run each: the crying brazier sheds no tears; plain obsidian
>     cries; tears form inside neighbouring blocks; the crying texture is still.
> - **Screenshots** in `temp/art/screenshots/`: `crying-tears-1`, `-2`,
>   `art-copper-and-amethyst-fire`, `art-tiers`, `art-tiers-night`,
>   `art-bars`, `art-bars-items`, `art-copper-items`,
>   `slice6-wall-and-hanging-2`, `slice6-floor-2`.

> **Addendum (2026-10-18, open question: the amethyst flame's colour):** In game
> the tinted soul flame reads blue-violet. Only its white core turns
> pink-purple, as the preview showed. A tint multiplies colours, and most of the
> soul flame's pixels are teal with almost no red (`#13afb3`, `#088f92`), so
> no tint can turn them purple. Measured over the flame's pixels, the best
> tint makes about a third of them purple. The choices are to keep the
> blue-violet tint, or to draw an original animated purple flame and flame
> particle (allowed: original art, PRD License).

> **Addendum (2026-10-18, rework after the owner's in-game review; PRD License
> addendum on recolouring and Content erratum):**
>
> - **Amethyst flame and particle:** a sprite source of the mod's own,
>   `mcbraziers:recolor` (`RecoloredSprite`), recolours the player's own
>   vanilla textures at load, keeping their animation. Vanilla's
>   `paletted_permutations` source can't be used: it flattens animations.
>   - Each pixel's brightness, scaled over the texture's own darkest to
>     brightest pixel, picks a colour on one ramp, crying obsidian's dark
>     purples up to amethyst's light ones.
>   - The block flame recolours `block/soul_campfire_fire`, and the particle
>     `particle/soul_fire_flame`. Both are untinted.
>   - The generator writes the ramp into both atlas files, so it lives in one
>     place.
> - **Crying stains:** no animation.
>   - The brazier's slats take their own texture, `crying_obsidian_brazier_tears`:
>     eight 2 px strips of slat, each with its stain in a different place.
>     Strips are read from the bottom, so the pool at the foot shows on every
>     slat height.
>   - Each slat gets its own strip, and the four slats of a side all differ.
>   - The generator writes three arrangements of the slat parts, and each
>     crying brazier picks one by its position. The game reseeds each part of
>     a multipart model alike, so one brazier uses one arrangement throughout.
>   - Crying bars come in four stain versions, each block picking one by its
>     position, the same way. The generator writes their blockstate and
>     models from the obsidian bars' files.
>   - The brazier's own texture keeps still beads on the rim and the basin.
> - **Tests:**
>   - the art check moves from "animated" to "still": every slat draws its own
>     strip, the four of a side differ, positions vary the arrangement, and bars
>     versions vary between blocks but stay consistent within one;
>   - the amethyst flame and particle draw purple, animated recolours of
>     vanilla's, untinted.

> **Addendum (2026-10-18, verification of the rework):**
>
> - **Check suite:**
>   - datagen and the art generator both leave their files unchanged;
>   - `./gradlew clean build` compiles with no warnings, unit tests pass 8/8 and
>     game tests 30/30;
>   - the client game tests pass, with no mod warnings in the log;
>   - the provider-memory, AGENTS.md budget and LORE checks pass.
> - **New checks:** the amethyst flame is a purple recolour of vanilla's soul
>   campfire flame (the same frames and shape), and so is the particle,
>   untinted. On a crying floor brazier, every side's four slats show four
>   different stains at every position, and positions differ in arrangement.
>   Each crying bars block shows one stain version, and versions differ between
>   blocks. Every crying texture is still.
> - **Mutation checks** (test-authoring Rule 1), one client run each, all
>   caught:
>   - a teal ramp: 999 of 999 flame pixels not purple;
>   - the recolour losing its animation: 1 still frame against vanilla's 8;
>   - the particle using vanilla's sprite: 22 of 22 pixels not purple;
>   - every slat on one strip: each side's slats show 1 stain;
>   - one arrangement everywhere;
>   - bars parts listing versions in different orders: one block mixing two
>     versions;
>   - an animated tears texture.
> - **Screenshots** in `temp/art/screenshots/`: `art-crying-close-up`,
>   `art-crying-bars-close-up`, `art-copper-and-amethyst-fire`, `crying-tears-1`
>   and `-2`, `art-tiers`, `art-tiers-night`, `art-bars`,
>   `slice6-wall-and-hanging-2`, `flame-cost-amethyst`.

