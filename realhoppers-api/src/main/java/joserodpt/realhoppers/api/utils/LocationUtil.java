package joserodpt.realhoppers.api.utils;

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
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LocationUtil {

    public static void teleportConservingPitchYaw(Player p, Location l) {
        l.setPitch(p.getLocation().getPitch());
        l.setYaw(p.getLocation().getYaw());
        p.teleport(l);

        if (RHConfig.file().getBoolean("RealHoppers.Effects.Sounds"))
            p.playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 10, 10);
    }

    public static Location deserializeLocation(String serializedLocation) {
        String[] parts = serializedLocation.split(":");

        if (parts.length != 4) {
            RealHoppersAPI.getInstance().getLogger().severe("Invalid serialized location format");
            return null;
        }

        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        String worldName = parts[3];

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            RealHoppersAPI.getInstance().getLogger().severe("World '" + worldName + "' not found");
            return null;
        }

        return new Location(world, x, y, z);
    }
}
