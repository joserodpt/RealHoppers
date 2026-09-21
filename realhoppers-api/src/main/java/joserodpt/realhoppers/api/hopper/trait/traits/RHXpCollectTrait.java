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
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;

/**
 * Soaks up experience orbs near the hopper and keeps them until somebody comes for them.
 *
 * <p>Stored on the hopper the way a balance is, and taken out of the hopper's panel - but kept
 * apart from the balance, because one is money paid through Vault and this is levels handed
 * straight back. Made to sit under a KILL_MOB grinder.</p>
 */
public class RHXpCollectTrait extends RHopperTraitBase {

    public RHXpCollectTrait(RHopper main) {
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

            final double radius = RHopperTrait.XP_COLLECT.configValue("Radius", 3) * super.power();

            int gathered = 0;
            for (Entity nearby : super.getHopper().getWorld()
                    .getNearbyEntities(super.getHopper().getLocation(), radius, radius, radius)) {
                if (nearby.getType() != EntityType.EXPERIENCE_ORB) {
                    continue;
                }
                gathered += ((ExperienceOrb) nearby).getExperience();
                nearby.remove();
            }

            //one write for the sweep rather than one per orb, since each fires an event and queues
            //a save
            if (gathered > 0) {
                super.getHopper().addXp(gathered);
            }
        }, 20, 20);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.XP_COLLECT;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
