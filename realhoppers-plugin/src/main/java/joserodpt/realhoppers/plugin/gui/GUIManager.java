package joserodpt.realhoppers.plugin.gui;

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

import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.utils.GUIBuilder;
import joserodpt.realhoppers.api.utils.Items;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.plugin.RealHoppers;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every screen RealHoppers opens, built on {@link GUIBuilder} - a click runnable per slot, rather
 * than the switch over raw slot numbers the old HopperGUI carried.
 */
public class GUIManager {

    /** Where the trait icons go, left to right. Seven traits, one row, no pagination needed. */
    private static final int[] TRAIT_SLOTS = {10, 11, 12, 13, 14, 15, 16};

    private final RealHoppers rh;

    /**
     * Which hopper each player currently has open, so a hopper whose balance moves can redraw the
     * screen showing it. The old GUI exposed a public static map of itself for this.
     */
    private final Map<UUID, RHopper> openHoppers = new HashMap<>();

    public GUIManager(final RealHoppers rh) {
        this.rh = rh;
    }

    /**
     * Forgets whichever screen a player had open. Bukkit only ever has one inventory open per
     * player, so any close means the hopper screen is gone.
     */
    public Listener getListener() {
        return new Listener() {
            @EventHandler
            public void onClose(final InventoryCloseEvent e) {
                openHoppers.remove(e.getPlayer().getUniqueId());
            }
        };
    }

    /** Redraws the hopper screen for anybody looking at this hopper. */
    public void refresh(final RHopper hopper) {
        openHoppers.entrySet().stream()
                .filter(entry -> entry.getValue() == hopper)
                .map(entry -> Bukkit.getPlayer(entry.getKey()))
                .filter(player -> player != null && player.isOnline())
                .forEach(player -> openHopper(player, hopper));
    }

