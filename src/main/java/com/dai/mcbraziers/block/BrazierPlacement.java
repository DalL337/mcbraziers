package com.dai.mcbraziers.block;

import net.minecraft.util.StringRepresentable;

public enum BrazierPlacement implements StringRepresentable {
    FLOOR("floor"),
    WALL("wall"),
    CEILING("ceiling");

    private final String name;

    BrazierPlacement(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }
}
