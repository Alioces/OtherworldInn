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
    public static final String FUNCTION_OPEN_VIRTUAL_ANVIL = "open_virtual_anvil";


    private static final DialogueDefinition BLACKSMITH_DIALOGUE =
            buildStoreDialogueWithFacilityTopics(
                    "blacksmith",
                    LocalizedText.of(
                        "铁火未冷，要打什么？", 
                        "The forge is hot. What are you after?"),
                    LocalizedText.of(
                        "矿石、矿锭、工具都在这，挑你顺手的", 
                        "Ores, ingots, and tools. Pick what suits your hand."),
                    null,
                    LocalizedText.of("关于锅炉房", "About the Boiler Room"),
                    null,
                    null,
                    LocalizedText.of("地下那座锅炉房好久没人修理了，不知道还能不能用", 
                                     "No one has repaired the boiler room downstairs for a long time. I don't even know if it still works."),
                    LocalizedText.of("你把它修好了？太好了，我们的机器可以用了",
                                     "You repaired it? That's great."),
                    LocalizedText.of("借用一下铁砧", "Borrow the Anvil"),
                    FUNCTION_OPEN_VIRTUAL_ANVIL);
    private static final DialogueDefinition FARMER_DIALOGUE =
            buildStoreDialogueWithFacilityTopics(
                    "farmer",
                    LocalizedText.of(
                        "田里刚收了货，想买点啥？", 
                        "Fresh harvest just came in. Need anything?"),
                    LocalizedText.of(
                        "种子、作物、农具都有，慢慢挑", 
                        "Seeds, produce, and farming goods. Take your time."),
                    LocalizedText.of("关于温室", "About the Greenhouse"),
                    null,
                    LocalizedText.of("对面那座温室废弃很久了...以前可好用了",
                                     "That greenhouse across the way has been abandoned for ages... it used to be so useful."),
                    LocalizedText.of("你居然真的把它修好了，感谢你的付出，现在种地更方便了", 
                                     "You actually got it repaired. Thank you for putting in the work."),
                    null,
                    null,
                    null,
                    null);
    private static final DialogueDefinition MAGICIAN_DIALOGUE =
            buildSimpleStoreDialogue(
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

    private static DialogueDefinition buildSimpleStoreDialogue(
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
                                        "open_store",
                                        LocalizedText.of("打开商店", "Open Shop"),
                                        DialogueOptionType.FUNCTION,
                                        null,
                                        FUNCTION_OPEN_STORE),
                                new DialogueOptionDef(
                                        "leave",
                                        LocalizedText.of("先告辞", "Leave"),
                                        DialogueOptionType.BRANCH,
                                        null,
                                        null))));
        return new DialogueDefinition(npcId, root, nodes);
    }

    private static DialogueDefinition buildStoreDialogueWithFacilityTopics(
            String npcId,
            LocalizedText rootText,
            LocalizedText askGoodsText,
            @Nullable LocalizedText aboutGreenhouseLabel,
            @Nullable LocalizedText aboutBoilerRoomLabel,
            @Nullable LocalizedText greenhouseUnrepairedText,
            @Nullable LocalizedText greenhouseRepairedText,
            @Nullable LocalizedText boilerRoomUnrepairedText,
            @Nullable LocalizedText boilerRoomRepairedText,
            @Nullable LocalizedText extraFunctionLabel,
            @Nullable String extraFunctionId) {
        String root = "root";
        String askGoods = "ask_goods";
        String greenhouseTopic = "topic_greenhouse";
        String boilerTopic = "topic_boiler_room";
        boolean hasGreenhouseTopic =
                aboutGreenhouseLabel != null
                        && greenhouseUnrepairedText != null
                        && greenhouseRepairedText != null;
        boolean hasBoilerTopic =
                aboutBoilerRoomLabel != null
                        && boilerRoomUnrepairedText != null
                        && boilerRoomRepairedText != null;
        Map<String, DialogueNodeDef> nodes = new LinkedHashMap<>();

        List<DialogueOptionDef> rootOptions = new ArrayList<>();
        rootOptions.add(
                new DialogueOptionDef(
                        "open_store",
                        LocalizedText.of("打开商店", "Open Shop"),
                        DialogueOptionType.FUNCTION,
                        null,
                        FUNCTION_OPEN_STORE));
        if (extraFunctionLabel != null && extraFunctionId != null && !extraFunctionId.isBlank()) {
            rootOptions.add(
                    new DialogueOptionDef(
                            "extra_function",
                            extraFunctionLabel,
                            DialogueOptionType.FUNCTION,
                            null,
                            extraFunctionId));
        }
        rootOptions.add(
                new DialogueOptionDef(
                        "ask_goods",
                        LocalizedText.of("这里卖什么", "What Do You Sell?"),
                        DialogueOptionType.BRANCH,
                        askGoods,
                        null));
        if (hasGreenhouseTopic) {
            rootOptions.add(
                    new DialogueOptionDef(
                            "about_greenhouse",
                            aboutGreenhouseLabel,
                            DialogueOptionType.BRANCH,
                            greenhouseTopic,
                            null));
        }
        if (hasBoilerTopic) {
            rootOptions.add(
                    new DialogueOptionDef(
                            "about_boiler_room",
                            aboutBoilerRoomLabel,
                            DialogueOptionType.BRANCH,
                            boilerTopic,
                            null));
        }
        rootOptions.add(
                new DialogueOptionDef(
                        "leave",
                        LocalizedText.of("先告辞", "Leave"),
                        DialogueOptionType.BRANCH,
                        null,
                        null));
        nodes.put(root, new DialogueNodeDef(root, rootText, List.copyOf(rootOptions)));

        List<DialogueOptionDef> askGoodsOptions = new ArrayList<>();
        askGoodsOptions.add(
                new DialogueOptionDef(
                        "open_store",
                        LocalizedText.of("打开商店", "Open Shop"),
                        DialogueOptionType.FUNCTION,
                        null,
                        FUNCTION_OPEN_STORE));
        if (extraFunctionLabel != null && extraFunctionId != null && !extraFunctionId.isBlank()) {
            askGoodsOptions.add(
                    new DialogueOptionDef(
                            "extra_function",
                            extraFunctionLabel,
                            DialogueOptionType.FUNCTION,
                            null,
                            extraFunctionId));
        }
        if (hasGreenhouseTopic) {
            askGoodsOptions.add(
                    new DialogueOptionDef(
                            "about_greenhouse",
                            aboutGreenhouseLabel,
                            DialogueOptionType.BRANCH,
                            greenhouseTopic,
                            null));
        }
        if (hasBoilerTopic) {
            askGoodsOptions.add(
                    new DialogueOptionDef(
                            "about_boiler_room",
                            aboutBoilerRoomLabel,
                            DialogueOptionType.BRANCH,
                            boilerTopic,
                            null));
        }
        askGoodsOptions.add(
                new DialogueOptionDef(
                        "leave",
                        LocalizedText.of("先告辞", "Leave"),
                        DialogueOptionType.BRANCH,
                        null,
                        null));
        nodes.put(askGoods, new DialogueNodeDef(askGoods, askGoodsText, List.copyOf(askGoodsOptions)));

        if (hasGreenhouseTopic) {
            List<DialogueOptionDef> greenhouseOptions = new ArrayList<>();
            greenhouseOptions.add(
                    new DialogueOptionDef(
                            "open_store",
                            LocalizedText.of("打开商店", "Open Shop"),
                            DialogueOptionType.FUNCTION,
                            null,
                            FUNCTION_OPEN_STORE));
            if (extraFunctionLabel != null && extraFunctionId != null && !extraFunctionId.isBlank()) {
                greenhouseOptions.add(
                        new DialogueOptionDef(
                                "extra_function",
                                extraFunctionLabel,
                                DialogueOptionType.FUNCTION,
                                null,
                                extraFunctionId));
            }
            greenhouseOptions.add(
                    new DialogueOptionDef(
                            "leave",
                            LocalizedText.of("先告辞", "Leave"),
                            DialogueOptionType.BRANCH,
                            null,
                            null));
            nodes.put(
                    greenhouseTopic,
                    new DialogueNodeDef(
                            greenhouseTopic,
                            greenhouseUnrepairedText,
                            List.copyOf(greenhouseOptions),
                            new DialogueNodeConditionalText(
                                    "greenhouse", greenhouseUnrepairedText, greenhouseRepairedText)));
        }
        if (hasBoilerTopic) {
            List<DialogueOptionDef> boilerOptions = new ArrayList<>();
            boilerOptions.add(
                    new DialogueOptionDef(
                            "open_store",
                            LocalizedText.of("打开商店", "Open Shop"),
                            DialogueOptionType.FUNCTION,
                            null,
                            FUNCTION_OPEN_STORE));
            if (extraFunctionLabel != null && extraFunctionId != null && !extraFunctionId.isBlank()) {
                boilerOptions.add(
                        new DialogueOptionDef(
                                "extra_function",
                                extraFunctionLabel,
                                DialogueOptionType.FUNCTION,
                                null,
                                extraFunctionId));
            }
            boilerOptions.add(
                    new DialogueOptionDef(
                            "leave",
                            LocalizedText.of("先告辞", "Leave"),
                            DialogueOptionType.BRANCH,
                            null,
                            null));
            nodes.put(
                    boilerTopic,
                    new DialogueNodeDef(
                            boilerTopic,
                            boilerRoomUnrepairedText,
                            List.copyOf(boilerOptions),
                            new DialogueNodeConditionalText(
                                    "boiler_room", boilerRoomUnrepairedText, boilerRoomRepairedText)));
        }
        return new DialogueDefinition(npcId, root, nodes);
    }

}
