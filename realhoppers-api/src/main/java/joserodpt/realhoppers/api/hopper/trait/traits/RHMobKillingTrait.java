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
import org.bukkit.metadata.FixedMetadataValue;

public class RHMobKillingTrait extends RHopperTraitBase {

    public RHMobKillingTrait(RHopper main) {
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

            final double radius = RHopperTrait.KILL_MOB.configValue("Radius", 1.5);
            final double damage = RHopperTrait.KILL_MOB.configValue("Damage", 2) * super.power();

            for (Entity nearbyEntity : super.getHopper().getWorld().getNearbyEntities(super.getHopper().getLocation(), radius, radius, radius)) {
                if (nearbyEntity.getType() != EntityType.PLAYER && nearbyEntity instanceof LivingEntity) {
                    //set on every hit, before it, so the kill is credited to the hopper that dealt it
                    //rather than whichever one first touched the mob
                    nearbyEntity.setMetadata("rh", new FixedMetadataValue(RealHoppersAPI.getInstance().getPlugin(), super.getHopper()));
                    ((LivingEntity) nearbyEntity).damage(damage);
                }
            }
        }, 20, 20);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.KILL_MOB;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
