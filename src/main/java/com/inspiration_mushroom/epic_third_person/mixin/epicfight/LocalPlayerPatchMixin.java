package com.inspiration_mushroom.epic_third_person.mixin.epicfight;

import com.inspiration_mushroom.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@OnlyIn(Dist.CLIENT)
@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class LocalPlayerPatchMixin {
    @Inject(method = "beginAction", at = @At("HEAD"), require = 0, remap = false)
    private void epicThirdPerson$captureEightDirectionAttackFacing(
            ActionAnimation animation,
            CallbackInfo callbackInfo
    ) {
        if (animation instanceof AttackAnimation) {
            LocalPlayerPatch playerPatch = (LocalPlayerPatch) (Object) this;
            EpicFightLeawindCompatibility.captureEightDirectionAttackFacing(playerPatch.getOriginal());
        } else {
            LocalPlayerPatch playerPatch = (LocalPlayerPatch) (Object) this;
            EpicFightLeawindCompatibility.clearEightDirectionAttackFacing(playerPatch.getOriginal());
        }
    }

    @WrapOperation(
            method = "beginAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;setYRot(F)V",
                    remap = true
            ),
            require = 0,
            remap = false
    )
    private void epicThirdPerson$preserveEightDirectionActionYaw(
            LocalPlayer player,
            float targetYaw,
            Operation<Void> original
    ) {
        if (!EpicFightLeawindCompatibility.shouldPreserveEightDirectionAttackActionYaw(player)) {
            original.call(player, targetYaw);
        }
    }
}
