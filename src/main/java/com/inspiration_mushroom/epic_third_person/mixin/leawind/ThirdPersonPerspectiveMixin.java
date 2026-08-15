package com.inspiration_mushroom.epic_third_person.mixin.leawind;

import com.inspiration_mushroom.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.leawind.thirdperson.internal.logic.base.ThirdPersonPerspective;
import io.github.leawind.thirdperson.internal.logic.base.rotation.LookController;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ThirdPersonPerspective.class, remap = false)
public abstract class ThirdPersonPerspectiveMixin {
    @WrapOperation(
            method = "computeCameraState",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/github/leawind/thirdperson/internal/logic/base/rotation/LookController;copyRotation(Lorg/joml/Quaternionf;)Z",
                    remap = false
            ),
            remap = false
    )
    private boolean epicThirdPerson$applyLockOnCameraRotation(
            LookController controller,
            Quaternionf destination,
            Operation<Boolean> original
    ) {
        boolean initialized = original.call(controller, destination);
        if (initialized) {
            EpicFightLeawindCompatibility.applyCameraRotation(destination);
        }
        return initialized;
    }
}