    public void openHopper(final Player target, final RHopper hopper) {
        final GUIBuilder inventory = new GUIBuilder(TranslatableLine.GUI_TITLE.get(), 27, target.getUniqueId());

        inventory.addItem(e -> {
            //the vanilla hopper inventory, which is a different inventory: closing first keeps the
            //two from fighting over which one is open
            target.closeInventory();
            hopper.openInventory(target);
        }, Items.createItem(Material.CHEST, 1, TranslatableLine.GUI_HOPPER_INVENTORY_NAME.get(),
                RHLanguage.file().getStringList("GUI.Items.Hopper-Inventory.Description")), 11);

        inventory.addItem(e -> collect(target, hopper, e.getClick()),
                Items.createItem(Material.HOPPER, 1, TranslatableLine.GUI_HOPPER_NAME.get(), hopper.getHopperDescription()), 13);

        inventory.addItem(e -> openLater(target, () -> openTraits(target, hopper)),
                Items.createItem(Material.BOOK, 1, TranslatableLine.GUI_TRAITS_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Traits.Description")), 15);

        inventory.addItem(e -> target.closeInventory(),
                Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.GUI_CLOSE_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Close.Description")), 22);

        inventory.openInventory(target);
        this.openHoppers.put(target.getUniqueId(), hopper);
    }

    /**
     * Pays out the hopper's balance: all of it on a shift-left-click, half otherwise. Traits with no
     * economy behind them have nothing banked, so the click does nothing.
     */
    private void collect(final Player target, final RHopper hopper, final ClickType click) {
        if (hopper.getBalance() <= 0) {
            return;
        }

        if (rh.getEconomy() == null) {
            TranslatableLine.SYSTEM_VAULT_MISSING.send(target);
            target.closeInventory();
            return;
        }

        final double amount = click == ClickType.SHIFT_LEFT ? hopper.getBalance() : hopper.getBalance() / 2;
        rh.getEconomy().depositPlayer(target, amount);
        TranslatableLine.HOPPER_BALANCE_COLLECTED
                .setV1(TranslatableLine.ReplacableVar.MONEY.eq(Text.formatNumber(amount))).send(target);

        //setBalance fires the state change event, which brings this screen back through refresh
        hopper.setBalance(hopper.getBalance() - amount);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    /**
     * The trait screen: one icon per constant, click to add or remove. Traits were command-only
     * before this, and there was no way at all to take one off.
     */
    public void openTraits(final Player target, final RHopper hopper) {
        final GUIBuilder inventory = new GUIBuilder(TranslatableLine.GUI_TRAITS_TITLE.get(), 27, target.getUniqueId());

        final RHopperTrait[] traits = RHopperTrait.values();
        for (int i = 0; i < traits.length && i < TRAIT_SLOTS.length; i++) {
            final RHopperTrait trait = traits[i];
            inventory.addItem(e -> {
                //left click switches the trait on or off, right click walks its tier up
                if (e.getClick().isRightClick() && hopper.hasTrait(trait)) {
                    raiseTier(target, hopper, trait);
                } else {
                    toggle(target, hopper, trait);
                }
            }, traitIcon(hopper, trait), TRAIT_SLOTS[i]);
        }

        inventory.addItem(e -> openLater(target, () -> openHopper(target, hopper)),
                Items.createItem(Material.RED_BED, 1, TranslatableLine.GUI_BACK_NAME.get()), 18);

        inventory.addItem(e -> target.closeInventory(),
                Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.GUI_CLOSE_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Close.Description")), 26);

        inventory.openInventory(target);
        //the trait screen is not the hopper screen, so nothing here should be redrawn by refresh
        this.openHoppers.remove(target.getUniqueId());
    }

    /**
     * Closes what is open and builds the next screen a couple of ticks later.
     *
     * <p>Both screens are 27-slot chests, and GUIBuilder pours a new inventory of the same type
     * into the one already open rather than opening a second - which keeps the old title on screen.
     * Closing first is how RealMines moves between its own screens.</p>
     */
    private void openLater(final Player target, final Runnable open) {
        target.closeInventory();
        Bukkit.getScheduler().scheduleSyncDelayedTask(rh.getPlugin(), open, 2);
    }

    private ItemStack traitIcon(final RHopper hopper, final RHopperTrait trait) {
        final List<String> lore = new ArrayList<>();

        if (trait.build(hopper) == null) {
            lore.addAll(RHLanguage.file().getStringList("GUI.Items.Trait.Unavailable-Description"));
            return Items.createItem(trait.getIcon(), 1, trait.getName(), lore);
        }

        final boolean active = hopper.hasTrait(trait);
        lore.addAll(RHLanguage.file().getStringList(active
                ? "GUI.Items.Trait.Active-Description"
                : "GUI.Items.Trait.Inactive-Description"));

        //only worth showing on a trait that is on and has something to multiply
        if (active && trait.isScalable()) {
            final int current = hopper.getTraitTier(trait);
            final String tier = current + "&7/&b" + trait.getMaxTier();
            final boolean maxed = current >= trait.getMaxTier();
            final String price = maxed ? "" : Text.formatNumber(trait.getTierPrice(current + 1));

            RHLanguage.file().getStringList(maxed
                            ? "GUI.Items.Trait.Tier-Max-Description"
                            : "GUI.Items.Trait.Tier-Description")
                    .forEach(line -> lore.add(line.replace("%value%", tier).replace("%money%", price)));
        }

        //a trait that follows the hopper's link can be switched on with no link there; it just has
        //nowhere to go until one is made, and the icon says so
        if (trait.requiresLink() && !hopper.hasLink()) {
            lore.addAll(RHLanguage.file().getStringList("GUI.Items.Trait.Needs-Link-Description"));
        }

        return active
                ? Items.createItemLoreEnchanted(trait.getIcon(), 1, trait.getName(), lore)
                : Items.createItem(trait.getIcon(), 1, trait.getName(), lore);
    }

    /**
     * Buys the next tier of a trait for the player.
     *
     * <p>Only ever upwards. An earlier version cycled back to 1 at the top, which was fine while a
     * tier was free and is not once one has been paid for.</p>
     */
    private void raiseTier(final Player target, final RHopper hopper, final RHopperTrait trait) {
        if (!trait.isScalable()) {
            TranslatableLine.TRAIT_NOT_SCALABLE
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(target);
            return;
        }

        final int next = hopper.getTraitTier(trait) + 1;
        if (next > trait.getMaxTier()) {
            TranslatableLine.TRAIT_TIER_MAX
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(target);
            return;
        }

        final double price = trait.getTierPrice(next);
        if (price > 0) {
            if (rh.getEconomy() == null) {
                //a priced upgrade with no economy to charge it to would otherwise be free
                TranslatableLine.SYSTEM_VAULT_MISSING.send(target);
                return;
            }
            if (!rh.getEconomy().has(target, price)) {
                TranslatableLine.TRAIT_TIER_TOO_EXPENSIVE
                        .setV1(TranslatableLine.ReplacableVar.MONEY.eq(Text.formatNumber(price))).send(target);
                return;
            }
            //taken before the tier is set, so a refused withdrawal cannot hand out the upgrade
            rh.getEconomy().withdrawPlayer(target, price);
        }

        final int set = hopper.setTraitTier(trait, next);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        TranslatableLine.TRAIT_TIER_UPGRADED
                .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName()))
                .setV2(TranslatableLine.ReplacableVar.VALUE.eq(String.valueOf(set))).send(target);
        openTraits(target, hopper);
    }

    private void toggle(final Player target, final RHopper hopper, final RHopperTrait trait) {
        if (hopper.removeTrait(trait)) {
            TranslatableLine.TRAIT_REMOVED
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(target);
            openTraits(target, hopper);
            return;
        }

        final RHopperTraitBase built = trait.build(hopper);
        if (built == null) {
            TranslatableLine.TRAIT_UNAVAILABLE
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(target);
            return;
        }

        hopper.setTrait(trait, built);
        TranslatableLine.TRAIT_ADDED
                .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(target);

        if (trait.requiresLink() && !hopper.hasLink()) {
            TranslatableLine.TRAIT_NEEDS_LINK
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(target);
        }
        openTraits(target, hopper);
    }
}
