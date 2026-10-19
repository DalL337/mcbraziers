package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierPlacement;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.command.BrazierCommand;
import com.dai.mcbraziers.item.ModItems;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Behaviors slice 1, controls and tops ({@code docs/plans/behaviors/behaviors-build-plan.md}). */
public class BrazierControlGameTests {

    private static final Block IRON_BRAZIER = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.IRON, ""));

    private static BlockState floorBrazier(boolean closed) {
        return IRON_BRAZIER.defaultBlockState().setValue(CampfireBlock.LIT, true).setValue(BrazierBlock.CLOSED, closed);
    }

    private static Player player(GameTestHelper helper, ItemStack inHand) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, inHand);
        return player;
    }

    private static List<ItemStack> food(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockEntity(pos, CampfireBlockEntity.class).getItems().stream().filter(s -> !s.isEmpty()).toList();
    }

    @GameTest
    public void floorBrazierIsPlacedOpenAndAClickTogglesTheGrate(GameTestHelper helper) {
        BlockPos ground = new BlockPos(1, 1, 1);
        BlockPos brazier = ground.above();
        helper.setBlock(ground, Blocks.STONE);
        Player player = player(helper, ItemStack.EMPTY);
        helper.placeAt(player, new ItemStack(ModItems.brazierItem(new BrazierVariant(BrazierTier.IRON, ""), BrazierFire.SOUL)), ground, Direction.UP);
        helper.assertBlockProperty(brazier, BrazierBlock.CLOSED, false);
        helper.assertBlockProperty(brazier, BrazierBlock.PLACEMENT, BrazierPlacement.FLOOR);
        helper.assertBlockProperty(brazier, BrazierBlock.FIRE, BrazierFire.SOUL);
        helper.useBlock(brazier, player);
        helper.assertBlockProperty(brazier, BrazierBlock.CLOSED, true);
        helper.useBlock(brazier, player);
        helper.assertBlockProperty(brazier, BrazierBlock.CLOSED, false);
        helper.succeed();
    }

    @GameTest
    public void grateRefusesToOpenWhileFoodIsCooking(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, floorBrazier(true));
        helper.getBlockEntity(pos, CampfireBlockEntity.class).placeFood(helper.getLevel(), null, new ItemStack(Items.BEEF));
        helper.useBlock(pos, player(helper, ItemStack.EMPTY));
        helper.assertBlockProperty(pos, BrazierBlock.CLOSED, true);
        helper.assertTrue(food(helper, pos).size() == 1, "the food should still be cooking");
        helper.succeed();
    }

    @GameTest
    public void foodIsRefusedOnAnOpenTopAndPlacedOnAClosedOne(GameTestHelper helper) {
        BlockPos open = new BlockPos(1, 1, 1);
        BlockPos closed = new BlockPos(3, 1, 1);
        helper.setBlock(open, floorBrazier(false));
        helper.setBlock(closed, floorBrazier(true));
        Player player = player(helper, new ItemStack(Items.BEEF, 2));
        helper.useBlock(open, player);
        helper.assertTrue(food(helper, open).isEmpty(), "an open brazier must not take food");
        helper.useBlock(closed, player);
        helper.assertTrue(food(helper, closed).size() == 1, "a closed brazier should take the food");
        helper.succeed();
    }

    @GameTest(maxTicks = 800)
    public void wallBrazierCooks(GameTestHelper helper) {
        BlockPos wall = new BlockPos(1, 1, 1);
        BlockPos pos = wall.south();
        helper.setBlock(wall, Blocks.STONE);
        helper.setBlock(pos, IRON_BRAZIER.defaultBlockState().setValue(CampfireBlock.LIT, true)
                .setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL).setValue(CampfireBlock.FACING, Direction.SOUTH)
                .setValue(BrazierBlock.CLOSED, true));
        helper.useBlock(pos, player(helper, new ItemStack(Items.BEEF)));
        helper.assertTrue(food(helper, pos).size() == 1, "a wall brazier should take food");
        // Raw beef takes 600 ticks on a campfire; when it is done the slot empties
        helper.succeedWhen(() -> helper.assertTrue(food(helper, pos).isEmpty(), "still cooking"));
    }

    @GameTest(maxTicks = 800)
    public void hangingBrazierCooks(GameTestHelper helper) {
        BlockPos ceiling = new BlockPos(1, 3, 1);
        BlockPos pos = ceiling.below();
        helper.setBlock(ceiling, Blocks.STONE);
        helper.setBlock(pos, IRON_BRAZIER.defaultBlockState().setValue(CampfireBlock.LIT, true)
                .setValue(BrazierBlock.PLACEMENT, BrazierPlacement.CEILING).setValue(BrazierBlock.CLOSED, true));
        helper.useBlock(pos, player(helper, new ItemStack(Items.BEEF)));
        helper.assertTrue(food(helper, pos).size() == 1, "a hanging brazier should take food");
        helper.succeedWhen(() -> helper.assertTrue(food(helper, pos).isEmpty(), "still cooking"));
    }

    @GameTest(maxTicks = 100)
    public void onlyAnOpenTopBurns(GameTestHelper helper) {
        BlockPos open = new BlockPos(1, 2, 1);
        BlockPos closed = new BlockPos(5, 2, 1);
        helper.setBlock(open.below(), Blocks.STONE);
        helper.setBlock(closed.below(), Blocks.STONE);
        helper.setBlock(open, floorBrazier(false));
        helper.setBlock(closed, floorBrazier(true));
        Pig burnt = helper.spawn(EntityTypes.PIG, open);
        Pig safe = helper.spawn(EntityTypes.PIG, closed);
        float fullHealth = safe.getMaxHealth();
        helper.runAfterDelay(60, () -> {
            helper.assertTrue(burnt.getHealth() < fullHealth, "a pig on an open brazier should be hurt");
            helper.assertTrue(safe.getHealth() == fullHealth, "a pig on a closed brazier should be unhurt");
            helper.succeed();
        });
    }

    @GameTest
    public void sneakClickTurnsOffEveryTierAndNeverLights(GameTestHelper helper) {
        Player player = player(helper, ItemStack.EMPTY);
        player.setShiftKeyDown(true);
        int i = 0;
        for (Block brazier : ModBlocks.BRAZIERS.values()) {
            BlockPos pos = new BlockPos(i % 7, 1, i / 7 * 2);
            i++;
            helper.setBlock(pos, brazier.defaultBlockState().setValue(CampfireBlock.LIT, true));
            helper.useBlock(pos, player);
            helper.assertBlockProperty(pos, CampfireBlock.LIT, false);
            helper.useBlock(pos, player);
            helper.assertBlockProperty(pos, CampfireBlock.LIT, false);
        }
        helper.succeed();
    }

    @GameTest
    public void commandTogglesAnyBrazier(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, floorBrazier(false));
        BlockPos absolute = helper.absolutePos(pos);
        helper.assertTrue(BrazierCommand.toggle(helper.getLevel(), absolute) == BrazierCommand.ToggleResult.PUT_OUT, "first toggle turns it off");
        helper.assertBlockProperty(pos, CampfireBlock.LIT, false);
        helper.assertTrue(BrazierCommand.toggle(helper.getLevel(), absolute) == BrazierCommand.ToggleResult.LIT, "second toggle lights it");
        helper.assertBlockProperty(pos, CampfireBlock.LIT, true);
        helper.succeed();
    }
}
