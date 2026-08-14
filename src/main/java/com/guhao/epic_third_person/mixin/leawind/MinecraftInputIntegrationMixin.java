package com.guhao.epic_third_person.mixin.leawind;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.leawind.thirdperson.internal.bridge.events.LocalPlayerMovementInputEvent;
import io.github.leawind.thirdperson.internal.logic.base.BaseRuntime;
import io.github.leawind.thirdperson.internal.logic.base.MinecraftInputIntegration;
import io.github.leawind.thirdperson.internal.logic.base.rotation.MovementIntent;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = MinecraftInputIntegration.class, remap = false)
public abstract class MinecraftInputIntegrationMixin {
    @Inject(method = "modifyMovementInput", at = @At("HEAD"), cancellable = true, remap = false)
    private static void epicThirdPerson$deferMovementToBetterLockOn(
            LocalPlayer player,
            LocalPlayerMovementInputEvent.MovementInput vanillaInput,
            CallbackInfoReturnable<LocalPlayerMovementInputEvent.MovementInput> callbackInfo
    ) {
        if (EpicFightLeawindCompatibility.deferMovementToBetterLockOn(player)) {
            BaseRuntime.getInstance().session().clearMovementIntent();
            callbackInfo.setReturnValue(vanillaInput);
        }
    }

    @WrapOperation(
            method = "modifyMovementInput",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/github/leawind/thirdperson/internal/logic/base/rotation/MovementIntent;tryCreate(FFFF)Ljava/util/Optional;",
                    remap = false
            ),
            remap = false
    )
    private static Optional<MovementIntent> epicThirdPerson$useHardLockCameraForMovementAxes(
            float leftImpulse,
            float forwardImpulse,
            float cameraYaw,
            float cameraPitch,
            Operation<Optional<MovementIntent>> original
    ) {
        EpicFightLeawindCompatibility.CameraRotation lockRotation =
                EpicFightLeawindCompatibility.resolveLockedMovementCameraRotation();
        if (lockRotation != null) {
            cameraYaw = lockRotation.yaw();
            cameraPitch = lockRotation.pitch();
        }

        return original.call(leftImpulse, forwardImpulse, cameraYaw, cameraPitch);
    }
}
