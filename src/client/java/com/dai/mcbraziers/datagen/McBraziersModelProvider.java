package com.dai.mcbraziers.datagen;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierPlacement;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.client.FireTint;
import com.dai.mcbraziers.item.ModItems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.data.models.blockstates.ConditionBuilder;
import net.minecraft.client.data.models.blockstates.MultiPartGenerator;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelTemplate;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.client.data.models.model.TextureSlot;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Blockstates and item models for every brazier, in every placement (docs/plans/art/copper-brazier-art.md).
 *
 * <p>The part models in {@code models/block/brazier/} come from {@code tools/art/brazier-art.mjs}. This
 * provider gives each part its textures and switches the parts on by block state: a side shows unless it
 * connects to a neighbour, and then a bridge carries the basin, rim, ember bed and grate across to it.
 */
public class McBraziersModelProvider extends FabricModelProvider {

    private static final TextureSlot BODY = TextureSlot.create("body");
    private static final TextureSlot EMBERS = TextureSlot.create("embers");
    private static final TextureSlot FIRE = TextureSlot.create("fire");
    private static final TextureSlot CHAIN = TextureSlot.create("chain");
    private static final TextureSlot TEARS = TextureSlot.create("tears");

    /**
     * Parts with slats. A crying brazier draws each in one of three arrangements, which give every slat its
     * own stain from {@code crying_obsidian_brazier_tears}; the game picks one by the block's position, and
     * the same one for every part, since it reseeds each part of a multipart model alike (crying obsidian
     * plan, Rework addendum). The arrangements are written by {@code tools/art/brazier-art.mjs}.
     */
    private static final Set<String> SLATTED_PARTS = Set.of("floor_side_north", "floor_side_east", "floor_side_south",
            "floor_side_west", "wall_core", "wall_side_west", "wall_side_east", "wall_corner_core", "wall_corner_end_west",
            "wall_corner_end_east", "ceiling_cage");
    private static final List<String> CRYING_ARRANGEMENTS = List.of("crying_a", "crying_b", "crying_c");

    /** Blockstate turn for a wall brazier drawn facing north. */
    private static final Map<Direction, VariantMutator> TURNS = Map.of(
            Direction.NORTH, BlockModelGenerators.NOP, Direction.EAST, BlockModelGenerators.Y_ROT_90,
            Direction.SOUTH, BlockModelGenerators.Y_ROT_180, Direction.WEST, BlockModelGenerators.Y_ROT_270);

    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private static final Map<Direction, BooleanProperty> CONNECTED = Map.of(
            Direction.NORTH, BrazierBlock.NORTH_CONNECTED, Direction.EAST, BrazierBlock.EAST_CONNECTED,
            Direction.SOUTH, BrazierBlock.SOUTH_CONNECTED, Direction.WEST, BrazierBlock.WEST_CONNECTED);

    /** Ember bed texture of a lit brazier, by fire. */
    private static final Map<BrazierFire, String> LIT_EMBERS = Map.of(
            BrazierFire.NORMAL, "brazier_embers_lit",
            BrazierFire.SOUL, "brazier_soul_embers_lit",
            BrazierFire.COPPER, "brazier_copper_embers_lit",
            BrazierFire.AMETHYST, "brazier_amethyst_embers_lit");

