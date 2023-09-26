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
import joserodpt.realhoppers.hopper.HopperGUI;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.hopper.trait.traits.RHItemTransfer;
import joserodpt.realhoppers.utils.Text;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private RealHoppers rh;
    public PlayerListener(RealHoppers rh) {
        this.rh = rh;
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        e.getPlayer().getInventory().addItem(new ItemStack(Material.STICK));
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();


        if (clickedBlock != null &&
                clickedBlock.getType() == Material.HOPPER && event.getAction() == Action.RIGHT_CLICK_BLOCK) {

            RHopper clicked = rh.getHopperManager().getHopper(clickedBlock);

            if (clicked == null) {
                return;
            }

            if (player.getInventory().getItemInMainHand().getType() == Material.STICK) {
                event.setCancelled(true);

                RHopperTrait trait = RHopperTrait.ITEM_TRANS;

                if (clicked.hasTrait(trait)) {
                    Text.send(player, "&cThis hopper already has a Teleportation Link.");
                    return;
                }

                RHopper prevClicked = rh.getPlayerManager().getClickedHoppers().get(player);

                if (rh.getPlayerManager().getClickedHoppers().containsKey(player)) {

                    if (prevClicked == clicked) {
                        player.sendMessage("nao pode ser o mm");
                    } else {
                        prevClicked.setTrait(trait, new RHItemTransfer(prevClicked, clicked));

                        // prevClicked.setTrait(trait, new RHTeleportation(prevClicked, clicked));
                        // clicked.setTrait(trait, new RHTeleportation(clicked, prevClicked));
                        //

                        player.sendMessage("Linked!");

                        rh.getPlayerManager().getClickedHoppers().remove(player);
                    }
                } else {
                    rh.getPlayerManager().getClickedHoppers().put(player, clicked);
                }
            } else {
                event.setCancelled(true);
                HopperGUI hg = new HopperGUI(player, clicked, rh);
                hg.openInventory(player);
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Block playerBlock = player.getLocation().getBlock();

        if (playerBlock.getType() == Material.HOPPER) {
            executeHopperTeleport(player, playerBlock);
            return;
        }

        playerBlock = playerBlock.getRelative(BlockFace.DOWN);

        if (playerBlock.getType() == Material.HOPPER) {
            executeHopperTeleport(player, playerBlock);
        }
    }

    private void executeHopperTeleport(Player player, Block playerBlock) {
        RHopper h = rh.getHopperManager().getHopper(playerBlock);
        if (h != null && h.hasTrait(RHopperTrait.TELEPORT) && h.getTrait(RHopperTrait.TELEPORT).isLinked()) {
            h.getTrait(RHopperTrait.TELEPORT).executeAction(player);
        }
    }
}
