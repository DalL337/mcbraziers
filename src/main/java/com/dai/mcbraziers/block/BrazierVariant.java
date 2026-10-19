package com.dai.mcbraziers.block;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.block.WeatheringCopper;

/**
 * One brazier block: a tier plus a look. Each variant is one block that stores its fire type in its
 * state; each (variant, fire) pair is one item.
 *
 * @param agingPrefix the look's prefix in IDs: vanilla copper's aging prefix ({@code ""}, {@code exposed_},
 *                    … {@code waxed_oxidized_}), {@code crying_} for crying obsidian (a cosmetic variant of the
 *                    obsidian tier, PRD Content addendum), otherwise {@code ""}
 */
public record BrazierVariant(BrazierTier tier, String agingPrefix) {

    /** Crying obsidian's prefix: the obsidian tier with tears. */
    public static final String CRYING_PREFIX = "crying_";

    /** Vanilla copper's 8 aging/wax states, in vanilla's order. */
    public static final List<String> COPPER_AGING_PREFIXES = List.of(
            "", "exposed_", "weathered_", "oxidized_",
            "waxed_", "waxed_exposed_", "waxed_weathered_", "waxed_oxidized_");

    /** All 14 brazier blocks: the 8 copper states, then the other tiers, crying obsidian after obsidian. */
    public static final List<BrazierVariant> ALL = createAll();

    private static List<BrazierVariant> createAll() {
        List<BrazierVariant> variants = new ArrayList<>();
        for (String prefix : COPPER_AGING_PREFIXES) {
            variants.add(new BrazierVariant(BrazierTier.COPPER, prefix));
        }
        for (BrazierTier tier : BrazierTier.values()) {
            if (tier != BrazierTier.COPPER) {
                variants.add(new BrazierVariant(tier, ""));
            }
            if (tier == BrazierTier.OBSIDIAN) {
                variants.add(new BrazierVariant(tier, CRYING_PREFIX));
            }
        }
        return List.copyOf(variants);
    }

    /** Copper only: waxed braziers never age (PRD: Variants). */
    public boolean crying() {
        return this.agingPrefix.equals(CRYING_PREFIX);
    }

    public boolean waxed() {
        return this.agingPrefix.startsWith("waxed_");
    }

    /** Copper only: vanilla's aging stage for this variant ({@code weathered_} → WEATHERED). */
    public WeatheringCopper.WeatherState weatherState() {
        return switch (this.agingPrefix.replace("waxed_", "")) {
            case "exposed_" -> WeatheringCopper.WeatherState.EXPOSED;
            case "weathered_" -> WeatheringCopper.WeatherState.WEATHERED;
            case "oxidized_" -> WeatheringCopper.WeatherState.OXIDIZED;
            default -> WeatheringCopper.WeatherState.UNAFFECTED;
        };
    }

    /** Block ID, e.g. {@code weathered_copper_brazier}. */
    public String blockId() {
        return this.agingPrefix + this.tier.id() + "_brazier";
    }

    /** Item ID: {@code <aging><fire><tier>_brazier}, e.g. {@code weathered_soul_copper_brazier}. */
    public String itemId(BrazierFire fire) {
        return this.agingPrefix + fire.idPrefix() + this.tier.id() + "_brazier";
    }

    /** Namespaced ID of the bars this brazier is crafted from (recipe contract, R5). */
    public String barsId() {
        return switch (this.tier) {
            case COPPER -> "minecraft:" + this.agingPrefix + "copper_bars";
            case IRON -> "minecraft:iron_bars";
            default -> "mcbraziers:" + this.agingPrefix + this.tier.id() + "_bars";
        };
    }
}
