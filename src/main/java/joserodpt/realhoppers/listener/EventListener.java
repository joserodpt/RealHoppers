package joserodpt.realhoppers.listener;

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

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.config.Config;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.utils.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {
    private RealHoppers rh;
    public EventListener(RealHoppers rh) {
        this.rh = rh;
    }

    @EventHandler
    public void onPlaceSpecialHopper(BlockPlaceEvent e) {
        if (e.getPlayer().getInventory().getItemInMainHand().getType().equals(Material.HOPPER)) {
            rh.getHopperManager().getHoppersMap().put(e.getBlockPlaced(), new RHopper(e.getBlockPlaced(), true));
        }
    }

    @EventHandler
    public void onRemoveSpecialHopper(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.HOPPER) {
            RHopper h = rh.getHopperManager().getHopper(e.getBlock());
            if (h != null) {
                rh.getHopperManager().delete(h);
                Text.send(e.getPlayer(), "You removed this special hopper.");
                //TODO: drop special hopper item
            }
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (block.getType() == Material.HOPPER) {
                RHopper h = rh.getHopperManager().getHopper(block);
                if (h != null) {
                    rh.getHopperManager().delete(h);
                    //TODO: drop special hopper item
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if the entity is a mob
        if (event.getEntity().hasMetadata("rh")) {
            RHopper source = (RHopper) event.getEntity().getMetadata("rh").get(0).value();
            if (source != null) {
                for (ItemStack drop : event.getDrops()) {
                    if (source.hasHopperSpace(drop)) {
                        source.addItem(drop);
                    } else {
                        if (source.hasTrait(RHopperTrait.AUTO_SELL)) {
                            source.sell(drop.getType());
                        } else {
                            if (Config.file().getBoolean("RealHoppers.Drop-Items-If-Full"))
                                source.getWorld().dropItemNaturally(source.getTeleportLocation(), drop);
                        }
                    }
                }
                event.getDrops().clear();
            }
        }
    }
}
