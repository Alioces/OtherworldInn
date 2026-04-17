package com.otherworldinn.world.dialogue;

import com.otherworldinn.entity.store.BlacksmithEntity;
import com.otherworldinn.entity.store.FarmerEntity;
import com.otherworldinn.entity.store.MagicianEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public final class DialogueRegistry {
    public static final String FUNCTION_OPEN_STORE = "open_store";

    private static final DialogueDefinition BLACKSMITH_DIALOGUE =
            buildStoreDialogue(
                    "blacksmith",
                    LocalizedText.of(
                        "铁火未冷，要打什么？", 
                        "The forge is hot. What are you after?"),
                    LocalizedText.of(
                        "矿石、矿锭、工具都在这，挑你顺手的", 
                        "Ores, ingots, and tools. Pick what suits your hand."));
    private static final DialogueDefinition FARMER_DIALOGUE =
            buildStoreDialogue(
                    "farmer",
                    LocalizedText.of(
                        "田里刚收了货，想买点啥？", 
                        "Fresh harvest just came in. Need anything?"),
                    LocalizedText.of(
                        "种子、作物、农具都有，慢慢挑", 
                        "Seeds, produce, and farming goods. Take your time."));
    private static final DialogueDefinition MAGICIAN_DIALOGUE =
            buildStoreDialogue(
                    "magician",
                    LocalizedText.of(
                        "你面前这么可爱的魔女是谁呢？没错，就是我，你需要什么？", 
                        "The arcane stirs. What do you seek?"),
                    LocalizedText.of(
                        "材料和书卷都在这，想要什么自己挑",
                        "Materials and tomes are ready. Mind the explosive stuff."));

    private static final List<DialogueDefinition> ALL_DIALOGUES =
            Collections.unmodifiableList(
                    new ArrayList<>(
                            List.of(BLACKSMITH_DIALOGUE, 
                                    FARMER_DIALOGUE, 
                                    MAGICIAN_DIALOGUE
                                )));

    private DialogueRegistry() {}

    @Nullable
    public static DialogueDefinition resolve(Entity entity) {
        if (entity instanceof BlacksmithEntity) {
            return BLACKSMITH_DIALOGUE;
        }
        if (entity instanceof FarmerEntity) {
            return FARMER_DIALOGUE;
        }
        if (entity instanceof MagicianEntity) {
            return MAGICIAN_DIALOGUE;
        }
        return null;
    }

    public static List<DialogueDefinition> allDialogues() {
        return ALL_DIALOGUES;
    }

    private static DialogueDefinition buildStoreDialogue(
            String npcId, LocalizedText rootText, LocalizedText askGoodsText) {
        String root = "root";
        String askGoods = "ask_goods";
        Map<String, DialogueNodeDef> nodes = new LinkedHashMap<>();
        nodes.put(
                root,
                new DialogueNodeDef(
                        root,
                        rootText,
                        List.of(
                                new DialogueOptionDef(
                                        "open_store",
                                        LocalizedText.of("打开商店", "Open Shop"),
                                        DialogueOptionType.FUNCTION,
                                        null,
                                        FUNCTION_OPEN_STORE),
                                new DialogueOptionDef(
                                        "ask_goods",
                                        LocalizedText.of("这里卖什么", "What Do You Sell?"),
                                        DialogueOptionType.BRANCH,
                                        askGoods,
                                        null),
                                new DialogueOptionDef(
                                        "leave",
                                        LocalizedText.of("先告辞", "Leave"),
                                        DialogueOptionType.BRANCH,
                                        null,
                                        null))));
        nodes.put(
                askGoods,
                new DialogueNodeDef(
                        askGoods,
                        askGoodsText,
                        List.of(
                                new DialogueOptionDef(
                                        "open_store_after_ask",
                                        LocalizedText.of("打开商店", "Open Shop"),
                                        DialogueOptionType.FUNCTION,
                                        null,
                                        FUNCTION_OPEN_STORE),
                                new DialogueOptionDef(
                                        "leave_after_ask",
                                        LocalizedText.of("先告辞", "Leave"),
                                        DialogueOptionType.BRANCH,
                                        null,
                                        null))));
        return new DialogueDefinition(npcId, root, nodes);
    }
}
