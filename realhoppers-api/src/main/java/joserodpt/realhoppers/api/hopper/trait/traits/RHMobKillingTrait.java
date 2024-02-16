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

    private int taskID;

    @Override
    public void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppersAPI.getInstance().getPlugin(), () -> {
            for (Entity nearbyEntity : super.getHopper().getWorld().getNearbyEntities(super.getHopper().getLocation(), 1.5, 1.5, 1.5)) {
                if (nearbyEntity.getType() != EntityType.PLAYER && nearbyEntity instanceof LivingEntity) {
                    ((LivingEntity) nearbyEntity).damage(2);
                    if (!nearbyEntity.hasMetadata("rh"))
                        nearbyEntity.setMetadata("rh", new FixedMetadataValue(RealHoppersAPI.getInstance().getPlugin(), super.getHopper()));
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
        Bukkit.getScheduler().cancelTask(taskID);
    }

    @Override
    public String getSerializedSave() {
        return getTraitType().name();
    }
}
