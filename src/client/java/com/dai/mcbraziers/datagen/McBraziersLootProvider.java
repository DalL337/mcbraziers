package com.dai.mcbraziers.datagen;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.item.ModItems;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootSubProvider;
import net.minecraft.advancements.predicates.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.MatchBlock;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProviders;

/** Every brazier drops its own item for its fire type; bars drop themselves; the mod's campfires drop like vanilla's. */
public class McBraziersLootProvider extends FabricBlockLootSubProvider {

    public McBraziersLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generate() {
        for (Map.Entry<BrazierVariant, Block> entry : ModBlocks.BRAZIERS.entrySet()) {
            Block block = entry.getValue();
            LootPool.Builder pool = LootPool.lootPool().setRolls(ContextIntProviders.exactly(1));
            for (BrazierFire fire : BrazierFire.values()) {
                pool.add(LootItem.lootTableItem(ModItems.brazierItem(entry.getKey(), fire))
                        .when(MatchBlock.blockMatches(this.blocks, block,
                                StatePropertiesPredicate.Builder.properties().hasProperty(BrazierBlock.FIRE, fire.getSerializedName()))));
            }
            this.add(block, LootTable.lootTable().withPool(this.applyExplosionCondition(block, pool)));
        }

        this.dropSelf(ModBlocks.DIAMOND_BARS);
        this.dropSelf(ModBlocks.OBSIDIAN_BARS);
        this.dropSelf(ModBlocks.CRYING_OBSIDIAN_BARS);
        this.dropSelf(ModBlocks.PRISMARINE_BARS);
        this.dropSelf(ModBlocks.NETHERITE_BARS);

        // Same as vanilla's campfire: itself with silk touch, otherwise 2 charcoal
        for (Block campfire : List.of(ModBlocks.COPPER_CAMPFIRE, ModBlocks.AMETHYST_CAMPFIRE)) {
            this.add(campfire, block -> this.createSilkTouchDispatchTable(block,
                    this.applyExplosionCondition(block,
                            LootItem.lootTableItem(Items.CHARCOAL).apply(SetItemCountFunction.setCount(ContextIntProviders.exactly(2))))));
        }
    }
}
