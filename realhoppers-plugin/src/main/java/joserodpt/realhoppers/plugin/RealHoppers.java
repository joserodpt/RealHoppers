package joserodpt.realhoppers.plugin;

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

import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.config.RHSQLConfig;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.managers.HopperManagerAPI;
import joserodpt.realhoppers.api.utils.Compacting;
import joserodpt.realhoppers.api.utils.Smelting;
import joserodpt.realhoppers.api.managers.PlayerManagerAPI;
import joserodpt.realhoppers.plugin.gui.GUIManager;
import joserodpt.realhoppers.plugin.managers.DatabaseManager;
import joserodpt.realhoppers.plugin.managers.HopperManager;
import joserodpt.realhoppers.plugin.managers.PlayerManager;
import net.milkbowl.vault.economy.Economy;

import java.sql.SQLException;
import java.util.logging.Logger;

public class RealHoppers extends RealHoppersAPI {

    private final Logger logger;
    private final RealHoppersPlugin plugin;
    private final HopperManagerAPI hopperManagerAPI;
    private final PlayerManagerAPI playerManagerAPI;
    private final GUIManager guiManager;
    private DatabaseManager databaseManager;

    public RealHoppers(RealHoppersPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        RHConfig.setup(plugin);
        RHLanguage.setup(plugin);
        RHSQLConfig.setup(plugin);

        try {
            this.databaseManager = new DatabaseManager(this);
        } catch (final SQLException e) {
            //left null, and the plugin disables itself: a hopper that cannot be saved would be
            //lost on the next restart along with everything it had banked
            this.logger.severe("Could not connect to the database: " + e.getMessage());
        }

        this.hopperManagerAPI = new HopperManager(this);
        this.playerManagerAPI = new PlayerManager();
        this.guiManager = new GUIManager(this);
    }

    @Override
    public RealHoppersPlugin getPlugin() {
        return this.plugin;
    }

    /**
     * Lives on the plugin class rather than on RealHoppersAPI: the GUIs are the plugin's, and the
     * API module cannot see them.
     */
    public GUIManager getGUIManager() {
        return this.guiManager;
    }

    @Override
    public HopperManagerAPI getHopperManager() {
        return this.hopperManagerAPI;
    }
    /** Null only when the connection failed at startup, in which case the plugin disables itself. */
    @Override
    public DatabaseManager getDatabaseManager() {
        return this.databaseManager;
    }

    @Override
    public PlayerManagerAPI getPlayerManager() {
        return this.playerManagerAPI;
    }
    @Override
    public void reload() {
        RHConfig.reload();
        RHLanguage.reload();
        //everything changed so far is written before the hoppers are read back, or the reload
        //would hand them what the database had at the last flush
        this.databaseManager.flush(true);
        //a datapack or plugin may have added furnace recipes since the last load
        Smelting.load();
        Compacting.load();
        RHopperTrait.loadTiers();
        //loadHoppers stops whatever is running before it replaces the map
        this.getHopperManager().loadHoppers();
    }

    @Override
    public Economy getEconomy() {
        return plugin.getEconomy();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}