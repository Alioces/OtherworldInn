package com.otherworldinn.world.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.item.ChartComponentItem;
import com.otherworldinn.world.expedition.ChartComponentType;
import com.otherworldinn.world.expedition.ExpeditionNbtHelper;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class ChartComponentLootModifier extends LootModifier {

    public static final Supplier<MapCodec<ChartComponentLootModifier>> CODEC =
            Suppliers.memoize(() ->
                    RecordCodecBuilder.mapCodec(inst ->
                            codecStart(inst)
                                    .and(
                                            Codec.unboundedMap(
                                                    Codec.STRING,
                                                    Codec.FLOAT)
                                                    .fieldOf("components")
                                                    .forGetter(m -> m.components))
                                    .apply(inst, ChartComponentLootModifier::new)));

    private final Map<String, Float> components;
    private float totalWeight = -1;

    public ChartComponentLootModifier(LootItemCondition[] conditions,
            Map<String, Float> components) {
        super(conditions);
        this.components = new LinkedHashMap<>(components);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(
            ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (components.isEmpty()) return generatedLoot;
        if (totalWeight < 0) {
            totalWeight = 0f;
            for (float w : components.values()) totalWeight += w;
        }
        if (totalWeight <= 0) return generatedLoot;

        float roll = context.getRandom().nextFloat() * totalWeight;
        String picked = null;
        float cumulative = 0f;
        for (Map.Entry<String, Float> entry : components.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                picked = entry.getKey();
                break;
            }
        }
        if (picked == null) return generatedLoot;

        ItemStack component = new ItemStack(ModItems.CHART_COMPONENT.get());
        CompoundTag tag = ExpeditionNbtHelper.readTag(component);
        tag.putString("component_type", picked);
        ExpeditionNbtHelper.writeTag(component, tag);

        generatedLoot.add(component);
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
