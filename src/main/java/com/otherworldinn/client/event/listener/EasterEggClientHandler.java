package com.otherworldinn.client.event.listener;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.PlayerEasterEggFlags;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = OtherworldInn.MODID, value = Dist.CLIENT)
public class EasterEggClientHandler {
    private static final ResourceLocation MAIMAI_SOUND_ID =
            ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "maimai");
    private static final Set<ResourceLocation> TRIGGER_BLOCK_IDS =
            Set.of(ResourceLocation.fromNamespaceAndPath("yuushya", "washing_machine"),
                    ResourceLocation.fromNamespaceAndPath("yuushya", "washing_machine_sym")
            );

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
        boolean shouldEnable = TRIGGER_BLOCK_IDS.contains(blockId);
        boolean wasEnabled = PlayerEasterEggFlags.isMaimaiAtlasEnabled();
        PlayerEasterEggFlags.setMaimaiAtlasEnabled(shouldEnable);
        if (shouldEnable && !wasEnabled) {
            event.getEntity().swing(event.getHand());
            level.playLocalSound(
                    event.getPos().getX() + 0.5D,
                    event.getPos().getY() + 0.5D,
                    event.getPos().getZ() + 0.5D,
                    SoundEvent.createVariableRangeEvent(MAIMAI_SOUND_ID),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F,
                    false);
        }
    }
}
