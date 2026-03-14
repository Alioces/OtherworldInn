package com.otherworldinn.network.packet;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.store.StoreEntity;
import com.otherworldinn.world.inventory.StoreMenu;
import com.otherworldinn.world.team.TeamData;
import com.otherworldinn.world.team.TeamManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商店购买数据包 (Client -> Server)
 */
public record C2SStorePurchasePacket(int entityId, List<PurchaseItem> items) implements CustomPacketPayload {
    private static final int MAX_REQUEST_ITEMS = 54;
    private static final double MAX_PURCHASE_DISTANCE_SQR = 64.0D;

    public record PurchaseItem(ItemStack stack, int quantity) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PurchaseItem> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC,
            PurchaseItem::stack,
            ByteBufCodecs.INT,
            PurchaseItem::quantity,
            PurchaseItem::new
        );
    }

    public static final Type<C2SStorePurchasePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "store_purchase"));

    public static final StreamCodec<RegistryFriendlyByteBuf, C2SStorePurchasePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            C2SStorePurchasePacket::entityId,
            PurchaseItem.STREAM_CODEC.apply(ByteBufCodecs.list()),
            C2SStorePurchasePacket::items,
            C2SStorePurchasePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(C2SStorePurchasePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Entity entity = player.level().getEntity(packet.entityId);
                if (entity instanceof StoreEntity storeEntity) {
                    if (!(player.containerMenu instanceof StoreMenu storeMenu)) {
                        return;
                    }
                    if (storeMenu.getStoreEntity() != storeEntity) {
                        return;
                    }
                    if (player.distanceToSqr(storeEntity) > MAX_PURCHASE_DISTANCE_SQR) {
                        return;
                    }
                    if (packet.items == null || packet.items.isEmpty() || packet.items.size() > MAX_REQUEST_ITEMS) {
                        return;
                    }
                    TeamManager manager = TeamManager.getInstance();
                    TeamData team = manager.getPlayerTeam(player);
                    
                    if (team == null) {
                        return;
                    }
                    
                    int totalPrice = 0;
                    List<ItemStack> toGive = new ArrayList<>();
                    List<StoreEntity.StoreItem> toDeductStock = new ArrayList<>();
                    List<Integer> deductQuantities = new ArrayList<>();
                    Map<StoreEntity.StoreItem, Integer> requestedByStock = new IdentityHashMap<>();
                    
                    // 验证并计算总价
                    for (PurchaseItem request : packet.items) {
                        if (request == null || request.stack == null || request.stack.isEmpty()) {
                            return;
                        }
                        if (request.quantity <= 0 || request.quantity > request.stack.getMaxStackSize()) {
                            return;
                        }
                        // 在商店库存中查找匹配项
                        boolean found = false;
                        for (StoreEntity.StoreItem stockItem : storeEntity.getStoreItems()) {
                            if (ItemStack.isSameItemSameComponents(stockItem.getItemStack(), request.stack)) {
                                if (!storeEntity.canPurchase(stockItem)) {
                                    return;
                                }
                                int aggregatedQuantity = requestedByStock.getOrDefault(stockItem, 0) + request.quantity;
                                if (aggregatedQuantity <= 0) {
                                    return;
                                }
                                // 检查库存
                                if (stockItem.getMaxStock() != -1 && stockItem.getCurrentStock() < aggregatedQuantity) {
                                    // 库存不足，交易失败 (或者只买部分？这里简单处理为失败)
                                    return;
                                }
                                requestedByStock.put(stockItem, aggregatedQuantity);
                                
                                totalPrice += storeEntity.getPurchasePrice(stockItem) * request.quantity;
                                if (totalPrice <= 0) {
                                    return;
                                }
                                ItemStack stack = stockItem.getItemStack().copy();
                                stack.setCount(request.quantity);
                                toGive.add(stack);
                                
                                toDeductStock.add(stockItem);
                                deductQuantities.add(request.quantity);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            // 请求了商店没有的物品，可能是作弊或数据不同步
                            return;
                        }
                    }
                    
                    // 检查余额
                    if (totalPrice <= 0) {
                        return;
                    }
                    if (team.getCoins() >= totalPrice) {
                        // 扣钱
                        team.removeCoins(totalPrice, player.getServer());
                        storeEntity.addSpentCoins(totalPrice);
                        manager.syncTeam(team, player.getServer());

                        // 扣除库存
                        for (int i = 0; i < toDeductStock.size(); i++) {
                            StoreEntity.StoreItem stockItem = toDeductStock.get(i);
                            int quantity = deductQuantities.get(i);
                            if (stockItem.getMaxStock() != -1) {
                                stockItem.setCurrentStock(stockItem.getCurrentStock() - quantity);
                            }
                        }
                        
                        // 发货
                        for (ItemStack stack : toGive) {
                            if (!player.getInventory().add(stack)) {
                                player.drop(stack, false);
                            }
                        }
                        
                        // 同步商店库存变化给附近玩家 (简单起见，可以只让当前打开界面的玩家刷新，或者依赖定期同步)
                        // 目前 StoreEntity 没有自动同步库存给所有打开的 Container 的机制，
                        // 但 StoreMenu 可以监听。
                        // 这里我们手动让客户端刷新可能比较复杂，StoreEntity 数据变化应自动同步。
                        // 如果 StoreEntity 使用 EntityDataSerializers 则会自动同步，但这里是用 NBT/List。
                        // 简单做法：不做额外同步，下次打开时更新。或者发送一个更新包。
                    }
                }
            }
        });
    }
    
    // 移除多余的 PurchaseItem 类定义
}
