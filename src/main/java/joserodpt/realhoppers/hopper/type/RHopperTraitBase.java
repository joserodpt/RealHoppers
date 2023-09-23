package joserodpt.realhoppers.hopper.type;

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.hopper.RHopper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import static joserodpt.realhoppers.utils.LocationUtil.deserializeLocation;

public abstract class RHopperTraitBase {

    private RHopper main;
    private RHopper linked;
    private String linkedLoc;

    public RHopperTraitBase(RHopper main) {
        this.main = main;
        this.executeLoop();
    }

    public RHopper getHopper() {
        return main;
    }

    public boolean isLinked() {
        return linked != null;
    }

    public void setLinked(RHopper linked) {
        this.linked = linked;
    }

    public void setLinkedLoc(String linkedLoc) {
        this.linkedLoc = linkedLoc;
    }

    public RHopper getLinkedHopper() {
        return linked;
    }

    public void loadLink() {
        if (linkedLoc != null && !linkedLoc.isEmpty()) {
            Location l = deserializeLocation(linkedLoc);
            if (l == null) {
                RealHoppers.getPlugin().getLogger().severe("Could not parse location for hopper " + linkedLoc + "! Skipping.");
                return;
            }

            Block b = l.getBlock();
            if (b == null || b.getType() != Material.HOPPER) {
                RealHoppers.getPlugin().getLogger().severe("Block at location " + linkedLoc + " isn't a Hopper! Skipping.");
                return;
            }

            this.setLinked(RealHoppers.getPlugin().getHopperManager().getHopper(b));
        } else {
            RealHoppers.getPlugin().getLogger().severe("Linked Hopper Location of the Trait " + this.getTraitType().name() + " for the Hopper at " + this.getHopper().getSerializedLocation() + "is invalid (" + this.linkedLoc + ")");
        }
    }

    public abstract void executeAction(Player p);

    public abstract void executeLoop();

    public abstract RHopper.Trait getTraitType();

    public abstract void stopTask();

    public abstract String getSerializedSave();
}
