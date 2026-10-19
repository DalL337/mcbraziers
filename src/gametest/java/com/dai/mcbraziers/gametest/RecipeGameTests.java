package com.dai.mcbraziers.gametest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * In-world half of the recipe contract ({@code docs/plans/recipes/brief-recipe-contract.md}): the server
 * loads every contract recipe (R2, and R9: a recipe naming an unknown item fails to load), and sample
 * grids craft the expected item. The file-level rules are checked by {@code RecipeContractTest}.
 */
public class RecipeGameTests {

    private static final int CONTRACT_RECIPE_COUNT = 79;

    @GameTest
    public void allContractRecipesLoad(GameTestHelper helper) {
        RecipeManager recipes = helper.getLevel().getServer().getRecipeManager();
        long loaded = recipes.getRecipes().stream()
                .filter(holder -> holder.id().identifier().getNamespace().equals("mcbraziers"))
                .count();
        helper.assertTrue(loaded == CONTRACT_RECIPE_COUNT,
                "expected " + CONTRACT_RECIPE_COUNT + " mcbraziers recipes, the server loaded " + loaded);
        helper.succeed();
    }

    @GameTest
    public void sampleGridsCraftTheContractItem(GameTestHelper helper) {
        // R3-R5: 8 bars around a campfire; bars set tier and copper aging, campfire sets fire
        assertCrafts(helper, surround("minecraft:iron_bars", "minecraft:campfire"), "mcbraziers:iron_brazier");
        assertCrafts(helper, surround("mcbraziers:diamond_bars", "minecraft:soul_campfire"), "mcbraziers:soul_diamond_brazier");
        assertCrafts(helper, surround("minecraft:waxed_weathered_copper_bars", "mcbraziers:copper_campfire"),
                "mcbraziers:waxed_weathered_copper_fire_copper_brazier");
        assertCrafts(helper, surround("mcbraziers:crying_obsidian_bars", "mcbraziers:amethyst_campfire"),
                "mcbraziers:crying_amethyst_fire_obsidian_brazier");
        // R6: 8 bars around 1 material
        assertCrafts(helper, surround("mcbraziers:diamond_bars", "minecraft:netherite_ingot"), "mcbraziers:netherite_bars");
        assertCrafts(helper, surround("minecraft:iron_bars", "minecraft:crying_obsidian"), "mcbraziers:crying_obsidian_bars");
        // R7: the vanilla campfire recipe with a copper nugget in place of the coal
        assertCrafts(helper, grid(
                "", "minecraft:stick", "",
                "minecraft:stick", "minecraft:copper_nugget", "minecraft:stick",
                "minecraft:oak_log", "minecraft:oak_log", "minecraft:oak_log"), "mcbraziers:copper_campfire");
        // R7 amendment: an amethyst shard in place of the coal
        assertCrafts(helper, grid(
                "", "minecraft:stick", "",
                "minecraft:stick", "minecraft:amethyst_shard", "minecraft:stick",
                "minecraft:birch_log", "minecraft:birch_log", "minecraft:birch_log"), "mcbraziers:amethyst_campfire");
        // R8: waxing
        assertCrafts(helper, CraftingInput.of(2, 1, List.of(stack("mcbraziers:exposed_soul_copper_brazier"), stack("minecraft:honeycomb"))),
                "mcbraziers:waxed_exposed_soul_copper_brazier");
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, CraftingInput input, String expectedResult) {
        Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent(), "no recipe matched the grid for " + expectedResult);
        ItemStack result = recipe.orElseThrow().value().assemble(input);
        String actual = BuiltInRegistries.ITEM.getKey(result.getItem()).toString();
        helper.assertTrue(actual.equals(expectedResult), "grid crafted " + actual + ", expected " + expectedResult);
    }

    private static CraftingInput surround(String outer, String center) {
        return grid(outer, outer, outer, outer, center, outer, outer, outer, outer);
    }

    private static CraftingInput grid(String... cells) {
        List<ItemStack> stacks = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        for (int i = 0; i < cells.length; i++) {
            if (!cells[i].isEmpty()) {
                stacks.set(i, stack(cells[i]));
            }
        }
        return CraftingInput.of(3, 3, stacks);
    }

    private static ItemStack stack(String id) {
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id))
                .orElseThrow(() -> new IllegalStateException("unknown item " + id));
        return new ItemStack(item);
    }
}
