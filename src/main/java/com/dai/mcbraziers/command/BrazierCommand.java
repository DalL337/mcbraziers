package com.dai.mcbraziers.command;

import com.dai.mcbraziers.block.BrazierBlock;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class BrazierCommand {

    /** Outcome of {@link #toggle}. */
    public enum ToggleResult {
        LIT,
        PUT_OUT,
        /** Unlit, and the water rules don't allow lighting it here (PRD: Water). */
        CANT_LIGHT_HERE,
        NOT_A_BRAZIER
    }

    public static void register() {
        // Available to every player (permission level 0 in the 1.20.1 version)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("brazier")
                    .then(Commands.literal("extinguish")
                            .executes(BrazierCommand::executeExtinguish)
                    )
            );
        });
    }

    private static int executeExtinguish(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        HitResult hitResult = player.pick(5.0, 0.0f, false);
        ToggleResult result = hitResult.getType() == HitResult.Type.BLOCK
                ? toggle(player.level(), ((BlockHitResult) hitResult).getBlockPos())
                : ToggleResult.NOT_A_BRAZIER;

        switch (result) {
            case LIT -> context.getSource().sendSuccess(() -> Component.literal("Brazier lit"), false);
            case PUT_OUT -> context.getSource().sendSuccess(() -> Component.literal("Brazier extinguished"), false);
            case CANT_LIGHT_HERE -> context.getSource().sendFailure(Component.literal("This brazier can't be lit in water"));
            case NOT_A_BRAZIER -> context.getSource().sendFailure(Component.literal("No brazier targeted"));
        }
        return result == ToggleResult.LIT || result == ToggleResult.PUT_OUT ? 1 : 0;
    }

    /**
     * Turns the brazier at {@code pos} on or off, any tier (PRD: Lighting and Putting Out). Lighting
     * follows the same water rules as flint and steel.
     */
    public static ToggleResult toggle(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BrazierBlock brazier)) {
            return ToggleResult.NOT_A_BRAZIER;
        }
        boolean lit = state.getValue(CampfireBlock.LIT);
        if (!lit && !brazier.canBeLit(state, level, pos)) {
            return ToggleResult.CANT_LIGHT_HERE;
        }
        level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, !lit));
        level.playSound(null, pos, lit ? SoundEvents.GENERIC_EXTINGUISH_FIRE : SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.BLOCKS, 1.0f, 1.0f);
        return lit ? ToggleResult.PUT_OUT : ToggleResult.LIT;
    }
}
