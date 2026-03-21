package com.otherworldinn.client.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.PlayerEasterEggFlags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class EasterEggClientHandler {
    private static final ResourceLocation TRIGGER_BLOCK_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "crafting_table");

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Level level = event.getLevel();
        if (!level.isClientSide) {
            return;
        }
        ResourceLocation blockId =
                BuiltInRegistries.BLOCK.getKey(level.getBlockState(event.getPos()).getBlock());
        if (TRIGGER_BLOCK_ID.equals(blockId)) {
            PlayerEasterEggFlags.setMaimaiAtlasEnabled(true);
        } else {
            PlayerEasterEggFlags.setMaimaiAtlasEnabled(false);
        }
    }
}
