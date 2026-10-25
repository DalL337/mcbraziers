package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierPlacement;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Placing braziers with the real use key, through the crosshair's raycast, the client's prediction and the
 * server, as a player does (owner's in-game tests, 2026-10-25). The headless placement tests click a block
 * directly and can't see which face the crosshair lands on.
 *
 * <p>Two log pillars, each with a copper wall brazier on its west face. On one the crosshair is on the
 * brazier's south end, on the other on the south half of its front. Either click grows the run on past the
 * pillar's edge, straight, since the pillar's south face has no run yet; a second click on that face puts
 * the next brazier there, which turns the first into the outside corner piece, and the trough runs round the
 * pillar's edge. Each wrap is photographed from straight above.
 *
 * <p>A third pillar has a brazier on its west face and a second pillar four blocks south of it. Clicking the
 * front of each new brazier strings the run across the gap between them, out over open space, without
 * turning (owner, 2026-10-25).
 */
public class BrazierPlacementClientGameTest implements FabricClientGameTest {

    private static final int GROUND = 100;
    private static final BlockPos END_PILLAR = new BlockPos(200, GROUND + 1, 0);
    private static final BlockPos FRONT_PILLAR = new BlockPos(210, GROUND + 1, 0);
    private static final BlockPos STRING_PILLAR = new BlockPos(220, GROUND + 1, 0);
    /** Four blocks south of {@link #STRING_PILLAR}, with nothing on its faces: the run is strung across to it. */
    private static final BlockPos FAR_PILLAR = STRING_PILLAR.south(4);

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!ClientScenarios.runs("placement")) {
            return;
        }
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("time set noon");
            server.runCommand("item replace entity @a weapon.mainhand with mcbraziers:copper_brazier");
            server.runOnServer(minecraft -> {
                ServerLevel level = minecraft.overworld();
                for (BlockPos pos : BlockPos.betweenClosed(195, GROUND, -5, 225, GROUND, 7)) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                buildPillar(level, END_PILLAR);
                buildPillar(level, FRONT_PILLAR);
                buildPillar(level, STRING_PILLAR);
                level.setBlock(FAR_PILLAR, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
                level.setBlock(FAR_PILLAR.above(), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
            });

            // Standing south of the brazier, looking down at its south end; then at the pillar's south face
            wrap(context, singleplayer, END_PILLAR, "tp @a 199.5 " + (GROUND + 1) + " 2.5 180 34", Direction.SOUTH, "its south end",
                    "art-wall-outside-corner-placed");
            // Standing west of the brazier, looking down at the south half of its front; then as above
            wrap(context, singleplayer, FRONT_PILLAR, "tp @a 207.5 " + (GROUND + 1) + " 0.75 -90 32.6", Direction.WEST,
                    "the south half of its front", "art-wall-outside-corner-placed-from-front");
            // Standing west of the run, clicking the south half of each new brazier's front, as above
            string(context, singleplayer);
        }
    }

    /** A one-block oak log pillar, two high, with a copper wall brazier on its west face. */
    private static void buildPillar(ServerLevel level, BlockPos pillar) {
        level.setBlock(pillar, Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        level.setBlock(pillar.above(), Blocks.OAK_LOG.defaultBlockState(), Block.UPDATE_CLIENTS);
        BlockState brazier = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.COPPER, "")).defaultBlockState()
                .setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL).setValue(BrazierBlock.CLOSED, true)
                .setValue(CampfireBlock.FACING, Direction.WEST);
        level.setBlock(pillar.west(), brazier, Block.UPDATE_CLIENTS);
    }

    /**
     * Clicks the brazier on {@code pillar}'s west face where {@code teleport} aims ({@code face} of it), checks
     * the run went on straight into the block beside the pillar's edge, then clicks the pillar's south face and
     * checks the trough now runs round the edge, and photographs it from above.
     */
    private static void wrap(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos pillar, String teleport,
            Direction face, String where, String screenshot) {
        TestServerContext server = singleplayer.getServer();
        BlockPos run = pillar.west();
        BlockPos corner = run.south();
        BlockPos aroundTheCorner = pillar.south();
        server.runCommand("gamemode creative @a");
        click(context, singleplayer, teleport, run, face, where);
        check(server, corner, Direction.WEST, Direction.NORTH, false, "clicking " + where + " should grow the run past the edge without turning");
        if (!server.computeOnServer(minecraft -> minecraft.overworld().getBlockState(aroundTheCorner).isAir())) {
            throw new AssertionError("clicking " + where + " should leave the pillar's south face for the next click");
        }

        click(context, singleplayer, "tp @a " + (pillar.getX() + 0.5) + " " + (GROUND + 1) + " 3.5 180 24", pillar, Direction.SOUTH,
                "the pillar's south face");
        check(server, corner, Direction.WEST, Direction.NORTH, true, "the corner piece should join the run on each face");
        boolean joined = server.computeOnServer(minecraft -> {
            BlockState next = minecraft.overworld().getBlockState(aroundTheCorner);
            return next.getBlock() instanceof BrazierBlock && next.getValue(CampfireBlock.FACING) == Direction.SOUTH
                    && next.getValue(BrazierBlock.WEST_CONNECTED);
        });
        if (!joined) {
            throw new AssertionError("the brazier on the pillar's south face should face south and join the corner piece");
        }

        photograph(context, singleplayer, "tp @a " + (pillar.getX() - 0.2) + " " + (GROUND + 1.8) + " 0.8 0 90", screenshot);
    }

    /**
     * Clicks the south half of the front of the last brazier in the run from {@link #STRING_PILLAR} four times,
     * stringing the run across the gap to {@link #FAR_PILLAR}; checks each brazier went on straight, joined to
     * the one before, and photographs the string from the front and from behind.
     */
    private static void string(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
        TestServerContext server = singleplayer.getServer();
        BlockPos first = STRING_PILLAR.west();
        server.runCommand("gamemode creative @a");
        for (int i = 0; i < 4; i++) {
            BlockPos last = first.south(i);
            click(context, singleplayer, "tp @a " + (last.getX() - 1.5) + " " + (GROUND + 1) + " " + (last.getZ() + 0.75) + " -90 32.6",
                    last, Direction.WEST, "the south half of the front of the brazier at " + last);
        }
        for (int i = 1; i <= 4; i++) {
            check(server, first.south(i), Direction.WEST, Direction.NORTH, false, "the run should go on straight across the gap");
        }
        photograph(context, singleplayer, "tp @a 215.5 " + (GROUND + 1.2) + " 6.5 -138.4 12.4", "art-wall-string-front");
        photograph(context, singleplayer, "tp @a 223.5 " + (GROUND + 1) + " 2.5 90 18.7", "art-wall-string-behind");
    }

    /** Takes a screenshot as a spectator where {@code teleport} puts them, with the HUD hidden. */
    private static void photograph(ClientGameTestContext context, TestSingleplayerContext singleplayer, String teleport, String screenshot) {
        TestServerContext server = singleplayer.getServer();
        server.runCommand("gamemode spectator @a");
        server.runCommand(teleport);
        context.runOnClient(minecraft -> {
            if (!minecraft.gui.hud.isHidden()) {
                minecraft.gui.hud.toggle();
            }
        });
        context.waitTicks(10);
        singleplayer.getConnection().waitForChunksRender();
        context.waitTicks(20);
        context.takeScreenshot(TestScreenshotOptions.of(screenshot).withSize(1280, 720));
        context.runOnClient(minecraft -> {
            if (minecraft.gui.hud.isHidden()) {
                minecraft.gui.hud.toggle();
            }
        });
    }

    /** Stands where {@code teleport} puts the player, checks the crosshair is on {@code face} of {@code target}, and uses the item once. */
    private static void click(ClientGameTestContext context, TestSingleplayerContext singleplayer, String teleport, BlockPos target,
            Direction face, String where) {
        singleplayer.getServer().runCommand(teleport);
        context.waitFor(minecraft -> minecraft.level != null && !minecraft.level.getBlockState(target).isAir());
        singleplayer.getConnection().waitForChunksRender();
        context.waitTicks(10);
        String aimed = context.computeOnClient(minecraft -> describe(minecraft.hitResult));
        boolean onFace = context.computeOnClient(minecraft -> minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(target) && hit.getDirection() == face);
        if (!onFace) {
            throw new AssertionError("the crosshair should be on " + where + " at " + target + ", but is on " + aimed);
        }
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(10);
    }

    /** A wall brazier at {@code pos} facing {@code facing}, joined on {@code side}, and behind only if {@code turned}. */
    private static void check(TestServerContext server, BlockPos pos, Direction facing, Direction side, boolean turned, String what) {
        String found = server.computeOnServer(minecraft -> {
            BlockState state = minecraft.overworld().getBlockState(pos);
            boolean ok = state.getBlock() instanceof BrazierBlock && state.getValue(BrazierBlock.PLACEMENT) == BrazierPlacement.WALL
                    && state.getValue(CampfireBlock.FACING) == facing && state.getValue(connected(side))
                    && state.getValue(connected(facing.getOpposite())) == turned;
            return ok ? null : state.toString();
        });
        if (found != null) {
            throw new AssertionError(what + "; found " + found);
        }
    }

    private static BooleanProperty connected(Direction side) {
        return switch (side) {
            case NORTH -> BrazierBlock.NORTH_CONNECTED;
            case SOUTH -> BrazierBlock.SOUTH_CONNECTED;
            case EAST -> BrazierBlock.EAST_CONNECTED;
            default -> BrazierBlock.WEST_CONNECTED;
        };
    }

    private static String describe(HitResult hit) {
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                ? block.getBlockPos() + " face " + block.getDirection() + " at " + hit.getLocation()
                : String.valueOf(hit == null ? null : hit.getType());
    }
}
