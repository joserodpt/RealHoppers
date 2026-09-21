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
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RHBlockBreakingTrait extends RHopperTraitBase {

    public RHBlockBreakingTrait(RHopper main) {
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

            final int blocks = (int) Math.max(1,
                    RHopperTrait.BLOCK_BREAKING.configValue("Blocks", 1) * super.power());

            for (int height = 1; height <= blocks; height++) {
                final Block toBreak = super.getHopper().getBlock().getRelative(0, height, 0);
                //a gap ends the run: the tier reaches further up a column, it does not mine
                //through air to whatever happens to be above it
                if (toBreak == null || !toBreak.getType().isSolid()) {
                    break;
                }

                final Material type = toBreak.getType();

                if (super.getHopper().hasHopperSpace(type)) {
                    super.getHopper().addItem(type);
                } else if (!super.getHopper().sell(type)) {
                    if (!RHConfig.file().getBoolean("RealHoppers.Drop-Items-If-Full")) {
                        //nowhere for it to go, so it stays in the ground
                        break;
                    }
                    super.getHopper().getWorld().dropItemNaturally(super.getHopper().getTeleportLocation(), new ItemStack(type));
                }

                toBreak.setType(Material.AIR);
            }
        }, 10, 10);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.BLOCK_BREAKING;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
