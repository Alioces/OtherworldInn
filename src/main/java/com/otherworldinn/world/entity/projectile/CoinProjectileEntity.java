package com.otherworldinn.world.entity.projectile;

import com.otherworldinn.init.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class CoinProjectileEntity extends ThrowableItemProjectile {
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
        int damage = 0;
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level().isClientSide) {
            return;
        }
        spawnAtLocation(new ItemStack(ModItems.COIN.get()));
        discard();
    }
}
