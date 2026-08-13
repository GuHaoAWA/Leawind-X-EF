package com.guhao.epic_third_person.mixin.minecraft;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    @ModifyExpressionValue(
            method = "set",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/settings/KeyMappingLookup;getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Ljava/util/List;",
                    remap = false
            )
    )
    private static List<KeyMapping> epicThirdPerson$prioritizeRangedInputState(
            List<KeyMapping> mappings,
            InputConstants.Key inputKey,
            boolean held
    ) {
        if (!held || !EpicFightLeawindCompatibility.shouldPrioritizeRangedSprintInput(inputKey)) {
            return mappings;
        }

        return List.of(Minecraft.getInstance().options.keySprint);
    }

    @ModifyExpressionValue(
            method = "click",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/settings/KeyMappingLookup;getAll(Lcom/mojang/blaze3d/platform/InputConstants$Key;)Ljava/util/List;",
                    remap = false
            )
    )
    private static List<KeyMapping> epicThirdPerson$prioritizeRangedInputClick(
            List<KeyMapping> mappings,
            InputConstants.Key inputKey
    ) {
        if (!EpicFightLeawindCompatibility.shouldPrioritizeRangedSprintInput(inputKey)) {
            return mappings;
        }

        return List.of(Minecraft.getInstance().options.keySprint);
    }
}
