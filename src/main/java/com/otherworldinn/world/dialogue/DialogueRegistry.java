package com.otherworldinn.world.dialogue;

import com.otherworldinn.entity.base.GuestEntity;
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
    private static final List<DialogueDefinition> GUEST_DIALOGUES =
            List.of(
                    buildGuestLineDialogue(
                            "guest_ordinary_welcome",
                            LocalizedText.of(
                                    "这里比我想象中更温暖，今晚应该能睡个好觉。",
                                    "This place feels warmer than I expected. I might sleep well tonight.")),
                    buildGuestLineDialogue(
                            "guest_ordinary_weather",
                            LocalizedText.of(
                                    "外面的风好大，幸好旅馆里很安静。",
                                    "The wind outside is harsh. Glad it's calm in the inn.")),
                    buildGuestLineDialogue(
                            "guest_ordinary_checkout",
                            LocalizedText.of(
                                    "明天我就启程，今晚先好好休息。",
                                    "I leave at dawn tomorrow. Tonight, I just need good rest.")),
                    buildGuestLineDialogue(
                            "guest_rich_service",
                            LocalizedText.of(
                                    "如果服务继续这么周到，我会常来。",
                                    "If the service stays this refined, I'll return often.")),
                    buildGuestLineDialogue(
                            "guest_rich_wine",
                            LocalizedText.of(
                                    "要是晚餐再配点好酒，就更完美了。",
                                    "A fine wine with dinner would make this place perfect.")),
                    buildGuestLineDialogue(
                            "guest_rich_tip",
                            LocalizedText.of(
                                    "照顾得不错，退房时我可能会留点小费。",
                                    "Good care deserves a tip when I check out.")),
                    buildGuestLineDialogue(
                            "guest_heavy_pack_route",
                            LocalizedText.of(
                                    "我背着这么多货，能有个落脚点真不容易。",
                                    "Traveling with this much cargo, a safe stop is priceless.")),
                    buildGuestLineDialogue(
                            "guest_heavy_pack_storage",
                            LocalizedText.of(
                                    "这些箱子先放这儿，别让它们淋雨了。",
                                    "Let me stash these crates here. Keep them out of the rain.")),
                    buildGuestLineDialogue(
                            "guest_heavy_pack_food",
                            LocalizedText.of(
                                    "跑了一整天，先来份热乎的饭再说。",
                                    "I've been on the road all day. A hot meal comes first.")),
                    buildGuestLineDialogue(
                            "guest_ultra_rich_suite",
                            LocalizedText.of(
                                    "这间套房勉强合格，希望夜里足够安静。",
                                    "This suite is acceptable. I expect absolute quiet at night.")),
                    buildGuestLineDialogue(
                            "guest_ultra_rich_privacy",
                            LocalizedText.of(
                                    "我不喜欢被打扰，安排人手时请注意。",
                                    "I dislike interruptions. Make sure your staff knows that.")),
                    buildGuestLineDialogue(
                            "guest_ultra_rich_guard",
                            LocalizedText.of(
                                    "我的行李很贵重，安保别出差错。",
                                    "My luggage is valuable. Security must not fail.")),
                    buildGuestLineDialogue(
                            "guest_vip_ordinary_schedule",
                            LocalizedText.of(
                                    "我的行程很紧，麻烦按时叫醒我。",
                                    "My schedule is tight. Wake me on time, please.")),
                    buildGuestLineDialogue(
                            "guest_vip_ordinary_tea",
                            LocalizedText.of(
                                    "如果能送一壶热茶到房间就更好了。",
                                    "A pot of hot tea to the room would be lovely.")),
                    buildGuestLineDialogue(
                            "guest_vip_ordinary_review",
                            LocalizedText.of(
                                    "服务不错，我会给旅馆写个好评。",
                                    "The service is excellent. I'll leave a positive review.")),
                    buildGuestLineDialogue(
                            "guest_vip_advanced_security",
                            LocalizedText.of(
                                    "我的随从稍后到，先把房门权限准备好。",
                                    "My attendants arrive later. Prepare room access in advance.")),
                    buildGuestLineDialogue(
                            "guest_vip_advanced_order",
                            LocalizedText.of(
                                    "晚些时候我会点餐，记得用最好的食材。",
                                    "I'll place an order later. Use your finest ingredients.")),
                    buildGuestLineDialogue(
                            "guest_vip_advanced_reward",
                            LocalizedText.of(
                                    "把事情办漂亮了，回头少不了赏金。",
                                    "Do this properly, and there will be a reward.")),
                    buildGuestLineDialogue(
                            "guest_sponsor_photo",
                            LocalizedText.of(
                                    "这里氛围真棒，我想拍张照留念。",
                                    "The vibe here is great. I want to take a commemorative photo.")),
                    buildGuestLineDialogue(
                            "guest_sponsor_renovation",
                            LocalizedText.of(
                                    "旅馆改造后舒服多了，辛苦你们了。",
                                    "The renovations made this place so much nicer. Well done.")),
                    buildGuestLineDialogue(
                            "guest_sponsor_support",
                            LocalizedText.of(
                                    "继续加油，我会一直支持这家旅馆。",
                                    "Keep it up. I'll keep supporting this inn.")));

    private static final List<DialogueDefinition> ALL_DIALOGUES;

    private static final Map<String, DialogueDefinition> DIALOGUE_BY_ID;

    static {
        List<DialogueDefinition> all =
                new ArrayList<>(List.of(BLACKSMITH_DIALOGUE, FARMER_DIALOGUE, MAGICIAN_DIALOGUE));
        all.addAll(GUEST_DIALOGUES);
        ALL_DIALOGUES = Collections.unmodifiableList(all);
        Map<String, DialogueDefinition> byId = new LinkedHashMap<>();
        for (DialogueDefinition dialogue : ALL_DIALOGUES) {
            byId.put(dialogue.id(), dialogue);
        }
        DIALOGUE_BY_ID = Collections.unmodifiableMap(byId);
    }

    private DialogueRegistry() {}

    @Nullable
    public static DialogueDefinition resolve(Entity entity) {
        if (entity instanceof GuestEntity guestEntity) {
            return resolveById(guestEntity.getAssignedDialogueId());
        }
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

    @Nullable
    public static DialogueDefinition resolveById(@Nullable String dialogueId) {
        if (dialogueId == null || dialogueId.isBlank()) {
            return null;
        }
        return DIALOGUE_BY_ID.get(dialogueId);
    }

    public static List<DialogueDefinition> allDialogues() {
        return ALL_DIALOGUES;
    }

    private static DialogueDefinition buildGuestLineDialogue(String id, LocalizedText lineText) {
        String root = "root";
        Map<String, DialogueNodeDef> nodes = new LinkedHashMap<>();
        nodes.put(
                root,
                new DialogueNodeDef(
                        root,
                        lineText,
                        List.of(
                                new DialogueOptionDef(
                                        "leave",
                                        LocalizedText.of("祝您旅居愉快", "Enjoy Your Stay"),
                                        DialogueOptionType.BRANCH,
                                        null,
                                        null))));
        return new DialogueDefinition(id, root, nodes);
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
