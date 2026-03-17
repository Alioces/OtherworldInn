package com.otherworldinn.init;

import com.otherworldinn.OtherworldInn;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 创造模式选项卡注册 */
public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OtherworldInn.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> OTHERWORLD_INN_TAB =
            CREATIVE_MODE_TABS.register(
                    "otherworld_inn_tab",
                    () ->
                            CreativeModeTab.builder()
                                    .title(Component.translatable("itemGroup.otherworldinn"))
                                    .icon(() -> new ItemStack(ModItems.RECALL_SCROLL.get()))
                                    .displayItems(
                                            (parameters, output) -> {
                                                // 添加物品
                                                output.accept(ModItems.RECALL_SCROLL.get());
                                                output.accept(ModItems.ROOM_REGISTER.get());
                                                output.accept(ModItems.BED_SHEET.get());
                                                output.accept(ModItems.MESSY_BED_SHEET.get());
                                                output.accept(ModItems.LAND_DEED.get());
                                                output.accept(ModItems.INN_KEY.get());
                                                output.accept(ModItems.ROOM_KEY.get());
                                                output.accept(ModItems.SPACE_SPHERE.get());
                                                output.accept(ModItems.NETHER_SPACE_SPHERE.get());
                                                output.accept(ModItems.END_SPACE_SPHERE.get());
                                                output.accept(ModItems.FACILITY_UPGRADE_TEMPLATE.get());
                                                BuiltInRegistries.ITEM
                                                        .getOptional(
                                                                ResourceLocation.parse(
                                                                        "patchouli:guide_book"))
                                                        .ifPresent(
                                                                item -> {
                                                                    ItemStack guideBook =
                                                                            new ItemStack(item);
                                                                    ResourceLocation bookId =
                                                                            ResourceLocation.parse(
                                                                                    "otherworldinn:otherworldinn_guide");
                                                                    CompoundTag tag =
                                                                            new CompoundTag();
                                                                    tag.putString(
                                                                            "patchouli:book",
                                                                            bookId.toString());
                                                                    guideBook.set(
                                                                            DataComponents
                                                                                    .CUSTOM_DATA,
                                                                            CustomData.of(tag));
                                                                    BuiltInRegistries
                                                                            .DATA_COMPONENT_TYPE
                                                                            .getOptional(
                                                                                    ResourceLocation
                                                                                            .parse(
                                                                                                    "patchouli:book"))
                                                                            .ifPresent(
                                                                                    componentType -> {
                                                                                        try {
                                                                                            @SuppressWarnings(
                                                                                                    "unchecked")
                                                                                            DataComponentType<
                                                                                                            ResourceLocation>
                                                                                                    patchouliBookComponent =
                                                                                                            (DataComponentType<
                                                                                                                            ResourceLocation>)
                                                                                                                    componentType;
                                                                                            guideBook
                                                                                                    .set(
                                                                                                            patchouliBookComponent,
                                                                                                            bookId);
                                                                                        } catch (ClassCastException
                                                                                                ignored) {
                                                                                        }
                                                                                    });
                                                                    output.accept(guideBook);
                                                                });
                                                output.accept(ModBlocks.OVERWORLD_PORTAL.get());
                                            })
                                    .build());
}
