package club.rainbowkitty.companions.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

import club.rainbowkitty.companions.WolfSweater;
import club.rainbowkitty.rkcore.common.armor.datagen.ArmorDatagen;

/** Datagen entrypoint; every per-colour asset, including the tinted textures, comes from here. **/
public final class CompanionsDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(SweaterRecipeProvider::new);
        pack.addProvider(SweaterTagProvider::new);

        ArmorDatagen.addProviders(pack, WolfSweater.sets(), SweaterArt.all());
    }
}
