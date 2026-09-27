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
import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.event.RealHoppersPluginLoadedEvent;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.utils.Compacting;
import joserodpt.realhoppers.api.utils.Smelting;
import joserodpt.realhoppers.plugin.command.RHCommandManager;
import joserodpt.realhoppers.plugin.listener.EventListener;
import joserodpt.realhoppers.plugin.listener.PlayerListener;
import joserodpt.realutils.RealUtils;
import joserodpt.realutils.dialog.Dialogs;
import joserodpt.realutils.gui.MaterialPickerGUI;
import joserodpt.realutils.input.PlayerInput;
import joserodpt.realutils.text.Text;
import joserodpt.realutils.update.UpdateChecker;
import joserodpt.realpermissions.api.RealPermissionsAPI;
import joserodpt.realpermissions.api.pluginhook.ExternalPlugin;
import joserodpt.realpermissions.api.pluginhook.ExternalPluginPermission;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.MATERIAL;

public final class RealHoppersPlugin extends JavaPlugin {

    public static final int SPIGOT_RESOURCE_ID = 139161;
    public static final String SPIGOT_URL = "https://www.spigotmc.org/resources/" + SPIGOT_RESOURCE_ID + "/";

    private static Economy econ = null;

    private static RealHoppersPlugin instance;

    private RealHoppers realHoppers;
    private BukkitTask hopperSweep;
    private BukkitTask hopperFlush;
    private BukkitTask screenSync;
    /** The newer version SpigotMC has, or null while this one is the latest (or it couldn't be asked). */
    private volatile String newVersion;

    public static RealHoppersPlugin getPlugin() {
        return instance;
    }

