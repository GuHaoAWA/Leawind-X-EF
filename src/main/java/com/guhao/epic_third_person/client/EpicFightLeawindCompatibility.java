package com.guhao.epic_third_person.client;

import com.guhao.epic_third_person.EpicThirdPerson;
import com.guhao.epic_third_person.config.EpicThirdPersonClientConfig;
import com.mojang.blaze3d.platform.InputConstants;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveMath;
import io.github.leawind.thirdperson.internal.logic.base.BaseRuntime;
import io.github.leawind.thirdperson.internal.logic.scheduler.MinecraftCameraAdjustmentIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.EpicFightClientHooks;
import yesman.epicfight.api.client.event.types.ActivateTPSCamera;
import yesman.epicfight.api.client.event.types.BuildCameraTransform;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

import java.util.Arrays;

@OnlyIn(Dist.CLIENT)
public final class EpicFightLeawindCompatibility {
    private static final String LEAWIND_PERSPECTIVE_ID = "leawind_third_person.third_person";
    private static final String BETTER_LOCK_ON_MOD_ID = "betterlockon";
    private static final String MODIFIER_ID = EpicThirdPerson.MODID + ":epic_fight_lock_on";
    private static final int EVENT_PRIORITY = 1_000;
    private static final long CAMERA_RETURN_DURATION_NANOS = 350_000_000L;
    private static final long ATTACK_PHASE_START_GRACE_NANOS = 750_000_000L;
    private static final float HORIZONTAL_PLAYER_PITCH = 0.1F;
    private static final float MAX_SAFE_CAMERA_PITCH = 89.0F;

    private static boolean initialized;
    private static boolean loggedGuardCameraDelegation;
    private static boolean loggedInteractionRotationSuppression;
    private static boolean loggedLockReleaseRecovery;
    private static boolean loggedRangedSprintPriority;
    private static CameraReturnAnimation cameraReturnAnimation;
    private static LocalPlayer eightDirectionAttackPlayer;
    private static float eightDirectionAttackYaw;
    private static float eightDirectionAttackPitch;
    private static long eightDirectionAttackStartedAtNanos;
    private static boolean eightDirectionAttackPhaseObserved;

