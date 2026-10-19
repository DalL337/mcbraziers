# Brief: Recipe Contract

## Status

- 2026-10-18 — Amended — crying obsidian bars and braziers, and amethyst fire (79 recipes;
  amendment at the end of Rules).
- 2026-10-18 — Verified in game — the owner crafted the recipes in the 26.3 dev client:
  "crafting works as intended. havent found any gaps."
- 2026-10-18 — Accepted — owner approved; building the recipes is next.
- 2026-10-18 — Draft — owner decisions on naming and grouping incorporated; no
  open decisions.
- 2026-10-18 — Draft — derived from the accepted PRD (`docs/prds/mcbraziers-1.0.md`).

## Purpose

One rulebook that every MC Braziers recipe must satisfy, checked by the build.
The PRD says what players can craft. This contract states it precisely enough
for a test to fail when a recipe file drifts. Vanilla facts below were checked
against the Minecraft 26.3 game files on 2026-10-18.

## Rules

Each rule is written to be checked automatically (see Enforcement).

- **R1. One source.** Recipes are generated from the contract table in code
  (Fabric data generation) and never edited by hand. The committed recipe files
  must match a fresh generation.
- **R2. Complete and exact.** The mod ships exactly the 56 recipes in the tables
  below: none missing, none extra.
- **R3. Brazier shape.** Shaped `BBB / BCB / BBB`. `B` is one exact bars item,
  `C` is one exact campfire item, and the result is 1 brazier.
- **R4. Fire comes from the campfire:**

  | Campfire (`C`) | Fire type |
  |---|---|
  | `minecraft:campfire` | orange |
  | `minecraft:soul_campfire` | blue (soul) |
  | `mcbraziers:copper_campfire` | green (copper fire) |

- **R5. Tier comes from the bars:**

  | Bars (`B`) | Tier |
  |---|---|
  | each of the 8 vanilla copper bars (below) | copper, same aging and wax state |
  | `minecraft:iron_bars` | iron |
  | `mcbraziers:diamond_bars` | diamond |
  | `mcbraziers:obsidian_bars` | obsidian |
  | `mcbraziers:prismarine_bars` | prismarine |
  | `mcbraziers:netherite_bars` | netherite |

  The vanilla copper bars are `copper_bars`, `exposed_copper_bars`,
  `weathered_copper_bars`, `oxidized_copper_bars`, and the `waxed_` form of each.
  So aging and wax carry over: oxidized bars make an oxidized brazier.
- **R6. Bars recipes.** Shaped `XXX / XYX / XXX` makes 8 bars:

  | Result | `X` | `Y` |
  |---|---|---|
  | `mcbraziers:diamond_bars` | `minecraft:iron_bars` | `minecraft:diamond` |
  | `mcbraziers:obsidian_bars` | `minecraft:iron_bars` | `minecraft:obsidian` |
  | `mcbraziers:prismarine_bars` | `minecraft:iron_bars` | `minecraft:prismarine_crystals` |
  | `mcbraziers:netherite_bars` | `mcbraziers:diamond_bars` | `minecraft:netherite_ingot` |

- **R7. Copper Campfire.** Shaped `" S " / "SNS" / "LLL"` makes 1
  `mcbraziers:copper_campfire`, with `S` = `minecraft:stick`, `N` =
  `minecraft:copper_nugget`, and `L` = `#minecraft:logs`. This mirrors vanilla's
  campfire (coal in the middle) and soul campfire (soul soil in the middle).
- **R8. Waxing.** Shapeless: an unwaxed copper brazier item plus
  `minecraft:honeycomb` makes its waxed form. That is 4 aging stages × 3 fires =
  12 recipes, using vanilla's convention (category `building`, one group per
  waxed family, ID `<waxed result>_from_honeycomb`). In-world waxing and axe
  scraping are block behavior, outside this contract.
- **R9. Real items.** Every ingredient and result is a registered item, either
  vanilla 26.3 or the mod's.
- **R10. No vanilla conflicts.** No mod recipe accepts a crafting grid that a
  vanilla 26.3 recipe also accepts.
- **R11. Unlocks.** Each recipe enters the recipe book when the player first
  holds its key ingredient:

  | Recipes | Unlocked by |
  |---|---|
  | Braziers | their bars item |
  | Diamond, obsidian, prismarine bars | iron bars or the center material |
  | Netherite bars | diamond bars or a netherite ingot |
  | Copper Campfire | a copper nugget |
  | Waxing | honeycomb or the unwaxed brazier |

