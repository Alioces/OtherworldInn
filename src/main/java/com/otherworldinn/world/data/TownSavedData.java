package com.otherworldinn.world.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 城镇数据
 * <p>
 * 存储城镇维度的状态，例如是否已生成初始建筑。
 * </p>
 */
public class TownSavedData extends SavedData {
    private static final String DATA_NAME = "otherworldinn_town";
    private boolean generated = false;

    public static TownSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
                TownSavedData::new,
                TownSavedData::load,
                null
        ), DATA_NAME);
    }

    public TownSavedData() {
    }

    public static TownSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        TownSavedData data = new TownSavedData();
        data.generated = tag.getBoolean("generated");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("generated", generated);
        return tag;
    }

    public boolean isGenerated() {
        return generated;
    }

    public void setGenerated(boolean generated) {
        this.generated = generated;
        this.setDirty();
    }
}
