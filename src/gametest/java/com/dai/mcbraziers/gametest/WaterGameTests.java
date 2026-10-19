package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.command.BrazierCommand;
import com.dai.mcbraziers.item.ModItems;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Behaviors slice 3, water and water-proof tiers ({@code docs/plans/behaviors/behaviors-build-plan.md}).
 * Expected outcomes are the PRD's water and lighting tables written out per tier, for iron (a plain
 * tier), prismarine and netherite.
 */
public class WaterGameTests {

    /** PRD water table: does this tier stay lit when water arrives? */
    private static final Map<BrazierTier, Boolean> STAYS_LIT_IN_WATER = Map.of(
            BrazierTier.IRON, false,
            BrazierTier.PRISMARINE, true,
            BrazierTier.NETHERITE, true);
    /** One column per tier inside the 8×8×8 test area. */
    private static final Map<BrazierTier, Integer> COLUMN = Map.of(
            BrazierTier.IRON, 1,
            BrazierTier.PRISMARINE, 4,
            BrazierTier.NETHERITE, 7);
    private static final int ROW = 3;

    private static final BlockState SOURCE = Blocks.WATER.defaultBlockState();
    /** Flowing water at its thinnest (level 7, amount 1). */
    private static final BlockState THINNEST_FLOW = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 7);

    private static Block brazier(BrazierTier tier) {
        return ModBlocks.BRAZIERS.get(new BrazierVariant(tier, ""));
    }

    private static BlockPos at(BrazierTier tier) {
        return new BlockPos(COLUMN.get(tier), 1, ROW);
    }

    /** A lit floor brazier standing on stone. */
    private static void placeLit(GameTestHelper helper, BrazierTier tier) {
        helper.setBlock(at(tier).below(), Blocks.STONE);
        helper.setBlock(at(tier), brazier(tier).defaultBlockState().setValue(CampfireBlock.LIT, true));
    }

    /** Water at {@code pos}, walled in with stone on every side except toward the brazier, so it can't spread. */
    private static void containedWater(GameTestHelper helper, BlockPos pos, BlockState water, Direction towardBrazier) {
        for (Direction side : Direction.values()) {
            if (side != towardBrazier && helper.getBlockState(pos.relative(side)).isAir()) {
                helper.setBlock(pos.relative(side), Blocks.STONE);
            }
        }
        helper.setBlock(pos, water);
    }

    private static void assertLitPerPrd(GameTestHelper helper, String situation) {
        STAYS_LIT_IN_WATER.forEach((tier, staysLit) -> helper.assertTrue(
                helper.getBlockState(at(tier)).getValue(CampfireBlock.LIT) == staysLit,
                situation + ": " + tier.id() + " should be " + (staysLit ? "lit" : "out")));
    }

    private static Player holding(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    @GameTest(maxTicks = 40)
    public void waterAbovePutsOutPlainTiersOnly(GameTestHelper helper) {
        for (BrazierTier tier : STAYS_LIT_IN_WATER.keySet()) {
            placeLit(helper, tier);
            containedWater(helper, at(tier).above(), SOURCE, Direction.DOWN);
        }
        helper.runAfterDelay(5, () -> {
            assertLitPerPrd(helper, "water above");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void waterOnTwoSidesPutsOutPlainTiersOnly(GameTestHelper helper) {
        for (BrazierTier tier : STAYS_LIT_IN_WATER.keySet()) {
            placeLit(helper, tier);
            containedWater(helper, at(tier).north(), THINNEST_FLOW, Direction.SOUTH);
            containedWater(helper, at(tier).south(), THINNEST_FLOW, Direction.NORTH);
        }
        helper.runAfterDelay(3, () -> {
            assertLitPerPrd(helper, "thinnest water on two sides");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void deepWaterOnOneSidePutsOutPlainTiersOnly(GameTestHelper helper) {
        for (BrazierTier tier : STAYS_LIT_IN_WATER.keySet()) {
            placeLit(helper, tier);
            containedWater(helper, at(tier).north(), SOURCE, Direction.SOUTH);
        }
        helper.runAfterDelay(5, () -> {
            assertLitPerPrd(helper, "a water source on one side");
            helper.succeed();
        });
    }

    @GameTest(maxTicks = 40)
    public void thinnestFlowOnOneSideIsSafe(GameTestHelper helper) {
        placeLit(helper, BrazierTier.IRON);
        containedWater(helper, at(BrazierTier.IRON).north(), THINNEST_FLOW, Direction.SOUTH);
        helper.runAfterDelay(3, () -> {
            helper.assertBlockProperty(at(BrazierTier.IRON), CampfireBlock.LIT, true);
            helper.succeed();
        });
    }

    @GameTest
    public void placedInWater(GameTestHelper helper) {
        Player player = holding(helper, ItemStack.EMPTY);
        for (BrazierTier tier : STAYS_LIT_IN_WATER.keySet()) {
            BlockPos ground = at(tier);
            helper.setBlock(ground, Blocks.STONE);
            containedWater(helper, ground.above(), SOURCE, Direction.DOWN);
            helper.placeAt(player, new ItemStack(ModItems.brazierItem(new BrazierVariant(tier, ""), BrazierFire.NORMAL)), ground, Direction.UP);
            BlockState placed = helper.getBlockState(ground.above());
            helper.assertTrue(placed.getBlock() == brazier(tier), tier.id() + " brazier should be placed in the water");
            helper.assertTrue(placed.getValue(CampfireBlock.WATERLOGGED), tier.id() + " should be waterlogged");
            helper.assertTrue(placed.getValue(CampfireBlock.LIT) == STAYS_LIT_IN_WATER.get(tier),
                    tier.id() + " placed in water should be " + (STAYS_LIT_IN_WATER.get(tier) ? "lit" : "out"));
        }
        helper.succeed();
    }

    @GameTest
    public void lightingFollowsTheWaterRules(GameTestHelper helper) {
        BlockState wetIron = brazier(BrazierTier.IRON).defaultBlockState().setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.WATERLOGGED, true);
        BlockState wetPrismarine = brazier(BrazierTier.PRISMARINE).defaultBlockState().setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.WATERLOGGED, true);
        BlockState wetNetherite = brazier(BrazierTier.NETHERITE).defaultBlockState().setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.WATERLOGGED, true);
        BlockState dryIron = brazier(BrazierTier.IRON).defaultBlockState().setValue(CampfireBlock.LIT, false);

        // PRD: flint and steel lights prismarine even underwater; netherite and plain tiers can't be lit underwater
        BlockPos ironUnderwater = new BlockPos(0, 1, 0);
        BlockPos prismarineUnderwater = new BlockPos(2, 1, 0);
        BlockPos netheriteUnderwater = new BlockPos(4, 1, 0);
        BlockPos ironUnderWaterAbove = new BlockPos(6, 1, 0);
        BlockPos ironDry = new BlockPos(0, 1, 4);
        BlockPos prismarineByFireCharge = new BlockPos(2, 1, 4);
        helper.setBlock(ironUnderwater, wetIron);
        helper.setBlock(prismarineUnderwater, wetPrismarine);
        helper.setBlock(netheriteUnderwater, wetNetherite);
        helper.setBlock(ironUnderWaterAbove, dryIron);
        containedWater(helper, ironUnderWaterAbove.above(), SOURCE, Direction.DOWN);
        helper.setBlock(ironDry, dryIron);
        helper.setBlock(prismarineByFireCharge, wetPrismarine);

        for (BlockPos pos : new BlockPos[] {ironUnderwater, prismarineUnderwater, netheriteUnderwater, ironUnderWaterAbove, ironDry}) {
            helper.useBlock(pos, holding(helper, new ItemStack(Items.FLINT_AND_STEEL)));
        }
        helper.useBlock(prismarineByFireCharge, holding(helper, new ItemStack(Items.FIRE_CHARGE)));

        helper.assertBlockProperty(ironUnderwater, CampfireBlock.LIT, false);
        helper.assertBlockProperty(prismarineUnderwater, CampfireBlock.LIT, true);
        helper.assertBlockProperty(netheriteUnderwater, CampfireBlock.LIT, false);
        helper.assertBlockProperty(ironUnderWaterAbove, CampfireBlock.LIT, false);
        helper.assertBlockProperty(ironDry, CampfireBlock.LIT, true);
        helper.assertBlockProperty(prismarineByFireCharge, CampfireBlock.LIT, true);

        // The command follows the same rules
        helper.assertTrue(BrazierCommand.toggle(helper.getLevel(), helper.absolutePos(ironUnderwater)) == BrazierCommand.ToggleResult.CANT_LIGHT_HERE,
                "the command must not light an iron brazier underwater");
        helper.succeed();
    }

    @GameTest
    public void shovelsDontWorkOnNetherite(GameTestHelper helper) {
        for (BrazierTier tier : STAYS_LIT_IN_WATER.keySet()) {
            placeLit(helper, tier);
            helper.useBlock(at(tier), holding(helper, new ItemStack(Items.IRON_SHOVEL)));
        }
        // PRD: shovels put out every tier except netherite
        helper.assertBlockProperty(at(BrazierTier.IRON), CampfireBlock.LIT, false);
        helper.assertBlockProperty(at(BrazierTier.PRISMARINE), CampfireBlock.LIT, false);
        helper.assertBlockProperty(at(BrazierTier.NETHERITE), CampfireBlock.LIT, true);
        helper.succeed();
    }

    @GameTest(maxTicks = 60)
    public void splashWaterSkipsWaterProofTiers(GameTestHelper helper) {
        for (BrazierTier tier : STAYS_LIT_IN_WATER.keySet()) {
            placeLit(helper, tier);
            Vec3 above = helper.absoluteVec(Vec3.atBottomCenterOf(at(tier).above(2)));
            ThrownSplashPotion potion = new ThrownSplashPotion(helper.getLevel(), above.x, above.y, above.z,
                    PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER));
            potion.setDeltaMovement(0, -0.5, 0);
            helper.getLevel().addFreshEntity(potion);
        }
        helper.runAfterDelay(30, () -> {
            assertLitPerPrd(helper, "splash water bottle");
            helper.succeed();
        });
    }
}
