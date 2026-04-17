package com.otherworldinn.world.dialogue;

import org.jetbrains.annotations.Nullable;

public record DialogueOptionDef(
        String id,
        LocalizedText label,
        DialogueOptionType type,
        @Nullable String nextNodeId,
        @Nullable String functionId) {}
