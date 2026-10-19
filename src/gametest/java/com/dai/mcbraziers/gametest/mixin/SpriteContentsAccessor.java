package com.dai.mcbraziers.gametest.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Test-only access to a sprite's pixels, for checking recoloured sprites ({@code SpritePixels}). */
@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {

    @Accessor("originalImage")
    NativeImage mcbraziers$originalImage();
}
