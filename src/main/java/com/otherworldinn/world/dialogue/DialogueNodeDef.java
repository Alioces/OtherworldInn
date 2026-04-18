package com.otherworldinn.world.dialogue;

import java.util.List;
import org.jetbrains.annotations.Nullable;

public record DialogueNodeDef(
        String id,
        LocalizedText text,
        List<DialogueOptionDef> options,
        @Nullable DialogueNodeConditionalText conditionalText) {
    public DialogueNodeDef(String id, LocalizedText text, List<DialogueOptionDef> options) {
        this(id, text, options, null);
    }
}