    private EpicFightLeawindCompatibility() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
        EpicThirdPerson.LOGGER.info("Loaded Epic Fight camera compatibility");
        EpicFightClientHooks.Camera.BUILD_TRANSFORM_PRE.registerEvent(
                EpicFightLeawindCompatibility::preventEpicFightCameraTransform,
                MODIFIER_ID + "/camera-transform",
                EVENT_PRIORITY
        );
        EpicFightClientHooks.Camera.ACTIVATE_TPS_CAMERA.registerEvent(
                EpicFightLeawindCompatibility::preventEpicFightTpsCamera,
                MODIFIER_ID + "/tps-camera",
                EVENT_PRIORITY
        );
    }

    private static void preventEpicFightTpsCamera(ActivateTPSCamera event) {
        if (isLeawindPerspectiveActive()) {
            event.cancel();
        }
    }

    private static void preventEpicFightCameraTransform(BuildCameraTransform.Pre event) {
        if (!isLeawindPerspectiveActive()) {
            return;
        }

        EpicFightCameraAPI cameraApi = event.getCameraApi();
        if (cameraApi.isLockingOnTarget()
                || isEpicFightGuardUsingVanillaUseKey(Minecraft.getInstance().player)) {
            event.cancel();
        }
    }

    public static void applyCameraRotation(Quaternionf destination) {
        CameraRotation rotation = resolveLockOnCameraRotation();
        if (rotation != null) {
            cameraReturnAnimation = null;
        } else {
            rotation = resolveCameraReturnRotation();
        }

        if (rotation != null) {
            PerspectiveMath.eulerDegToQuat(
                    rotation.pitch(),
                    rotation.yaw(),
                    0.0F,
                    destination
            );
        }
    }

    public static CameraRotation resolveLockOnCameraRotation() {
        TargetRotation rotation = resolveTargetRotation();
        if (rotation == null) {
            return null;
        }

        CameraRotation betterLockOnRotation = resolveBetterLockOnCameraRotation();
        if (betterLockOnRotation != null) {
            return new CameraRotation(
                    betterLockOnRotation.yaw(),
                    clampSafeCameraPitch(betterLockOnRotation.pitch())
            );
        }

        if (InputManager.isActionActive(EpicFightInputAction.LOCK_ON_SHIFT_FREELY)) {
            return null;
        }

        return new CameraRotation(rotation.yaw(), Mth.clamp(rotation.pitch(), -30.0F, 30.0F));
    }

    public static CameraRotation resolveLockedMovementCameraRotation() {
        LocalPlayer player = Minecraft.getInstance().player;
        return shouldUseLeawindEightDirectionMovement(player)
                ? resolveLockOnCameraRotation()
                : null;
    }

    public static void applyPlayerFacing(LocalPlayer player, boolean synchronize) {
        if (shouldUseLeawindEightDirectionMovement(player)) {
            return;
        }
        TargetRotation rotation = resolveTargetRotation();
        if (rotation == null || player != Minecraft.getInstance().player) {
            return;
        }

        float yaw = rotation.yaw();
        float pitch = rotation.pitch();
        player.setYRot(yaw);
        player.yRotO = yaw;
        player.setXRot(pitch);
        player.xRotO = pitch;
        player.setYHeadRot(yaw);
        player.yHeadRotO = yaw;
        player.setYBodyRot(yaw);
        player.yBodyRotO = yaw;

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch != null) {
            playerPatch.setYRotO(yaw);
            playerPatch.setModelYRot(yaw, synchronize);
        }

        if (synchronize) {
            player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround()));
        }
    }

    public static boolean deferPlayerFacingToBetterLockOn(LocalPlayer player) {
        if (!isBetterLockOnActive(player) || shouldUseLeawindEightDirectionMovement(player)) {
            return false;
        }

        BaseRuntime.getInstance().session().playerRotationController().reset();
        return true;
    }

    public static boolean deferMovementToBetterLockOn(LocalPlayer player) {
        return isBetterLockOnActive(player) && !shouldUseLeawindEightDirectionMovement(player);
    }

    public static boolean shouldReleaseBetterLockOnTargetImmediately() {
        return shouldReleaseEpicFightTargetImmediately();
    }

    public static boolean shouldReleaseEpicFightTargetImmediately() {
        return isLeawindPerspectiveActive();
    }

    public static boolean shouldHandleNativeLockOnLifecycle() {
        return !isBetterLockOnLoaded();
    }

    public static boolean shouldSuppressLeawindInteractionRotation() {
        Minecraft minecraft = Minecraft.getInstance();
        if (!isLeawindPerspectiveActive()) {
            return false;
        }

        LocalPlayer player = minecraft.player;
        boolean attackInputActive = isEpicFightAttackInputActive(player);
        if (attackInputActive && player != eightDirectionAttackPlayer) {
            captureEightDirectionAttackFacing(player);
        }

        boolean suppress = attackInputActive
                || resolveTargetRotation() != null
                || isEpicFightGuardUsingVanillaUseKey(minecraft.player);
        if (suppress && !loggedInteractionRotationSuppression) {
            loggedInteractionRotationSuppression = true;
            EpicThirdPerson.LOGGER.info(
                    "Suppressed Leawind interaction rotation while Epic Fight owns combat facing"
            );
        }
        return suppress;
    }

    public static void captureEightDirectionAttackFacing(LocalPlayer player) {
        if (!shouldPreserveLeawindAttackFacing(player)) {
            resetEightDirectionAttackFacing();
            return;
        }

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        float yaw = playerPatch == null ? player.getYRot() : playerPatch.getModelYRot();
        float pitch = player.getXRot();
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            resetEightDirectionAttackFacing();
            return;
        }

        eightDirectionAttackPlayer = player;
        eightDirectionAttackYaw = Mth.wrapDegrees(yaw);
        eightDirectionAttackPitch = clampSafeCameraPitch(pitch);
        eightDirectionAttackStartedAtNanos = System.nanoTime();
        eightDirectionAttackPhaseObserved = false;
    }

    public static void clearEightDirectionAttackFacing(LocalPlayer player) {
        if (player == null || player == eightDirectionAttackPlayer) {
            resetEightDirectionAttackFacing();
        }
    }

    public static boolean shouldPreserveEightDirectionAttackActionYaw(LocalPlayer player) {
        return player != null
                && player == eightDirectionAttackPlayer
                && shouldPreserveLeawindAttackFacing(player);
    }

    public static CameraRotation resolveStationaryEightDirectionAttackFacing(LocalPlayer player) {
        if (player == null
                || player != eightDirectionAttackPlayer
                || !shouldPreserveLeawindAttackFacing(player)) {
            resetEightDirectionAttackFacing();
            return null;
        }

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch == null) {
            resetEightDirectionAttackFacing();
            return null;
        }

        if (playerPatch.getEntityState().getLevel() > 0) {
            eightDirectionAttackPhaseObserved = true;
        } else if (eightDirectionAttackPhaseObserved
                || System.nanoTime() - eightDirectionAttackStartedAtNanos
                >= ATTACK_PHASE_START_GRACE_NANOS) {
            resetEightDirectionAttackFacing();
            return null;
        }

        var movementIntent = BaseRuntime.getInstance().session().movementIntent().orElse(null);
        if (movementIntent != null && movementIntent.hasDirectionalImpulse(1.0E-5D)) {
            movementIntent.facingYawDegrees().ifPresent(
                    yaw -> eightDirectionAttackYaw = Mth.wrapDegrees((float) yaw)
            );
            if (Float.isFinite(player.getXRot())) {
                eightDirectionAttackPitch = clampSafeCameraPitch(player.getXRot());
            }
            return null;
        }

        return new CameraRotation(eightDirectionAttackYaw, eightDirectionAttackPitch);
    }

    public static PlayerControlRotation resolveIndependentPlayerControlRotation(LocalPlayer player) {
        CameraRotation attackRotation = resolveStationaryEightDirectionAttackFacing(player);
        if (attackRotation != null) {
            return new PlayerControlRotation(attackRotation, false);
        }

        if (!shouldUseLeawindEightDirectionMovement(player)) {
            return null;
        }

        var movementIntent = BaseRuntime.getInstance().session().movementIntent().orElse(null);
        if (movementIntent != null && movementIntent.hasDirectionalImpulse(1.0E-5D)) {
            var movementYaw = movementIntent.facingYawDegrees();
            if (movementYaw.isPresent()) {
                return new PlayerControlRotation(
                        new CameraRotation(
                                Mth.wrapDegrees((float) movementYaw.getAsDouble()),
                                HORIZONTAL_PLAYER_PITCH
                        ),
                        true
                );
            }
        }

        float yaw = player.getYRot();
        float pitch = player.getXRot();
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            return null;
        }

        return new PlayerControlRotation(
                new CameraRotation(Mth.wrapDegrees(yaw), clampSafeCameraPitch(pitch)),
                false
        );
    }

    private static void resetEightDirectionAttackFacing() {
        eightDirectionAttackPlayer = null;
        eightDirectionAttackYaw = 0.0F;
        eightDirectionAttackPitch = 0.0F;
        eightDirectionAttackStartedAtNanos = 0L;
        eightDirectionAttackPhaseObserved = false;
    }

    public static boolean shouldPrioritizeRangedSprintInput(InputConstants.Key inputKey) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return minecraft.options.keySprint.getKey().equals(inputKey)
                && shouldPrioritizeRangedSprintInput(minecraft, player);
    }

    public static boolean shouldSuppressRangedSprintKeyEvent(int key, int scanCode, int action) {
        return action != 0
                && shouldPrioritizeRangedSprintInput(InputConstants.getKey(key, scanCode));
    }

    public static boolean handleEpicFightCameraInput(
            EpicFightCameraAPI cameraApi,
            double yawDelta,
            double pitchDelta
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && MinecraftCameraAdjustmentIntegration.onTurn(player, yawDelta, pitchDelta)) {
            return true;
        }

        if (!isLeawindPerspectiveCurrent()
                || hasValidLockOnTarget(cameraApi)) {
            return false;
        }

        boolean guardCamera = isEpicFightGuardUsingVanillaUseKey(player);
        if (!guardCamera) {
            return false;
        }

        var runtime = BaseRuntime.getInstance();
        if (!runtime.session().isPerspectiveActive()) {
            runtime.onPerspectiveActivated();
        }

        var lookController = runtime.session().lookController();
        if (!lookController.isInitialized()) {
            CameraRotation rotation = resolveDisplayedCameraRotation();
            if (rotation == null) {
                rotation = resolveEpicFightCameraRotation(cameraApi);
            }
            if (rotation != null) {
                lookController.initialize(rotation.pitch(), rotation.yaw());
            }
        }

        cameraReturnAnimation = null;
        if (!lookController.turn(yawDelta, pitchDelta)) {
            return false;
        }

        cameraApi.setCameraRotations(
                lookController.pitchDegrees(),
                lookController.yawDegrees(),
                true
        );
        if (guardCamera && !loggedGuardCameraDelegation) {
            loggedGuardCameraDelegation = true;
            EpicThirdPerson.LOGGER.info(
                    "Applied Epic Fight guard camera input directly to Leawind Third Person"
            );
        }
        return true;
    }

    private static boolean isEpicFightGuardUsingVanillaUseKey(LocalPlayer player) {
        Minecraft minecraft = Minecraft.getInstance();
        if (player == null
                || !InputManager.isBoundToSamePhysicalInput(
                EpicFightInputAction.GUARD,
                MinecraftInputAction.USE
        )
                || !minecraft.options.keyUse.isDown()
                && !InputManager.isActionActive(EpicFightInputAction.GUARD)) {
            return false;
        }

        if (isChargeableRangedItem(player.getUseItem())
                || isChargeableRangedItem(player.getMainHandItem())
                || isChargeableRangedItem(player.getOffhandItem())) {
            return false;
        }

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch == null || !playerPatch.isEpicFightMode()) {
            return false;
        }

        if (player.getMainHandItem().getItem() instanceof ShieldItem
                || player.getOffhandItem().getItem() instanceof ShieldItem) {
            return false;
        }

        SkillContainer guard = playerPatch.getSkill(SkillSlots.GUARD);
        if (guard == null || guard.isEmpty()) {
            return false;
        }

        boolean holdingGuard = guard.isActivated() || playerPatch.isHoldingSkill(guard.getSkill());
        return holdingGuard || !playerPatch.isHoldingAny() && guard.getSkill().canExecute(guard);
    }

    private static boolean shouldPrioritizeRangedSprintInput(Minecraft minecraft, LocalPlayer player) {
        if (player == null
                || minecraft.screen != null
                || !isLeawindPerspectiveActive()
                || !player.isUsingItem()
                || !isChargeableRangedItem(player.getUseItem())
                || !hasPhysicalBindingConflict(minecraft.options.keySprint)) {
            return false;
        }

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch == null || !playerPatch.isEpicFightMode()) {
            return false;
        }

        if (!loggedRangedSprintPriority) {
            loggedRangedSprintPriority = true;
            EpicThirdPerson.LOGGER.info(
                    "Prioritized bow and crossbow charging over conflicting sprint-key combat actions"
            );
        }
        return true;
    }

    private static boolean hasPhysicalBindingConflict(KeyMapping prioritizedMapping) {
        return Arrays.stream(Minecraft.getInstance().options.keyMappings)
                .anyMatch(keyMapping -> keyMapping != prioritizedMapping
                        && keyMapping.getKey().equals(prioritizedMapping.getKey()));
    }

    private static boolean isChargeableRangedItem(ItemStack itemStack) {
        UseAnim useAnimation = itemStack.getUseAnimation();
        return useAnimation == UseAnim.BOW || useAnimation == UseAnim.CROSSBOW;
    }

    public static void synchronizeAfterNativeLockRelease(
            EpicFightCameraAPI cameraApi,
            CameraRotation returnRotation
    ) {
        if (!shouldReleaseEpicFightTargetImmediately()) {
            return;
        }

        restoreCameraAfterLockOnRelease(cameraApi, returnRotation);
    }

    public static CameraRotation captureCameraBeforeLockOn() {
        cameraReturnAnimation = null;
        if (!isLeawindPerspectiveActive()) {
            return null;
        }

        CameraRotation controlRotation = resolveControlCameraRotation();
        if (controlRotation != null) {
            return controlRotation;
        }

        CameraRotation displayedRotation = resolveDisplayedCameraRotation();
        if (displayedRotation != null) {
            return displayedRotation;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        return player == null
                ? null
                : new CameraRotation(player.getYRot(), clampSafeCameraPitch(player.getXRot()));
    }

    public static void restoreCameraAfterLockOnRelease(
            EpicFightCameraAPI cameraApi,
            CameraRotation returnRotation
    ) {
        if (!isLeawindPerspectiveActive()) {
            return;
        }

        var session = BaseRuntime.getInstance().session();
        var lookController = session.lookController();
        CameraRotation releasedRotation = resolveDisplayedCameraRotation();
        if (releasedRotation == null) {
            releasedRotation = resolveEpicFightCameraRotation(cameraApi);
        }

        if (returnRotation == null) {
            returnRotation = resolveControlCameraRotation();
        }
        if (returnRotation == null) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                returnRotation = new CameraRotation(
                        player.getYRot(),
                        clampSafeCameraPitch(player.getXRot())
                );
            }
        }

        if (returnRotation != null) {
            returnRotation = new CameraRotation(
                    Mth.wrapDegrees(returnRotation.yaw()),
                    clampSafeCameraPitch(returnRotation.pitch())
            );
            lookController.initialize(returnRotation.pitch(), returnRotation.yaw());
            cameraApi.setCameraRotations(returnRotation.pitch(), returnRotation.yaw(), true);
        }

        session.playerRotationController().reset();
        session.clearMovementIntent();
        cameraReturnAnimation = releasedRotation == null || returnRotation == null
                ? null
                : new CameraReturnAnimation(releasedRotation, System.nanoTime());

        if (!loggedLockReleaseRecovery) {
            loggedLockReleaseRecovery = true;
            EpicThirdPerson.LOGGER.info(
                    "Restored Leawind camera smoothly after Epic Fight lock-on release"
            );
        }
    }

    private static CameraRotation resolveCameraReturnRotation() {
        CameraReturnAnimation animation = cameraReturnAnimation;
        if (animation == null) {
            return null;
        }

        var session = BaseRuntime.getInstance().session();
        var lookController = session.lookController();
        if (!isLeawindPerspectiveActive() || !lookController.isInitialized()) {
            cameraReturnAnimation = null;
            return null;
        }

        float progress = Mth.clamp(
                (float) ((System.nanoTime() - animation.startedAtNanos()) / (double) CAMERA_RETURN_DURATION_NANOS),
                0.0F,
                1.0F
        );
        if (progress >= 1.0F) {
            cameraReturnAnimation = null;
            return null;
        }

        float easedProgress = progress * progress * (3.0F - 2.0F * progress);
        float targetYaw = lookController.yawDegrees();
        float targetPitch = lookController.pitchDegrees();
        float yaw = Mth.wrapDegrees(
                animation.start().yaw()
                        + Mth.wrapDegrees(targetYaw - animation.start().yaw()) * easedProgress
        );
        float pitch = clampSafeCameraPitch(
                Mth.lerp(easedProgress, animation.start().pitch(), targetPitch)
        );
        return new CameraRotation(yaw, pitch);
    }

    private static CameraRotation resolveDisplayedCameraRotation() {
        return BaseRuntime.getInstance().session().finalCameraPose()
                .map(cameraPose -> cameraPose.copyRotation(new Quaternionf()))
                .map(rotation -> PerspectiveMath.toEulerDeg(rotation, new Vector2f()))
                .filter(rotation -> Float.isFinite(rotation.x()) && Float.isFinite(rotation.y()))
                .map(rotation -> new CameraRotation(
                        Mth.wrapDegrees(rotation.y()),
                        clampSafeCameraPitch(rotation.x())
                ))
                .orElse(null);
    }

    private static CameraRotation resolveControlCameraRotation() {
        var lookController = BaseRuntime.getInstance().session().lookController();
        if (!lookController.isInitialized()) {
            return null;
        }

        float yaw = lookController.yawDegrees();
        float pitch = lookController.pitchDegrees();
        return Float.isFinite(yaw) && Float.isFinite(pitch)
                ? new CameraRotation(Mth.wrapDegrees(yaw), clampSafeCameraPitch(pitch))
                : null;
    }

    private static CameraRotation resolveBetterLockOnCameraRotation() {
        if (!isBetterLockOnLoaded()) {
            return null;
        }

        return resolveEpicFightCameraRotation(EpicFightCameraAPI.getInstance());
    }

    private static CameraRotation resolveEpicFightCameraRotation(EpicFightCameraAPI cameraApi) {
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getFrameTime();
        float pitch = Mth.rotLerp(partialTick, cameraApi.getCameraXRotO(), cameraApi.getCameraXRot());
        float yaw = Mth.rotLerp(partialTick, cameraApi.getCameraYRotO(), cameraApi.getCameraYRot());
        return Float.isFinite(pitch) && Float.isFinite(yaw)
                ? new CameraRotation(Mth.wrapDegrees(yaw), clampSafeCameraPitch(pitch))
                : null;
    }

    private static boolean isBetterLockOnActive(LocalPlayer player) {
        if (!isBetterLockOnLoaded() || player != Minecraft.getInstance().player) {
            return false;
        }

        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        return isLeawindPerspectiveActive() && hasValidLockOnTarget(cameraApi);
    }

    private static boolean hasValidLockOnTarget(EpicFightCameraAPI cameraApi) {
        LivingEntity target = cameraApi.getFocusingEntity();
        return cameraApi.isLockingOnTarget()
                && target != null
                && target.isAlive()
                && !target.isRemoved();
    }

    public static boolean shouldUseLeawindEightDirectionMovement(LocalPlayer player) {
        return EpicThirdPersonClientConfig.useLeawindEightDirectionMovementWhileLocked()
                && player != null
                && player == Minecraft.getInstance().player
                && resolveTargetRotation() != null;
    }

    private static boolean shouldPreserveLeawindAttackFacing(LocalPlayer player) {
        if (player == null
                || player != Minecraft.getInstance().player
                || !isLeawindPerspectiveActive()) {
            return false;
        }

        LocalPlayerPatch playerPatch = EpicFightCapabilities.getLocalPlayerPatch(player);
        if (playerPatch == null || !playerPatch.isEpicFightMode()) {
            return false;
        }

        return !hasValidLockOnTarget(EpicFightCameraAPI.getInstance())
                || EpicThirdPersonClientConfig.useLeawindEightDirectionMovementWhileLocked();
    }

    private static boolean isEpicFightAttackInputActive(LocalPlayer player) {
        return Minecraft.getInstance().screen == null
                && shouldPreserveLeawindAttackFacing(player)
                && InputManager.isActionPhysicallyActive(EpicFightInputAction.ATTACK);
    }

    private static boolean isBetterLockOnLoaded() {
        return ModList.get().isLoaded(BETTER_LOCK_ON_MOD_ID);
    }

    private static TargetRotation resolveTargetRotation() {
        if (!isLeawindPerspectiveActive()) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        LivingEntity target = cameraApi.getFocusingEntity();
        if (player == null || !cameraApi.isLockingOnTarget() || target == null || !target.isAlive() || target.isRemoved()) {
            return null;
        }

        float partialTick = minecraft.getFrameTime();
        Vec3 direction = target.getEyePosition(partialTick).subtract(player.getEyePosition(partialTick));
        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        if (horizontalDistance < 1.0E-6D && Math.abs(direction.y) < 1.0E-6D) {
            return null;
        }

        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) (-(Mth.atan2(direction.y, horizontalDistance) * Mth.RAD_TO_DEG));
        return new TargetRotation(Mth.wrapDegrees(yaw), clampSafeCameraPitch(pitch));
    }

    private static float clampSafeCameraPitch(float pitch) {
        return Mth.clamp(pitch, -MAX_SAFE_CAMERA_PITCH, MAX_SAFE_CAMERA_PITCH);
    }

    private static boolean isLeawindPerspectiveActive() {
        return isLeawindPerspectiveCurrent();
    }

    private static boolean isLeawindPerspectiveCurrent() {
        return PerspectiveAPI.isEnabled()
                && PerspectiveAPI.isCurrent(LEAWIND_PERSPECTIVE_ID);
    }

    private record TargetRotation(float yaw, float pitch) {
    }

    private record CameraReturnAnimation(CameraRotation start, long startedAtNanos) {
    }

    public record PlayerControlRotation(CameraRotation rotation, boolean moving) {
    }

    public record CameraRotation(float yaw, float pitch) {
    }
}
