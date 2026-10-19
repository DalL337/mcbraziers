package com.dai.mcbraziers.particle;

import com.dai.mcbraziers.McBraziers;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

public class ModParticles {

    /**
     * Amethyst fire's flame on wall and hanging braziers. Vanilla has no purple flame, so the client draws
     * vanilla's soul fire flame sprite recoloured purple while it loads (PRD License addendum).
     */
    public static final SimpleParticleType AMETHYST_FIRE_FLAME = register("amethyst_fire_flame");

    private static SimpleParticleType register(String name) {
        return Registry.register(BuiltInRegistries.PARTICLE_TYPE,
                Identifier.fromNamespaceAndPath(McBraziers.MOD_ID, name), FabricParticleTypes.simple());
    }

    // Runs first in the initializer: fire types name this particle, so it must exist before any block does
    public static void register() {
        McBraziers.LOGGER.info("Registering particles for " + McBraziers.MOD_ID);
    }
}
