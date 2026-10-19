package com.dai.mcbraziers;

import com.dai.mcbraziers.block.ModBlocks;
import com.dai.mcbraziers.block.entity.ModBlockEntities;
import com.dai.mcbraziers.command.BrazierCommand;
import com.dai.mcbraziers.item.ModItems;
import com.dai.mcbraziers.particle.ModParticles;
import com.dai.mcbraziers.registry.ModItemGroups;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McBraziers implements ModInitializer {
    public static final String MOD_ID = "mcbraziers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModParticles.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModItemGroups.register();
        BrazierCommand.register();
        LOGGER.info("MC Braziers loaded!");
    }
}
