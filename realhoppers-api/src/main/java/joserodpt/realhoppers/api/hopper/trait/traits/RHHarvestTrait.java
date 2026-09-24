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
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cuts fully grown crops around the hopper and replants them.
 *
 * <p>The harvest goes through {@link RHopper#offer}, so it is smelted, sold or voided by whatever
 * else is on the hopper, exactly as suction and block breaking are.</p>
 */
public class RHHarvestTrait extends RHopperTraitBase {

    /**
     * What counts as a crop. Anything Ageable used to: sugar cane, cactus, kelp and fire are too,
     * and setting one of those back to age 0 while keeping its drops made them endless.
     */
    private static final Set<Material> CROPS = EnumSet.of(
            Material.WHEAT, Material.CARROTS, Material.POTATOES, Material.BEETROOTS,
            Material.NETHER_WART, Material.COCOA, Material.SWEET_BERRY_BUSH);

    public RHHarvestTrait(RHopper main) {
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

            final int radius = (int) Math.max(1, RHopperTrait.HARVEST.configValue("Radius", 2) * super.power());

            super.forEachBlockAround(radius, block -> {
                if (!CROPS.contains(block.getType())) {
                    return;
                }
                final BlockData data = block.getBlockData();
                if (!(data instanceof Ageable)) {
                    return;
                }

                final Ageable crop = (Ageable) data;
                if (crop.getAge() < crop.getMaximumAge()) {
                    return;
                }

                for (final ItemStack drop : block.getDrops()) {
                    final ItemStack left = super.getHopper().offer(drop);
                    if (left != null && RHConfig.file().getBoolean("RealHoppers.Drop-Items-If-Full")) {
                        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), left);
                    }
                }

                //back to nothing rather than to air: the crop is replanted, not dug up
                crop.setAge(0);
                block.setBlockData(crop);
            });
        }, 40, 40);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.HARVEST;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
