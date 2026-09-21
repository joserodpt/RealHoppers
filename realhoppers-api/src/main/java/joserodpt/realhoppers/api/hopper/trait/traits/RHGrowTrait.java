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
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Nudges crops around the hopper along, the way bonemeal would.
 *
 * <p>Rolls per block rather than growing everything at once, so a field ripens unevenly instead of
 * all in the same tick. Pairs with HARVEST on the same hopper: one grows the field, the other cuts
 * it.</p>
 */
public class RHGrowTrait extends RHopperTraitBase {

    public RHGrowTrait(RHopper main) {
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

            final int radius = (int) Math.max(1, RHopperTrait.GROW.configValue("Radius", 2) * super.power());
            final double chance = RHopperTrait.GROW.configValue("Chance", 10);

            super.forEachBlockAround(radius, block -> {
                final BlockData data = block.getBlockData();
                if (!(data instanceof Ageable)) {
                    return;
                }

                final Ageable crop = (Ageable) data;
                if (crop.getAge() >= crop.getMaximumAge()) {
                    return;
                }

                if (ThreadLocalRandom.current().nextDouble() * 100D > chance) {
                    return;
                }

                crop.setAge(crop.getAge() + 1);
                block.setBlockData(crop);
            });
        }, 40, 40);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.GROW;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
