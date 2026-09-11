package club.rainbowkitty.companions.datagen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.equipment.ArmorType;

import club.rainbowkitty.companions.Main;
import club.rainbowkitty.companions.WolfSweater;
import club.rainbowkitty.rkcore.common.armor.ArmorLayerTarget;
import club.rainbowkitty.rkcore.common.armor.datagen.ArmorArt;

/**
 * Base art for every colour: one sweater drawing, tinted sixteen ways. Colours come straight from
 * {@link DyeColor#getTextureDiffuseColor()} rather than a duplicated constant list.
 **/
public final class SweaterArt {
    private static final String ART = "assets/" + Main.MOD_ID + "/textures/";
    private static final String ITEM_ART = ART + "item/wolf_sweater.png";
    private static final String EQUIPMENT_ART = ART + "entity/equipment/wolf_body/wolf_sweater.png";

    private SweaterArt() {
    }

    /** One entry per colour, each tinting the shared art into that variant's textures. */
    public static List<ArmorArt> all() {
        List<ArmorArt> art = new ArrayList<>();
        for (DyeColor colour : DyeColor.values()) {
            art.add(new ArmorArt(WolfSweater.set(colour), colour.getTextureDiffuseColor(),
                    Map.of(ArmorType.BODY, ITEM_ART),
                    Map.of(ArmorLayerTarget.WOLF_BODY, EQUIPMENT_ART)));
        }
        return art;
    }
}
