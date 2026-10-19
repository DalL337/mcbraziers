package com.dai.mcbraziers.item;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModItems {

    /** The 56 brazier items: for each brazier block (in {@link BrazierVariant#ALL} order), one item per fire. */
    public static final Map<BrazierVariant, Map<BrazierFire, Item>> BRAZIERS = registerBraziers();

    // Placeable bars (copper and iron bars are vanilla's own)
    public static final Item DIAMOND_BARS = registerBlockItem("diamond_bars", ModBlocks.DIAMOND_BARS, new Item.Properties());
    public static final Item OBSIDIAN_BARS = registerBlockItem("obsidian_bars", ModBlocks.OBSIDIAN_BARS, new Item.Properties());
    public static final Item PRISMARINE_BARS = registerBlockItem("prismarine_bars", ModBlocks.PRISMARINE_BARS, new Item.Properties());
    public static final Item NETHERITE_BARS = registerBlockItem("netherite_bars", ModBlocks.NETHERITE_BARS,
            new Item.Properties().fireResistant());
    public static final Item CRYING_OBSIDIAN_BARS = registerBlockItem("crying_obsidian_bars", ModBlocks.CRYING_OBSIDIAN_BARS,
            new Item.Properties());

    public static final Item COPPER_CAMPFIRE = registerBlockItem("copper_campfire", ModBlocks.COPPER_CAMPFIRE, new Item.Properties());
    public static final Item AMETHYST_CAMPFIRE = registerBlockItem("amethyst_campfire", ModBlocks.AMETHYST_CAMPFIRE, new Item.Properties());

    public static Item brazierItem(BrazierVariant variant, BrazierFire fire) {
        return BRAZIERS.get(variant).get(fire);
    }

    /** The item for a placed brazier block and fire (pick-block, loot). */
    public static Item brazierItem(Block block, BrazierFire fire) {
        for (Map.Entry<BrazierVariant, Block> entry : ModBlocks.BRAZIERS.entrySet()) {
            if (entry.getValue() == block) {
                return brazierItem(entry.getKey(), fire);
            }
        }
        throw new IllegalArgumentException("Not a brazier block: " + block);
    }

    private static Map<BrazierVariant, Map<BrazierFire, Item>> registerBraziers() {
        Map<BrazierVariant, Map<BrazierFire, Item>> braziers = new LinkedHashMap<>();
        for (BrazierVariant variant : BrazierVariant.ALL) {
            Block block = ModBlocks.BRAZIERS.get(variant);
            Map<BrazierFire, Item> byFire = new EnumMap<>(BrazierFire.class);
            for (BrazierFire fire : BrazierFire.values()) {
                ResourceKey<Item> key = itemKey(variant.itemId(fire));
                Item.Properties properties = new Item.Properties().setId(key);
                if (variant.tier().fireResistant()) {
                    properties = properties.fireResistant();
                }
                byFire.put(fire, Registry.register(BuiltInRegistries.ITEM, key, new BrazierItem(block, fire, properties)));
            }
            braziers.put(variant, Collections.unmodifiableMap(byFire));
        }
        return Collections.unmodifiableMap(braziers);
    }

    private static Item registerBlockItem(String name, Block block, Item.Properties properties) {
        ResourceKey<Item> key = itemKey(name);
        return Registry.register(BuiltInRegistries.ITEM, key,
                new BlockItem(block, properties.setId(key).useBlockDescriptionPrefix()));
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, name));
    }

    public static void register() {
        McBraziers.LOGGER.info("Registering items for " + McBraziers.MOD_ID);
    }
}
