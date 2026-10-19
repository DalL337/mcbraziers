package com.dai.mcbraziers.block;

import net.minecraft.world.level.material.MapColor;

/** Brazier tiers, set by the bars a brazier is crafted from (PRD: Tiers). */
public enum BrazierTier {
    COPPER("copper", MapColor.COLOR_ORANGE, false, 1.0),
    IRON("iron", MapColor.METAL, false, 1.25),
    DIAMOND("diamond", MapColor.DIAMOND, false, 1.5),
    OBSIDIAN("obsidian", MapColor.COLOR_BLACK, false, 1.5),
    PRISMARINE("prismarine", MapColor.COLOR_CYAN, false, 1.5),
    NETHERITE("netherite", MapColor.COLOR_BLACK, true, 2.0);

    private final String id;
    private final MapColor mapColor;
    private final boolean fireResistant;
    private final double cookingSpeed;

    BrazierTier(String id, MapColor mapColor, boolean fireResistant, double cookingSpeed) {
        this.id = id;
        this.mapColor = mapColor;
        this.fireResistant = fireResistant;
        this.cookingSpeed = cookingSpeed;
    }

    /** Multiplier on vanilla campfire cooking speed (PRD: Cooking). */
    public double cookingSpeed() {
        return this.cookingSpeed;
    }

    /** Survives explosions like obsidian (blast resistance 1200) and can't be pushed (PRD: Tiers). */
    public boolean explosionProof() {
        return this == OBSIDIAN || this == NETHERITE;
    }

    /** Copper braziers mine with any pickaxe, wood included; every other tier needs stone or better (PRD: Tiers). */
    public boolean needsStoneTool() {
        return this != COPPER;
    }

    /** Water never puts it out, including waterlogging and splash water (PRD: Water). */
    public boolean waterProof() {
        return this == PRISMARINE || this == NETHERITE;
    }

    /** Can be lit while waterlogged; netherite stays lit in water but can't be relit there (behaviors plan, decision 4). */
    public boolean lightsUnderwater() {
        return this == PRISMARINE;
    }

    /** Shovels have no effect; only sneak-click (or the command) turns it off (PRD: Lighting and Putting Out). */
    public boolean ignoresShovels() {
        return this == NETHERITE;
    }

    /**
     * Vanilla cook steps to run on this game tick so that, over time, this tier cooks
     * {@link #cookingSpeed()} times as fast as a campfire. Derived from the game time, so no extra
     * saved state is needed: at 1.25 the steps go 1, 1, 1, 2; at 1.5 they go 1, 2; at 2.0 always 2.
     */
    public int cookingSteps(long gameTime) {
        return (int) (Math.floor((gameTime + 1) * this.cookingSpeed) - Math.floor(gameTime * this.cookingSpeed));
    }

    public String id() {
        return this.id;
    }

    public MapColor mapColor() {
        return this.mapColor;
    }

    /** Netherite items survive fire and lava, like vanilla netherite. */
    public boolean fireResistant() {
        return this.fireResistant;
    }
}
