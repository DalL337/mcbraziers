package com.dai.mcbraziers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * File-level half of the recipe contract ({@code docs/plans/recipes/brief-recipe-contract.md}).
 *
 * <p>The expected recipes are re-derived here from the contract's rules, independently of the data
 * generator, and compared with the committed files in {@code src/main/generated}. A hand edit or a
 * generator change that breaks a rule fails the build. The in-world half (recipes load, sample grids
 * craft) is {@code RecipeGameTests} in the game test source set.
 */
class RecipeContractTest {

    private static final Path GENERATED = Path.of("src/main/generated/data/mcbraziers");
    private static final String MOD = "mcbraziers:";

    private static final List<String> COPPER_AGING = List.of(
            "", "exposed_", "weathered_", "oxidized_", "waxed_", "waxed_exposed_", "waxed_weathered_", "waxed_oxidized_");
    private static final List<String> COPPER_STAGES = List.of("", "exposed_", "weathered_", "oxidized_");
    /** R5: the bars of the other tiers, as (look prefix, tier, bars). Crying obsidian is obsidian with the prefix {@code crying_}. */
    private static final List<List<String>> OTHER_TIER_BARS = List.of(
            List.of("", "iron", "minecraft:iron_bars"),
            List.of("", "diamond", MOD + "diamond_bars"),
            List.of("", "obsidian", MOD + "obsidian_bars"),
            List.of("crying_", "obsidian", MOD + "crying_obsidian_bars"),
            List.of("", "prismarine", MOD + "prismarine_bars"),
            List.of("", "netherite", MOD + "netherite_bars"));
    /** R4: fire prefix in item IDs → campfire ingredient. */
    private static final Map<String, String> FIRES = orderedMap(
            "", "minecraft:campfire",
            "soul_", "minecraft:soul_campfire",
            "copper_fire_", MOD + "copper_campfire",
            "amethyst_fire_", MOD + "amethyst_campfire");

    /** One recipe, reduced to the fields the contract governs. Absent category means "misc". */
    record Recipe(String type, List<String> pattern, Map<String, String> key, List<String> ingredients,
                  String result, int count, String group, String category) {
    }

    record Expected(Recipe recipe, Set<String> unlockedBy) {
    }

    // --- the contract, derived from its rules ---

    static Map<String, Expected> contract() {
        Map<String, Expected> recipes = new TreeMap<>();
        // R3-R5: braziers, 8 bars around a campfire; each entry is (look prefix, tier, bars)
        List<List<String>> allBars = new ArrayList<>();
        for (String aging : COPPER_AGING) {
            allBars.add(List.of(aging, "copper", "minecraft:" + aging + "copper_bars"));
        }
        allBars.addAll(OTHER_TIER_BARS);
        allBars.forEach(entry -> {
            // the look prefix goes before the fire prefix: weathered_soul_copper_brazier, crying_soul_obsidian_brazier
            String aging = entry.get(0);
            String tier = entry.get(1);
            String bars = entry.get(2);
            FIRES.forEach((fire, campfire) -> {
                String id = aging + fire + tier + "_brazier";
                recipes.put(id, new Expected(new Recipe("minecraft:crafting_shaped", List.of("BBB", "BCB", "BBB"),
                        orderedMap("B", bars, "C", campfire), null, MOD + id, 1, aging + tier + "_brazier", "misc"),
                        Set.of(bars)));
            });
        });
        // R6: bars, 8 + 1 → 8
        bars(recipes, "diamond_bars", "minecraft:iron_bars", "minecraft:diamond");
        bars(recipes, "obsidian_bars", "minecraft:iron_bars", "minecraft:obsidian");
        bars(recipes, "crying_obsidian_bars", "minecraft:iron_bars", "minecraft:crying_obsidian");
        bars(recipes, "prismarine_bars", "minecraft:iron_bars", "minecraft:prismarine_crystals");
        bars(recipes, "netherite_bars", MOD + "diamond_bars", "minecraft:netherite_ingot");
        // R7: Copper Campfire and Amethyst Campfire
        recipes.put("copper_campfire", new Expected(new Recipe("minecraft:crafting_shaped", List.of(" S ", "SNS", "LLL"),
                orderedMap("L", "#minecraft:logs", "N", "minecraft:copper_nugget", "S", "minecraft:stick"),
                null, MOD + "copper_campfire", 1, null, "misc"), Set.of("minecraft:copper_nugget")));
        recipes.put("amethyst_campfire", new Expected(new Recipe("minecraft:crafting_shaped", List.of(" S ", "SAS", "LLL"),
                orderedMap("A", "minecraft:amethyst_shard", "L", "#minecraft:logs", "S", "minecraft:stick"),
                null, MOD + "amethyst_campfire", 1, null, "misc"), Set.of("minecraft:amethyst_shard")));
        // R8: waxing
        for (String stage : COPPER_STAGES) {
            for (String fire : FIRES.keySet()) {
                String unwaxed = MOD + stage + fire + "copper_brazier";
                String waxed = "waxed_" + stage + fire + "copper_brazier";
                recipes.put(waxed + "_from_honeycomb", new Expected(new Recipe("minecraft:crafting_shapeless", null, null,
                        List.of(unwaxed, "minecraft:honeycomb"), MOD + waxed, 1, "waxed_" + fire + "copper_brazier", "building"),
                        Set.of(unwaxed, "minecraft:honeycomb")));
            }
        }
        return recipes;
    }

