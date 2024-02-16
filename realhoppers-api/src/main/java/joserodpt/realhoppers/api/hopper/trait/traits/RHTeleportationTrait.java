package joserodpt.realhoppers.api.hopper.trait.traits;

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
import joserodpt.realhoppers.api.utils.LocationUtil;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class RHTeleportationTrait extends RHopperTraitBase {

    public RHTeleportationTrait(RHopper main, String linkLocation) {
        super(main);
        super.setLinkedLoc(linkLocation);
    }

    public RHTeleportationTrait(RHopper main, RHopper link) {
        super(main);
        super.setLinked(link);
    }

    @Override
    public void executeAction(Player p) {
        if (!RealHoppersAPI.getInstance().getPlayerManager().getTpFreeze().contains(p)) {
            RealHoppersAPI.getInstance().getPlayerManager().getTpFreeze().add(p);

            LocationUtil.teleportConservingPitchYaw(p, super.getLinkedHopper().getTeleportLocation());
            if (RHConfig.file().getBoolean("RealHoppers.Effects.Particles"))
                super.getLinkedHopper().getTeleportLocation().getWorld().spawnParticle(Particle.PORTAL, super.getLinkedHopper().getTeleportLocation(), 20);

            Text.send(p, "You have been teleported!");
            Bukkit.getScheduler().scheduleSyncDelayedTask(RealHoppersAPI.getInstance().getPlugin(), () -> RealHoppersAPI.getInstance().getPlayerManager().getTpFreeze().remove(p), RHConfig.file().getInt("RealHoppers.Teleportation-Cooldown"));
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
