package joserodpt.realhoppers.hopper.type;

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.hopper.RHopper;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

public class RHBlockBreaking extends RHopperTraitBase {

    public RHBlockBreaking(RHopper main) {
        super(main);
    }

    @Override
    public void executeAction(Player p) { }

    private int taskID;

    @Override
    public void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppers.getPlugin(), () -> {
            Block toBreak = super.getHopper().getBlock().getRelative(BlockFace.UP);
            if (toBreak != null && toBreak.getType().isSolid()) {
                if (super.getHopper().addItem(toBreak.getType())) {
                    toBreak.setType(Material.AIR);
                } else {
                    //TODO: sell item
                }
            }
        }, 10, 10);
    }

    @Override
    public RHopper.Trait getTraitType() {
        return RHopper.Trait.BLOCK_BREAKING;
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
