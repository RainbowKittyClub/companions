package club.rainbowkitty.companions.datagen;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;

import club.rainbowkitty.companions.Main;
import club.rainbowkitty.companions.WolfSweater;

/** The {@code companions:wolf_sweaters} item tag, containing every colour variant. **/
public final class SweaterTagProvider extends FabricTagsProvider.ItemTagsProvider {
    public static final TagKey<Item> WOLF_SWEATERS = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(Main.MOD_ID, "wolf_sweaters"));

    public SweaterTagProvider(FabricPackOutput output,
            CompletableFuture<HolderLookup.Provider> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        var builder = tag(WOLF_SWEATERS);
        for (DyeColor colour : DyeColor.values()) {
            builder.add(WolfSweater.key(colour));
        }
    }
}
