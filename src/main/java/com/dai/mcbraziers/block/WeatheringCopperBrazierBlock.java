package com.dai.mcbraziers.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An unwaxed copper brazier that ages like vanilla copper (PRD: Variants). It implements vanilla's
 * {@link WeatheringCopper}, so aging runs vanilla's own process and values: the per-random-tick roll,
 * the 0.75 slowdown for unaffected copper, and the influence of nearby copper. The stage order and wax
 * pairs are registered with Fabric's {@code OxidizableBlocksRegistry}, which also lets honeycomb wax
 * and axes scrape it in the world. Aging is cosmetic: the brazier keeps every other property.
 */
public class WeatheringCopperBrazierBlock extends BrazierBlock implements WeatheringCopper {

    private final WeatheringCopper.WeatherState weatherState;

    public WeatheringCopperBrazierBlock(WeatheringCopper.WeatherState weatherState, Properties properties) {
        super(BrazierTier.COPPER, properties);
        this.weatherState = weatherState;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        this.changeOverTime(state, level, pos, random);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    @Override
    public WeatheringCopper.WeatherState getAge() {
        return this.weatherState;
    }
}
