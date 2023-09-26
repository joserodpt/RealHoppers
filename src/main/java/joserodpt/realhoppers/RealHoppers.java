package joserodpt.realhoppers;

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

import joserodpt.realhoppers.config.Config;
import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.config.Language;
import joserodpt.realhoppers.hopper.HopperGUI;
import joserodpt.realhoppers.manager.HopperManager;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.listener.PlayerListener;
import joserodpt.realhoppers.listener.EventListener;
import joserodpt.realhoppers.manager.PlayerManager;
import joserodpt.realhoppers.utils.Text;
import me.mattstudios.mf.base.CommandManager;
import me.mattstudios.mf.base.components.TypeResult;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealHoppers extends JavaPlugin {

    private PlayerManager pm = new PlayerManager();
    private HopperManager hm = new HopperManager(this);

    private static RealHoppers pl;
    public static RealHoppers getPlugin() {
        return pl;
    }

    @Override
    public void onEnable() {
        pl = this;
        //new Metrics(this, 19311);

        getLogger().info("<------------------ RealHoppers PT ------------------>".replace("PT", "| " +
                this.getDescription().getVersion()));

        Config.setup(this);
        Hoppers.setup(this);
        Language.setup(this);

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new EventListener(this), this);
        pm.registerEvents(HopperGUI.getListener(), this);

        hm.loadHoppers();

        getLogger().info("Loaded " + hm.getHoppersMap().size() + " hoppers.");

        /*
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

        cm.register(new RealHoppersCMD(this));

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> hm.getHoppers().forEach(RHopper::loopView), 10, 10);

        getLogger().info("Plugin has been loaded.");
        getLogger().info("Author: JoseGamer_PT | " + this.getDescription().getWebsite());
        getLogger().info("<------------------ RealHoppers PT ------------------>".replace("PT", "| " +
                this.getDescription().getVersion()));
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        hm.stopHoppers();
    }

    public PlayerManager getPlayerManager() {
        return this.pm;
    }

    public HopperManager getHopperManager() {
        return this.hm;
    }
}
