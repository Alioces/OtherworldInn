package com.otherworldinn.world.event;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import com.otherworldinn.item.RoomKeyItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * 房间钥匙事件处理器
 * <p>
 * 处理房间钥匙的左键解绑逻辑。
 * </p>
 */
@EventBusSubscriber(modid = OtherworldInn.MODID)
public class RoomKeyHandler {

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        // 检查是否手持房间钥匙
        if (stack.is(ModItems.ROOM_KEY.get())) {
            // 检查是否已绑定
            Optional<Integer> roomId = RoomKeyItem.getBoundRoomId(stack);
            if (roomId.isPresent()) {
                // 执行解绑
                RoomKeyItem.unbindRoom(stack);
                player.displayClientMessage(Component.translatable("message.otherworldinn.room_key.unbound"), true);
                
                // 取消方块破坏
                event.setCanceled(true);
            }
        }
    }
}
