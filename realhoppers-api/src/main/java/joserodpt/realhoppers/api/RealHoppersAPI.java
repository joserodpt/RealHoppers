package joserodpt.realhoppers.api;

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

import com.google.common.base.Preconditions;
import joserodpt.realhoppers.api.managers.HopperManagerAPI;
import joserodpt.realhoppers.api.managers.PlayerManagerAPI;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public abstract class RealHoppersAPI {

    private static RealHoppersAPI instance;

    /**
     * Gets instance of this API
     *
     * @return RealHoppersAPI API instance
     */
    public static RealHoppersAPI getInstance() {
        return instance;
    }

    /**
     * Sets the RealHoppersAPI instance.
     * <b>Note! This method may only be called once</b>
     *
     * @param instance the new instance to set
     */
    public static void setInstance(RealHoppersAPI instance) {
        Preconditions.checkNotNull(instance, "instance");
        Preconditions.checkArgument(RealHoppersAPI.instance == null, "Instance already set");
        RealHoppersAPI.instance = instance;
    }

    public abstract JavaPlugin getPlugin();

    public abstract PlayerManagerAPI getPlayerManager();

    public abstract HopperManagerAPI getHopperManager();

    public abstract void reload();

    public abstract String getVersion();

    /** Named to match RealMinesAPI; it was getVault. */
    public abstract Economy getEconomy();

    public abstract Logger getLogger();
}