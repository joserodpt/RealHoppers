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
 * @author José Rodrigues © 2023-2026
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

    private boolean started;

    public RHopperTraitBase(RHopper main) {
        this.main = main;
    }

    /**
     * Starts the trait's repeating task, if it has one. Kept out of the constructor: a trait is
     * constructed before {@link #setLinkedLoc(String)} or {@link #setLinked(RHopper)} has run, so a
     * loop started there would spin on a link that is not resolved yet.
     *
     * <p>Traits that need a link and have not got one are left stopped rather than started against
     * null - {@link RHopperTrait#requiresLink()} decides which those are.</p>
     */
    public void startTask() {
        if (this.started || (this.getTraitType().requiresLink() && !this.isLinked())) {
            return;
        }
        this.started = true;
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

    /**
     * Whether this trait's loop should do anything this tick.
     *
     * <p>False while the chunk is out, so a timer never drags a chunk back in. False too once the
     * block has stopped being a hopper, and in that case the hopper retires itself: the plugin
     * cannot hear every way a block can go away, so rather than throw out of the loop forever it
     * notices on the next tick and unregisters.</p>
     */
    protected boolean hopperReady() {
        if (!this.getHopper().isChunkLoaded()) {
            return false;
        }
        if (!this.getHopper().isValid()) {
            RealHoppersAPI.getInstance().getHopperManager().delete(this.getHopper());
            return false;
        }
        return true;
    }

    /**
     * The same question for a trait that points at a second hopper, which has to be there too.
     * A link whose hopper has gone takes this trait off with it.
     */
    protected boolean linkReady() {
        if (!this.hopperReady()) {
            return false;
        }
        final RHopper linked = this.getLinkedHopper();
        if (linked == null) {
            return false;
        }
        if (!linked.isChunkLoaded()) {
            return false;
        }
        if (!linked.isValid()) {
            RealHoppersAPI.getInstance().getHopperManager().delete(linked);
            return false;
        }
        return true;
    }

    public abstract void executeAction(Player p);

    /**
     * Schedules whatever the trait does on a timer. Call {@link #startTask()} instead - it is what
     * decides whether the trait is ready to run.
     */
    protected abstract void executeLoop();

    public abstract RHopperTrait getTraitType();

    public abstract void stopTask();

    /**
     * Whether {@link #startTask()} has already run. Nothing schedules a trait twice.
     */
    protected boolean isStarted() {
        return this.started;
    }

    public abstract String getSerializedSave();
}
