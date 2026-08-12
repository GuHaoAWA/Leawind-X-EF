package com.guhao.epic_third_person.mixin.leawind;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import io.github.leawind.thirdperson.internal.logic.base.MinecraftClientIntegration;
import io.github.leawind.thirdperson.internal.logic.base.rotation.LookRotation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftClientIntegration.class, remap = false)
public abstract class MinecraftClientIntegrationMixin {
    @Inject(method = "beforeRenderFrame", at = @At("HEAD"), cancellable = true, remap = false)
    private static void epicThirdPerson$deferPlayerFacingToBetterLockOn(
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (EpicFightLeawindCompatibility.deferPlayerFacingToBetterLockOn(player)) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "setPlayerRotation", at = @At("TAIL"), remap = false)
    private static void epicThirdPerson$keepPlayerFacingLockTarget(
            LocalPlayer player,
            LookRotation rotation,
            CallbackInfo callbackInfo
    ) {
        EpicFightLeawindCompatibility.applyPlayerFacing(player, false);
    }
}
