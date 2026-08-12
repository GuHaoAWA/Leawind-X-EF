package com.guhao.epic_third_person.mixin.epicfight;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;

@OnlyIn(Dist.CLIENT)
@Mixin(value = EpicFightCameraAPI.class, remap = false)
public abstract class EpicFightCameraAPIMixin {
    @Redirect(
            method = "postClientTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;setXRot(F)V",
                    ordinal = 1,
                    remap = true
            ),
            require = 1,
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
            require = 1,
            remap = false
    )
    private void epicThirdPerson$preserveLeawindYaw(LocalPlayer player, float yaw) {
        if (!EpicFightLeawindCompatibility.shouldUseLeawindEightDirectionMovement(player)) {
            player.setYRot(yaw);
        }
    }
}
