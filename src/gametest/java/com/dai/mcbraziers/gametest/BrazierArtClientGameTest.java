package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierPlacement;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Art step (docs/plans/art/copper-brazier-art.md). Checks that every state of every brazier has its models
 * and textures, and that food is drawn on each grate. Saves screenshots of the copper aging stages, the
 * fires, food on the grate, fire pits, wall and hanging braziers, every tier in every placement, how the
 * wall mount meets the wall, and the items. Crying obsidian and amethyst fire are checked and photographed
 * with the rest (docs/plans/additions/crying-obsidian-amethyst-fire.md).
 */
public class BrazierArtClientGameTest implements FabricClientGameTest {

    private static final int GROUND = 100;
    private static final List<String> STAGES = List.of("", "exposed_", "weathered_", "oxidized_");

    /**
     * Food layouts from the plan, as {centre x, height, centre z, spacing, scale} in blocks. Food floats
     * 3/16 px above a grate, as vanilla's does above a campfire's 7 px top.
     */
    private static final float[] ON_CAMPFIRE = {0.5f, 7.1875f / 16, 0.5f, 5f / 16, 0.375f};
    /** Floor: four 6 px items fill the quarters of the 12 px grate, whose top is 13 px. */
    private static final float[] ON_FLOOR_GRATE = {0.5f, 13.1875f / 16, 0.5f, 3f / 16, 0.375f};
    /** Wall: 4 px items on the 12 x 8 grate (top 13 px), which the bracket holds in the middle of the block. */
    private static final float[] ON_WALL_GRATE = {0.5f, 13.1875f / 16, 0.5f, 2f / 16, 0.25f};
    /** Hanging: 4 px items on the 10 px grate, whose top is 12 px. */
    private static final float[] ON_HANGING_GRATE = {0.5f, 12.1875f / 16, 0.5f, 2f / 16, 0.25f};

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("time set noon");
            server.runCommand("weather clear");
            // Spectators hold the camera where it is put, with no hand or block outline in view
            server.runCommand("gamemode spectator @a");
            server.runOnServer(minecraft -> buildScenes(minecraft.overworld()));
            // Hide the HUD for the screenshots; set it rather than toggle, since the game keeps it between tests
            context.runOnClient(minecraft -> {
                if (!minecraft.gui.hud.isHidden()) {
                    minecraft.gui.hud.toggle();
                }
            });

            checkEveryStateHasItsArt(context);
            checkFlames(context);
            checkAmethystFlameIsRecoloured(context);
            checkCryingStains(context);

            server.runCommand("tp @a 0.5 " + (GROUND + 1) + " 6 180 28");
            singleplayer.getConnection().waitForChunksRender();
            checkFood(context, floorFoodPos(), ON_FLOOR_GRATE, Direction.NORTH, "the floor grate");
            context.waitTicks(20);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-day").withSize(1280, 720));
            server.runCommand("time set midnight");
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-night").withSize(1280, 720));

            server.runCommand("tp @a 0.5 " + (GROUND + 1.4) + " 4.4 180 33");
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-grate-close-up").withSize(1280, 720));

            server.runCommand("time set noon");
            server.runCommand("tp @a 30 " + (GROUND + 3) + " 5.5 180 32");
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-fire-pit").withSize(1280, 720));

            server.runCommand("tp @a -30 " + (GROUND + 1.5) + " 7 180 -8");
            singleplayer.getConnection().waitForChunksRender();
            checkFood(context, wallFoodPos(), ON_WALL_GRATE, Direction.SOUTH, "the wall grate");
            checkFood(context, hangingFoodPos(), ON_HANGING_GRATE, Direction.NORTH, "the hanging grate");
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-wall-and-hanging").withSize(1280, 720));
            server.runCommand("time set midnight");
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-wall-and-hanging-night").withSize(1280, 720));
            server.runCommand("tp @a -35.5 " + (GROUND + 2.4) + " 2.2 180 12");
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-wall-close-up").withSize(1280, 720));

            // Every tier in every placement, by day and night
            server.runCommand("time set noon");
            server.runCommand("tp @a 64.5 " + (GROUND + 0.9) + " 13 180 0");
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-tiers").withSize(1280, 720));
            server.runCommand("time set midnight");
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-tiers-night").withSize(1280, 720));

            // Crying obsidian close up, by day: the floor brazier in the lineup (block 68, z 3) with the wall
            // and hanging ones behind it, then the crying bars pane (blocks 97-99)
            server.runCommand("time set noon");
            server.runCommand("tp @a 68.5 " + (GROUND + 1.9) + " 6.2 180 22");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-crying-close-up").withSize(1280, 720));
            server.runCommand("tp @a 98.5 " + (GROUND + 1.6) + " 3.6 180 -8");
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-crying-bars-close-up").withSize(1280, 720));

            // The wall mount: along the wall from the side, from below, and from the front corner
            server.runCommand("time set noon");
            // The single wall brazier on the south face of the wall at x = -36 (block -36..-35, z -1..0)
            server.runCommand("tp @a -37.8 " + (GROUND + 1.78) + " -0.5 -90 0");
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-mount-side").withSize(1280, 720));
            server.runCommand("tp @a -37.6 " + (GROUND + 1.55) + " 0.6 -122 -10");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-mount-side-low").withSize(1280, 720));
            server.runCommand("tp @a -35.5 " + (GROUND + 0.6) + " 1.4 180 -30");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-mount-below").withSize(1280, 720));
            // The netherite wall brazier in the tier lineup (block 76..77, z -1..0), from the side
            server.runCommand("tp @a 78.6 " + (GROUND + 1.55) + " 0.6 122 -10");
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-mount-netherite").withSize(1280, 720));

