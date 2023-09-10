package joserodpt.realhoppers.listener;

import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.type.RHTeleportation;
import joserodpt.realhoppers.utils.LocationUtil;
import joserodpt.realhoppers.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Block clickedBlock = event.getClickedBlock();

        if (player.getInventory().getItemInMainHand().getType() == Material.STICK && clickedBlock != null &&
                clickedBlock.getType() == Material.HOPPER) {

            event.setCancelled(true);

            RHopper h = rh.getHopperManager().getHopper(clickedBlock);

            if (h == null) {
                return;
            }

            if (h.hasTrait(RHopper.Trait.TELEPORT)) {
                Text.send(player, "&cThis hopper already has a Teleportation Link.");
                return;
            }

            if (rh.getPlayerManager().getClickedHoppers().containsKey(player)) {
                RHopper prevClicked = rh.getPlayerManager().getClickedHoppers().get(player);

                if (prevClicked == h) {
                    player.sendMessage("nao pode ser o mm");
                } else {
                    prevClicked.setTrait(RHopper.Trait.TELEPORT, new RHTeleportation(prevClicked, h));
                    h.setTrait(RHopper.Trait.TELEPORT, new RHTeleportation(h, prevClicked));

                    player.sendMessage("Linked!");

                    rh.getPlayerManager().getClickedHoppers().remove(player);
                }
            } else {
                rh.getPlayerManager().getClickedHoppers().put(player, h);
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
        if (h != null && h.hasTrait(RHopper.Trait.TELEPORT) && h.getTrait(RHopper.Trait.TELEPORT).isLinked()) {
            h.getTrait(RHopper.Trait.TELEPORT).executeAction(player);
        }
    }
}
