package com.otherworldinn.world.commission;

import com.otherworldinn.world.dialogue.LocalizedText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

public final class CommissionRegistry {
    private static final List<CommissionTemplate> TEMPLATES = new ArrayList<>();

    static {
        register("farm_supply")
                .stars(1, 2)
                .weight(12)
                .description(
                        LocalizedText.of(
                                "农场的粮食储备不够了，请带一点过来。",
                                "The farm is short on staple supplies. Please restock soon."))
                .submit("minecraft:wheat", 16)
                .submit("minecraft:bread", 8)
                .rewardCoins(40)
                .rewardFavor("otherworldinn:farmer", 60)
                .build();

        register("forge_maintenance")
                .stars(2, 3)
                .weight(10)
                .description(
                        LocalizedText.of(
                                "铁匠铺需要备用金属件，请先送一批材料过来。",
                                "The forge needs spare metal parts. Deliver a batch of materials."))
                .submit("minecraft:iron_ingot", 12)
                .submit("minecraft:gold_ingot", 4)
                .rewardCoins(40)
                .rewardFavor("otherworldinn:blacksmith", 90)
                .build();

        register("town_patrol_zombie")
                .stars(1, 3)
                .weight(14)
                .description(
                        LocalizedText.of(
                                "最近城镇外夜间不是很太平，请清理游荡的僵尸。",
                                "Nights beyond town are unsafe. Clear out wandering zombies."))
                .kill("minecraft:zombie", 10)
                .rewardCoins(80)
                .build();

        register("road_patrol_mixed")
                .stars(3, 4)
                .weight(9)
                .description(
                        LocalizedText.of(
                                "城镇的巡逻队请求支援：沿路清理骷髅并补给食物和工具。",
                                "Patrol requests support: clear skeletons and deliver field rations and tools."))
                .kill("minecraft:skeleton", 12)
                .submit("minecraft:bread", 12)
                .submit("minecraft:diamond_sword", 1)
                .rewardCoins(120)
                .build();
    }

    private CommissionRegistry() {}

    public static Builder register(String id) {
        return new Builder(id);
    }

    public static List<CommissionTemplate> allTemplates() {
        return Collections.unmodifiableList(TEMPLATES);
    }

    public static CommissionTemplate pickRandomTemplate(
            RandomSource random, List<String> excludedTemplateIds) {
        List<CommissionTemplate> pool = new ArrayList<>();
        for (CommissionTemplate template : TEMPLATES) {
            if (!excludedTemplateIds.contains(template.id())) {
                pool.add(template);
            }
        }
        if (pool.isEmpty()) {
            pool = TEMPLATES;
        }
        if (pool.isEmpty()) {
            return null;
        }
        int totalWeight = 0;
        for (CommissionTemplate template : pool) {
            totalWeight += Math.max(1, template.weight());
        }
        int roll = random.nextInt(Math.max(1, totalWeight));
        int acc = 0;
        for (CommissionTemplate template : pool) {
            acc += Math.max(1, template.weight());
            if (roll < acc) {
                return template;
            }
        }
        return pool.get(random.nextInt(pool.size()));
    }

    public record CommissionTemplate(
            String id,
            int minStars,
            int maxStars,
            int weight,
            LocalizedText description,
            List<CommissionEntry.ItemRequirement> submitRequirements,
            List<CommissionEntry.KillRequirement> killRequirements,
            List<CommissionEntry.ItemReward> itemRewards,
            int coinReward,
            List<CommissionEntry.NpcFavorReward> npcFavorRewards) {
        public String descriptionKey() {
            return "commission.otherworldinn.description." + id;
        }
    }

    public static final class Builder {
        private final String id;
        private int minStars = 1;
        private int maxStars = 5;
        private int weight = 1;
        private LocalizedText description = LocalizedText.of("暂无描述", "No description");
        private final List<CommissionEntry.ItemRequirement> submitRequirements = new ArrayList<>();
        private final List<CommissionEntry.KillRequirement> killRequirements = new ArrayList<>();
        private final List<CommissionEntry.ItemReward> itemRewards = new ArrayList<>();
        private int coinReward = 0;
        private final List<CommissionEntry.NpcFavorReward> npcFavorRewards = new ArrayList<>();

        private Builder(String id) {
            this.id = id;
        }

        public Builder stars(int minStars, int maxStars) {
            this.minStars = Math.max(1, Math.min(5, minStars));
            this.maxStars = Math.max(this.minStars, Math.max(1, Math.min(5, maxStars)));
            return this;
        }

        public Builder weight(int weight) {
            this.weight = Math.max(1, weight);
            return this;
        }

        public Builder description(LocalizedText description) {
            if (description != null) {
                this.description = description;
            }
            return this;
        }

        public Builder submit(String itemId, int count) {
            this.submitRequirements.add(new CommissionEntry.ItemRequirement(itemId, count));
            return this;
        }

        public Builder submit(String itemId, int count, CompoundTag nbt) {
            this.submitRequirements.add(new CommissionEntry.ItemRequirement(itemId, count, nbt));
            return this;
        }

        public Builder kill(String entityTypeId, int count) {
            this.killRequirements.add(new CommissionEntry.KillRequirement(entityTypeId, count));
            return this;
        }

        public Builder rewardItem(String itemId, int count) {
            this.itemRewards.add(new CommissionEntry.ItemReward(itemId, count));
            return this;
        }

        public Builder rewardCoins(int coinReward) {
            this.coinReward = Math.max(0, coinReward);
            return this;
        }

        public Builder rewardFavor(String npcEntityTypeId, int favorProgress) {
            this.npcFavorRewards.add(
                    new CommissionEntry.NpcFavorReward(npcEntityTypeId, favorProgress));
            return this;
        }

        public CommissionTemplate build() {
            CommissionTemplate template =
                    new CommissionTemplate(
                            id,
                            minStars,
                            maxStars,
                            weight,
                            description,
                            List.copyOf(submitRequirements),
                            List.copyOf(killRequirements),
                            List.copyOf(itemRewards),
                            coinReward,
                            List.copyOf(npcFavorRewards));
            TEMPLATES.add(template);
            return template;
        }
    }
}
