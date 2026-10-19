package com.dai.mcbraziers.datagen;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.item.ModItems;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.ItemLike;

/** The 79 recipes of the recipe contract ({@code docs/plans/recipes/brief-recipe-contract.md}). */
public class McBraziersRecipeProvider extends FabricRecipeProvider {

    private static final List<String> COPPER_STAGES = List.of("", "exposed_", "weathered_", "oxidized_");

    public McBraziersRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
            BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        return new RecipeProvider(recipes, advancements) {
            @Override
            public void buildRecipes() {
                buildBraziers();
                buildBars();
                buildModCampfires();
                buildWaxing();
            }

            // R3-R5: 8 bars around a campfire; bars set tier (and copper aging), campfire sets fire
            private void buildBraziers() {
                for (BrazierVariant variant : BrazierVariant.ALL) {
                    Item bars = item(variant.barsId());
                    for (BrazierFire fire : BrazierFire.values()) {
                        this.shaped(RecipeCategory.DECORATIONS, ModItems.brazierItem(variant, fire))
                                .pattern("BBB")
                                .pattern("BCB")
                                .pattern("BBB")
                                .define('B', bars)
                                .define('C', campfire(fire))
                                .group(variant.itemId(BrazierFire.NORMAL))
                                .unlockedBy(getHasName(bars), this.has(bars))
                                .save(this.output, key(variant.itemId(fire)));
                    }
                }
            }

            // R6: 8 bars around 1 material make 8 bars
            private void buildBars() {
                bars(ModItems.DIAMOND_BARS, Items.IRON_BARS, Items.DIAMOND);
                bars(ModItems.OBSIDIAN_BARS, Items.IRON_BARS, Items.OBSIDIAN);
                bars(ModItems.CRYING_OBSIDIAN_BARS, Items.IRON_BARS, Items.CRYING_OBSIDIAN);
                bars(ModItems.PRISMARINE_BARS, Items.IRON_BARS, Items.PRISMARINE_CRYSTALS);
                bars(ModItems.NETHERITE_BARS, ModItems.DIAMOND_BARS, Items.NETHERITE_INGOT);
            }

            private void bars(Item result, Item surround, Item center) {
                this.shaped(RecipeCategory.DECORATIONS, result, 8)
                        .pattern("XXX")
                        .pattern("XYX")
                        .pattern("XXX")
                        .define('X', surround)
                        .define('Y', center)
                        .unlockedBy(getHasName(surround), this.has(surround))
                        .unlockedBy(getHasName(center), this.has(center))
                        .save(this.output, key(getItemName(result)));
            }

            // R7: the vanilla campfire recipe with a copper nugget, or an amethyst shard, in place of the coal
            private void buildModCampfires() {
                campfire(ModItems.COPPER_CAMPFIRE, 'N', Items.COPPER_NUGGET);
                campfire(ModItems.AMETHYST_CAMPFIRE, 'A', Items.AMETHYST_SHARD);
            }

            private void campfire(Item result, char symbol, Item center) {
                this.shaped(RecipeCategory.DECORATIONS, result)
                        .pattern(" S ")
                        .pattern("S" + symbol + "S")
                        .pattern("LLL")
                        .define('S', Items.STICK)
                        .define(symbol, center)
                        .define('L', ItemTags.LOGS)
                        .unlockedBy(getHasName(center), this.has(center))
                        .save(this.output, key(getItemName(result)));
            }

            // R8: unwaxed copper brazier + honeycomb make the waxed brazier, vanilla's waxing convention
            private void buildWaxing() {
                for (String stage : COPPER_STAGES) {
                    BrazierVariant unwaxed = new BrazierVariant(BrazierTier.COPPER, stage);
                    BrazierVariant waxed = new BrazierVariant(BrazierTier.COPPER, "waxed_" + stage);
                    for (BrazierFire fire : BrazierFire.values()) {
                        Item from = ModItems.brazierItem(unwaxed, fire);
                        this.shapeless(RecipeCategory.BUILDING_BLOCKS, ModItems.brazierItem(waxed, fire))
                                .requires(from)
                                .requires(Items.HONEYCOMB)
                                .group("waxed_" + fire.idPrefix() + "copper_brazier")
                                .unlockedBy(getHasName(from), this.has(from))
                                .unlockedBy(getHasName(Items.HONEYCOMB), this.has(Items.HONEYCOMB))
                                .save(this.output, key(waxed.itemId(fire) + "_from_honeycomb"));
                    }
                }
            }

            private ItemLike campfire(BrazierFire fire) {
                return switch (fire) {
                    case NORMAL -> Items.CAMPFIRE;
                    case SOUL -> Items.SOUL_CAMPFIRE;
                    case COPPER -> ModItems.COPPER_CAMPFIRE;
                    case AMETHYST -> ModItems.AMETHYST_CAMPFIRE;
                };
            }
        };
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id))
                .orElseThrow(() -> new IllegalStateException("Unknown item in recipe contract: " + id));
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, path));
    }

    @Override
    public String getName() {
        return "MC Braziers recipes";
    }
}
