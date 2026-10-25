package com.dai.mcbraziers.gametest;

import com.dai.mcbraziers.block.BrazierBlock;
import com.dai.mcbraziers.block.BrazierFire;
import com.dai.mcbraziers.block.BrazierPlacement;
import com.dai.mcbraziers.block.BrazierTier;
import com.dai.mcbraziers.block.BrazierVariant;
import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.gametest.mixin.SingleQuadParticleAccessor;
import com.dai.mcbraziers.particle.ModParticles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Behaviors slice 6, fire-type particles ({@code docs/plans/behaviors/behaviors-build-plan.md}).
 * Particles exist only on the client, so this runs in a real client. {@link ParticleLog} records what
 * each block spawns, and the test checks it against the plan. It also saves screenshots of two night
 * scenes for a person to look at, each brazier beside its vanilla reference: a wall torch next to each
 * wall brazier (amethyst fire has none), and a campfire next to each floor brazier. Crying obsidian's tears
 * are checked and photographed in a third scene.
 */
public class FireParticleClientGameTest implements FabricClientGameTest {

    private static final Block IRON_BRAZIER = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.IRON, ""));

    /**
     * Plan, slice 6: wall and hanging braziers use vanilla's matching flame. Amethyst fire has no vanilla
     * flame, so it uses the mod's purple one (crying obsidian and amethyst fire plan, slice A).
     */
    private static final Map<BrazierFire, ParticleType<?>> EXPECTED_FLAME = Map.of(
            BrazierFire.NORMAL, ParticleTypes.FLAME,
            BrazierFire.SOUL, ParticleTypes.SOUL_FIRE_FLAME,
            BrazierFire.COPPER, ParticleTypes.COPPER_FIRE_FLAME,
            BrazierFire.AMETHYST, ModParticles.AMETHYST_FIRE_FLAME);

    private static final Set<ParticleType<?>> FLAMES = Set.of(ParticleTypes.FLAME, ParticleTypes.SMALL_FLAME,
            ParticleTypes.SOUL_FIRE_FLAME, ParticleTypes.COPPER_FIRE_FLAME, ModParticles.AMETHYST_FIRE_FLAME);

    private static final Set<ParticleType<?>> SMOKE_COLUMNS = Set.of(
            ParticleTypes.CAMPFIRE_COSY_SMOKE, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE);

    /** Vanilla's smoke over cooking food rises at this speed ({@code CampfireBlockEntity.particleTick}). */
    private static final double FOOD_SMOKE_SPEED = 5.0E-4;

    /** Column x positions, left to right as the camera sees them. */
    private static final Map<BrazierFire, Integer> COLUMNS = Map.of(
            BrazierFire.NORMAL, -4, BrazierFire.SOUL, 0, BrazierFire.COPPER, 4, BrazierFire.AMETHYST, 8);
    /** The middle of the columns, where the camera stands. */
    private static final double CAMERA_X = 2.5;

    /** Vanilla has no amethyst torch, so amethyst fire's wall brazier stands alone. */
    private static final Map<BrazierFire, Block> REFERENCE_TORCHES = Map.of(
            BrazierFire.NORMAL, Blocks.WALL_TORCH, BrazierFire.SOUL, Blocks.SOUL_WALL_TORCH,
            BrazierFire.COPPER, Blocks.COPPER_WALL_TORCH);

    private static final Map<BrazierFire, Block> REFERENCE_CAMPFIRES = Map.of(
            BrazierFire.NORMAL, Blocks.CAMPFIRE, BrazierFire.SOUL, Blocks.SOUL_CAMPFIRE,
            BrazierFire.COPPER, ModBlocks.COPPER_CAMPFIRE, BrazierFire.AMETHYST, ModBlocks.AMETHYST_CAMPFIRE);

    private static final int GROUND = 100;
    /** The floor scene sits far enough east that its smoke columns stay out of the wall scene. */
    private static final int FLOOR_SCENE_X = 40;
    /** Lava pops on 1 in 5 animate ticks, so 200 leave no realistic chance of missing one. */
    private static final int ANIMATE_TICKS = 200;

    @Override
    public void runTest(ClientGameTestContext context) {
        if (!ClientScenarios.runs("fire-particles")) {
            return;
        }
        try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
            TestServerContext server = singleplayer.getServer();
            server.runCommand("time set midnight");
            server.runCommand("weather clear");
            server.runOnServer(minecraft -> {
                ServerLevel level = minecraft.overworld();
                buildWallScene(level);
                buildFloorScene(level);
                buildSmokeScene(level);
                buildTearScene(level);
            });
            // Hide the HUD for the screenshots; set it rather than toggle, since the game keeps it between tests
            context.runOnClient(minecraft -> {
                if (!minecraft.gui.hud.isHidden()) {
                    minecraft.gui.hud.toggle();
                }
            });

            server.runCommand("tp @a " + CAMERA_X + " " + (GROUND + 1) + " 8.5 180 0");
            singleplayer.getConnection().waitForChunksRender();
            checkWallAndHangingFlames(context);
            checkAmethystFlameParticle(context);
            checkWallAndHangingSmoke(context);
            for (int shot = 1; shot <= 4; shot++) {
                context.waitTicks(40);
                context.takeScreenshot("slice6-wall-and-hanging-" + shot);
            }

            server.runCommand("tp @a " + (FLOOR_SCENE_X + CAMERA_X) + " " + (GROUND + 1) + " 6.5 180 10");
            singleplayer.getConnection().waitForChunksRender();
            checkLavaPops(context);
            for (int shot = 1; shot <= 4; shot++) {
                context.waitTicks(40);
                context.takeScreenshot("slice6-floor-" + shot);
            }

            server.runCommand("tp @a " + (SMOKE_X + 10.5) + " " + (GROUND + 1) + " 9 180 0");
            singleplayer.getConnection().waitForChunksRender();
            checkFirePitSmoke(context);

            server.runCommand("tp @a " + (TEAR_X + 4.5) + " " + (GROUND + 1) + " 6.5 180 0");
            singleplayer.getConnection().waitForChunksRender();
            checkTears(context);
            for (int shot = 1; shot <= 2; shot++) {
                context.waitTicks(40);
                context.takeScreenshot("crying-tears-" + shot);
            }
        }
    }

    /**
     * Tear scene: crying obsidian bars and an unlit crying obsidian brazier, then the plain obsidian ones,
     * left to right, and a crying brazier hanging from a ledge above them.
     */
    private static final int TEAR_X = 120;

    /**
     * Crying obsidian plan, slice B: crying bars and braziers drip vanilla's crying obsidian tears, lit or
     * not, and plain obsidian doesn't. A tear forms on the block, never inside the ground or the ledge.
     */
    private static void checkTears(ClientGameTestContext context) {
        Map<String, BlockPos> crying = Map.of("crying obsidian bars", tearPos(0), "unlit crying obsidian brazier", tearPos(3),
                "hanging crying obsidian brazier", hangingTearPos());
        Map<String, BlockPos> plain = Map.of("obsidian bars", tearPos(6), "obsidian brazier", tearPos(9));
        crying.forEach((name, pos) -> {
            List<ParticleLog.Entry> tears = animateLog(context, pos).stream()
                    .filter(entry -> entry.type() == ParticleTypes.DRIPPING_OBSIDIAN_TEAR).toList();
            check(!tears.isEmpty(), name + " shed no tears in " + ANIMATE_TICKS + " animate ticks");
            List<BlockPos> buried = tears.stream().map(ParticleLog.Entry::block)
                    .filter(block -> !block.equals(pos) && !context.computeOnClient(minecraft -> minecraft.level.getBlockState(block).isAir()))
                    .distinct().toList();
            check(buried.isEmpty(), name + " formed tears inside the blocks at " + buried);
        });
        plain.forEach((name, pos) -> check(!animate(context, pos).contains(ParticleTypes.DRIPPING_OBSIDIAN_TEAR),
                name + " shed tears; only crying obsidian should"));
    }

    /** Smoke scene, far from the others: a lone open brazier, an open 3 x 2 pit, a lone closed one, a signal fire. */
    private static final int SMOKE_X = 200;
    private static final int SMOKE_TICKS = 800;

    /**
     * Owner's design (2026-10-18): a lone open floor brazier smokes like a campfire; a fire pit shares that
     * smoke, each brazier smoking as often as a campfire divided by one plus its joined neighbours, so a 3 x 2
     * pit sends up about 1.8 campfires' worth, not 6; a closed grate swaps the columns for light wisps; a
     * signal fire keeps vanilla's full signal smoke. Counted from the smoke the client is asked to spawn.
     */
    private static void checkFirePitSmoke(ClientGameTestContext context) {
        ParticleLog.start();
        context.waitTicks(SMOKE_TICKS);
        List<ParticleLog.Entry> logged = ParticleLog.stop();

        long lone = columns(logged, ParticleTypes.CAMPFIRE_COSY_SMOKE, SMOKE_X, SMOKE_X, 0, 0);
        long pit = columns(logged, ParticleTypes.CAMPFIRE_COSY_SMOKE, SMOKE_X + 6, SMOKE_X + 8, 0, 1);
        long closedColumns = columns(logged, ParticleTypes.CAMPFIRE_COSY_SMOKE, SMOKE_X + 14, SMOKE_X + 14, 0, 0);
        long closedWisps = logged.stream().filter(e -> e.type() == ParticleTypes.SMOKE && e.block().getX() == SMOKE_X + 14).count();
        long signal = columns(logged, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, SMOKE_X + 20, SMOKE_X + 20, 0, 0);

        // Vanilla: a 0.11 chance each tick of 2 or 3 columns, so about 220 over 800 ticks
        check(lone >= 100 && lone <= 400, "a lone open brazier sent up " + lone + " smoke columns in " + SMOKE_TICKS + " ticks, expected about 220");
        double pitShare = (double) pit / lone;
        check(pitShare >= 1.0 && pitShare <= 3.0, "a 3 x 2 fire pit sent up " + pitShare + " times a lone brazier's smoke, expected about 1.8 (6 unshared)");
        check(closedColumns == 0, "a closed brazier sent up " + closedColumns + " smoke columns, expected none");
        check(closedWisps > 0, "a closed brazier gave off no wisps of smoke");
        check(signal >= lone / 2, "a signal fire sent up " + signal + " signal smoke columns, expected about as many as a lone campfire (" + lone + ")");
    }

    /** Particles of a type whose spawn column lies in the given block x and z ranges, at any height. */
    private static long columns(List<ParticleLog.Entry> logged, ParticleType<?> type, int x0, int x1, int z0, int z1) {
        return logged.stream().filter(e -> e.type() == type && e.block().getX() >= x0 && e.block().getX() <= x1
                && e.block().getZ() >= z0 && e.block().getZ() <= z1).count();
    }

    private static void checkWallAndHangingFlames(ClientGameTestContext context) {
        COLUMNS.forEach((fire, x) -> {
            for (BlockPos pos : List.of(wallPos(x), hangingPos(x))) {
                Set<ParticleType<?>> flames = new HashSet<>(animate(context, pos));
                flames.retainAll(FLAMES);
                check(flames.equals(Set.of(EXPECTED_FLAME.get(fire))),
                        fire + " brazier at " + pos + " spawned flames " + names(flames) + ", expected " + name(EXPECTED_FLAME.get(fire)));
            }
        });
    }

    /**
     * Amethyst fire's flame particle draws vanilla's soul fire flame sprite recoloured purple, untinted (PRD
     * License addendum, recolouring at load, and Content erratum).
     */
    private static void checkAmethystFlameParticle(ClientGameTestContext context) {
        List<String> problems = context.computeOnClient(minecraft -> {
            Particle amethyst = minecraft.particleEngine.createParticle(ModParticles.AMETHYST_FIRE_FLAME, 0.5, GROUND + 3, 0.5, 0, 0, 0);
            Particle soul = minecraft.particleEngine.createParticle(ParticleTypes.SOUL_FIRE_FLAME, 0.5, GROUND + 3, 0.5, 0, 0, 0);
            if (!(amethyst instanceof SingleQuadParticleAccessor purple) || !(soul instanceof SingleQuadParticleAccessor vanilla)) {
                return List.of("the game made no amethyst or soul flame particle to compare");
            }
            List<String> found = new ArrayList<>(SpritePixels.purpleRecolourProblems(purple.mcbraziers$sprite().contents(),
                    vanilla.mcbraziers$sprite().contents()));
            if (purple.mcbraziers$red() != 1 || purple.mcbraziers$green() != 1 || purple.mcbraziers$blue() != 1) {
                found.add("the particle is tinted " + purple.mcbraziers$red() + ", " + purple.mcbraziers$green() + ", " + purple.mcbraziers$blue());
            }
            return found;
        });
        check(problems.isEmpty(), "the amethyst flame particle is wrong: " + problems);
    }

    /** Wall and hanging braziers smoke over their food, and never send up the campfire's smoke columns. */
    private static void checkWallAndHangingSmoke(ClientGameTestContext context) {
        ParticleLog.start();
        context.waitTicks(100);
        List<ParticleLog.Entry> logged = ParticleLog.stop();

        List<ParticleLog.Entry> columns = logged.stream()
                .filter(entry -> SMOKE_COLUMNS.contains(entry.type()) && Math.abs(entry.block().getX()) < FLOOR_SCENE_X / 2)
                .toList();
        check(columns.isEmpty(), "wall scene sent up campfire smoke columns: "
                + names(columns.stream().map(ParticleLog.Entry::type).collect(Collectors.toSet()))
                + " at " + columns.stream().map(ParticleLog.Entry::block).distinct().toList());

        Set<BlockPos> foodSmoke = logged.stream()
                .filter(entry -> entry.type() == ParticleTypes.SMOKE && entry.yd() == FOOD_SMOKE_SPEED)
                .map(ParticleLog.Entry::block)
                .collect(Collectors.toSet());
        COLUMNS.forEach((fire, x) -> {
            for (BlockPos pos : List.of(wallPos(x), hangingPos(x))) {
                check(foodSmoke.contains(pos), fire + " brazier at " + pos + " gave no smoke over its food");
            }
        });
    }

    /** Plan, slice 6: floor braziers pop lava only with normal fire; the Copper Campfire never does. */
    private static void checkLavaPops(ClientGameTestContext context) {
        COLUMNS.forEach((fire, x) -> {
            BlockPos brazier = floorPos(x);
            boolean expected = fire == BrazierFire.NORMAL;
            check(animate(context, brazier).contains(ParticleTypes.LAVA) == expected,
                    fire + " floor brazier lava pops: expected " + expected);
        });
        check(!animate(context, floorPos(COLUMNS.get(BrazierFire.COPPER)).east()).contains(ParticleTypes.LAVA),
                "Copper Campfire popped lava");
        check(!animate(context, floorPos(COLUMNS.get(BrazierFire.AMETHYST)).east()).contains(ParticleTypes.LAVA),
                "Amethyst Campfire popped lava");
        check(animate(context, floorPos(COLUMNS.get(BrazierFire.NORMAL)).east()).contains(ParticleTypes.LAVA),
                "vanilla campfire popped no lava; the check itself is broken");
    }

    /** Runs the block's own animate tick {@link #ANIMATE_TICKS} times and returns the particle types it spawned. */
    private static Set<ParticleType<?>> animate(ClientGameTestContext context, BlockPos pos) {
        return animateLog(context, pos).stream().map(ParticleLog.Entry::type).collect(Collectors.toSet());
    }

    /** Runs the block's own animate tick {@link #ANIMATE_TICKS} times and returns the particles it spawned. */
    private static List<ParticleLog.Entry> animateLog(ClientGameTestContext context, BlockPos pos) {
        return context.computeOnClient(minecraft -> {
            ClientLevel level = minecraft.level;
            BlockState state = level.getBlockState(pos);
            RandomSource random = RandomSource.create(42);
            ParticleLog.start();
            for (int i = 0; i < ANIMATE_TICKS; i++) {
                state.getBlock().animateTick(state, level, pos, random);
            }
            return ParticleLog.stop();
        });
    }

    private static void check(boolean condition, String failure) {
        if (!condition) {
            throw new AssertionError(failure);
        }
    }

    private static String name(ParticleType<?> type) {
        return String.valueOf(BuiltInRegistries.PARTICLE_TYPE.getKey(type));
    }

    private static List<String> names(Set<ParticleType<?>> types) {
        return types.stream().map(FireParticleClientGameTest::name).sorted().toList();
    }

    private static BlockPos wallPos(int x) {
        return new BlockPos(x, GROUND + 3, -1);
    }

    private static BlockPos hangingPos(int x) {
        return new BlockPos(x, GROUND + 5, -1);
    }

    private static BlockPos floorPos(int x) {
        return new BlockPos(FLOOR_SCENE_X + x, GROUND + 1, 0);
    }

    /** A stone wall at z = -2 with, per fire type, a wall brazier and torch, and a hanging brazier under a ledge. */
    private static void buildWallScene(ServerLevel level) {
        fill(level, -7, GROUND, -2, 11, GROUND, 9, Blocks.STONE);
        fill(level, -7, GROUND + 1, -2, 11, GROUND + 7, -2, Blocks.STONE);

        COLUMNS.forEach((fire, x) -> {
            BlockState brazier = IRON_BRAZIER.defaultBlockState()
                    .setValue(BrazierBlock.FIRE, fire)
                    .setValue(CampfireBlock.LIT, true)
                    .setValue(BrazierBlock.CLOSED, true)
                    .setValue(CampfireBlock.FACING, Direction.SOUTH);

            BlockPos wall = wallPos(x);
            set(level, wall, brazier.setValue(BrazierBlock.PLACEMENT, BrazierPlacement.WALL));
            addFood(level, wall);
            if (REFERENCE_TORCHES.containsKey(fire)) {
                set(level, wall.east(), REFERENCE_TORCHES.get(fire).defaultBlockState().setValue(WallTorchBlock.FACING, Direction.SOUTH));
            }

            BlockPos hanging = hangingPos(x);
            set(level, hanging.above(), Blocks.STONE.defaultBlockState());
            set(level, hanging, brazier.setValue(BrazierBlock.PLACEMENT, BrazierPlacement.CEILING));
            addFood(level, hanging);
        });
    }

    /** Per fire type, an open floor brazier beside its campfire. */
    private static void buildFloorScene(ServerLevel level) {
        fill(level, FLOOR_SCENE_X - 7, GROUND, -4, FLOOR_SCENE_X + 11, GROUND, 9, Blocks.STONE);

        COLUMNS.forEach((fire, x) -> {
            BlockPos floor = floorPos(x);
            set(level, floor, IRON_BRAZIER.defaultBlockState()
                    .setValue(BrazierBlock.FIRE, fire)
                    .setValue(CampfireBlock.LIT, true)
                    .setValue(CampfireBlock.FACING, Direction.SOUTH));
            set(level, floor.east(), REFERENCE_CAMPFIRES.get(fire).defaultBlockState()
                    .setValue(CampfireBlock.LIT, true)
                    .setValue(CampfireBlock.FACING, Direction.SOUTH));
        });
    }

    private static void addFood(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            campfire.placeFood(level, null, new ItemStack(Items.BEEF));
        }
    }

    private static void buildSmokeScene(ServerLevel level) {
        fill(level, SMOKE_X - 4, GROUND, -4, SMOKE_X + 24, GROUND, 12, Blocks.STONE);
        BlockState open = IRON_BRAZIER.defaultBlockState().setValue(CampfireBlock.LIT, true);
        set(level, new BlockPos(SMOKE_X, GROUND + 1, 0), open);
        List<BlockPos> pit = new ArrayList<>();
        for (int x = SMOKE_X + 6; x <= SMOKE_X + 8; x++) for (int z = 0; z <= 1; z++) pit.add(new BlockPos(x, GROUND + 1, z));
        pit.forEach(pos -> set(level, pos, open));
        pit.forEach(pos -> set(level, pos, Block.updateFromNeighbourShapes(level.getBlockState(pos), level, pos)));
        set(level, new BlockPos(SMOKE_X + 14, GROUND + 1, 0), open.setValue(BrazierBlock.CLOSED, true));
        set(level, new BlockPos(SMOKE_X + 20, GROUND, 0), Blocks.HAY_BLOCK.defaultBlockState());
        set(level, new BlockPos(SMOKE_X + 20, GROUND + 1, 0), open.setValue(CampfireBlock.SIGNAL_FIRE, true));
    }

    private static BlockPos tearPos(int x) {
        return new BlockPos(TEAR_X + x, GROUND + 1, 0);
    }

    private static BlockPos hangingTearPos() {
        return new BlockPos(TEAR_X + 4, GROUND + 4, 0);
    }

    private static void buildTearScene(ServerLevel level) {
        fill(level, TEAR_X - 4, GROUND, -4, TEAR_X + 13, GROUND, 8, Blocks.STONE);
        Block cryingBrazier = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.OBSIDIAN, "crying_"));
        Block obsidianBrazier = ModBlocks.BRAZIERS.get(new BrazierVariant(BrazierTier.OBSIDIAN, ""));
        set(level, tearPos(0), ModBlocks.CRYING_OBSIDIAN_BARS.defaultBlockState());
        // Campfire blocks place lit by default; these two are put out
        set(level, tearPos(3), cryingBrazier.defaultBlockState().setValue(CampfireBlock.LIT, false));
        set(level, tearPos(6), ModBlocks.OBSIDIAN_BARS.defaultBlockState());
        set(level, tearPos(9), obsidianBrazier.defaultBlockState().setValue(CampfireBlock.LIT, false));
        set(level, hangingTearPos().above(), Blocks.STONE.defaultBlockState());
        set(level, hangingTearPos(), cryingBrazier.defaultBlockState()
                .setValue(BrazierBlock.PLACEMENT, BrazierPlacement.CEILING)
                .setValue(BrazierBlock.FIRE, BrazierFire.AMETHYST)
                .setValue(CampfireBlock.LIT, true));
    }

    private static void fill(ServerLevel level, int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (BlockPos pos : BlockPos.betweenClosed(x1, y1, z1, x2, y2, z2)) {
            set(level, pos, block.defaultBlockState());
        }
    }

    /** Places without neighbour updates, so supports placed later don't matter. */
    private static void set(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}
