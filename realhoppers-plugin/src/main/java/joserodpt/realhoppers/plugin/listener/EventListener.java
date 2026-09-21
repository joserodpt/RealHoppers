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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

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
            rh.getHopperManager().getHoppersMap().put(e.getBlockPlaced(), new RHopper(e.getBlockPlaced(), true));
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
                .setV1(TranslatableLine.ReplacableVar.MONEY.eq(Text.formatNumber(h.getBalance()))).send(p);
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

    @EventHandler
    public void onHopperStateChange(RHopperStateChangeEvent e) {
        rh.getGUIManager().refresh(e.getHopper());
    }
}
