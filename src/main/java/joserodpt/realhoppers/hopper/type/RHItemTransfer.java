package joserodpt.realhoppers.hopper.type;

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.hopper.RHopper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class RHItemTransfer extends RHopperTraitBase {

    public RHItemTransfer(RHopper main, String linkLocation) {
        super(main);
        super.setLinkedLoc(linkLocation);
    }

    public RHItemTransfer(RHopper main, RHopper link) {
        super(main);
        super.setLinked(link);
    }

    @Override
    public void executeAction(Player p) { }

    private int taskID;

    @Override
    public void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppers.getPlugin(), () -> {
            ItemStack itemStack = getFirst();
            if (itemStack != null) {
                if (itemStack.getAmount() > 1) {
                    itemStack.setAmount(itemStack.getAmount() - 1);
                } else {
                    super.getHopper().getInventory().removeItem(itemStack);
                }

                final ItemStack clone = itemStack.clone();
                clone.setAmount(1);

                super.getLinkedHopper().getInventory().addItem(clone);
            }
        }, 10, 10);
    }

    private ItemStack getFirst() {
        return Arrays.stream(super.getHopper().getInventory().getContents())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public RHopper.Trait getTraitType() {
        return RHopper.Trait.ITEM_TRANS;
    }

    @Override
    public void stopTask() {
        Bukkit.getScheduler().cancelTask(taskID);
    }

    @Override
    public String getSerializedSave() {
        return getTraitType() + "|" + getLinkedHopper().getSerializedLocation();
    }
}
