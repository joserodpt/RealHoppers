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
import joserodpt.realutils.config.YamlConfig;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * sql.yml: where the hoppers are stored. Kept apart from config.yml, as RealSkywars does, so the
 * database credentials never end up pasted along with the rest of the config.
 */
public class RHSQLConfig {

    private static YamlConfig config;

    public static void setup(final JavaPlugin rm) {
        //not versioned: it never was, and a server's credentials are left exactly as written
        config = YamlConfig.of(rm, "sql.yml").load();
    }

    /** Null when sql.yml could not be read at setup; the database manager refuses to start then. */
    public static YamlDocument file() {
        return config == null ? null : config.file();
    }

    public static void reload() {
        //nothing to reload when it could not be read at setup, and nothing worth logging again
        if (file() == null) {
            return;
        }
        config.reload();
    }
}
