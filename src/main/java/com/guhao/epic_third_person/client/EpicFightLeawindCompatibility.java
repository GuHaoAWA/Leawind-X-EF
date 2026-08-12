package com.guhao.epic_third_person.client;

import com.guhao.epic_third_person.EpicThirdPerson;
import com.guhao.epic_third_person.config.EpicThirdPersonClientConfig;
import io.github.leawind.perspectiveapi.api.PerspectiveAPI;
import io.github.leawind.perspectiveapi.api.PerspectiveMath;
import io.github.leawind.thirdperson.internal.logic.base.BaseRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;

@OnlyIn(Dist.CLIENT)
public final class EpicFightLeawindCompatibility {
    private static final String LEAWIND_PERSPECTIVE_ID = "leawind_third_person.third_person";
    private static final String BETTER_LOCK_ON_MOD_ID = "betterlockon";
    private static final String MODIFIER_ID = EpicThirdPerson.MODID + ":epic_fight_lock_on";
    private static final int EVENT_PRIORITY = 1_000;
    private static final long CAMERA_RETURN_DURATION_NANOS = 350_000_000L;

    private static boolean initialized;
    private static CameraReturnAnimation cameraReturnAnimation;

    private EpicFightLeawindCompatibility() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }

        initialized = true;
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
        if (!isBetterLockOnLoaded()
                && isLeawindPerspectiveActive()
                && event.getCameraApi().isLockingOnTarget()) {
            event.cancel();
        }
    }

    private static void preventEpicFightCameraTransform(BuildCameraTransform.Pre event) {
        if (isLeawindPerspectiveActive() && event.getCameraApi().isLockingOnTarget()) {
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
            return betterLockOnRotation;
        }

        if (InputManager.isActionActive(EpicFightInputAction.LOCK_ON_SHIFT_FREELY)) {
            return null;
        }

        return new CameraRotation(rotation.yaw(), Mth.clamp(rotation.pitch(), -30.0F, 30.0F));
    }

    public static CameraRotation resolveDisplayedLockOnCameraRotation() {
        CameraRotation targetRotation = resolveLockOnCameraRotation();
        if (targetRotation == null) {
            return null;
        }

        CameraRotation displayedRotation = resolveDisplayedCameraRotation();
        return displayedRotation == null ? targetRotation : displayedRotation;
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
        return isLeawindPerspectiveActive();
    }

    public static boolean shouldSuppressLeawindInteractionRotation() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.options.keyUse.isDown() && resolveTargetRotation() != null;
    }

    public static CameraRotation captureCameraBeforeBetterLockOn() {
        cameraReturnAnimation = null;
        if (!isLeawindPerspectiveActive()) {
            return null;
        }

        var session = BaseRuntime.getInstance().session();
        CameraRotation displayedRotation = resolveDisplayedCameraRotation();
        if (displayedRotation != null) {
            return displayedRotation;
        }

        var lookController = session.lookController();
        if (lookController.isInitialized()) {
            return new CameraRotation(lookController.yawDegrees(), lookController.pitchDegrees());
        }

        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : new CameraRotation(player.getYRot(), player.getXRot());
    }

    public static void restoreCameraAfterBetterLockOnRelease(CameraRotation rotation) {
        if (rotation == null || !isLeawindPerspectiveActive()) {
            return;
        }

        var session = BaseRuntime.getInstance().session();
        CameraRotation displayedRotation = resolveDisplayedCameraRotation();
        if (displayedRotation == null) {
            displayedRotation = resolveBetterLockOnCameraRotation();
        }

        session.lookController().initialize(rotation.pitch(), rotation.yaw());
        session.playerRotationController().reset();
        session.clearMovementIntent();
        cameraReturnAnimation = displayedRotation == null
                ? null
                : new CameraReturnAnimation(displayedRotation, System.nanoTime());
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
        float pitch = Mth.lerp(easedProgress, animation.start().pitch(), targetPitch);
        return new CameraRotation(yaw, pitch);
    }

    private static CameraRotation resolveDisplayedCameraRotation() {
        return BaseRuntime.getInstance().session().finalCameraPose()
                .map(cameraPose -> cameraPose.copyRotation(new Quaternionf()))
                .map(rotation -> PerspectiveMath.toEulerDeg(rotation, new Vector2f()))
                .filter(rotation -> Float.isFinite(rotation.x()) && Float.isFinite(rotation.y()))
                .map(rotation -> new CameraRotation(rotation.y(), rotation.x()))
                .orElse(null);
    }

    private static CameraRotation resolveBetterLockOnCameraRotation() {
        if (!isBetterLockOnLoaded()) {
            return null;
        }

        Minecraft minecraft = Minecraft.getInstance();
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        float partialTick = minecraft.getFrameTime();
        float pitch = Mth.rotLerp(partialTick, cameraApi.getCameraXRotO(), cameraApi.getCameraXRot());
        float yaw = Mth.rotLerp(partialTick, cameraApi.getCameraYRotO(), cameraApi.getCameraYRot());
        return Float.isFinite(pitch) && Float.isFinite(yaw)
                ? new CameraRotation(yaw, pitch)
                : null;
    }

    private static boolean isBetterLockOnActive(LocalPlayer player) {
        if (!isBetterLockOnLoaded() || player != Minecraft.getInstance().player) {
            return false;
        }

        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        LivingEntity target = cameraApi.getFocusingEntity();
        return isLeawindPerspectiveActive()
                && cameraApi.isLockingOnTarget()
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
        return new TargetRotation(yaw, Mth.clamp(pitch, -90.0F, 90.0F));
    }

    private static boolean isLeawindPerspectiveActive() {
        return PerspectiveAPI.isCurrent(LEAWIND_PERSPECTIVE_ID);
    }

    private record TargetRotation(float yaw, float pitch) {
    }

    private record CameraReturnAnimation(CameraRotation start, long startedAtNanos) {
    }

    public record CameraRotation(float yaw, float pitch) {
    }
}
