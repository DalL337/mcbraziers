package com.dai.mcbraziers.datagen;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.item.ModItems;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.core.HolderLookup;

/** English names. Names follow the IDs word for word, the way vanilla names copper blocks (recipe contract, Item IDs). */
public class McBraziersLanguageProvider extends FabricLanguageProvider {

    public McBraziersLanguageProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void generateTranslations(HolderLookup.Provider registries, TranslationBuilder builder) {
        for (BrazierVariant variant : BrazierVariant.ALL) {
            builder.add(ModBlocks.BRAZIERS.get(variant), titleCase(variant.blockId()));
            for (BrazierFire fire : BrazierFire.values()) {
                builder.add(ModItems.brazierItem(variant, fire), titleCase(variant.itemId(fire)));
            }
        }
        builder.add(ModBlocks.DIAMOND_BARS, "Diamond Bars");
        builder.add(ModBlocks.OBSIDIAN_BARS, "Obsidian Bars");
        builder.add(ModBlocks.CRYING_OBSIDIAN_BARS, "Crying Obsidian Bars");
        builder.add(ModBlocks.PRISMARINE_BARS, "Prismarine Bars");
        builder.add(ModBlocks.NETHERITE_BARS, "Netherite Bars");
        builder.add(ModBlocks.COPPER_CAMPFIRE, "Copper Campfire");
        builder.add(ModBlocks.AMETHYST_CAMPFIRE, "Amethyst Campfire");
        builder.add("itemGroup.mcbraziers", "MC Braziers");
        builder.add(BrazierBlock.GRATE_HAS_FOOD, "The grate can't open while food is cooking.");
    }

    /** {@code waxed_weathered_soul_copper_brazier} → {@code Waxed Weathered Soul Copper Brazier}. */
    static String titleCase(String id) {
        return Arrays.stream(id.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
