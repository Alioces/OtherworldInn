package com.otherworldinn.world.entity.projectile;

import com.otherworldinn.init.ModItems;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class CoinProjectileEntity extends ThrowableItemProjectile {
    private static final ResourceLocation CHANNELING_COIN_ADVANCEMENT_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "otherworldinn", "channeling_coin_hidden");

    public CoinProjectileEntity(EntityType<? extends CoinProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public CoinProjectileEntity(
            EntityType<? extends CoinProjectileEntity> entityType, LivingEntity owner, Level level) {
        super(entityType, owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.COIN.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide) {
            return;
        }
        int channelingLevel = getChannelingLevel();
        if (channelingLevel > 0 && getOwner() instanceof ServerPlayer serverPlayer) {
            awardChannelingCoinAdvancement(serverPlayer);
        }
        maybeChannelLightning(result, channelingLevel);
        int damage = 0;
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        ItemStack dropped = this.getItem().copy();
        dropped.setCount(1);
        if (dropped.isEmpty()) {
            dropped = new ItemStack(ModItems.COIN.get());
        }
        spawnAtLocation(dropped);
        discard();
    }

    private void maybeChannelLightning(EntityHitResult result, int channelingLevel) {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }
        if (!serverLevel.isThundering()) {
            return;
        }
        BlockPos strikePos = result.getEntity().blockPosition();
        if (!serverLevel.canSeeSky(strikePos)) {
            return;
        }
        if (channelingLevel <= 0) {
            return;
        }
        LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightning == null) {
            return;
        }
        lightning.moveTo(result.getEntity().getX(), result.getEntity().getY(), result.getEntity().getZ());
        if (getOwner() instanceof ServerPlayer serverPlayer) {
            lightning.setCause(serverPlayer);
        }
        serverLevel.addFreshEntity(lightning);
    }

    private int getChannelingLevel() {
        if (!(level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return 0;
        }
        ItemStack projectileStack = this.getItem();
        return serverLevel.registryAccess()
                .lookup(Registries.ENCHANTMENT)
                .flatMap(registry -> registry.get(Enchantments.CHANNELING))
                .map(projectileStack::getEnchantmentLevel)
                .orElse(0);
    }

    private void awardChannelingCoinAdvancement(ServerPlayer player) {
        AdvancementHolder advancement =
                player.server.getAdvancements().get(CHANNELING_COIN_ADVANCEMENT_ID);
        if (advancement == null) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
