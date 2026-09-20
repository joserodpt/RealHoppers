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
 * @author José Rodrigues
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (clickedBlock == null || clickedBlock.getType() != Material.HOPPER
                || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        RHopper clicked = rh.getHopperManager().getHopper(clickedBlock);
        if (clicked == null) {
            return;
        }

        event.setCancelled(true);

        if (player.getInventory().getItemInMainHand().getType() == LINK_TOOL) {
            this.link(player, clicked);
            return;
        }

        rh.getGUIManager().openHopper(player, clicked);
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
