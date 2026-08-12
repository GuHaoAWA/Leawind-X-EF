package com.guhao.epic_third_person.mixin.betterlockon;

import com.bawnorton.mixinsquared.TargetHandler;
import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = LocalPlayer.class, priority = 1_500)
public abstract class BetterLockOnLocalPlayerMixin {
    @TargetHandler(
            mixin = "net.shelmarow.betterlockon.mixins.LocalPlayerMixin",
            name = "aiStep",
            prefix = "redirect"
    )
    @Inject(
            method = "@MixinSquared:Handler",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void epicThirdPerson$keepSprintingWithEightDirectionMovement(
            Input input,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!EpicFightLeawindCompatibility.shouldUseLeawindEightDirectionMovement(player)) {
            return;
        }

        callbackInfo.setReturnValue(
                Math.abs(input.forwardImpulse) > 1.0E-5F
                        || Math.abs(input.leftImpulse) > 1.0E-5F
        );
    }
}
