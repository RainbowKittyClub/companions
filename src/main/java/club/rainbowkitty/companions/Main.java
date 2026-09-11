package club.rainbowkitty.companions;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import club.rainbowkitty.rkcore.common.armor.ArmorPolymerSupport;

/** Mod entrypoint. **/
public class Main implements ModInitializer {
    public static final String MOD_ID = "companions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ArmorPolymerSupport.init(MOD_ID);

        WolfSweater.register();

        LOGGER.info("Companions loaded!");
    }
}
