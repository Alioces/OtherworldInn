package com.otherworldinn.world.dialogue;

import java.util.List;

public record DialogueNodeDef(String id, LocalizedText text, List<DialogueOptionDef> options) {}
