package com.dai.mcbraziers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Crying obsidian bars: bars that cry like the crying obsidian block (PRD Content addendum). */
public class CryingObsidianBarsBlock extends IronBarsBlock {

    public CryingObsidianBarsBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        CryingTears.drip(state, level, pos, random);
    }
}
