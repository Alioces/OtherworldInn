package com.otherworldinn.entity;

import com.otherworldinn.OtherworldInn;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.Util;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 普通旅客实体
 * <p>
 * 使用 Alex (slim) 模型。
 * 支持多种皮肤变体。
 * </p>
 */
public class OrdinaryGuestEntity extends GuestEntity {

    private static final ResourceLocation DEFAULT_TEXTURE = ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "textures/entity/guest/ordinary_guest/1.png");
    private static final List<ResourceLocation> TEXTURES = new ArrayList<>();
    private static boolean texturesLoaded = false;

    public OrdinaryGuestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public ResourceLocation getSkinTexture() {
        // 懒加载纹理列表 (仅限客户端)
        if (!texturesLoaded && this.level().isClientSide) {
            try {
                // 使用反射或直接调用客户端工具类加载纹理
                // 为了避免服务端类加载错误，这里假设 TextureUtils 是安全的或使用全限定名
                List<ResourceLocation> found = com.otherworldinn.client.util.TextureUtils.findTexturesInFolder(OtherworldInn.MODID, "textures/entity/guest/ordinary_guest");
                if (!found.isEmpty()) {
                    TEXTURES.clear();
                    TEXTURES.addAll(found);
                }
            } catch (Throwable e) {
                // 忽略加载错误 (例如在服务端)
            }
            texturesLoaded = true;
        }
        
        if (TEXTURES.isEmpty()) {
            return DEFAULT_TEXTURE;
        }
        
        // 使用取模运算确保索引有效
        return TEXTURES.get(Math.abs(this.getSkinVariant()) % TEXTURES.size());
    }

    @Override
    public String getModelType() {
        return "slim";
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        spawnData = super.finalizeSpawn(level, difficulty, reason, spawnData);
        // 随机设置一个较大的变体索引，客户端通过取模来映射到具体纹理
        // 这样服务端不需要知道具体的纹理数量
        this.setSkinVariant(this.getRandom().nextInt(10000));
        return spawnData;
    }
}
