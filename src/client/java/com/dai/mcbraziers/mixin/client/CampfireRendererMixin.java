package com.dai.mcbraziers.mixin.client;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.FoodLayout;
import com.dai.mcbraziers.client.BrazierFoodLayout;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.blockentity.CampfireRenderer;
import net.minecraft.client.renderer.blockentity.state.CampfireRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Braziers use vanilla's campfire block entity, so vanilla's campfire renderer draws their food. This lays
 * it out on the brazier's grate ({@link BrazierBlock#foodLayout}); campfires keep vanilla's layout.
 */
@Mixin(CampfireRenderer.class)
abstract class CampfireRendererMixin {

    private static final String SUBMIT = "submit(Lnet/minecraft/client/renderer/blockentity/state/CampfireRenderState;"
            + "Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;"
            + "Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V";
    private static final String TRANSLATE = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V";
    private static final String SCALE = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V";

    @Inject(method = "extractRenderState(Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;"
            + "Lnet/minecraft/client/renderer/blockentity/state/CampfireRenderState;FLnet/minecraft/world/phys/Vec3;"
            + "Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V", at = @At("TAIL"))
    private void mcbraziers$recordFoodLayout(CampfireBlockEntity campfire, CampfireRenderState state, float partialTicks,
            Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress, CallbackInfo info) {
        BlockState block = campfire.getBlockState();
        ((BrazierFoodLayout) state).mcbraziers$setFoodLayout(
                block.getBlock() instanceof BrazierBlock ? BrazierBlock.foodLayout(block) : FoodLayout.CAMPFIRE);
    }

    /** The first translate moves to the middle of the food, at its height. */
    @WrapOperation(method = SUBMIT, at = @At(value = "INVOKE", target = TRANSLATE, ordinal = 0))
    private void mcbraziers$placeFood(PoseStack poseStack, float x, float y, float z, Operation<Void> original,
            @Local(argsOnly = true) CampfireRenderState state) {
        FoodLayout food = ((BrazierFoodLayout) state).mcbraziers$foodLayout();
        original.call(poseStack, food.centerX(), food.height(), food.centerZ());
    }

    /** The second moves each item out from the middle, by the same amount on both axes. */
    @WrapOperation(method = SUBMIT, at = @At(value = "INVOKE", target = TRANSLATE, ordinal = 1))
    private void mcbraziers$spaceFood(PoseStack poseStack, float x, float y, float z, Operation<Void> original,
            @Local(argsOnly = true) CampfireRenderState state) {
        float spacing = ((BrazierFoodLayout) state).mcbraziers$foodLayout().spacing();
        original.call(poseStack, -spacing, -spacing, z);
    }

    @WrapOperation(method = SUBMIT, at = @At(value = "INVOKE", target = SCALE, ordinal = 0))
    private void mcbraziers$sizeFood(PoseStack poseStack, float x, float y, float z, Operation<Void> original,
            @Local(argsOnly = true) CampfireRenderState state) {
        float scale = ((BrazierFoodLayout) state).mcbraziers$foodLayout().scale();
        original.call(poseStack, scale, scale, scale);
    }
}
