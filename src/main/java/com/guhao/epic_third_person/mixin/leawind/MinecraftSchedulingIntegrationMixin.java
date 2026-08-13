package com.guhao.epic_third_person.mixin.leawind;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.leawind.thirdperson.internal.logic.base.rotation.LookRotation;
import io.github.leawind.thirdperson.internal.logic.scheduler.MinecraftSchedulingIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = MinecraftSchedulingIntegration.class, remap = false)
public abstract class MinecraftSchedulingIntegrationMixin {
    @ModifyExpressionValue(
            method = "schedulePlayerRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;isDown()Z",
                    ordinal = 0,
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private static boolean epicThirdPerson$ignoreGuardAsInteraction(boolean keyUseDown) {
        return keyUseDown && !EpicFightLeawindCompatibility.shouldSuppressLeawindInteractionRotation();
    }

    @Inject(method = "beforeInteraction", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void epicThirdPerson$keepLockOnFacingDuringInteraction(
            Optional<LookRotation> rotation,
            CallbackInfo callbackInfo
    ) {
        if (EpicFightLeawindCompatibility.shouldSuppressLeawindInteractionRotation()) {
            callbackInfo.cancel();
        }
    }
}
