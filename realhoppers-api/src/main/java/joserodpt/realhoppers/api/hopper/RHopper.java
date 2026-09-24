package joserodpt.realhoppers.api.hopper;

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
import joserodpt.realhoppers.api.hopper.events.RHopperStateChangeEvent;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSellTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSmeltTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHFilterTrait;
import joserodpt.realhoppers.api.utils.LocationUtil;
import joserodpt.realhoppers.api.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.MONEY;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.PLAYER;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.TRAIT;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

public class RHopper {

    public enum Data {ALL, BALANCE, TRAITS, LINK, XP, OWNERSHIP }

    /** Lets an admin open, break, link and manage any hopper, private or not. */
    public static final String ADMIN_PERMISSION = "realhoppers.admin";

    private Block block;
    private double balance;

    /**
     * Experience gathered by XP_COLLECT, waiting to be taken out.
     *
     * <p>Kept apart from the balance on purpose: the balance is money and is paid through Vault,
     * this is levels and is handed back to the player directly.</p>
     */
    private int xp;
    private Map<RHopperTrait, RHopperTraitBase> traitMap = new HashMap<>();

    /**
     * The one other hopper this one points at. Every trait that needs a second hopper - TELEPORT
     * and ITEM_TRANSF - uses this one, rather than each carrying a destination of its own: a hopper
     * has one link, and whatever is on it follows that link.
     */
    private RHopper link;

    /** The link as it was read off disk, until {@link #loadLink()} can turn it into a hopper. */
    private String linkLocation;

    /** What the hopper is called on its screen and in /rh list. */
    private String name;

    /** Who placed it. Null only for a hopper nobody placed, which is treated as public. */
    private UUID owner;

    /** The owner's name when they were last seen, so it can be shown without a lookup. */
    private String ownerName;

    private RHopperAccess access = RHopperAccess.PUBLIC;

    /** Players who may use a private hopper as its owner can, by UUID, with their last known name. */
    private final Map<UUID, String> whitelist = new LinkedHashMap<>();

    private long createdAt;

    /**
     * A hopper being read back from the database. Everything else is put back through the
     * {@code restore} methods, which neither fire events nor queue writes.
     */
    public RHopper(Block b, boolean save) {
        this.block = b;
        this.name = getDefaultName();
        this.createdAt = System.currentTimeMillis();

        if (save)
            this.saveData(Data.ALL);
    }

    /** A hopper just placed by a player, who owns it from then on. */
    public RHopper(Block b, Player owner) {
        this.block = b;
        this.name = getDefaultName();
        this.access = RHopperAccess.getDefault();
        this.createdAt = System.currentTimeMillis();
        if (owner != null) {
            this.owner = owner.getUniqueId();
            this.ownerName = owner.getName();
        }
        this.saveData(Data.ALL);
    }

    public static String getDefaultName() {
        return RHConfig.file().getString("RealHoppers.Hoppers.Default-Name", "RealHopper");
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
        Bukkit.getPluginManager().callEvent(new RHopperStateChangeEvent(this));
        this.saveData(Data.OWNERSHIP);
    }

    public void restoreName(final String name) {
        this.name = name == null || name.isEmpty() ? getDefaultName() : name;
    }

