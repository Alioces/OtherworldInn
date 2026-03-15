package com.otherworldinn.world.inn;

import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.init.ModEntities;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;

final class GuestSpawnRules {
    private static final List<Rule> RULES = List.of(
        new Rule(ModEntities.ORDINARY_GUEST, 0, 2, 1));

    private GuestSpawnRules() {}

    static GuestEntity createGuestForRating(int rating, RandomSource random, ServerLevel level) {
        Rule selectedRule = selectRule(rating, random);
        if (selectedRule == null) {
            return null;
        }
        return selectedRule.create(level);
    }

    private static Rule selectRule(int rating, RandomSource random) {
        List<Rule> availableRules = new ArrayList<>();
        int totalWeight = 0;
        for (Rule rule : RULES) {
            if (rule.matchesRating(rating)) {
                availableRules.add(rule);
                totalWeight += rule.weight();
            }
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
            Supplier<? extends EntityType<? extends GuestEntity>> entityTypeSupplier,
            int minRating,
            int maxRating,
            int weight) {
        private Rule {
            weight = Math.max(1, weight);
        }

        private boolean matchesRating(int rating) {
            return rating >= minRating && rating <= maxRating;
        }

        private GuestEntity create(ServerLevel level) {
            EntityType<? extends GuestEntity> entityType = entityTypeSupplier.get();
            return entityType.create(level);
        }
    }
}
