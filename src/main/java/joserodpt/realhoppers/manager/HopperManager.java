package joserodpt.realhoppers.manager;

/*
 *   ____            _ _   _
 *  |  _ \ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___
 *  | |_) / _ \/ _` | | |_| |/ _ \| '_ \| '_ \ / _ \ '__/ __|
 *  |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \__ \
 *  |_| \_\___|\__,_|_|_| |_|\___/| .__/| .__/ \___|_|  |___/
 *                                |_|   |_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues
 * @link https://github.com/joserodpt/RealHoppers
 */

import com.google.common.collect.ImmutableList;
import joserodpt.realhoppers.RealHoppers;
import joserodpt.realhoppers.config.Config;
import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.hopper.trait.traits.RHBlockBreaking;
import joserodpt.realhoppers.hopper.trait.traits.RHItemTransfer;
import joserodpt.realhoppers.hopper.trait.traits.RHMobKilling;
import joserodpt.realhoppers.hopper.trait.traits.RHTeleportation;
import joserodpt.realhoppers.hopper.trait.RHopperTraitBase;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static joserodpt.realhoppers.utils.LocationUtil.deserializeLocation;

public class HopperManager {
    private RealHoppers rh;

    public HopperManager(RealHoppers rh) {
        this.rh = rh;
    }

    public HashMap<Block, RHopper> hoppers = new HashMap<>();
    public HashMap<Material, Double> sellMaterials = new HashMap<>();

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

                Map<RHopperTrait, RHopperTraitBase> traitMap = new HashMap<>();
                RHopper loaded = new RHopper(b, false);

                if (Hoppers.file().isList("Hoppers." + hopperSTR + ".Traits")) {
                    List<String> traits = Hoppers.file().getStringList("Hoppers." + hopperSTR + ".Traits");

                    for (final String trait : traits) {
                        final String[] split = trait.split("\\|");
                        final String traitType = split[0];
                        switch (RHopperTrait.valueOf(traitType)) {
                            case TELEPORT:
                                traitMap.put(RHopperTrait.TELEPORT, new RHTeleportation(loaded, split[1]));
                                break;
                            case ITEM_TRANS:
                                traitMap.put(RHopperTrait.ITEM_TRANS, new RHItemTransfer(loaded, split[1]));
                                break;
                            case BLOCK_BREAKING:
                                traitMap.put(RHopperTrait.BLOCK_BREAKING, new RHBlockBreaking(loaded));
                                break;
                            case KILL_MOB:
                                traitMap.put(RHopperTrait.KILL_MOB, new RHMobKilling(loaded));
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

        //load material's prices from config
        if (Config.file().isSection("RealHoppers.Material-Values")) {
            for (String material : Config.file().getSection("RealHoppers.Material-Values").getRoutesAsStrings(false)) {
                Material m = null;
                try {
                    m = Material.valueOf(material);

                    try {
                        Double d = Config.file().getDouble("RealHoppers.Material-Values." + material);

                        getSellMaterials().put(m, d);
                    } catch (Exception e) {
                        rh.getLogger().severe(Config.file().getString("RealHoppers.Material-Values." + material) + " isn't a valid double. Skipping.");
                    }
                } catch (Exception e) {
                    rh.getLogger().severe(material + " isn't a valid material type. Skipping.");
                }
            }
        }
    }

    public HashMap<Material, Double> getSellMaterials() {
        return sellMaterials;
    }

    public void delete(RHopper h) {
        Hoppers.file().remove("Hoppers." + h.getSerializedLocation());
        Hoppers.save();
        h.stopTasks();
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
