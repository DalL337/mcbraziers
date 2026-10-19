package com.dai.mcbraziers.mixin;

import com.dai.mcbraziers.block.BrazierBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Splash water puts out lit campfires through a private method, so water-proof braziers (prismarine,
 * netherite) need this hook to stay lit (PRD: Lighting and Putting Out). Every other block, including
 * other braziers, keeps vanilla's behavior.
 */
@Mixin(AbstractThrownPotion.class)
abstract class AbstractThrownPotionMixin {

    @Inject(method = "douseFire", at = @At("HEAD"), cancellable = true)
    private void mcbraziers$keepWaterProofBraziersLit(BlockPos pos, CallbackInfo info) {
        BlockState state = ((AbstractThrownPotion) (Object) this).level().getBlockState(pos);
        if (state.getBlock() instanceof BrazierBlock brazier && brazier.tier().waterProof()) {
            info.cancel();
        }
    }
}
