package com.otherworldinn.world.expedition;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.dimension.TownDimensions;
import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public final class ExpeditionDimensions {

    private ExpeditionDimensions() {}

    public static ResourceKey<Level> createChartKey(UUID chartUuid) {
        return ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID,
                        "expedition_" + chartUuid.toString()));
    }

    public static boolean isExpeditionDimension(ResourceKey<Level> key) {
        if (key == null) return false;
        if (!OtherworldInn.MODID.equals(key.location().getNamespace())) return false;
        String path = key.location().getPath();
        return path.startsWith("expedition_")
                || path.equals("expedition");
    }
}
