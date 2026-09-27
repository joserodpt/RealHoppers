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

import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.RHopperAccess;
import joserodpt.realhoppers.api.hopper.events.RHopperStateChangeEvent;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.plugin.RealHoppers;
import joserodpt.realhoppers.plugin.managers.StoredHopper;
import joserodpt.realutils.text.Text;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.MONEY;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

public class EventListener implements Listener {
    /** Needed, on top of the config option, for a broken private hopper to keep its owner and contents. */
    public static final String KEEP_CONTENTS_PERMISSION = "realhoppers.keepcontents";

    private final RealHoppers rh;
    public EventListener(RealHoppers rh) {
        this.rh = rh;
    }

    /** Prefix of the permissions that cap how many hoppers a player can own: .none, .5, .20... */
    public static final String LIMIT_PERMISSION_PREFIX = "realhoppers.limit.";

    /**
     * Refuses a hopper its owner has no room for. Here, at HIGH, because the MONITOR handler below
     * can no longer cancel the placement.
     *
     * <p>With Hoppers.Place-Default-When-Limit-Reached on, the placement goes ahead as a plain
     * vanilla hopper instead, which {@link #onPlaceSpecialHopper} then leaves unregistered. A private
     * hopper being put back is refused either way: as a plain hopper its owner and items would be lost.</p>
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlaceOverLimit(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.HOPPER) {
            return;
        }
        final StoredHopper stored = StoredHopper.fromItem(rh.getPlugin(), e.getItemInHand());
        final int limit = reachedLimit(e, stored);
        if (limit >= 0 && (stored != null || !placeDefaultWhenLimitReached())) {
            e.setCancelled(true);
            TranslatableLine.HOPPER_LIMIT_REACHED.with(VALUE, limit).send(e.getPlayer());
        }
    }

    /**
     * The limit this placement runs into, or -1 when its owner still has room.
     *
     * <p>A private hopper being put back counts against its old owner, since that is who it goes
     * back to - not against whoever happens to be placing it.</p>
     */
    private int reachedLimit(final BlockPlaceEvent e, final StoredHopper stored) {
        final UUID owner = stored != null && stored.getOwner() != null ? stored.getOwner() : e.getPlayer().getUniqueId();
        final int limit = hopperLimit(owner.equals(e.getPlayer().getUniqueId()) ? e.getPlayer() : Bukkit.getPlayer(owner));
        return limit >= 0 && rh.getHopperManager().getHoppersOwnedBy(owner).size() >= limit ? limit : -1;
    }

    private static boolean placeDefaultWhenLimitReached() {
        return RHConfig.file().getBoolean("RealHoppers.Hoppers.Place-Default-When-Limit-Reached", false);
    }

    /**
     * How many hoppers a player may own, or -1 for no limit. realhoppers.limit.none and
     * realhoppers.admin lift it; otherwise the highest realhoppers.limit.&lt;n&gt; the player has
     * wins, and a player with none of them - or one who is offline, whose permissions can't be
     * asked - gets Hoppers.Max-Per-Player.
     */
    private static int hopperLimit(final Player player) {
        if (player == null) {
            return defaultHopperLimit();
        }
        if (player.hasPermission(LIMIT_PERMISSION_PREFIX + "none") || player.hasPermission(RHopper.ADMIN_PERMISSION)) {
            return -1;
        }
        int limit = -1;
        for (final PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            final String permission = info.getPermission().toLowerCase();
            if (!info.getValue() || !permission.startsWith(LIMIT_PERMISSION_PREFIX)) {
                continue;
            }
            try {
                limit = Math.max(limit, Integer.parseInt(permission.substring(LIMIT_PERMISSION_PREFIX.length())));
            } catch (final NumberFormatException ignored) {
                //realhoppers.limit.* and the like: not a number, so not a limit
            }
        }
        return limit >= 0 ? limit : defaultHopperLimit();
    }

    /** Hoppers.Max-Per-Player as a number, or -1 when it is "unlimited" or not a number at all. */
    private static int defaultHopperLimit() {
        final String value = RHConfig.file().getString("RealHoppers.Hoppers.Max-Per-Player", "unlimited").trim();
        if (value.equalsIgnoreCase("unlimited")) {
            return -1;
        }
        try {
            return Math.max(-1, Integer.parseInt(value));
        } catch (final NumberFormatException ex) {
            RealHoppersAPI.getInstance().getLogger().warning("Hoppers.Max-Per-Player is \"" + value
                    + "\", which is neither \"unlimited\" nor a number. Treating it as unlimited.");
            return -1;
        }
    }