    private static void bars(Map<String, Expected> recipes, String id, String surround, String center) {
        recipes.put(id, new Expected(new Recipe("minecraft:crafting_shaped", List.of("XXX", "XYX", "XXX"),
                orderedMap("X", surround, "Y", center), null, MOD + id, 8, null, "misc"), Set.of(surround, center)));
    }

    // --- tests ---

    @Test
    void theContractHas79Recipes() {
        assertEquals(79, contract().size(), "the contract's recipe count (brief: R2 amendment)");
    }

    /** R2 and R13: exactly the contract's recipes, named after their results. */
    @Test
    void generatedRecipesAreExactlyTheContract() {
        Set<String> expected = new TreeSet<>(contract().keySet());
        Set<String> actual = new TreeSet<>(generatedRecipes().keySet());
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(actual);
        Set<String> extra = new TreeSet<>(actual);
        extra.removeAll(expected);
        assertTrue(missing.isEmpty() && extra.isEmpty(), "missing: " + missing + "; extra: " + extra);
    }

    /** R1, R3-R8, R12, R14: every committed recipe equals its contract entry, field for field. */
    @Test
    void everyRecipeMatchesItsContractEntry() {
        Map<String, Recipe> generated = generatedRecipes();
        List<String> mismatches = new ArrayList<>();
        contract().forEach((id, expected) -> {
            Recipe actual = generated.get(id);
            if (actual != null && !actual.equals(expected.recipe())) {
                mismatches.add(id + "\n  expected " + expected.recipe() + "\n  actual   " + actual);
            }
        });
        assertTrue(mismatches.isEmpty(), String.join("\n", mismatches));
    }

    /** R11: each recipe enters the recipe book when the player first holds its key ingredient. */
    @Test
    void everyRecipeUnlocksFromItsKeyIngredient() {
        Map<String, Set<String>> unlocks = generatedUnlocks();
        List<String> problems = new ArrayList<>();
        contract().forEach((id, expected) -> {
            Set<String> actual = unlocks.get(MOD + id);
            if (actual == null) {
                problems.add(id + ": no recipe-book unlock");
            } else if (!actual.equals(expected.unlockedBy())) {
                problems.add(id + ": unlocked by " + actual + ", expected " + expected.unlockedBy());
            }
        });
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }

    /** R10: no vanilla 26.3 crafting recipe accepts a grid that a mod recipe accepts. */
    @Test
    void noVanillaRecipeAcceptsTheSameGrid() throws IOException {
        try (FileSystem jar = FileSystems.newFileSystem(minecraftJar())) {
            Map<String, Set<String>> tags = new HashMap<>();
            Path tagDir = jar.getPath("data/minecraft/tags/item");
            Map<String, JsonObject> rawTags = readJsonTree(tagDir);
            rawTags.keySet().forEach(name -> expandTag("minecraft:" + name, rawTags, tags, new HashSet<>()));

            List<Grid> vanilla = new ArrayList<>();
            readJsonTree(jar.getPath("data/minecraft/recipe")).forEach((name, json) -> {
                Grid grid = Grid.of(json, tags);
                if (grid != null) {
                    vanilla.add(grid.named("minecraft:" + name));
                }
            });
            assertTrue(vanilla.size() > 500, "expected the vanilla crafting recipes, found " + vanilla.size());

            List<String> conflicts = new ArrayList<>();
            readJsonTree(GENERATED.resolve("recipe")).forEach((name, json) -> {
                Grid mod = Grid.of(json, tags);
                for (Grid other : vanilla) {
                    if (mod.overlaps(other)) {
                        conflicts.add(MOD + name + " overlaps " + other.name());
                    }
                }
            });
            assertTrue(conflicts.isEmpty(), String.join("\n", conflicts));
        }
    }

    // --- reading the generated files ---

