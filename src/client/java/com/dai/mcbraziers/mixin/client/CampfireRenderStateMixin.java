package com.dai.mcbraziers.mixin.client;

import com.dai.mcbraziers.block.FoodLayout;
import com.dai.mcbraziers.client.BrazierFoodLayout;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CampfireRenderState.class)
abstract class CampfireRenderStateMixin implements BrazierFoodLayout {

    @Unique
    private FoodLayout mcbraziers$foodLayout = FoodLayout.CAMPFIRE;

    @Override
    public FoodLayout mcbraziers$foodLayout() {
        return this.mcbraziers$foodLayout;
    }

    @Override
    public void mcbraziers$setFoodLayout(FoodLayout layout) {
        this.mcbraziers$foodLayout = layout;
    }
}