            // Wall corners: the iron inside corner from the room, close up, from above and at night; the copper
            // one, bracketed to the other wall; and the gap around the pillar's outside corner
            server.runCommand("tp @a -55.5 " + (GROUND + 4.2) + " 3.5 -143 13");
            checkWallCorner(context);
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-corner").withSize(1280, 720));
            server.runCommand("tp @a -54.5 " + (GROUND + 2.9) + " 1 -127 16");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-corner-close-up").withSize(1280, 720));
            server.runCommand("tp @a -52.5 " + (GROUND + 5.6) + " 1.3 180 50");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-corner-above").withSize(1280, 720));
            server.runCommand("tp @a -44.5 " + (GROUND + 4.2) + " 3.5 143 13");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-corner-copper").withSize(1280, 720));
            server.runCommand("tp @a -54.5 " + (GROUND + 4) + " 8.5 135 6");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-outside-corner").withSize(1280, 720));
            // The trough round the pillar from straight above, then its south-east corner at basket height
            server.runCommand("tp @a -57.5 " + (GROUND + 3.8) + " 6 0 90");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-outside-corner-above").withSize(1280, 720));
            server.runCommand("tp @a -55.3 " + (GROUND + 1.95) + " 7.7 135 3");
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-outside-corner-front").withSize(1280, 720));
            server.runCommand("time set midnight");
            server.runCommand("tp @a -55.5 " + (GROUND + 4.2) + " 3.5 -143 13");
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-wall-corner-night").withSize(1280, 720));

            // Copper and amethyst fire: a soul campfire, then each mod campfire with its brazier, at night
            server.runCommand("time set midnight");
            server.runCommand("tp @a 14.5 " + (GROUND + 0.6) + " 6.5 180 12");
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(40);
            context.takeScreenshot(TestScreenshotOptions.of("art-copper-and-amethyst-fire").withSize(1280, 720));

