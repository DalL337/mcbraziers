package com.dai.mcbraziers.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Generates the mod's data (recipes, loot tables, tags, English names) into {@code src/main/generated}.
 * Run with {@code ./gradlew runDatagen}; the output is committed. Recipes follow the recipe contract
 * ({@code docs/plans/recipes/brief-recipe-contract.md}) and are never edited by hand.
 */
public class McBraziersDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(McBraziersRecipeProvider::new);
        pack.addProvider(McBraziersLootProvider::new);
        pack.addProvider(McBraziersBlockTagProvider::new);
        pack.addProvider(McBraziersLanguageProvider::new);
        pack.addProvider(McBraziersModelProvider::new);
    }
}
