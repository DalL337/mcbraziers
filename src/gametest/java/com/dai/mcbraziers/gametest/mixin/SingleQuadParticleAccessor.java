package com.dai.mcbraziers.gametest.mixin;

import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only access to the sprite and colour a particle draws with. */
@Mixin(SingleQuadParticle.class)
public interface SingleQuadParticleAccessor {

    @Accessor("sprite")
    TextureAtlasSprite mcbraziers$sprite();

    @Accessor("rCol")
    float mcbraziers$red();

    @Accessor("gCol")
    float mcbraziers$green();

    @Accessor("bCol")
    float mcbraziers$blue();
}
