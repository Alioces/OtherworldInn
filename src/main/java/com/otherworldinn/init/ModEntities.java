package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.guest.HeavyPackGuestEntity;
import com.otherworldinn.entity.guest.OrdinaryGuestEntity;
import com.otherworldinn.entity.guest.RichGuestEntity;
import com.otherworldinn.entity.guest.UltraRichGuestEntity;
import com.otherworldinn.entity.store.BlacksmithEntity;
import com.otherworldinn.entity.store.MagicianEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 实体注册中心 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, OtherworldInn.MODID);

    public static final Supplier<EntityType<OrdinaryGuestEntity>> ORDINARY_GUEST =
            ENTITY_TYPES.register(
                    "ordinary_guest",
                    () ->
                            EntityType.Builder.of(OrdinaryGuestEntity::new, MobCategory.CREATURE)
                                    .sized(0.6F, 1.8F) // Alex/Steve size
                                    .clientTrackingRange(80)
                                    .updateInterval(2)
                                    .setShouldReceiveVelocityUpdates(true)
                                    .build("ordinary_guest"));

    public static final Supplier<EntityType<RichGuestEntity>> RICH_GUEST =
            ENTITY_TYPES.register(
                    "rich_guest",
                    () ->
                            EntityType.Builder.of(RichGuestEntity::new, MobCategory.CREATURE)
                                    .sized(0.6F, 1.8F)
                                    .clientTrackingRange(80)
                                    .updateInterval(2)
                                    .setShouldReceiveVelocityUpdates(true)
                                    .build("rich_guest"));

    public static final Supplier<EntityType<HeavyPackGuestEntity>> HEAVY_PACK_GUEST =
            ENTITY_TYPES.register(
                    "heavy_pack_guest",
                    () ->
                            EntityType.Builder.of(HeavyPackGuestEntity::new, MobCategory.CREATURE)
                                    .sized(0.6F, 1.8F)
                                    .clientTrackingRange(80)
                                    .updateInterval(2)
                                    .setShouldReceiveVelocityUpdates(true)
                                    .build("heavy_pack_guest"));

    public static final Supplier<EntityType<UltraRichGuestEntity>> ULTRA_RICH_GUEST =
            ENTITY_TYPES.register(
                    "ultra_rich_guest",
                    () ->
                            EntityType.Builder.of(UltraRichGuestEntity::new, MobCategory.CREATURE)
                                    .sized(0.6F, 1.8F)
                                    .clientTrackingRange(80)
                                    .updateInterval(2)
                                    .setShouldReceiveVelocityUpdates(true)
                                    .build("ultra_rich_guest"));

    public static final Supplier<EntityType<BlacksmithEntity>> BLACKSMITH =
            ENTITY_TYPES.register(
                    "blacksmith",
                    () ->
                            EntityType.Builder.of(
                                            BlacksmithEntity::new,
                                            MobCategory.MISC) // 使用 MISC 分类，因为不是生物
                                    .sized(0.6F, 1.95F) // 村民大小
                                    .clientTrackingRange(80)
                                    .updateInterval(2)
                                    .setShouldReceiveVelocityUpdates(true)
                                    .build("blacksmith"));

    public static final Supplier<EntityType<MagicianEntity>> MAGICIAN =
            ENTITY_TYPES.register(
                    "magician",
                    () ->
                            EntityType.Builder.of(
                                            MagicianEntity::new,
                                            MobCategory.MISC)
                                    .sized(0.6F, 1.95F)
                                    .clientTrackingRange(80)
                                    .updateInterval(2)
                                    .setShouldReceiveVelocityUpdates(true)
                                    .build("magician"));
}
