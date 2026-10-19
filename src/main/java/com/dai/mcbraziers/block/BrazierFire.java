package com.dai.mcbraziers.block;

import com.dai.mcbraziers.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.StringRepresentable;

/**
 * The fire inside a brazier, set by the campfire it was crafted from (PRD: Fire Types).
 * Light comes from the fire, using the vanilla value of the matching flame.
 */
public enum BrazierFire implements StringRepresentable {
    NORMAL("normal", "", 15),
    SOUL("soul", "soul_", 10),
    COPPER("copper", "copper_fire_", 14),
    /** Purple, from the Amethyst Campfire. Vanilla has no purple fire, so its light sits between soul and copper. */
    AMETHYST("amethyst", "amethyst_fire_", 12);

    private final String name;
    private final String idPrefix;
    private final int lightLevel;

    BrazierFire(String name, String idPrefix, int lightLevel) {
        this.name = name;
        this.idPrefix = idPrefix;
        this.lightLevel = lightLevel;
    }

    /** Prefix in item IDs, e.g. {@code soul_} in {@code soul_iron_brazier} (recipe contract, Item IDs). */
    public String idPrefix() {
        return this.idPrefix;
    }

    public int lightLevel() {
        return this.lightLevel;
    }

    /**
     * Flame particle for wall and hanging braziers: the one vanilla's matching torch uses, or for amethyst
     * fire, which has no vanilla torch, the mod's purple flame.
     */
    public SimpleParticleType flameParticle() {
        return switch (this) {
            case NORMAL -> ParticleTypes.FLAME;
            case SOUL -> ParticleTypes.SOUL_FIRE_FLAME;
            case COPPER -> ParticleTypes.COPPER_FIRE_FLAME;
            case AMETHYST -> ModParticles.AMETHYST_FIRE_FLAME;
        };
    }

    /** Floor braziers pop lava sparks only with normal fire, like vanilla's campfire versus soul campfire. */
    public boolean popsLava() {
        return this == NORMAL;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
