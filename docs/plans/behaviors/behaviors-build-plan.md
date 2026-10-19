# Build Plan: Brazier Behaviors

## Status

- 2026-10-18 — Done — the owner tested every slice in game during the art review and
  closed the plan; slice 6 accepted. The proposed client-test check rule is ratified
  (`lore/policies/verification.md`).
- 2026-10-18 — Accepted — owner approved the plan. Question 1 was changed (the
  grate refuses to open while food cooks); 2–5 as proposed.
- 2026-10-18 — Draft — derived from the accepted PRD (`docs/prds/mcbraziers-1.0.md`).

## Scope

Everything the PRD says braziers *do*, built on the content from the recipe
step. Art (models, textures, the hanging chain loop, the wall mount) is a
separate step. Each slice below ends with the standard check suite
(`lore/policies/verification.md`) and stops for owner review.

The slices are in dependency order: the controls and tops come first because
cooking speed, water and aging all build on the reworked interactions.

## Slice 1 — Controls and Tops

**Goal:** the PRD's controls, the open or closed top, and cooking on every
closed-top brazier.

**Tasks**
- Floor braziers get an open or closed top (a new block property). They are
  placed open. An empty-hand click toggles the grate.
- Wall and hanging braziers are always closed.
- Cooking works only on closed tops: a closed floor brazier, plus every wall
  and hanging brazier. Wall and hanging braziers gain the campfire block entity
  so they can cook. Food can't be placed on an open floor brazier.
- The grate refuses to open while food is cooking, with an action-bar message
  saying why (decision 1).
- Fire damage only from an open, lit floor brazier.
- Sneak + empty-hand click turns off any lit brazier, every tier. It never
  lights one.
- `/brazier extinguish` toggles any brazier, not just obsidian.
- Obsidian drops its old toggle and its always-placed-lit behavior. Its special
  arrives in slice 4.

**Tests (game tests)**
- Placing a floor brazier gives an open top, and a click closes it.
- Clicking to open a closed brazier with food on it leaves it closed.
- Food is refused on an open floor brazier and cooks on a closed one.
- A wall brazier and a hanging brazier each cook.
- An entity takes fire damage on an open, lit brazier and none on a closed one.
- Sneak-click turns off each tier and doesn't light an unlit one.
- The command toggles a non-obsidian brazier.

**Acceptance:** all of the above pass. Existing tests stay green.

**Placeholder caveat:** items cooking on wall and hanging braziers draw where
vanilla's campfire would put them, until the art step places the grates.

> **Addendum (2026-10-18, slice 1 built):** All slice 1 tests pass in
> `BrazierControlGameTests` (8 tests; 11 game tests in total with the recipe
> tests). `./gradlew build` is green with no warnings, and the contract test
> passes 5 of 5.
>
> Mutation check: letting closed braziers burn, and letting open braziers take
> food, each made its test fail.
>
> Built differently from the plan:
> - The grate toggle plays the iron trapdoor's sounds.
> - On the client, wall and hanging braziers skip the campfire's smoke columns,
>   which would rise through ceilings. Slice 6 revisits this.
> - `ObsidianBrazierBlock` was removed; obsidian keeps strength 50 / 1200 until
>   slice 4.
> - `/brazier extinguish` turns any brazier on or off, without water checks
>   yet. Slice 3 adds "can't be lit in water" to every lighting path,
>   including this command.

## Slice 2 — Cooking Speed by Tier

**Goal:** copper 1.0×, iron 1.25×, diamond, obsidian and prismarine 1.5×,
netherite 2.0× (PRD).

**Tasks**
- Wrap vanilla's cooking tick so a brazier runs it as many extra times as its
  multiplier calls for. The count is worked out from the game time, so no extra
  saved state is needed. Vanilla campfires are untouched.

**Tests**
- A food item finishes in the expected number of ticks on each tier. Raw beef
  takes 600 ticks on a campfire: 600 on copper, 480 on iron, 400 on diamond, 300
  on netherite.

**Acceptance:** cooking times match per tier within one tick.

> **Addendum (2026-10-18, slice 2 built):** Measured beef cooking times:
> copper 600, iron 480, diamond 400, obsidian 400, prismarine 400, netherite
> 300, and the vanilla campfire 600. All exact, measured by
> `CookingSpeedGameTests` against a vanilla campfire.
> `BrazierCookingStepsTest` covers the per-tick step arithmetic and checks the
> speed table against the PRD. Totals: 12 game tests and 8 unit tests pass,
> with no warnings.
>
> The mutation check (iron set to 1.0×) first slipped past the game test,
> because it computed its expected times from the code's own speeds. The test
> now uses the PRD's times written out directly, and the mutation fails it.
> This incident led to proposed Rule 1 in
> `lore/policies/reserved/test-authoring.md`, awaiting owner ratification.

## Slice 3 — Water and Water-Proof Tiers

**Goal:** the PRD's water rules and the prismarine and netherite immunities.

