package com.guhao.epic_third_person.mixin.betterlockon;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.leawind.thirdperson.internal.logic.base.BaseRuntime;
import io.github.leawind.thirdperson.internal.logic.base.ThirdPersonPerspective;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraCollisionPort;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraFrameInput;
import io.github.leawind.thirdperson.internal.logic.base.camera.CameraPose;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.shelmarow.betterlockon.client.control.BLOCameraSetting;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

import java.util.Optional;

@Mixin(value = ThirdPersonPerspective.class, remap = false)
public abstract class BetterLockOnThirdPersonPerspectiveMixin {
    @WrapOperation(
            method = "computeCameraState",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/github/leawind/thirdperson/internal/logic/base/BaseRuntime;updateCamera(Lio/github/leawind/thirdperson/internal/logic/base/camera/CameraFrameInput;Lio/github/leawind/thirdperson/internal/logic/base/camera/CameraCollisionPort;)Ljava/util/Optional;",
                    remap = false
            ),
            remap = false
    )
    private Optional<CameraPose> epicThirdPerson$applyBetterLockOnCameraOffset(
            BaseRuntime runtime,
            CameraFrameInput frame,
            CameraCollisionPort collision,
            Operation<Optional<CameraPose>> original
    ) {
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        if (!cameraApi.isLockingOnTarget()) {
            return original.call(runtime, frame, collision);
        }

        Vec3 offset = BLOCameraSetting.getCameraPos(Minecraft.getInstance().getFrameTime());
        if (!Double.isFinite(offset.x) || !Double.isFinite(offset.y) || !Double.isFinite(offset.z)) {
            return original.call(runtime, frame, collision);
        }

        CameraCollisionPort offsetCollision = (pivot, desiredPosition) -> collision.resolve(
                pivot,
                new Vector3d(desiredPosition).add(offset.x, offset.y, offset.z)
        );
        return original.call(runtime, frame, offsetCollision);
    }
}
