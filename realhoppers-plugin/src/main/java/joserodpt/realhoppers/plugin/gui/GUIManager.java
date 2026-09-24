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
import joserodpt.realhoppers.api.hopper.RHopperAccess;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.hopper.trait.traits.RHFilterTrait;
import joserodpt.realhoppers.api.utils.GUIBuilder;
import joserodpt.realhoppers.api.utils.Items;
import joserodpt.realhoppers.api.utils.Pagination;
import joserodpt.realhoppers.api.utils.PlayerInput;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.plugin.RealHoppers;
import joserodpt.realhoppers.plugin.managers.HopperOwnership;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.MATERIAL;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.MONEY;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.PLAYER;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.TRAIT;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

/**
 * Every screen RealHoppers opens, built on {@link GUIBuilder} - a click runnable per slot, rather
 * than the switch over raw slot numbers the old HopperGUI carried.
 */
public class GUIManager {

    /** A full double chest, so the layout has room to grow into rather than be rearranged. */
    private static final int GUI_SIZE = 54;

    /**
     * Rows four and five, a column of padding either side: fourteen places for fourteen traits.
     *
     * <p>Exactly full, so a fifteenth trait has nowhere to go. The render loop stops at this
     * array's length and used to do so silently - that is how an eighth trait went missing when
     * this held seven - so {@link #openHopper} says so in the log now instead.</p>
     */
    private static final int[] TRAIT_SLOTS = {
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};

    /** The hopper's own five slots, centred on row two. */
    private static final int[] HOPPER_SLOTS = {11, 12, 13, 14, 15};

    /** The rest of row two, around them: experience and the balance on the left, close on the right. */
    private static final int XP_SLOT = 9;
    private static final int BALANCE_SLOT = 10;
    private static final int CLOSE_SLOT = 16;

    /** The filter screen puts its buttons on the bottom row instead. */
    private static final int FILTER_BACK_SLOT = 45;
    private static final int FILTER_PICK_SLOT = 48;
    private static final int FILTER_ADD_SLOT = 50;
    private static final int FILTER_CLOSE_SLOT = 53;
    /** In among where the entries would be, since it only shows when there are none. */
    private static final int FILTER_EMPTY_SLOT = 31;

    /**
     * The hopper screen's bottom row: who owns it, shown to everyone, and the owner's controls,
     * shown only to those who may manage it.
     */
    private static final int RENAME_SLOT = 47;
    private static final int ACCESS_SLOT = 49;
    private static final int WHITELIST_SLOT = 51;
    private static final int OWNER_SLOT = 53;

