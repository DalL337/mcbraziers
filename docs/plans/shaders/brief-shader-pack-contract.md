# Brief: Shader Pack Contract

## Status

- 2026-10-29 — Draft — written with its guard test, `ShaderPackContractGameTests`, on the owner's go-ahead.
  Accepted when its PR merges.

## Purpose

Two shader packs give braziers coloured light by naming MC Braziers blocks in their `shaders/block.properties`:
Solas, merged on 2026-10-26 ([Septonious/Solas-Shader#206](https://github.com/Septonious/Solas-Shader/pull/206)),
and Photon, still open ([sixthsurge/photon#692](https://github.com/sixthsurge/photon/pull/692)). Both PRs use the
same entries. Those names are now a public contract.

A pack only reads names. If one of them changes, nothing crashes: lit braziers just lose their colour, or every
fire gets the same colour. Iris drops a state filter whose property the block lacks, so the entry then matches
every state (Iris source, read on 2026-10-25). Players also keep the pack version they downloaded, so a rename
breaks every copy of a pack that already shipped with the old name.

## What the packs match

- The block tag `mcbraziers:braziers`, filtered by the brazier's fire type and `lit`. Every brazier must be in
  the tag.
- The copper and amethyst campfires, filtered by `lit`.

The exact names live in `src/gametest/java/com/dai/mcbraziers/gametest/ShaderPackContractGameTests.java`,
copied from Solas's file. `./gradlew runGameTest` fails if any of them changes, or if a brazier is left out of
the tag.

**Solas also reads texture colour.** Its `shaders/lib/pbr/blocks/mcbraziers.glsl` picks the pixels that glow:
soul fire where the texture is blue, copper fire where it is green, amethyst fire where it is purple. A brazier
body that passes those tests glows as well. No test covers this.

## Free to change

Everything else: placement modes, the grate, connected faces, models, recipes, behaviours, and the ids of
individual brazier tiers, because the packs match the tag rather than a list of blocks. A new tier is covered as
soon as it is in the tag. A new fire type gets no coloured light until the packs add it, but nothing breaks.

## Changing a contract name

1. Avoid it. Prefer adding a new name to replacing an old one.
2. If a name must change, keep the old one working where possible (a renamed tag can stay with the same
   members), open follow-up PRs to Solas and Photon, and say it in the changelog. Players on older pack versions
   lose the coloured light either way.
3. Change the test's expected values in the same PR, citing the pack PRs.

## Repainting flames or brazier bodies

Check the result in game under Solas: each flame should still glow in its colour, and the metal should not.
