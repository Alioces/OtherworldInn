package com.otherworldinn.compat.jei;

import com.otherworldinn.init.ModItems;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class NpcStoreJeiCategory implements IRecipeCategory<NpcStoreJeiRecipe> {
    private static final int WIDTH = 166;
    private static final int HEIGHT = 70;
    private static final String FARMER = "entity.otherworldinn.farmer";
    private static final String BLACKSMITH = "entity.otherworldinn.blacksmith";
    private static final String MAGICIAN = "entity.otherworldinn.magician";
    private static final String GROCER = "entity.otherworldinn.grocer";
    private static final ResourceLocation BG_FARMER =
            ResourceLocation.fromNamespaceAndPath("otherworldinn", "textures/gui/jei/npc_store_farmer.png");
    private static final ResourceLocation BG_BLACKSMITH =
            ResourceLocation.fromNamespaceAndPath(
                    "otherworldinn", "textures/gui/jei/npc_store_blacksmith.png");
    private static final ResourceLocation BG_MAGICIAN =
            ResourceLocation.fromNamespaceAndPath("otherworldinn", "textures/gui/jei/npc_store_magician.png");
    private static final ResourceLocation BG_GROCER =
            ResourceLocation.fromNamespaceAndPath("otherworldinn", "textures/gui/jei/npc_store_grocer.png");

    private final IDrawableStatic background;
    private final IDrawable icon;

    NpcStoreJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(ModItems.COIN.get().getDefaultInstance());
    }

    @Override
    public mezz.jei.api.recipe.RecipeType<NpcStoreJeiRecipe> getRecipeType() {
        return NpcStoreJeiPlugin.NPC_STORE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.otherworldinn.npc_store.title");
    }

    @Override
    public IDrawableStatic getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, NpcStoreJeiRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(8, 24).setStandardSlotBackground().addItemStacks(buildCoinDisplayStacks(recipe));
        builder.addOutputSlot(50, 24).setOutputSlotBackground().addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(
            NpcStoreJeiRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        boolean hasCustomBackground = drawNpcBackground(guiGraphics, recipe);
        Font font = Minecraft.getInstance().font;

        int x = 70;
        int y = 4;
        if (!hasCustomBackground) {
            guiGraphics.drawString(
                    font,
                    Component.translatable(
                            "jei.otherworldinn.npc_store.store",
                            Component.translatable(recipe.storeNameKey())),
                    x,
                    y,
                    0x404040,
                    false);
            y += 12;
        }
        if (recipe.requiredFavorLevel() > 1) {
            guiGraphics.drawString(
                    font,
                    Component.translatable("jei.otherworldinn.npc_store.favor", recipe.requiredFavorLevel()),
                    x,
                    y,
                    0x8B5A2B,
                    false);
            y += 10;
        }
        if (recipe.requiredAdvancementTitleKey() != null) {
            guiGraphics.drawString(
                    font,
                    Component.translatable(
                            "jei.otherworldinn.npc_store.advancement",
                            Component.translatable(recipe.requiredAdvancementTitleKey())),
                    x,
                    y,
                    0x8A2BE2,
                    false);
            y += 10;
        }
        if (recipe.randomOffer()) {
            guiGraphics.drawString(
                    font,
                    Component.translatable("jei.otherworldinn.npc_store.random"),
                    x,
                    y,
                    0x2E8B57,
                    false);
        }
    }

    private static List<ItemStack> buildCoinDisplayStacks(NpcStoreJeiRecipe recipe) {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(ModItems.COIN.get(), recipe.minPrice()));
        if (recipe.maxPrice() > recipe.minPrice()) {
            stacks.add(new ItemStack(ModItems.COIN.get(), recipe.maxPrice()));
        }
        return stacks;
    }

    private static boolean drawNpcBackground(GuiGraphics guiGraphics, NpcStoreJeiRecipe recipe) {
        ResourceLocation texture = switch (recipe.storeNameKey()) {
            case FARMER -> BG_FARMER;
            case BLACKSMITH -> BG_BLACKSMITH;
            case MAGICIAN -> BG_MAGICIAN;
            case GROCER -> BG_GROCER;
            default -> BG_FARMER;
        };
        if (!Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) {
            // 纹理不存在时回退 JEI 默认背景表现（不绘制自定义背景层）。
            return false;
        }
        guiGraphics.blit(texture, 0, 0, 0, 0, WIDTH, HEIGHT, WIDTH, HEIGHT);
        return true;
    }
}