**Tasks**
- Copper, iron, diamond and obsidian go out when any of these is true:
  - water is directly above;
  - water is on 2 or more sides;
  - deeper flowing water or a source block is on 1 side.

  The thinnest flowing water on one side is safe. This is checked when a
  neighbouring block changes and when the brazier is placed.
- Placed in water: those tiers place unlit and can't be lit.
- Prismarine and netherite: water never puts them out, including waterlogging.
  Placed in water, they place lit.
- Prismarine can be lit underwater with flint and steel or a fire charge;
  netherite can't (open question 4).
- Shovels don't work on netherite. Splash water bottles don't affect prismarine
  or netherite. This needs one small mixin (a code patch into vanilla) on the
  splash potion's douse logic, because that logic is private to vanilla.
- Rain does nothing, as in vanilla. No change needed.

**Tests (game tests)**
- Each row of the PRD water table, for one plain tier and for prismarine and
  netherite: water above; 2 sides; thinnest flow on 1 side; a source on 1 side;
  placed in water.
- A shovel on netherite does nothing.
- A splash water bottle on prismarine does nothing.

**Acceptance:** the PRD water table holds in tests. Mixins are registered and
documented.

> **Addendum (2026-10-18, slice 3 built):** `WaterGameTests` passes 8 of 8
> (20 game tests in total). The expected outcomes are the PRD's water and
> lighting tables, written out for iron, prismarine and netherite.
>
> Mutation check: four breakages, one run, each caught by its own test:
> - thinnest flow counted as dangerous;
> - prismarine not water-proof;
> - the splash-potion mixin disabled;
> - netherite affected by shovels.
>
> As built:
> - A "drenched" check (water above, water on two or more sides, or
>   deeper-than-thinnest water on any side) schedules a one-tick extinguish
>   when a neighbour changes. Placement applies the same check.
> - One lighting gate (`BrazierBlock.canBeLit`) covers flint and steel, fire
>   charges, burning projectiles and `/brazier extinguish`. The command now
>   reports when a brazier can't be lit in water. A refused lighting consumes
>   the click, so the item can't light the brazier on its own.
> - Water-proof tiers waterlog without going out and are always placed lit.
> - The project's first mixin is `AbstractThrownPotionMixin` in
>   `mcbraziers.mixins.json`. It cancels vanilla's private splash-water douse
>   only for water-proof braziers.

## Slice 4 — Tier Specials and Mining

**Goal:** obsidian and netherite resist explosions, and mining follows the PRD.

**Tasks**
- Obsidian and netherite get obsidian's blast resistance (1200) and can't be
  pushed by pistons.
- Hardness stays at the campfire's 2.0 for every brazier (open question 3).
- Mining: copper braziers with any pickaxe, including wood; every other brazier
  and every mod bar block needs stone or better.
- The wrong tool, or bare hands, drops nothing, as with vanilla metal blocks
  (open question 2).
- The Copper Campfire stays like vanilla's campfire: best with an axe, no tool
  needed.

**Tests**
- An explosion next to obsidian and netherite braziers leaves them standing,
  and a plain brazier breaks.
- Loot with and without the right pickaxe tier.

**Acceptance:** tests pass. Mining tags are generated, never hand-written.

> **Addendum (2026-10-18, slice 4 built):** `SpecialsAndMiningGameTests`
> passes 4 of 4 (24 game tests in total).
>
> What was built:
> - Obsidian and netherite braziers: blast resistance 1200 and push reaction
>   `IMMOVEABLE`. Hardness is 2.0 on every brazier; obsidian's old 50 is gone.
> - Every brazier and mod bar block requires the correct tool to drop.
> - Generated tags: `mineable/pickaxe` (17 entries) and `needs_stone_tool` (9,
>   everything except copper); the Copper Campfire is in `mineable/axe`.
>
> Mutation check: obsidian without blast resistance, a soul brazier dropping
> the wrong item, braziers dropping by hand, and copper needing stone were each
> caught. The copper mutation needed regenerated tags to take effect; the
> first combined run missed it for that reason, and it was re-run alone.
>
> Known limit: the piston test can't catch a regression in the explicit push
> reaction, because vanilla pistons already refuse any block with a block
> entity. It checks the PRD outcome only.

## Slice 5 — Copper Aging

**Goal:** copper braziers age and wax like vanilla copper, cosmetically.

**Tasks**
- Unwaxed copper braziers age a stage at a time on random ticks, as vanilla
  copper does. Registered through Fabric's `OxidizableBlocksRegistry`, which also
  makes honeycomb waxing and axe scraping work in the world.
- Fire, lit state, top, placement and connections carry over to the aged block.
  Food cooking on it stays (open question 5).
- Aging changes only the look. The visible stages are art-step work; until then
  every stage shares the copper placeholder.

**Tests**
- Forcing an aging step keeps every property and the food.
- Honeycomb waxes, and an axe scrapes back one stage.
- Waxed braziers never age.

**Acceptance:** tests pass.

