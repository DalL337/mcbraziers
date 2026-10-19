package com.dai.mcbraziers.block;

import com.dai.mcbraziers.McBraziers;
import net.fabricmc.fabric.api.registry.OxidizableBlocksRegistry;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public class ModBlocks {

    /** The 14 brazier blocks, in {@link BrazierVariant#ALL} order. */
    public static final Map<BrazierVariant, Block> BRAZIERS = registerBraziers();

    public static final Block DIAMOND_BARS = registerBlock("diamond_bars", IronBarsBlock::new, createBarsProperties(MapColor.DIAMOND));
    public static final Block OBSIDIAN_BARS = registerBlock("obsidian_bars", IronBarsBlock::new, createBarsProperties(MapColor.COLOR_BLACK));
    public static final Block PRISMARINE_BARS = registerBlock("prismarine_bars", IronBarsBlock::new, createBarsProperties(MapColor.COLOR_CYAN));
    public static final Block NETHERITE_BARS = registerBlock("netherite_bars", IronBarsBlock::new, createBarsProperties(MapColor.COLOR_BLACK));
    public static final Block CRYING_OBSIDIAN_BARS = registerBlock("crying_obsidian_bars", CryingObsidianBarsBlock::new,
            createBarsProperties(MapColor.COLOR_BLACK));

    /**
     * A campfire with green copper-fire flames (PRD: Copper Campfire). Properties mirror vanilla's campfire;
     * like the soul campfire, it gives no lava pops.
     */
    public static final Block COPPER_CAMPFIRE = registerCampfire("copper_campfire", BrazierFire.COPPER);

    /** A campfire with purple amethyst-fire flames (PRD Content addendum), made like the Copper Campfire. */
    public static final Block AMETHYST_CAMPFIRE = registerCampfire("amethyst_campfire", BrazierFire.AMETHYST);

    private static Block registerCampfire(String name, BrazierFire fire) {
        return registerBlock(name,
                properties -> new CampfireBlock(false, 1, properties),
                BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PODZOL)
                        .instrument(NoteBlockInstrument.BASS)
                        .strength(2.0f)
                        .sound(SoundType.WOOD)
                        .lightLevel(state -> state.getValue(CampfireBlock.LIT) ? fire.lightLevel() : 0)
                        .noOcclusion()
                        .ignitedByLava());
    }

    private static Map<BrazierVariant, Block> registerBraziers() {
        Map<BrazierVariant, Block> braziers = new LinkedHashMap<>();
        for (BrazierVariant variant : BrazierVariant.ALL) {
            // Unwaxed copper ages like vanilla copper and crying obsidian cries; the rest are plain braziers
            Function<BlockBehaviour.Properties, Block> factory;
            if (variant.tier() == BrazierTier.COPPER && !variant.waxed()) {
                factory = p -> new WeatheringCopperBrazierBlock(variant.weatherState(), p);
            } else if (variant.crying()) {
                factory = CryingObsidianBrazierBlock::new;
            } else {
                factory = p -> new BrazierBlock(variant.tier(), p);
            }
            braziers.put(variant, registerBlock(variant.blockId(), factory, createBrazierProperties(variant.tier())));
        }
        return Collections.unmodifiableMap(braziers);
    }

    /** Copper aging order and wax pairs, through Fabric so vanilla aging, honeycomb and axes all see them. */
    private static void registerCopperAging() {
        List<String> stages = List.of("", "exposed_", "weathered_", "oxidized_");
        for (int i = 0; i < stages.size(); i++) {
            Block unwaxed = copperBrazier(stages.get(i));
            if (i + 1 < stages.size()) {
                OxidizableBlocksRegistry.registerNextStage(unwaxed, copperBrazier(stages.get(i + 1)));
            }
            OxidizableBlocksRegistry.registerWaxable(unwaxed, copperBrazier("waxed_" + stages.get(i)));
        }
    }

    private static Block copperBrazier(String agingPrefix) {
        return BRAZIERS.get(new BrazierVariant(BrazierTier.COPPER, agingPrefix));
    }

    // Every brazier mines like a campfire (hardness 2.0) but needs a pickaxe to drop (behaviors plan,
    // decisions 2 and 3); the pickaxe tier comes from the generated mining tags.
    private static BlockBehaviour.Properties createBrazierProperties(BrazierTier tier) {
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(tier.mapColor())
                .strength(2.0f)
                .requiresCorrectToolForDrops()
                .noOcclusion()
                .lightLevel(state -> state.getValue(CampfireBlock.LIT) ? state.getValue(BrazierBlock.FIRE).lightLevel() : 0)
                .sound(SoundType.METAL);
        if (tier.explosionProof()) {
            // Obsidian's blast resistance; pistons already refuse blocks with block entities, this states it outright
            properties = properties.explosionResistance(1200.0f).pushReaction(PushReaction.IMMOVEABLE);
        }
        return properties;
    }

    // Vanilla iron bars' properties; mining needs a stone pickaxe or better (generated tags)
    private static BlockBehaviour.Properties createBarsProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .requiresCorrectToolForDrops()
                .strength(5.0f, 6.0f)
                .sound(SoundType.IRON)
                .noOcclusion();
    }

    private static Block registerBlock(String name, Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, name));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
    }

    public static void register() {
        registerCopperAging();
        McBraziers.LOGGER.info("Registering blocks for " + McBraziers.MOD_ID);
    }
}
