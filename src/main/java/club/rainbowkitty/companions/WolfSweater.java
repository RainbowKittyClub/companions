package club.rainbowkitty.companions;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

import club.rainbowkitty.rkcore.common.armor.ArmorLayer;
import club.rainbowkitty.rkcore.common.armor.ArmorLayerTarget;
import club.rainbowkitty.rkcore.common.armor.ArmorRegistration;
import club.rainbowkitty.rkcore.common.armor.ArmorSet;

/**
 * Wolf-worn sweaters, registered alongside vanilla wolf armor — one item per vanilla dye colour.
 * A variant's own colour is baked into its textures, so it wears that colour with no
 * {@code minecraft:dyed_color} component; applying a dye recolours only its trim.
 **/
public final class WolfSweater {
    /** Armor points a sweater grants, a little under armadillo-scute wolf armor's own. */
    public static final int DEFENSE = 17;

    /** The trim texture, shared by every colour and tinted by the client once a sweater is dyed. */
    public static final Identifier OVERLAY =
            Identifier.fromNamespaceAndPath(Main.MOD_ID, "wolf_sweater_overlay");

    private static final Map<DyeColor, ArmorSet> SETS = declare();

    private WolfSweater() {
    }

    /** Every colour's set, in {@link DyeColor} order. */
    public static List<ArmorSet> sets() {
        return List.copyOf(SETS.values());
    }

    /** A colour's set. */
    public static ArmorSet set(DyeColor colour) {
        return SETS.get(colour);
    }

    /** Registers every colour variant; call once, before the item registry freezes. **/
    public static void register() {
        SETS.values().forEach(ArmorRegistration::register);
    }

    /** Registry path for a variant, e.g. {@code light_blue_wolf_sweater}. **/
    public static String name(DyeColor colour) {
        return colour.getSerializedName() + "_wolf_sweater";
    }

    /** A variant's registry key, e.g. for use as a {@code TagAppender} element in datagen. **/
    public static ResourceKey<Item> key(DyeColor colour) {
        return ResourceKey.create(Registries.ITEM,
                Identifier.fromNamespaceAndPath(Main.MOD_ID, name(colour)));
    }

    /** The registered item for a variant; only valid to call after {@link #register}. **/
    public static Item item(DyeColor colour) {
        return BuiltInRegistries.ITEM.getValueOrThrow(key(colour));
    }

    private static Map<DyeColor, ArmorSet> declare() {
        Map<DyeColor, ArmorSet> sets = new EnumMap<>(DyeColor.class);
        for (DyeColor colour : DyeColor.values()) {
            Identifier base = Identifier.fromNamespaceAndPath(Main.MOD_ID, name(colour));
            sets.put(colour, ArmorSet.builder(Main.MOD_ID, name(colour))
                    .defense(ArmorType.BODY, DEFENSE)
                    .enchantmentValue(10)
                    .equipSound(SoundEvents.ARMOR_EQUIP_WOLF)
                    .repairIngredient(ItemTags.REPAIRS_WOLF_ARMOR)
                    .allowedWearers(EntityTypes.WOLF)
                    .equippable(equippable -> equippable.setCanBeSheared(true)
                            .setShearingSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(
                                    SoundEvents.ARMOR_UNEQUIP_WOLF)))
                    .piece(ArmorType.BODY, piece -> piece.drawsOn(ArmorLayerTarget.WOLF_BODY,
                            ArmorLayer.plain(base), ArmorLayer.onlyIfDyed(OVERLAY)))
                    .build());
        }
        return sets;
    }
}
