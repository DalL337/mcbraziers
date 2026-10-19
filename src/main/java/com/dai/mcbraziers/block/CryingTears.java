package com.dai.mcbraziers.block;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Crying obsidian's tears on the mod's crying blocks (PRD Content addendum): vanilla's dripping tear
 * particle, the one the crying obsidian block uses, so it beads up, hangs, falls and splashes the same way.
 * Vanilla's block is a full cube; these are bars and cages, so a tear beads on a face of the block's
 * outline instead.
 */
public final class CryingTears {

    // How far outside a face a tear beads; vanilla's block uses 0.1 on a full cube, bars are thinner
    private static final double OFF_FACE = 1.0 / 16.0;

    private CryingTears() {
    }

    /** One animate tick of tears, at vanilla crying obsidian's rate: a try in 5, on any face but the top. */
    public static void drip(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        Direction face = Direction.getRandom(random);
        if (face == Direction.UP) return;

        List<AABB> boxes = state.getShape(level, pos).toAabbs();
        if (boxes.isEmpty()) return;
        AABB box = boxes.get(random.nextInt(boxes.size()));
        double x = onFace(box.minX, box.maxX, face.getStepX(), random);
        double y = onFace(box.minY, box.maxY, face.getStepY(), random);
        double z = onFace(box.minZ, box.maxZ, face.getStepZ(), random);

        // A face on the block's edge may meet the floor, a wall or joined bars; a tear there would sit inside
        // them, so it only beads where that neighbour is empty, such as under bars hanging in the air
        x += pos.getX();
        y += pos.getY();
        z += pos.getZ();
        BlockPos at = BlockPos.containing(x, y, z);
        if (!at.equals(pos) && !level.getBlockState(at).getShape(level, at).isEmpty()) return;
        level.addParticle(ParticleTypes.DRIPPING_OBSIDIAN_TEAR, x, y, z, 0.0, 0.0, 0.0);
    }

    private static double onFace(double min, double max, int step, RandomSource random) {
        if (step == 0) return Mth.lerp(random.nextDouble(), min, max);
        return step > 0 ? max + OFF_FACE : min - OFF_FACE;
    }
}
