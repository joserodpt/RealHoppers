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
 * @author José Rodrigues © 2019-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import dev.dejvokep.boostedyaml.YamlDocument;
import joserodpt.realhoppers.api.RealHoppersAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class RHHoppers {

    private static final String name = "hoppers.yml";

    private static YamlDocument document;

    /**
     * Set whenever a hopper writes into the document, cleared by {@link #saveIfDirty()}. Hoppers
     * change constantly - every item an auto-seller swallows moves its balance - and rewriting the
     * whole file on each of those was the plugin's heaviest piece of IO.
     */
    private static boolean dirty;

    public static void setup(final JavaPlugin rm) {
        try {
            document = YamlDocument.create(new File(rm.getDataFolder(), name));
        } catch (final IOException e) {
            RealHoppersAPI.getInstance().getLogger().severe( "Couldn't setup " + name + "!");
            RealHoppersAPI.getInstance().getLogger().severe(e.getMessage());
        }
    }

    public static YamlDocument file() {
        return document;
    }

    public static void markDirty() {
        dirty = true;
    }

    /**
     * Writes the file only if something changed since the last write. Called on a timer and once
     * more on shutdown.
     *
     * @return whether anything was written
     */
    public static boolean saveIfDirty() {
        if (!dirty) {
            return false;
        }
        save();
        return true;
    }

    public static void save() {
        dirty = false;
        try {
            document.save();
        } catch (final IOException e) {
            RealHoppersAPI.getInstance().getLogger().severe( "Couldn't save " + name + "!");
        }
    }

    public static void reload() {
        try {
            document.reload();
        } catch (final IOException e) {
            RealHoppersAPI.getInstance().getLogger().severe( "Couldn't reload " + name + "!");
        }
    }
}
