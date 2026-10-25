package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierPlacement;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.FoodLayout;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.item.ModItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Art step, copper braziers (docs/plans/art/copper-brazier-art.md): which braziers join into fire pits,
 * the hitbox that follows each cage, and where food sits on each grate. Expected values are the plan's,
 * in pixels.
 */
public class BrazierCageGameTests {

    private static final BrazierVariant COPPER = new BrazierVariant(BrazierTier.COPPER, "");

    private static ItemStack brazierItem() {
        return new ItemStack(ModItems.brazierItem(COPPER, BrazierFire.NORMAL));
    }

    @GameTest
    public void onlyBraziersPlacedTheSameWayJoin(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.CREATIVE);
        helper.setBlock(new BlockPos(1, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 4), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(3, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 2, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(6, 2, 3), Blocks.STONE);

        // Two floor braziers side by side join
        BlockPos floorA = new BlockPos(1, 2, 4);
        helper.placeAt(player, brazierItem(), floorA.below(), Direction.UP);
        helper.setBlock(new BlockPos(1, 1, 3), Blocks.STONE);
        BlockPos floorB = new BlockPos(1, 2, 3);
        helper.placeAt(player, brazierItem(), floorB.below(), Direction.UP);
        helper.assertBlockProperty(floorA, BrazierBlock.NORTH_CONNECTED, true);
        helper.assertBlockProperty(floorB, BrazierBlock.SOUTH_CONNECTED, true);

        // A wall brazier beside a floor brazier stays apart
        BlockPos wallA = new BlockPos(2, 2, 4);
        helper.placeAt(player, brazierItem(), wallA.south(), Direction.NORTH);
        helper.assertBlockProperty(wallA, BrazierBlock.PLACEMENT, BrazierPlacement.WALL);
        helper.assertBlockProperty(floorA, BrazierBlock.EAST_CONNECTED, false);
        helper.assertBlockProperty(wallA, BrazierBlock.WEST_CONNECTED, false);

        // Two wall braziers on the same wall join
        BlockPos wallB = new BlockPos(3, 2, 4);
        helper.placeAt(player, brazierItem(), wallB.south(), Direction.NORTH);
        helper.assertBlockProperty(wallA, BrazierBlock.EAST_CONNECTED, true);
        helper.assertBlockProperty(wallB, BrazierBlock.WEST_CONNECTED, true);

        // Wall braziers side by side on opposite walls stay apart
        BlockPos facingNorth = new BlockPos(5, 2, 4);
        BlockPos facingSouth = new BlockPos(6, 2, 4);
        helper.placeAt(player, brazierItem(), facingNorth.south(), Direction.NORTH);
        helper.placeAt(player, brazierItem(), facingSouth.north(), Direction.SOUTH);
        helper.assertBlockProperty(facingSouth, CampfireBlock.FACING, Direction.SOUTH);
        helper.assertBlockProperty(facingNorth, BrazierBlock.EAST_CONNECTED, false);
        helper.assertBlockProperty(facingSouth, BrazierBlock.WEST_CONNECTED, false);
        helper.succeed();
    }

