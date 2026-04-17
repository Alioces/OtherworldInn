package com.otherworldinn.world.dialogue;

import com.otherworldinn.entity.base.StoreEntity;
import com.otherworldinn.network.ModMessages;
import com.otherworldinn.network.packet.S2CDialogueClosePacket;
import com.otherworldinn.network.packet.S2CDialogueNodePacket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class DialogueService {
    private static final double MAX_DIALOGUE_DISTANCE_SQR = 100.0D;
    private static final Map<UUID, DialogueSession> SESSIONS = new HashMap<>();

    private DialogueService() {}

    public static boolean tryStartDialogue(ServerPlayer player, Entity entity) {
        DialogueDefinition definition = DialogueRegistry.resolve(entity);
        if (definition == null) {
            return false;
        }
        DialogueNodeDef root = definition.getNode(definition.rootNodeId());
        if (root == null) {
            return false;
        }
        DialogueSession session =
                new DialogueSession(player.getUUID(), entity.getId(), definition, root.id(), entity.getUUID());
        SESSIONS.put(player.getUUID(), session);
        sendNodeToPlayer(player, session, root);
        return true;
    }

    public static void selectOption(ServerPlayer player, int entityId, String optionId) {
        DialogueSession session = SESSIONS.get(player.getUUID());
        if (session == null || session.entityId() != entityId) {
            return;
        }
        Entity entity = player.level().getEntity(entityId);
        if (entity == null || !entity.isAlive()) {
            closeDialogue(player, true);
            return;
        }
        if (!entity.getUUID().equals(session.entityUuid())
                || player.distanceToSqr(entity) > MAX_DIALOGUE_DISTANCE_SQR) {
            closeDialogue(player, true);
            return;
        }
        DialogueNodeDef currentNode = session.definition().getNode(session.currentNodeId());
        if (currentNode == null) {
            closeDialogue(player, true);
            return;
        }
        DialogueOptionDef selected = null;
        for (DialogueOptionDef option : currentNode.options()) {
            if (option.id().equals(optionId)) {
                selected = option;
                break;
            }
        }
        if (selected == null) {
            return;
        }

        if (selected.type() == DialogueOptionType.FUNCTION) {
            if (DialogueRegistry.FUNCTION_OPEN_STORE.equals(selected.functionId())
                    && entity instanceof StoreEntity storeEntity) {
                storeEntity.openStoreForPlayer(player);
            }
            closeDialogue(player, true);
            return;
        }

        if (selected.nextNodeId() == null || selected.nextNodeId().isBlank()) {
            closeDialogue(player, true);
            return;
        }
        DialogueNodeDef nextNode = session.definition().getNode(selected.nextNodeId());
        if (nextNode == null) {
            closeDialogue(player, true);
            return;
        }
        DialogueSession nextSession =
                new DialogueSession(
                        session.playerUuid(),
                        session.entityId(),
                        session.definition(),
                        nextNode.id(),
                        session.entityUuid());
        SESSIONS.put(player.getUUID(), nextSession);
        sendNodeToPlayer(player, nextSession, nextNode);
    }

    public static void closeDialogue(ServerPlayer player, boolean notifyClient) {
        SESSIONS.remove(player.getUUID());
        if (notifyClient) {
            ModMessages.sendToPlayer(new S2CDialogueClosePacket(), player);
        }
    }

    private static void sendNodeToPlayer(
            ServerPlayer player, DialogueSession session, DialogueNodeDef node) {
        String nodeTextKey = session.definition().nodeTextKey(node.id());
        DialogueNodeView nodeView =
                new DialogueNodeView(
                        session.entityId(),
                        session.definition().id(),
                        node.id(),
                        nodeTextKey,
                        node.options().stream()
                                .map(
                                        o ->
                                                new DialogueOptionView(
                                                        o.id(),
                                                        session.definition()
                                                                .optionTextKey(node.id(), o.id()),
                                                        o.type()))
                                .toList());
        ModMessages.sendToPlayer(new S2CDialogueNodePacket(nodeView), player);
    }

    private record DialogueSession(
            UUID playerUuid,
            int entityId,
            DialogueDefinition definition,
            String currentNodeId,
            @Nullable UUID entityUuid) {}
}
