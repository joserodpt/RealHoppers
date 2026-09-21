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
import joserodpt.realhoppers.api.utils.Compacting;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Squashes what the hopper is holding: nine ingots become a block, nine nuggets an ingot.
 *
 * <p>Unlike smelting this cannot happen on the way in, because it takes nine of a thing to do
 * anything at all - so it runs over the hopper's contents on a timer instead. The power is how many
 * passes it makes each cycle, which is what lets a high tier carry nuggets all the way to a block
 * in one go.</p>
 */
public class RHAutoCompactTrait extends RHopperTraitBase {

    public RHAutoCompactTrait(RHopper main) {
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

            final int passes = (int) Math.max(1, RHopperTrait.AUTO_COMPACT.configValue("Passes", 1) * super.power());

            for (int pass = 0; pass < passes; pass++) {
                //a pass that changed nothing means nothing is left to squash, and the tier should
                //not cost the server the remaining sweeps for it
                if (!compactOnce()) {
                    break;
                }
            }
        }, 20, 20);
    }

    /**
     * One sweep of the hopper's slots.
     *
     * @return whether anything was compacted
     */
    private boolean compactOnce() {
        final Inventory inventory = super.getHopper().getInventory();
        if (inventory == null) {
            return false;
        }

        boolean changed = false;

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            final ItemStack held = inventory.getItem(slot);
            if (held == null || held.getAmount() < Compacting.required()) {
                continue;
            }

            final int times = held.getAmount() / Compacting.required();
            final ItemStack made = Compacting.compact(held.getType(), times);
            if (made == null) {
                continue;
            }

            //the ingredients come out first, so the room they free is there for the result
            final int keeping = held.getAmount() - (times * Compacting.required());
            if (keeping <= 0) {
                inventory.clear(slot);
            } else {
                held.setAmount(keeping);
            }

            //back through the hopper, so the result is smelted, sold or voided like anything else
            final ItemStack left = super.getHopper().offer(made);
            if (left != null) {
                super.getHopper().getWorld().dropItemNaturally(super.getHopper().getTeleportLocation(), left);
            }
            changed = true;
        }

        return changed;
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.AUTO_COMPACT;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
