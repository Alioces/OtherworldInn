package com.otherworldinn.world.dialogue;

import java.util.List;
import java.util.Map;

public record DialogueDefinition(String id, String rootNodeId, Map<String, DialogueNodeDef> nodes) {
    public DialogueNodeDef getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public List<DialogueOptionDef> getOptions(String nodeId) {
        DialogueNodeDef node = getNode(nodeId);
        return node == null ? List.of() : node.options();
    }

    public String nodeTextKey(String nodeId) {
        return "dialogue.otherworldinn." + id + ".node." + nodeId;
    }

    public String nodeConditionalTextKey(String nodeId, boolean repaired) {
        return nodeTextKey(nodeId) + (repaired ? ".repaired" : ".unrepaired");
    }

    public String optionTextKey(String nodeId, String optionId) {
        return "dialogue.otherworldinn.option." + optionId;
    }
}
