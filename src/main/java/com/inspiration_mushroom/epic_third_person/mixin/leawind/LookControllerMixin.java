package com.inspiration_mushroom.epic_third_person.mixin.leawind;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.github.leawind.thirdperson.internal.logic.base.rotation.LookController;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LookController.class, remap = false)
public abstract class LookControllerMixin {
    @Unique
    private static final float EPIC_THIRD_PERSON$MAX_SAFE_PITCH = 89.0F;

    @ModifyExpressionValue(
            method = {"initialize(FF)V", "turn(DD)Z"},
            at = @At(
                    value = "INVOKE",
                    target = "Lio/github/leawind/thirdperson/internal/logic/base/rotation/PlayerRotationGeometry;clampPitch(F)F"
            ),
            remap = false
    )
    private float epicThirdPerson$avoidVerticalCameraSingularity(float pitch) {
        return Mth.clamp(
                pitch,
                -EPIC_THIRD_PERSON$MAX_SAFE_PITCH,
                EPIC_THIRD_PERSON$MAX_SAFE_PITCH
        );
    }
}
