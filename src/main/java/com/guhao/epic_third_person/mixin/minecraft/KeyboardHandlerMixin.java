package com.guhao.epic_third_person.mixin.minecraft;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
    @WrapWithCondition(
            method = "keyPress",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/ForgeHooksClient;onKeyInput(IIII)V",
                    remap = false
            )
    )
    private boolean epicThirdPerson$allowRangedSprintKeyEvent(
            int key,
            int scanCode,
            int action,
            int modifiers
    ) {
        return !EpicFightLeawindCompatibility.shouldSuppressRangedSprintKeyEvent(key, scanCode, action);
    }
}
