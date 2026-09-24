package joserodpt.realhoppers.api.config;

/*
 *   ____            _ _   _
 *  |  _ \ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___
 *  | |_) / _ \/ _` | | |_| |/ _ \| '_ \| '_ \ / _ \ '__/ __|
 *  |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \__ \
 *  |_| \_\___|\__,_|_|_| |_|\___/| .__/| .__/ \___|_|  |___/
 *                                |_|   |_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2023-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import dev.dejvokep.boostedyaml.YamlDocument;
import joserodpt.realhoppers.api.RealHoppersAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * sql.yml: where the hoppers are stored. Kept apart from config.yml, as RealSkywars does, so the
 * database credentials never end up pasted along with the rest of the config.
 */
public class RHSQLConfig {

    private static final String name = "sql.yml";

    private static YamlDocument document;

    public static void setup(final JavaPlugin rm) {
        try {
            document = YamlDocument.create(new File(rm.getDataFolder(), name), rm.getResource(name));
        } catch (final IOException e) {
            RealHoppersAPI.getInstance().getLogger().severe("Couldn't setup " + name + "!");
            RealHoppersAPI.getInstance().getLogger().severe(e.getMessage());
        }
    }

    public static YamlDocument file() {
        return document;
    }

    public static void reload() {
        try {
            document.reload();
        } catch (final IOException e) {
            RealHoppersAPI.getInstance().getLogger().severe("Couldn't reload " + name + "!");
        }
    }
}
