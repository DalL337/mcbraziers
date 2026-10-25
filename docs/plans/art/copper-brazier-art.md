# Plan: Copper Brazier Art

## Status

- 2026-10-18 — Done — the owner kept the grille design for the bars ("we will keep the
  grate for the new bars. finally decided") after comparing it with tinted vanilla bars. Every
  mod block and item has its final art.
- 2026-10-18 — Owner's in-game review of every tier and mount (dev client, about 10 minutes, no
  mod warnings or errors in the log): "holy shit this is nice".
- 2026-10-18 — Every tier built (addendum at the end); awaiting the owner's
  once-over of each tier's mounting types in game.
- 2026-10-18 — Wall and hanging copper braziers built and checked; awaiting the
  owner's look in game.
- 2026-10-18 — Wall and hanging designs approved by the owner ("yeah these are
  fine"); building them.
- 2026-10-18 — Floor brazier approved by the owner after seeing it in game.
  Wall and hanging braziers next.
- 2026-10-18 — Floor brazier built; awaiting the owner's look in game (see the
  addendum at the end).
- 2026-10-18 — Accepted — the owner agreed to all six recommendations below and
  approved the 16×16 textures. Building the copper floor brazier first; wall
  and hanging braziers follow from the same parts.

## Scope

Final models and textures for the copper brazier (PRD: Art Direction, Placement,
Variants), starting with the floor brazier in every state:
- open and closed;
- lit and unlit;
- all three fires;
- all four aging stages (waxed braziers share them);
- every combination of connected sides.

The wall and hanging braziers come next, built from the same parts. The other
tiers follow once copper is settled.

## Design

The shape is the owner's base model, `temp/BrazierBlockGrd.bbmodel`:
- a basin;
- slim slats;
- a rim;
- a crown of flared tips.

On the PRD's 16×16 textures it looked better than a rebuilt version with
whole-pixel parts, so the silhouette is kept. The owner agreed to these changes
(2026-10-18):

1. **Keep the owner's shape.** The rebuilt version was dropped.
2. **Same slats on every side.** Four slats per side, so a brazier looks the
   same from every direction and when sides open. The corners are L-shaped.
3. **Fire pits join seamlessly.** A connected side drops its slats, rim, tips
   and basin wall. The basin, rim, ember bed and grate bridge to the block edge,
   so neighbours form one trough. A corner fills in where two connected sides
   meet.
4. **Closed grate as a lid** inside the rim, with the tips standing around it.
   Food is drawn on the grate.
