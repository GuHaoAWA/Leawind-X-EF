package com.inspiration_mushroom.epic_third_person.client;

import com.inspiration_mushroom.epic_third_person.config.EpicThirdPersonClientConfig;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;

@OnlyIn(Dist.CLIENT)
public final class EpicThirdPersonClientCommands {
    private EpicThirdPersonClientCommands() {
    }

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("epicthirdperson")
                        .then(Commands.literal("eightdirection")
                                .executes(context -> setEightDirectionMovement(
                                        context.getSource(),
                                        EpicThirdPersonClientConfig.toggleLeawindEightDirectionMovementWhileLocked()
                                ))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(context -> setEightDirectionMovement(
                                                context.getSource(),
                                                EpicThirdPersonClientConfig.setLeawindEightDirectionMovementWhileLocked(
                                                        BoolArgumentType.getBool(context, "enabled")
                                                )
                                        )))
        ));
    }

    private static int setEightDirectionMovement(CommandSourceStack source, boolean enabled) {
        source.sendSuccess(
                () -> Component.translatable(
                        enabled
                                ? "command.epic_third_person.eight_direction.enabled"
                                : "command.epic_third_person.eight_direction.disabled"
                ).withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                false
        );
        return 1;
    }
}
