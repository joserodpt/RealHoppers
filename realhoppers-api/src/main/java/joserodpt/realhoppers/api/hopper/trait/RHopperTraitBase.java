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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.function.Consumer;

public abstract class RHopperTraitBase {

    private RHopper main;

    private boolean started;

    /**
     * How far this trait is upgraded. It multiplies whatever the trait's configured value is - the
     * radius suction reaches, the damage a mob killer deals - so tier 1 is the config as written.
     */
    private int tier = 1;

    public RHopperTraitBase(RHopper main) {
        this.main = main;
    }

    /**
     * Starts the trait's repeating task, if it has one. Kept out of the constructor: at load time a
     * hopper's link is resolved after every hopper has been read, so a loop started on construction
     * would spin on a link that is not there yet.
     *
     * <p>Traits that need a link and whose hopper has none are left stopped rather than started
     * against null - {@link RHopperTrait#requiresLink()} decides which those are - and
     * {@link RHopper#setLink(RHopper)} starts them once there is one.</p>
     */
    public void startTask() {
        if (this.started || (this.getTraitType().requiresLink() && !this.isLinked())) {
            return;
        }
        this.started = true;
        this.executeLoop();
    }

    /**
     * Stops the trait and lets it start again later. Unlinking a hopper stops the traits that
     * needed the link; re-linking it has to be able to bring them back.
     */
    public void stop() {
        this.stopTask();
        this.started = false;
    }

    public int getTier() {
        return this.tier;
    }

    /**
     * This trait's own settings as a single string, for a trait that needs to remember more than a
     * tier - a filter's material list, say. Stored beside the tier in the database. Null, the
     * default, means the trait keeps nothing else.
     */
    public String serializeSettings() {
        return null;
    }

    /**
     * Reads back what {@link #serializeSettings()} wrote. The tier is handled by the loader, so the
     * base does nothing; a trait with more to keep overrides this. Handed null for a trait that
     * had nothing stored.
     */
    public void deserializeSettings(final String settings) {
    }

    /**
     * What this trait's configured value should be multiplied by right now: the power of the tier
     * it sits at. Read per cycle, so editing the table and reloading takes effect at once.
     */
    protected double power() {
        return this.getTraitType().getTierPower(this.tier);
    }

    /**
     * Sets the tier, clamped to what the trait allows. A trait that does not scale stays at 1
     * whatever it is handed.
     *
     * @return the tier actually set
     */
    public int setTier(final int tier) {
        this.tier = Math.min(Math.max(1, tier), this.getTraitType().getMaxTier());
        return this.tier;
    }

    public RHopper getHopper() {
        return main;
    }

    /** Whether the hopper this trait is on points at another one. */
    public boolean isLinked() {
        return this.main.hasLink();
    }

    /**
     * The hopper this one points at. The link belongs to the hopper, not to the trait, so every
     * trait that needs one reads the same link - and re-pointing a hopper re-points all of them.
     */
    public RHopper getLinkedHopper() {
        return this.main.getLink();
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

    /**
     * Runs over the blocks around the hopper - the column it stands in, one above and one below -
     * skipping anything whose chunk is not in memory.
     *
     * <p>The cost grows with the square of the radius, and the radius is what a tier raises, so the
     * traits using this run on a slow cycle and their configured radius starts small.</p>
     */
    protected void forEachBlockAround(final int radius, final Consumer<Block> action) {
        final Block origin = this.getHopper().getBlock();
        final World world = origin.getWorld();

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                final int blockX = origin.getX() + x;
                final int blockZ = origin.getZ() + z;

                //asked once per column rather than per block, and before getBlockAt, which would
                //pull the chunk in
                if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                    continue;
                }

                for (int y = -1; y <= 1; y++) {
                    action.accept(world.getBlockAt(blockX, origin.getY() + y, blockZ));
                }
            }
        }
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

}
