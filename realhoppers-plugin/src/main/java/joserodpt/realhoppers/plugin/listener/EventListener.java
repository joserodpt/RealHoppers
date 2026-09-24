package joserodpt.realhoppers.plugin.listener;

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

import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.plugin.RealHoppers;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.events.RHopperStateChangeEvent;
import joserodpt.realhoppers.api.utils.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.MONEY;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

public class EventListener implements Listener {
    private final RealHoppers rh;
    public EventListener(RealHoppers rh) {
        this.rh = rh;
    }

    //MONITOR with ignoreCancelled: a protection plugin gets to refuse the break first. At NORMAL
    //the hopper was deleted from disk even when the block was never actually broken.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceSpecialHopper(BlockPlaceEvent e) {
        //asking what is in the main hand misses a hopper placed from the off hand; the placed
        //block is what decides whether this is a hopper
        if (e.getBlockPlaced().getType() == Material.HOPPER) {
            //whoever placed it owns it
            final RHopper placed = new RHopper(e.getBlockPlaced(), e.getPlayer());
            rh.getHopperManager().getHoppersMap().put(e.getBlockPlaced(), placed);
            TranslatableLine.HOPPER_PLACED
                    .with(NAME, placed.getName())
                    .with(VALUE, placed.getAccess().getDisplayName()).send(e.getPlayer());
        }
    }

    /**
     * Keeps a private hopper from being broken by anyone it is not open to. Refused here, at HIGH,
     * so the break never reaches the MONITOR handler below that pays the balance out and deletes
     * the hopper - and so a protection plugin at HIGHEST still gets the last word.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreakPrivateHopper(BlockBreakEvent e) {
        if (e.getBlock().getType() != Material.HOPPER) {
            return;
        }
        final RHopper h = rh.getHopperManager().getHopper(e.getBlock());
        if (h != null && !h.canAccess(e.getPlayer())) {
            e.setCancelled(true);
            TranslatableLine.ACCESS_NO_BREAK
                    .with(NAME, h.getName()).send(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemoveSpecialHopper(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.HOPPER) {
            RHopper h = rh.getHopperManager().getHopper(e.getBlock());
            if (h != null) {
                payOut(h, e.getPlayer());
                rh.getHopperManager().delete(h);
                TranslatableLine.HOPPER_REMOVED.send(e.getPlayer());
                //TODO: drop special hopper item
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        deleteBlownUp(e.blockList());
    }

    /**
     * TNT, a creeper, a bed in the nether. Only block-caused explosions were handled, so anything
     * blown up by an entity left its hopper registered with its trait loops still running against a
     * block that was no longer there.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        deleteBlownUp(e.blockList());
    }

    private void deleteBlownUp(final List<Block> blocks) {
        for (Block block : blocks) {
            if (block.getType() == Material.HOPPER) {
                RHopper h = rh.getHopperManager().getHopper(block);
                if (h != null) {
                    rh.getHopperManager().delete(h);
                    //TODO: drop special hopper item
                }
            }
        }
    }

    /**
     * Hands whatever an auto-selling hopper had banked to the player breaking it. Deleting the
     * hopper deletes its balance with it, so without this the money is simply gone.
     */
    private void payOut(final RHopper h, final Player p) {
        if (h.getBalance() <= 0 || rh.getEconomy() == null) {
            return;
        }
        rh.getEconomy().depositPlayer(p, h.getBalance());
        TranslatableLine.HOPPER_BALANCE_COLLECTED_ON_BREAK
                .with(MONEY, Text.formatNumber(h.getBalance())).send(p);
        h.setBalance(0);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if the entity is a mob
        if (event.getEntity().hasMetadata("rh")) {
            RHopper source = (RHopper) event.getEntity().getMetadata("rh").get(0).value();
            if (source != null) {
                for (ItemStack drop : event.getDrops()) {
                    //sold a single unit of the stack and threw the rest away before this
                    final ItemStack left = source.offer(drop);
                    if (left != null && RHConfig.file().getBoolean("RealHoppers.Drop-Items-If-Full")) {
                        source.getWorld().dropItemNaturally(source.getTeleportLocation(), left);
                    }
                }
                event.getDrops().clear();
            }
        }
    }

    /**
     * A hopper sucking up a dropped item off the ground.
     */
    @EventHandler(ignoreCancelled = true)
    public void onHopperPickup(InventoryPickupItemEvent e) {
        final RHopper hopper = hopperOf(e.getInventory());
        if (hopper == null) {
            return;
        }

        final ItemStack incoming = e.getItem().getItemStack();
        if (!hopper.intercepts(incoming)) {
            return;
        }

        e.setCancelled(true);
        final ItemStack left = hopper.offer(incoming);
        if (left == null) {
            e.getItem().remove();
        } else {
            e.getItem().setItemStack(left);
        }
    }

    /**
     * A container pushing into a hopper, or a hopper pulling out of one above it.
     *
     * <p>Until this existed, AUTO_SMELT and AUTO_SELL only applied to what RealHoppers' own traits
     * fed a hopper - so a chest feeding one from above went straight past both, which is not what
     * anybody looking at the hopper would expect.</p>
     */
    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent e) {
        final RHopper hopper = hopperOf(e.getDestination());
        if (hopper == null) {
            return;
        }

        final ItemStack incoming = e.getItem();
        if (!hopper.intercepts(incoming)) {
            return;
        }

        //cancelling leaves the item where it was, so whatever the hopper took has to be taken out
        //of the source by hand
        e.setCancelled(true);
        final ItemStack left = hopper.offer(incoming);
        final int taken = incoming.getAmount() - (left == null ? 0 : left.getAmount());
        if (taken > 0) {
            final ItemStack removed = incoming.clone();
            removed.setAmount(taken);
            e.getSource().removeItem(removed);
        }
    }

    /**
     * The RealHoppers hopper an inventory belongs to, or null for anything else.
     *
     * <p>Both callers are hot paths, so this answers on an empty map before it touches the holder -
     * reading one builds a block state snapshot.</p>
     */
    private RHopper hopperOf(final Inventory inventory) {
        if (rh.getHopperManager().getHoppersMap().isEmpty()) {
            return null;
        }
        //a hopper minecart holds a HopperMinecart rather than a Hopper, and is not ours
        final InventoryHolder holder = inventory.getHolder();
        return holder instanceof Hopper ? rh.getHopperManager().getHopper(((Hopper) holder).getBlock()) : null;
    }

    @EventHandler
    public void onHopperStateChange(RHopperStateChangeEvent e) {
        rh.getGUIManager().refresh(e.getHopper());
    }
}
