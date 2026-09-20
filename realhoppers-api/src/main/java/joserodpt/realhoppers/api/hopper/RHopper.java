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
 * @author José Rodrigues © 2019-2026
 * @link https://github.com/joserodpt/RealHoppers
 */


import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHHoppers;
import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.events.RHopperStateChangeEvent;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
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

    public enum Data {ALL, BALANCE, TRAITS }

    private Block block;
    private boolean visualizing;
    private double balance;
    private Map<RHopperTrait, RHopperTraitBase> traitMap = new HashMap<>();

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

    public List<RHopper> getLinkedHoppers() {
        return this.getTraitMap().values().stream().map(RHopperTraitBase::getLinkedHopper).collect(Collectors.toList());
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
        desc.add(TranslatableLine.GUI_HOPPER_TRAITS_HEADER.get());

        if (this.getTraitMap().isEmpty()) {
            desc.add(TranslatableLine.GUI_HOPPER_NO_TRAITS.get());
        } else {
            this.getTraitMap().keySet().forEach(trait -> desc.add(TranslatableLine.GUI_HOPPER_TRAIT_ENTRY
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).get()));
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

    public void removeTrait(RHopper h) {
        for (Map.Entry<RHopperTrait, RHopperTraitBase> entry : this.getTraitMap().entrySet()) {
            RHopperTrait key = entry.getKey();
            RHopperTraitBase value = entry.getValue();
            if (value.getLinkedHopper() == h) {
                this.getTraitMap().remove(key);
                this.saveData(Data.TRAITS);
                return;
            }
        }
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
        final Double price = RealHoppersAPI.getInstance().getHopperManager().getMaterialCost().get(type);
        if (price == null) {
            return false;
        }
        this.setBalance(this.getBalance() + price);
        return true;
    }

    public Block getBlock() {
        return block;
    }

    public Inventory getInventory() {
        return ((Hopper) this.getBlock().getState()).getInventory();
    }

    public void openInventory(Player p) {
        Hopper hopper = (Hopper) this.getBlock().getState();
        if (hopper != null) {
            Inventory hopperInventory = hopper.getInventory();

            p.openInventory(hopperInventory);
        }
    }

    public boolean hasHopperSpace(Material m) {
        return hasHopperSpace(new ItemStack(m));
    }

    public boolean hasHopperSpace(ItemStack itemToCheck) {
        Inventory hopperInventory = this.getInventory();

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
        this.getInventory().addItem(i);
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
                RHHoppers.file().set("Hoppers." + this.getSerializedLocation() + ".Traits", this.getTraitMap().values().stream().map(RHopperTraitBase::getSerializedSave).collect(Collectors.toList()));
                break;
            case BALANCE:
                RHHoppers.file().set("Hoppers." + this.getSerializedLocation() + ".Balance", this.getBalance());
                break;
            case ALL:
                saveData(Data.TRAITS);
                saveData(Data.BALANCE);
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

    public void loadLinks() {
        //loadLink logs loudly when there is no location to read, so the traits that never
        //have one - suction, block breaking, mob killing - are not put through it
        this.getTraitMap().entrySet().stream()
                .filter(entry -> entry.getKey().requiresLink())
                .forEach(entry -> entry.getValue().loadLink());
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
