package joserodpt.realhoppers.utils;

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.config.Config;
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

        if (Config.file().getBoolean("RealHoppers.Effects.Sounds"))
            p.playSound(l, Sound.ENTITY_ENDERMAN_TELEPORT, 10, 10);
    }

    public static Location deserializeLocation(String serializedLocation) {
        String[] parts = serializedLocation.split(":");

        if (parts.length != 4) {
            RealHoppers.getPlugin().getLogger().severe("Invalid serialized location format");
            return null;
        }

        double x = Double.parseDouble(parts[0]);
        double y = Double.parseDouble(parts[1]);
        double z = Double.parseDouble(parts[2]);
        String worldName = parts[3];

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            RealHoppers.getPlugin().getLogger().severe("World '" + worldName + "' not found");
            return null;
        }

        return new Location(world, x, y, z);
    }
}
