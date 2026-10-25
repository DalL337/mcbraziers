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
 * its run along the wall, and on past its end over open space; sneaking places by the clicked face, as
 * before. Wall runs turn inside corners, and outside corners only where both faces have runs.
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
    public void pastTheWallsEdgeTheRunGoesOnStraight(GameTestHelper helper) {
        // A one-block pillar at (1, 2, 3): past the brazier's east end the wall stops, and with no run on the
        // pillar's east face the run goes on straight out over open space instead of turning the corner
        BlockPos first = wallBrazier(helper, 1, 1, 1);
        BlockPos next = first.east();
        helper.placeAt(player(helper, false), brazierItem(), first, Direction.EAST);
        helper.placeAt(player(helper, false), brazierItem(), next, Direction.EAST);
        for (BlockPos past : new BlockPos[] {next, next.east()}) {
            assertStraight(helper, past, Direction.NORTH);
            helper.assertBlockProperty(past, BrazierBlock.WEST_CONNECTED, true);
            helper.assertBlockPresent(Blocks.AIR, past.south());
        }
        helper.assertBlockProperty(next, BrazierBlock.EAST_CONNECTED, true);
        helper.succeed();
    }

    @GameTest
    public void aRunCanBeStrungAcrossAGapToTheNextWall(GameTestHelper helper) {
        // Pillars at (0, 2, 3) and (4, 2, 3), three blocks apart: clicking each new brazier's east end strings
        // the run from the first pillar across the gap to the second
        BlockPos first = wallBrazier(helper, 0, 0, 0);
        helper.setBlock(new BlockPos(4, 2, 3), Blocks.STONE);
        for (int x = 0; x < 4; x++) {
            helper.placeAt(player(helper, false), brazierItem(), first.east(x), Direction.EAST);
        }
        for (int x = 1; x <= 4; x++) {
            assertStraight(helper, first.east(x), Direction.NORTH);
            helper.assertBlockProperty(first.east(x), BrazierBlock.WEST_CONNECTED, true);
            helper.assertBlockProperty(first.east(x), BrazierBlock.EAST_CONNECTED, x < 4);
        }
        // Breaking one in the middle leaves each half hanging from its pillar
        helper.setBlock(first.east(2), Blocks.AIR);
        helper.assertBlockProperty(first.east(1), BrazierBlock.EAST_CONNECTED, false);
        helper.assertBlockProperty(first.east(1), BrazierBlock.WEST_CONNECTED, true);
        helper.assertBlockProperty(first.east(3), BrazierBlock.WEST_CONNECTED, false);
        helper.assertBlockProperty(first.east(3), BrazierBlock.EAST_CONNECTED, true);
        helper.succeed();
    }

    @GameTest
    public void aRunOnTheNextFaceTurnsTheBrazierAtTheEdgeIntoTheCornerPiece(GameTestHelper helper) {
        // Only where both faces of the pillar have runs does the brazier beside its edge turn the corner: clicking
        // that brazier's open back puts the next one on the pillar's east face, and the runs join through it
        BlockPos first = wallBrazier(helper, 1, 1, 1);
        BlockPos cornerBlock = first.east();
        BlockPos aroundTheCorner = new BlockPos(2, 2, 3);
        helper.placeAt(player(helper, false), brazierItem(), first, Direction.EAST);
        helper.placeAt(player(helper, false), brazierItem(), cornerBlock, Direction.SOUTH);
        helper.assertBlockProperty(aroundTheCorner, CampfireBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(aroundTheCorner, BrazierBlock.NORTH_CONNECTED, true);
        assertOutsideCorner(helper, cornerBlock, Direction.NORTH, Direction.WEST);
        // Without the run around the corner, it's straight again, hanging from the run it came from
        helper.setBlock(aroundTheCorner, Blocks.AIR);
        assertStraight(helper, cornerBlock, Direction.NORTH);
        helper.assertBlockProperty(cornerBlock, BrazierBlock.WEST_CONNECTED, true);
        // With neither, it falls
        helper.setBlock(first, Blocks.AIR);
        helper.assertBlockPresent(Blocks.AIR, cornerBlock);
        helper.succeed();
    }

    @GameTest
    public void aRunThatGoesOnPastTheEdgeDoesNotTurnTheCorner(GameTestHelper helper) {
        // With the run strung on past the pillar's edge, a brazier then put on the pillar's east face stays
        // apart, rather than making the run a T
        BlockPos first = wallBrazier(helper, 1, 1, 1);
        BlockPos pastTheEdge = first.east();
        BlockPos onEastFace = new BlockPos(2, 2, 3);
        helper.placeAt(player(helper, false), brazierItem(), first, Direction.EAST);
        helper.placeAt(player(helper, false), brazierItem(), pastTheEdge, Direction.EAST);
        helper.placeAt(player(helper, false), brazierItem(), onEastFace.west(), Direction.EAST);
        helper.assertBlockProperty(onEastFace, CampfireBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(onEastFace, BrazierBlock.NORTH_CONNECTED, false);
        assertStraight(helper, pastTheEdge, Direction.NORTH);
        helper.assertBlockProperty(pastTheEdge, BrazierBlock.WEST_CONNECTED, true);
        helper.assertBlockProperty(pastTheEdge, BrazierBlock.EAST_CONNECTED, true);
        helper.succeed();
    }

    @GameTest
    public void aBrazierHungOnTheEndOfTheOnlyRunAtAnEdgeStaysApart(GameTestHelper helper) {
        // A run on the pillar's east face only: sneaking hangs one on its north end, in the block beside the
        // pillar's edge, but with no run on the pillar's north face it doesn't turn the corner
        BlockPos pillar = new BlockPos(1, 2, 3);
        helper.setBlock(pillar, Blocks.STONE);
        BlockPos onEast = pillar.east();
        helper.placeAt(player(helper, false), brazierItem(), pillar, Direction.EAST);
        helper.placeAt(player(helper, true), brazierItem(), onEast, Direction.NORTH);
        assertStraight(helper, onEast.north(), Direction.NORTH);
        helper.assertBlockProperty(onEast, BrazierBlock.NORTH_CONNECTED, false);
        helper.succeed();
    }

    /** A straight wall brazier at {@code pos} facing {@code facing}: joined neither in front nor behind. */
    private static void assertStraight(GameTestHelper helper, BlockPos pos, Direction facing) {
        helper.assertBlockProperty(pos, BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
        helper.assertBlockProperty(pos, CampfireBlock.FACING, facing);
        helper.assertBlockProperty(pos, connected(facing), false);
        helper.assertBlockProperty(pos, connected(facing.getOpposite()), false);
    }

    /** The outside corner piece at {@code pos}: facing {@code facing}, joined behind and on {@code run}'s side only. */
    private static void assertOutsideCorner(GameTestHelper helper, BlockPos pos, Direction facing, Direction run) {
        helper.assertBlockProperty(pos, BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
        helper.assertBlockProperty(pos, CampfireBlock.FACING, facing);
        helper.assertBlockProperty(pos, connected(facing.getOpposite()), true);
        helper.assertBlockProperty(pos, connected(run), true);
        helper.assertBlockProperty(pos, connected(facing), false);
        helper.assertBlockProperty(pos, connected(run.getOpposite()), false);
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
    public void clickingTheFrontNearTheWallsEdgeGoesOnStraight(GameTestHelper helper) {
        // A one-block pillar at (1, 2, 3): the east half of the brazier's front grows the run on past the
        // pillar's edge, as clicking its east end does
        BlockPos first = wallBrazier(helper, 1, 1, 1);
        clickFront(helper, first, Direction.NORTH, Direction.EAST);
        assertStraight(helper, first.east(), Direction.NORTH);
        helper.assertBlockProperty(first.east(), BrazierBlock.WEST_CONNECTED, true);
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
    public void runsOnTwoFacesJoinThroughTheCornerPiece(GameTestHelper helper) {
        // Two one-block pillars, each with a brazier on its north face and one on its east face: clicking either
        // run's end at the pillar's edge places the corner piece between them, facing that run's way, and the
        // trough runs round the pillar's edge
        for (boolean fromEast : new boolean[] {true, false}) {
            BlockPos pillar = new BlockPos(fromEast ? 2 : 5, 2, 2);
            helper.setBlock(pillar, Blocks.STONE);
            BlockPos onNorth = pillar.north();
            BlockPos onEast = pillar.east();
            helper.placeAt(player(helper, false), brazierItem(), pillar, Direction.NORTH);
            helper.placeAt(player(helper, false), brazierItem(), pillar, Direction.EAST);
            BlockPos cornerBlock = onEast.north();
            if (fromEast) {
                helper.placeAt(player(helper, false), brazierItem(), onEast, Direction.NORTH);
                assertOutsideCorner(helper, cornerBlock, Direction.EAST, Direction.SOUTH);
            } else {
                helper.placeAt(player(helper, false), brazierItem(), onNorth, Direction.EAST);
                assertOutsideCorner(helper, cornerBlock, Direction.NORTH, Direction.WEST);
            }
            helper.assertBlockProperty(onEast, BrazierBlock.NORTH_CONNECTED, true);
            helper.assertBlockProperty(onNorth, BrazierBlock.EAST_CONNECTED, true);
        }
        helper.succeed();
    }

    @GameTest
    public void aBrazierHungOnTheEndOfARunAlongAWallStaysApart(GameTestHelper helper) {
        // Sneaking hangs one on the end of a run, as before; where the wall goes on behind it, it isn't a
        // corner, so it doesn't join the run
        BlockPos first = wallBrazier(helper, 1, 0, 2);
        BlockPos hung = first.east();
        helper.placeAt(player(helper, true), brazierItem(), first, Direction.EAST);
        helper.assertBlockProperty(hung, CampfireBlock.FACING, Direction.EAST);
        helper.assertBlockProperty(hung, BrazierBlock.WEST_CONNECTED, false);
        helper.assertBlockProperty(first, BrazierBlock.EAST_CONNECTED, false);
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
