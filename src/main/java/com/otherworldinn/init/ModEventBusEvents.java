package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.entity.store.BlacksmithEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

/** 模组事件总线事件处理器 */
@EventBusSubscriber(modid = OtherworldInn.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusEvents {

    @SubscribeEvent
    public static void onAttributeCreation(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ORDINARY_GUEST.get(), GuestEntity.createAttributes().build());
        event.put(ModEntities.BLACKSMITH.get(), BlacksmithEntity.createAttributes().build());
    }
}
