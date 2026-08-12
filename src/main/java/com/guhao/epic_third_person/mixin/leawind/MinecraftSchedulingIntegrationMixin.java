package com.guhao.epic_third_person.mixin.leawind;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import io.github.leawind.thirdperson.internal.logic.base.rotation.LookRotation;
import io.github.leawind.thirdperson.internal.logic.scheduler.MinecraftSchedulingIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = MinecraftSchedulingIntegration.class, remap = false)
public abstract class MinecraftSchedulingIntegrationMixin {
    @Inject(method = "beforeInteraction", at = @At("HEAD"), cancellable = true, remap = false)
    private static void epicThirdPerson$keepLockOnFacingDuringInteraction(
            Optional<LookRotation> rotation,
            CallbackInfo callbackInfo
    ) {
        if (EpicFightLeawindCompatibility.shouldSuppressLeawindInteractionRotation()) {
            callbackInfo.cancel();
        }
    }
}
