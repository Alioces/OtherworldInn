package com.otherworldinn.mixin;

import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MinecraftServer.class)
public interface MixinMinecraftServerLevelsAccessor {

    @Accessor("levels")
    Map<ResourceKey<Level>, ServerLevel> otherworldinn$getLevels();
}
