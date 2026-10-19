package com.dai.mcbraziers.registry;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.item.ModItems;
import java.util.Map;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {

    public static final CreativeModeTab MC_BRAZIERS_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, "mcbraziers"),
            FabricCreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.brazierItem(new BrazierVariant(BrazierTier.COPPER, ""), BrazierFire.NORMAL)))
                    .title(Component.translatable("itemGroup.mcbraziers"))
                    .displayItems((parameters, output) -> {
                        // Braziers grouped by block, each block's fire types together
                        for (Map<BrazierFire, Item> byFire : ModItems.BRAZIERS.values()) {
                            for (Item item : byFire.values()) {
                                output.accept(item);
                            }
                        }
                        output.accept(ModItems.DIAMOND_BARS);
                        output.accept(ModItems.OBSIDIAN_BARS);
                        output.accept(ModItems.CRYING_OBSIDIAN_BARS);
                        output.accept(ModItems.PRISMARINE_BARS);
                        output.accept(ModItems.NETHERITE_BARS);
                        output.accept(ModItems.COPPER_CAMPFIRE);
                        output.accept(ModItems.AMETHYST_CAMPFIRE);
                    })
                    .build()
    );

    public static void register() {
        McBraziers.LOGGER.info("Registering item groups for " + McBraziers.MOD_ID);
    }
}
