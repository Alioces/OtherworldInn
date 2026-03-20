package com.otherworldinn.entity.guest;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.entity.base.VipGuestEntity;
import com.otherworldinn.world.inn.GuestData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

public class OrdinaryVipGuestEntity extends VipGuestEntity {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    OtherworldInn.MODID, "textures/entity/guest/ordinary_guest/1.png");

    public OrdinaryVipGuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public String getModelType() {
        return "slim";
    }

    @Override
    protected GuestProfile getGuestProfile() {
        return new GuestProfile(
                new PreferenceRangeProfile(new GuestData.IntRange(44, 62), new GuestData.IntRange(80, 100)),
                new PreferenceRangeProfile(new GuestData.IntRange(46, 64), new GuestData.IntRange(70, 100)),
                new PreferenceRangeProfile(new GuestData.IntRange(22, 30), new GuestData.IntRange(65, 100)),
                new GuestData.IntRange(86, 140));
    }

    @Override
    public ResourceLocation getSkinTexture() {
        return DEFAULT_TEXTURE;
    }
}
