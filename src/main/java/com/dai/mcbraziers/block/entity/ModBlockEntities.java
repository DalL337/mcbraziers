package com.dai.mcbraziers.block.entity;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.ModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityTypes;

public class ModBlockEntities {

    /**
     * Floor braziers and the mod's campfires use the vanilla campfire block entity, which gives them
     * campfire cooking, the items-on-top renderer, and cooking smoke. The campfire block entity type
     * only accepts campfires by default, so each of these blocks is added as a valid block.
     */
    public static void register() {
        for (Block brazier : ModBlocks.BRAZIERS.values()) {
            BlockEntityTypes.CAMPFIRE.addValidBlock(brazier);
        }
        BlockEntityTypes.CAMPFIRE.addValidBlock(ModBlocks.COPPER_CAMPFIRE);
        BlockEntityTypes.CAMPFIRE.addValidBlock(ModBlocks.AMETHYST_CAMPFIRE);
        McBraziers.LOGGER.info("Registering block entities for " + McBraziers.MOD_ID);
    }
}
