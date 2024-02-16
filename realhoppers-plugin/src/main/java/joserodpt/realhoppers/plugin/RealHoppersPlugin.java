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
import joserodpt.realhoppers.plugin.gui.HopperGUI;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.plugin.listener.EventListener;
import joserodpt.realhoppers.plugin.listener.PlayerListener;
import me.mattstudios.mf.base.CommandManager;
import me.mattstudios.mf.base.components.TypeResult;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealHoppersPlugin extends JavaPlugin {

    private static Economy econ = null;

    private static RealHoppersPlugin instance;

    private RealHoppers realHoppers;

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
        pm.registerEvents(HopperGUI.getListener(), this);

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

        CommandManager cm = new CommandManager(this);

        cm.getMessageHandler().register("cmd.no.permission", (sender) -> Text.send(sender, "&cYou don't have permission to execute this command!"));
        cm.getMessageHandler().register("cmd.no.exists", (sender) -> Text.send(sender, "&cThe command you're trying to use doesn't exist"));
        cm.getMessageHandler().register("cmd.wrong.usage", (sender) -> Text.send(sender, "&cWrong usage for the command!"));
        cm.getMessageHandler().register("cmd.no.console", sender -> Text.send(sender,  "&cCommand can't be used in the console!"));

        cm.hideTabComplete(true);

        cm.getParameterHandler().register(RHopperTrait.class, argument -> {
            try {
                RHopperTrait tt = RHopperTrait.valueOf(argument.toString().toUpperCase());
                return new TypeResult(tt, argument);
            } catch (Exception e) {
                return new TypeResult(null, argument);
            }
        });

        cm.register(new RealHoppersCMD(realHoppers));

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

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> realHoppers.getHopperManager().getHoppers().forEach(RHopper::loopView), 10, 10);

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
        // Plugin shutdown logic
        realHoppers.getHopperManager().stopHoppers();
    }

    public Economy getVault() {
        return econ;
    }
}
