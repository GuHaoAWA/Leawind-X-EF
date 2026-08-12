package com.guhao.epic_third_person.mixin.betterlockon;

import com.bawnorton.mixinsquared.TargetHandler;
import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.shelmarow.betterlockon.client.control.BLOCameraSetting;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.EpicFightClientHooks;
import yesman.epicfight.api.client.event.types.LockOnEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPSetPlayerTarget;

@SuppressWarnings("InvalidMemberReference")
@Mixin(value = EpicFightCameraAPI.class, priority = 1_500, remap = false)
public abstract class BetterLockOnEpicFightCameraAPIMixin {
    @Shadow
    private LivingEntity focusingEntity;

    @Shadow
    private boolean lockingOnTarget;

    @Shadow
    public abstract boolean isLockingOnTarget();

    @Shadow
    public abstract void setCameraRotations(float xRot, float yRot, boolean syncOld);

    @Unique
    private EpicFightLeawindCompatibility.CameraRotation epicThirdPerson$cameraBeforeLockOn;

    @Inject(method = "setLockOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void epicThirdPerson$handleLockOnState(boolean lockingOnTarget, CallbackInfo callbackInfo) {
        if (lockingOnTarget) {
            if (!this.lockingOnTarget) {
                this.epicThirdPerson$cameraBeforeLockOn =
                        EpicFightLeawindCompatibility.captureCameraBeforeBetterLockOn();
            }
            return;
        }

        if (!EpicFightLeawindCompatibility.shouldReleaseBetterLockOnTargetImmediately()) {
            return;
        }

        boolean wasLockingOnTarget = this.lockingOnTarget;
        LivingEntity releasedTarget = this.focusingEntity;
        boolean hadRetainedState = wasLockingOnTarget
                || releasedTarget != null
                || this.epicThirdPerson$cameraBeforeLockOn != null;

        if (wasLockingOnTarget) {
            EpicFightClientHooks.Camera.LOCK_ON_RELEASED.post(
                    new LockOnEvent.Release((EpicFightCameraAPI) (Object) this, releasedTarget)
            );
        }

        this.lockingOnTarget = false;
        this.focusingEntity = null;
        BLOCameraSetting.reset();
        for (int tick = 0; tick < 30 && !BLOCameraSetting.transitionFinished(); tick++) {
            BLOCameraSetting.tick();
        }

        EpicFightLeawindCompatibility.CameraRotation returnRotation =
                this.epicThirdPerson$cameraBeforeLockOn;
        if (returnRotation != null) {
            this.setCameraRotations(returnRotation.pitch(), returnRotation.yaw(), true);
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                this.setCameraRotations(player.getXRot(), player.getYRot(), true);
            }
        }

        EpicFightLeawindCompatibility.restoreCameraAfterBetterLockOnRelease(
                returnRotation
        );
        this.epicThirdPerson$cameraBeforeLockOn = null;
        if (hadRetainedState) {
            EpicFightNetworkManager.sendToServer(new CPSetPlayerTarget(-1));
        }
        callbackInfo.cancel();
    }

    @Inject(method = "setLockOn", at = @At("RETURN"), remap = false)
    private void epicThirdPerson$captureSuccessfulLockOn(boolean lockingOnTarget, CallbackInfo callbackInfo) {
        if (lockingOnTarget) {
            if (this.isLockingOnTarget() && this.epicThirdPerson$cameraBeforeLockOn == null) {
                this.epicThirdPerson$cameraBeforeLockOn =
                        EpicFightLeawindCompatibility.captureCameraBeforeBetterLockOn();
            } else if (!this.isLockingOnTarget()) {
                this.epicThirdPerson$cameraBeforeLockOn = null;
            }
        }
    }

    @TargetHandler(
            mixin = "net.shelmarow.betterlockon.mixins.EpicFightCameraAPIMixin",
            name = "rewroteClientTick",
            prefix = "handler"
    )
    @Redirect(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lyesman/epicfight/api/client/camera/EpicFightCameraAPI;focusingEntity:Lnet/minecraft/world/entity/LivingEntity;",
                    opcode = Opcodes.PUTFIELD
            ),
            require = 1,
            remap = false
    )
    private void epicThirdPerson$preventSoftTargetReacquire(
            EpicFightCameraAPI cameraApi,
            LivingEntity target
    ) {
        this.focusingEntity = EpicFightLeawindCompatibility.shouldReleaseBetterLockOnTargetImmediately()
                && !this.isLockingOnTarget()
                ? null
                : target;
    }

    @TargetHandler(
            mixin = "net.shelmarow.betterlockon.mixins.EpicFightCameraAPIMixin",
            name = "rewroteClientTick",
            prefix = "handler"
    )
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "FIELD",
                    target = "Lyesman/epicfight/api/client/camera/EpicFightCameraAPI;blo$maxUnlockDelayTick:I",
                    opcode = Opcodes.GETFIELD
            ),
            require = 2,
            remap = false
    )
    private int epicThirdPerson$removeOcclusionUnlockDelay(int originalDelay) {
        return EpicFightLeawindCompatibility.shouldReleaseBetterLockOnTargetImmediately()
                ? 1
                : originalDelay;
    }
}
