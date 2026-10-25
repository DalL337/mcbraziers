package com.dai.mcbraziers.item;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A brazier item for one fire type: it places its block with that fire. */
public class BrazierItem extends BlockItem {

    private final BrazierFire fire;

    public BrazierItem(Block block, BrazierFire fire, Item.Properties properties) {
        super(block, properties);
        this.fire = fire;
    }

    public BrazierFire fire() {
        return this.fire;
    }

    /** At the outside edge of a wall run, the next brazier goes around the corner (or nowhere). */
    @Override
    public @Nullable BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        return BrazierBlock.aroundOutsideCorner(context);
    }

    @Override
    protected @Nullable BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(BrazierBlock.FIRE, this.fire);
    }
}
