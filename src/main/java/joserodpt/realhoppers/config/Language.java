package joserodpt.realhoppers.config;

/*
 *   ____            _ _   _
 *  |  _ \ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___
 *  | |_) / _ \/ _` | | |_| |/ _ \| '_ \| '_ \ / _ \ '__/ __|
 *  |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \__ \
 *  |_| \_\___|\__,_|_|_| |_|\___/| .__/| .__/ \___|_|  |___/
 *                                |_|   |_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues
 * @link https://github.com/joserodpt/RealHoppers
 */

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class Language implements Listener {

    private static final String name = "language.yml";
    private static YamlDocument configFile;

    public static void setup(final JavaPlugin rm) {
        try {
            configFile = YamlDocument.create(new File(rm.getDataFolder(), name), rm.getResource(name),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("Version")).build());
        } catch (final IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Couldn't setup " + name + "!");
        }
    }

    public static YamlDocument file() {
        return configFile;
    }

    public static void save() {
        try {
            configFile.save();
        } catch (final IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Couldn't save " + name + "!");
        }
    }

    public static void reload() {
        try {
            configFile.reload();
        } catch (final IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Couldn't reload " + name + "!");
        }
    }
}