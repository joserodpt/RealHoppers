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
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

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

    /**
     * Slots that show another inventory - the hopper's own five - with slot {@code i} of it in
     * {@code mirrored[i]}. Empty on a screen that is only buttons.
     *
     * <p>What is drawn there is only ever a picture. Every click on one of them is cancelled and
     * applied to the real inventory instead, so there is no copy to write back: writing a copy back
     * is what duplicated or deleted whatever the hopper moved while the screen was open, and let two
     * players viewing one hopper both take the same stack.</p>
     */
    private int[] mirrored = new int[0];
    private Supplier<Inventory> backing;
    private Runnable onMirrorChange;

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
        //registered when it is opened rather than here, so the screen still showing can be told
        //apart from this one until then
    }

    public static Listener getListener() {
        return new Listener() {
            @EventHandler
            public void onClick(final InventoryClickEvent e) {
                final GUIBuilder current = builderFor(e.getWhoClicked(), e.getView());
                if (current == null) {
                    return;
                }

                //a double click gathers matching items from every slot of the view, the pictures
                //of the hopper's slots included
                if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                    e.setCancelled(true);
                    return;
                }

                final int raw = e.getRawSlot();
                final boolean inTop = raw >= 0 && raw < e.getView().getTopInventory().getSize();

                if (inTop) {
                    e.setCancelled(true);
                    final int index = current.mirrorIndex(raw);
                    if (index >= 0) {
                        current.clickMirrored(e, index);
                        return;
                    }

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
                    if (current.mirrored.length > 0) {
                        current.shiftIntoMirror(e);
                    }
                }
            }

            @EventHandler
            public void onDrag(final InventoryDragEvent e) {
                final GUIBuilder current = builderFor(e.getWhoClicked(), e.getView());
                if (current == null) {
                    return;
                }

                //dragging over this GUI's buttons would drop the dragged items into it
                final int topSize = e.getView().getTopInventory().getSize();
                boolean intoMirror = false;
                for (final int raw : e.getRawSlots()) {
                    if (raw < topSize) {
                        if (current.mirrorIndex(raw) < 0) {
                            e.setCancelled(true);
                            return;
                        }
                        intoMirror = true;
                    }
                }

                if (intoMirror) {
                    current.dragIntoMirror(e);
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
                    if (current != null && e.getInventory().equals(current.getInventory())) {
                        current.unRegister();
                    }
                }
            }
        };
    }

    /** The screen this player has open, or null when it is not one of ours. */
    private static GUIBuilder builderFor(final HumanEntity clicker, final InventoryView view) {
        if (!(clicker instanceof Player)) {
            return null;
        }
        final GUIBuilder current = inventories.get(clicker.getUniqueId());
        if (current == null || current.getInventory() == null) {
            return null;
        }
        //by identity: openInventory only reuses a screen that is already one of ours, and adopts
        //it, so the inventory the player is looking at is always the one registered here
        return current.getInventory().equals(view.getTopInventory()) ? current : null;
    }

    /** Whether the player is looking at one of these screens. */
    public static boolean isOpen(final Player player) {
        return builderFor(player, player.getOpenInventory()) != null;
    }

    /** Closes every one of these screens, for a reload: their buttons hold hoppers that are about to be replaced. */
    public static void closeAll() {
        for (final Player player : Bukkit.getOnlinePlayers()) {
            if (isOpen(player)) {
                player.closeInventory();
            }
        }
        inventories.clear();
    }

    /**
     * Shows {@code backing()}'s slot {@code i} in {@code slots[i]}, and lets the player take from and
     * put into it as if it were that inventory.
     *
     * @param backing  looked up again on every click, and null when there is nothing there any more
     * @param onChange run after every change a player makes, so other screens can be redrawn
     */
    public void setMirroredSlots(final int[] slots, final Supplier<Inventory> backing, final Runnable onChange) {
        this.mirrored = slots.clone();
        this.backing = backing;
        this.onMirrorChange = onChange;
        this.syncMirror();
    }

    /** Redraws the mirrored slots from the real inventory, touching only those that differ. */
    public void syncMirror() {
        final Inventory real = this.backing == null ? null : this.backing.get();
        if (real == null || this.inv == null) {
            return;
        }
        for (int i = 0; i < this.mirrored.length && i < real.getSize(); i++) {
            final ItemStack now = real.getItem(i);
            if (!Objects.equals(now, this.inv.getItem(this.mirrored[i]))) {
                //a clone: getItem hands back a live view of the real slot
                this.inv.setItem(this.mirrored[i], now == null ? null : now.clone());
            }
        }
    }

    private int mirrorIndex(final int raw) {
        for (int i = 0; i < this.mirrored.length; i++) {
            if (this.mirrored[i] == raw) {
                return i;
            }
        }
        return -1;
    }

    private static ItemStack copyOf(final ItemStack item) {
        return item == null || item.getType() == Material.AIR || item.getAmount() <= 0 ? null : item.clone();
    }

    private static int limit(final ItemStack item, final Inventory real) {
        return Math.min(item.getMaxStackSize(), real.getMaxStackSize());
    }

    private static ItemStack withAmount(final ItemStack item, final int amount) {
        if (amount <= 0) {
            return null;
        }
        final ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }

    /**
     * What the click would have done to the slot, done to the real inventory instead - in the same
     * tick, so nothing can move in between. Clicks with no plain meaning here do nothing.
     */
    private void clickMirrored(final InventoryClickEvent e, final int index) {
        final Inventory real = this.backing.get();
        if (real == null) {
            return;
        }
        final Player p = (Player) e.getWhoClicked();
        ItemStack slot = copyOf(real.getItem(index));
        ItemStack cursor = copyOf(e.getCursor());

        switch (e.getClick()) {
            case LEFT:
                if (cursor == null) {
                    if (slot == null) {
                        return;
                    }
                    cursor = slot;
                    slot = null;
                } else if (slot == null) {
                    final int put = Math.min(cursor.getAmount(), limit(cursor, real));
                    slot = withAmount(cursor, put);
                    cursor = withAmount(cursor, cursor.getAmount() - put);
                } else if (slot.isSimilar(cursor)) {
                    final int moved = Math.min(limit(slot, real) - slot.getAmount(), cursor.getAmount());
                    if (moved <= 0) {
                        return;
                    }
                    slot.setAmount(slot.getAmount() + moved);
                    cursor = withAmount(cursor, cursor.getAmount() - moved);
                } else {
                    if (cursor.getAmount() > limit(cursor, real)) {
                        return;
                    }
                    final ItemStack swapped = slot;
                    slot = cursor;
                    cursor = swapped;
                }
                break;
            case RIGHT:
                if (cursor == null) {
                    if (slot == null) {
                        return;
                    }
                    final int taken = (slot.getAmount() + 1) / 2;
                    cursor = withAmount(slot, taken);
                    slot = withAmount(slot, slot.getAmount() - taken);
                } else if (slot == null) {
                    slot = withAmount(cursor, 1);
                    cursor = withAmount(cursor, cursor.getAmount() - 1);
                } else if (slot.isSimilar(cursor)) {
                    if (slot.getAmount() >= limit(slot, real)) {
                        return;
                    }
                    slot.setAmount(slot.getAmount() + 1);
                    cursor = withAmount(cursor, cursor.getAmount() - 1);
                } else {
                    if (cursor.getAmount() > limit(cursor, real)) {
                        return;
                    }
                    final ItemStack swapped = slot;
                    slot = cursor;
                    cursor = swapped;
                }
                break;
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
                if (slot == null) {
                    return;
                }
                final Map<Integer, ItemStack> left = p.getInventory().addItem(slot);
                slot = left.isEmpty() ? null : left.values().iterator().next();
                break;
            case NUMBER_KEY:
                final ItemStack held = copyOf(p.getInventory().getItem(e.getHotbarButton()));
                if (held != null && held.getAmount() > limit(held, real)) {
                    return;
                }
                p.getInventory().setItem(e.getHotbarButton(), slot);
                slot = held;
                break;
            default:
                return;
        }

        real.setItem(index, slot);
        p.setItemOnCursor(cursor);
        this.mirrorChanged(p);
    }

    /** A shift click from the player's own inventory, into the real inventory rather than the picture of it. */
    private void shiftIntoMirror(final InventoryClickEvent e) {
        final Inventory real = this.backing.get();
        final ItemStack moving = copyOf(e.getCurrentItem());
        if (real == null || moving == null) {
            return;
        }

        final Map<Integer, ItemStack> left = real.addItem(moving);
        e.setCurrentItem(left.isEmpty() ? null : left.values().iterator().next());
        this.mirrorChanged((Player) e.getWhoClicked());
    }

    /**
     * A drag over the mirrored slots. Left to run, since it may also cover the player's own slots,
     * but what it would paint into a mirrored slot is put into the real one, and whatever the real
     * one had no room for goes back onto the cursor.
     */
    private void dragIntoMirror(final InventoryDragEvent e) {
        final Inventory real = this.backing.get();
        if (real == null) {
            e.setCancelled(true);
            return;
        }

        final ItemStack dragged = e.getOldCursor();
        final Inventory top = e.getView().getTopInventory();
        int unplaced = 0;
        for (final Map.Entry<Integer, ItemStack> entry : e.getNewItems().entrySet()) {
            final int index = entry.getKey() < top.getSize() ? this.mirrorIndex(entry.getKey()) : -1;
            if (index < 0) {
                continue;
            }

            //what the drag adds on top of what the picture showed
            final ItemStack shown = copyOf(top.getItem(entry.getKey()));
            final int intended = entry.getValue().getAmount()
                    - (shown != null && shown.isSimilar(dragged) ? shown.getAmount() : 0);

            ItemStack slot = copyOf(real.getItem(index));
            int placed = 0;
            if (slot == null) {
                placed = Math.min(intended, limit(dragged, real));
                slot = withAmount(dragged, placed);
            } else if (slot.isSimilar(dragged)) {
                placed = Math.max(0, Math.min(intended, limit(slot, real) - slot.getAmount()));
                slot.setAmount(slot.getAmount() + placed);
            }
            if (placed > 0) {
                real.setItem(index, slot);
            }
            unplaced += Math.max(0, intended - placed);
        }

        if (unplaced > 0) {
            final int onCursor = e.getCursor() == null ? 0 : e.getCursor().getAmount();
            e.setCursor(withAmount(dragged, onCursor + unplaced));
        }
        //the drag still paints the picture after this; the next sync puts the real contents back
        this.mirrorChanged((Player) e.getWhoClicked());
    }

    private void mirrorChanged(final Player p) {
        if (this.onMirrorChange != null) {
            this.onMirrorChange.run();
        }
        //the cancelled click leaves the client showing its own guess until it is told otherwise
        Bukkit.getScheduler().runTask(RealHoppersAPI.getInstance().getPlugin(), p::updateInventory);
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
        final GUIBuilder showing = inventories.get(player.getUniqueId());
        final Inventory openTop = player.getOpenInventory().getTopInventory();
        //poured into the screen already open only when that screen is one of ours; any other
        //inventory of the same type - a real chest - would have been overwritten
        if (showing != null && showing != this && showing.getInventory() != null
                && showing.getInventory().equals(openTop) && openTop.getSize() == this.inv.getSize()) {
            openTop.setContents(this.inv.getContents());
            //adopted, so the identity checks keep matching the inventory on screen
            this.inv = openTop;
        } else if (!this.inv.equals(openTop)) {
            player.openInventory(this.inv);
        }
        this.register();
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
