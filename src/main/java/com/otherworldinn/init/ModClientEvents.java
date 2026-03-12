package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.item.BedSheetItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import com.otherworldinn.client.renderer.GuestRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 模组客户端事件处理器
 * <p>
 * 处理仅限客户端的事件，例如按键绑定注册。
 * </p>
 */
@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(ModItems.ROOM_REGISTER.get(), 
                    ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "offhand"), 
                    (stack, level, entity, seed) -> {
                        if (entity == null) return 0.0F;
                        return entity.getOffhandItem() == stack ? 1.0F : 0.0F;
                    });
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ORDINARY_GUEST.get(), GuestRenderer::new);
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(ModKeyBindings.TOGGLE_MAP_MODE);
    }
}
