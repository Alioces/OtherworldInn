package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.world.inventory.StoreMenu;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(BuiltInRegistries.MENU, OtherworldInn.MODID);

    public static final Supplier<MenuType<StoreMenu>> STORE_MENU =
            MENU_TYPES.register("store_menu", () -> IMenuTypeExtension.create(StoreMenu::new));
}
