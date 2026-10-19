package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.item.ModItems;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Behaviors slice 4, tier specials and mining ({@code docs/plans/behaviors/behaviors-build-plan.md}).
 * Expected outcomes are written out from the PRD's Tiers and Bars sections and decisions 2–3.
 */
public class SpecialsAndMiningGameTests {

    private static Block brazier(BrazierTier tier) {
        return ModBlocks.BRAZIERS.get(new BrazierVariant(tier, ""));
    }

    /** The crying obsidian brazier: obsidian's cosmetic variant, with obsidian's specials (PRD Content addendum). */
    private static Block cryingObsidian() {
        return ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.OBSIDIAN, "crying_"));
    }

    @GameTest
    public void obsidianAndNetheriteSurviveExplosions(GameTestHelper helper) {
        // PRD: obsidian (crying obsidian too) and netherite are explosion-proof; other tiers break
        Map<String, Block> braziers = new LinkedHashMap<>();
        braziers.put("iron", brazier(BrazierTier.IRON));
        braziers.put("obsidian", brazier(BrazierTier.OBSIDIAN));
        braziers.put("crying obsidian", cryingObsidian());
        braziers.put("netherite", brazier(BrazierTier.NETHERITE));
        Map<String, Boolean> survives = Map.of("iron", false, "obsidian", true, "crying obsidian", true, "netherite", true);
        int x = 1;
        Map<String, BlockPos> positions = new LinkedHashMap<>();
        for (Map.Entry<String, Block> entry : braziers.entrySet()) {
            BlockPos pos = new BlockPos(x, 1, 3);
            x += 2;
            helper.setBlock(pos, entry.getValue().defaultBlockState().setValue(CampfireBlock.LIT, true));
            positions.put(entry.getKey(), pos);
        }
        positions.forEach((name, pos) -> {
            Vec3 blast = helper.absoluteVec(Vec3.atCenterOf(pos.north()));
            helper.getLevel().explode(null, blast.x, blast.y, blast.z, 4.0f, Level.ExplosionInteraction.TNT);
        });
        braziers.forEach((name, block) -> helper.assertTrue(
                (helper.getBlockState(positions.get(name)).getBlock() == block) == survives.get(name),
                name + " brazier should " + (survives.get(name) ? "survive" : "break in") + " a TNT-sized explosion"));
        helper.succeed();
    }

    @GameTest(maxTicks = 40)
    public void pistonsCantPushExplosionProofBraziers(GameTestHelper helper) {
        // PRD: obsidian and netherite can't be pushed by pistons. Vanilla also refuses any block with a block
        // entity, so this checks the PRD outcome but can't catch a regression in the explicit push reaction.
        List<BlockPos> braziers = new ArrayList<>();
        int z = 1;
        for (Block brazier : List.of(brazier(BrazierTier.OBSIDIAN), cryingObsidian(), brazier(BrazierTier.NETHERITE))) {
            BlockPos piston = new BlockPos(1, 1, z);
            BlockPos pos = piston.east();
            helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(PistonBaseBlock.FACING, Direction.EAST));
            helper.setBlock(pos, brazier.defaultBlockState());
            helper.setBlock(piston.west(), Blocks.REDSTONE_BLOCK);
            braziers.add(pos);
            z += 3;
        }
        helper.runAfterDelay(10, () -> {
            for (BlockPos pos : braziers) {
                helper.assertTrue(helper.getBlockState(pos).getBlock() instanceof BrazierBlock, "the brazier at " + pos + " was pushed");
                helper.assertTrue(helper.getBlockState(pos.east()).isAir(), "something moved past the brazier at " + pos);
            }
            helper.succeed();
        });
    }

    @GameTest
    public void miningToolsFollowThePrd(GameTestHelper helper) {
        // PRD Tiers/Bars and decision 2: copper with any pickaxe (wood too); everything else stone or better;
        // bare hands and the wrong tool drop nothing. The mod's campfires drop with anything, like vanilla's.
        // Crying obsidian mines like obsidian (PRD Content addendum).
        Map<String, Block> blocks = new LinkedHashMap<>();
        for (BrazierTier tier : BrazierTier.values()) {
            blocks.put(tier.id() + " brazier", brazier(tier));
        }
        blocks.put("weathered copper brazier", ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.COPPER, "weathered_")));
        blocks.put("crying obsidian brazier", cryingObsidian());
        blocks.put("diamond bars", ModBlocks.DIAMOND_BARS);
        blocks.put("obsidian bars", ModBlocks.OBSIDIAN_BARS);
        blocks.put("crying obsidian bars", ModBlocks.CRYING_OBSIDIAN_BARS);
        blocks.put("prismarine bars", ModBlocks.PRISMARINE_BARS);
        blocks.put("netherite bars", ModBlocks.NETHERITE_BARS);
        blocks.put("copper campfire", ModBlocks.COPPER_CAMPFIRE);
        blocks.put("amethyst campfire", ModBlocks.AMETHYST_CAMPFIRE);

        // Expected drops per tool: [bare hand, wooden pickaxe, stone pickaxe, wooden axe]
        Map<String, boolean[]> expected = new LinkedHashMap<>();
        boolean[] anyPickaxe = {false, true, true, false};
        boolean[] stoneOrBetter = {false, false, true, false};
        boolean[] anything = {true, true, true, true};
        expected.put("copper brazier", anyPickaxe);
        expected.put("weathered copper brazier", anyPickaxe);
        for (String name : List.of("iron brazier", "diamond brazier", "obsidian brazier", "crying obsidian brazier", "prismarine brazier",
                "netherite brazier", "diamond bars", "obsidian bars", "crying obsidian bars", "prismarine bars", "netherite bars")) {
            expected.put(name, stoneOrBetter);
        }
        expected.put("copper campfire", anything);
        expected.put("amethyst campfire", anything);

        Item[] tools = {Items.AIR, Items.WOODEN_PICKAXE, Items.STONE_PICKAXE, Items.WOODEN_AXE};
        String[] toolNames = {"bare hand", "wooden pickaxe", "stone pickaxe", "wooden axe"};
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<String, Block> entry : blocks.entrySet()) {
            BlockState state = entry.getValue().defaultBlockState();
            for (int i = 0; i < tools.length; i++) {
                Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(tools[i]));
                boolean drops = player.hasCorrectToolForDrops(state);
                if (drops != expected.get(entry.getKey())[i]) {
                    wrong.add(entry.getKey() + " with " + toolNames[i] + ": drops=" + drops);
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "mining rules differ from the PRD: " + wrong);
        helper.succeed();
    }

    @GameTest
    public void aHarvestedBrazierDropsItsOwnFireType(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 1, 1);
        BlockState state = brazier(BrazierTier.IRON).defaultBlockState().setValue(BrazierBlock.FIRE, BrazierFire.SOUL);
        helper.setBlock(pos, state);
        List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(pos), null, null, new ItemStack(Items.STONE_PICKAXE));
        Item soulIron = ModItems.brazierItem(new BrazierVariant(BrazierTier.IRON, ""), BrazierFire.SOUL);
        helper.assertTrue(drops.size() == 1 && drops.getFirst().is(soulIron), "expected one soul iron brazier, got " + drops);
        helper.succeed();
    }
}
