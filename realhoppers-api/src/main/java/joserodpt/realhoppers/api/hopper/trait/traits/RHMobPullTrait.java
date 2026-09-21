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
 * @author José Rodrigues © 2023-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Drags living things nearby onto the hopper itself.
 *
 * <p>Built to pair with KILL_MOB on the same hopper, which is the whole grinder: one trait gathers
 * what wanders past, the other kills it and takes the drops. Players are left alone - that is what
 * TELEPORT is for.</p>
 *
 * <p>Onto the top of the hopper normally. With something built over it there is nowhere up there to
 * stand, so they go beside it instead, and if there is no room there either they are left where
 * they are rather than shoved inside a wall.</p>
 */
public class RHMobPullTrait extends RHopperTraitBase {

    public RHMobPullTrait(RHopper main) {
        super(main);
    }

    @Override
    public void executeAction(Player p) { }

    private int taskID = -1;

    @Override
    protected void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppersAPI.getInstance().getPlugin(), () -> {
            if (!super.hopperReady()) {
                return;
            }

            final Location destination = this.destination();
            if (destination == null) {
                return;
            }

            final double radius = RHopperTrait.MOB_PULL.configValue("Radius", 3) * super.power();

            for (Entity nearby : super.getHopper().getWorld()
                    .getNearbyEntities(super.getHopper().getLocation(), radius, radius, radius)) {
                if (nearby.getType() == EntityType.PLAYER || !(nearby instanceof LivingEntity)) {
                    continue;
                }
                nearby.teleport(destination);
            }
        }, 20, 20);
    }

    /** The four ways round the hopper, tried in turn when the top of it is covered. */
    private static final BlockFace[] SIDES = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};

    /**
     * Where the mobs are put: the top of the hopper, or beside it when something is built over it.
     *
     * @return the spot, or null when the hopper is walled in and there is nowhere to stand
     */
    private Location destination() {
        final Block above = super.getHopper().getBlock().getRelative(BlockFace.UP);
        if (roomToStand(above)) {
            return super.getHopper().getTeleportLocation();
        }

        for (final BlockFace side : SIDES) {
            final Block beside = above.getRelative(side);
            if (roomToStand(beside)) {
                return beside.getLocation().add(0.5, 0, 0.5);
            }
        }

        return null;
    }

    /** Whether something could stand here: this block clear, and head room above it. */
    private static boolean roomToStand(final Block block) {
        return !block.getType().isSolid() && !block.getRelative(BlockFace.UP).getType().isSolid();
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.MOB_PULL;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
