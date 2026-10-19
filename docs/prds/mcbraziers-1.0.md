# PRD: MC Braziers 1.0

## Status

- 2026-10-18 — Amended — owner decision: the game may recolour vanilla textures
  while loading (License addendum); amethyst fire uses it. Crying obsidian's
  stains are static and varied (Content erratum).
- 2026-10-18 — Amended — crying obsidian variant and amethyst fire added for 1.0
  (Content addendum).
- 2026-10-18 — Amended — owner decision: tinting vanilla textures in game is allowed
  (License addendum); copper fire uses it.
- 2026-10-18 — Accepted — owner approved; the recipe contract is next.
- 2026-10-18 — Draft — owner answers to the open questions incorporated; only
  the copyright holder remains open.
- 2026-10-18 — Draft — written from the owner interview of 2026-10-18.

## Summary

MC Braziers adds metal braziers to Minecraft in a vanilla+ style. A brazier is
a campfire held in a cage of bars. The bars set its **tier**, and the campfire
sets its **fire type**. Braziers sit on the floor, mount on walls, or hang from
chains and ceilings. They cook food, light builds, and link into fire pits. Each
tier earns its place in survival through cooking speed and special resistances.
For builders, every tier comes in every fire type, and copper ages like vanilla
copper.

## Audience

- **Survival players:** a lighting and cooking upgrade path with real reasons
  to climb tiers (cooking speed, explosion-proof and water-proof braziers).
- **Builders:** decorative variety (6 tiers × 3 fire types, copper aging,
  placeable bars, three placement modes, connected fire pits).

## Platform and Distribution

- Minecraft 26.3 on Fabric: Loader 0.19.5+, Fabric API, Java 25
  (`docs/TOOLING.md`).
- Published on CurseForge.
- License: MIT for the mod's own code and original art. Minecraft and its
  assets remain Mojang's: textures are original vanilla-style work, never
  copies or recolors of Mojang's files. The listing and README carry an
  "official product / affiliation" disclaimer, worded to Mojang's current
  usage guidelines (to verify at release).

> **Addendum (2026-10-18, owner decision: tinting vanilla textures in game):**
> Models may point at vanilla's own textures, and the game may tint them while
> drawing, the way vanilla tints grass and leaves. The mod still ships no
> Mojang image, edited or not. Shipping edited copies of Mojang's files stays
> forbidden.
>
> First use: vanilla has no green campfire flame, so copper fire is vanilla's
> soul campfire flame tinted green, close to the copper torch's flame. This
> covers the Copper Campfire and every copper-fire brazier. It replaces the
> plan to draw an original animated green flame.

> **Addendum (2026-10-18, owner decision: recolouring vanilla textures at
> load):** The game may also recolour a vanilla texture while it loads its
> textures, from the player's own copy of the game. The mod ships only the
> colours to use, never the image. Shipping edited copies of Mojang's files
> stays forbidden.
>
> Why: a tint multiplies colours, so it can't add a colour the texture lacks.
> Vanilla's soul flame is teal with almost no red, so a purple tint left it
> blue-violet. A recolour swaps each colour for any other.
>
> Cost, measured before the decision: the recolour takes 3–5 ms once per
> texture load. With 400 lit campfires in view, recoloured, tinted and vanilla
> flames ran at the same frame rate within the test's noise. The full numbers
> are in `.research/2026-10-18-crying-obsidian-amethyst-fire.md`.
>
> First use: amethyst fire. Vanilla's animated soul campfire flame and its soul
> fire flame particle are recoloured purple, by brightness, from crying
> obsidian's dark purples up to amethyst's light ones.

## Art Direction

Vanilla+: 16×16 textures in vanilla's palette and proportions, sitting
comfortably beside vanilla blocks. The 64×64 copper model in `temp/` was an
early sketch and is not the target style.

## Content

> **Addendum (2026-10-18, owner decision: crying obsidian and amethyst fire):**
> Added at the request of the owner's friend, with every option the owner picked.
> Plan: `docs/plans/additions/crying-obsidian-amethyst-fire.md`.
>
> - **Amethyst fire (purple), a fourth fire type for every brazier.** It comes
>   from a new **Amethyst Campfire**, crafted like the Copper Campfire with an
>   amethyst shard in place of the copper nugget. The Amethyst Campfire works on
>   its own like vanilla's campfires.
>   - **Light:** 12. Vanilla has no purple fire to copy, so this sits between
>     soul fire (10) and copper fire (14).
>   - **Look:** the flame is vanilla's soul campfire flame tinted purple (License
>     addendum). *Superseded by the erratum below: recoloured, not tinted.*
>   - **Floor braziers:** they don't pop lava, like soul and copper fire.
> - **Crying obsidian, a cosmetic variant of the obsidian tier**, like copper's
>   aging stages. It has the same explosion-proof special, cooking speed and
>   mining rules as obsidian. It is made from new **crying obsidian bars**: 8 iron
>   bars around 1 crying obsidian make 8, as obsidian bars are made.
> - **Crying obsidian's tears:** the bars and the brazier have glowing purple
>   tear streaks that slowly creep down. Vanilla's tear particles bead up on
>   them, hang, fall and splash, as on the crying obsidian block. *Streaks
>   superseded by the erratum below.*
> - **New totals:**
>   - 14 brazier blocks (8 copper, iron, diamond, obsidian, crying obsidian,
>     prismarine, netherite);
>   - × 4 fires = **56 brazier items**;
>   - 5 placeable bars;
>   - 2 mod campfires.

