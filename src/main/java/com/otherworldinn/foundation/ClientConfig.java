package com.otherworldinn.foundation;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ClientConfig {
    public static final ClientConfig INSTANCE;
    public static final ModConfigSpec SPEC;

    static {
        final Pair<ClientConfig, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(ClientConfig::new);
        SPEC = specPair.getRight();
        INSTANCE = specPair.getLeft();
    }

    public final ModConfigSpec.DoubleValue cameraX;
    public final ModConfigSpec.DoubleValue cameraY;
    public final ModConfigSpec.DoubleValue cameraZ;
    public final ModConfigSpec.DoubleValue cameraPitch;
    public final ModConfigSpec.DoubleValue cameraYaw;
    public final ModConfigSpec.DoubleValue orthoSize;

    public ClientConfig(ModConfigSpec.Builder builder) {
        builder.push("camera");
        cameraX = builder.comment("Camera X Position")
                .defineInRange("x", -46.188, -100000.0, 100000.0);
        cameraY = builder.comment("Camera Y Position")
                .defineInRange("y", 116.188, -64.0, 1000.0);
        cameraZ = builder.comment("Camera Z Position")
                .defineInRange("z", -46.188, -100000.0, 100000.0);

        cameraPitch = builder.comment("Camera Pitch (90 is looking straight down)")
                .defineInRange("pitch", 35.264, -90.0, 90.0);
        cameraYaw = builder.comment("Camera Yaw")
                .defineInRange("yaw", -45.0, -360.0, 360.0);

        orthoSize = builder.comment("Orthographic View Size (Zoom Level)")
                .defineInRange("orthoSize", 22.0, 1.0, 200.0);

        builder.pop();
    }
}