- **R12. Recipe book.** Braziers and bars use category `misc`, like vanilla's
  campfire and iron bars. The three fire variants made from the same bars share
  a group, so they appear under a single recipe-book button.
- **R13. IDs.** A recipe's ID is its result item's ID; waxing recipes add
  `_from_honeycomb`.
- **R14. Nothing else.** No brazier-to-brazier upgrades, and no recipes for
  vanilla items (vanilla already crafts copper and iron bars).
- **R15. Cooking.** Braziers cook vanilla `campfire_cooking` recipes; the mod
  adds no cooking recipes. Cooking speed by tier is block behavior (PRD), outside
  this contract.


> **Amendment (2026-10-18, owner decision: crying obsidian and amethyst fire;
> PRD Content addendum):** These rules replace the matching parts above.
> `RecipeContractTest` follows them.
>
> - **R2:** the mod ships exactly **79** recipes:
>
>   | Recipes | Count |
>   |---|---|
>   | Braziers: 14 bars items × 4 campfires | 56 |
>   | Bars (diamond, obsidian, crying obsidian, prismarine, netherite) | 5 |
>   | Copper Campfire, Amethyst Campfire | 2 |
>   | Waxing: 4 copper stages × 4 fires | 16 |
>   | **Total** | **79** |
>
> - **R4:** a fourth campfire, `mcbraziers:amethyst_campfire` → purple
>   (amethyst fire). Fire prefix in item IDs: `amethyst_fire_`, e.g.
>   `amethyst_fire_iron_brazier`.
> - **R5:** `mcbraziers:crying_obsidian_bars` → the obsidian tier, crying
>   variant. Its braziers take the prefix `crying_` where copper takes its aging
>   prefix, before the fire prefix:
>   - `crying_obsidian_brazier`, `crying_soul_obsidian_brazier`,
>     `crying_copper_fire_obsidian_brazier` and
>     `crying_amethyst_fire_obsidian_brazier`;
>   - names follow the IDs word for word ("Crying Soul Obsidian Brazier");
>   - the recipe-book group is `crying_obsidian_brazier` (R12).
> - **R6:** crying obsidian bars: `minecraft:iron_bars` around
>   `minecraft:crying_obsidian` make 8. They unlock from either ingredient (R11).
> - **R7:** Amethyst Campfire: shaped `" S " / "SAS" / "LLL"` makes 1
>   `mcbraziers:amethyst_campfire`, with `S` = `minecraft:stick`, `A` =
>   `minecraft:amethyst_shard` and `L` = `#minecraft:logs`. It unlocks from an
>   amethyst shard (R11).
> - **R8:** waxing covers all four fires: 4 stages × 4 fires = 16.

## Recipe Count

| Kind | Count |
|---|---|
| Braziers: copper (8 bars variants × 3 fires) | 24 |
| Braziers: iron, diamond, obsidian, prismarine, netherite (5 × 3) | 15 |
| Bars | 4 |
| Copper Campfire | 1 |
| Waxing copper braziers | 12 |
| **Total** | **56** |

## Item IDs and Names

Prefix style, vanilla's own pattern (owner decision 2026-10-18; the suffix
alternative was declined). An item ID is built as:

`<aging><fire><tier>_brazier`

| Part | Values |
|---|---|
| `<aging>` (copper only) | `""`, `exposed_`, `weathered_`, `oxidized_`, and each of those with `waxed_` in front |
| `<fire>` | `""` (orange), `soul_` (blue), `copper_fire_` (green) |
| `<tier>` | `copper`, `iron`, `diamond`, `obsidian`, `prismarine`, `netherite` |

English names follow the same order, the way vanilla names copper blocks:

| ID | Name |
|---|---|
| `iron_brazier` | Iron Brazier |
| `soul_iron_brazier` | Soul Iron Brazier |
| `copper_fire_iron_brazier` | Copper Fire Iron Brazier |
| `weathered_soul_copper_brazier` | Weathered Soul Copper Brazier |
| `waxed_weathered_copper_fire_copper_brazier` | Waxed Weathered Copper Fire Copper Brazier |

### Brazier results

Each bars item makes three braziers, one per campfire:

