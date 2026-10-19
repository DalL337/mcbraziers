package com.dai.mcbraziers.datagen;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class McBraziersBlockTagProvider extends FabricTagsProvider.BlockTagsProvider {

    public static final TagKey<Block> BRAZIERS = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, "braziers"));

    public McBraziersBlockTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Campfire tag membership is what lets flint and steel light braziers and the mod's campfires
        var campfires = this.builder(BlockTags.CAMPFIRES);
        var braziers = this.builder(BRAZIERS);
        // Mining (PRD: Tiers, Bars): pickaxes for every brazier and bar block; stone or better except copper
        var pickaxe = this.builder(BlockTags.MINEABLE_WITH_PICKAXE);
        var stoneTool = this.builder(BlockTags.NEEDS_STONE_TOOL);
        for (Map.Entry<BrazierVariant, Block> entry : ModBlocks.BRAZIERS.entrySet()) {
            ResourceKey<Block> key = key(entry.getValue());
            campfires.add(key);
            braziers.add(key);
            pickaxe.add(key);
            if (entry.getKey().tier().needsStoneTool()) {
                stoneTool.add(key);
            }
        }
        for (Block bars : List.of(ModBlocks.DIAMOND_BARS, ModBlocks.OBSIDIAN_BARS, ModBlocks.CRYING_OBSIDIAN_BARS,
                ModBlocks.PRISMARINE_BARS, ModBlocks.NETHERITE_BARS)) {
            pickaxe.add(key(bars));
            stoneTool.add(key(bars));
        }
        // The mod's campfires mine like vanilla's: best with an axe, no tool needed to drop
        var axe = this.builder(BlockTags.MINEABLE_WITH_AXE);
        for (Block campfire : List.of(ModBlocks.COPPER_CAMPFIRE, ModBlocks.AMETHYST_CAMPFIRE)) {
            campfires.add(key(campfire));
            axe.add(key(campfire));
        }
    }

    private static ResourceKey<Block> key(Block block) {
        return BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow();
    }
}