    /** The whitelist screen: four rows of players, and its buttons along the bottom. */
    private static final int[] WHITELIST_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43};
    private static final int WHITELIST_BACK_SLOT = 45;
    private static final int WHITELIST_PREVIOUS_SLOT = 48;
    private static final int WHITELIST_ADD_SLOT = 49;
    private static final int WHITELIST_NEXT_SLOT = 50;
    private static final int WHITELIST_CLOSE_SLOT = 53;
    private static final int WHITELIST_EMPTY_SLOT = 22;

    private final RealHoppers rh;

    /**
     * Which hopper each player has a screen of open, so a hopper whose balance moves can redraw the
     * screen showing it, and one that is broken can close it. The old GUI exposed a public static
     * map of itself for this.
     */
    private final Map<UUID, Screen> openScreens = new HashMap<>();

    /** One player's open screen: the hopper it is about, and whether it is the hopper screen itself. */
    private static final class Screen {
        private final RHopper hopper;
        private final GUIBuilder gui;
        private final boolean hopperScreen;

        private Screen(final RHopper hopper, final GUIBuilder gui, final boolean hopperScreen) {
            this.hopper = hopper;
            this.gui = gui;
            this.hopperScreen = hopperScreen;
        }
    }

    public GUIManager(final RealHoppers rh) {
        this.rh = rh;
    }

    /** Forgets whichever screen a player had open, once that very screen closes. */
    public Listener getListener() {
        return new Listener() {
            @EventHandler
            public void onClose(final InventoryCloseEvent e) {
                final Screen screen = openScreens.get(e.getPlayer().getUniqueId());
                //opening the next screen closes this one after the next is already recorded
                if (screen != null && screen.gui.getInventory().equals(e.getInventory())) {
                    openScreens.remove(e.getPlayer().getUniqueId());
                }
            }
        };
    }

    /** Whether this is still the hopper registered at its block, rather than one a reload or a break replaced. */
    private boolean isLive(final RHopper hopper) {
        return rh.getHopperManager().getHopper(hopper.getBlock()) == hopper;
    }

    /** The hopper's real inventory, or null once it is gone, so a screen left open cannot write into a dead block. */
    private Inventory liveInventory(final RHopper hopper) {
        return this.isLive(hopper) ? hopper.getInventory() : null;
    }

    /** A button's action, run only while the hopper is still live; a stale screen is closed instead. */
    private GUIBuilder.ClickRunnable guarded(final Player target, final RHopper hopper, final GUIBuilder.ClickRunnable action) {
        return e -> {
            if (!this.isLive(hopper)) {
                //a tick later: Bukkit doesn't support closing the inventory from inside its own click event
                Bukkit.getScheduler().runTask(rh.getPlugin(), () -> target.closeInventory());
                return;
            }
            action.run(e);
        };
    }

    private void record(final Player target, final RHopper hopper, final GUIBuilder gui, final boolean hopperScreen) {
        this.openScreens.put(target.getUniqueId(), new Screen(hopper, gui, hopperScreen));
    }

    /**
     * Redraws the hopper's five slots on every screen showing it. Run every tick, which is what
     * keeps them showing what vanilla hoppers feeding it in and out are doing, and after every
     * change a player makes through one of them.
     */
    public void syncOpenScreens() {
        for (final Map.Entry<UUID, Screen> entry : this.openScreens.entrySet()) {
            if (entry.getValue().hopperScreen) {
                entry.getValue().gui.syncMirror();
            }
        }
    }

    private void syncViewers(final RHopper hopper) {
        for (final Screen screen : this.openScreens.values()) {
            if (screen.hopperScreen && screen.hopper == hopper) {
                screen.gui.syncMirror();
            }
        }
    }

    private List<Player> viewersOf(final RHopper hopper, final boolean onlyHopperScreen) {
        //collected first: closing a screen removes its entry from the map being walked
        return this.openScreens.entrySet().stream()
                .filter(entry -> entry.getValue().hopper == hopper && (!onlyHopperScreen || entry.getValue().hopperScreen))
                .map(entry -> Bukkit.getPlayer(entry.getKey()))
                .filter(player -> player != null && player.isOnline())
                .collect(Collectors.toList());
    }

    /** Closes every screen about this hopper, before it is broken or blown up. */
    public void closeScreens(final RHopper hopper) {
        for (final Player player : this.viewersOf(hopper, false)) {
            player.closeInventory();
        }
        this.openScreens.values().removeIf(screen -> screen.hopper == hopper);
    }

    /** Closes every screen, picker and prompt RealHoppers has open, for a reload that replaces every hopper. */
    public void closeAll() {
        GUIBuilder.closeAll();
        MaterialPickerGUI.closeAll();
        PlayerInput.cancelAll();
        this.openScreens.clear();
    }

    /**
     * Redraws the hopper screen for anybody looking at this hopper, and closes it on anybody it has
     * just been made private to.
     */
    public void refresh(final RHopper hopper) {
        //a hopper on its way out - paid out and zeroed as it is broken - has nothing to redraw
        if (!this.isLive(hopper)) {
            return;
        }

        for (final Player player : this.viewersOf(hopper, true)) {
            if (hopper.canAccess(player)) {
                openHopper(player, hopper);
            } else {
                player.closeInventory();
            }
        }
    }

    public void openHopper(final Player target, final RHopper hopper) {
        final GUIBuilder inventory = new GUIBuilder(TranslatableLine.GUI_TITLE
                .with(NAME, hopper.getName()).get(), GUI_SIZE, target.getUniqueId());

        //the hopper's own five slots, along the top, with its contents in them. They used to be a
        //button that closed this screen and opened the vanilla hopper one; there is one screen now.
        //Clicks on them act on the hopper itself, and every screen of it is redrawn after each one
        inventory.setMirroredSlots(HOPPER_SLOTS, () -> this.liveInventory(hopper), () -> this.syncViewers(hopper));

        //and the traits, which were a second screen of their own
        final RHopperTrait[] traits = RHopperTrait.values();
        if (traits.length > TRAIT_SLOTS.length) {
            //the screen is full. Said out loud, because the alternative is a trait that exists,
            //loads, works and cannot be switched on by anybody looking for it
            rh.getLogger().warning("There are " + traits.length + " traits and only " + TRAIT_SLOTS.length
                    + " places for them, so " + (traits.length - TRAIT_SLOTS.length)
                    + " will not appear in the hopper screen.");
        }

        for (int i = 0; i < traits.length && i < TRAIT_SLOTS.length; i++) {
            final RHopperTrait trait = traits[i];
            inventory.addItem(this.guarded(target, hopper, e -> {
                //shift click edits a trait that has something to edit, right click walks its tier
                //up, plain click switches it on or off
                if (e.getClick().isShiftClick() && trait == RHopperTrait.FILTER && hopper.hasTrait(trait)) {
                    openLater(target, () -> openFilter(target, hopper));
                } else if (e.getClick().isRightClick() && hopper.hasTrait(trait)) {
                    raiseTier(target, hopper, trait);
                } else {
                    toggle(target, hopper, trait);
                }
            }), traitIcon(hopper, trait), TRAIT_SLOTS[i]);
        }

        inventory.addItem(this.guarded(target, hopper, e -> collect(target, hopper, e.getClick())),
                Items.createItem(Material.HOPPER, 1, TranslatableLine.GUI_HOPPER_NAME.get(), hopper.getHopperDescription()), BALANCE_SLOT);

        //only on a hopper that gathers any, so the panel of one that does not is unchanged
        if (hopper.hasXpCapabilities()) {
            inventory.addItem(this.guarded(target, hopper, e -> collectXp(target, hopper)),
                    Items.createItem(Material.EXPERIENCE_BOTTLE, 1,
                            TranslatableLine.GUI_XP_NAME
                                    .with(VALUE, String.valueOf(hopper.getXp())).get(),
                            RHLanguage.file().getStringList("GUI.Items.Xp.Description")), XP_SLOT);
        }

        inventory.addItem(e -> target.closeInventory(),
                Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.GUI_CLOSE_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Close.Description")), CLOSE_SLOT);

        this.addOwnershipButtons(inventory, target, hopper);

        inventory.openInventory(target);
        this.record(target, hopper, inventory, true);
    }

    private void addOwnershipButtons(final GUIBuilder inventory, final Player target, final RHopper hopper) {
        final List<String> ownerLore = new ArrayList<>();
        for (final String line : RHLanguage.file().getStringList("GUI.Items.Owner.Description")) {
            ownerLore.add(line
                    .replace("%value%", hopper.getAccess().getDisplayName())
                    .replace("%whitelisted%", String.valueOf(hopper.getWhitelist().size())));
        }
        inventory.setItem(head(hopper.getOwner(), TranslatableLine.GUI_OWNER_NAME
                .with(PLAYER, hopper.getOwnerDisplayName()).get(), ownerLore), OWNER_SLOT);

        //only the owner and admins see the controls; everyone else just sees whose it is
        if (!hopper.canManage(target)) {
            return;
        }

        inventory.addItem(this.guarded(target, hopper, e -> this.rename(target, hopper)),
                Items.createItem(Material.NAME_TAG, 1, TranslatableLine.GUI_RENAME_NAME
                                .with(NAME, hopper.getName()).get(),
                        RHLanguage.file().getStringList("GUI.Items.Rename.Description")), RENAME_SLOT);

        final boolean isPublic = hopper.getAccess() == RHopperAccess.PUBLIC;
        //setAccess fires the state change event, which redraws this screen through refresh
        inventory.addItem(this.guarded(target, hopper, e -> HopperOwnership.setAccess(target, hopper, hopper.getAccess().next())),
                Items.createItem(isPublic ? Material.LIME_DYE : Material.RED_DYE, 1,
                        TranslatableLine.GUI_ACCESS_NAME
                                .with(VALUE, hopper.getAccess().getDisplayName()).get(),
                        RHLanguage.file().getStringList(isPublic
                                ? "GUI.Items.Access.Public-Description"
                                : "GUI.Items.Access.Private-Description")), ACCESS_SLOT);

        inventory.addItem(this.guarded(target, hopper, e -> openLater(target, () -> openWhitelist(target, hopper, 0))),
                Items.createItem(Material.BOOK, 1, TranslatableLine.GUI_WHITELIST_NAME
                                .with(VALUE, String.valueOf(hopper.getWhitelist().size())).get(),
                        RHLanguage.file().getStringList("GUI.Items.Whitelist.Description")), WHITELIST_SLOT);
    }

    /** Asks for the new name in chat, then brings the hopper screen back either way. */
    private void rename(final Player target, final RHopper hopper) {
        if (!HopperOwnership.checkManage(target, hopper)) {
            return;
        }
        //colours are kept, so a hopper can be named in them
        new PlayerInput(false, target, RHLanguage.file().getStringList("Hoppers.Name.Prompt"),
                input -> {
                    //the hopper may have been broken or reloaded away while they typed
                    if (this.isLive(hopper)) {
                        HopperOwnership.rename(target, hopper, input);
                    }
                    this.reopen(target, hopper);
                },
                input -> this.reopen(target, hopper));
    }

    /**
     * Brings the hopper screen back after a chat prompt, unless the hopper went while the player
     * was typing or they may no longer open it.
     */
    private void reopen(final Player target, final RHopper hopper) {
        if (target.isOnline() && this.isLive(hopper) && hopper.canAccess(target)) {
            openHopper(target, hopper);
        }
    }

    /** The players allowed into a private hopper. Clicking one takes them off; the owner and admins only. */
    public void openWhitelist(final Player target, final RHopper hopper, final int page) {
        if (!this.isLive(hopper) || !hopper.canManage(target)) {
            this.reopen(target, hopper);
            return;
        }

        final GUIBuilder inventory = new GUIBuilder(TranslatableLine.GUI_WHITELIST_TITLE
                .with(NAME, hopper.getName()).get(), GUI_SIZE, target.getUniqueId());

        final Pagination<Map.Entry<UUID, String>> pages = new Pagination<>(WHITELIST_SLOTS.length,
                new ArrayList<>(hopper.getWhitelist().entrySet()));
        //a removal can empty the last page out from under the player
        final int shown = pages.exists(page) ? page : Math.max(0, pages.totalPages() - 1);

        if (pages.isEmpty()) {
            inventory.setItem(Items.createItem(Material.BARRIER, 1, TranslatableLine.GUI_WHITELIST_EMPTY_NAME.get(),
                    RHLanguage.file().getStringList("GUI.Items.Whitelist.Empty.Description")), WHITELIST_EMPTY_SLOT);
        } else {
            final List<Map.Entry<UUID, String>> entries = pages.getPage(shown);
            for (int i = 0; i < entries.size(); i++) {
                final UUID uuid = entries.get(i).getKey();
                final String name = entries.get(i).getValue() == null ? uuid.toString() : entries.get(i).getValue();
                inventory.addItem(this.guarded(target, hopper, e -> {
                    HopperOwnership.removeFromWhitelist(target, hopper, uuid, name);
                    openWhitelist(target, hopper, shown);
                }), head(uuid, TranslatableLine.GUI_WHITELIST_ENTRY_NAME
                                .with(PLAYER, name).get(),
                        RHLanguage.file().getStringList("GUI.Items.Whitelist.Entry.Description")), WHITELIST_SLOTS[i]);
            }
        }

        if (pages.exists(shown - 1)) {
            inventory.addItem(e -> openWhitelist(target, hopper, shown - 1),
                    Items.createItem(Material.YELLOW_STAINED_GLASS, 1, TranslatableLine.GUI_PREVIOUS_PAGE_NAME.get(),
                            RHLanguage.file().getStringList("GUI.Items.Picker.Back-Description")), WHITELIST_PREVIOUS_SLOT);
        }
        if (pages.exists(shown + 1)) {
            inventory.addItem(e -> openWhitelist(target, hopper, shown + 1),
                    Items.createItem(Material.GREEN_STAINED_GLASS, 1, TranslatableLine.GUI_NEXT_PAGE_NAME.get(),
                            RHLanguage.file().getStringList("GUI.Items.Picker.Next-Description")), WHITELIST_NEXT_SLOT);
        }

        inventory.addItem(this.guarded(target, hopper, e -> new PlayerInput(true, target, RHLanguage.file().getStringList("Hoppers.Whitelist.Prompt"),
                        input -> {
                            if (this.isLive(hopper)) {
                                HopperOwnership.addToWhitelist(target, hopper, input);
                            }
                            openWhitelist(target, hopper, shown);
                        },
                        input -> openWhitelist(target, hopper, shown))),
                Items.createItem(Material.EMERALD, 1, TranslatableLine.GUI_WHITELIST_ADD_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Whitelist.Add.Description")), WHITELIST_ADD_SLOT);

        inventory.addItem(e -> openLater(target, () -> this.reopen(target, hopper)),
                Items.createItem(Material.RED_BED, 1, TranslatableLine.GUI_BACK_NAME.get()), WHITELIST_BACK_SLOT);

        inventory.addItem(e -> target.closeInventory(),
                Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.GUI_CLOSE_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Close.Description")), WHITELIST_CLOSE_SLOT);

        inventory.openInventory(target);
        //not the hopper screen, so nothing here is redrawn by refresh, but it is closed with the hopper
        this.record(target, hopper, inventory, false);
    }

    /** A player's head, or a plain one for a hopper nobody owns. */
    private ItemStack head(final UUID owner, final String name, final List<String> lore) {
        final ItemStack head = Items.createItem(Material.PLAYER_HEAD, 1, name, lore);
        if (owner != null && head.getItemMeta() instanceof SkullMeta) {
            final SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            head.setItemMeta(meta);
        }
        return head;
    }

    /**
     * Pays out the hopper's balance: all of it on a shift-left-click, half otherwise. Traits with no
     * economy behind them have nothing banked, so the click does nothing.
     */
    private void collect(final Player target, final RHopper hopper, final ClickType click) {
        if (hopper.getBalance() <= 0) {
            return;
        }
        //being let into a public hopper is not a claim on its money
        if (!HopperOwnership.checkManage(target, hopper)) {
            return;
        }

        if (rh.getEconomy() == null) {
            TranslatableLine.SYSTEM_VAULT_MISSING.send(target);
            target.closeInventory();
            return;
        }

        final double amount = click == ClickType.SHIFT_LEFT ? hopper.getBalance() : hopper.getBalance() / 2;
        final EconomyResponse paid = rh.getEconomy().depositPlayer(target, amount);
        if (paid == null || !paid.transactionSuccess()) {
            //nothing was paid, so nothing comes off the balance
            TranslatableLine.SYSTEM_ERROR_OCCURRED.send(target);
            return;
        }
        TranslatableLine.HOPPER_BALANCE_COLLECTED
                .with(MONEY, Text.formatNumber(amount)).send(target);

        //setBalance fires the state change event, which brings this screen back through refresh
        hopper.setBalance(hopper.getBalance() - amount);
        //the money is already in their account, so the lower balance is written now rather than at
        //the next flush, which a crash could skip
        rh.getDatabaseManager().saveNow(hopper);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    /**
     * Hands the hopper's gathered experience to the player. Unlike the balance this is not paid
     * through Vault - it goes straight back as levels.
     */
    private void collectXp(final Player target, final RHopper hopper) {
        if (hopper.getXp() <= 0) {
            return;
        }
        if (!HopperOwnership.checkManage(target, hopper)) {
            return;
        }

        final int gathered = hopper.getXp();
        target.giveExp(gathered);
        //setXp fires the state change event, which brings this screen back through refresh
        hopper.setXp(0);
        rh.getDatabaseManager().saveNow(hopper);
        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        TranslatableLine.HOPPER_XP_COLLECTED
                .with(VALUE, String.valueOf(gathered)).send(target);
    }

    /**
     * The list a FILTER hopper keeps: what it is allowed to store, one icon each.
     *
     * <p>Materials are added by holding the item and clicking, rather than through a picker of
     * every material in the game - a player deciding what a hopper should keep is nearly always
     * holding the thing already.</p>
     */
    public void openFilter(final Player target, final RHopper hopper) {
        if (!this.isLive(hopper)) {
            return;
        }
        final RHFilterTrait filter = hopper.getTrait(RHopperTrait.FILTER, RHFilterTrait.class);
        if (filter == null) {
            openHopper(target, hopper);
            return;
        }

        final GUIBuilder inventory = new GUIBuilder(TranslatableLine.GUI_FILTER_TITLE.get(),
                GUI_SIZE, target.getUniqueId());

        final List<Material> listed = new ArrayList<>(filter.getMaterials());
        for (int i = 0; i < listed.size() && i < TRAIT_SLOTS.length; i++) {
            final Material material = listed.get(i);
            inventory.addItem(this.guarded(target, hopper, e -> {
                filter.remove(material);
                TranslatableLine.FILTER_REMOVED
                        .with(MATERIAL, Text.beautifyMaterialName(material)).send(target);
                openFilter(target, hopper);
            }), Items.createItem(material, 1, "&f" + Text.beautifyMaterialName(material),
                    RHLanguage.file().getStringList("GUI.Items.Filter.Entry-Description")), TRAIT_SLOTS[i]);
        }

        //an empty list keeps everything, which is worth saying on the screen that looks empty
        if (listed.isEmpty()) {
            inventory.setItem(Items.createItem(Material.BARRIER, 1, TranslatableLine.GUI_FILTER_EMPTY_NAME.get(),
                    RHLanguage.file().getStringList("GUI.Items.Filter.Empty-Description")), FILTER_EMPTY_SLOT);
        }

        inventory.addItem(this.guarded(target, hopper, e -> openLater(target, () -> {
            //the picker from RealMines: every material, paged, with a chat search
            final MaterialPickerGUI picker = new MaterialPickerGUI(target, TranslatableLine.GUI_PICKER_TITLE.get(),
                    MaterialPickerGUI.MaterialLists.ONLY_ITEMS, material -> {
                if (material != null && this.isLive(hopper) && filter.add(material)) {
                    TranslatableLine.FILTER_ADDED
                            .with(MATERIAL, Text.beautifyMaterialName(material)).send(target);
                }
                openFilter(target, hopper);
            });
            picker.openInventory(target);
        })), Items.createItem(Material.COMPASS, 1, TranslatableLine.GUI_FILTER_PICK_NAME.get(),
                RHLanguage.file().getStringList("GUI.Items.Filter.Pick-Description")), FILTER_PICK_SLOT);

        //still the quicker way when the thing is already in hand
        inventory.addItem(this.guarded(target, hopper, e -> addHeldToFilter(target, hopper, filter)),
                Items.createItem(Material.NAME_TAG, 1, TranslatableLine.GUI_FILTER_ADD_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Filter.Add-Description")), FILTER_ADD_SLOT);

        inventory.addItem(e -> openLater(target, () -> this.reopen(target, hopper)),
                Items.createItem(Material.RED_BED, 1, TranslatableLine.GUI_BACK_NAME.get()), FILTER_BACK_SLOT);

        inventory.addItem(e -> target.closeInventory(),
                Items.createItem(Material.OAK_DOOR, 1, TranslatableLine.GUI_CLOSE_NAME.get(),
                        RHLanguage.file().getStringList("GUI.Items.Close.Description")), FILTER_CLOSE_SLOT);

        inventory.openInventory(target);
        //not the hopper screen, so nothing here is redrawn by refresh, but it is closed with the hopper
        this.record(target, hopper, inventory, false);
    }

    private void addHeldToFilter(final Player target, final RHopper hopper, final RHFilterTrait filter) {
        final ItemStack held = target.getInventory().getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            TranslatableLine.FILTER_NOTHING_HELD.send(target);
            return;
        }

        if (!filter.add(held.getType())) {
            TranslatableLine.FILTER_ALREADY_LISTED
                    .with(MATERIAL, Text.beautifyMaterialName(held.getType())).send(target);
            return;
        }

        TranslatableLine.FILTER_ADDED
                .with(MATERIAL, Text.beautifyMaterialName(held.getType())).send(target);
        openFilter(target, hopper);
    }

    /**
     * Closes what is open and builds the next screen a couple of ticks later.
     *
     * <p>GUIBuilder pours a new screen into the one already open when that one is also ours, which is
     * what keeps this one from flickering as a balance changes - but it also keeps the old title.
     * Closing first is how RealMines moves between its own screens.</p>
     */
    private void openLater(final Player target, final Runnable open) {
        target.closeInventory();
        Bukkit.getScheduler().scheduleSyncDelayedTask(rh.getPlugin(), open, 2);
    }

    private ItemStack traitIcon(final RHopper hopper, final RHopperTrait trait) {
        //what the trait does comes first, before whether this hopper has it
        final List<String> lore = new ArrayList<>(trait.getDescription());
        if (!lore.isEmpty()) {
            lore.add("");
        }

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

        if (active && trait == RHopperTrait.FILTER) {
            lore.addAll(RHLanguage.file().getStringList("GUI.Items.Trait.Filter-Description"));
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
                    .with(TRAIT, trait.getName()).send(target);
            return;
        }

        final int next = hopper.getTraitTier(trait) + 1;
        if (next > trait.getMaxTier()) {
            TranslatableLine.TRAIT_TIER_MAX
                    .with(TRAIT, trait.getName()).send(target);
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
                        .with(MONEY, Text.formatNumber(price)).send(target);
                return;
            }
            //taken before the tier is set, so a refused withdrawal cannot hand out the upgrade
            final EconomyResponse charged = rh.getEconomy().withdrawPlayer(target, price);
            if (charged == null || !charged.transactionSuccess()) {
                TranslatableLine.TRAIT_TIER_TOO_EXPENSIVE
                        .with(MONEY, Text.formatNumber(price)).send(target);
                return;
            }
        }

        final int set = hopper.setTraitTier(trait, next);
        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        TranslatableLine.TRAIT_TIER_UPGRADED
                .with(TRAIT, trait.getName())
                .with(VALUE, String.valueOf(set)).send(target);
        openHopper(target, hopper);
    }

    private void toggle(final Player target, final RHopper hopper, final RHopperTrait trait) {
        if (hopper.removeTrait(trait)) {
            TranslatableLine.TRAIT_REMOVED
                    .with(TRAIT, trait.getName()).send(target);
            openHopper(target, hopper);
            return;
        }

        final RHopperTraitBase built = trait.build(hopper);
        if (built == null) {
            TranslatableLine.TRAIT_UNAVAILABLE
                    .with(TRAIT, trait.getName()).send(target);
            return;
        }

        hopper.setTrait(trait, built);
        TranslatableLine.TRAIT_ADDED
                .with(TRAIT, trait.getName()).send(target);

        if (trait.requiresLink() && !hopper.hasLink()) {
            TranslatableLine.TRAIT_NEEDS_LINK
                    .with(TRAIT, trait.getName()).send(target);
        }
        openHopper(target, hopper);
    }
}