| Bars | with campfire | with soul campfire | with copper campfire |
|---|---|---|---|
| `minecraft:copper_bars` | `copper_brazier` | `soul_copper_brazier` | `copper_fire_copper_brazier` |
| `minecraft:exposed_copper_bars` | `exposed_copper_brazier` | `exposed_soul_copper_brazier` | `exposed_copper_fire_copper_brazier` |
| `minecraft:weathered_copper_bars` | `weathered_copper_brazier` | `weathered_soul_copper_brazier` | `weathered_copper_fire_copper_brazier` |
| `minecraft:oxidized_copper_bars` | `oxidized_copper_brazier` | `oxidized_soul_copper_brazier` | `oxidized_copper_fire_copper_brazier` |
| `minecraft:waxed_copper_bars` | `waxed_copper_brazier` | `waxed_soul_copper_brazier` | `waxed_copper_fire_copper_brazier` |
| `minecraft:waxed_exposed_copper_bars` | `waxed_exposed_copper_brazier` | `waxed_exposed_soul_copper_brazier` | `waxed_exposed_copper_fire_copper_brazier` |
| `minecraft:waxed_weathered_copper_bars` | `waxed_weathered_copper_brazier` | `waxed_weathered_soul_copper_brazier` | `waxed_weathered_copper_fire_copper_brazier` |
| `minecraft:waxed_oxidized_copper_bars` | `waxed_oxidized_copper_brazier` | `waxed_oxidized_soul_copper_brazier` | `waxed_oxidized_copper_fire_copper_brazier` |
| `minecraft:iron_bars` | `iron_brazier` | `soul_iron_brazier` | `copper_fire_iron_brazier` |
| `mcbraziers:diamond_bars` | `diamond_brazier` | `soul_diamond_brazier` | `copper_fire_diamond_brazier` |
| `mcbraziers:obsidian_bars` | `obsidian_brazier` | `soul_obsidian_brazier` | `copper_fire_obsidian_brazier` |
| `mcbraziers:prismarine_bars` | `prismarine_brazier` | `soul_prismarine_brazier` | `copper_fire_prismarine_brazier` |
| `mcbraziers:netherite_bars` | `netherite_brazier` | `soul_netherite_brazier` | `copper_fire_netherite_brazier` |

All results are in the `mcbraziers` namespace. Each row is one recipe-book group
(R12), named after the row's orange-fire ID.

## Enforcement

1. **Generation:** a Fabric data generation provider holds the contract table
   (Loom `fabricApi.configureDataGeneration()`). `./gradlew runDatagen` writes the
   recipe JSON and its recipe-book unlock files into `src/main/generated/`, and
   those files are committed.
2. **Contract test** (JUnit with `fabric-loader-junit`, part of `./gradlew build`).
   It boots Minecraft and the mod's registries in the test and checks:
   - R2 to R8 and R11 to R14 against the committed files;
   - R9 by registry lookup;
   - R10 against every vanilla crafting recipe in the 26.3 jar;
   - R1, by comparing the committed files with a fresh generation.
3. **Game test** (Loom `fabricApi.configureTests()`, a headless server). It
   checks that the data loads with zero errors, that the server's recipe
   manager holds all 56 recipes, and that a few sample crafts produce the
   expected item.
4. **Verification slot:** the standard check suite
   (`lore/policies/verification.md` Rule 1) becomes `./gradlew build` plus the
   game test task.

The test libraries are dev-only (`fabric-loader-junit`, Apache-2.0; JUnit,
EPL-2.0), so the bundled-license rule doesn't apply. They are registered in
`docs/TOOLING.md` when added.

> **Addendum (2026-10-18, enforcement as built):** Two details differ from
> the plan above:
>
> - **R1** isn't checked by re-running the generator. Instead,
>   `RecipeContractTest` re-derives all 56 recipes from these rules
>   independently and compares the committed files field for field. Any drift
>   fails, whether from a hand edit or from a generator change.
> - **R9** is checked in-world. `RecipeGameTests` confirms the server loads
>   all 56 recipes, which fails if any recipe names an unknown item, and that
>   sample grids craft the right item.
>
> Both checks were shown to fail on purpose before being trusted: a wrong
> output count, an edited ingredient, an extra recipe that clashes with
> vanilla's iron bars, and a wrong expected recipe count in the game test.

## Replaces

All 14 current recipe files go away: the mod's `copper_bars`,
`iron_bars_from_copper`, the four `*_brazier_upgrade` recipes, the old bars
recipes, and the old brazier recipes.

## Decisions

- 2026-10-18 (owner): prefix naming (`soul_iron_brazier`, "Soul Iron Brazier").
- 2026-10-18 (owner): each brazier's three fire variants share one recipe-book
  button.
