package com.inspiration_mushroom.epic_third_person.mixin.epicfight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.leawind.perspectiveapi.api.Perspective;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcher;
import io.github.leawind.perspectiveapi.api.PerspectiveSwitcherBehavior;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@Mixin(value = LocalPlayerPatch.class, remap = false)
public abstract class AutoCameraMixin {
    @Unique
    private static final String EPIC_THIRD_PERSON$LEAWIND_PERSPECTIVE_ID =
            "leawind_third_person.third_person";
    @Unique
    private static final String EPIC_THIRD_PERSON$FIRST_PERSON_PERSPECTIVE_ID =
            "perspective_api.first_person";

    @WrapOperation(
            method = "toEpicFightMode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;setCameraType(Lnet/minecraft/client/CameraType;)V",
                    remap = true
            ),
            remap = false
    )
    private void epicThirdPerson$enterLeawindThirdPerson(
            Options options,
            CameraType fallbackCameraType,
            Operation<Void> original
    ) {
        if (!epicThirdPerson$activatePerspective(EPIC_THIRD_PERSON$LEAWIND_PERSPECTIVE_ID)) {
            original.call(options, fallbackCameraType);
        }
    }

    @WrapOperation(
            method = "toVanillaMode",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Options;setCameraType(Lnet/minecraft/client/CameraType;)V",
                    remap = true
            ),
            remap = false
    )
    private void epicThirdPerson$returnToFirstPerson(
            Options options,
            CameraType fallbackCameraType,
            Operation<Void> original
    ) {
        if (!epicThirdPerson$activatePerspective(EPIC_THIRD_PERSON$FIRST_PERSON_PERSPECTIVE_ID)) {
            original.call(options, fallbackCameraType);
        }
    }

    @Unique
    private static boolean epicThirdPerson$activatePerspective(String perspectiveId) {
        if (!PerspectiveAPI.isEnabled()) {
            return false;
        }
        if (PerspectiveAPI.isCurrent(perspectiveId)) {
            return true;
        }

        try {
            var registry = PerspectiveAPI.getRegistry();
            if (!registry.contains(perspectiveId)) {
                return false;
            }

            Perspective perspective = registry.get(perspectiveId);
            if (perspective == null
                    || !perspective.isAvailable()
                    || !perspective.info().switchable()) {
                return false;
            }

            PerspectiveSwitcher switcher = PerspectiveAPI.getSwitcherManager().getSelectedSwitcher();
            if (!(switcher instanceof PerspectiveSwitcherBehavior behavior)) {
                return false;
            }

            behavior.onActivated(perspective);
            return true;
        } catch (IllegalStateException ignored) {
            return false;
        }
    }
}