> **Addendum (2026-10-18, slice 5 built, with vanilla values per the owner):**
> `CopperAgingGameTests` passes 3 of 3 (27 game tests in total).
>
> How it works:
> - The four unwaxed copper braziers implement vanilla's `WeatheringCopper`,
>   so they age by vanilla's own process: the per-random-tick roll, the 0.75
>   slowdown for unaffected copper, and the influence of nearby copper, which
>   includes vanilla copper blocks. No numbers were copied into the mod.
> - Stages and wax pairs are registered through Fabric's
>   `OxidizableBlocksRegistry`. In 26.3 axe scraping is a data-driven block
>   transformer, and the registry covers it too; a game test confirms this.
> - Any change from one brazier block to another keeps the campfire block
>   entity, as vanilla's copper chest does, so food survives aging, waxing and
>   scraping.
>
> The tests use vanilla's own copper blocks as the reference values.
> Mutation check: the block entity not kept, the wax pairs missing, and a
> changed chance modifier were each caught.

## Slice 6 — Fire-Type Particles

**Goal:** the flames look right for each fire type.

**Tasks**
- Wall and hanging braziers use the matching flame particle (vanilla's normal,
  soul or copper-fire flame).
- Floor braziers follow their campfire: the normal fire gives lava pops, while
  soul and copper fire don't, matching vanilla's soul campfire.

**Tests:** a client check. Particles are client-side, so this is a visual
check in the dev client, recorded in the journal.

**Acceptance:** confirmed by the owner in game.

> **Addendum (2026-10-18, slice 6 built; awaiting the owner's in-game check):**
> The new client game test, `FireParticleClientGameTest`, passes. Server game
> tests pass 27 of 27, unit tests 8 of 8, and the build has no warnings.
>
> What was built:
> - Wall and hanging braziers use their fire type's flame: vanilla's flame,
>   soul fire flame, or copper fire flame.
> - Floor braziers pop lava only with normal fire. The Copper Campfire no
>   longer pops lava, matching the soul campfire.
> - Wall and hanging braziers now give off vanilla's smoke over cooking food,
>   but still no smoke columns. This settles the question deferred in slice 1.
>
> Built differently from the plan:
> - **Particle rate:** the first screenshots showed one flame across four
>   frames of six braziers, because the old code fired only 20% of the time
>   on top of the client's random animate ticks. Wall and hanging braziers now
>   spawn particles at the vanilla torch rate: one smoke and one flame every
>   animate tick.
> - **Particle position:** particles rise from the top of the placeholder
>   baskets. Before, they spawned inside the wall basket, where they were
>   hidden. The two heights are constants, and the art step moves them with the
>   final models.
> - **Crackle:** every lit brazier now crackles like a campfire. Wall and
>   hanging braziers used to be silent.
> - **Test:** instead of a visual-only check, the client test logs every
>   particle the client is asked to spawn (through a test-only mixin) and
>   checks it against this plan. It also saves screenshots of each brazier
>   beside its vanilla torch or campfire.
>
> Mutation check: five breakages, each caught by the client test:
> - the soul flame swapped for the normal one;
> - lava pops on every fire type;
> - lava pops on the Copper Campfire;
> - wall braziers given smoke columns;
> - wall braziers without food smoke.
>
> Known limits, for the art step:
> - The placeholder models show no fire of their own.
> - Seen from below, the hanging basket hides its flames.
> - Food on wall and hanging braziers still draws where a campfire would put
>   it.

## Decisions (owner, 2026-10-18)

1. **Opening the grate mid-cook:** refused while food is cooking (changed from
   the proposed "pop the food off").
2. **Mining by hand:** drops nothing without the right pickaxe, as vanilla metal
   blocks do.
3. **Hardness:** every brazier at 2.0; obsidian and netherite differ only in
   blast resistance.
4. **Netherite underwater:** stays lit in water, but can't be relit underwater.
   Only prismarine lights underwater.
5. **Food during aging:** kept when a copper brazier ages.

> **Addendum (2026-10-18, fire-pit smoke; owner's design after the plan was
> done):** In a fire pit every floor brazier sent up a full campfire smoke
> column, and the smoke hid the pit. The owner chose two rules that stack:
> - **Pits share their smoke.** Each floor brazier sends up columns as often as
>   a campfire divided by one plus the number of braziers joined to it. A lone
>   brazier smokes exactly like a campfire, and a 3×2 pit puts out about 1.8
>   campfires' worth instead of 6.
> - **A closed grate damps it.** A closed floor brazier gives light wisps
>   instead of the tall columns, at the same shared rate.
>
> Signal fires (a hay bale underneath) keep vanilla's full signal smoke, and
> the smoke over cooking food is unchanged.
>
> Checked by `FireParticleClientGameTest`, which counts smoke over 800 ticks for
> a lone brazier, an open pit, a closed brazier and a signal fire. Mutation
> check: pits not sharing and closed grates still sending up columns were both
> caught.
