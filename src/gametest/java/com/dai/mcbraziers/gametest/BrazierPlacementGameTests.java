package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierPlacement;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Growing fire pits by clicking a brazier (owner's rulings, 2026-10-25; PRD Placement addendum): the side of
 * a floor brazier places a floor brazier beside it, even out over air; the end of a wall brazier continues
 * its run along the wall, or around the corner where the wall stops; sneaking places by the clicked face, as
 * before. Wall runs turn inside corners and leave the corner block empty at outside corners.
 */
public class BrazierPlacementGameTests {

    private static final BrazierVariant IRON = new BrazierVariant(BrazierTier.IRON, "");

    private static ItemStack brazierItem() {
        return new ItemStack(ModItems.brazierItem(IRON, BrazierFire.NORMAL));
    }

    private static Player player(GameTestHelper helper, boolean sneaking) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setShiftKeyDown(sneaking);
        return player;
    }

    /** A floor brazier standing on stone at (1, 2, 1). */
    private static BlockPos floorBrazier(GameTestHelper helper) {
        BlockPos ground = new BlockPos(1, 1, 1);
        helper.setBlock(ground, Blocks.STONE);
        helper.placeAt(player(helper, false), brazierItem(), ground, Direction.UP);
        return ground.above();
    }

    /** A wall brazier at (x, 2, 2) on the north face of a stone wall along z = 3 from x0 to x1. */
    private static BlockPos wallBrazier(GameTestHelper helper, int x, int x0, int x1) {
        for (int wall = x0; wall <= x1; wall++) {
            helper.setBlock(new BlockPos(wall, 2, 3), Blocks.STONE);
        }
        BlockPos pos = new BlockPos(x, 2, 2);
        helper.placeAt(player(helper, false), brazierItem(), pos.south(), Direction.NORTH);
        helper.assertBlockProperty(pos, CampfireBlock.FACING, Direction.NORTH);
        return pos;
    }

    @GameTest
    public void theSideOfAFloorBrazierGrowsItsFirePitOutOverAir(GameTestHelper helper) {
        BlockPos first = floorBrazier(helper);
        BlockPos next = first.east();
        helper.placeAt(player(helper, false), brazierItem(), first, Direction.EAST);
        helper.assertBlockProperty(next, BrazierBlock.PLACEMENT, BrazierPlacement.FLOOR);
        helper.assertBlockPresent(Blocks.AIR, next.below());
        helper.assertBlockProperty(first, BrazierBlock.EAST_CONNECTED, true);
        helper.assertBlockProperty(next, BrazierBlock.WEST_CONNECTED, true);
        helper.succeed();
    }

    @GameTest
    public void sneakingOnTheSideOfAFloorBrazierMountsOneOnIt(GameTestHelper helper) {
        BlockPos first = floorBrazier(helper);
        BlockPos mounted = first.east();
        helper.placeAt(player(helper, true), brazierItem(), first, Direction.EAST);
        helper.assertBlockProperty(mounted, BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
        helper.assertBlockProperty(mounted, CampfireBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(first, BrazierBlock.EAST_CONNECTED, false);
        helper.succeed();
    }

    @GameTest
    public void eitherEndOfAWallBrazierContinuesItsRunAlongTheWall(GameTestHelper helper) {
        BlockPos first = wallBrazier(helper, 1, 0, 2);
        for (Direction end : new Direction[] {Direction.EAST, Direction.WEST}) {
            BlockPos next = first.relative(end);
            helper.placeAt(player(helper, false), brazierItem(), first, end);
            helper.assertBlockProperty(next, BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
            helper.assertBlockProperty(next, CampfireBlock.FACING, Direction.NORTH);
            helper.assertBlockProperty(first, connected(end), true);
            helper.assertBlockProperty(next, connected(end.getOpposite()), true);
        }
        helper.succeed();
    }

    @GameTest
    public void whereTheWallStopsTheNextBrazierGoesAroundTheCorner(GameTestHelper helper) {
        // A one-block pillar at (1, 2, 3): past the brazier's east end there's no wall to continue on, so the
        // next brazier goes onto the pillar's east face, bracketed to the pillar, and the corner block stays empty
        BlockPos first = wallBrazier(helper, 1, 1, 1);
        BlockPos cornerBlock = first.east();
        BlockPos aroundTheCorner = new BlockPos(2, 2, 3);
        helper.placeAt(player(helper, false), brazierItem(), first, Direction.EAST);
        helper.assertBlockPresent(Blocks.AIR, cornerBlock);
        helper.assertBlockProperty(aroundTheCorner, BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
        helper.assertBlockProperty(aroundTheCorner, CampfireBlock.FACING, Direction.EAST);
        helper.succeed();
    }

    @GameTest
    public void clickingTheFrontOfAWallBrazierGrowsTheRunFromTheNearerEnd(GameTestHelper helper) {
        BlockPos first = wallBrazier(helper, 1, 0, 2);
        for (Direction end : new Direction[] {Direction.EAST, Direction.WEST}) {
            clickFront(helper, first, Direction.NORTH, end);
            helper.assertBlockProperty(first.relative(end), CampfireBlock.FACING, Direction.NORTH);
            helper.assertBlockProperty(first, connected(end), true);
        }
        helper.assertBlockPresent(Blocks.AIR, first.north());
        helper.succeed();
    }

    @GameTest
    public void clickingTheFrontNearTheWallsEdgeGoesAroundTheCorner(GameTestHelper helper) {
        // A one-block pillar at (1, 2, 3): the east half of the brazier's front sends the next one around the
        // pillar's corner, as clicking its east end does
        BlockPos first = wallBrazier(helper, 1, 1, 1);
        clickFront(helper, first, Direction.NORTH, Direction.EAST);
        helper.assertBlockProperty(new BlockPos(2, 2, 3), CampfireBlock.FACING, Direction.EAST);
        helper.assertBlockPresent(Blocks.AIR, first.east());
        helper.assertBlockPresent(Blocks.AIR, first.north());
        helper.succeed();
    }

    @GameTest
    public void clickingTheFrontTowardATakenSpotPlacesNothing(GameTestHelper helper) {
        BlockPos first = wallBrazier(helper, 1, 0, 2);
        helper.setBlock(first.east(), Blocks.STONE);
        clickFront(helper, first, Direction.NORTH, Direction.EAST);
        helper.assertBlockPresent(Blocks.STONE, first.east());
        helper.assertBlockPresent(Blocks.AIR, first.north());
        helper.succeed();
    }

    /**
     * Clicks the front of the wall brazier at {@code pos}, facing {@code facing}, on the half toward
     * {@code toward}, as the crosshair would: on the basket's front, 4 px out from the middle of the block.
     */
    private static void clickFront(GameTestHelper helper, BlockPos pos, Direction facing, Direction toward) {
        Player player = player(helper, false);
        ItemStack stack = brazierItem();
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos brazier = helper.absolutePos(pos);
        Vec3 click = Vec3.atCenterOf(brazier).relative(facing, 0.25).relative(toward, 0.3);
        stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(click, facing, brazier, false)));
    }

    @GameTest
    public void sneakingOnTheEndOfAWallBrazierMountsOneOnIt(GameTestHelper helper) {
        BlockPos first = wallBrazier(helper, 1, 0, 2);
        BlockPos mounted = first.east();
        helper.placeAt(player(helper, true), brazierItem(), first, Direction.EAST);
        helper.assertBlockProperty(mounted, CampfireBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(first, BrazierBlock.EAST_CONNECTED, false);
        helper.succeed();
    }

    /**
     * An inside corner: a wall along z = 3 (x 0 to 4) meets one along x = 4 (z 0 to 3). A run on the first
     * wall ends at (2, 2, 2) facing north, one on the second at (3, 2, 1) facing west; the corner block
     * between them is (3, 2, 2).
     */
    private static BlockPos[] insideCorner(GameTestHelper helper) {
        for (int i = 0; i <= 3; i++) {
            helper.setBlock(new BlockPos(i, 2, 3), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 2, i), Blocks.STONE);
        }
        BlockPos alongSouth = new BlockPos(2, 2, 2);
        BlockPos alongEast = new BlockPos(3, 2, 1);
        helper.placeAt(player(helper, false), brazierItem(), alongSouth.south(), Direction.NORTH);
        helper.placeAt(player(helper, false), brazierItem(), alongEast.east(), Direction.WEST);
        return new BlockPos[] {alongSouth, alongEast, new BlockPos(3, 2, 2)};
    }

    @GameTest
    public void aRunContinuedIntoAnInsideCornerTurnsIntoTheOtherRun(GameTestHelper helper) {
        BlockPos[] corner = insideCorner(helper);
        BlockPos alongSouth = corner[0], alongEast = corner[1], piece = corner[2];
        helper.placeAt(player(helper, false), brazierItem(), alongSouth, Direction.EAST);
        // The corner piece keeps its bracket on the wall of the run it continues
        helper.assertBlockProperty(piece, CampfireBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(piece, BrazierBlock.WEST_CONNECTED, true);
        helper.assertBlockProperty(piece, BrazierBlock.NORTH_CONNECTED, true);
        helper.assertBlockProperty(piece, BrazierBlock.EAST_CONNECTED, false);
        helper.assertBlockProperty(alongSouth, BrazierBlock.EAST_CONNECTED, true);
        helper.assertBlockProperty(alongEast, BrazierBlock.SOUTH_CONNECTED, true);
        helper.succeed();
    }

    @GameTest
    public void theOtherRunContinuedIntoTheCornerTurnsTheSameWay(GameTestHelper helper) {
        BlockPos[] corner = insideCorner(helper);
        BlockPos alongSouth = corner[0], alongEast = corner[1], piece = corner[2];
        helper.placeAt(player(helper, false), brazierItem(), alongEast, Direction.SOUTH);
        helper.assertBlockProperty(piece, CampfireBlock.FACING, Direction.WEST);
        helper.assertBlockProperty(piece, BrazierBlock.NORTH_CONNECTED, true);
        helper.assertBlockProperty(piece, BrazierBlock.WEST_CONNECTED, true);
        helper.assertBlockProperty(piece, BrazierBlock.SOUTH_CONNECTED, false);
        helper.assertBlockProperty(alongSouth, BrazierBlock.EAST_CONNECTED, true);
        helper.assertBlockProperty(alongEast, BrazierBlock.SOUTH_CONNECTED, true);
        helper.succeed();
    }

    @GameTest
    public void runsAroundAnOutsideCornerStopWithAGap(GameTestHelper helper) {
        // A one-block pillar with a brazier on its north face and one on its east face
        BlockPos pillar = new BlockPos(2, 2, 2);
        helper.setBlock(pillar, Blocks.STONE);
        BlockPos onEast = pillar.east();
        helper.placeAt(player(helper, false), brazierItem(), pillar, Direction.NORTH);
        helper.placeAt(player(helper, false), brazierItem(), pillar, Direction.EAST);
        // Clicking the east run's north end: the pillar's north face already has its brazier, so nothing is
        // placed and the corner block stays empty
        BlockPos cornerBlock = onEast.north();
        helper.placeAt(player(helper, false), brazierItem(), onEast, Direction.NORTH);
        helper.assertBlockPresent(Blocks.AIR, cornerBlock);
        // Sneaking mounts one on that end, as before; nothing joins behind a wall brazier, so it stays apart
        helper.placeAt(player(helper, true), brazierItem(), onEast, Direction.NORTH);
        helper.assertBlockProperty(cornerBlock, CampfireBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(onEast, BrazierBlock.NORTH_CONNECTED, false);
        helper.assertBlockProperty(cornerBlock, BrazierBlock.SOUTH_CONNECTED, false);
        helper.succeed();
    }

    @GameTest
    public void aWallBrazierMountedOnTheFrontOfAnotherStaysApart(GameTestHelper helper) {
        // Only braziers beside each other on a wall join; one hung out in front of another (sneaking) doesn't
        BlockPos first = wallBrazier(helper, 1, 1, 1);
        BlockPos inFront = first.north();
        helper.placeAt(player(helper, true), brazierItem(), first, Direction.NORTH);
        helper.assertBlockProperty(inFront, CampfireBlock.FACING, Direction.NORTH);
        helper.assertBlockProperty(first, BrazierBlock.NORTH_CONNECTED, false);
        helper.assertBlockProperty(inFront, BrazierBlock.SOUTH_CONNECTED, false);
        helper.succeed();
    }

    private static BooleanProperty connected(Direction side) {
        return switch (side) {
            case NORTH -> BrazierBlock.NORTH_CONNECTED;
            case SOUTH -> BrazierBlock.SOUTH_CONNECTED;
            case EAST -> BrazierBlock.EAST_CONNECTED;
            default -> BrazierBlock.WEST_CONNECTED;
        };
    }
}
