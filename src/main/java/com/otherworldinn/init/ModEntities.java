package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.OrdinaryGuestEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 实体注册中心
 */
public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, OtherworldInn.MODID);

    public static final Supplier<EntityType<OrdinaryGuestEntity>> ORDINARY_GUEST = ENTITY_TYPES.register("ordinary_guest",
            () -> EntityType.Builder.of(OrdinaryGuestEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F) // Alex/Steve size
                    .clientTrackingRange(80)
                    .updateInterval(2)
                    .setShouldReceiveVelocityUpdates(true)
                    .build("ordinary_guest"));
}
