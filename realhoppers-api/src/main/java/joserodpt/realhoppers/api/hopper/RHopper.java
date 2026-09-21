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
import joserodpt.realhoppers.api.config.RHHoppers;
import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.events.RHopperStateChangeEvent;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSellTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSmeltTrait;
import joserodpt.realhoppers.api.utils.LocationUtil;
import joserodpt.realhoppers.api.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RHopper {

    public enum Data {ALL, BALANCE, TRAITS, LINK }

    private Block block;
    private boolean visualizing;
    private double balance;
    private Map<RHopperTrait, RHopperTraitBase> traitMap = new HashMap<>();

    /**
     * The one other hopper this one points at. Every trait that needs a second hopper - TELEPORT
     * and ITEM_TRANSF - uses this one, rather than each carrying a destination of its own: a hopper
     * has one link, and whatever is on it follows that link.
     */
    private RHopper link;

    /** The link as it was read off disk, until {@link #loadLink()} can turn it into a hopper. */
    private String linkLocation;

    public RHopper(Block b, boolean save) {
        //new hopper
        this.block = b;

        this.setVisualizing(true);

        if (save)
            this.saveData(Data.ALL);
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

    public List<String> getHopperDescription() {
        List<String> desc = new ArrayList<>();
        if (this.hasEconomyCapabilities()) {
            desc.add(TranslatableLine.GUI_HOPPER_BALANCE
                    .setV1(TranslatableLine.ReplacableVar.MONEY.eq(Text.formatNumber(this.getBalance()))).get());
        }
        if (this.hasLink()) {
            desc.add(TranslatableLine.GUI_HOPPER_LINK
                    .setV1(TranslatableLine.ReplacableVar.VALUE.eq(Text.cords(this.link.getLocation()))).get());
        }

        desc.add(TranslatableLine.GUI_HOPPER_TRAITS_HEADER.get());

        if (this.getTraitMap().isEmpty()) {
            desc.add(TranslatableLine.GUI_HOPPER_NO_TRAITS.get());
        } else {
            this.getTraitMap().forEach((trait, base) -> desc.add(TranslatableLine.GUI_HOPPER_TRAIT_ENTRY
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName()))
                    .setV2(TranslatableLine.ReplacableVar.VALUE.eq(String.valueOf(base.getTier()))).get()));
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
     * Writes this hopper into the in-memory hoppers document and marks it dirty. The file itself is
     * written by the flush task, not here: an auto-selling hopper changes its balance every time it
     * swallows an item, and this used to serialise and rewrite the whole of hoppers.yml each time.
     */
    public void saveData(Data d) {
        switch (d) {
            case TRAITS:
                //a section of TRAIT: tier rather than a list of names, so a trait's tier has
                //somewhere to live and the file says what it means without being decoded
                final String traitsRoute = "Hoppers." + this.getSerializedLocation() + ".Traits";
                RHHoppers.file().remove(traitsRoute);
                this.getTraitMap().forEach((trait, base) ->
                        RHHoppers.file().set(traitsRoute + "." + trait.name(), base.getTier()));
                break;
            case BALANCE:
                RHHoppers.file().set("Hoppers." + this.getSerializedLocation() + ".Balance", this.getBalance());
                break;
            case LINK:
                RHHoppers.file().set("Hoppers." + this.getSerializedLocation() + ".Link",
                        this.link == null ? null : this.link.getSerializedLocation());
                break;
            case ALL:
                saveData(Data.TRAITS);
                saveData(Data.BALANCE);
                saveData(Data.LINK);
                break;
        }
        RHHoppers.markDirty();
    }

    @Override
    public String toString() {
        return "RHopper{" +
                "location=" + block.getLocation() +
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
        this.saveData(Data.LINK);
        this.startTraitTasks();
    }

    /** Drops the link. Traits that need one stop until the hopper is linked again. */
    public void removeLink() {
        this.link = null;
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

        final Location l = LocationUtil.deserializeLocation(this.linkLocation);
        if (l == null) {
            RealHoppersAPI.getInstance().getLogger().severe("Could not parse the link of the hopper at "
                    + this.getSerializedLocation() + " (" + this.linkLocation + ")! Unlinking.");
            this.linkLocation = null;
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

    public Location getTeleportLocation() {
        return this.getBlock().getLocation().add(0.5, 1, 0.5);
    }

    public void setVisualizing(boolean visualizing) {
        this.visualizing = visualizing;
    }

    public void stopHopper() {
        this.getTraitMap().values().forEach(RHopperTraitBase::stopTask);
        this.saveData(Data.BALANCE);
    }

    public void loopView() {
        if (visualizing) {
            double minX = this.getLocation().getBlockX();
            double minY = this.getLocation().getBlockY();
            double minZ = this.getLocation().getBlockZ();

            double maxX = this.getLocation().getBlockX() + 1;
            double maxY = this.getLocation().getBlockY() + 1;
            double maxZ = this.getLocation().getBlockZ() + 1;

            double dist = 0.5;
            for (double x = minX; x <= maxX; x += dist) {
                for (double y = minY; y <= maxY; y += dist) {
                    for (double z = minZ; z <= maxZ; z += dist) {
                        int components = 0;
                        if (x == minX || x == maxX) components++;
                        if (y == minY || y == maxY) components++;
                        if (z == minZ || z == maxZ) components++;
                        if (components >= 2) {
                            final Location l = new Location(this.getBlock().getWorld(), x, y, z);
                            l.getWorld().spawnParticle(Particle.REDSTONE, l.getX(), l.getY(), l.getZ(), 0, 0.001, 1, 0, 1, new Particle.DustOptions(Color.WHITE, 1));
                        }
                    }
                }
            }
        }
    }
}
