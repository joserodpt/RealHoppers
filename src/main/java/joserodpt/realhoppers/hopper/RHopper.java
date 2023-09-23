package joserodpt.realhoppers.hopper;

import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.hopper.type.RHopperTraitBase;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RHopper {

    public enum Data { TRAITS }
    public enum Trait {
        TELEPORT(Material.ENDER_PEARL),
        ITEM_TRANS(Material.ENDER_EYE),
        AUTO_CRAFT(Material.CRAFTING_TABLE),
        AUTO_SELL(Material.EMERALD),
        AUTO_SMELT(Material.FURNACE),
        SUCTION(Material.FEATHER),
        BLOCK_BREAKING(Material.TNT),
        KILL_MOB(Material.DIAMOND_SWORD);

        private Material icon;
        Trait(Material icon) {
            this.icon = icon;
        }

        public Material getIcon() {
            return icon;
        }
    }

    private Block block;
    private boolean visualizing;
    private Map<Trait, RHopperTraitBase> traitMap = new HashMap<>();

    public RHopper(Block b, boolean save) {
        //new hopper
        this.block = b;

        this.setVisualizing(true);

        if (save)
            this.saveData(Data.TRAITS, true);
    }

    public Location getLocation() {
        return this.getBlock().getLocation();
    }

    public void setTraits(Map<Trait, RHopperTraitBase> traitMap, boolean save) {
        this.traitMap = traitMap;
        if (save)
            this.saveData(Data.TRAITS, true);
    }

    public List<RHopper> getLinkedHoppers() {
        return this.getTraitMap().values().stream().map(RHopperTraitBase::getLinkedHopper).collect(Collectors.toList());
    }

    public Map<Trait, RHopperTraitBase> getTraitMap() {
        return traitMap;
    }

    public boolean hasTrait(Trait t) {
        return this.getTraitMap().containsKey(t);
    }

    public RHopperTraitBase getTrait(Trait t) {
        return this.getTraitMap().get(t);
    }

    public void removeTrait(RHopper h) {
        for (Map.Entry<Trait, RHopperTraitBase> entry : this.getTraitMap().entrySet()) {
            Trait key = entry.getKey();
            RHopperTraitBase value = entry.getValue();
            if (value.getLinkedHopper() == h) {
                this.getTraitMap().remove(key);
                this.saveData(Data.TRAITS, true);
                return;
            }
        }
    }

    public void setTrait(Trait trait, RHopperTraitBase t) {
        this.getTraitMap().put(trait, t);
        this.saveData(Data.TRAITS, true);
    }

    public Block getBlock() {
        return block;
    }

    public Inventory getInventory() {
        return ((Hopper) this.getBlock().getState()).getInventory();
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

    private boolean addItem(ItemStack i) {
        if (hasHopperSpace(i)) {
            this.getInventory().addItem(i);
            return true;
        } else {
            return false;
        }
    }

    public boolean addItem(Material type) {
       return this.addItem(new ItemStack(type));
    }

    public void saveData(Data d, boolean save) {
        switch (d) {
            case TRAITS:
                Hoppers.file().set("Hoppers." + this.getSerializedLocation() + ".Traits", this.getTraitMap().values().stream().map(RHopperTraitBase::getSerializedSave).collect(Collectors.toList()));
                break;
        }
        if (save)
            Hoppers.save();
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
        this.getTraitMap().values().forEach(RHopperTraitBase::loadLink);
    }

    public Location getTeleportLocation() {
        return this.getBlock().getLocation().add(0.5, 1, 0.5);
    }

    public void setVisualizing(boolean visualizing) {
        this.visualizing = visualizing;
    }

    public void stopTasks() {
        this.getTraitMap().values().forEach(RHopperTraitBase::stopTask);
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
