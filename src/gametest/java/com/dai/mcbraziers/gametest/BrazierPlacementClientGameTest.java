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
 * brazier's south end, on the other on the south half of its front. Either click places the outside corner
 * piece in the block beside the pillar's edge, turned toward the pillar's south face; a second click on that
 * face puts the next brazier there, and the trough runs round the pillar's edge. Each wrap is photographed
 * from straight above.
 */
public class BrazierPlacementClientGameTest implements FabricClientGameTest {

    private static final int GROUND = 100;
    private static final BlockPos END_PILLAR = new BlockPos(200, GROUND + 1, 0);
    private static final BlockPos FRONT_PILLAR = new BlockPos(210, GROUND + 1, 0);

    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("time set noon");
            server.runCommand("item replace entity @a weapon.mainhand with mcbraziers:copper_brazier");
            server.runOnServer(minecraft -> {
                ServerLevel level = minecraft.overworld();
                for (BlockPos pos : BlockPos.betweenClosed(195, GROUND, -5, 215, GROUND, 5)) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                buildPillar(level, END_PILLAR);
                buildPillar(level, FRONT_PILLAR);
            });

            // Standing south of the brazier, looking down at its south end; then at the pillar's south face
            wrap(context, singleplayer, END_PILLAR, "tp @a 199.5 " + (GROUND + 1) + " 2.5 180 34", Direction.SOUTH, "its south end",
                    "art-wall-outside-corner-placed");
            // Standing west of the brazier, looking down at the south half of its front; then as above
            wrap(context, singleplayer, FRONT_PILLAR, "tp @a 207.5 " + (GROUND + 1) + " 0.75 -90 32.6", Direction.WEST,
                    "the south half of its front", "art-wall-outside-corner-placed-from-front");
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
     * the corner piece went into the block beside the pillar's edge, then clicks the pillar's south face and
     * checks the trough runs round the edge, and photographs it from above.
     */
    private static void wrap(ClientGameTestContext context, TestSingleplayerContext singleplayer, BlockPos pillar, String teleport,
            Direction face, String where, String screenshot) {
        TestServerContext server = singleplayer.getServer();
        BlockPos run = pillar.west();
        BlockPos corner = run.south();
        BlockPos aroundTheCorner = pillar.south();
        server.runCommand("gamemode creative @a");
        click(context, singleplayer, teleport, run, face, where);
        check(server, corner, Direction.WEST, Direction.EAST, Direction.NORTH, "clicking " + where + " should place the corner piece");
        if (!server.computeOnServer(minecraft -> minecraft.overworld().getBlockState(aroundTheCorner).isAir())) {
            throw new AssertionError("clicking " + where + " should leave the pillar's south face for the next click");
        }

        click(context, singleplayer, "tp @a " + (pillar.getX() + 0.5) + " " + (GROUND + 1) + " 3.5 180 24", pillar, Direction.SOUTH,
                "the pillar's south face");
        check(server, corner, Direction.WEST, Direction.EAST, Direction.NORTH, "the corner piece should join the run on each face");
        boolean joined = server.computeOnServer(minecraft -> {
            BlockState next = minecraft.overworld().getBlockState(aroundTheCorner);
            return next.getBlock() instanceof BrazierBlock && next.getValue(CampfireBlock.FACING) == Direction.SOUTH
                    && next.getValue(BrazierBlock.WEST_CONNECTED);
        });
        if (!joined) {
            throw new AssertionError("the brazier on the pillar's south face should face south and join the corner piece");
        }

        server.runCommand("gamemode spectator @a");
        server.runCommand("tp @a " + (pillar.getX() - 0.2) + " " + (GROUND + 1.8) + " 0.8 0 90");
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

    /** A wall brazier at {@code pos} facing {@code facing}, joined behind ({@code back}) and on {@code side}. */
    private static void check(TestServerContext server, BlockPos pos, Direction facing, Direction back, Direction side, String what) {
        String found = server.computeOnServer(minecraft -> {
            BlockState state = minecraft.overworld().getBlockState(pos);
            boolean ok = state.getBlock() instanceof BrazierBlock && state.getValue(BrazierBlock.PLACEMENT) == BrazierPlacement.WALL
                    && state.getValue(CampfireBlock.FACING) == facing && state.getValue(connected(back)) && state.getValue(connected(side));
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
