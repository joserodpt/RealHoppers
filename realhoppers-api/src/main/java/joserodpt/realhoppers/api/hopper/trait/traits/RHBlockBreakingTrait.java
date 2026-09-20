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
import org.bukkit.block.BlockFace;
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

            Block toBreak = super.getHopper().getBlock().getRelative(BlockFace.UP);
            if (toBreak != null && toBreak.getType().isSolid()) {
                final Material type = toBreak.getType();

                if (super.getHopper().hasHopperSpace(type)) {
                    super.getHopper().addItem(type);
                    toBreak.setType(Material.AIR);
                } else if (super.getHopper().hasTrait(RHopperTrait.AUTO_SELL) && super.getHopper().sell(type)) {
                    toBreak.setType(Material.AIR);
                } else if (RHConfig.file().getBoolean("RealHoppers.Drop-Items-If-Full")) {
                    super.getHopper().getWorld().dropItemNaturally(super.getHopper().getTeleportLocation(), new ItemStack(type));
                    toBreak.setType(Material.AIR);
                }
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

    @Override
    public String getSerializedSave() {
        return getTraitType().name();
    }
}
