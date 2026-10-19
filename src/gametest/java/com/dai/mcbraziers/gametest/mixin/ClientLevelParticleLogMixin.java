package com.dai.mcbraziers.gametest.mixin;

import com.dai.mcbraziers.gametest.ParticleLog;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Test-only: every client particle request passes through {@code doAddParticle}, so it is logged there. */
@Mixin(ClientLevel.class)
abstract class ClientLevelParticleLogMixin {

    @Inject(method = "doAddParticle", at = @At("HEAD"))
    private void mcbraziersGametest$log(ParticleOptions particle, boolean overrideLimiter, boolean alwaysShowParticles,
            double x, double y, double z, double xd, double yd, double zd, CallbackInfo info) {
        ParticleLog.record(particle.getType(), x, y, z, yd);
    }
}
