package com.otherworldinn.client.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.PlayerEasterEggFlags;
import com.otherworldinn.init.ModItems;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class EasterEggClientHandler {
    private static final String SERVER_STATE_NBT_KEY = "otherworldinn_maimai_enabled";
    private static final ResourceLocation MAIMAI_SOUND_ID =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "maimai");
    private static final ResourceLocation MAIMAI_END_SOUND_ID =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "maimai_end");
    private static final Set<ResourceLocation> TRIGGER_BLOCK_IDS =
            Set.of(ResourceLocation.fromNamespaceAndPath("yuushya", "washing_machine"),
                    ResourceLocation.fromNamespaceAndPath("yuushya", "washing_machine_sym")
            );

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        Level level = event.getLevel();
        ResourceLocation blockId =
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock());
        boolean isTriggerBlock = TRIGGER_BLOCK_IDS.contains(blockId);
        if (!isTriggerBlock) {
            return;
        }
        if (level.isClientSide) {
            boolean wasEnabled = PlayerEasterEggFlags.isMaimaiAtlasEnabled();
            boolean shouldEnable = !wasEnabled;
            if (shouldEnable && !isHoldingCoin(player)) {
                return;
            }
            PlayerEasterEggFlags.setMaimaiAtlasEnabled(shouldEnable);
            player.swing(event.getHand());
            if (shouldEnable && !wasEnabled) {
                consumeOneCoin(player);
            }
            level.playLocalSound(
                    event.getPos().getX() + 0.5D,
                    event.getPos().getY() + 0.5D,
                    event.getPos().getZ() + 0.5D,
                    SoundEvent.createVariableRangeEvent(shouldEnable ? MAIMAI_SOUND_ID : MAIMAI_END_SOUND_ID),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F,
                    false);
            return;
        }
        CompoundTag persistentData = player.getPersistentData();
        boolean wasEnabled = persistentData.getBoolean(SERVER_STATE_NBT_KEY);
        boolean shouldEnable = !wasEnabled;
        if (shouldEnable) {
            if (!isHoldingCoin(player)) {
                return;
            }
            consumeOneCoin(player);
        }
        persistentData.putBoolean(SERVER_STATE_NBT_KEY, shouldEnable);
    }

    private static boolean isHoldingCoin(Player player) {
        return player.getMainHandItem().is(ModItems.COIN.get());
    }

    private static void consumeOneCoin(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty()) {
            stack.shrink(1);
        }
    }
}
