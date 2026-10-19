package com.dai.mcbraziers.block;

/**
 * Where food is drawn on a campfire block entity, in blocks: the centre of the four items, their height,
 * how far each item's centre sits from that centre on both axes, and the item scale. Vanilla's campfire
 * renderer lays items out this way; braziers change the numbers to fit their grates.
 */
public record FoodLayout(float centerX, float height, float centerZ, float spacing, float scale) {

    /** Vanilla's campfire: 6 px items, 5 px out from the middle, 3/16 px above its 7 px top. */
    public static final FoodLayout CAMPFIRE = new FoodLayout(0.5f, 7.1875f / 16, 0.5f, 5f / 16, 0.375f);
}
