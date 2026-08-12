package com.guhao.epic_third_person.mixin.betterlockon;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.shelmarow.betterlockon.client.control.LockOnControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LockOnControl.class, remap = false)
public abstract class BetterLockOnLockOnControlMixin {
    @Inject(method = "movementInputUpdateEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void epicThirdPerson$preserveLeawindMovementInput(
            MovementInputUpdateEvent event,
            CallbackInfo callbackInfo
    ) {
        if (event.getEntity() instanceof LocalPlayer player
                && EpicFightLeawindCompatibility.shouldUseLeawindEightDirectionMovement(player)) {
            callbackInfo.cancel();
        }
    }
}
