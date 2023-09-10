package joserodpt.realhoppers;

import joserodpt.realhoppers.config.Config;
import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.hopper.HopperManager;
import joserodpt.realhoppers.listener.PlayerListener;
import joserodpt.realhoppers.manager.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class RealHoppers extends JavaPlugin {

    private PlayerManager pm = new PlayerManager();
    private HopperManager hm = new HopperManager();

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

        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);

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

        getLogger().info("Plugin has been loaded.");
        getLogger().info("Author: JoseGamer_PT | " + this.getDescription().getWebsite());
        getLogger().info("<------------------ RealHoppers PT ------------------>".replace("PT", "| " +
                this.getDescription().getVersion()));
    }
    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public PlayerManager getPlayerManager() {
        return this.pm;
    }

    public HopperManager getHopperManager() {
        return this.hm;
    }
}
