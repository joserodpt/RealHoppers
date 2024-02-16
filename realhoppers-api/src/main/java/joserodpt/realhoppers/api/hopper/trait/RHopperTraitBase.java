package joserodpt.realhoppers.api.hopper.trait;

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
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

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
            Location l = LocationUtil.deserializeLocation(linkedLoc);
            if (l == null) {
                RealHoppersAPI.getInstance().getLogger().severe("Could not parse location for hopper " + linkedLoc + "! Skipping.");
                return;
            }

            Block b = l.getBlock();
            if (b == null || b.getType() != Material.HOPPER) {
                RealHoppersAPI.getInstance().getLogger().severe("Block at location " + linkedLoc + " isn't a Hopper! Skipping.");
                return;
            }

            this.setLinked(RealHoppersAPI.getInstance().getHopperManager().getHopper(b));
        } else {
            RealHoppersAPI.getInstance().getLogger().severe("Linked Hopper Location of the Trait " + this.getTraitType().name() + " for the Hopper at " + this.getHopper().getSerializedLocation() + "is invalid (" + this.linkedLoc + ")");
        }
    }

    public abstract void executeAction(Player p);

    public abstract void executeLoop();

    public abstract RHopperTrait getTraitType();

    public abstract void stopTask();

    public abstract String getSerializedSave();
}
