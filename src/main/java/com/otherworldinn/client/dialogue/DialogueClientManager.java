package com.otherworldinn.client.dialogue;

import com.otherworldinn.client.gui.screen.NpcDialogueScreen;
import com.otherworldinn.world.dialogue.DialogueNodeView;
import net.minecraft.client.Minecraft;

public final class DialogueClientManager {
    private static DialogueNodeView currentView;

    private DialogueClientManager() {}

    public static void handleNode(DialogueNodeView view) {
        currentView = view;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof NpcDialogueScreen dialogueScreen) {
            dialogueScreen.updateView(view);
        } else {
            mc.setScreen(new NpcDialogueScreen(view));
        }
    }

    public static void handleClose() {
        currentView = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof NpcDialogueScreen) {
            mc.setScreen(null);
        }
    }

    public static DialogueNodeView getCurrentView() {
        return currentView;
    }
}
