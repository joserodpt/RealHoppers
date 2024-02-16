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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class RHItemTransferTrait extends RHopperTraitBase {

    public RHItemTransferTrait(RHopper main, String linkLocation) {
        super(main);
        super.setLinkedLoc(linkLocation);
    }

    public RHItemTransferTrait(RHopper main, RHopper link) {
        super(main);
        super.setLinked(link);
    }

    @Override
    public void executeAction(Player p) { }

    private int taskID;

    @Override
    public void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppersAPI.getInstance().getPlugin(), () -> {

            ItemStack itemStack = getFirst();
            if (itemStack != null) {
                if (super.getLinkedHopper().hasHopperSpace(itemStack.getType())) {
                    if (itemStack.getAmount() > 1) {
                        itemStack.setAmount(itemStack.getAmount() - 1);
                    } else {
                        super.getHopper().getInventory().removeItem(itemStack);
                    }

                    final ItemStack clone = itemStack.clone();
                    clone.setAmount(1);

                    super.getLinkedHopper().getInventory().addItem(clone);
                }
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
    public RHopperTrait getTraitType() {
        return RHopperTrait.ITEM_TRANS;
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
