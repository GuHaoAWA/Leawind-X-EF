package com.inspiration_mushroom.epic_third_person.mixin.minecraft;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OnlyIn(Dist.CLIENT)
@Mixin(PostChain.class)
public abstract class PostChainMixin {
    @Shadow
    @Final
    private RenderTarget screenTarget;

    @Inject(method = "process", at = @At("RETURN"))
    private void epicThirdPerson$restoreMainRenderTarget(
            float partialTick,
            CallbackInfo callbackInfo
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.screenTarget == minecraft.getMainRenderTarget()) {
            minecraft.getMainRenderTarget().bindWrite(false);
        }
    }
}
