package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.block.WeatheringCopperBrazierBlock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Behaviors slice 5, copper aging ({@code docs/plans/behaviors/behaviors-build-plan.md}). The owner asked
 * for vanilla's values, so the reference is vanilla's own copper blocks, not numbers copied into the test.
 */
public class CopperAgingGameTests {

    private static Block copper(String agingPrefix) {
        return ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.COPPER, agingPrefix));
    }

    @GameTest
    public void agingUsesVanillaCopperValues(GameTestHelper helper) {
        // Vanilla copper blocks per stage are the reference for the aging chance
        Map<String, Block> vanillaByStage = new LinkedHashMap<>();
        vanillaByStage.put("", Blocks.COPPER_BLOCK.weathering().unaffected());
        vanillaByStage.put("exposed_", Blocks.COPPER_BLOCK.weathering().exposed());
        vanillaByStage.put("weathered_", Blocks.COPPER_BLOCK.weathering().weathered());
        vanillaByStage.put("oxidized_", Blocks.COPPER_BLOCK.weathering().oxidized());
        List<String> problems = new ArrayList<>();
        vanillaByStage.forEach((stage, vanilla) -> {
            WeatheringCopper ours = (WeatheringCopper) copper(stage);
            WeatheringCopper reference = (WeatheringCopper) vanilla;
            if (ours.getAge() != reference.getAge()) {
                problems.add(stage + "copper brazier is stage " + ours.getAge() + ", vanilla " + reference.getAge());
            }
            if (ours.getChanceModifier() != reference.getChanceModifier()) {
                problems.add(stage + "copper brazier chance modifier " + ours.getChanceModifier() + ", vanilla " + reference.getChanceModifier());
            }
            boolean oursTicks = copper(stage).defaultBlockState().isRandomlyTicking();
            boolean vanillaTicks = vanilla.defaultBlockState().isRandomlyTicking();
            if (oursTicks != vanillaTicks) {
                problems.add(stage + "copper brazier random ticking " + oursTicks + ", vanilla " + vanillaTicks);
            }
        });

        // PRD: unaffected → exposed → weathered → oxidized; waxed braziers and other tiers never age
        Map<String, String> next = new LinkedHashMap<>();
        next.put("", "exposed_");
        next.put("exposed_", "weathered_");
        next.put("weathered_", "oxidized_");
        next.forEach((from, to) -> {
            if (WeatheringCopper.getNext(copper(from)).orElse(null) != copper(to)) {
                problems.add(from + "copper brazier should age into " + to + "copper brazier");
            }
        });
        for (String waxed : List.of("waxed_", "waxed_exposed_", "waxed_weathered_", "waxed_oxidized_", "oxidized_")) {
            if (WeatheringCopper.getNext(copper(waxed)).isPresent() || copper(waxed).defaultBlockState().isRandomlyTicking()) {
                problems.add(waxed + "copper brazier must not age");
            }
        }
        if (ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.IRON, "")).defaultBlockState().isRandomlyTicking()) {
            problems.add("iron brazier must not age");
        }
        helper.assertTrue(problems.isEmpty(), String.join("; ", problems));
        helper.succeed();
    }

    @GameTest
    public void agingKeepsEveryPropertyAndTheFood(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState before = copper("").defaultBlockState()
                .setValue(BrazierBlock.FIRE, BrazierFire.SOUL).setValue(CampfireBlock.LIT, true).setValue(BrazierBlock.CLOSED, true);
        helper.setBlock(pos, before);
        helper.getBlockEntity(pos, CampfireBlockEntity.class).placeFood(helper.getLevel(), null, new ItemStack(Items.BEEF));

        // Vanilla's own aging roll, repeated with a fixed seed until it ages
        RandomSource random = RandomSource.create(42);
        BlockPos absolute = helper.absolutePos(pos);
        for (int roll = 0; roll < 100_000 && helper.getBlockState(pos).getBlock() == copper(""); roll++) {
            ((WeatheringCopperBrazierBlock) copper("")).changeOverTime(helper.getBlockState(pos), helper.getLevel(), absolute, random);
        }

        BlockState after = helper.getBlockState(pos);
        helper.assertTrue(after.getBlock() == copper("exposed_"), "the copper brazier should have aged into exposed, got " + after.getBlock());
        helper.assertTrue(after.getValue(BrazierBlock.FIRE) == BrazierFire.SOUL, "fire type should carry over");
        helper.assertTrue(after.getValue(CampfireBlock.LIT), "lit should carry over");
        helper.assertTrue(after.getValue(BrazierBlock.CLOSED), "the closed grate should carry over");
        boolean foodKept = helper.getBlockEntity(pos, CampfireBlockEntity.class).getItems().stream().anyMatch(s -> s.is(Items.BEEF));
        helper.assertTrue(foodKept, "the food should keep cooking through aging (decision 5)");
        helper.succeed();
    }

    @GameTest
    public void honeycombWaxesAndAxesScrape(GameTestHelper helper) {
        BlockPos unwaxed = new BlockPos(1, 1, 1);
        BlockPos waxedExposed = new BlockPos(3, 1, 1);
        BlockPos exposed = new BlockPos(5, 1, 1);
        helper.setBlock(unwaxed, copper("").defaultBlockState().setValue(BrazierBlock.FIRE, BrazierFire.COPPER));
        helper.setBlock(waxedExposed, copper("waxed_exposed_").defaultBlockState());
        helper.setBlock(exposed, copper("exposed_").defaultBlockState());

        helper.useBlock(unwaxed, holding(helper, Items.HONEYCOMB));
        helper.useBlock(waxedExposed, holding(helper, Items.IRON_AXE));
        helper.useBlock(exposed, holding(helper, Items.IRON_AXE));

        helper.assertTrue(helper.getBlockState(unwaxed).getBlock() == copper("waxed_"), "honeycomb should wax the copper brazier");
        helper.assertTrue(helper.getBlockState(unwaxed).getValue(BrazierBlock.FIRE) == BrazierFire.COPPER, "waxing keeps the fire type");
        helper.assertTrue(helper.getBlockState(waxedExposed).getBlock() == copper("exposed_"), "an axe should take the wax off");
        helper.assertTrue(helper.getBlockState(exposed).getBlock() == copper(""), "an axe should scrape exposed back to unaffected");
        helper.succeed();
    }

    private static Player holding(GameTestHelper helper, net.minecraft.world.item.Item item) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        return player;
    }
}