    //MONITOR with ignoreCancelled: a protection plugin gets to refuse the break first. At NORMAL
    //the hopper was deleted from disk even when the block was never actually broken.
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaceSpecialHopper(BlockPlaceEvent e) {
        //asking what is in the main hand misses a hopper placed from the off hand; the placed
        //block is what decides whether this is a hopper
        if (e.getBlockPlaced().getType() == Material.HOPPER) {
            final StoredHopper stored = StoredHopper.fromItem(rh.getPlugin(), e.getItemInHand());

            //over the limit and let through by onPlaceOverLimit: it stays a plain vanilla hopper.
            //Told here rather than there, so a protection plugin refusing it after HIGH says nothing
            if (stored == null) {
                final int limit = reachedLimit(e, null);
                if (limit >= 0) {
                    TranslatableLine.HOPPER_LIMIT_REACHED_DEFAULT.with(VALUE, limit).send(e.getPlayer());
                    return;
                }
            }

            //whoever placed it owns it
            final RHopper placed = new RHopper(e.getBlockPlaced(), e.getPlayer());
            rh.getHopperManager().getHoppersMap().put(e.getBlockPlaced(), placed);

            //unless it is a private hopper that was broken and is being put back: then it is still
            //its old owner's, still private, and has its items again
            if (stored != null) {
                placed.restoreOwner(stored.getOwner(), stored.getOwnerName());
                placed.restoreAccess(RHopperAccess.PRIVATE);
                placed.saveData(RHopper.Data.OWNERSHIP);
                final Inventory inventory = placed.getInventory();
                if (inventory != null) {
                    inventory.setContents(Arrays.copyOf(stored.getContents(), inventory.getSize()));
                }
            }
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
                //before the contents drop and the hopper goes, so nobody is left clicking at a dead block
                rh.getGUIManager().closeScreens(h);
                if (h.getAccess() == RHopperAccess.PRIVATE && h.hasOwner()
                        && RHConfig.file().getBoolean("RealHoppers.Hoppers.Keep-Private-Contents-On-Break", true)
                        && (e.getPlayer().hasPermission(KEEP_CONTENTS_PERMISSION)
                        || e.getPlayer().hasPermission(RHopper.ADMIN_PERMISSION))) {
                    dropStored(e, h);
                }
                payOut(h, e.getPlayer());
                rh.getHopperManager().delete(h);
                TranslatableLine.HOPPER_REMOVED.send(e.getPlayer());
                //TODO: drop special hopper item
            }
        }
    }

    /**
     * Drops a private hopper as an item that remembers its owner and contents, instead of the plain
     * hopper and its items spilling out. Only when the break would have dropped the hopper at all;
     * otherwise (by hand, say) the contents spill as they always did.
     */
    private void dropStored(final BlockBreakEvent e, final RHopper h) {
        final Inventory inventory = h.getInventory();
        if (inventory == null || !e.isDropItems()
                || e.getBlock().getDrops(e.getPlayer().getInventory().getItemInMainHand()).isEmpty()) {
            return;
        }

        final ItemStack[] contents = new ItemStack[inventory.getSize()];
        for (int i = 0; i < contents.length; i++) {
            final ItemStack item = inventory.getItem(i);
            contents[i] = item == null ? null : item.clone();
        }
        //emptied first, or the block would still spill its contents as it breaks
        inventory.clear();
        e.setDropItems(false);
        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation().add(0.5, 0.5, 0.5),
                StoredHopper.toItem(rh.getPlugin(), h, contents));
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
                    rh.getGUIManager().closeScreens(h);
                    //nobody broke it, so the balance can only go to its owner
                    payOut(h, null);
                    rh.getHopperManager().delete(h);
                    //TODO: drop special hopper item
                }
            }
        }
    }

    /**
     * Hands whatever the hopper had banked to its owner. Deleting the hopper deletes its balance
     * with it, so without this the money is simply gone. It used to go to whoever broke it, which on
     * a public hopper was anyone.
     *
     * @param breaker who broke it, or null for an explosion. Paid only for a hopper nobody owns.
     */
    private void payOut(final RHopper h, final Player breaker) {
        final OfflinePlayer payee = h.hasOwner() ? Bukkit.getOfflinePlayer(h.getOwner()) : breaker;
        if (payee == null) {
            return;
        }

        final double balance = h.getBalance();
        if (balance > 0 && rh.getEconomy() != null) {
            //Vault pays offline players too
            final EconomyResponse paid = rh.getEconomy().depositPlayer(payee, balance);
            if (paid != null && paid.transactionSuccess()) {
                if (payee.getPlayer() != null) {
                    TranslatableLine.HOPPER_BALANCE_COLLECTED_ON_BREAK
                            .with(MONEY, Text.formatNumber(balance)).send(payee.getPlayer());
                }
            } else {
                rh.getLogger().warning("Could not pay the balance of the hopper at " + h.getSerializedLocation()
                        + " (" + balance + ") to " + payee.getName() + ": "
                        + (paid == null ? "no response" : paid.errorMessage));
            }
        }

        //experience cannot be handed to an offline player, so it only goes to one who is online
        final Player online = payee.getPlayer();
        if (h.getXp() > 0 && online != null) {
            online.giveExp(h.getXp());
        }

        //the restore setters: the hopper is about to be deleted, and the event would only redraw it
        h.restoreBalance(0);
        h.restoreXp(0);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!event.getEntity().hasMetadata("rh")) {
            return;
        }
        //a mob a hopper wore down and a player finished off is the player's kill
        if (event.getEntity().getKiller() != null) {
            return;
        }

        for (final MetadataValue value : event.getEntity().getMetadata("rh")) {
            if (value.getOwningPlugin() != rh.getPlugin() || !(value.value() instanceof RHopper)) {
                continue;
            }
            final RHopper tagged = (RHopper) value.value();
            //looked up again by location: the tagged hopper may have been broken since, or replaced by
            //a reload, and a stale one would store into a block that isn't registered any more
            final RHopper source = rh.getHopperManager().getHopper(tagged.getBlock());
            if (source != null && source.hasTrait(RHopperTrait.KILL_MOB)) {
                for (ItemStack drop : event.getDrops()) {
                    //sold a single unit of the stack and threw the rest away before this
                    final ItemStack left = source.offer(drop);
                    if (left != null && RHConfig.file().getBoolean("RealHoppers.Drop-Items-If-Full")) {
                        source.getWorld().dropItemNaturally(source.getTeleportLocation(), left);
                    }
                }
                event.getDrops().clear();
                return;
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