    @GameTest
    public void hitboxFollowsEachCageAndReachesJoinedSides(GameTestHelper helper) {
        BlockPos floor = new BlockPos(1, 1, 1);
        helper.setBlock(floor, floorBrazier());
        assertBounds(helper, floor, new AABB(1.5, 0, 1.5, 14.5, 13, 14.5), "a lone floor brazier");

        BlockPos joinedFloor = new BlockPos(3, 1, 1);
        helper.setBlock(joinedFloor, floorBrazier().setValue(BrazierBlock.EAST_CONNECTED, true));
        assertBounds(helper, joinedFloor, new AABB(1.5, 0, 1.5, 16, 13, 14.5), "a floor brazier joined on its east side");

        // Wall: a basket 12 wide and 8 deep with its rim, held off the wall by a bracket whose plate (with its
        // cap and foot) runs from 0.5 to 14 px up the wall
        BlockPos wallNorth = new BlockPos(5, 1, 1);
        helper.setBlock(wallNorth, wallBrazier(Direction.NORTH));
        assertBounds(helper, wallNorth, new AABB(1.5, 0.5, 3.5, 14.5, 14, 16), "a wall brazier facing north");

        BlockPos wallEast = new BlockPos(7, 1, 1);
        helper.setBlock(wallEast, wallBrazier(Direction.EAST));
        assertBounds(helper, wallEast, new AABB(0, 0.5, 1.5, 12.5, 14, 14.5), "a wall brazier facing east");

        BlockPos joinedWall = new BlockPos(9, 1, 1);
        helper.setBlock(joinedWall, wallBrazier(Direction.NORTH).setValue(BrazierBlock.EAST_CONNECTED, true));
        assertBounds(helper, joinedWall, new AABB(1.5, 0.5, 3.5, 16, 14, 16), "a north-facing wall brazier joined on its east side");

        // Facing east, the basket's sides run north and south, so it joins along the wall to the north
        BlockPos joinedEastFacing = new BlockPos(13, 1, 1);
        helper.setBlock(joinedEastFacing, wallBrazier(Direction.EAST).setValue(BrazierBlock.NORTH_CONNECTED, true));
        assertBounds(helper, joinedEastFacing, new AABB(0, 0.5, 0, 12.5, 14, 14.5), "an east-facing wall brazier joined on its north side");

        // Inside corner (art plan, Inside corners addendum): joined on its front, the cage narrows to the 8 px
        // junction (rims 3.5 to 12.5 px) and reaches forward to the block edge, and to the edge on a joined side
        BlockPos cornerAlone = new BlockPos(1, 1, 4);
        helper.setBlock(cornerAlone, wallBrazier(Direction.NORTH).setValue(BrazierBlock.NORTH_CONNECTED, true));
        assertBounds(helper, cornerAlone, new AABB(3.5, 0.5, 0, 12.5, 14, 16), "a north-facing wall brazier joined only on its front");

        BlockPos cornerWest = new BlockPos(3, 1, 4);
        helper.setBlock(cornerWest, wallBrazier(Direction.NORTH).setValue(BrazierBlock.NORTH_CONNECTED, true)
                .setValue(BrazierBlock.WEST_CONNECTED, true));
        assertBounds(helper, cornerWest, new AABB(0, 0.5, 0, 12.5, 14, 16), "an inside corner facing north, joined front and west");

        BlockPos cornerEastFacing = new BlockPos(5, 1, 4);
        helper.setBlock(cornerEastFacing, wallBrazier(Direction.EAST).setValue(BrazierBlock.EAST_CONNECTED, true)
                .setValue(BrazierBlock.SOUTH_CONNECTED, true));
        assertBounds(helper, cornerEastFacing, new AABB(0, 0.5, 3.5, 16, 14, 16), "an inside corner facing east, joined front and south");

        // Outside corner (art plan, Outside corners addendum): the same 8 px junction with no bracket, reaching
        // behind it to the block edge and to the edge on its joined side; the rim of its closed side at 12.5 px
        BlockPos outside = new BlockPos(7, 1, 4);
        helper.setBlock(outside, wallBrazier(Direction.NORTH).setValue(BrazierBlock.SOUTH_CONNECTED, true)
                .setValue(BrazierBlock.WEST_CONNECTED, true));
        assertBounds(helper, outside, new AABB(0, 4, 3.5, 12.5, 13, 16), "an outside corner facing north, joined behind and west");

        // Hanging: a 10 px basket with its rim
        BlockPos hanging = new BlockPos(11, 1, 1);
        helper.setBlock(hanging, floorBrazier().setValue(BrazierBlock.PLACEMENT, BrazierPlacement.CEILING));
        assertBounds(helper, hanging, new AABB(2.5, 1, 2.5, 13.5, 12, 13.5), "a hanging brazier");
        helper.succeed();
    }

    @GameTest
    public void foodSitsOnEachGrate(GameTestHelper helper) {
        // Food floats 3/16 px above a grate, as vanilla's does above a campfire's 7 px top.
        // Floor: four 6 px items fill the quarters of the 12 px grate, whose top is 13 px.
        assertFood(helper, floorBrazier(), new FoodLayout(0.5f, 13.1875f / 16, 0.5f, 3f / 16, 0.375f), "floor");
        // Wall: 4 px items on the 12 x 8 grate (top 13 px), which the bracket holds in the middle of the block.
        assertFood(helper, wallBrazier(Direction.NORTH), new FoodLayout(0.5f, 13.1875f / 16, 0.5f, 2f / 16, 0.25f), "wall facing north");
        assertFood(helper, wallBrazier(Direction.EAST), new FoodLayout(0.5f, 13.1875f / 16, 0.5f, 2f / 16, 0.25f), "wall facing east");
        // An inside corner's 8 px junction is the middle of the block, where the four items sit
        assertFood(helper, wallBrazier(Direction.NORTH).setValue(BrazierBlock.NORTH_CONNECTED, true).setValue(BrazierBlock.WEST_CONNECTED, true),
                new FoodLayout(0.5f, 13.1875f / 16, 0.5f, 2f / 16, 0.25f), "inside corner");
        // Hanging: 4 px items on the 10 px grate, whose top is 12 px.
        assertFood(helper, floorBrazier().setValue(BrazierBlock.PLACEMENT, BrazierPlacement.CEILING),
                new FoodLayout(0.5f, 12.1875f / 16, 0.5f, 2f / 16, 0.25f), "hanging");
        helper.succeed();
    }

    private static void assertFood(GameTestHelper helper, BlockState state, FoodLayout expected, String what) {
        FoodLayout actual = BrazierBlock.foodLayout(state);
        helper.assertTrue(actual.equals(expected), what + " food laid out as " + actual + ", expected " + expected);
    }

    private static BlockState wallBrazier(Direction facing) {
        return floorBrazier().setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL).setValue(CampfireBlock.FACING, facing)
                .setValue(BrazierBlock.CLOSED, true);
    }

    private static BlockState floorBrazier() {
        return ModBlocks.BRAZIERS.get(COPPER).defaultBlockState();
    }

    private static void assertBounds(GameTestHelper helper, BlockPos pos, AABB pixels, String what) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockState state = helper.getLevel().getBlockState(absolute);
        AABB expected = new AABB(pixels.minX / 16, pixels.minY / 16, pixels.minZ / 16, pixels.maxX / 16, pixels.maxY / 16, pixels.maxZ / 16);
        AABB outline = state.getShape(helper.getLevel(), absolute).bounds();
        AABB collision = state.getCollisionShape(helper.getLevel(), absolute).bounds();
        helper.assertTrue(outline.equals(expected), what + ": outline " + outline + ", expected " + expected);
        helper.assertTrue(collision.equals(expected), what + ": collision " + collision + ", expected " + expected);
    }
}
