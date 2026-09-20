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
import joserodpt.realhoppers.api.utils.GUIBuilder;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.plugin.command.RHCommandManager;
import joserodpt.realhoppers.plugin.listener.EventListener;
import joserodpt.realhoppers.plugin.listener.PlayerListener;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class RealHoppersPlugin extends JavaPlugin {

    private static Economy econ = null;

    private static RealHoppersPlugin instance;

    private RealHoppers realHoppers;
    private BukkitTask hopperHighlight;
    private BukkitTask hopperFlush;

    public static RealHoppersPlugin getPlugin() {
        return instance;
    }

    @Override
    public void onEnable() {
        printASCII();
        final long start = System.currentTimeMillis();
        //new Metrics(this, 19311); TODO metrics


        instance = this;
        realHoppers = new RealHoppers(this);
        RealHoppersAPI.setInstance(realHoppers);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(realHoppers), this);
        pm.registerEvents(new EventListener(realHoppers), this);
        pm.registerEvents(GUIBuilder.getListener(), this);
        pm.registerEvents(realHoppers.getGUIManager().getListener(), this);

        realHoppers.getHopperManager().loadHoppers();
        getLogger().info("Loaded " + realHoppers.getHopperManager().getHoppersMap().size() + " hoppers.");

        /* TODO plugin update
        new UpdateChecker(this, 111629).getVersion(version -> {
            if (this.getDescription().getVersion().equalsIgnoreCase(version)) {
                this.getLogger().info("The plugin is updated to the latest version.");
            } else {
                this.newUpdate = true;
                this.getLogger().warning("There is a new update available! Version: " + version + " -> https://www.spigotmc.org/resources/111629/");
            }
        });
         */

        //Lamp owns the command tree: the suggestions, the permissions and the error messages.
        //The RHopperTrait resolver that used to live here is gone - Lamp parses enums
        //case-insensitively and tab-completes their constants without being told about them.
        new RHCommandManager(realHoppers);

        //vault hook
        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) {
                econ = rsp.getProvider();

                if (econ != null) {
                    getLogger().info("Hooked into Vault!");
                }
            }
        }

        this.hopperHighlight = Bukkit.getScheduler().runTaskTimer(this,
                () -> realHoppers.getHopperManager().getHoppers().forEach(RHopper::loopView), 10, 10);

        //hoppers write themselves into the document as they go and this is what puts it on disk.
        //It used to be written in full on every balance change, which for an auto-selling hopper is
        //once per item swallowed.
        final long flushTicks = Math.max(1L, RHConfig.file().getInt("RealHoppers.Save-Interval-Seconds", 60)) * 20L;
        this.hopperFlush = Bukkit.getScheduler().runTaskTimer(this, RHHoppers::saveIfDirty, flushTicks, flushTicks);

        getLogger().info("Finished loading in " + ((System.currentTimeMillis() - start) / 1000F) + " seconds.");
        getLogger().info("<------------------ RealHoppers vPT ------------------>".replace("PT", this.getDescription().getVersion()));

    }

    private void printASCII() {
        logWithColor("&e  ____            _ _   _");
        logWithColor("&e |  _ \\ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___");
        logWithColor("&e | |_) / _ \\/ _` | | |_| |/ _ \\| '_ \\| '_ \\ / _ \\ '__/ __|");
        logWithColor("&e |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \\__ \\");
        logWithColor("&e |_| \\_\\___|\\__,_|_|_| |_|\\___/| .__/| .__/ \\___|_|  |___/");
        logWithColor("&e &8Made by: &9JoseGamer_PT&e         |_|   |_| &8Version: &9" + this.getDescription().getVersion());
        logWithColor("");

    }

    public void logWithColor(String s) {
        getServer().getConsoleSender().sendMessage("[" + this.getDescription().getName() + "] " + Text.color(s));
    }

    @Override
    public void onDisable() {
        if (this.hopperHighlight != null) {
            this.hopperHighlight.cancel();
        }
        if (this.hopperFlush != null) {
            this.hopperFlush.cancel();
        }

        //stopHoppers cancels the trait tasks and flushes the last balances itself
        realHoppers.getHopperManager().stopHoppers();
    }

    public Economy getVault() {
        return econ;
    }
}
