package com.dai.mcbraziers.client;

import com.dai.mcbraziers.McBraziers;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.IntUnaryOperator;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import org.jspecify.annotations.Nullable;

/**
 * An atlas sprite source that recolours one of the game's own textures while the atlas loads, keeping its
 * animation. The mod ships only the colour ramp, never the image: the texture comes from the player's own
 * game files. Each pixel's brightness, scaled over the texture's darkest to brightest pixel, picks a colour
 * from the ramp, which runs dark to bright.
 *
 * <p>Vanilla's {@code paletted_permutations} source recolours too, but builds a single-frame sprite, which
 * would flatten an animation strip.
 */
public record RecoloredSprite(Identifier source, Identifier sprite, List<String> ramp) implements SpriteSource {

    public static final MapCodec<RecoloredSprite> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("source").forGetter(RecoloredSprite::source),
            Identifier.CODEC.fieldOf("sprite").forGetter(RecoloredSprite::sprite),
            Codec.STRING.listOf(2, Integer.MAX_VALUE).fieldOf("ramp").forGetter(RecoloredSprite::ramp)
    ).apply(instance, RecoloredSprite::new));

    @Override
    public void run(ResourceManager resourceManager, SpriteSource.Output output) {
        Identifier file = TEXTURE_ID_CONVERTER.idToFile(this.source);
        Optional<Resource> resource = resourceManager.getResource(file);
        if (resource.isEmpty()) {
            McBraziers.LOGGER.warn("Missing texture to recolour: {}", file);
            return;
        }
        output.add(this.sprite, loader -> this.load(resource.get()));
    }

    private @Nullable SpriteContents load(Resource resource) {
        long start = System.nanoTime();
        try {
            ResourceMetadata metadata = resource.metadata();
            Optional<AnimationMetadataSection> animation = metadata.getSection(AnimationMetadataSection.TYPE);
            Optional<TextureMetadataSection> texture = metadata.getSection(TextureMetadataSection.TYPE);
            NativeImage recoloured;
            try (InputStream in = resource.open(); NativeImage original = NativeImage.read(in)) {
                recoloured = original.mappedCopy(this.rampOver(original));
            }
            FrameSize frameSize = animation
                    .map(section -> section.calculateFrameSize(recoloured.getWidth(), recoloured.getHeight()))
                    .orElse(new FrameSize(recoloured.getWidth(), recoloured.getHeight()));
            McBraziers.LOGGER.debug("Recoloured {} into {} in {} ms", this.source, this.sprite,
                    String.format("%.2f", (System.nanoTime() - start) / 1e6));
            return new SpriteContents(this.sprite, frameSize, recoloured, animation, List.of(), texture);
        } catch (IOException | IllegalArgumentException e) {
            McBraziers.LOGGER.error("Couldn't recolour {}", this.source, e);
            return null;
        }
    }

    /** Maps each visible pixel to the ramp by its brightness within the image's own range; alpha is kept. */
    private IntUnaryOperator rampOver(NativeImage image) {
        float darkest = Float.MAX_VALUE;
        float brightest = -Float.MAX_VALUE;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixel(x, y);
                if ((pixel >>> 24) != 0) {
                    darkest = Math.min(darkest, brightness(pixel));
                    brightest = Math.max(brightest, brightness(pixel));
                }
            }
        }
        int[] colours = this.ramp.stream().mapToInt(hex -> Integer.parseInt(hex.substring(1), 16)).toArray();
        float low = darkest;
        float range = Math.max(brightest - darkest, 1e-3f);
        return pixel -> {
            if ((pixel >>> 24) == 0) {
                return pixel;
            }
            float position = (brightness(pixel) - low) / range * (colours.length - 1);
            int index = Math.min((int) position, colours.length - 2);
            return (pixel & 0xFF000000) | lerp(colours[index], colours[index + 1], position - index);
        };
    }

    private static float brightness(int argb) {
        return 0.299f * ((argb >> 16) & 0xFF) + 0.587f * ((argb >> 8) & 0xFF) + 0.114f * (argb & 0xFF);
    }

    private static int lerp(int from, int to, float t) {
        int rgb = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int a = (from >> shift) & 0xFF;
            int b = (to >> shift) & 0xFF;
            rgb |= Math.round(a + (b - a) * t) << shift;
        }
        return rgb;
    }

    @Override
    public MapCodec<RecoloredSprite> codec() {
        return MAP_CODEC;
    }
}
