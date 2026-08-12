package com.guhao.epic_third_person.mixin.epicfight;

import com.guhao.epic_third_person.client.EpicFightLeawindCompatibility;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.entity.eventlistener.SkillCastEvent;

@OnlyIn(Dist.CLIENT)
@Mixin(value = SkillContainer.class, remap = false)
public abstract class SkillContainerMixin {
    @Shadow
    public abstract SkillSlot getSlot();

    @Inject(method = "sendCastRequest", at = @At("HEAD"), remap = false)
    private void epicThirdPerson$syncLockOnFacingBeforeBasicAttack(
            LocalPlayerPatch executor,
            ControlEngine controlEngine,
            CallbackInfoReturnable<SkillCastEvent> callbackInfo
    ) {
        if (this.getSlot() == SkillSlots.BASIC_ATTACK) {
            EpicFightLeawindCompatibility.applyPlayerFacing(executor.getOriginal(), true);
        }
    }
}
