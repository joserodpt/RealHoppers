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
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Drags living things nearby onto the hopper this one is linked to.
 *
 * <p>Built to pair with KILL_MOB: point a ring of pullers at one killer and they gather what
 * wanders past. Players are left alone - that is what TELEPORT is for.</p>
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
            if (!super.linkReady()) {
                return;
            }

            final double radius = RHopperTrait.MOB_PULL.configValue("Radius", 3) * super.power();

            for (Entity nearby : super.getHopper().getWorld()
                    .getNearbyEntities(super.getHopper().getLocation(), radius, radius, radius)) {
                if (nearby.getType() == EntityType.PLAYER || !(nearby instanceof LivingEntity)) {
                    continue;
                }
                nearby.teleport(super.getLinkedHopper().getTeleportLocation());
            }
        }, 20, 20);
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
