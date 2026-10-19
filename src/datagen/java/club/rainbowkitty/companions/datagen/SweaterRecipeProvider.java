package club.rainbowkitty.companions.datagen;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

//? if >=26.3
//import net.minecraft.advancements.Advancement;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
//? if <26.3
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
//? if >=26.3
//import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
//? if >=26.3
//import net.minecraft.world.item.crafting.Recipe;

import club.rainbowkitty.companions.WolfSweater;

/**
 * Per-colour sweater recipes, shaped from wool and matching vanilla wolf armor's own recipe shape.
 **/
public final class SweaterRecipeProvider extends FabricRecipeProvider {
    public SweaterRecipeProvider(FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    // 26.3 builds recipes into registry bootstrap contexts rather than a RecipeOutput.
    @Override
    //? if >=26.3 {
    /*protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
            BootstrapContext<Recipe<?>> recipes, BootstrapContext<Advancement> advancements) {
        return new RecipeProvider(recipes, advancements) {
    *///?} else {
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
            RecipeOutput output) {
        return new RecipeProvider(registries, output) {
    //?}
            @Override
            public void buildRecipes() {
                for (DyeColor colour : DyeColor.values()) {
                    Item wool = Items.WOOL.pick(colour);
                    Item sweater = WolfSweater.item(colour);

                    this.shaped(RecipeCategory.MISC, sweater)
                            .pattern("X  ")
                            .pattern("XXX")
                            .pattern("X X")
                            .define('X', wool)
                            .group("wolf_sweater")
                            .unlockedBy(getHasName(wool), this.has(wool))
                            .save(this.output);
                }
            }
        };
    }

    @Override
    public String getName() {
        return "Companions Recipes";
    }
}
