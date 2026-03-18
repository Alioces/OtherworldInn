package com.otherworldinn.entity.guest;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.client.util.TextureUtils;
import com.otherworldinn.entity.base.GuestEntity;
import com.otherworldinn.world.inn.GuestData;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

public class HeavyPackGuestEntity extends GuestEntity {
    private static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    OtherworldInn.MODID, "textures/entity/guest/ordinary_guest/1.png");
    private static final ResourceLocation IRON_INGOT_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "iron_ingot");
    private static final ResourceLocation COPPER_INGOT_ID =
            ResourceLocation.fromNamespaceAndPath("minecraft", "copper_ingot");
    private static final ResourceLocation ANDESITE_ALLOY_ID =
            ResourceLocation.fromNamespaceAndPath("create", "andesite_alloy");
    private static final ResourceLocation BRASS_INGOT_ID =
            ResourceLocation.fromNamespaceAndPath("create", "brass_ingot");
    private static final ResourceLocation PRECISION_MECHANISM_ID =
            ResourceLocation.fromNamespaceAndPath("create", "precision_mechanism");
    private static final List<ResourceLocation> TEXTURES = new ArrayList<>();
    private static boolean texturesLoaded = false;

    public HeavyPackGuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected GuestProfile getGuestProfile() {
        return new GuestProfile(
                new PreferenceRangeProfile(new GuestData.IntRange(28, 42), new GuestData.IntRange(72, 88)),
                new PreferenceRangeProfile(new GuestData.IntRange(30, 45), new GuestData.IntRange(74, 90)),
                new PreferenceRangeProfile(new GuestData.IntRange(25, 40), new GuestData.IntRange(70, 86)),
                new GuestData.IntRange(14, 34));
    }

    @Override
    protected void initRewardItems() {
        this.getGuestData().addRewardItem(IRON_INGOT_ID, 0, 3);
        this.getGuestData().addRewardItem(COPPER_INGOT_ID, 0, 4);
        this.getGuestData().addRewardItem(ANDESITE_ALLOY_ID, 0, 2);
        this.getGuestData().addRewardItem(BRASS_INGOT_ID, 0, 1);
        this.getGuestData().addRewardItem(PRECISION_MECHANISM_ID, 0, 1);
    }

    @Override
    public ResourceLocation getSkinTexture() {
        if (!texturesLoaded && this.level().isClientSide) {
            try {
                List<ResourceLocation> found =
                        TextureUtils.findTexturesInFolder(
                                OtherworldInn.MODID, "textures/entity/guest/heavy_pack_guest");
                if (!found.isEmpty()) {
                    TEXTURES.clear();
                    TEXTURES.addAll(found);
                }
            } catch (Throwable e) {
            }
            texturesLoaded = true;
        }

        if (TEXTURES.isEmpty()) {
            return DEFAULT_TEXTURE;
        }
        return TEXTURES.get(Math.abs(this.getSkinVariant()) % TEXTURES.size());
    }

    @Override
    public String getModelType() {
        return "default";
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType reason,
            @Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData);
        this.setSkinVariant(this.getRandom().nextInt(10000));
        return spawnData;
    }
}