    @Override
    public void onEnable() {
        printASCII();
        final long start = System.currentTimeMillis();

        instance = this;
        //first: the GUIs' listeners, and the plugin RealUtils schedules and logs through
        RealUtils.setup(this);
        //read on every message, so a reloaded prefix applies
        Text.prefix(() -> RHConfig.file().getString("RealHoppers.Prefix") + " &r");
        realHoppers = new RealHoppers(this);
        RealHoppersAPI.setInstance(realHoppers);

        if (realHoppers.getDatabaseManager() == null) {
            getLogger().severe("RealHoppers cannot run without its database. Check sql.yml. Disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(realHoppers), this);
        pm.registerEvents(new EventListener(realHoppers), this);
        pm.registerEvents(PlayerInput.getListener(), this);
        //typed input is asked for in a dialog on servers that have them, in chat everywhere else
        Dialogs.setup(this, () -> RHConfig.file().getBoolean("RealHoppers.useDialogs", true));
        Dialogs.labels(TranslatableLine.SYSTEM_DIALOG_CONFIRM.get(), TranslatableLine.SYSTEM_DIALOG_CANCEL.get(),
                TranslatableLine.SYSTEM_DIALOG_CLOSE.get(), TranslatableLine.SYSTEM_DIALOG_BACK.get(), TranslatableLine.SYSTEM_DIALOG_SAVE.get());
        PlayerInput.setup(this,
                p -> RHLanguage.file().getStringList("System.Type-Input"),
                p -> RHLanguage.file().getStringList("System.Type-Input-Dialog"),
                TranslatableLine.SYSTEM_INPUT_CANCELLED::send,
                TranslatableLine.SYSTEM_ERROR_OCCURRED::send);
        //the picker's words, read for every picker opened so a reloaded language applies
        MaterialPickerGUI.labels(() -> {
            final MaterialPickerGUI.Labels labels = new MaterialPickerGUI.Labels();
            labels.nextName = TranslatableLine.GUI_NEXT_PAGE_NAME.get();
            labels.nextLore = RHLanguage.file().getStringList("GUI.Items.Picker.Next-Description");
            labels.previousName = TranslatableLine.GUI_PREVIOUS_PAGE_NAME.get();
            labels.previousLore = RHLanguage.file().getStringList("GUI.Items.Picker.Back-Description");
            labels.closeName = TranslatableLine.GUI_CLOSE_NAME.get();
            labels.closeLore = RHLanguage.file().getStringList("GUI.Items.Close.Description");
            labels.searchName = TranslatableLine.GUI_SEARCH_ITEM_NAME.get();
            labels.pickName = m -> TranslatableLine.GUI_PICK_NAME.with(MATERIAL, Text.beautifyMaterialName(m)).get();
            labels.pickLore = RHLanguage.file().getStringList("GUI.Items.Picker.Pick-Description");
            return labels;
        });
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

        this.hopperSweep = Bukkit.getScheduler().runTaskTimer(this,
                () -> realHoppers.getHopperManager().tick(), 10, 10);

        //every tick, so an open hopper screen shows what vanilla hoppers are moving in and out of it
        this.screenSync = Bukkit.getScheduler().runTaskTimer(this,
                () -> realHoppers.getGUIManager().syncOpenScreens(), 1, 1);

        //hoppers mark themselves as changed and this is what writes them to the database. A write
        //per change would be one per item for an auto-selling hopper.
        final long flushTicks = Math.max(1L, RHConfig.file().getInt("RealHoppers.Save-Interval-Seconds", 60)) * 20L;
        this.hopperFlush = Bukkit.getScheduler().runTaskTimer(this,
                () -> realHoppers.getDatabaseManager().flush(true), flushTicks, flushTicks);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RealHoppersPlaceholderAPI(realHoppers).register();
            getLogger().info("Hooked onto PlaceholderAPI!");
        }

        if (getServer().getPluginManager().getPlugin("RealPermissions") != null) {
            registerRealPermissions();
        }

        new Metrics(this, 34359);

        new UpdateChecker(this, SPIGOT_RESOURCE_ID).getVersion(version -> {
            if (version != null && UpdateChecker.isNewer(version, this.getDescription().getVersion())) {
                this.newVersion = version;
                this.getLogger().warning("There is a new update available! Version: " + version + " -> " + SPIGOT_URL);
            } else {
                this.getLogger().info("The plugin is updated to the latest version.");
            }
        });

        Bukkit.getPluginManager().callEvent(new RealHoppersPluginLoadedEvent());

        getLogger().info("Finished loading in " + ((System.currentTimeMillis() - start) / 1000F) + " seconds.");
        getLogger().info("<------------------ RealHoppers vPT ------------------>".replace("PT", this.getDescription().getVersion()));

    }

    /**
     * Publishes the plugin's permissions to RealPermissions, so they can be handed out from its GUI
     * instead of being typed from the wiki.
     */
    /** The newer version on SpigotMC, or null when there is none. */
    public String getNewVersion() {
        return this.newVersion;
    }

    private void registerRealPermissions() {
        try {
            final List<ExternalPluginPermission> permissions = new ArrayList<>();
            permissions.add(new ExternalPluginPermission("realhoppers.admin",
                    "Allow access to the main operator commands of RealHoppers, and to open, break, link and manage any hopper, private or not.",
                    Arrays.asList("rh reload", "rh settrait <trait>", "rh list <player>")));
            permissions.add(new ExternalPluginPermission(EventListener.KEEP_CONTENTS_PERMISSION,
                    "Breaking a private hopper drops it with its owner and contents kept, so it can be placed back as it was.",
                    Collections.emptyList()));
            permissions.add(new ExternalPluginPermission(RHopperTrait.WILDCARD_PERMISSION,
                    "Allow adding every trait to a hopper.", Collections.emptyList()));
            for (final RHopperTrait trait : RHopperTrait.values()) {
                permissions.add(new ExternalPluginPermission(trait.getPermission(),
                        "Allow adding the " + trait.name() + " trait to a hopper.", Collections.emptyList()));
            }

            RealPermissionsAPI.getInstance().getHooksAPI().addHook(new ExternalPlugin(
                    this.getDescription().getName(), "&fReal&6Hoppers", this.getDescription().getDescription(),
                    Material.HOPPER, permissions, this.getDescription().getVersion()));
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
        Dialogs.shutdown();
        if (this.hopperSweep != null) {
            this.hopperSweep.cancel();
        }
        if (this.hopperFlush != null) {
            this.hopperFlush.cancel();
        }
        if (this.screenSync != null) {
            this.screenSync.cancel();
        }

        //disabled before it got going, for want of a database
        if (realHoppers == null || realHoppers.getDatabaseManager() == null) {
            return;
        }

        //once disabled nothing cancels clicks on these screens, and the hopper slots on them are
        //only pictures that could be taken out as real items
        realHoppers.getGUIManager().closeAll();

        //stopHoppers cancels the trait tasks and marks the last balances; close writes them out
        realHoppers.getHopperManager().stopHoppers();
        realHoppers.getDatabaseManager().close();
    }

    public Economy getEconomy() {
        return econ;
    }
}
