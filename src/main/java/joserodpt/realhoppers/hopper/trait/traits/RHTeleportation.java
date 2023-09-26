package joserodpt.realhoppers.hopper.trait.traits;

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

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.config.Config;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.utils.LocationUtil;
import joserodpt.realhoppers.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class RHTeleportation extends RHopperTraitBase {

    public RHTeleportation(RHopper main, String linkLocation) {
        super(main);
        super.setLinkedLoc(linkLocation);
    }

    public RHTeleportation(RHopper main, RHopper link) {
        super(main);
        super.setLinked(link);
    }

    @Override
    public void executeAction(Player p) {
        if (!RealHoppers.getPlugin().getPlayerManager().getTpFreeze().contains(p)) {
            RealHoppers.getPlugin().getPlayerManager().getTpFreeze().add(p);

            LocationUtil.teleportConservingPitchYaw(p, super.getLinkedHopper().getTeleportLocation());
            if (Config.file().getBoolean("RealHoppers.Effects.Particles"))
                super.getLinkedHopper().getTeleportLocation().getWorld().spawnParticle(Particle.PORTAL, super.getLinkedHopper().getTeleportLocation(), 20);

            Text.send(p, "You have been teleported!");
            Bukkit.getScheduler().scheduleSyncDelayedTask(RealHoppers.getPlugin(), () -> RealHoppers.getPlugin().getPlayerManager().getTpFreeze().remove(p), Config.file().getInt("RealHoppers.Teleportation-Cooldown"));
        }
    }

    @Override
    public void executeLoop() { }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.TELEPORT;
    }

    @Override
    public void stopTask() {}

    @Override
    public String getSerializedSave() {
        return getTraitType() + "|" + getLinkedHopper().getSerializedLocation();
    }
}
