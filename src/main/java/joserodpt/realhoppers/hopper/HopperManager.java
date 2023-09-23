package joserodpt.realhoppers.hopper;

import com.google.common.collect.ImmutableList;
import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.hopper.type.RHBlockBreaking;
import joserodpt.realhoppers.hopper.type.RHItemTransfer;
import joserodpt.realhoppers.hopper.type.RHTeleportation;
import joserodpt.realhoppers.hopper.type.RHopperTraitBase;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static joserodpt.realhoppers.utils.LocationUtil.deserializeLocation;

public class HopperManager {

    public HashMap<Block, RHopper> hoppers = new HashMap<>();

    public HashMap<Block, RHopper> getHoppersMap() {
        return hoppers;
    }

    public List<RHopper> getHoppers() {
        return ImmutableList.copyOf(hoppers.values());
    }

    public RHopper getHopper(Block b) {
        return this.getHoppersMap().get(b);
    }

    public void loadHoppers() {
        this.getHoppersMap().clear();
        if (Hoppers.file().isSection("Hoppers")) {
            for (String hopperSTR : Hoppers.file().getSection("Hoppers").getRoutesAsStrings(false)) {
                Location l = deserializeLocation(hopperSTR);
                if (l == null) {
                    RealHoppers.getPlugin().getLogger().severe("Could not parse location for hopper " + hopperSTR + "! Skipping.");
                    continue;
                }

                Block b = l.getBlock();
                if (b == null || b.getType() != Material.HOPPER) {
                    RealHoppers.getPlugin().getLogger().severe("Block at location " + hopperSTR + " isn't a Hopper! Skipping.");
                    continue;
                }

                Map<RHopper.Trait, RHopperTraitBase> traitMap = new HashMap<>();
                RHopper loaded = new RHopper(b, false);

                if (Hoppers.file().isList("Hoppers." + hopperSTR + ".Traits")) {
                    List<String> traits = Hoppers.file().getStringList("Hoppers." + hopperSTR + ".Traits");

                    for (final String trait : traits) {
                        final String[] split = trait.split("\\|");
                        final String traitType = split[0];
                        switch (RHopper.Trait.valueOf(traitType)) {
                            case TELEPORT:
                                traitMap.put(RHopper.Trait.TELEPORT, new RHTeleportation(loaded, split[1]));
                                break;
                            case ITEM_TRANS:
                                traitMap.put(RHopper.Trait.ITEM_TRANS, new RHItemTransfer(loaded, split[1]));
                                break;
                            case BLOCK_BREAKING:
                                traitMap.put(RHopper.Trait.BLOCK_BREAKING, new RHBlockBreaking(loaded));
                                break;
                            default:
                                RealHoppers.getPlugin().getLogger().severe(traitType + " trait is not supported in this version of RealHoppers! Skipping.");
                                break;
                        }
                    }
                }

                loaded.setTraits(traitMap, false);

                this.getHoppersMap().put(b, loaded);
            }

            //load links
            this.getHoppersMap().values().forEach(RHopper::loadLinks);
        }
    }

    public void delete(RHopper h) {
        Hoppers.file().remove("Hoppers." + h.getSerializedLocation());
        Hoppers.save();
        //loop through all hoppers and check if any trait constains this hopper and if so remove it
        for (RHopper hopper : this.getHoppers()) {
            if (hopper.getLinkedHoppers().contains(h)) {
                hopper.removeTrait(h);
            }
        }
        this.getHoppersMap().remove(h.getBlock());
    }

    public void stopHoppers() {
        this.getHoppers().forEach(RHopper::stopTasks);
    }
}
