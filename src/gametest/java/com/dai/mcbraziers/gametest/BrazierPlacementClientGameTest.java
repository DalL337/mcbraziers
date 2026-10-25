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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Placing braziers with the real use key, through the crosshair's raycast, the client's prediction and the
 * server, as a player does (owner's in-game tests, 2026-10-25). The headless placement tests click a block
 * directly and can't see which face the crosshair lands on. Two log pillars, each with a copper wall brazier
 * on its west face: clicking the brazier's south end, and the south half of its front, each send the next
 * brazier around the pillar's corner onto its south face.
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
            server.runCommand("gamemode creative @a");
            server.runCommand("item replace entity @a weapon.mainhand with mcbraziers:copper_brazier");
            server.runOnServer(minecraft -> {
                ServerLevel level = minecraft.overworld();
                for (BlockPos pos : BlockPos.betweenClosed(195, GROUND, -5, 215, GROUND, 5)) {
                    level.setBlock(pos, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                buildPillar(level, END_PILLAR);
                buildPillar(level, FRONT_PILLAR);
            });

            // Standing south of the brazier, looking down at its south end
            clickAndCheck(context, singleplayer, "tp @a 199.5 " + (GROUND + 1) + " 2.5 180 34", END_PILLAR, Direction.SOUTH,
                    "its south end");
            // Standing west of the brazier, looking down at the south half of its front
            clickAndCheck(context, singleplayer, "tp @a 207.5 " + (GROUND + 1) + " 0.75 -90 32.6", FRONT_PILLAR, Direction.WEST,
                    "the south half of its front");
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
     * Stands where {@code teleport} puts the player, checks the crosshair is on {@code face} of the brazier on
     * the pillar's west face, presses the use key once, and checks the new brazier went onto the pillar's south
     * face, facing south, with the corner block and the block in front of the clicked brazier left empty.
     */
    private static void clickAndCheck(ClientGameTestContext context, TestSingleplayerContext singleplayer, String teleport,
            BlockPos pillar, Direction face, String where) {
        TestServerContext server = singleplayer.getServer();
        BlockPos clicked = pillar.west();
        server.runCommand(teleport);
        context.waitFor(minecraft -> minecraft.level != null && minecraft.level.getBlockState(clicked).getBlock() instanceof BrazierBlock);
        singleplayer.getConnection().waitForChunksRender();
        context.waitTicks(10);
        String aimed = context.computeOnClient(minecraft -> describe(minecraft.hitResult));
        boolean onFace = context.computeOnClient(minecraft -> minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(clicked) && hit.getDirection() == face);
        if (!onFace) {
            throw new AssertionError("the crosshair should be on " + where + " of the brazier at " + clicked + ", but is on " + aimed);
        }
        context.getInput().pressKey(options -> options.keyUse);
        context.waitTicks(10);
        String problem = server.computeOnServer(minecraft -> {
            ServerLevel level = minecraft.overworld();
            BlockState placed = level.getBlockState(pillar.south());
            if (!(placed.getBlock() instanceof BrazierBlock) || placed.getValue(BrazierBlock.PLACEMENT) != BrazierPlacement.WALL
                    || placed.getValue(CampfireBlock.FACING) != Direction.SOUTH) {
                return "the pillar's south face holds " + placed;
            }
            if (!level.getBlockState(clicked.south()).isAir() || !level.getBlockState(clicked.west()).isAir()) {
                return "the corner block holds " + level.getBlockState(clicked.south()) + " and the block in front "
                        + level.getBlockState(clicked.west());
            }
            return null;
        });
        if (problem != null) {
            throw new AssertionError("clicking " + where + " (crosshair on " + aimed + "): " + problem);
        }
    }

    private static String describe(HitResult hit) {
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK
                ? block.getBlockPos() + " face " + block.getDirection() + " at " + hit.getLocation()
                : String.valueOf(hit == null ? null : hit.getType());
    }
}
