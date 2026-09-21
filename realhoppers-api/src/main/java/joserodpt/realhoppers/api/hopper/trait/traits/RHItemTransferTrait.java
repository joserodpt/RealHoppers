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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public class RHItemTransferTrait extends RHopperTraitBase {

    public RHItemTransferTrait(RHopper main) {
        super(main);
    }

    @Override
    public void executeAction(Player p) { }

    private int taskID = -1;

    @Override
    protected void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppersAPI.getInstance().getPlugin(), () -> {

            if (!super.linkReady()) {
                return;
            }

            final ItemStack itemStack = getFirst();
            if (itemStack == null) {
                return;
            }

            final int perCycle = (int) Math.max(1,
                    RHopperTrait.ITEM_TRANSF.configValue("Items", 1) * super.power());
            final int moving = Math.min(perCycle, itemStack.getAmount());

            final ItemStack clone = itemStack.clone();
            clone.setAmount(moving);

            //asked about the whole amount being moved, not about one of them: a tier that moves
            //eight at a time must not start on a hopper with room for three
            if (!super.getLinkedHopper().hasHopperSpace(clone)) {
                return;
            }

            if (itemStack.getAmount() > moving) {
                itemStack.setAmount(itemStack.getAmount() - moving);
            } else {
                final Inventory source = super.getHopper().getInventory();
                if (source != null) {
                    source.removeItem(itemStack);
                }
            }

            super.getLinkedHopper().addItem(clone);
        }, 10, 10);
    }

    private ItemStack getFirst() {
        final Inventory inventory = super.getHopper().getInventory();
        if (inventory == null) {
            return null;
        }
        return Arrays.stream(inventory.getContents())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.ITEM_TRANSF;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
