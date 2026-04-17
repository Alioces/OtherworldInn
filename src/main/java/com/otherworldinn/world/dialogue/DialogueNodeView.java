package com.otherworldinn.world.dialogue;

import java.util.List;

public record DialogueNodeView(
        int entityId, String dialogueId, String nodeId, String textKey, List<DialogueOptionView> options) {}
