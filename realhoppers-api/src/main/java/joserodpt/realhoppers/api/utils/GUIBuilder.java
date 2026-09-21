package joserodpt.realhoppers.api.utils;

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

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import joserodpt.realhoppers.api.RealHoppersAPI;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class GUIBuilder {

    /*
     * Modified and optimized version of AdvInventory Original author:
     * http://spigotmc.org/members/25376/ - Homer04 Original utility version:
     * http://www.spigotmc.org/threads/133942/ Modified by AnyOD Compatible
     * https://www.spigotmc.org/threads/gui-creator-v2-making-inventories-was-never-easier.296898/
     * versions: 1.8 and up
     */

    private static final Map<UUID, GUIBuilder> inventories = new HashMap<>();
    private Inventory inv;
    private final Map<Integer, ClickRunnable> runnables = new HashMap<>();
    private final UUID uuid;

    /** Slots the player may move things in and out of. Empty on a screen that is only buttons. */
    private final Set<Integer> editable = new LinkedHashSet<>();
    private Consumer<Inventory> onEdit;

    public GUIBuilder(final String name, final int size, final UUID uuid) {
        this(Text.color(name), size, uuid, null);
    }

    public GUIBuilder(final String name, final int size, final UUID uuid, final ItemStack placeholder) {
        this.uuid = uuid;
        if (size == 0) {
            return;
        }
        this.inv = Bukkit.createInventory(null, size, Text.color(name));
        if (placeholder != null) {
            for (int i = 0; i < size; ++i) {
                this.inv.setItem(i, placeholder);
            }
        }
        this.register();
    }

    public static Listener getListener() {
        return new Listener() {
            @EventHandler
            public void onClick(final InventoryClickEvent e) {
                final GUIBuilder current = builderFor(e.getWhoClicked(), e.getView());
                if (current == null) {
                    return;
                }

                final int raw = e.getRawSlot();
                final boolean inTop = raw >= 0 && raw < e.getView().getTopInventory().getSize();

                if (inTop) {
                    //a slot the player is meant to reach into - the hopper's own contents - is left
                    //alone, and the screen is read back into the hopper once the click has landed
                    if (current.editable.contains(raw)) {
                        current.scheduleEdit(e.getView().getTopInventory());
                        return;
                    }

                    e.setCancelled(true);
                    //by raw slot, not getSlot(): the two are the same in the top inventory and
                    //differ in the player's own, where a click used to fire a button of this one
                    final ClickRunnable runnable = current.runnables.get(raw);
                    if (runnable != null && e.getCurrentItem() != null) {
                        runnable.run(e);
                    }
                    return;
                }

                //the player's own inventory. A plain click there is theirs to make; a shift click
                //would throw the item into the first free slot up top, which may be a button
                if (e.isShiftClick()) {
                    e.setCancelled(true);
                    if (!current.editable.isEmpty()) {
                        current.shiftIntoEditable(e);
                    }
                }
            }

            @EventHandler
            public void onDrag(final InventoryDragEvent e) {
                final GUIBuilder current = builderFor(e.getWhoClicked(), e.getView());
                if (current == null) {
                    return;
                }

                //dragging was not handled at all before, so a dragged stack could be painted over
                //the buttons
                final int topSize = e.getView().getTopInventory().getSize();
                for (final int raw : e.getRawSlots()) {
                    if (raw < topSize && !current.editable.contains(raw)) {
                        e.setCancelled(true);
                        return;
                    }
                }

                if (!current.editable.isEmpty()) {
                    current.scheduleEdit(e.getView().getTopInventory());
                }
            }

            @EventHandler
            public void onClose(final InventoryCloseEvent e) {
                if (e.getPlayer() instanceof Player) {
                    if (e.getInventory() == null) {
                        return;
                    }
                    final Player p = (Player) e.getPlayer();
                    final UUID uuid = p.getUniqueId();
                    final GUIBuilder current = inventories.get(uuid);
                    if (current != null) {
                        //a last read, for anything moved and then closed on straight away
                        if (!current.editable.isEmpty()) {
                            current.readEdit(e.getView().getTopInventory());
                        }
                        current.unRegister();
                    }
                }
            }
        };
    }

    /**
     * The screen this player has open, or null when it is not one of ours.
     *
     * <p>Matched on type and size rather than identity because {@link #openInventory(Player)} pours
     * a new screen into the one already open when they are alike, so the inventory the player is
     * looking at is not always the one this object built.</p>
     */
    private static GUIBuilder builderFor(final HumanEntity clicker, final InventoryView view) {
        if (!(clicker instanceof Player)) {
            return null;
        }
        final GUIBuilder current = inventories.get(clicker.getUniqueId());
        if (current == null || current.getInventory() == null) {
            return null;
        }
        final Inventory top = view.getTopInventory();
        return top.getType() == current.getInventory().getType()
                && top.getSize() == current.getInventory().getSize() ? current : null;
    }

    /**
     * Lets the player move things in and out of these slots, and hands the screen back afterwards
     * so what they did can be written where it belongs.
     */
    public void setEditableSlots(final Collection<Integer> slots, final Consumer<Inventory> onEdit) {
        this.editable.clear();
        this.editable.addAll(slots);
        this.onEdit = onEdit;
    }

    /**
     * Reads the screen back a tick later. It has to be later: the click that caused this has not
     * been applied to the inventory yet while the event is still running.
     */
    private void scheduleEdit(final Inventory top) {
        if (this.onEdit == null) {
            return;
        }
        Bukkit.getScheduler().runTask(RealHoppersAPI.getInstance().getPlugin(), () -> this.readEdit(top));
    }

    private void readEdit(final Inventory top) {
        if (this.onEdit != null) {
            this.onEdit.accept(top);
        }
    }

    /**
     * Puts a shift clicked stack into the editable slots, which is what the client would have done
     * if the rest of the screen were not buttons.
     */
    private void shiftIntoEditable(final InventoryClickEvent e) {
        final ItemStack moving = e.getCurrentItem();
        if (moving == null || moving.getType() == Material.AIR) {
            return;
        }

        final Inventory top = e.getView().getTopInventory();
        final ItemStack remaining = moving.clone();

        for (final int slot : this.editable) {
            if (remaining.getAmount() <= 0) {
                break;
            }

            final ItemStack inSlot = top.getItem(slot);
            if (inSlot == null || inSlot.getType() == Material.AIR) {
                top.setItem(slot, remaining.clone());
                remaining.setAmount(0);
                break;
            }
            if (!inSlot.isSimilar(remaining)) {
                continue;
            }

            final int room = inSlot.getMaxStackSize() - inSlot.getAmount();
            if (room <= 0) {
                continue;
            }
            final int moved = Math.min(room, remaining.getAmount());
            inSlot.setAmount(inSlot.getAmount() + moved);
            top.setItem(slot, inSlot);
            remaining.setAmount(remaining.getAmount() - moved);
        }

        e.setCurrentItem(remaining.getAmount() <= 0 ? null : remaining);
        this.scheduleEdit(top);
    }

    public static ItemStack placeholder(final DyeColor d, final String n) {
        @SuppressWarnings("deprecation") final ItemStack placeholder = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1, d.getDyeData());
        final ItemMeta placeholdermeta = placeholder.getItemMeta();
        placeholdermeta.setDisplayName(n);
        placeholder.setItemMeta(placeholdermeta);
        return placeholder;
    }

    public Inventory getInventory() {
        return this.inv;
    }

    public int getSize() {
        return this.inv.getSize();
    }

    public void setItem(final ItemStack is, final Integer slot, final ClickRunnable executeOnClick) {
        this.inv.setItem(slot, is);
        this.runnables.put(slot, executeOnClick);
    }

    public void setItem(final ClickRunnable executeOnClick, final ItemStack itemstack, final Integer slot) {
        final ItemMeta im = itemstack.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        this.inv.setItem(slot, itemstack);
        this.runnables.put(slot, executeOnClick);
    }

    public void removeItem(final int slot) {
        this.inv.setItem(slot, new ItemStack(Material.AIR));
    }

    public void setItem(final ItemStack itemstack, final Integer slot) {
        this.inv.setItem(slot, itemstack);
    }

    public void openInventory(final Player player) {
        final Inventory inv = this.getInventory();
        final InventoryView openInv = player.getOpenInventory();
        if (openInv != null) {
            final Inventory openTop = player.getOpenInventory().getTopInventory();
            if (openTop != null && openTop.getType().name().equalsIgnoreCase(inv.getType().name())) {
                openTop.setContents(inv.getContents());
            } else {
                player.openInventory(inv);
            }
            this.register();
        }
    }

    private void register() {
        inventories.put(this.uuid, this);
    }

    private void unRegister() {
        inventories.remove(this.uuid);
    }

    public void addItem(final ClickRunnable clickRunnable, final ItemStack i, final int slot) {
        final ItemMeta im = i.getItemMeta();
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        this.inv.setItem(slot, i);
        this.runnables.put(slot, clickRunnable);
    }

    @FunctionalInterface
    public interface ClickRunnable {
        void run(InventoryClickEvent event);
    }

    @FunctionalInterface
    public interface CloseRunnable {
        void run(InventoryCloseEvent event);
    }
}
