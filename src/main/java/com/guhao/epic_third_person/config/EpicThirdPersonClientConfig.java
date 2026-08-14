package com.guhao.epic_third_person.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class EpicThirdPersonClientConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue USE_LEAWIND_EIGHT_DIRECTION_MOVEMENT_WHILE_LOCKED;

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

        SPEC = builder.build();
    }

    private EpicThirdPersonClientConfig() {
    }

    public static boolean useLeawindEightDirectionMovementWhileLocked() {
        return USE_LEAWIND_EIGHT_DIRECTION_MOVEMENT_WHILE_LOCKED.get();
    }

    public static boolean toggleLeawindEightDirectionMovementWhileLocked() {
        return setLeawindEightDirectionMovementWhileLocked(
                !useLeawindEightDirectionMovementWhileLocked()
        );
    }

    public static boolean setLeawindEightDirectionMovementWhileLocked(boolean enabled) {
        if (useLeawindEightDirectionMovementWhileLocked() != enabled) {
            USE_LEAWIND_EIGHT_DIRECTION_MOVEMENT_WHILE_LOCKED.set(enabled);
            USE_LEAWIND_EIGHT_DIRECTION_MOVEMENT_WHILE_LOCKED.save();
        }
        return enabled;
    }
}
