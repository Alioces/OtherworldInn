package com.otherworldinn.world.inn;

import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.init.ModEntities;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

final class GuestSpawnRules {
    private static final int MIN_RATING = 0;
    private static final int MAX_RATING = 5;
    private static final List<List<Rule>> RULES_BY_RATING = List.of(
            List.of(//0
                    new Rule(ModEntities.ORDINARY_GUEST, 4)),
            List.of(//1
                    new Rule(ModEntities.ORDINARY_GUEST, 4),
                    new Rule(ModEntities.HEAVY_PACK_GUEST, 6)),
            List.of(//2
                    new Rule(ModEntities.ORDINARY_GUEST, 4),
                    new Rule(ModEntities.RICH_GUEST, 4),
                    new Rule(ModEntities.HEAVY_PACK_GUEST, 6)),
            List.of(//3
                    new Rule(ModEntities.ORDINARY_GUEST, 4),
                    new Rule(ModEntities.RICH_GUEST, 4),
                    new Rule(ModEntities.HEAVY_PACK_GUEST, 6),
                    new Rule(ModEntities.ORDINARY_VIP_GUEST, 2),
                    new Rule(ModEntities.ULTRA_RICH_GUEST, 3)),
            List.of(//4
                    new Rule(ModEntities.ORDINARY_GUEST, 3),
                    new Rule(ModEntities.RICH_GUEST, 5),
                    new Rule(ModEntities.HEAVY_PACK_GUEST, 6),
                    new Rule(ModEntities.ORDINARY_VIP_GUEST, 2),
                    new Rule(ModEntities.ULTRA_RICH_GUEST, 3)),
            List.of(//5
                    new Rule(ModEntities.ORDINARY_GUEST, 4),
                    new Rule(ModEntities.RICH_GUEST, 3),
                    new Rule(ModEntities.HEAVY_PACK_GUEST, 6),
                    new Rule(ModEntities.ORDINARY_VIP_GUEST, 2),
                    new Rule(ModEntities.ULTRA_RICH_GUEST, 3)));

    private GuestSpawnRules() {}

    static GuestEntity createGuestForRating(int rating, RandomSource random, ServerLevel level) {
        Rule selectedRule = selectRuleByRating(rating, random);
        if (selectedRule == null) {
            return null;
        }
        return selectedRule.create(level);
    }

    private static Rule selectRuleByRating(int rating, RandomSource random) {
        int clampedRating = Math.max(MIN_RATING, Math.min(MAX_RATING, rating));
        List<Rule> availableRules = RULES_BY_RATING.get(clampedRating - MIN_RATING);
        int totalWeight = 0;
        for (Rule rule : availableRules) {
            totalWeight += rule.weight();
        }
        if (availableRules.isEmpty()) {
            return null;
        }
        if (availableRules.size() == 1) {
            return availableRules.get(0);
        }
        int roll = random.nextInt(totalWeight);
        int current = 0;
        for (Rule rule : availableRules) {
            current += rule.weight();
            if (roll < current) {
                return rule;
            }
        }
        return availableRules.get(availableRules.size() - 1);
    }

    private record Rule(
            Supplier<? extends EntityType<? extends GuestEntity>> entityTypeSupplier, int weight) {
        private Rule {
            weight = Math.max(1, weight);
        }

        private GuestEntity create(ServerLevel level) {
            EntityType<? extends GuestEntity> entityType = entityTypeSupplier.get();
            return entityType.create(level);
        }
    }
}
