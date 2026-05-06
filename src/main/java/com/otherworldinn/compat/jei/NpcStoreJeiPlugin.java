package com.otherworldinn.compat.jei;

import com.otherworldinn.OtherworldInn;
import com.otherworldinn.init.ModItems;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class NpcStoreJeiPlugin implements IModPlugin {
    public static final RecipeType<NpcStoreJeiRecipe> NPC_STORE_RECIPE_TYPE =
            RecipeType.create(OtherworldInn.MODID, "npc_store", NpcStoreJeiRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(OtherworldInn.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new NpcStoreJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<NpcStoreJeiRecipe> recipes = NpcStoreJeiData.getAllRecipes();
        registration.addRecipes(NPC_STORE_RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.COIN.get()), NPC_STORE_RECIPE_TYPE);
    }
}
