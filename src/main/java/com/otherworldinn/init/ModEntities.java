package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.guest.OrdinaryGuestEntity;
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