5. **Inside:**
   - **Bed:** embers glowing in the fire's colour, or dead coals when unlit.
   - **Flames:** crossed flames like a campfire's, using the fire of the campfire
     the brazier was made from (vanilla's campfire and soul campfire fire; the
     Copper Campfire's own green fire).
6. **The hitbox follows the cage:** the rim spans 1.5 to 14.5 px and is 13 px
   tall, and it reaches the block edge on connected sides. It stays solid like a
   campfire, so standing on an open brazier still burns.

Also changed, because the art needs it: braziers join only with braziers placed
the same way. Floor joins floor, and wall joins wall on the same wall. Before,
a floor brazier joined a wall brazier beside it.

## Textures

All textures are original 16×16 work in vanilla copper's palette (PRD: License).
Each copper stage has one texture:
- rim band;
- basin band with rivets;
- slat;
- grate.

The grate repeats every 4 px from world x = 2, so it continues across fire pits.
Ember beds are shared by every tier: dead coals, plus an animated glow for each
fire.

## Wall and Hanging Braziers

Approved by the owner on 2026-10-18 from mockups
(`temp/art/renders/wall-hanging-overview.png`).

- **Hanging:**
  - **Basket:** a 10 px basket (the floor's is 12) with a grate lid and a crown
    of three tips per side.
  - **Chain:** a chain loop at the top centre, set up as vanilla's hanging
    lantern does it. The loop uses vanilla's chain texture for the brazier's
    metal; copper braziers get copper chain in the matching aging stage.
  - **Placement:** the same model hangs from chains and from solid ceilings. It
    never joins.
- **Wall:**
  - **Basket:** a half-depth basket, 12 wide and 8 deep, with a grate lid and
    tips on the front and sides.
  - **Mount:** a back plate on the wall and two support arms under the basin.
  - **Fire pits:** wall braziers on the same wall join into one trough.
- **Food on the smaller grates:** the four items are drawn at 4 px instead of 6,
  so they stay inside the basket.
- **Hitbox:** follows each basket.

## How the Assets Are Built

- `tools/art/brazier-art.mjs` (Node, no packages) writes the textures and the
  floor part models in `models/block/brazier/`. Each part is a model of its own,
  baked once per side, so items can reuse the parts without blockstate
  rotations. `--preview <dir>` also writes whole-brazier models for rendering
  in Blockbench.
- `McBraziersModelProvider` (datagen, `./gradlew runDatagen`) gives each part
  its textures and writes the multipart blockstates and the item models into
  `src/main/generated`. Wall and hanging copper braziers point at their
  placeholder models until their art is done.
- The datagen package lives in the client source set, because model datagen
  uses client classes.

## Checks

- `FloorCageGameTests`: which braziers join, the hitbox, and the food height.
- `CopperBrazierArtClientGameTest`:
  - every floor state of every copper brazier has its model and textures;
  - the renderer draws food on the grate;
  - screenshots of the aging stages, the fires, food on the grate and a fire
    pit.

> **Addendum (2026-10-18, copper floor brazier built):** All checks pass:
> - `./gradlew build` with no warnings; unit tests 8 of 8;
> - game tests 30 of 30, including `FloorCageGameTests`;
> - both client game tests, with no mod warnings in the log.
>
> Screenshots are in `temp/art/screenshots/`.
>
> Mutation check: five breakages, each caught by its own test:
> - braziers joining any placement;
> - the old campfire-slab hitbox;
> - campfire food spacing on the grate;
> - a missing ember texture (256 soul states flagged);
> - food not raised to the grate.
>
> The food check first read only the values the renderer records, and the
> food-not-raised mutation slipped past it. It now checks where each item is
> actually submitted for drawing, compared with the same food on a vanilla
> campfire. Comparing with fixed positions missed by about a pixel, because a
> food sprite isn't centred in its square. This led to Rule 2 (ratified 2026-10-18) in
> `lore/policies/reserved/test-authoring.md`.
>
> Built differently from the plan: food is also drawn closer in. Vanilla spaces
> it for a 16 px campfire, which hung it over the 12 px grate, so on the floor
> grate each item sits 3 px out and the four fill the grate's quarters.
>
> Still to do in this plan:
> - **Wall and hanging copper braziers:** they still use placeholder models,
>   and their food stays at campfire height.
> - **Copper fire:** it uses the Copper Campfire's placeholder green fire until
>   that campfire gets its art.

> **Addendum (2026-10-18, wall and hanging copper braziers built):** All checks
> pass:
> - `./gradlew build` with no warnings; unit tests 8 of 8;
> - game tests 30 of 30, with `FloorCageGameTests` renamed `BrazierCageGameTests`
>   and now covering every placement;
> - both client game tests, with no mod warnings in the log.
>
> Copper now has final art in every placement. The 17 superseded copper
> placeholder files are backed up outside the repository and removed.
>
> What was built:
> - **Wall and hanging parts:** the generator writes 12 more part models.
>   Datagen wires them: walls are turned by blockstate for each facing, and
>   the hanging cage takes vanilla's copper chain of the matching stage.
> - **Food layout:** `BrazierBlock.foodLayout` (a `FoodLayout` record) sets
>   where food sits on each grate. The renderer mixin applies it by wrapping
>   vanilla's two translates and its scale.
> - **Hitboxes:** wall and hanging hitboxes follow their baskets. A wall's
>   hitbox reaches the block edge on a joined side.
> - **Fire particles:** a wall brazier's fire particles rise above its basket,
>   toward the wall.
>
> Built differently from the plan:
> - Smoke over cooking food now rises from where each grate draws the food,
>   on floor braziers too. Floor braziers keep vanilla's smoke columns.
>
> New checks:
> - Every copper state's model reaches the edge of its block on exactly the
>   sides that join a neighbour.
> - Food placement is checked on the pose each item is submitted with,
>   against vanilla's layout maths, for the floor, wall and hanging grates.
>
> Mutation check: five breakages, each caught:
> - wall sides not turned in the hitbox (caught by the east-facing case);
> - wall food at full size;
> - the old hanging hitbox;
> - food not resized on the small grates;
> - wall joins not turned in datagen (2,112 states flagged).

> **Addendum (2026-10-18, every tier built):** Iron, diamond, obsidian,
> prismarine and netherite braziers now have final art in every placement. They
> use the copper parts with their own 16×16 textures. Each texture has a palette
> close to its vanilla material, with accents of its own:
> - diamond: glints;
> - obsidian: purple flecks;
> - prismarine: mottling;
> - netherite: warm flecks.
>
> Hanging braziers other than copper use vanilla's iron chain. Datagen covers
> all 13 brazier blocks and 39 items. The remaining 92 placeholder files are
> removed and backed up outside the repository.
>
> Checks:
> - `./gradlew build` with no warnings; unit 8 of 8; game tests 30 of 30.
> - The art client test, renamed `BrazierArtClientGameTest`, checks every
>   state of every brazier for missing art and wrong edges, with no mod
>   warnings in the log. Mutation check: datagen skipping netherite was caught
>   (2,292 states).
> - Screenshots in `temp/art/screenshots/` show every tier in every placement
>   (`art-tiers*`) and the wall mount from the side, low corner and below
>   (`art-wall-mount-*`), as the owner asked.
>
> Found on the way: datagen leaves stale folders behind after a run that
> writes different files. The generated assets were rebuilt from scratch, and
> the only difference was an empty folder from a mutation run.
>
> Not in scope: the placeable bars (diamond, obsidian, prismarine, netherite)
> keep their placeholder textures, and the Copper Campfire keeps its
> placeholder green fire.

> **Addendum (2026-10-18, wall bracket redesigned at the owner's request):**
> The owner asked for a mount "narrower than the body of the wall brazier.
> maybe two squares wide", which pushes the brazier away from the wall "with an
> arm that connects to the bottom of the brazier", with "a little character
> other than a flat piece of metal".
>
> The wall brazier now hangs on a bracket:
> - a 2 px wall plate with a cap on top, a foot at the bottom and two rivets;
> - a 2 px arm from the plate to under the basket, ending in a small drop
>   below the basket's front;
> - a 45° brace from low on the plate up to the arm.
>
> The basket keeps its 12×8 size but stands off the wall, centred in its block,
> with a back side of its own. It has no tips on the back, toward the wall.
> Food on the wall grate and the fire particles sit in the middle of the block.
> The wall hitbox covers the basket, the rim and the bracket.
>
> Checks: build with no warnings; game tests 30 of 30, with the cage test's
> wall values updated to this design; client tests pass with no mod warnings.
> Mutation check: the old basket hitbox and food pushed toward the wall were
> both caught. The side, low-corner and netherite shots in
> `temp/art/screenshots/art-wall-mount-*` show the bracket.

> **Addendum (2026-10-18, copper fire from a tinted vanilla flame; owner
> decision, PRD License addendum):** Vanilla has no green campfire flame, so
> copper fire now draws vanilla's soul campfire flame tinted `#BFFF73`. That is
> close to the copper torch's flame, and the vanilla animation comes with it.
> The placeholder green flame is removed.
>
> How it works:
> - Every flame part has tint index 0. The client tints it green only for
>   copper fire. Normal and soul fire stay untinted, although soul braziers
>   share the same texture.
> - The Copper Campfire uses vanilla's campfire model for its logs, with that
>   model's untintable flame hidden by a transparent texture. On top sits a
>   tintable flame of the same size (`brazier/campfire_fire`).
> - The Copper Campfire's blockstate and item now come from datagen.
> - Copper-fire items carry the same tint.
>
> Checks:
> - Build with no warnings; game tests 30 of 30.
> - The art client test checks that the flame quads and their tint are right
>   for the Copper Campfire and for normal, soul and copper fire in every
>   placement.
> - Mutation check: the brazier tint left unregistered was caught.
> - `temp/art/screenshots/art-copper-fire.png` shows the soul campfire, the
>   Copper Campfire and a copper-fire brazier side by side at night.

> **Addendum (2026-10-18, bars textures):** The placeable diamond, obsidian,
> prismarine and netherite bars have final textures, drawn by the generator in
> the matching brazier's palette. Each is our own grille: three vertical bars
> and two full-width crossbars, with rivets where they cross.
>
> The bars keep vanilla's shared bars models, so the middle bar sits on the
> columns those models use for a post, and the outer bars line up across panes.
> The item is the same texture as a flat icon, as vanilla's iron bars are. That
> one texture covers the placed block, the inventory and hotbar icon, the item
> in hand, and dropped items.
>
> Checks:
> - The art client test now also checks every state of the four bars and the
>   Copper Campfire for missing art.
> - Mutation check: removing the prismarine bars texture was caught (32
>   states).
> - Screenshots: `art-bars.png` shows a pane of each beside vanilla's iron and
>   copper bars; `art-bars-items.png` shows the bars in the hotbar and diamond
>   bars in hand.
> - Build with no warnings; game tests 30 of 30.
>
> With this, every mod block and item has its final art.

> **Addendum (2026-10-25, inside corners; owner decision, PRD Placement
> addendum):** A wall brazier joined on its front is an inside corner: it turns
> its run through a right angle into the run on the other wall. The owner
> asked for a vanilla+ look, a bracket on one wall, and a plain gap at outside
> corners, which get no piece.
>
> Drawn facing north (front at z 0, wall at z 16), in pixels:
> - The basket narrows to an 8 px junction, x 4–12 by z 4–12, as wide as the
>   run coming in from the front. It has a floor, a back wall with the two
>   middle slats of a straight basket, a back rim and a grate. The same
>   bracket holds it to this brazier's own wall.
> - An arm reaches forward to the block edge (x 4–12, z 0–4). It is open like
>   the bridges between braziers, and its low walls run on to the junction's
>   corners, so the inside of the turn has no gap.
> - Each side has an arm to the block edge (x 0–4 or 12–16) when the next
>   brazier along this wall joins. Otherwise it closes with an end wall at x 4
>   or x 12, with three slats and no tips.
> - Ember bed pieces sit under each part. The wall fire is the straight wall
>   brazier's, whose crossed flames fit inside the junction.
> - Rims sit 0.5 px outside the walls. The hitbox runs from 3.5 to 12.5 px
>   across a corner with closed ends, and to the block edge on the front and on
>   a joined side. The bracket takes it from 0.5 to 14 px up and back to the
>   wall. Food and the fire particles stay in the middle of the block, on the
>   junction.
>
> A corner needs no new block state: it is a wall brazier whose front
> connection is set. The parts are `wall_corner_*` in
> `tools/art/brazier-art.mjs`. The slatted ones get the three crying
> arrangements, numbered after the existing parts so stains already in the
> world stay put.
>
> Checks:
> - Build with no warnings; game tests 39 of 39. `BrazierPlacementGameTests`
>   covers the joins, and the cage test covers the corner hitboxes and food,
>   with the values above.
> - The art client test checks every corner state for missing art. Its edge
>   check now looks at the cage and the ember bed separately: before, a
>   bridge's ember bed reaching the edge could hide a missing cage arm.
> - Mutation checks, each caught: no corner at the front, joining behind, no
>   corner arm on the side, a front joining any wall brazier, the corner
>   hitbox ignored, and, in the client test, the corner's front arm and its
>   front ember bed each left out.
> - Screenshots in `temp/art/screenshots/`: `art-wall-corner*.png` (iron, by
>   day, close up, from above, at night; copper bracketed to the other wall)
>   and `art-wall-outside-corner.png` (the gap around a pillar).
