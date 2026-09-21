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
import joserodpt.realhoppers.api.event.RealHoppersPluginLoadedEvent;
import joserodpt.realhoppers.api.config.RHHoppers;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.utils.Compacting;
import joserodpt.realhoppers.api.utils.GUIBuilder;
import joserodpt.realhoppers.api.utils.PlayerInput;
import joserodpt.realhoppers.plugin.gui.MaterialPickerGUI;
import joserodpt.realhoppers.api.utils.Smelting;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.plugin.command.RHCommandManager;
import joserodpt.realhoppers.plugin.listener.EventListener;
import joserodpt.realhoppers.plugin.listener.PlayerListener;
import joserodpt.realpermissions.api.RealPermissionsAPI;
import joserodpt.realpermissions.api.pluginhook.ExternalPlugin;
import joserodpt.realpermissions.api.pluginhook.ExternalPluginPermission;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Arrays;
import java.util.Collections;

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

        instance = this;
        realHoppers = new RealHoppers(this);
        RealHoppersAPI.setInstance(realHoppers);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(realHoppers), this);
        pm.registerEvents(new EventListener(realHoppers), this);
        pm.registerEvents(GUIBuilder.getListener(), this);
        pm.registerEvents(MaterialPickerGUI.getListener(), this);
        pm.registerEvents(PlayerInput.getListener(), this);
        pm.registerEvents(realHoppers.getGUIManager().getListener(), this);

        //the server's furnace recipes, which is what AUTO_SMELT smelts by. Read here rather than
        //per item: the recipe list does not change while the server is up.
        Smelting.load();
        Compacting.load();
        getLogger().info("Loaded " + Smelting.size() + " smelting and " + Compacting.size() + " compacting recipes.");

        //the tier tables, before any hopper is read: loading a trait clamps its tier to the table
        RHopperTrait.loadTiers();

        realHoppers.getHopperManager().loadHoppers();
        getLogger().info("Loaded " + realHoppers.getHopperManager().getHoppersMap().size() + " hoppers.");

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
                () -> realHoppers.getHopperManager().tick(), 10, 10);

        //hoppers write themselves into the document as they go and this is what puts it on disk.
        //It used to be written in full on every balance change, which for an auto-selling hopper is
        //once per item swallowed.
        final long flushTicks = Math.max(1L, RHConfig.file().getInt("RealHoppers.Save-Interval-Seconds", 60)) * 20L;
        this.hopperFlush = Bukkit.getScheduler().runTaskTimer(this, RHHoppers::saveIfDirty, flushTicks, flushTicks);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RealHoppersPlaceholderAPI(realHoppers).register();
            getLogger().info("Hooked onto PlaceholderAPI!");
        }

        if (getServer().getPluginManager().getPlugin("RealPermissions") != null) {
            registerRealPermissions();
        }

        Bukkit.getPluginManager().callEvent(new RealHoppersPluginLoadedEvent());

        getLogger().info("Finished loading in " + ((System.currentTimeMillis() - start) / 1000F) + " seconds.");
        getLogger().info("<------------------ RealHoppers vPT ------------------>".replace("PT", this.getDescription().getVersion()));

    }

    /**
     * Publishes the plugin's permissions to RealPermissions, so they can be handed out from its GUI
     * instead of being typed from the wiki.
     */
    private void registerRealPermissions() {
        try {
            RealPermissionsAPI.getInstance().getHooksAPI().addHook(new ExternalPlugin(
                    this.getDescription().getName(), "&fReal&6Hoppers", this.getDescription().getDescription(),
                    Material.HOPPER,
                    Collections.singletonList(new ExternalPluginPermission("realhoppers.admin",
                            "Allow access to the main operator commands of RealHoppers.",
                            Arrays.asList("rh reload", "rh settrait <trait>"))),
                    this.getDescription().getVersion()));
        } catch (final Exception e) {
            getLogger().warning("Error while trying to register RealHoppers permissions onto RealPermissions.");
            e.printStackTrace();
        }
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

    public Economy getEconomy() {
        return econ;
    }
}