    /**
     * Flame texture by fire: the campfire the brazier was made from burns inside it. Vanilla has no green
     * or purple campfire flame: copper fire uses the soul campfire's, tinted in game (FireTint), and
     * amethyst fire a purple copy of it that the game recolours while loading (RecoloredSprite,
     * {@code assets/minecraft/atlases/blocks.json}).
     */
    private static final Map<BrazierFire, Identifier> FLAMES = Map.of(
            BrazierFire.NORMAL, Identifier.withDefaultNamespace("block/campfire_fire"),
            BrazierFire.SOUL, Identifier.withDefaultNamespace("block/soul_campfire_fire"),
            BrazierFire.COPPER, Identifier.withDefaultNamespace("block/soul_campfire_fire"),
            BrazierFire.AMETHYST, Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, "block/amethyst_campfire_fire"));

    private static final String DEAD_EMBERS = "brazier_embers";

    private final Set<Identifier> written = new HashSet<>();

    public McBraziersModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        for (BrazierVariant variant : BrazierVariant.ALL) {
            generateBrazier(generators, variant);
        }
        generateModCampfire(generators, ModBlocks.COPPER_CAMPFIRE, ModItems.COPPER_CAMPFIRE, "copper_campfire", BrazierFire.COPPER);
        generateModCampfire(generators, ModBlocks.AMETHYST_CAMPFIRE, ModItems.AMETHYST_CAMPFIRE, "amethyst_campfire", BrazierFire.AMETHYST);
    }

    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        // Brazier item models are written with their blocks, which own the part models they use.
    }

    private void generateBrazier(BlockModelGenerators generators, BrazierVariant variant) {
        Block block = ModBlocks.BRAZIERS.get(variant);
        // Waxing doesn't change the look, so waxed braziers share their stage's texture.
        String texture = variant.agingPrefix().replace("waxed_", "") + variant.tier().id() + "_brazier";
        MultiPartGenerator multipart = MultiPartGenerator.multiPart(block);

        multipart.with(floor(), part(generators, texture, "floor_base", BODY));
        for (Direction side : SIDES) {
            Direction ccw = side.getCounterClockWise();
            Direction cw = side.getClockWise();
            multipart.with(floor().term(CONNECTED.get(side), false), part(generators, texture, "floor_side_" + side.getName(), BODY));
            multipart.with(floor().term(CONNECTED.get(side), true), part(generators, texture, "floor_bridge_" + side.getName(), BODY));
            multipart.with(floor().term(CONNECTED.get(side), true).term(CONNECTED.get(ccw), false),
                    part(generators, texture, "floor_bridge_ccw_" + side.getName(), BODY));
            multipart.with(floor().term(CONNECTED.get(side), true).term(CONNECTED.get(cw), false),
                    part(generators, texture, "floor_bridge_cw_" + side.getName(), BODY));
            multipart.with(floor().term(CONNECTED.get(side), true).term(CONNECTED.get(cw), true),
                    part(generators, texture, "floor_corner_" + side.getName(), BODY));
            multipart.with(floor().term(BrazierBlock.CLOSED, true).term(CONNECTED.get(side), true),
                    part(generators, texture, "floor_grate_bridge_" + side.getName(), BODY));
            multipart.with(floor().term(BrazierBlock.CLOSED, true).term(CONNECTED.get(side), true).term(CONNECTED.get(cw), true),
                    part(generators, texture, "floor_grate_corner_" + side.getName(), BODY));
        }
        multipart.with(floor().term(BrazierBlock.CLOSED, true), part(generators, texture, "floor_grate", BODY));

        addEmberBed(generators, multipart, () -> floor().term(CampfireBlock.LIT, false), DEAD_EMBERS);
        for (BrazierFire fire : BrazierFire.values()) {
            Supplier<ConditionBuilder> lit = () -> floor().term(CampfireBlock.LIT, true).term(BrazierBlock.FIRE, fire);
            addEmberBed(generators, multipart, lit, LIT_EMBERS.get(fire));
            multipart.with(lit.get(), flames(generators, fire, "floor_fire"));
        }

        addWall(generators, multipart, texture);
        addCeiling(generators, multipart, texture, chainTexture(variant));
        generators.blockStateOutput.accept(multipart);

        for (BrazierFire fire : BrazierFire.values()) {
            generators.itemModelOutput.accept(ModItems.brazierItem(variant, fire), itemModel(generators, texture, fire));
        }
    }

    /** An item shows the brazier on the floor, standing alone, lit with its fire. */
    private ItemModel.Unbaked itemModel(BlockModelGenerators generators, String texture, BrazierFire fire) {
        List<ItemModel.Unbaked> parts = new ArrayList<>();
        parts.add(ItemModelUtils.plainModel(partId(texture, "floor_base")));
        for (Direction side : SIDES) {
            String part = "floor_side_" + side.getName();
            // A crying brazier's item shows its first slat arrangement
            parts.add(ItemModelUtils.plainModel(partId(texture, crying(texture, part) ? CRYING_ARRANGEMENTS.getFirst() + "/" + part : part)));
        }
        parts.add(ItemModelUtils.plainModel(partId(LIT_EMBERS.get(fire), "floor_bed")));
        parts.add(flameItemModel(fire, flameId(fire, "floor_fire")));
        return ItemModelUtils.composite(parts.toArray(ItemModel.Unbaked[]::new));
    }

    /** A flame in an item model, tinted for copper and amethyst fire as the block is in the world. */
    private static ItemModel.Unbaked flameItemModel(BrazierFire fire, Identifier flame) {
        int tint = FireTint.of(fire);
        return tint != FireTint.NONE
                ? ItemModelUtils.tintedModel(flame, ItemModelUtils.constantTint(tint))
                : ItemModelUtils.plainModel(flame);
    }

    /**
     * The Copper and Amethyst Campfires: vanilla's campfire logs with their own flame. Vanilla's lit
     * campfire model can't tint its flame, so these use that model with the flame hidden, plus a tintable
     * flame of the same size that draws the soul campfire's flame in the fire's colour.
     */
    private void generateModCampfire(BlockModelGenerators generators, Block block, Item item, String name, BrazierFire fire) {
        Identifier logs = id("block/" + name);
        new ModelTemplate(Optional.of(Identifier.withDefaultNamespace("block/template_campfire")), Optional.empty(),
                TextureSlot.FIRE, TextureSlot.LIT_LOG).create(logs, new TextureMapping()
                .put(TextureSlot.FIRE, new Material(id("block/hidden_campfire_fire")))
                .put(TextureSlot.LIT_LOG, new Material(Identifier.withDefaultNamespace("block/campfire_log_lit"))), generators.modelOutput);
        Identifier flame = id("block/" + name + "_fire");
        template("campfire_fire", FIRE).create(flame, new TextureMapping().put(FIRE, new Material(FLAMES.get(fire))),
                generators.modelOutput);

        MultiPartGenerator multipart = MultiPartGenerator.multiPart(block);
        for (Direction facing : SIDES) {
            // Vanilla's campfire models face south; its blockstates turn them the same way
            VariantMutator turn = TURNS.get(facing.getOpposite());
            Supplier<ConditionBuilder> faced = () -> new ConditionBuilder().term(CampfireBlock.FACING, facing);
            multipart.with(faced.get().term(CampfireBlock.LIT, false),
                    BlockModelGenerators.plainVariant(Identifier.withDefaultNamespace("block/campfire_off")).with(turn));
            multipart.with(faced.get().term(CampfireBlock.LIT, true), BlockModelGenerators.plainVariant(logs).with(turn));
            multipart.with(faced.get().term(CampfireBlock.LIT, true), BlockModelGenerators.plainVariant(flame).with(turn));
        }
        generators.blockStateOutput.accept(multipart);
        generators.itemModelOutput.accept(item,
                ItemModelUtils.composite(ItemModelUtils.plainModel(logs), flameItemModel(fire, flame)));
    }

    /** Condition builders collect terms in place, so each part takes a fresh one from {@code when}. */
    private void addEmberBed(BlockModelGenerators generators, MultiPartGenerator multipart, Supplier<ConditionBuilder> when, String embers) {
        multipart.with(when.get(), part(generators, embers, "floor_bed", EMBERS));
        for (Direction side : SIDES) {
            multipart.with(when.get().term(CONNECTED.get(side), true), part(generators, embers, "floor_bed_bridge_" + side.getName(), EMBERS));
            multipart.with(when.get().term(CONNECTED.get(side), true).term(CONNECTED.get(side.getClockWise()), true),
                    part(generators, embers, "floor_bed_corner_" + side.getName(), EMBERS));
        }
    }

    private MultiVariant flames(BlockModelGenerators generators, BrazierFire fire, String part) {
        Identifier model = flameId(fire, part);
        if (this.written.add(model)) {
            template(part, FIRE).create(model, new TextureMapping().put(FIRE, new Material(FLAMES.get(fire))), generators.modelOutput);
        }
        return BlockModelGenerators.plainVariant(model);
    }

    /** The part model with this texture, written once however many blocks use it. */
    private MultiVariant part(BlockModelGenerators generators, String texture, String part, TextureSlot slot) {
        if (crying(texture, part)) {
            return cryingArrangements(generators, texture, part, new TextureMapping().put(slot, new Material(id("block/" + texture))), slot);
        }
        Identifier model = partId(texture, part);
        if (this.written.add(model)) {
            template(part, slot).create(model, new TextureMapping().put(slot, new Material(id("block/" + texture))), generators.modelOutput);
        }
        return BlockModelGenerators.plainVariant(model);
    }

    /**
     * Wall braziers, drawn facing north and turned for each facing. Their west and east sides (seen facing
     * north) open into the next wall brazier along the same wall. Joined on its front, a wall brazier is an
     * inside corner: the corner parts replace the straight basket, with an arm forward and, on each side, an
     * arm or a closed end.
     */
    private void addWall(BlockModelGenerators generators, MultiPartGenerator multipart, String texture) {
        for (Direction facing : SIDES) {
            VariantMutator turn = TURNS.get(facing);
            BooleanProperty front = CONNECTED.get(facing);
            Supplier<ConditionBuilder> wall = () -> placed(BrazierPlacement.WALL).term(CampfireBlock.FACING, facing);
            Supplier<ConditionBuilder> straight = () -> wall.get().term(front, false);
            Supplier<ConditionBuilder> corner = () -> wall.get().term(front, true);
            multipart.with(straight.get(), part(generators, texture, "wall_core", BODY).with(turn));
            multipart.with(corner.get(), part(generators, texture, "wall_corner_core", BODY).with(turn));
            multipart.with(corner.get(), part(generators, texture, "wall_corner_front", BODY).with(turn));
            for (Direction drawn : List.of(Direction.WEST, Direction.EAST)) {
                BooleanProperty connected = CONNECTED.get(turned(drawn, facing));
                multipart.with(straight.get().term(connected, false), part(generators, texture, "wall_side_" + drawn.getName(), BODY).with(turn));
                multipart.with(straight.get().term(connected, true), part(generators, texture, "wall_bridge_" + drawn.getName(), BODY).with(turn));
                multipart.with(corner.get().term(connected, false), part(generators, texture, "wall_corner_end_" + drawn.getName(), BODY).with(turn));
                multipart.with(corner.get().term(connected, true), part(generators, texture, "wall_corner_arm_" + drawn.getName(), BODY).with(turn));
            }
            addWallEmbers(generators, multipart, () -> wall.get().term(CampfireBlock.LIT, false), DEAD_EMBERS, facing);
            for (BrazierFire fire : BrazierFire.values()) {
                Supplier<ConditionBuilder> lit = () -> wall.get().term(CampfireBlock.LIT, true).term(BrazierBlock.FIRE, fire);
                addWallEmbers(generators, multipart, lit, LIT_EMBERS.get(fire), facing);
                multipart.with(lit.get(), flames(generators, fire, "wall_fire").with(turn));
            }
        }
    }

    private void addWallEmbers(BlockModelGenerators generators, MultiPartGenerator multipart, Supplier<ConditionBuilder> when,
            String embers, Direction facing) {
        VariantMutator turn = TURNS.get(facing);
        BooleanProperty front = CONNECTED.get(facing);
        multipart.with(when.get().term(front, false), part(generators, embers, "wall_bed", EMBERS).with(turn));
        multipart.with(when.get().term(front, true), part(generators, embers, "wall_corner_bed", EMBERS).with(turn));
        multipart.with(when.get().term(front, true), part(generators, embers, "wall_corner_bed_front", EMBERS).with(turn));
        for (Direction drawn : List.of(Direction.WEST, Direction.EAST)) {
            BooleanProperty connected = CONNECTED.get(turned(drawn, facing));
            multipart.with(when.get().term(front, false).term(connected, true),
                    part(generators, embers, "wall_bed_bridge_" + drawn.getName(), EMBERS).with(turn));
            multipart.with(when.get().term(front, true).term(connected, true),
                    part(generators, embers, "wall_corner_bed_arm_" + drawn.getName(), EMBERS).with(turn));
        }
    }

    /** A slatted part of a crying brazier, as a choice of its arrangements, written once each. */
    private MultiVariant cryingArrangements(BlockModelGenerators generators, String texture, String part, TextureMapping textures,
            TextureSlot... slots) {
        textures.put(TEARS, new Material(id("block/" + texture + "_tears")));
        List<Variant> arrangements = new ArrayList<>();
        for (String arrangement : CRYING_ARRANGEMENTS) {
            Identifier model = partId(texture, arrangement + "/" + part);
            if (this.written.add(model)) {
                TextureSlot[] all = Arrays.copyOf(slots, slots.length + 1);
                all[slots.length] = TEARS;
                template(arrangement + "/" + part, all).create(model, textures, generators.modelOutput);
            }
            arrangements.add(BlockModelGenerators.plainModel(model));
        }
        return BlockModelGenerators.variants(arrangements.toArray(Variant[]::new));
    }

    private static boolean crying(String texture, String part) {
        return texture.startsWith(BrazierVariant.CRYING_PREFIX) && SLATTED_PARTS.contains(part);
    }

    /** Hanging braziers never join: one cage, hung by a loop of the chain that matches its metal. */
    private void addCeiling(BlockModelGenerators generators, MultiPartGenerator multipart, String texture, Identifier chain) {
        TextureMapping cageTextures = new TextureMapping().put(BODY, new Material(id("block/" + texture))).put(CHAIN, new Material(chain));
        if (crying(texture, "ceiling_cage")) {
            multipart.with(placed(BrazierPlacement.CEILING), cryingArrangements(generators, texture, "ceiling_cage", cageTextures, BODY, CHAIN));
        } else {
            Identifier cage = partId(texture, "ceiling_cage");
            if (this.written.add(cage)) {
                template("ceiling_cage", BODY, CHAIN).create(cage, cageTextures, generators.modelOutput);
            }
            multipart.with(placed(BrazierPlacement.CEILING), BlockModelGenerators.plainVariant(cage));
        }
        multipart.with(placed(BrazierPlacement.CEILING).term(CampfireBlock.LIT, false), part(generators, DEAD_EMBERS, "ceiling_bed", EMBERS));
        for (BrazierFire fire : BrazierFire.values()) {
            Supplier<ConditionBuilder> lit = () -> placed(BrazierPlacement.CEILING).term(CampfireBlock.LIT, true).term(BrazierBlock.FIRE, fire);
            multipart.with(lit.get(), part(generators, LIT_EMBERS.get(fire), "ceiling_bed", EMBERS));
            multipart.with(lit.get(), flames(generators, fire, "ceiling_fire"));
        }
    }

    /** A side of the north-facing model, after the model is turned to face {@code facing}. */
    private static Direction turned(Direction side, Direction facing) {
        Direction result = side;
        for (Direction step = Direction.NORTH; step != facing; step = step.getClockWise()) {
            result = result.getClockWise();
        }
        return result;
    }

    private static ConditionBuilder floor() {
        return placed(BrazierPlacement.FLOOR);
    }

    private static ConditionBuilder placed(BrazierPlacement placement) {
        return new ConditionBuilder().term(BrazierBlock.PLACEMENT, placement);
    }

    private static ModelTemplate template(String part, TextureSlot... slots) {
        return new ModelTemplate(Optional.of(id("block/brazier/" + part)), Optional.empty(), slots);
    }

    /** Vanilla's chain of the brazier's metal: copper chain in the brazier's aging stage, iron otherwise. */
    private static Identifier chainTexture(BrazierVariant variant) {
        return variant.tier() == BrazierTier.COPPER
                ? Identifier.withDefaultNamespace("block/" + variant.agingPrefix().replace("waxed_", "") + "copper_chain")
                : Identifier.withDefaultNamespace("block/iron_chain");
    }

    private static Identifier partId(String texture, String part) {
        return id("block/" + texture + "/" + part);
    }

    private static Identifier flameId(BrazierFire fire, String part) {
        return id("block/brazier_fire/" + fire.getSerializedName() + "/" + part);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, path);
    }
}
