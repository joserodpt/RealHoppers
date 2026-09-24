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
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
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

import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.PLAYER;

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

        //sneaking is how vanilla places a block against a container rather than opening it, and
        //a hopper feeding a hopper is the whole point of them - so the plugin gets out of the way
        if (player.isSneaking()) {
            return;
        }

        event.setCancelled(true);

        //a private hopper is closed to everyone but its owner, its whitelist and admins - its
        //screen and its link alike
        if (!clicked.canAccess(player)) {
            if (player.getInventory().getItemInMainHand().getType() == LINK_TOOL) {
                //kept pending, so the player can go and click a hopper they may link to
                TranslatableLine.ACCESS_NO_LINK
                        .with(NAME, clicked.getName()).send(player);
                return;
            }
            cancelLink(player);
            TranslatableLine.ACCESS_DENIED
                    .with(NAME, clicked.getName())
                    .with(PLAYER, clicked.getOwnerDisplayName()).send(player);
            return;
        }

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
        if (rh.getPlayerManager().getPendingLinks().remove(player.getUniqueId()) != null) {
            TranslatableLine.LINK_CANCELLED.send(player);
        }
    }

    /**
     * The second half of the linking flow: the player has a source hopper picked and has just
     * clicked the one it should point at.
     *
     * <p>The link belongs to the hopper, not to a trait. Linking is now one gesture that says
     * "this hopper points at that one", and TELEPORT and ITEM_TRANSF both follow it - so a pair of
     * hoppers can be a teleporter and an item pipe at once, and re-pointing the source moves both.</p>
     */
    private void link(final Player player, final RHopper clicked) {
        final RHopper source = rh.getPlayerManager().getPendingLinks().get(player.getUniqueId());

        if (source == null) {
            rh.getPlayerManager().getPendingLinks().put(player.getUniqueId(), clicked);
            TranslatableLine.LINK_SOURCE_SELECTED.send(player);
            return;
        }

        if (source == clicked) {
            //the selection is kept, so the player can go and click the one they meant
            TranslatableLine.LINK_SAME_HOPPER.send(player);
            return;
        }

        rh.getPlayerManager().getPendingLinks().remove(player.getUniqueId());

        //the destination was checked when it was clicked; the source was when it was picked, but
        //its owner may have made it private since
        if (!source.canAccess(player)) {
            TranslatableLine.ACCESS_NO_LINK
                    .with(NAME, source.getName()).send(player);
            return;
        }

        //replaces whatever it pointed at before, rather than refusing: one link per hopper means
        //this is the only way to change it
        source.setLink(clicked);
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
