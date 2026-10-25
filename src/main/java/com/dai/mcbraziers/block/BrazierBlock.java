package com.dai.mcbraziers.block;

import com.dai.mcbraziers.item.ModItems;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BrazierBlock extends CampfireBlock {

    public static final EnumProperty<BrazierPlacement> PLACEMENT =
            EnumProperty.create("placement", BrazierPlacement.class);
    /** Fire type, set when placed from a {@link com.dai.mcbraziers.item.BrazierItem}. */
    public static final EnumProperty<BrazierFire> FIRE = EnumProperty.create("fire", BrazierFire.class);
    /**
     * Grate over the fire. Floor braziers are placed open and toggle with an empty-hand click;
     * wall and hanging braziers are always closed. Only closed braziers cook (PRD: Placement, Cooking).
     */
    public static final BooleanProperty CLOSED = BooleanProperty.create("closed");
    /** Action-bar message when the grate can't open because food is cooking on it. */
    public static final String GRATE_HAS_FOOD = "block.mcbraziers.brazier.grate_has_food";
    public static final BooleanProperty NORTH_CONNECTED = BooleanProperty.create("north_connected");
    public static final BooleanProperty SOUTH_CONNECTED = BooleanProperty.create("south_connected");
    public static final BooleanProperty EAST_CONNECTED = BooleanProperty.create("east_connected");
    public static final BooleanProperty WEST_CONNECTED = BooleanProperty.create("west_connected");

    /** Floor cage, up to the top of its rim: the basin, slats and rim of the model. */
    private static final VoxelShape FLOOR_CAGE = Shapes.or(Block.box(2, 0, 2, 14, 13, 14), Block.box(1.5, 11, 1.5, 14.5, 13, 14.5));
    /** Where a connected side bridges to its neighbour, by side. */
    private static final Map<Direction, VoxelShape> FLOOR_BRIDGES = Shapes.rotateHorizontal(Block.box(1.5, 0, 0, 14.5, 13, 2));
    /** The corner filled when a side and the side clockwise of it both connect, by the first side. */
    private static final Map<Direction, VoxelShape> FLOOR_CORNERS = Shapes.rotateHorizontal(Block.box(14, 0, 0, 16, 13, 2));
    /** Floor shapes for every combination of connected sides. */
    private static final Map<Integer, VoxelShape> FLOOR_SHAPES = floorShapes();

    /** Hanging cage: the basket and its rim (models/block/brazier/ceiling_cage.json). */
    private static final VoxelShape CEILING_SHAPE = Shapes.or(Block.box(3, 1, 3, 13, 12, 13), Block.box(2.5, 10, 2.5, 13.5, 12, 13.5));
    /**
     * Wall cage by facing, then by which sides join the next wall brazier (west 1, east 2, as drawn facing
     * north): the basket and rim, and the bracket (plate, arm and brace), stretched to the block edge on a
     * joined side.
     */
    private static final Map<Direction, VoxelShape[]> WALL_SHAPES = wallShapes();

    /**
     * Where food is drawn on each brazier's grate (docs/plans/art/copper-brazier-art.md), in blocks. Food
     * floats 3/16 px above the grate, as vanilla's does above a campfire.
     */
    public static FoodLayout foodLayout(BlockState state) {
        return switch (state.getValue(PLACEMENT)) {
            // Four 6 px items fill the quarters of the 12 px grate, whose top is at 13 px
            case FLOOR -> new FoodLayout(0.5f, (13 + 0.1875f) / 16, 0.5f, 3f / 16, 0.375f);
            // 4 px items on the 12 x 8 grate (top at 13 px); the bracket holds the basket in the middle of the block
            case WALL -> new FoodLayout(0.5f, (13 + 0.1875f) / 16, 0.5f, 2f / 16, 0.25f);
            // 4 px items on the 10 px hanging grate, whose top is at 12 px
            case CEILING -> new FoodLayout(0.5f, (12 + 0.1875f) / 16, 0.5f, 2f / 16, 0.25f);
        };
    }

    private final BrazierTier tier;

    public BrazierBlock(BrazierTier tier, Properties properties) {
        super(true, 1, properties);
        this.tier = tier;
        this.registerDefaultState(this.defaultBlockState()
                .setValue(PLACEMENT, BrazierPlacement.FLOOR)
                .setValue(FIRE, BrazierFire.NORMAL)
                .setValue(CLOSED, false)
                .setValue(NORTH_CONNECTED, false)
                .setValue(SOUTH_CONNECTED, false)
                .setValue(EAST_CONNECTED, false)
                .setValue(WEST_CONNECTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PLACEMENT, FIRE, CLOSED, NORTH_CONNECTED, SOUTH_CONNECTED, EAST_CONNECTED, WEST_CONNECTED);
    }

    public BrazierTier tier() {
        return this.tier;
    }

    /**
     * Keeps the campfire block entity, and the food cooking on it, when one brazier block turns into
     * another: copper aging, waxing and axe scraping (behaviors plan, decision 5). Vanilla's copper chest
     * does the same for its contents.
     */
    @Override
    public boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.getBlock() instanceof BrazierBlock;
    }

    // --- Water (PRD: Water; behaviors plan, slice 3) ---

    /**
     * Whether water around {@code pos} puts out a brazier that isn't water-proof: water directly above
     * (falling or source), water on two or more sides, or deeper-than-thinnest water on any side.
     * The thinnest flowing water (amount 1) on a single side is safe.
     */
    public static boolean drenched(LevelReader level, BlockPos pos) {
        if (level.getFluidState(pos.above()).is(FluidTags.WATER)) {
            return true;
        }
        int wetSides = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            FluidState fluid = level.getFluidState(pos.relative(side));
            if (fluid.is(FluidTags.WATER)) {
                if (fluid.getAmount() > 1) {
                    return true;
                }
                wetSides++;
            }
        }
        return wetSides >= 2;
    }

    /** Every lighting path goes through this: items, burning projectiles, and {@code /brazier extinguish}. */
    public boolean canBeLit(BlockState state, LevelReader level, BlockPos pos) {
        if (this.tier.lightsUnderwater()) {
            return true;
        }
        if (state.getValue(WATERLOGGED)) {
            return false;
        }
        return this.tier.waterProof() || !drenched(level, pos);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state, FluidState fluid) {
        // Water-proof tiers take on water without going out
        if (this.tier.waterProof()) {
            if (!state.getValue(WATERLOGGED) && fluid.is(Fluids.WATER)) {
                level.setBlock(pos, state.setValue(WATERLOGGED, true), Block.UPDATE_ALL);
                level.scheduleTick(pos, fluid.getType(), fluid.getType().getTickDelay(level));
                return true;
            }
            return false;
        }
        return super.placeLiquid(level, pos, state, fluid);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // Scheduled from updateShape when water arrives; re-checked here because the water may have moved on
        if (state.getValue(LIT) && !this.tier.waterProof() && drenched(level, pos)) {
            level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
            CampfireBlock.douse(null, level, pos, state);
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hitResult, Projectile projectile) {
        // Burning projectiles light braziers under the same rules as flint and steel
        BlockPos pos = hitResult.getBlockPos();
        if (level instanceof ServerLevel serverLevel && projectile.isOnFire() && projectile.mayInteract(serverLevel, pos)
                && !state.getValue(LIT) && this.canBeLit(state, level, pos)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL_IMMEDIATE);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        // Lighting goes through the water rules instead of vanilla's waterlogged-only check
        if (!state.getValue(LIT) && (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE))) {
            if (!this.canBeLit(state, level, pos)) {
                return InteractionResult.CONSUME; // refused: also keeps flint and steel from lighting it
            }
            if (!level.isClientSide()) {
                boolean fireCharge = stack.is(Items.FIRE_CHARGE);
                level.playSound(null, pos, fireCharge ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS, 1.0f, level.getRandom().nextFloat() * 0.4f + 0.8f);
                level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL_IMMEDIATE);
                level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
                if (fireCharge) {
                    if (!player.hasInfiniteMaterials()) {
                        stack.shrink(1);
                    }
                } else {
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                }
            }
            return InteractionResult.SUCCESS;
        }
        // Netherite ignores shovels; consuming the click keeps the shovel from acting on it
        if (this.tier.ignoresShovels() && stack.is(ItemTags.DOUSES_CAMPFIRES)) {
            return InteractionResult.CONSUME;
        }
        // Food only goes on a closed top; on an open brazier the item is used normally (e.g. eaten)
        if (!state.getValue(CLOSED) && level.recipeAccess().propertySet(RecipePropertySet.CAMPFIRE_INPUT).test(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        // Closed braziers cook through the vanilla campfire block entity; shovels douse
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hitResult) {
        // The game also calls this after an item interaction falls through; only a truly empty hand counts
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }

        // Sneak + empty hand turns a lit brazier off, on every tier; it never lights one
        if (player.isShiftKeyDown()) {
            if (!state.getValue(LIT)) {
                return InteractionResult.PASS;
            }
            CampfireBlock.douse(player, level, pos, state);
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1.0f, 1.0f);
                level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
            }
            return InteractionResult.SUCCESS;
        }

        // Empty-hand click toggles a floor brazier's grate; wall and hanging grates are fixed
        if (state.getValue(PLACEMENT) != BrazierPlacement.FLOOR) {
            return InteractionResult.PASS;
        }
        boolean closed = state.getValue(CLOSED);
        if (closed && hasFoodCooking(level, pos)) {
            // Refused while food is cooking (behaviors plan, decision 1); say why
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable(GRATE_HAS_FOOD));
            }
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(CLOSED, !closed), Block.UPDATE_ALL);
            level.playSound(null, pos, closed ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(player, closed ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean hasFoodCooking(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire
                && campfire.getItems().stream().anyMatch(stack -> !stack.isEmpty());
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        // Pick-block gives the item for this brazier's fire type
        return new ItemStack(ModItems.brazierItem(this, state.getValue(FIRE)));
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        if (state == null) return null;

        Direction side = ctx.getClickedFace();
        BrazierPlacement placement;
        BlockState against = placedAgainst(ctx);

        if (side == Direction.UP) {
            placement = BrazierPlacement.FLOOR;
        } else if (side == Direction.DOWN) {
            placement = BrazierPlacement.CEILING;
        } else if (growsFrom(ctx, against, BrazierPlacement.FLOOR)) {
            // The side of a floor brazier grows its fire pit, out over air too (owner, 2026-10-25)
            placement = BrazierPlacement.FLOOR;
        } else {
            placement = BrazierPlacement.WALL;
            state = state.setValue(FACING, wallFacing(ctx, against, side));
        }

        state = state.setValue(PLACEMENT, placement);

        // Floor braziers are placed open; wall and hanging braziers always have a closed grate
        state = state.setValue(CLOSED, placement != BrazierPlacement.FLOOR);

        // Water: water-proof tiers are placed lit even in water; others are placed out when drenched
        if (this.tier.waterProof()) {
            state = state.setValue(LIT, true);
        } else if (drenched(ctx.getLevel(), ctx.getClickedPos())) {
            state = state.setValue(LIT, false);
        }

        // Wall/ceiling don't use signal fire
        if (placement != BrazierPlacement.FLOOR) {
            state = state.setValue(SIGNAL_FIRE, false);
        }

        // Calculate initial connections
        state = calculateConnections(state, ctx.getLevel(), ctx.getClickedPos(), placement);

        return state;
    }

    /** The block a brazier is placed against: the one behind the clicked face, as vanilla's wall torch reads it. */
    private static BlockState placedAgainst(BlockPlaceContext ctx) {
        return ctx.getLevel().getBlockState(ctx.getClickedPos().relative(ctx.getClickedFace().getOpposite()));
    }

    /**
     * Whether this brazier joins the fire pit of the brazier it's placed against, which is placed this way.
     * Sneaking places it by the clicked face instead, as before (owner, 2026-10-25).
     */
    private static boolean growsFrom(BlockPlaceContext ctx, BlockState against, BrazierPlacement placement) {
        return !ctx.isSecondaryUseActive() && against.getBlock() instanceof BrazierBlock && against.getValue(PLACEMENT) == placement;
    }

    /**
     * A wall brazier faces the clicked side, except that clicking either end of a wall brazier continues
     * its run along the same wall, wherever that wall goes on. Where it stops, as at a pillar's edge, the
     * brazier faces the clicked side as before.
     */
    private static Direction wallFacing(BlockPlaceContext ctx, BlockState against, Direction side) {
        if (growsFrom(ctx, against, BrazierPlacement.WALL)) {
            Direction run = against.getValue(FACING);
            BlockPos wallPos = ctx.getClickedPos().relative(run.getOpposite());
            if (side.getAxis() != run.getAxis() && ctx.getLevel().getBlockState(wallPos).isFaceSturdy(ctx.getLevel(), wallPos, run)) {
                return run;
            }
        }
        return side;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BrazierPlacement placement = state.getValue(PLACEMENT);

        switch (placement) {
            case FLOOR:
                return super.canSurvive(state, level, pos);
            case WALL: {
                Direction wallDir = state.getValue(FACING).getOpposite();
                BlockPos wallPos = pos.relative(wallDir);
                BlockState supportState = level.getBlockState(wallPos);
                return supportState.isFaceSturdy(level, wallPos, state.getValue(FACING))
                        || supportState.getBlock() instanceof BrazierBlock;
            }
            case CEILING: {
                BlockPos abovePos = pos.above();
                return canSupportCeiling(level, abovePos);
            }
        }
        return false;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // Let CampfireBlock handle waterlogging
        state = super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);

        // Water arriving next to or above a lit brazier puts it out on the next tick (see tick)
        if (state.getValue(LIT) && !this.tier.waterProof() && drenched(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }

        return updateConnectionsAndSupport(state, direction, level, pos);
    }

    /**
     * Shared logic for connection updates and support checks.
     * Protected so tier-specific subclasses can reuse it.
     */
    protected BlockState updateConnectionsAndSupport(BlockState state, Direction direction,
            LevelReader level, BlockPos pos) {
        BrazierPlacement placement = state.getValue(PLACEMENT);

        // Check wall support (solid block or another brazier)
        if (placement == BrazierPlacement.WALL) {
            Direction wallDir = state.getValue(FACING).getOpposite();
            if (direction == wallDir) {
                BlockPos wallPos = pos.relative(wallDir);
                BlockState supportState = level.getBlockState(wallPos);
                if (!supportState.isFaceSturdy(level, wallPos, state.getValue(FACING))
                        && !(supportState.getBlock() instanceof BrazierBlock)) {
                    return Blocks.AIR.defaultBlockState();
                }
            }
        }

        // Check ceiling support (solid block or chain)
        if (placement == BrazierPlacement.CEILING && direction == Direction.UP) {
            BlockPos abovePos = pos.above();
            if (!canSupportCeiling(level, abovePos)) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        // Update connections (horizontal only, floor+wall only)
        if (direction.getAxis().isHorizontal() && placement != BrazierPlacement.CEILING) {
            BooleanProperty connectionProp = getConnectionProperty(direction);
            if (connectionProp != null) {
                state = state.setValue(connectionProp, canConnect(level, pos, state, direction));
            }
        }

        return state;
    }

    // Every placement uses the vanilla campfire block entity, so closed wall and hanging braziers cook too.
    // newBlockEntity is inherited from CampfireBlock.

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Client: smoke over cooking food rises from the grate, where the food is drawn. Campfire smoke
        // columns stay on floor braziers only; on wall and hanging braziers they would rise into the wall
        // or ceiling, so those keep their own wisps (animateTick).
        if (level.isClientSide()) {
            if (!state.getValue(LIT)) {
                return null;
            }
            return state.getValue(PLACEMENT) == BrazierPlacement.FLOOR
                    ? createTickerHelper(type, BlockEntityTypes.CAMPFIRE, BrazierBlock::floorParticleTick)
                    : createTickerHelper(type, BlockEntityTypes.CAMPFIRE, BrazierBlock::cookingSmokeTick);
        }
        // Lit braziers cook faster by tier: vanilla's cook step runs extra times per tick
        if (level instanceof ServerLevel serverLevel && state.getValue(LIT)) {
            RecipeManager.CachedCheck<SingleRecipeInput, CampfireCookingRecipe> recipes =
                    RecipeManager.createCheck(RecipeType.CAMPFIRE_COOKING);
            return createTickerHelper(type, BlockEntityTypes.CAMPFIRE, (innerLevel, pos, innerState, campfire) -> {
                for (int step = this.tier.cookingSteps(innerLevel.getGameTime()); step > 0; step--) {
                    CampfireBlockEntity.cookTick(serverLevel, pos, innerState, campfire, recipes);
                }
            });
        }
        return super.getTicker(level, state, type);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
            InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        // Only an open floor brazier burns what steps into it; a closed grate protects (PRD: Fire Damage)
        if (state.getValue(PLACEMENT) == BrazierPlacement.FLOOR && !state.getValue(CLOSED)) {
            super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BrazierPlacement placement = state.getValue(PLACEMENT);

        if (placement == BrazierPlacement.CEILING) {
            return CEILING_SHAPE;
        } else if (placement == BrazierPlacement.WALL) {
            return wallShape(state);
        }
        return floorShape(state);
    }

    /**
     * Client tick for lit floor braziers: smoke like vanilla's campfire ({@link CampfireBlockEntity#particleTick}),
     * thinned so fire pits stay visible (owner's design, 2026-10-18), plus the smoke over each cooking item.
     * <ul>
     *   <li>A fire pit shares its smoke: each brazier smokes as often as a campfire divided by one plus the
     *       number of braziers joined to it, so a lone brazier smokes exactly like a campfire.</li>
     *   <li>A closed grate damps it: light wisps instead of the tall columns.</li>
     *   <li>A signal fire (hay bale underneath) always sends up vanilla's full signal smoke.</li>
     * </ul>
     */
    private static void floorParticleTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        RandomSource random = level.getRandom();
        if (state.getValue(SIGNAL_FIRE)) {
            if (random.nextFloat() < CAMPFIRE_SMOKE_CHANCE) {
                for (int i = 0; i < random.nextInt(2) + 2; i++) {
                    CampfireBlock.makeParticles(level, pos, true, false);
                }
            }
        } else if (random.nextFloat() < CAMPFIRE_SMOKE_CHANCE / (1 + joinedSides(state))) {
            if (state.getValue(CLOSED)) {
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ParticleTypes.SMOKE,
                            pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                            pos.getY() + FLOOR_GRATE_TOP_BLOCKS + random.nextDouble() * 0.2,
                            pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.5,
                            0.0, 0.02, 0.0);
                }
            } else {
                for (int i = 0; i < random.nextInt(2) + 2; i++) {
                    CampfireBlock.makeParticles(level, pos, false, false);
                }
            }
        }
        cookingSmokeTick(level, pos, state, campfire);
    }

    /** How often vanilla's campfire sends up a puff of smoke columns, per tick. */
    private static final float CAMPFIRE_SMOKE_CHANCE = 0.11f;
    /** Top of the floor grate, in blocks (models/block/brazier/floor_grate.json). */
    private static final double FLOOR_GRATE_TOP_BLOCKS = 13.0 / 16.0;

    private static int joinedSides(BlockState state) {
        int joined = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (state.getValue(getConnectionProperty(side))) {
                joined++;
            }
        }
        return joined;
    }

    /**
     * Client tick for lit wall and hanging braziers, and part of the floor tick: the smoke vanilla's
     * campfire gives off over each cooking item, from where {@link #foodLayout} draws it.
     */
    private static void cookingSmokeTick(Level level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        RandomSource random = level.getRandom();
        int rotation = state.getValue(FACING).get2DDataValue();
        NonNullList<ItemStack> items = campfire.getItems();
        FoodLayout food = foodLayout(state);

        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty() && random.nextFloat() < 0.2f) {
                Direction direction = Direction.from2DDataValue(Math.floorMod(slot + rotation, 4));
                double x = pos.getX() + food.centerX() + (direction.getClockWise().getStepX() - direction.getStepX()) * food.spacing();
                double y = pos.getY() + food.height();
                double z = pos.getZ() + food.centerZ() + (direction.getClockWise().getStepZ() - direction.getStepZ()) * food.spacing();

                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 5.0E-4, 0.0);
                }
            }
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) return;

        BrazierPlacement placement = state.getValue(PLACEMENT);

        // Floor braziers follow their campfire: crackle, plus lava pops only for normal fire
        if (placement == BrazierPlacement.FLOOR && state.getValue(FIRE).popsLava()) {
            super.animateTick(state, level, pos, random);
            return;
        }
        playCrackle(level, pos, random);

        if (placement == BrazierPlacement.WALL) {
            spawnFireParticles(state, level, pos, 0, WALL_FIRE_HEIGHT, 0, random);
        } else if (placement == BrazierPlacement.CEILING) {
            spawnFireParticles(state, level, pos, 0, CEILING_FIRE_HEIGHT, 0, random);
        }
    }

    // Grate tops of the wall and hanging models, where the fire shows; both baskets sit in the middle of
    // their block.
    private static final double WALL_FIRE_HEIGHT = 13.0 / 16.0;
    private static final double CEILING_FIRE_HEIGHT = 12.0 / 16.0;

    /** The campfire's crackle, as {@link CampfireBlock#animateTick} plays it. */
    private static void playCrackle(Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS,
                    0.5f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
        }
    }

    /**
     * Wall and hanging braziers burn like vanilla's torches: one smoke and one flame of the fire type on
     * every animate tick, with no velocity. They rise from above the fire, spread over the brazier's wider
     * fire; the offsets move the fire's middle away from the block's.
     */
    private static void spawnFireParticles(BlockState state, Level level, BlockPos pos, double offsetX, double fireHeight,
            double offsetZ, RandomSource random) {
        double x = pos.getX() + 0.5 + offsetX;
        double y = pos.getY() + fireHeight;
        double z = pos.getZ() + 0.5 + offsetZ;

        level.addParticle(ParticleTypes.SMOKE,
                x + (random.nextDouble() - 0.5) * 0.3,
                y + random.nextDouble() * 0.2,
                z + (random.nextDouble() - 0.5) * 0.3,
                0.0, 0.0, 0.0);
        level.addParticle(state.getValue(FIRE).flameParticle(),
                x + (random.nextDouble() - 0.5) * 0.2,
                y,
                z + (random.nextDouble() - 0.5) * 0.2,
                0.0, 0.0, 0.0);
    }

    // --- Support helpers ---

    protected static boolean canSupportCeiling(LevelReader level, BlockPos abovePos) {
        BlockState aboveState = level.getBlockState(abovePos);
        return aboveState.isFaceSturdy(level, abovePos, Direction.DOWN)
                || aboveState.is(BlockTags.CHAINS)
                || aboveState.getBlock() instanceof BrazierBlock;
    }

    // --- Connection helpers (protected for subclass access) ---

    private static Map<Integer, VoxelShape> floorShapes() {
        Map<Integer, VoxelShape> shapes = new HashMap<>();
        for (int mask = 0; mask < 16; mask++) {
            VoxelShape shape = FLOOR_CAGE;
            for (Direction side : Direction.Plane.HORIZONTAL) {
                if (connected(mask, side)) {
                    shape = Shapes.or(shape, FLOOR_BRIDGES.get(side));
                    if (connected(mask, side.getClockWise())) {
                        shape = Shapes.or(shape, FLOOR_CORNERS.get(side));
                    }
                }
            }
            shapes.put(mask, shape.optimize());
        }
        return shapes;
    }

    private static Map<Direction, VoxelShape[]> wallShapes() {
        VoxelShape cage = Shapes.or(Block.box(2, 4, 4, 14, 13, 12), Block.box(1.5, 11, 3.5, 14.5, 13, 12.5),
                Block.box(6.5, 0.5, 14.5, 9.5, 14, 16), Block.box(7, 2.2, 6, 9, 4, 15), Block.box(7, 0.5, 12.5, 9, 3, 15));
        VoxelShape westBridge = Block.box(0, 4, 3.5, 2, 13, 12.5);
        VoxelShape eastBridge = Block.box(14, 4, 3.5, 16, 13, 12.5);
        Map<Direction, VoxelShape[]> shapes = new HashMap<>();
        for (int joined = 0; joined < 4; joined++) {
            VoxelShape north = cage;
            if ((joined & 1) != 0) {
                north = Shapes.or(north, westBridge);
            }
            if ((joined & 2) != 0) {
                north = Shapes.or(north, eastBridge);
            }
            Map<Direction, VoxelShape> turned = Shapes.rotateHorizontal(north.optimize());
            for (Direction facing : Direction.Plane.HORIZONTAL) {
                shapes.computeIfAbsent(facing, f -> new VoxelShape[4])[joined] = turned.get(facing);
            }
        }
        return shapes;
    }

    /** The wall cage for its facing, stretched over the sides that join the next wall brazier. */
    private static VoxelShape wallShape(BlockState state) {
        Direction facing = state.getValue(FACING);
        int joined = 0;
        if (state.getValue(getConnectionProperty(drawnSide(Direction.WEST, facing)))) {
            joined |= 1;
        }
        if (state.getValue(getConnectionProperty(drawnSide(Direction.EAST, facing)))) {
            joined |= 2;
        }
        return WALL_SHAPES.get(facing)[joined];
    }

    /** A side of the wall model as drawn facing north, once the model is turned to face {@code facing}. */
    private static Direction drawnSide(Direction side, Direction facing) {
        Direction result = side;
        for (Direction step = Direction.NORTH; step != facing; step = step.getClockWise()) {
            result = result.getClockWise();
        }
        return result;
    }

    private static boolean connected(int mask, Direction side) {
        return (mask & (1 << side.get2DDataValue())) != 0;
    }

    /** The floor cage, stretched over each connected side, as the model's bridges are. */
    private static VoxelShape floorShape(BlockState state) {
        int mask = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            if (state.getValue(getConnectionProperty(side))) {
                mask |= 1 << side.get2DDataValue();
            }
        }
        return FLOOR_SHAPES.get(mask);
    }

    protected static @Nullable BooleanProperty getConnectionProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_CONNECTED;
            case SOUTH -> SOUTH_CONNECTED;
            case EAST -> EAST_CONNECTED;
            case WEST -> WEST_CONNECTED;
            default -> null;
        };
    }

    /**
     * Fire pits join braziers placed the same way: floor beside floor, or wall beside wall on the same
     * wall. Hanging braziers never connect (PRD: Placement).
     */
    protected static boolean canConnect(LevelReader level, BlockPos pos, BlockState state, Direction direction) {
        BlockState neighborState = level.getBlockState(pos.relative(direction));
        if (!(neighborState.getBlock() instanceof BrazierBlock)) {
            return false;
        }
        BrazierPlacement placement = state.getValue(PLACEMENT);
        if (placement == BrazierPlacement.CEILING || neighborState.getValue(PLACEMENT) != placement) {
            return false;
        }
        return placement == BrazierPlacement.FLOOR || neighborState.getValue(FACING) == state.getValue(FACING);
    }

    private BlockState calculateConnections(BlockState state, LevelReader level, BlockPos pos, BrazierPlacement placement) {
        if (placement == BrazierPlacement.CEILING) {
            return state
                    .setValue(NORTH_CONNECTED, false)
                    .setValue(SOUTH_CONNECTED, false)
                    .setValue(EAST_CONNECTED, false)
                    .setValue(WEST_CONNECTED, false);
        }

        return state
                .setValue(NORTH_CONNECTED, canConnect(level, pos, state, Direction.NORTH))
                .setValue(SOUTH_CONNECTED, canConnect(level, pos, state, Direction.SOUTH))
                .setValue(EAST_CONNECTED, canConnect(level, pos, state, Direction.EAST))
                .setValue(WEST_CONNECTED, canConnect(level, pos, state, Direction.WEST));
    }
}
