package joserodpt.realhoppers.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import joserodpt.realhoppers.RealHoppers;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class Hoppers {

    private static String name = "hoppers.yml";

    private static YamlDocument document;

    public static void setup(final JavaPlugin rm) {
        try {
            document = YamlDocument.create(new File(rm.getDataFolder(), name));
        } catch (final IOException e) {
            RealHoppers.getPlugin().getLogger().severe( "Couldn't setup " + name + "!");
            RealHoppers.getPlugin().getLogger().severe(e.getMessage());
        }
    }

    public static YamlDocument file() {
        return document;
    }

    public static void save() {
        try {
            document.save();
        } catch (final IOException e) {
            RealHoppers.getPlugin().getLogger().severe( "Couldn't save " + name + "!");
        }
    }

    public static void reload() {
        try {
            document.reload();
        } catch (final IOException e) {
            RealHoppers.getPlugin().getLogger().severe( "Couldn't reload " + name + "!");
        }
    }
}
