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
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHItemTransferTrait;
import joserodpt.realhoppers.plugin.RealHoppers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerListener implements Listener {

    /** What a player holds to link two hoppers instead of opening one. */
    private static final Material LINK_TOOL = Material.STICK;

    private final RealHoppers rh;

    public PlayerListener(RealHoppers rh) {
        this.rh = rh;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        //a half-finished link and a teleport cooldown are both meaningless once they are gone,
        //and holding either keeps the entry around for a player who may never come back
        rh.getPlayerManager().clear(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        //a right-click fires once per hand, and the off-hand pass would run the whole link flow a
        //second time - far enough along to answer its own first click with "cannot link to itself"
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        final Player player = event.getPlayer();
        final Block clickedBlock = event.getClickedBlock();
        final Action action = event.getAction();

        final RHopper clicked = clickedBlock == null || clickedBlock.getType() != Material.HOPPER
                ? null
                : rh.getHopperManager().getHopper(clickedBlock);

        if (clicked == null || action != Action.RIGHT_CLICK_BLOCK) {
            //clicking anything else abandons a half-finished link, so it cannot be completed
            //minutes later against a hopper the player has forgotten about. Swinging at the air is
            //not clicking something else, and a pressure plate is not a click at all.
            if (action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK) {
                cancelLink(player);
            }
            return;
        }

        event.setCancelled(true);

        if (player.getInventory().getItemInMainHand().getType() == LINK_TOOL) {
            this.link(player, clicked);
            return;
        }

        //a hopper right-clicked without the tool opens its panel, and is not the link gesture either
        cancelLink(player);
        rh.getGUIManager().openHopper(player, clicked);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.getItemDrop().getItemStack().getType() == LINK_TOOL) {
            cancelLink(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        cancelLink(event.getPlayer());
    }

    /**
     * Drops a link the player started and never finished, telling them so. Silent when there was
     * nothing pending, which is almost every click that reaches it.
     */
    private void cancelLink(final Player player) {
        if (rh.getPlayerManager().getClickedHoppers().remove(player.getUniqueId()) != null) {
            TranslatableLine.LINK_CANCELLED.send(player);
        }
    }

    /**
     * The two-click linking flow: the first click remembers a source hopper, the second points it at
     * the hopper clicked.
     */
    private void link(final Player player, final RHopper clicked) {
        final RHopper source = rh.getPlayerManager().getClickedHoppers().get(player.getUniqueId());

        if (source == null) {
            rh.getPlayerManager().getClickedHoppers().put(player.getUniqueId(), clicked);
            TranslatableLine.LINK_SOURCE_SELECTED.send(player);
            return;
        }

        if (source == clicked) {
            TranslatableLine.LINK_SAME_HOPPER.send(player);
            return;
        }

        //the trait goes on the source, so the source is what has to be free. This used to test the
        //hopper being clicked, which refused valid links and silently overwrote invalid ones.
        if (source.hasTrait(RHopperTrait.ITEM_TRANS)) {
            TranslatableLine.LINK_ALREADY_LINKED.send(player);
            rh.getPlayerManager().getClickedHoppers().remove(player.getUniqueId());
            return;
        }

        source.setTrait(RHopperTrait.ITEM_TRANS, new RHItemTransferTrait(source, clicked));
        rh.getPlayerManager().getClickedHoppers().remove(player.getUniqueId());
        TranslatableLine.LINK_DONE.send(player);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        final Location to = event.getTo();
        final Location from = event.getFrom();

        //fires for every look and every fraction of a step. Only a change of block can put somebody
        //onto a hopper they were not on a moment ago, so everything else is dropped before it costs
        //a block lookup.
        if (to == null || (from.getBlockX() == to.getBlockX()
                && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()
                && from.getWorld() == to.getWorld())) {
            return;
        }

        Player player = event.getPlayer();
        Block playerBlock = to.getBlock();

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
