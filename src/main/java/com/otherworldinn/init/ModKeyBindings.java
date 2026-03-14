package com.otherworldinn.init;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** 客户端按键绑定 */
public class ModKeyBindings {
    public static final KeyMapping TOGGLE_MAP_MODE =
            new KeyMapping(
                    "key.otherworldinn.map_mode",
                    KeyConflictContext.IN_GAME,
                    KeyModifier.NONE,
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_M,
                    "key.categories.otherworldinn");
}
