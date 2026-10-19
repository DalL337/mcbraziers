package com.dai.mcbraziers.client;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.particle.ModParticles;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockColorRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.SpriteSourceRegistry;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.color.block.BlockTintSources;
import net.minecraft.client.particle.FlameParticle;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public class McBraziersClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Render layers need no registration: since 26.x each quad's render layer (solid/cutout/translucent)
        // comes from its texture's transparency.

        // Atlas sprites recoloured at load from the player's own vanilla textures (assets/minecraft/atlases)
        SpriteSourceRegistry.register(Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, "recolor"), RecoloredSprite.MAP_CODEC);

        // Copper fire is vanilla's soul campfire flame, tinted (FireTint). Flame faces use tint index 0; the
        // other fires draw untinted.
        BlockColorRegistry.register(List.of(new BlockTintSource() {
            @Override
            public int color(BlockState state) {
                return FireTint.of(state.getValue(BrazierBlock.FIRE));
            }

            @Override
            public Set<Property<?>> relevantProperties() {
                return Set.of(BrazierBlock.FIRE);
            }
        }), ModBlocks.BRAZIERS.values().toArray(Block[]::new));
        BlockColorRegistry.register(List.of(BlockTintSources.constant(FireTint.COPPER)), ModBlocks.COPPER_CAMPFIRE);

        // Amethyst fire's flame particle: vanilla's flame particle, drawn with the soul fire flame sprite
        // recoloured purple at load (particles/amethyst_fire_flame.json, atlases/particles.json)
        ParticleProviderRegistry.getInstance().register(ModParticles.AMETHYST_FIRE_FLAME, FlameParticle.Provider::new);
    }
}
