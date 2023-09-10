package joserodpt.realhoppers.hopper.type;

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.utils.LocationUtil;
import joserodpt.realhoppers.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class RHTeleportation extends RHopperTrait {

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

            Text.send(p, "Ooppp");
            Bukkit.getScheduler().scheduleSyncDelayedTask(RealHoppers.getPlugin(), () -> RealHoppers.getPlugin().getPlayerManager().getTpFreeze().remove(p), 20);
        }
    }

    @Override
    public RHopper.Trait getTraitType() {
        return RHopper.Trait.TELEPORT;
    }
}