    static Map<String, Recipe> generatedRecipes() {
        Map<String, Recipe> recipes = new TreeMap<>();
        readJsonTree(GENERATED.resolve("recipe")).forEach((name, json) -> recipes.put(name, toRecipe(json)));
        return recipes;
    }

    static Recipe toRecipe(JsonObject json) {
        List<String> pattern = null;
        Map<String, String> key = null;
        List<String> ingredients = null;
        if (json.has("pattern")) {
            pattern = new ArrayList<>();
            for (JsonElement row : json.getAsJsonArray("pattern")) {
                pattern.add(row.getAsString());
            }
            key = new TreeMap<>();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("key").entrySet()) {
                key.put(entry.getKey(), ingredientText(entry.getValue()));
            }
        }
        if (json.has("ingredients")) {
            ingredients = new ArrayList<>();
            for (JsonElement ingredient : json.getAsJsonArray("ingredients")) {
                ingredients.add(ingredientText(ingredient));
            }
        }
        JsonObject result = json.getAsJsonObject("result");
        return new Recipe(json.get("type").getAsString(), pattern, key, ingredients,
                result.get("id").getAsString(), result.has("count") ? result.get("count").getAsInt() : 1,
                json.has("group") ? json.get("group").getAsString() : null,
                json.has("category") ? json.get("category").getAsString() : "misc");
    }

    private static String ingredientText(JsonElement ingredient) {
        if (ingredient.isJsonArray()) {
            List<String> parts = new ArrayList<>();
            ingredient.getAsJsonArray().forEach(part -> parts.add(part.getAsString()));
            return String.join("|", parts);
        }
        return ingredient.getAsString();
    }

    /** Recipe ID → items whose pickup unlocks it, from the generated recipe advancements. */
    static Map<String, Set<String>> generatedUnlocks() {
        Map<String, Set<String>> unlocks = new HashMap<>();
        readJsonTree(GENERATED.resolve("advancement")).values().forEach(json -> {
            if (!json.has("rewards") || !json.getAsJsonObject("rewards").has("recipes")) {
                return;
            }
            Set<String> items = new TreeSet<>();
            for (Map.Entry<String, JsonElement> criterion : json.getAsJsonObject("criteria").entrySet()) {
                JsonObject value = criterion.getValue().getAsJsonObject();
                if (!value.get("trigger").getAsString().equals("minecraft:inventory_changed")) {
                    continue;
                }
                for (JsonElement predicate : value.getAsJsonObject("conditions").getAsJsonArray("items")) {
                    JsonElement predicateItems = predicate.getAsJsonObject().get("items");
                    if (predicateItems.isJsonArray()) {
                        predicateItems.getAsJsonArray().forEach(item -> items.add(item.getAsString()));
                    } else {
                        items.add(predicateItems.getAsString());
                    }
                }
            }
            for (JsonElement recipe : json.getAsJsonObject("rewards").getAsJsonArray("recipes")) {
                unlocks.put(recipe.getAsString(), items);
            }
        });
        return unlocks;
    }

    // --- vanilla data ---

    /** The Minecraft jar holding vanilla data, located through a vanilla class on the test classpath. */
    private static Path minecraftJar() {
        try {
            URI location = Class.forName("net.minecraft.world.item.crafting.Recipe", false, RecipeContractTest.class.getClassLoader())
                    .getProtectionDomain().getCodeSource().getLocation().toURI();
            return Path.of(location);
        } catch (ReflectiveOperationException | java.net.URISyntaxException e) {
            throw new IllegalStateException("Minecraft is not on the test classpath", e);
        }
    }

    private static Set<String> expandTag(String tag, Map<String, JsonObject> rawTags, Map<String, Set<String>> expanded, Set<String> visiting) {
        if (expanded.containsKey(tag)) {
            return expanded.get(tag);
        }
        Set<String> items = new HashSet<>();
        JsonObject json = rawTags.get(tag.substring("minecraft:".length()));
        if (json != null && visiting.add(tag)) {
            for (JsonElement value : json.getAsJsonArray("values")) {
                String entry = value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString();
                if (entry.startsWith("#")) {
                    items.addAll(expandTag(entry.substring(1), rawTags, expanded, visiting));
                } else {
                    items.add(entry);
                }
            }
        }
        expanded.put(tag, items);
        return items;
    }

    /** Reads every JSON file below a folder, keyed by its path without the extension (e.g. {@code blocks/campfire}). */
    private static Map<String, JsonObject> readJsonTree(Path root) {
        Map<String, JsonObject> files = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : (Iterable<Path>) paths.filter(p -> p.toString().endsWith(".json"))::iterator) {
                String name = root.relativize(path).toString().replace('\\', '/');
                files.put(name.substring(0, name.length() - ".json".length()),
                        JsonParser.parseString(Files.readString(path)).getAsJsonObject());
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return files;
    }

    // --- grid overlap (R10) ---

    /**
     * A crafting recipe as the items each slot accepts. Shaped recipes keep their rows (and may be
     * mirrored, as in vanilla); shapeless recipes are a multiset of slots.
     */
    record Grid(String name, boolean shaped, int width, List<Set<String>> slots) {

        static Grid of(JsonObject json, Map<String, Set<String>> tags) {
            String type = json.get("type").getAsString();
            switch (type) {
                case "minecraft:crafting_shaped" -> {
                    JsonArray rows = json.getAsJsonArray("pattern");
                    JsonObject key = json.getAsJsonObject("key");
                    int width = rows.get(0).getAsString().length();
                    List<Set<String>> slots = new ArrayList<>();
                    for (JsonElement row : rows) {
                        for (char symbol : row.getAsString().toCharArray()) {
                            slots.add(symbol == ' ' ? Set.of() : accepted(key.get(String.valueOf(symbol)), tags));
                        }
                    }
                    return new Grid(null, true, width, slots);
                }
                case "minecraft:crafting_shapeless" -> {
                    List<Set<String>> slots = new ArrayList<>();
                    json.getAsJsonArray("ingredients").forEach(ingredient -> slots.add(accepted(ingredient, tags)));
                    return new Grid(null, false, 0, slots);
                }
                case "minecraft:crafting_transmute" -> {
                    return new Grid(null, false, 0, List.of(accepted(json.get("input"), tags), accepted(json.get("material"), tags)));
                }
                default -> {
                    return null; // smelting, smithing, stonecutting and special crafting recipes don't use plain grid ingredients
                }
            }
        }

        Grid named(String newName) {
            return new Grid(newName, this.shaped, this.width, this.slots);
        }

        private static Set<String> accepted(JsonElement ingredient, Map<String, Set<String>> tags) {
            Set<String> items = new HashSet<>();
            List<JsonElement> parts = new ArrayList<>();
            if (ingredient.isJsonArray()) {
                ingredient.getAsJsonArray().forEach(parts::add);
            } else {
                parts.add(ingredient);
            }
            for (JsonElement part : parts) {
                String text = part.getAsString();
                if (text.startsWith("#")) {
                    items.addAll(tags.getOrDefault(text.substring(1), Set.of()));
                } else {
                    items.add(text);
                }
            }
            return items;
        }

        boolean overlaps(Grid other) {
            if (this.shaped && other.shaped) {
                return this.width == other.width && this.slots.size() == other.slots.size()
                        && (slotsMatch(this.slots, other.slots) || slotsMatch(this.slots, other.mirrored()));
            }
            // any shapeless side: some arrangement of the filled slots satisfies both
            List<Set<String>> mine = this.filled();
            List<Set<String>> theirs = other.filled();
            return mine.size() == theirs.size() && assignable(mine, theirs, 0, new boolean[theirs.size()]);
        }

        private List<Set<String>> mirrored() {
            List<Set<String>> mirrored = new ArrayList<>();
            for (int row = 0; row < this.slots.size() / this.width; row++) {
                for (int col = this.width - 1; col >= 0; col--) {
                    mirrored.add(this.slots.get(row * this.width + col));
                }
            }
            return mirrored;
        }

        private List<Set<String>> filled() {
            return this.slots.stream().filter(slot -> !slot.isEmpty()).toList();
        }

        private static boolean slotsMatch(List<Set<String>> a, List<Set<String>> b) {
            for (int i = 0; i < a.size(); i++) {
                if (a.get(i).isEmpty() != b.get(i).isEmpty()) {
                    return false;
                }
                if (!a.get(i).isEmpty() && a.get(i).stream().noneMatch(b.get(i)::contains)) {
                    return false;
                }
            }
            return true;
        }

        private static boolean assignable(List<Set<String>> mine, List<Set<String>> theirs, int index, boolean[] used) {
            if (index == mine.size()) {
                return true;
            }
            for (int j = 0; j < theirs.size(); j++) {
                if (!used[j] && mine.get(index).stream().anyMatch(theirs.get(j)::contains)) {
                    used[j] = true;
                    if (assignable(mine, theirs, index + 1, used)) {
                        return true;
                    }
                    used[j] = false;
                }
            }
            return false;
        }
    }

    // --- helpers ---

    private static Map<String, String> orderedMap(String... keysAndValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        if (keysAndValues.length % 2 != 0) {
            fail("orderedMap needs key/value pairs");
        }
        return map;
    }
}