> **Erratum (2026-10-18, owner's in-game review of the crying obsidian and
> amethyst addition):**
>
> - **Amethyst flame:** the tint read blue-violet in game, so the flame, and the
>   wall and hanging flame particle, are recoloured instead (License addendum,
>   recolouring at load). The particle had been tinted the same way.
> - **Tears:** the animated streaks ran in lockstep, since every slat shared one
>   strip of the texture. They read as neon, the moving head looked square, and
>   nothing pooled. The stains are now still, as on vanilla's crying obsidian,
>   and the falling tear particles carry the motion. The stains vary:
>   - each slat of a crying brazier shows its own stain: a bead at the top, a
>     run through the middle, a pool at the foot, and so on;
>   - each crying brazier picks one of a few slat arrangements by its position,
>     so neighbours in a fire pit differ;
>   - each crying bars block picks one of four stain versions by its position;
>   - tears pool at the foot of the slats and the bars.

### Tiers

| Tier | Bars | Special | Mining |
|---|---|---|---|
| Copper | vanilla copper bars (all aging states) | the cheap starting tier; ages like vanilla copper | any pickaxe, including wood |
| Iron | vanilla iron bars | standard | stone pickaxe or better |
| Diamond | diamond bars | faster cooking | stone pickaxe or better |
| Obsidian | obsidian bars | **explosion-proof**; can't be pushed by pistons | stone pickaxe or better |
| Prismarine | prismarine bars | **water-proof**: stays lit in water and can be lit underwater; the cheaper, earlier-game underwater light | stone pickaxe or better |
| Netherite | netherite bars | **explosion-proof and water-proof**; the item is lava-proof; only a sneak-click turns it off | stone pickaxe or better |

Every brazier drops itself. Cooking speed rises with tier (see Cooking).

### Fire Types

Light comes from the fire, using vanilla's values, so a brazier is never
dimmer than the campfire it was made from.

| Fire | Campfire used | Light | Vanilla reference |
|---|---|---|---|
| Orange | Campfire | 15 | campfire |
| Green | Copper Campfire (new) | 14 | copper torch |
| Blue | Soul Campfire | 10 | soul campfire |

An unlit brazier gives no light.

### Copper Campfire (new)

Vanilla has no green campfire, so the mod adds one: a full campfire with green
copper-fire flames that works on its own like vanilla's campfires (cooking,
smoke, damage). It is crafted like the vanilla campfire, with a copper nugget
in place of the coal, echoing the copper torch (soul campfires use soul soil
there). It is the ingredient for green-fire braziers.

### Variants

- One block per tier, with the fire type stored in the block. That is 6 tiers ×
  3 fire types = 18 combinations, each a creative-tab item.
- Copper adds aging: 4 stages (normal, exposed, weathered, oxidized), each
  waxed or unwaxed, giving 24 copper braziers. Total: **39 brazier items**.
- Aging is cosmetic: it changes the texture only, never light or cooking.
  Waxing with honeycomb stops aging; axes scrape it back, as with vanilla
  copper.

### Placement

| Mode | Placed by clicking | Top | Look |
|---|---|---|---|
| Floor | the top of a block | **open** by default; empty-hand click closes the grate, and clicking again opens it | brazier standing on the ground |
| Wall | the side of a block | closed grate | wall mount with a closed-top grate |
| Hanging | the underside of a block or chain | closed grate | grate top with a chain loop, aligned like vanilla's hanging lantern; hangs from chains or solid ceilings |

Floor and wall braziers link into **connected fire pits** with neighbouring
braziers (shared sides open up). Hanging braziers never connect.

### Cooking

- Any **closed-top** brazier cooks: a floor brazier with its grate closed, plus
  every wall and hanging brazier. Open floor braziers don't cook.
- Same rules as a campfire: up to 4 items, campfire recipes, and food pops
  out when done.
- Cooking speed rises with tier, so survival players have a reason to upgrade
  (owner-confirmed 2026-10-18).

| Tier | Speed |
|---|---|
| Copper | 1.0× (campfire speed) |
| Iron | 1.25× |
| Diamond, Obsidian, Prismarine | 1.5× |
| Netherite | 2.0× |

