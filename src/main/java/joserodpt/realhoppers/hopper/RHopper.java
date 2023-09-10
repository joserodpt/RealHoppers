package joserodpt.realhoppers.hopper;

import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.hopper.type.RHopperTrait;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RHopper {

    public void setTraits(Map<Trait, RHopperTrait> traitMap, boolean save) {
        this.traitMap = traitMap;
        if (save)
            this.saveData(Data.TRAITS, true);
    }

    public enum Data { TRAITS }
    public enum Trait { TELEPORT, ITEM_TRANS, AUTO_CRAFT, AUTO_SELL, AUTO_SMELT, SUCTION, BLOCK_BREAKING, KILL_MOB }

    private Block block;
    private Map<Trait, RHopperTrait> traitMap = Collections.emptyMap();

    public RHopper(Block b, boolean save) {
        //new hopper
        this.block = b;

        if (save)
            this.saveData(Data.TRAITS, true);
    }

    public List<RHopper> getLinkedHoppers() {
        return this.getTraitMap().values().stream().map(rHopperTrait -> rHopperTrait.getLinkedHopper()).collect(Collectors.toList());
    }

    public Map<Trait, RHopperTrait> getTraitMap() {
        return traitMap;
    }

    public boolean hasTrait(Trait t) {
        return this.getTraitMap().containsKey(t);
    }

    public RHopperTrait getTrait(Trait t) {
        return this.getTraitMap().get(t);
    }

    public void removeTrait(RHopper h) {
        for (Map.Entry<Trait, RHopperTrait> entry : this.getTraitMap().entrySet()) {
            Trait key = entry.getKey();
            RHopperTrait value = entry.getValue();
            if (value.getLinkedHopper() == h) {
                this.getTraitMap().remove(key);
                this.saveData(Data.TRAITS, true);
                return;
            }
        }
    }

    public void setTrait(Trait trait, RHopperTrait t) {
        this.getTraitMap().put(trait, t);
        this.saveData(Data.TRAITS, true);
    }

    public Block getBlock() {
        return block;
    }

    public void saveData(Data d, boolean save) {
        switch (d) {
            case TRAITS:
                Hoppers.file().set("Hoppers." + this.getSerializedLocation() + ".Traits", this.getTraitMap().values().stream().map(rHopperTrait -> rHopperTrait.getTraitType() + "|" + rHopperTrait.getLinkedHopper().getSerializedLocation()).collect(Collectors.toList()));
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
        this.getTraitMap().values().forEach(RHopperTrait::loadLink);
    }

    public Location getTeleportLocation() {
        return this.getBlock().getLocation().add(0.5, 1, 0.5);
    }
}
