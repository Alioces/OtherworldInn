package com.otherworldinn.world.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.item.ExpeditionChartItem;
import com.otherworldinn.network.packet.C2SExpeditionCancelPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class ExpeditionInputHandler {

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ItemStack stack = mc.player.getMainHandItem();
        if (stack.getItem() instanceof ExpeditionChartItem
                && "recruiting".equals(ExpeditionChartItem.getChartState(stack))) {
            PacketDistributor.sendToServer(new C2SExpeditionCancelPacket());
        }
    }
}
