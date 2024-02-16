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

    private int taskID;

    @Override
    public void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppersAPI.getInstance().getPlugin(), () -> {
            Block toBreak = super.getHopper().getBlock().getRelative(BlockFace.UP);
            if (toBreak != null && toBreak.getType().isSolid()) {
                if (super.getHopper().hasHopperSpace(toBreak.getType()) ||
                        (super.getHopper().hasTrait(RHopperTrait.AUTO_SELL) &&
                                RealHoppersAPI.getInstance().getHopperManager().getMaterialCost().containsKey(toBreak.getType()))) {

                    if (super.getHopper().hasHopperSpace(toBreak.getType())) {
                        super.getHopper().addItem(toBreak.getType());
                    } else {
                        super.getHopper().sell(toBreak.getType(), true);
                    }

                    toBreak.setType(Material.AIR);
                } else {
                    if (RHConfig.file().getBoolean("RealHoppers.Drop-Items-If-Full")) {
                        super.getHopper().getWorld().dropItemNaturally(super.getHopper().getTeleportLocation(), new ItemStack(toBreak.getType()));
                        toBreak.setType(Material.AIR);
                    }
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
        Bukkit.getScheduler().cancelTask(taskID);
    }

    @Override
    public String getSerializedSave() {
        return getTraitType().name();
    }
}
