package com.guhao.epic_third_person.mixin.leawind;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.leawind.thirdperson.internal.logic.base.rotation.LookRotation;
import io.github.leawind.thirdperson.internal.logic.base.rotation.PlayerRotationParameters;
import io.github.leawind.thirdperson.internal.logic.base.rotation.PlayerRotationSmoothing;
import io.github.leawind.thirdperson.internal.logic.scheduler.MinecraftSchedulingIntegration;
import io.github.leawind.thirdperson.internal.logic.scheduler.SchedulerRuntime;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = MinecraftSchedulingIntegration.class, remap = false)
public abstract class MinecraftSchedulingIntegrationMixin {
    @WrapOperation(
            method = "schedulePlayerRotation",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/KeyMapping;isDown()Z",
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private static boolean epicThirdPerson$filterEpicFightCombatInteractionKeys(
            KeyMapping keyMapping,
            Operation<Boolean> original
    ) {
        boolean keyDown = original.call(keyMapping);
        if (!keyDown) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean interactionKey = keyMapping == minecraft.options.keyUse
                || keyMapping == minecraft.options.keyAttack
                || keyMapping == minecraft.options.keyPickItem;
        return !interactionKey
                || !EpicFightLeawindCompatibility.shouldSuppressLeawindInteractionRotation();
    }

    @Inject(
            method = "schedulePlayerRotation",
            at = @At("RETURN"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private static void epicThirdPerson$isolatePlayerControlRotation(
            Minecraft minecraft,
            SchedulerRuntime runtime,
            LocalPlayer player,
            CallbackInfoReturnable<PlayerRotationParameters> callbackInfo
    ) {
        EpicFightLeawindCompatibility.PlayerControlRotation controlRotation =
                EpicFightLeawindCompatibility.resolveIndependentPlayerControlRotation(player);
        if (controlRotation == null) {
            return;
        }

        PlayerRotationParameters scheduled = callbackInfo.getReturnValue();
        PlayerRotationSmoothing smoothing = PlayerRotationSmoothing.IMMEDIATE;
        double halfLifeSeconds = 0.0D;
        if (controlRotation.moving() && scheduled != null) {
            smoothing = scheduled.smoothing();
            if (smoothing != PlayerRotationSmoothing.IMMEDIATE) {
                halfLifeSeconds = scheduled.halfLifeSeconds();
            }
        }

        EpicFightLeawindCompatibility.CameraRotation rotation = controlRotation.rotation();
        callbackInfo.setReturnValue(PlayerRotationParameters.custom(
                Optional.of(new LookRotation(rotation.yaw(), rotation.pitch())),
                halfLifeSeconds,
                smoothing
        ));
    }

    @Inject(method = "beforeInteraction", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void epicThirdPerson$suppressProgrammaticCameraInteractionFacing(
            Optional<LookRotation> rotation,
            CallbackInfo callbackInfo
    ) {
        if (EpicFightLeawindCompatibility.shouldSuppressLeawindInteractionRotation()) {
            callbackInfo.cancel();
        }
    }
}
