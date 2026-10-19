package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.McBraziers;
import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Behaviors slice 2, cooking speed by tier ({@code docs/plans/behaviors/behaviors-build-plan.md}). */
public class CookingSpeedGameTests {

    /** Raw beef takes 600 ticks on a vanilla campfire. */
    private static final int CAMPFIRE_TICKS = 600;

    /**
     * Expected beef cooking ticks per tier, written out from the PRD's speeds (1.0 / 1.25 / 1.5 / 2.0).
     * Deliberately not computed from {@link BrazierTier#cookingSpeed()}: the test must not trust the
     * values it checks.
     */
    private static final Map<BrazierTier, Long> PRD_TICKS = Map.of(
            BrazierTier.COPPER, 600L,
            BrazierTier.IRON, 480L,
            BrazierTier.DIAMOND, 400L,
            BrazierTier.OBSIDIAN, 400L,
            BrazierTier.PRISMARINE, 400L,
            BrazierTier.NETHERITE, 300L);

    @GameTest(maxTicks = 700)
    public void eachTierCooksAtItsSpeed(GameTestHelper helper) {
        // Expected cooking time per position: one closed, lit floor brazier per tier, plus a vanilla campfire
        Map<BlockPos, Long> expected = new LinkedHashMap<>();
        Map<BlockPos, String> names = new LinkedHashMap<>();
        int x = 0;
        for (BrazierTier tier : BrazierTier.values()) {
            BlockPos pos = new BlockPos(x++, 1, 1);
            BlockState state = ModBlocks.BRAZIERS.get(new BrazierVariant(tier, "")).defaultBlockState()
                    .setValue(CampfireBlock.LIT, true).setValue(BrazierBlock.CLOSED, true);
            helper.setBlock(pos, state);
            expected.put(pos, PRD_TICKS.get(tier));
            names.put(pos, tier.id());
        }
        // PRD Content addendum: crying obsidian cooks at obsidian's speed, 1.5x
        BlockPos crying = new BlockPos(x++, 1, 1);
        helper.setBlock(crying, ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.OBSIDIAN, "crying_")).defaultBlockState()
                .setValue(CampfireBlock.LIT, true).setValue(BrazierBlock.CLOSED, true));
        expected.put(crying, 400L);
        names.put(crying, "crying obsidian");
        BlockPos campfire = new BlockPos(x, 1, 1);
        helper.setBlock(campfire, Blocks.CAMPFIRE.defaultBlockState());
        expected.put(campfire, (long) CAMPFIRE_TICKS);
        names.put(campfire, "vanilla campfire");

        long start = helper.getTick();
        for (BlockPos pos : expected.keySet()) {
            helper.getBlockEntity(pos, CampfireBlockEntity.class).placeFood(helper.getLevel(), null, new ItemStack(Items.BEEF));
        }

        // Record the tick each slot empties (the beef is done and pops off)
        Map<BlockPos, Long> doneAfter = new LinkedHashMap<>();
        helper.onEachTick(() -> {
            for (BlockPos pos : expected.keySet()) {
                boolean empty = helper.getBlockEntity(pos, CampfireBlockEntity.class).getItems().stream().allMatch(ItemStack::isEmpty);
                if (empty && !doneAfter.containsKey(pos)) {
                    doneAfter.put(pos, helper.getTick() - start);
                }
            }
        });

        helper.succeedWhen(() -> {
            helper.assertTrue(doneAfter.size() == expected.size(), "still cooking: " + doneAfter.size() + " of " + expected.size() + " done");
            List<String> report = new ArrayList<>();
            // Measured against the vanilla campfire, so the harness's own tick offset cancels out
            long offset = doneAfter.get(campfire) - CAMPFIRE_TICKS;
            for (BlockPos pos : expected.keySet()) {
                long actual = doneAfter.get(pos) - offset;
                report.add(names.get(pos) + "=" + actual + " (expected " + expected.get(pos) + ")");
                helper.assertTrue(Math.abs(actual - expected.get(pos)) <= 1,
                        names.get(pos) + " cooked in " + actual + " ticks, expected " + expected.get(pos));
            }
            McBraziers.LOGGER.info("Cooking times: {} (harness offset {})", String.join(", ", report), offset);
        });
    }
}