    public UUID getOwner() {
        return this.owner;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public boolean hasOwner() {
        return this.owner != null;
    }

    public void setOwner(final OfflinePlayer owner) {
        this.owner = owner == null ? null : owner.getUniqueId();
        this.ownerName = owner == null ? null : owner.getName();
        //the new owner has no business being on their own whitelist
        if (this.owner != null) {
            this.whitelist.remove(this.owner);
        }
        Bukkit.getPluginManager().callEvent(new RHopperStateChangeEvent(this));
        this.saveData(Data.OWNERSHIP);
    }

    public void restoreOwner(final UUID owner, final String ownerName) {
        this.owner = owner;
        this.ownerName = ownerName;
    }

    public RHopperAccess getAccess() {
        return this.access;
    }

    public void setAccess(final RHopperAccess access) {
        this.access = access;
        Bukkit.getPluginManager().callEvent(new RHopperStateChangeEvent(this));
        this.saveData(Data.OWNERSHIP);
    }

    public void restoreAccess(final RHopperAccess access) {
        this.access = access;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public void restoreCreatedAt(final long createdAt) {
        this.createdAt = createdAt;
    }

    /** The whitelist, UUID to last known name, in the order players were added. Read only. */
    public Map<UUID, String> getWhitelist() {
        return Collections.unmodifiableMap(this.whitelist);
    }

    public boolean isWhitelisted(final UUID uuid) {
        return this.whitelist.containsKey(uuid);
    }

    /** @return false when the player was already on it */
    public boolean addToWhitelist(final OfflinePlayer player) {
        if (this.whitelist.containsKey(player.getUniqueId())) {
            return false;
        }
        this.whitelist.put(player.getUniqueId(), player.getName());
        Bukkit.getPluginManager().callEvent(new RHopperStateChangeEvent(this));
        this.saveData(Data.OWNERSHIP);
        return true;
    }

    /** @return false when the player was not on it */
    public boolean removeFromWhitelist(final UUID uuid) {
        if (this.whitelist.remove(uuid) == null) {
            return false;
        }
        Bukkit.getPluginManager().callEvent(new RHopperStateChangeEvent(this));
        this.saveData(Data.OWNERSHIP);
        return true;
    }

    public void restoreWhitelisted(final UUID uuid, final String name) {
        this.whitelist.put(uuid, name);
    }

    public boolean isOwner(final Player p) {
        return this.owner != null && this.owner.equals(p.getUniqueId());
    }

    /**
     * Whether a player may open this hopper's screen - and, on a private hopper, break it or link
     * it. The one place that is decided.
     */
    public boolean canAccess(final Player p) {
        return this.access == RHopperAccess.PUBLIC
                || this.owner == null
                || this.isOwner(p)
                || this.isWhitelisted(p.getUniqueId())
                || p.hasPermission(ADMIN_PERMISSION);
    }

    /** Whether a player may rename this hopper, change its access and edit its whitelist. */
    public boolean canManage(final Player p) {
        return this.isOwner(p) || p.hasPermission(ADMIN_PERMISSION);
    }

    public Location getLocation() {
        return this.getBlock().getLocation();
    }

    public World getWorld() {
        return this.getLocation().getWorld();
    }

    public void setTraits(Map<RHopperTrait, RHopperTraitBase> traitMap, boolean save) {
        this.traitMap = traitMap;
        if (save)
            this.saveData(Data.TRAITS);
    }

    public Map<RHopperTrait, RHopperTraitBase> getTraitMap() {
        return traitMap;
    }

    public double getBalance() {
        return this.balance;
    }

    public void setBalance(double i) {
        this.balance = i;
        Bukkit.getPluginManager().callEvent(new RHopperStateChangeEvent(this));
        this.saveData(Data.BALANCE);
    }

    /**
     * Puts a balance back on a hopper being read from disk. Unlike {@link #setBalance(double)} this
     * fires no event and queues no write - at load time there is nothing to tell and nothing to
     * save, the value came from the file in the first place.
     */
    public void restoreBalance(double i) {
        this.balance = i;
    }

    public int getXp() {
        return this.xp;
    }

    public void setXp(final int xp) {
        this.xp = Math.max(0, xp);
        Bukkit.getPluginManager().callEvent(new RHopperStateChangeEvent(this));
        this.saveData(Data.XP);
    }

    public void addXp(final int amount) {
        this.setXp(this.xp + amount);
    }

    /** Puts experience back on a hopper being read from disk, firing nothing and saving nothing. */
    public void restoreXp(final int xp) {
        this.xp = Math.max(0, xp);
    }

    /** Whether anything on this hopper gathers experience, which is what the GUI asks. */
    public boolean hasXpCapabilities() {
        return this.hasTrait(RHopperTrait.XP_COLLECT);
    }

    public List<String> getHopperDescription() {
        List<String> desc = new ArrayList<>();
        desc.add(TranslatableLine.GUI_HOPPER_OWNER
                .with(PLAYER, this.getOwnerDisplayName()).get());
        desc.add(TranslatableLine.GUI_HOPPER_ACCESS
                .with(VALUE, this.access.getDisplayName()).get());
        if (this.hasEconomyCapabilities()) {
            desc.add(TranslatableLine.GUI_HOPPER_BALANCE
                    .with(MONEY, Text.formatNumber(this.getBalance())).get());
        }
        if (this.hasXpCapabilities()) {
            desc.add(TranslatableLine.GUI_HOPPER_XP
                    .with(VALUE, String.valueOf(this.getXp())).get());
        }
        if (this.hasLink()) {
            desc.add(TranslatableLine.GUI_HOPPER_LINK
                    .with(VALUE, Text.cords(this.link.getLocation())).get());
        }

        desc.add(TranslatableLine.GUI_HOPPER_TRAITS_HEADER.get());

        if (this.getTraitMap().isEmpty()) {
            desc.add(TranslatableLine.GUI_HOPPER_NO_TRAITS.get());
        } else {
            this.getTraitMap().forEach((trait, base) -> desc.add(TranslatableLine.GUI_HOPPER_TRAIT_ENTRY
                    .with(TRAIT, trait.getName())
                    .with(VALUE, String.valueOf(base.getTier())).get()));
        }

        if (this.hasEconomyCapabilities()) {
            desc.add("");
            desc.add(TranslatableLine.GUI_HOPPER_COLLECT.get());
        }
        return desc;
    }

    public boolean hasEconomyCapabilities() {
        for (RHopperTrait trait : this.getTraitMap().keySet()) {
            if (trait.hasEconomyCapabilities()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasTrait(RHopperTrait t) {
        return this.getTraitMap().containsKey(t);
    }

    public RHopperTraitBase getTrait(RHopperTrait t) {
        return this.getTraitMap().get(t);
    }

    /**
     * The trait as its own type, or null when the hopper has not got it. Lets the hopper ask a
     * trait to do its job instead of reimplementing it behind a {@code hasTrait} check.
     */
    public <T extends RHopperTraitBase> T getTrait(final RHopperTrait t, final Class<T> type) {
        final RHopperTraitBase base = this.getTraitMap().get(t);
        return type.isInstance(base) ? type.cast(base) : null;
    }

    /**
     * Takes a trait off this hopper, stopping whatever it had running. The other overload removes
     * by linked hopper, which is what a deleted hopper needs; this is what the GUI needs.
     *
     * @return whether the hopper had the trait at all
     */
    public boolean removeTrait(RHopperTrait trait) {
        final RHopperTraitBase removed = this.getTraitMap().remove(trait);
        if (removed == null) {
            return false;
        }
        removed.stopTask();
        this.saveData(Data.TRAITS);
        return true;
    }

    public void setTrait(RHopperTrait trait, RHopperTraitBase t) {
        this.getTraitMap().put(trait, t);
        t.startTask();
        this.saveData(Data.TRAITS);
    }

    /**
     * Raises or lowers a trait's tier, clamped to what that trait allows.
     *
     * @return the tier actually set, or -1 when the hopper has not got the trait
     */
    public int setTraitTier(final RHopperTrait trait, final int tier) {
        final RHopperTraitBase base = this.getTrait(trait);
        if (base == null) {
            return -1;
        }
        final int set = base.setTier(tier);
        this.saveData(Data.TRAITS);
        return set;
    }

    /** A trait's tier, or 0 when the hopper has not got it. */
    public int getTraitTier(final RHopperTrait trait) {
        final RHopperTraitBase base = this.getTrait(trait);
        return base == null ? 0 : base.getTier();
    }

    /**
     * Starts every trait on this hopper. Called once the whole file is loaded and the links between
     * hoppers have been resolved, which is the earliest point a linked trait can safely run.
     */
    public void startTraitTasks() {
        this.getTraitMap().values().forEach(RHopperTraitBase::startTask);
    }

    /**
     * Sells one of {@code type} into this hopper's balance.
     *
     * <p>Used to take a {@code skipMaterialVerify} flag that short-circuited the very
     * {@code containsKey} guard protecting the {@code get} on the next line, so passing true for an
     * unpriced material unboxed null. Callers now read the return value instead: false means the
     * material has no price and nothing was sold, so the item is still the caller's to deal with.</p>
     *
     * @return whether the material had a price and the balance went up
     */
    public boolean sell(Material type) {
        final RHAutoSellTrait autoSell = this.getTrait(RHopperTrait.AUTO_SELL, RHAutoSellTrait.class);
        //a hopper without the trait answers false rather than refusing to be asked, which is what
        //lets callers write `if (!sell(x))` instead of pairing every call with a hasTrait of
        //their own. A smelting hopper sells what it would have stored, not what went in.
        return autoSell != null && autoSell.sell(this.transform(type));
    }

    public Block getBlock() {
        return block;
    }

    /**
     * Whether the block this hopper was registered on is still a hopper.
     *
     * <p>A hopper can stop being one without the plugin hearing about it: a piston pushes it, a
     * plugin or a command replaces the block, a world edit paints over it. Only ever asked with the
     * chunk loaded, since {@link #isChunkLoaded()} decides that first.</p>
     */
    public boolean isValid() {
        return this.getBlock().getType() == Material.HOPPER;
    }

    /**
     * Whether the hopper's chunk is in memory. Nothing may touch the block otherwise: reading it
     * pulls the chunk back in, and a trait on a timer would do that every few ticks for a hopper
     * nobody is anywhere near.
     */
    public boolean isChunkLoaded() {
        final World world = this.getBlock().getWorld();
        return world.isChunkLoaded(this.getBlock().getX() >> 4, this.getBlock().getZ() >> 4);
    }

    /**
     * This hopper's inventory, or null when the block is no longer a hopper.
     *
     * <p>Used to cast the block state to Hopper and hand the result straight back, so a hopper
     * blown up by TNT - which the plugin was not listening for - threw a ClassCastException out of
     * its own trait loop every ten ticks.</p>
     */
    public Inventory getInventory() {
        final BlockState state = this.getBlock().getState();
        return state instanceof Hopper ? ((Hopper) state).getInventory() : null;
    }

    public void openInventory(Player p) {
        final Inventory hopperInventory = this.getInventory();
        if (hopperInventory != null) {
            p.openInventory(hopperInventory);
        }
    }

    /**
     * What this hopper would actually end up holding for an incoming item: the item itself, or its
     * smelted form on a hopper with {@link RHopperTrait#AUTO_SMELT}.
     *
     * <p>Every path in has to agree on this. Checking for room for cobblestone and then storing
     * stone would be asking the wrong question, and selling a full hopper's overflow would price
     * the material the hopper never had.</p>
     */
    public ItemStack transform(ItemStack incoming) {
        final RHAutoSmeltTrait autoSmelt = this.getTrait(RHopperTrait.AUTO_SMELT, RHAutoSmeltTrait.class);
        return autoSmelt == null ? incoming : autoSmelt.smelt(incoming);
    }

    public Material transform(Material incoming) {
        return this.transform(new ItemStack(incoming)).getType();
    }

    public boolean hasHopperSpace(Material m) {
        return hasHopperSpace(new ItemStack(m));
    }

    public boolean hasHopperSpace(ItemStack itemToCheck) {
        itemToCheck = this.transform(itemToCheck);
        Inventory hopperInventory = this.getInventory();
        if (hopperInventory == null) {
            return false;
        }

        for (int i = 0; i < hopperInventory.getSize(); i++) {
            ItemStack slotItem = hopperInventory.getItem(i);

            if (slotItem == null) {
                return true; // Hopper has an empty slot
            } else if (slotItem.isSimilar(itemToCheck)) {
                int spaceRemaining = slotItem.getMaxStackSize() - slotItem.getAmount();
                if (spaceRemaining >= itemToCheck.getAmount()) {
                    return true; // There is space to add more of the same item
                }
            }
        }

        return false; // Hopper is full or no matching slots were found
    }

    public void addItem(ItemStack i) {
        final Inventory hopperInventory = this.getInventory();
        if (hopperInventory != null) {
            hopperInventory.addItem(this.transform(i));
        }
    }

    public void addItem(Material type) {
        this.addItem(new ItemStack(type));
    }

    /**
     * Hands the hopper an item and lets its traits decide what becomes of it: smelted on the way in,
     * stored if there is room, sold if there is not, and handed back if none of that applied.
     *
     * <p>The one place that decision lives. Suction, block breaking and mob drops each used to
     * carry their own copy of it, and every one of them worked a single item at a time - so a
     * dropped stack of sixty-four was one item stored and sixty-three destroyed. Amounts are
     * respected here.</p>
     *
     * @param incoming the item offered, which is not modified
     * @return what the hopper could not take, for the caller to drop or leave where it was, or
     *         null when all of it was dealt with
     */
    /**
     * Whether any trait on this hopper would change what becomes of an incoming item.
     *
     * <p>Asked before the plugin takes a vanilla hopper transfer over. False means vanilla should
     * be left to do exactly what it was going to, which keeps transfer cooldowns and the rest of
     * the hopper's own behaviour intact - and these events fire constantly, so the common answer
     * has to be cheap. It is: a hopper with neither trait costs two map lookups.</p>
     */
    public boolean intercepts(final ItemStack incoming) {
        //transform hands back the very same instance when nothing would smelt it, so this is an
        //identity check rather than a comparison
        final ItemStack stored = this.transform(incoming);
        if (stored != incoming) {
            return true;
        }

        //a filter that turns this away changes the outcome whether or not there is room
        final RHFilterTrait filter = this.getTrait(RHopperTrait.FILTER, RHFilterTrait.class);
        if (filter != null && !filter.accepts(stored.getType())) {
            return true;
        }

        //selling and voiding only change anything once there is no room, and that is the expensive
        //question, so it is asked last and only of a hopper that can do one of them
        if (this.hasTrait(RHopperTrait.VOID) || this.hasTrait(RHopperTrait.AUTO_SELL)) {
            return !this.hasHopperSpace(incoming);
        }
        return false;
    }

    public ItemStack offer(final ItemStack incoming) {
        if (incoming == null || incoming.getType() == Material.AIR || incoming.getAmount() <= 0) {
            return null;
        }

        ItemStack remaining = this.transform(incoming.clone());

        //a filter governs what may be kept, and nothing else. What it turns away still goes on to
        //be sold or voided, so a filter beside either keeps what is listed and disposes of the rest
        final RHFilterTrait filter = this.getTrait(RHopperTrait.FILTER, RHFilterTrait.class);
        final boolean mayKeep = filter == null || filter.accepts(remaining.getType());

        final Inventory hopperInventory = this.getInventory();
        if (mayKeep && hopperInventory != null) {
            final Map<Integer, ItemStack> leftover = hopperInventory.addItem(remaining);
            if (leftover.isEmpty()) {
                return null;
            }
            //addItem takes what it can and reports the rest, so a stack can be split between
            //being stored and being sold
            remaining = leftover.values().iterator().next();
        }

        //already transformed, so the trait is asked directly rather than through sell(Material),
        //which would smelt it a second time
        final RHAutoSellTrait autoSell = this.getTrait(RHopperTrait.AUTO_SELL, RHAutoSellTrait.class);
        if (autoSell != null && autoSell.sell(remaining.getType(), remaining.getAmount())) {
            return null;
        }

        //last word: sell what has a price, destroy the rest
        if (this.hasTrait(RHopperTrait.VOID)) {
            return null;
        }

        return remaining;
    }

    /**
     * Queues this hopper to be written to the database. The write itself happens on the flush
     * task, not here: an auto-selling hopper changes its balance every time it swallows an item, and
     * a write per item would be the plugin's heaviest piece of IO. Whatever changed, the whole
     * hopper is written, so the part is only kept for callers that already say it.
     */
    public void saveData(Data d) {
        RealHoppersAPI.getInstance().getDatabaseManager().markDirty(this);
    }

    /** The owner's name for display, or the language file's word for nobody. */
    public String getOwnerDisplayName() {
        if (this.owner == null) {
            return TranslatableLine.HOPPER_UNKNOWN_OWNER.get();
        }
        if (this.ownerName == null) {
            final String name = Bukkit.getOfflinePlayer(this.owner).getName();
            return name == null ? TranslatableLine.HOPPER_UNKNOWN_OWNER.get() : name;
        }
        return this.ownerName;
    }

    @Override
    public String toString() {
        return "RHopper{" +
                "location=" + block.getLocation() +
                ", name=" + this.name +
                ", owner=" + this.owner +
                ", traits=" + this.getTraitMap().keySet() +
                '}';
    }

    public String getSerializedLocation() {
        return serializeLocation(this.getBlock().getLocation());
    }

    private String serializeLocation(Location location) {
        return String.format("%d:%d:%d:%s", location.getBlockX(), location.getBlockY(), location.getBlockZ(), location.getWorld().getName());
    }

    public RHopper getLink() {
        return this.link;
    }

    public boolean hasLink() {
        return this.link != null;
    }

    /**
     * Points this hopper at another, replacing whatever it pointed at before. Traits waiting on a
     * link start as soon as there is one.
     */
    public void setLink(final RHopper link) {
        this.link = link;
        this.linkLocation = null;
        this.saveData(Data.LINK);
        this.startTraitTasks();
    }

    /** Drops the link. Traits that need one stop until the hopper is linked again. */
    public void removeLink() {
        this.link = null;
        this.linkLocation = null;
        this.saveData(Data.LINK);
        this.getTraitMap().forEach((trait, base) -> {
            if (trait.requiresLink()) {
                base.stop();
            }
        });
    }

    /** Only for loading: the destination's position, before the hopper it names has been read. */
    public void setLinkLocation(final String linkLocation) {
        this.linkLocation = linkLocation;
    }

    /**
     * Turns the position read off disk into the hopper it names. Runs once the whole file is in, so
     * that a hopper can be linked to one stored after it.
     */
    public void loadLink() {
        if (this.linkLocation == null || this.linkLocation.isEmpty()) {
            return;
        }

        //a world that isn't loaded right now isn't proof the target is gone: the stored link is
        //kept, and written back as it was, until that world comes back
        final String[] parts = this.linkLocation.split(":");
        if (parts.length == 4 && Bukkit.getWorld(parts[3]) == null) {
            RealHoppersAPI.getInstance().getLogger().warning("The hopper at " + this.getSerializedLocation()
                    + " is linked to " + this.linkLocation + ", in a world that is not loaded. Keeping the link.");
            return;
        }

        final Location l = LocationUtil.deserializeLocation(this.linkLocation);
        if (l == null) {
            RealHoppersAPI.getInstance().getLogger().severe("Could not parse the link of the hopper at "
                    + this.getSerializedLocation() + " (" + this.linkLocation + ")! Unlinking.");
            this.linkLocation = null;
            this.saveData(Data.LINK);
            return;
        }

        final RHopper linked = RealHoppersAPI.getInstance().getHopperManager().getHopper(l.getBlock());
        if (linked == null) {
            RealHoppersAPI.getInstance().getLogger().warning("The hopper at " + this.getSerializedLocation()
                    + " is linked to " + this.linkLocation + ", where there is no hopper. Unlinking.");
            this.linkLocation = null;
            this.saveData(Data.LINK);
            return;
        }

        this.link = linked;
        this.linkLocation = null;
    }

    /**
     * What to store as the link: the linked hopper, or a link still waiting on its world to load.
     * Null when there is neither.
     */
    public String getStoredLink() {
        if (this.link != null) {
            return this.link.getSerializedLocation();
        }
        return this.linkLocation;
    }

    /** Cancels everything this hopper has running and puts its balance beyond the next flush. */
    public void stopHopper() {
        this.getTraitMap().values().forEach(RHopperTraitBase::stopTask);
        this.saveData(Data.BALANCE);
    }

    public Location getTeleportLocation() {
        return this.getBlock().getLocation().add(0.5, 1, 0.5);
    }

}
