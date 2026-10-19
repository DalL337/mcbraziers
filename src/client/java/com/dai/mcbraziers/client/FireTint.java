package com.dai.mcbraziers.client;

import com.dai.mcbraziers.block.BrazierFire;

/**
 * Flame tints (PRD License addendum, 2026-10-18). Copper fire draws vanilla's soul campfire flame tinted
 * green, close to the copper torch's flame. Amethyst fire draws a purple copy of that flame recoloured at
 * load instead (RecoloredSprite), since a tint can't make the teal flame purple.
 */
public final class FireTint {

    /** ARGB tint for copper fire. */
    public static final int COPPER = 0xFFBFFF73;
    /** No tint: the flame draws as vanilla made it. */
    public static final int NONE = -1;

    private FireTint() {
    }

    /** The tint for a fire's flame; the other fires draw their flames as they are. */
    public static int of(BrazierFire fire) {
        return switch (fire) {
            case NORMAL, SOUL, AMETHYST -> NONE;
            case COPPER -> COPPER;
        };
    }
}
