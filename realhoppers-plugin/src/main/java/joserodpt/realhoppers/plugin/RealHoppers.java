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
 * @author José Rodrigues
 * @link https://github.com/joserodpt/RealHoppers
 */

import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.config.RHHoppers;
import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.managers.HopperManagerAPI;
import joserodpt.realhoppers.api.managers.PlayerManagerAPI;
import joserodpt.realhoppers.plugin.managers.HopperManager;
import joserodpt.realhoppers.plugin.managers.PlayerManager;
import net.milkbowl.vault.economy.Economy;

import java.util.logging.Logger;

public class RealHoppers extends RealHoppersAPI {

    private final Logger logger;
    private final RealHoppersPlugin plugin;
    private final HopperManagerAPI hopperManagerAPI;
    private final PlayerManagerAPI playerManagerAPI;
    private final Economy econ;

    public RealHoppers(RealHoppersPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();

        RHConfig.setup(plugin);
        RHHoppers.setup(plugin);
        RHLanguage.setup(plugin);

        this.hopperManagerAPI = new HopperManager(this);
        this.playerManagerAPI = new PlayerManager();
        this.econ = plugin.getVault();
    }

    @Override
    public RealHoppersPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public HopperManagerAPI getHopperManager() {
        return this.hopperManagerAPI;
    }
    @Override
    public PlayerManagerAPI getPlayerManager() {
        return this.playerManagerAPI;
    }
    @Override
    public boolean hasNewUpdate() {
        return false; //TODO
    }

    @Override
    public void reload() {
        RHConfig.reload();
        RHHoppers.reload();
        //reload worlds config
        this.getHopperManager().loadHoppers();
    }

    @Override
    public Economy getVault() {
        return this.econ;
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