### Fire Damage

An **open**, lit brazier hurts and sets on fire anything that steps into it,
like a campfire. A closed top doesn't hurt.

> **Erratum (2026-10-18, owner's in-game check):** Vanilla campfires hurt
> what stands in them but don't set it on fire, so "sets on fire" was wrong
> about campfires. Braziers do what campfires do: an open, lit brazier hurts
> anything that steps into it without setting it alight. Animals killed this
> way drop raw meat, as they do on a campfire. The owner saw this in game (a
> cow took damage, flashed red and died without burning) and kept it.

### Lighting and Putting Out

| Action | Copper / Iron / Diamond / Obsidian | Prismarine | Netherite |
|---|---|---|---|
| Flint and steel, fire charge | lights it | lights it, even underwater | lights it |
| Shovel | puts it out | puts it out | no effect |
| Splash water bottle | puts it out | no effect | no effect |
| Sneak + empty-hand click | turns it off | turns it off | turns it off (the only way, besides the command) |
| `/brazier extinguish` (looking at a brazier) | toggles it on or off | toggles it on or off | toggles it on or off |

Once out, a brazier stays out until relit. Sneak-click only turns a brazier
off, as a convenience on every tier; lighting still needs fire.

### Water

| Situation | Copper / Iron / Diamond / Obsidian | Prismarine, Netherite |
|---|---|---|
| Water above (falling or source) | goes out | stays lit |
| Water on 2 or more sides | goes out | stays lit |
| Thinnest flowing water on 1 side | stays lit | stays lit |
| Deeper flowing water or a source on 1 side | goes out | stays lit |
| Placed in water | can't be lit | placed lit, stays lit |
| Rain | no effect | no effect |

The rules aim to feel physically believable without full simulation.

### Bars

- Diamond, obsidian, prismarine and netherite bars are **placeable blocks** that
  connect like vanilla iron and copper bars.
- Copper and iron bars are vanilla's own; the mod adds no copper bars item.
- Netherite bars are lava-proof as items.
- Mining follows the brazier rule: stone pickaxe or better.

## Recipes

The exact list, the ingredient forms and the automated check live in the recipe
contract (next deliverable). The rules:

- **Brazier:** 8 bars around any campfire (`BBB / BCB / BBB`). The bars set the
  tier, and the campfire sets the fire type, so any campfire with any bars
  works.
- **Copper aging carries over:** bars of a given aging state or wax make a
  brazier in that same state.
- **Bars** (8 + 1 → 8, like dyeing glass):

| Bars | Recipe |
|---|---|
| Diamond | 8 iron bars around 1 diamond |
| Obsidian | 8 iron bars around 1 obsidian |
| Prismarine | 8 iron bars around 1 prismarine crystals |
| Netherite | 8 diamond bars around 1 netherite ingot |

- **Copper Campfire:** the campfire recipe with a copper nugget in place of the
  coal.
- Placed braziers can't be upgraded; each tier is crafted from its bars.

## Changes from the Current Mod

- Removed: the mod's `copper_bars` item (vanilla copper bars replace it), the
  `iron_bars_from_copper` recipe, and the brazier upgrade recipes.
- Light now follows the fire type instead of a per-tier ladder (5/7/10/15/15).
- Obsidian changes role: it becomes the explosion-proof tier and no longer
  always places lit. Its sneak-click toggle becomes sneak-click off for every
  tier, and `/brazier extinguish` now works on every brazier, not just
  obsidian.
- Added: the prismarine tier, the Copper Campfire, the fire types, copper aging,
  placeable bars, open and closed tops, closed-top cooking on wall and hanging
  braziers, and tiered cooking speed.

## Out of Scope for 1.0

- Fuel: braziers burn forever, like campfires.
- Redstone control.
- Compatibility with 1.20.1 worlds.
- Languages other than English.
- Upgrading placed braziers.

## Done for 1.0

- Everything under Content and Recipes works in game.
- Recipes match the recipe contract, and its automated check passes in the
  build.
- Every block, item and variant has vanilla+ models and textures. Copper's
  aging stages are visible.
- The game loads with no data, model or texture warnings from the mod.
- A recorded gameplay pass covers placement modes, cooking, fire damage,
  lighting and putting out, the water rules, aging and waxing, connected fire
  pits, and mining tiers.
- The CurseForge listing is ready: description, icon, license, disclaimer, and
  the Fabric API dependency.

## Open Questions

1. **Copyright holder** for the MIT `LICENSE`: left open by the owner
   (2026-10-18). Needed before a `LICENSE` file ships.

Resolved 2026-10-18 (owner): the cooking speeds stand as listed;
`/brazier extinguish` stays and toggles any brazier; the Copper Campfire uses a
copper nugget; sneak-click turns off every tier.
