package com.dai.mcbraziers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The crying obsidian brazier: a cosmetic variant of the obsidian tier, like copper's aging stages (PRD
 * Content addendum). It keeps obsidian's special, cooking speed and mining, and cries, lit or not.
 */
public class CryingObsidianBrazierBlock extends BrazierBlock {

    public CryingObsidianBrazierBlock(Properties properties) {
        super(BrazierTier.OBSIDIAN, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        CryingTears.drip(state, level, pos, random);
    }
}
