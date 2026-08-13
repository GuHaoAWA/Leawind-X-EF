package com.guhao.epic_third_person.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EpicThirdPersonClientConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue USE_LEAWIND_EIGHT_DIRECTION_MOVEMENT_WHILE_LOCKED;
    private static final ForgeConfigSpec.BooleanValue USE_LEAWIND_CAMERA_IN_EPIC_FIGHT_TPS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("lockOn");
        USE_LEAWIND_EIGHT_DIRECTION_MOVEMENT_WHILE_LOCKED = builder
                .comment(
                        "Use Leawind Third Person movement and body rotation while locked on.",
                        "true: the camera stays locked to the target while the body follows Leawind's eight-direction movement.",
                        "false: preserve the legacy behavior that delegates movement to Better Lock On and faces the target."
                )
                .define("useLeawindEightDirectionMovement", true);
        builder.pop();

        builder.push("tps");
        USE_LEAWIND_CAMERA_IN_EPIC_FIGHT_TPS = builder
                .comment(
                        "Let Leawind Third Person own the rendered camera while Epic Fight TPS mode is active.",
                        "true: preserve Epic Fight aiming, crosshair picking, and attack direction, but use Leawind camera and movement direction.",
                        "false: let Epic Fight apply its native TPS camera transform."
                )
                .define("useLeawindCamera", true);
        builder.pop();

        SPEC = builder.build();
    }

    private EpicThirdPersonClientConfig() {
    }

    public static boolean useLeawindEightDirectionMovementWhileLocked() {
        return USE_LEAWIND_EIGHT_DIRECTION_MOVEMENT_WHILE_LOCKED.get();
    }

    public static boolean useLeawindCameraInEpicFightTps() {
        return USE_LEAWIND_CAMERA_IN_EPIC_FIGHT_TPS.get();
    }
}
