package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.gametest.mixin.SpriteContentsAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.SpriteContents;

/**
 * Pixel checks for the sprites the game recolours at load (PRD License addendum, recolouring at load):
 * a recolour keeps the vanilla sprite's frames and shape, and amethyst fire's turns every visible pixel
 * purple, from crying obsidian's dark purples to amethyst's light ones.
 */
final class SpritePixels {

    /** Hues counted as purple, in degrees: crying obsidian's purples sit near 262-275, amethyst's lightest near 291. */
    static final double PURPLE_FROM = 250;
    static final double PURPLE_TO = 300;

    private SpritePixels() {
    }

    /** What makes {@code recoloured} not a purple recolour of {@code vanilla}; empty when it is one. */
    static List<String> purpleRecolourProblems(SpriteContents recoloured, SpriteContents vanilla) {
        List<String> problems = new ArrayList<>();
        String name = recoloured.name().toString();
        if (recoloured.isAnimated() != vanilla.isAnimated() || recoloured.getUniqueFrames().size() != vanilla.getUniqueFrames().size()
                || recoloured.width() != vanilla.width() || recoloured.height() != vanilla.height()) {
            problems.add(name + " has " + recoloured.getUniqueFrames().size() + " frames of " + recoloured.width() + "x" + recoloured.height()
                    + (recoloured.isAnimated() ? ", animated" : ", still") + "; " + vanilla.name() + " has " + vanilla.getUniqueFrames().size()
                    + " of " + vanilla.width() + "x" + vanilla.height() + (vanilla.isAnimated() ? ", animated" : ", still"));
        }
        NativeImage image = ((SpriteContentsAccessor) recoloured).mcbraziers$originalImage();
        NativeImage original = ((SpriteContentsAccessor) vanilla).mcbraziers$originalImage();
        if (image.getWidth() != original.getWidth() || image.getHeight() != original.getHeight()) {
            problems.add(name + " is " + image.getWidth() + "x" + image.getHeight() + ", not " + original.getWidth() + "x" + original.getHeight());
            return problems;
        }
        int reshaped = 0;
        int visible = 0;
        int notPurple = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixel(x, y);
                if ((pixel >>> 24) != (original.getPixel(x, y) >>> 24)) {
                    reshaped++;
                }
                if ((pixel >>> 24) != 0) {
                    visible++;
                    double hue = hue(pixel);
                    if (hue < PURPLE_FROM || hue > PURPLE_TO) {
                        notPurple++;
                    }
                }
            }
        }
        if (reshaped > 0) {
            problems.add(name + " differs from " + vanilla.name() + " in the transparency of " + reshaped + " pixels");
        }
        if (visible == 0 || notPurple > 0) {
            problems.add(name + " has " + notPurple + " of " + visible + " visible pixels outside the purple hues");
        }
        return problems;
    }

    /** Hue in degrees, or -1 for a grey. */
    static double hue(int argb) {
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));
        if (max == min) {
            return -1;
        }
        double h;
        if (max == r) {
            h = ((double) (g - b) / (max - min)) % 6;
        } else if (max == g) {
            h = (double) (b - r) / (max - min) + 2;
        } else {
            h = (double) (r - g) / (max - min) + 4;
        }
        h *= 60;
        return h < 0 ? h + 360 : h;
    }
}
