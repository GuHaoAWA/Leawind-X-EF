package com.guhao.epic_third_person.mixin.epicfight;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPSetPlayerTarget;

@OnlyIn(Dist.CLIENT)
@Mixin(value = EpicFightCameraAPI.class, priority = 2_000, remap = false)
public abstract class EpicFightCameraAPIMixin {
    @Shadow
    private LivingEntity focusingEntity;

    @Shadow
    private boolean lockingOnTarget;

    @Inject(method = "setLockOn", at = @At("RETURN"), remap = false)
    private void epicThirdPerson$clearReleasedNativeTarget(
            boolean requestedLockOn,
            CallbackInfo callbackInfo
    ) {
        if (requestedLockOn || !EpicFightLeawindCompatibility.shouldReleaseEpicFightTargetImmediately()) {
            return;
        }

        boolean hadRetainedTarget = this.lockingOnTarget || this.focusingEntity != null;
        this.lockingOnTarget = false;
        this.focusingEntity = null;
        EpicFightLeawindCompatibility.synchronizeAfterNativeLockRelease(
                (EpicFightCameraAPI) (Object) this
        );
        if (hadRetainedTarget) {
            EpicFightNetworkManager.sendToServer(new CPSetPlayerTarget(-1));
        }
    }

    @Inject(method = "turnCamera", at = @At("HEAD"), cancellable = true, remap = false)
    private void epicThirdPerson$delegateCameraInputToLeawind(
            double yawDelta,
            double pitchDelta,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        EpicFightCameraAPI cameraApi = (EpicFightCameraAPI) (Object) this;
        if (EpicFightLeawindCompatibility.handleEpicFightCameraInput(
                cameraApi,
                yawDelta,
                pitchDelta
        )) {
            callbackInfo.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(
            method = "getRelativeMove",
            at = @At(
                    value = "INVOKE",
                    target = "Lyesman/epicfight/api/client/camera/EpicFightCameraAPI;isTPSMode()Z"
            ),
            require = 0,
            remap = false
    )
    private boolean epicThirdPerson$usePlayerFacingForLeawindTpsMovement(boolean tpsMode) {
        return tpsMode && !EpicFightLeawindCompatibility.shouldUseLeawindMovementInEpicFightTps(
                Minecraft.getInstance().player
        );
    }

    @Redirect(
            method = "postClientTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;setXRot(F)V",
                    ordinal = 1,
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private void epicThirdPerson$preserveLeawindPitch(LocalPlayer player, float pitch) {
        if (!EpicFightLeawindCompatibility.shouldUseLeawindEightDirectionMovement(player)) {
            player.setXRot(pitch);
        }
    }

    @Redirect(
            method = "postClientTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;setYRot(F)V",
                    ordinal = 1,
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private void epicThirdPerson$preserveLeawindYaw(LocalPlayer player, float yaw) {
        if (!EpicFightLeawindCompatibility.shouldUseLeawindEightDirectionMovement(player)) {
            player.setYRot(yaw);
        }
    }
}