            // Items: the hotbar, and the first one in hand
            server.runCommand("time set noon");
            server.runCommand("gamemode creative @a");
            for (int slot = 0; slot < HOTBAR.size(); slot++) {
                server.runCommand("item replace entity @a hotbar." + slot + " with mcbraziers:" + HOTBAR.get(slot));
            }
            context.runOnClient(minecraft -> {
                if (minecraft.gui.hud.isHidden()) {
                    minecraft.gui.hud.toggle();
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("art-copper-items");

            // Bars: a pane of each beside vanilla's iron and copper bars, then the bars in the hotbar and in hand
            server.runCommand("gamemode spectator @a");
            context.runOnClient(minecraft -> {
                if (!minecraft.gui.hud.isHidden()) {
                    minecraft.gui.hud.toggle();
                }
            });
            server.runCommand("tp @a 94.5 " + (GROUND + 0.6) + " 12 180 -4");
            singleplayer.getConnection().waitForChunksRender();
            context.waitTicks(30);
            context.takeScreenshot(TestScreenshotOptions.of("art-bars").withSize(1280, 720));
            server.runCommand("gamemode creative @a");
            List<String> bars = List.of("mcbraziers:diamond_bars", "mcbraziers:obsidian_bars", "mcbraziers:crying_obsidian_bars",
                    "mcbraziers:prismarine_bars", "mcbraziers:netherite_bars", "minecraft:iron_bars", "minecraft:copper_bars");
            for (int slot = 0; slot < bars.size(); slot++) {
                server.runCommand("item replace entity @a hotbar." + slot + " with " + bars.get(slot));
            }
            context.runOnClient(minecraft -> {
                if (minecraft.gui.hud.isHidden()) {
                    minecraft.gui.hud.toggle();
                }
            });
            context.waitTicks(20);
            context.takeScreenshot("art-bars-items");
        }
    }

    private static final List<String> HOTBAR = List.of("copper_brazier", "soul_copper_brazier", "copper_fire_copper_brazier",
            "exposed_copper_brazier", "weathered_soul_copper_brazier", "oxidized_copper_fire_copper_brazier", "waxed_weathered_copper_brazier",
            "crying_amethyst_fire_obsidian_brazier", "amethyst_campfire");

    /**
     * Every state of every brazier draws its models and textures, and reaches the edge of its block
     * only where it joins a neighbour. Waterlogging and the signal fire don't change the model, and facing
     * changes it only on a wall, so those copies are skipped.
     */
    private static void checkEveryStateHasItsArt(ClientGameTestContext context) {
        List<String> missing = context.computeOnClient(minecraft -> {
            List<String> problems = new ArrayList<>();
            var models = minecraft.getModelManager().getBlockStateModelSet();
            for (BrazierVariant variant : BrazierVariant.ALL) {
                Block block = ModBlocks.BRAZIERS.get(variant);
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    if (state.getValue(CampfireBlock.WATERLOGGED) || state.getValue(CampfireBlock.SIGNAL_FIRE)
                            || (state.getValue(BrazierBlock.PLACEMENT) != BrazierPlacement.WALL && state.getValue(CampfireBlock.FACING) != Direction.NORTH)) {
                        continue;
                    }
                    BlockStateModel model = models.get(state);
                    if (model == models.missingModel() || usesMissingTexture(model)) {
                        problems.add(state + " draws a missing model or texture");
                    }
                    String edges = edgeProblem(state, model);
                    if (edges != null) {
                        problems.add(state + " " + edges);
                    }
                }
            }
            // The bars and the mod's campfires: every state draws its models and textures
            for (Block block : List.of(ModBlocks.DIAMOND_BARS, ModBlocks.OBSIDIAN_BARS, ModBlocks.CRYING_OBSIDIAN_BARS, ModBlocks.PRISMARINE_BARS,
                    ModBlocks.NETHERITE_BARS, ModBlocks.COPPER_CAMPFIRE, ModBlocks.AMETHYST_CAMPFIRE)) {
                for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                    BlockStateModel model = models.get(state);
                    if (model == models.missingModel() || usesMissingTexture(model)) {
                        problems.add(state + " draws a missing model or texture");
                    }
                }
            }
            return problems;
        });
        check(missing.isEmpty(), missing.size() + " block states are wrong, e.g. " + missing.stream().limit(3).toList());
    }

    /**
     * A brazier's cage and its ember bed each reach the edge of the block exactly on the sides that join a
     * neighbour: any side on the floor, the sides along the wall and the front for a wall brazier (whose back
     * plate sits on the wall), and none for a hanging brazier. Checking them apart means a missing bridge or
     * arm shows even where the other still reaches the edge. Null when that holds.
     */
    private static String edgeProblem(BlockState state, BlockStateModel model) {
        Set<Direction> cage = EnumSet.noneOf(Direction.class);
        Set<Direction> bed = EnumSet.noneOf(Direction.class);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0), parts);
        for (BlockStateModelPart part : parts) {
            for (Direction side : new Direction[] {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                for (BakedQuad quad : part.getQuads(side)) {
                    Identifier sprite = quad.materialInfo().sprite().contents().name();
                    if (FLAMES.contains(sprite)) {
                        continue;
                    }
                    Set<Direction> reached = sprite.getPath().contains("embers") ? bed : cage;
                    for (Vector3fc corner : List.of(quad.position0(), quad.position1(), quad.position2(), quad.position3())) {
                        if (corner.x() < 1e-4f) reached.add(Direction.WEST);
                        if (corner.x() > 1 - 1e-4f) reached.add(Direction.EAST);
                        if (corner.z() < 1e-4f) reached.add(Direction.NORTH);
                        if (corner.z() > 1 - 1e-4f) reached.add(Direction.SOUTH);
                    }
                }
            }
        }
        Set<Direction> joinable = switch (state.getValue(BrazierBlock.PLACEMENT)) {
            case FLOOR -> EnumSet.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
            case WALL -> {
                // Joined on its front, a wall brazier turns an inside corner into the run in front of it; joined
                // on its back, an outside corner, with no bracket plate reaching the back edge
                Direction facing = state.getValue(CampfireBlock.FACING);
                if (state.getValue(CONNECTED.get(facing)) || !state.getValue(CONNECTED.get(facing.getOpposite()))) {
                    cage.remove(facing.getOpposite());
                }
                yield EnumSet.of(facing.getClockWise(), facing.getCounterClockWise(), facing, facing.getOpposite());
            }
            case CEILING -> EnumSet.noneOf(Direction.class);
        };
        Set<Direction> joined = EnumSet.noneOf(Direction.class);
        for (Direction side : joinable) {
            if (state.getValue(CONNECTED.get(side))) {
                joined.add(side);
            }
        }
        // Joined on both front and back, a wall brazier draws as the inside corner, which has no back arm
        if (state.getValue(BrazierBlock.PLACEMENT) == BrazierPlacement.WALL && joined.contains(state.getValue(CampfireBlock.FACING))) {
            joined.remove(state.getValue(CampfireBlock.FACING).getOpposite());
        }
        if (!cage.equals(joined)) {
            return "has its cage reach the block edge on " + cage + " but joins on " + joined;
        }
        return bed.equals(joined) ? null : "has its ember bed reach the block edge on " + bed + " but joins on " + joined;
    }

    private static final Map<Direction, BooleanProperty> CONNECTED = Map.of(
            Direction.NORTH, BrazierBlock.NORTH_CONNECTED, Direction.EAST, BrazierBlock.EAST_CONNECTED,
            Direction.SOUTH, BrazierBlock.SOUTH_CONNECTED, Direction.WEST, BrazierBlock.WEST_CONNECTED);

    /** Copper fire's green, chosen by the owner on 2026-10-18 (PRD License addendum). */
    private static final int COPPER_FIRE_GREEN = 0xFFBFFF73;
    private static final Identifier SOUL_FLAME = Identifier.withDefaultNamespace("block/soul_campfire_fire");
    private static final Identifier FLAME = Identifier.withDefaultNamespace("block/campfire_fire");
    /** Amethyst fire's flame: vanilla's soul campfire flame, recoloured purple while the game loads. */
    private static final Identifier AMETHYST_FLAME = Identifier.fromNamespaceAndPath("mcbraziers", "block/amethyst_campfire_fire");
    private static final Set<Identifier> FLAMES = Set.of(SOUL_FLAME, FLAME, AMETHYST_FLAME);

    /**
     * Copper fire draws vanilla's soul campfire flame tinted green, on the Copper Campfire and on copper-fire
     * braziers in every placement. Amethyst fire draws its recoloured flame untinted (PRD License addendum,
     * recolouring at load). Soul and normal fire draw their own flames untinted, though soul fire shares the
     * texture. Checked on the baked flame quads and the tint the game resolves for them.
     */
    private static void checkFlames(ClientGameTestContext context) {
        List<String> problems = context.computeOnClient(minecraft -> {
            List<String> found = new ArrayList<>();
            BlockState copperCampfire = ModBlocks.COPPER_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
            checkFlame(minecraft, copperCampfire, SOUL_FLAME, COPPER_FIRE_GREEN, found);
            BlockState amethystCampfire = ModBlocks.AMETHYST_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true);
            checkFlame(minecraft, amethystCampfire, AMETHYST_FLAME, -1, found);
            Block iron = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.IRON, ""));
            for (BrazierPlacement placement : BrazierPlacement.values()) {
                BlockState lit = iron.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(BrazierBlock.PLACEMENT, placement);
                checkFlame(minecraft, lit.setValue(BrazierBlock.FIRE, BrazierFire.COPPER), SOUL_FLAME, COPPER_FIRE_GREEN, found);
                checkFlame(minecraft, lit.setValue(BrazierBlock.FIRE, BrazierFire.AMETHYST), AMETHYST_FLAME, -1, found);
                checkFlame(minecraft, lit.setValue(BrazierBlock.FIRE, BrazierFire.SOUL), SOUL_FLAME, -1, found);
                checkFlame(minecraft, lit.setValue(BrazierBlock.FIRE, BrazierFire.NORMAL), FLAME, -1, found);
            }
            return found;
        });
        check(problems.isEmpty(), "fire flames are wrong: " + problems);
    }

    /**
     * The amethyst flame is vanilla's soul campfire flame recoloured purple: the same animation frames and
     * shape, every visible pixel purple (PRD License addendum, recolouring at load).
     */
    private static void checkAmethystFlameIsRecoloured(ClientGameTestContext context) {
        List<String> problems = context.computeOnClient(minecraft -> {
            SpriteContents amethyst = flameSprite(minecraft, ModBlocks.AMETHYST_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            SpriteContents soul = flameSprite(minecraft, Blocks.SOUL_CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
            if (amethyst == null || !amethyst.name().equals(AMETHYST_FLAME) || soul == null) {
                return List.of("the Amethyst Campfire draws " + (amethyst == null ? "no flame" : amethyst.name()) + ", expected " + AMETHYST_FLAME);
            }
            return SpritePixels.purpleRecolourProblems(amethyst, soul);
        });
        check(problems.isEmpty(), "the amethyst flame is wrong: " + problems);
    }

    /** The sprite of the first flame quad the state draws, or null. */
    private static SpriteContents flameSprite(Minecraft minecraft, BlockState state) {
        for (BakedQuad quad : quads(minecraft, state, 0)) {
            SpriteContents contents = quad.materialInfo().sprite().contents();
            if (FLAMES.contains(contents.name())) {
                return contents;
            }
        }
        return null;
    }

    /** Every quad of the state's model, with the model's random choices made from {@code seed}. */
    private static List<BakedQuad> quads(Minecraft minecraft, BlockState state, long seed) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        minecraft.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(seed), parts);
        List<BakedQuad> quads = new ArrayList<>();
        for (BlockStateModelPart part : parts) {
            for (Direction side : new Direction[] {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                quads.addAll(part.getQuads(side));
            }
        }
        return quads;
    }

    /** The state's flame quads all draw {@code flame} with tint index 0, and the game resolves that tint to {@code tint}. */
    private static void checkFlame(Minecraft minecraft, BlockState state, Identifier flame, int tint, List<String> found) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        minecraft.getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(0), parts);
        int flames = 0;
        for (BlockStateModelPart part : parts) {
            for (Direction side : new Direction[] {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                for (BakedQuad quad : part.getQuads(side)) {
                    Identifier sprite = quad.materialInfo().sprite().contents().name();
                    if (FLAMES.contains(sprite)) {
                        flames++;
                        if (!sprite.equals(flame) || quad.materialInfo().tintIndex() != 0) {
                            found.add(state + " draws " + sprite + " with tint index " + quad.materialInfo().tintIndex());
                        }
                    }
                }
            }
        }
        if (flames == 0) {
            found.add(state + " draws no flame");
        }
        var source = minecraft.getBlockColors().getTintSource(state, 0);
        int resolved = source == null ? -1 : source.color(state);
        if (resolved != tint) {
            found.add(state + " flame tint " + Integer.toHexString(resolved) + ", expected " + Integer.toHexString(tint));
        }
    }

    private static final String CRYING_BRAZIER = "mcbraziers:block/crying_obsidian_brazier";
    private static final String CRYING_TEARS = "mcbraziers:block/crying_obsidian_brazier_tears";
    /** Seeds standing in for block positions: the game seeds a block's model choices from its position. */
    private static final int SEEDS = 32;

    /**
     * Crying obsidian's stains are still, and vary (PRD Content erratum, 2026-10-18):
     * <ul>
     *   <li>the crying brazier draws its own still textures in every placement, plain obsidian none of them;
     *   <li>each slat of a side shows a different stain, so the four slats of a floor brazier's side differ;
     *   <li>positions pick different slat arrangements;
     *   <li>crying bars draw still stains in several versions, positions picking different ones, and one block
     *       showing one version throughout.
     * </ul>
     */
    private static void checkCryingStains(ClientGameTestContext context) {
        List<String> problems = context.computeOnClient(minecraft -> {
            Set<String> found = new LinkedHashSet<>();
            Block crying = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.OBSIDIAN, "crying_"));
            Block obsidian = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.OBSIDIAN, ""));
            for (BrazierPlacement placement : BrazierPlacement.values()) {
                BlockState cryingState = crying.defaultBlockState().setValue(BrazierBlock.PLACEMENT, placement);
                Set<String> sprites = spriteNames(quads(minecraft, cryingState, 0));
                for (String expected : List.of(CRYING_BRAZIER, CRYING_TEARS)) {
                    if (!sprites.contains(expected)) {
                        found.add(cryingState + " never draws " + expected);
                    }
                }
                Set<String> plain = spriteNames(quads(minecraft, obsidian.defaultBlockState().setValue(BrazierBlock.PLACEMENT, placement), 0));
                plain.removeIf(name -> !name.contains("crying"));
                if (!plain.isEmpty()) {
                    found.add("the plain obsidian brazier on the " + placement + " draws " + plain);
                }
            }

            // Floor slats: per side, the strip of the tears texture each slat's broad faces read
            BlockState floor = crying.defaultBlockState();
            Set<Map<Direction, Set<Integer>>> arrangements = new HashSet<>();
            for (int seed = 0; seed < SEEDS; seed++) {
                Map<Direction, Set<Integer>> strips = slatStrips(quads(minecraft, floor, seed));
                arrangements.add(strips);
                for (Direction side : SIDES) {
                    if (strips.getOrDefault(side, Set.of()).size() != 4) {
                        found.add("the " + side + " side's 4 slats show " + strips.getOrDefault(side, Set.of()).size() + " different stains (seed " + seed + ")");
                    }
                }
            }
            if (arrangements.size() < 2) {
                found.add("every position draws the same slat arrangement");
            }

            // Bars: one stain version per block, versions varying between blocks
            BlockState pane = ModBlocks.CRYING_OBSIDIAN_BARS.defaultBlockState()
                    .setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
            Set<String> versions = new HashSet<>();
            for (int seed = 0; seed < SEEDS; seed++) {
                Set<String> sprites = spriteNames(quads(minecraft, pane, seed));
                if (sprites.size() != 1) {
                    found.add("one crying bars block draws " + sprites + " (seed " + seed + ")");
                }
                versions.addAll(sprites);
            }
            if (versions.size() < 2) {
                found.add("every crying bars block draws the same stains: " + versions);
            }

            // Every crying sprite is still: the stains don't move, the tear particles do
            List<BakedQuad> all = new ArrayList<>(quads(minecraft, floor, 0));
            for (int seed = 0; seed < SEEDS; seed++) {
                all.addAll(quads(minecraft, pane, seed));
            }
            for (BakedQuad quad : all) {
                SpriteContents contents = quad.materialInfo().sprite().contents();
                if (contents.name().getPath().contains("crying") && contents.isAnimated()) {
                    found.add(contents.name() + " is animated");
                }
            }
            return List.copyOf(found);
        });
        check(problems.isEmpty(), "crying obsidian art is wrong: " + problems);
    }

    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    /**
     * For each side of the block, the tears-texture strips (2 px wide, numbered from the left) that its slats'
     * broad faces show. A face's side is the side of the block its centre is nearest.
     */
    private static Map<Direction, Set<Integer>> slatStrips(List<BakedQuad> quads) {
        Map<Direction, Set<Integer>> strips = new HashMap<>();
        for (BakedQuad quad : quads) {
            var sprite = quad.materialInfo().sprite();
            if (!sprite.contents().name().toString().equals(CRYING_TEARS) || quad.direction().getAxis() == Direction.Axis.Y) {
                continue;
            }
            float u0 = Float.MAX_VALUE;
            float u1 = -Float.MAX_VALUE;
            Vector3f centre = new Vector3f();
            for (int vertex = 0; vertex < 4; vertex++) {
                float u = (UVPair.unpackU(quad.packedUV(vertex)) - sprite.getU0()) / (sprite.getU1() - sprite.getU0()) * 16;
                u0 = Math.min(u0, u);
                u1 = Math.max(u1, u);
                centre.add(quad.position(vertex));
            }
            if (u1 - u0 < 1.5f) {
                continue; // a slat's thin edge
            }
            centre.mul(0.25f).sub(0.5f, 0.5f, 0.5f);
            Direction side = Math.abs(centre.z()) > Math.abs(centre.x())
                    ? (centre.z() < 0 ? Direction.NORTH : Direction.SOUTH)
                    : (centre.x() < 0 ? Direction.WEST : Direction.EAST);
            strips.computeIfAbsent(side, s -> new HashSet<>()).add((int) (u0 / 2 + 0.01f));
        }
        return strips;
    }

    private static Set<String> spriteNames(List<BakedQuad> quads) {
        Set<String> names = new HashSet<>();
        quads.forEach(quad -> names.add(quad.materialInfo().sprite().contents().name().toString()));
        return names;
    }

    private static boolean usesMissingTexture(BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0), parts);
        for (BlockStateModelPart part : parts) {
            for (Direction side : new Direction[] {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                for (BakedQuad quad : part.getQuads(side)) {
                    if (quad.materialInfo().sprite().contents().name().equals(MissingTextureAtlasSprite.getLocation())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * The renderer draws the food on a brazier's grate where the plan puts it, checked on the pose each item
     * is actually submitted with. The same food sits in the same slots of a vanilla campfire facing north.
     * Each item's pose is (layout) x (the item's own display transform). Taking the campfire's layout off
     * its pose leaves the item's own part, which must match the brazier's pose under the brazier's layout.
     */
    /**
     * The corner scene came out as built: the iron corner piece joins the run along the north wall and the one
     * coming along the east wall, with its food drawn on the junction's grate as on a straight wall brazier,
     * and the outside corner pieces join the braziers on the pillar's faces into one trough round its edges.
     */
    private static void checkWallCorner(ClientGameTestContext context) {
        BlockPos onSouth = new BlockPos(-58, GROUND + 3, 6);
        // The client may still be loading the scene after the teleport
        context.waitFor(minecraft -> minecraft.level != null && minecraft.level.getBlockState(ironCornerPos()).getBlock() instanceof BrazierBlock);
        String problem = context.computeOnClient(minecraft -> {
            BlockState corner = minecraft.level.getBlockState(ironCornerPos());
            if (!corner.getValue(BrazierBlock.SOUTH_CONNECTED) || !corner.getValue(BrazierBlock.WEST_CONNECTED)
                    || corner.getValue(BrazierBlock.EAST_CONNECTED)) {
                return "the iron corner piece is " + corner;
            }
            BlockState south = minecraft.level.getBlockState(onSouth);
            if (!south.getValue(BrazierBlock.EAST_CONNECTED) || !south.getValue(BrazierBlock.WEST_CONNECTED)) {
                return "the brazier on the pillar's south face is " + south;
            }
            for (BlockPos outside : PILLAR_CORNERS) {
                BlockState state = minecraft.level.getBlockState(outside);
                Direction toPillarRun = outside.getX() > onSouth.getX() ? Direction.WEST : Direction.EAST;
                if (!state.getValue(BrazierBlock.NORTH_CONNECTED) || !state.getValue(CONNECTED.get(toPillarRun))
                        || !minecraft.level.getBlockState(outside.north()).getValue(BrazierBlock.SOUTH_CONNECTED)) {
                    return "the outside corner piece at " + outside + " is " + state;
                }
            }
            return null;
        });
        check(problem == null, problem);
        checkFood(context, ironCornerPos(), ON_WALL_GRATE, Direction.SOUTH, "the inside corner's grate");
    }

    private static void checkFood(ClientGameTestContext context, BlockPos brazierPos, float[] layout, Direction facing, String where) {
        context.waitFor(minecraft -> minecraft.level != null
                && minecraft.level.getBlockEntity(brazierPos) instanceof CampfireBlockEntity brazier && !brazier.getItems().get(1).isEmpty()
                && minecraft.level.getBlockEntity(campfirePos()) instanceof CampfireBlockEntity campfire && !campfire.getItems().get(1).isEmpty());
        List<Matrix4f> onBrazier = context.computeOnClient(minecraft -> submittedFood(minecraft, brazierPos));
        List<Matrix4f> onCampfire = context.computeOnClient(minecraft -> submittedFood(minecraft, campfirePos()));
        check(onBrazier.size() == 2 && onCampfire.size() == 2,
                "expected 2 food items drawn on each, got " + onBrazier.size() + " on " + where + " and " + onCampfire.size() + " on the campfire");
        for (int slot = 0; slot < 2; slot++) {
            Matrix4f item = layoutPose(ON_CAMPFIRE, Direction.NORTH, slot).invert().mul(onCampfire.get(slot));
            Matrix4f expected = layoutPose(layout, facing, slot).mul(item);
            check(expected.equals(onBrazier.get(slot), 1e-4f), "food in slot " + slot + " on " + where + " drawn at "
                    + onBrazier.get(slot).getTranslation(new Vector3f()) + ", expected "
                    + expected.getTranslation(new Vector3f()));
        }
    }

    /** Vanilla's campfire layout for one slot (CampfireRenderer.submit), with the given numbers. */
    private static Matrix4f layoutPose(float[] layout, Direction facing, int slot) {
        Direction direction = Direction.from2DDataValue((slot + facing.get2DDataValue()) % 4);
        return new Matrix4f().translate(layout[0], layout[1], layout[2])
                .rotateY((float) Math.toRadians(-direction.toYRot()))
                .rotateX((float) Math.toRadians(90))
                .translate(-layout[3], -layout[3], 0)
                .scale(layout[4]);
    }

    /** The pose of each food item the renderer submits for the block entity at pos, in block coordinates. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<Matrix4f> submittedFood(Minecraft minecraft, BlockPos pos) {
        BlockEntity campfire = minecraft.level.getBlockEntity(pos);
        BlockEntityRenderer renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(campfire);
        BlockEntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(campfire, state, 0, Vec3.ZERO, null);

        List<Matrix4f> poses = new ArrayList<>();
        InvocationHandler recorder = (proxy, method, args) -> {
            if (method.getName().equals("submitItem")) {
                poses.add(new Matrix4f(((PoseStack) args[0]).last().pose()));
                return null;
            }
            Class<?> type = method.getReturnType();
            return type.isInstance(proxy) ? proxy : type == boolean.class ? false : type.isPrimitive() && type != void.class ? 0 : null;
        };
        SubmitNodeCollector collector = (SubmitNodeCollector) Proxy.newProxyInstance(
                SubmitNodeCollector.class.getClassLoader(), new Class<?>[] {SubmitNodeCollector.class}, recorder);
        renderer.submit(state, new PoseStack(), collector, new CameraRenderState());
        return poses;
    }

    private static BlockPos floorFoodPos() {
        return new BlockPos(0, GROUND + 1, 2);
    }

    private static BlockPos campfirePos() {
        return new BlockPos(3, GROUND + 1, 2);
    }

    private static BlockPos wallFoodPos() {
        return new BlockPos(-36, GROUND + 3, -1);
    }

    private static BlockPos hangingFoodPos() {
        return new BlockPos(-31, GROUND + 5, 1);
    }

    private static void buildScenes(ServerLevel level) {
        fill(level, -8, GROUND, -3, 8, GROUND, 9, Blocks.STONE);
        // Back row: the four aging stages, open and lit
        for (int i = 0; i < STAGES.size(); i++) {
            place(level, new BlockPos(-6 + 4 * i, GROUND + 1, -1), brazier(STAGES.get(i), BrazierFire.NORMAL, true, false));
        }
        // Front row: closed grates with food, one per fire, an unlit one and a campfire for reference
        place(level, new BlockPos(-6, GROUND + 1, 2), brazier("", BrazierFire.SOUL, true, true));
        place(level, new BlockPos(-3, GROUND + 1, 2), brazier("weathered_", BrazierFire.COPPER, true, true));
        place(level, floorFoodPos(), brazier("", BrazierFire.NORMAL, true, true));
        place(level, campfirePos(), Blocks.CAMPFIRE.defaultBlockState());
        place(level, new BlockPos(6, GROUND + 1, 2), brazier("oxidized_", BrazierFire.NORMAL, false, false));
        addFood(level, new BlockPos(-6, GROUND + 1, 2), new BlockPos(-3, GROUND + 1, 2), floorFoodPos(), campfirePos());

        // Copper and amethyst fire beside soul fire: a soul campfire, then the Copper Campfire and the Amethyst
        // Campfire, each with an open brazier of its fire
        fill(level, 9, GROUND, -3, 20, GROUND, 9, Blocks.STONE);
        place(level, new BlockPos(10, GROUND + 1, 2), Blocks.SOUL_CAMPFIRE.defaultBlockState());
        place(level, new BlockPos(12, GROUND + 1, 2), ModBlocks.COPPER_CAMPFIRE.defaultBlockState());
        place(level, new BlockPos(14, GROUND + 1, 2), brazier("", BrazierFire.COPPER, true, false));
        place(level, new BlockPos(16, GROUND + 1, 2), ModBlocks.AMETHYST_CAMPFIRE.defaultBlockState());
        place(level, new BlockPos(18, GROUND + 1, 2), brazier("", BrazierFire.AMETHYST, true, false));

        // Bars: a 3 x 3 pane of each, vanilla's iron and copper first, joined by the blocks' own rules
        fill(level, 78, GROUND, -3, 110, GROUND, 12, Blocks.STONE);
        List<Block> barTypes = List.of(Blocks.IRON_BARS, Blocks.COPPER_BARS.weathering().unaffected(), ModBlocks.DIAMOND_BARS,
                ModBlocks.OBSIDIAN_BARS, ModBlocks.CRYING_OBSIDIAN_BARS, ModBlocks.PRISMARINE_BARS, ModBlocks.NETHERITE_BARS);
        List<BlockPos> panes = new ArrayList<>();
        for (int i = 0; i < barTypes.size(); i++) {
            for (int dx = 0; dx < 3; dx++) for (int dy = 1; dy <= 3; dy++) {
                BlockPos pos = new BlockPos(81 + 4 * i + dx, GROUND + dy, 0);
                place(level, pos, barTypes.get(i).defaultBlockState());
                panes.add(pos);
            }
        }
        panes.forEach((pos) -> connect(level, pos));

        // Fire pit: a closed 3 x 2 pit and an open L beside it, joined by the blocks' own rules
        fill(level, 22, GROUND, -3, 38, GROUND, 9, Blocks.STONE);
        List<BlockPos> pit = new ArrayList<>();
        for (int x = 26; x <= 28; x++) for (int z = 0; z <= 1; z++) pit.add(new BlockPos(x, GROUND + 1, z));
        pit.forEach((pos) -> place(level, pos, brazier("", BrazierFire.NORMAL, true, true)));
        List<BlockPos> ell = List.of(new BlockPos(32, GROUND + 1, 0), new BlockPos(33, GROUND + 1, 0), new BlockPos(33, GROUND + 1, 1),
                new BlockPos(33, GROUND + 1, 2));
        ell.forEach((pos) -> place(level, pos, brazier("exposed_", BrazierFire.SOUL, true, false)));
        pit.forEach((pos) -> connect(level, pos));
        ell.forEach((pos) -> connect(level, pos));

        // Wall and hanging: a wall facing south with braziers on it, and a beam above them to hang from
        fill(level, -40, GROUND, -3, -20, GROUND, 9, Blocks.STONE);
        fill(level, -40, GROUND + 1, -2, -20, GROUND + 8, -2, Blocks.STONE);
        fill(level, -40, GROUND + 7, 1, -20, GROUND + 7, 1, Blocks.STONE);
        BlockState onWall = brazier("", BrazierFire.NORMAL, true, true).setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL)
                .setValue(CampfireBlock.FACING, Direction.SOUTH);
        place(level, wallFoodPos(), onWall);
        List<BlockPos> wallPit = List.of(new BlockPos(-33, GROUND + 3, -1), new BlockPos(-32, GROUND + 3, -1));
        wallPit.forEach((pos) -> place(level, pos, onWall.setValue(BrazierBlock.FIRE, BrazierFire.COPPER)));
        List<BlockPos> wallRun = List.of(new BlockPos(-28, GROUND + 3, -1), new BlockPos(-27, GROUND + 3, -1), new BlockPos(-26, GROUND + 3, -1));
        wallRun.forEach((pos) -> place(level, pos, ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.COPPER, "weathered_"))
                .withPropertiesOf(onWall).setValue(BrazierBlock.FIRE, BrazierFire.SOUL)));
        wallPit.forEach((pos) -> connect(level, pos));
        wallRun.forEach((pos) -> connect(level, pos));

        BlockState hanging = brazier("", BrazierFire.NORMAL, true, true).setValue(BrazierBlock.PLACEMENT, BrazierPlacement.CEILING);
        place(level, new BlockPos(-36, GROUND + 6, 1), Blocks.COPPER_CHAIN.weathering().unaffected().defaultBlockState());
        place(level, new BlockPos(-36, GROUND + 5, 1), hanging);
        place(level, new BlockPos(-31, GROUND + 6, 1), Blocks.COPPER_CHAIN.weathering().unaffected().defaultBlockState());
        place(level, hangingFoodPos(), hanging.setValue(BrazierBlock.FIRE, BrazierFire.COPPER));
        place(level, new BlockPos(-26, GROUND + 6, 1), ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.COPPER, "oxidized_"))
                .withPropertiesOf(hanging).setValue(BrazierBlock.FIRE, BrazierFire.SOUL));
        addFood(level, wallFoodPos(), hangingFoodPos());

        // Every tier, crying obsidian beside obsidian: a floor brazier in front, a wall brazier on the wall and
        // a hanging one from the beam. Every fire shows; crying obsidian burns amethyst fire, the pairing it was
        // asked for with.
        List<BrazierFire> fires = List.of(BrazierFire.NORMAL, BrazierFire.SOUL, BrazierFire.COPPER, BrazierFire.NORMAL,
                BrazierFire.AMETHYST, BrazierFire.SOUL, BrazierFire.COPPER);
        fill(level, 48, GROUND, -3, 79, GROUND, 12, Blocks.STONE);
        fill(level, 48, GROUND + 1, -2, 79, GROUND + 8, -2, Blocks.STONE);
        fill(level, 48, GROUND + 7, 1, 79, GROUND + 7, 1, Blocks.STONE);
        List<BrazierVariant> lineup = BrazierVariant.ALL.stream()
                .filter(variant -> variant.tier() != BrazierTier.COPPER || variant.agingPrefix().isEmpty()).toList();
        for (int i = 0; i < lineup.size(); i++) {
            BrazierTier tier = lineup.get(i).tier();
            BlockState tierBrazier = ModBlocks.BRAZIERS.get(lineup.get(i)).defaultBlockState()
                    .setValue(BrazierBlock.FIRE, fires.get(i)).setValue(CampfireBlock.LIT, true);
            int x = 52 + 4 * i;
            place(level, new BlockPos(x, GROUND + 1, 3), tierBrazier.setValue(BrazierBlock.CLOSED, i % 2 == 1));
            place(level, new BlockPos(x, GROUND + 3, -1), tierBrazier.setValue(BrazierBlock.CLOSED, true)
                    .setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL).setValue(CampfireBlock.FACING, Direction.SOUTH));
            place(level, new BlockPos(x, GROUND + 6, 1), (tier == BrazierTier.COPPER ? Blocks.COPPER_CHAIN.weathering().unaffected() : Blocks.IRON_CHAIN)
                    .defaultBlockState());
            place(level, new BlockPos(x, GROUND + 5, 1), tierBrazier.setValue(BrazierBlock.CLOSED, true)
                    .setValue(BrazierBlock.PLACEMENT, BrazierPlacement.CEILING));
        }

        // Wall corners (owner's rulings, 2026-10-25). Two rooms with walls on the north and one side: iron runs
        // turning the corner with the bracket on the north wall, and copper runs turning it with the bracket on
        // the west wall. A pillar in the iron room has braziers on two faces, with the gap of an outside corner.
        fill(level, -63, GROUND, -3, -42, GROUND, 9, Blocks.STONE);
        fill(level, -63, GROUND + 1, -2, -52, GROUND + 6, -2, Blocks.STONE);
        fill(level, -52, GROUND + 1, -2, -52, GROUND + 6, 4, Blocks.STONE);
        fill(level, -49, GROUND + 1, -2, -42, GROUND + 6, -2, Blocks.STONE);
        fill(level, -49, GROUND + 1, -2, -49, GROUND + 6, 4, Blocks.STONE);
        fill(level, -58, GROUND + 1, 5, -58, GROUND + 6, 5, Blocks.STONE);
        BlockState iron = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.IRON, "")).defaultBlockState()
                .setValue(CampfireBlock.LIT, true).setValue(BrazierBlock.CLOSED, true).setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
        BlockState copper = brazier("", BrazierFire.SOUL, true, true).setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
        List<BlockPos> corners = new ArrayList<>();
        for (int x = -57; x <= -54; x++) corners.add(placeWall(level, new BlockPos(x, GROUND + 3, -1), iron, Direction.SOUTH));
        corners.add(placeWall(level, ironCornerPos(), iron, Direction.SOUTH));
        for (int z = 0; z <= 2; z++) corners.add(placeWall(level, new BlockPos(-53, GROUND + 3, z), iron, Direction.WEST));
        for (int x = -47; x <= -44; x++) corners.add(placeWall(level, new BlockPos(x, GROUND + 3, -1), copper, Direction.SOUTH));
        corners.add(placeWall(level, new BlockPos(-48, GROUND + 3, -1), copper, Direction.EAST));
        for (int z = 0; z <= 2; z++) corners.add(placeWall(level, new BlockPos(-48, GROUND + 3, z), copper, Direction.EAST));
        corners.add(placeWall(level, new BlockPos(-58, GROUND + 3, 6), iron, Direction.SOUTH));
        corners.add(placeWall(level, new BlockPos(-57, GROUND + 3, 5), iron, Direction.EAST));
        corners.add(placeWall(level, new BlockPos(-59, GROUND + 3, 5), iron, Direction.WEST));
        // Outside corner pieces beside the pillar's two southern edges wrap the trough round three faces
        for (BlockPos outside : PILLAR_CORNERS) {
            corners.add(placeWall(level, outside, iron, Direction.SOUTH));
        }
        corners.forEach((pos) -> connect(level, pos));
        addFood(level, ironCornerPos());
    }

    /** The outside corner pieces beside the pillar's south-east and south-west edges, in its south face's run. */
    private static final List<BlockPos> PILLAR_CORNERS = List.of(new BlockPos(-57, GROUND + 3, 6), new BlockPos(-59, GROUND + 3, 6));

    /** The iron inside corner: walls to its north and east, runs coming in from the west and the south. */
    private static BlockPos ironCornerPos() {
        return new BlockPos(-53, GROUND + 3, -1);
    }

    private static BlockPos placeWall(ServerLevel level, BlockPos pos, BlockState wall, Direction facing) {
        place(level, pos, wall.setValue(CampfireBlock.FACING, facing));
        return pos;
    }

    private static void addFood(ServerLevel level, BlockPos... positions) {
        for (BlockPos pos : positions) {
            if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
                campfire.placeFood(level, null, new ItemStack(Items.BEEF));
                campfire.placeFood(level, null, new ItemStack(Items.SALMON));
            }
        }
    }

    private static BlockState brazier(String aging, BrazierFire fire, boolean lit, boolean closed) {
        return ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.COPPER, aging)).defaultBlockState()
                .setValue(BrazierBlock.FIRE, fire).setValue(CampfireBlock.LIT, lit).setValue(BrazierBlock.CLOSED, closed);
    }

    /** Recomputes a placed brazier's connections from its neighbours, as a neighbour update would. */
    private static void connect(ServerLevel level, BlockPos pos) {
        level.setBlock(pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos), Block.UPDATE_CLIENTS);
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(x1, y1, z1, x2, y2, z2)) {
            level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private static void place(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError(failure);
        }
    }
}
