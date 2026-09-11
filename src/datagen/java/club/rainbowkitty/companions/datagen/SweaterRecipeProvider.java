package club.rainbowkitty.companions.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;

import club.rainbowkitty.companions.WolfSweater;

/**
 * Per-colour sweater recipes, shaped from wool and matching vanilla wolf armor's own recipe shape.
 **/
public final class SweaterRecipeProvider extends FabricRecipeProvider {
    public SweaterRecipeProvider(FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new RecipeProvider(registries, output) {
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
