package joserodpt.realhoppers.plugin.managers;

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

import com.google.common.collect.ImmutableList;
import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.config.RHHoppers;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.hopper.trait.traits.RHBlockBreakingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHDummyTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHItemTransferTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHMobKillingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHSuctionTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHTeleportationTrait;
import joserodpt.realhoppers.api.managers.HopperManagerAPI;
import joserodpt.realhoppers.api.utils.LocationUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HopperManager extends HopperManagerAPI {
    private final RealHoppersAPI rh;

    public HopperManager(RealHoppersAPI rh) {
        this.rh = rh;
    }

    public Map<Block, RHopper> hoppers = new HashMap<>();
    public Map<Material, Double> materialCost = new HashMap<>();

    @Override
    public Map<Block, RHopper> getHoppersMap() {
        return hoppers;
    }

    @Override
    public List<RHopper> getHoppers() {
        return ImmutableList.copyOf(hoppers.values());
    }

    @Override
    public RHopper getHopper(Block b) {
        return this.getHoppersMap().get(b);
    }


    @Override
    public void loadHoppers() {
        //reload comes through here too, and the hoppers already in the map own scheduled tasks.
        //Dropping them without stopping first left every task running against an orphaned hopper.
        this.stopHoppers();
        this.getHoppersMap().clear();
        if (RHHoppers.file().isSection("Hoppers")) {
            for (String hopperSTR : RHHoppers.file().getSection("Hoppers").getRoutesAsStrings(false)) {
                Location l = LocationUtil.deserializeLocation(hopperSTR);
                if (l == null) {
                    rh.getLogger().severe("Could not parse location for hopper " + hopperSTR + "! Skipping.");
                    continue;
                }

                Block b = l.getBlock();
                if (b == null || b.getType() != Material.HOPPER) {
                    rh.getLogger().severe("Block at location " + hopperSTR + " isn't a Hopper! Skipping.");
                    continue;
                }

                Map<RHopperTrait, RHopperTraitBase> traitMap = new HashMap<>();
                RHopper loaded = new RHopper(b, false);
                //not setBalance: that fires a state change event and queues a write, for a value
                //that was just read out of the file
                loaded.restoreBalance(RHHoppers.file().getDouble("Hoppers." + hopperSTR + ".Balance"));

                if (RHHoppers.file().isList("Hoppers." + hopperSTR + ".Traits")) {
                    List<String> traits = RHHoppers.file().getStringList("Hoppers." + hopperSTR + ".Traits");

                    for (final String trait : traits) {
                        final String[] split = trait.split("\\|");
                        final String traitType = split[0];
                        switch (RHopperTrait.valueOf(traitType)) {
                            case TELEPORT:
                                traitMap.put(RHopperTrait.TELEPORT, new RHTeleportationTrait(loaded, split[1]));
                                break;
                            case ITEM_TRANS:
                                traitMap.put(RHopperTrait.ITEM_TRANS, new RHItemTransferTrait(loaded, split[1]));
                                break;
                            case BLOCK_BREAKING:
                                traitMap.put(RHopperTrait.BLOCK_BREAKING, new RHBlockBreakingTrait(loaded));
                                break;
                            case KILL_MOB:
                                traitMap.put(RHopperTrait.KILL_MOB, new RHMobKillingTrait(loaded));
                                break;
                            case SUCTION:
                                traitMap.put(RHopperTrait.SUCTION, new RHSuctionTrait(loaded));
                                break;
                            case AUTO_SELL:
                                traitMap.put(RHopperTrait.AUTO_SELL, new RHDummyTrait(loaded, RHopperTrait.AUTO_SELL));
                                break;
                            default:
                                rh.getLogger().severe(traitType + " trait is not supported in this version of RealHoppers! Skipping.");
                                break;
                        }
                    }
                }

                loaded.setTraits(traitMap, false);

                this.getHoppersMap().put(b, loaded);
            }

            //links first: a trait that points at another hopper cannot run until that hopper
            //exists and has been found, so nothing is started before the whole file is read
            this.getHoppersMap().values().forEach(RHopper::loadLinks);
            this.getHoppersMap().values().forEach(RHopper::startTraitTasks);
        }

        //load material cost
        this.getMaterialCost().clear();

        if (RHConfig.file().isSection("RealHoppers.Material-Values")) {
            for (String material : RHConfig.file().getSection("RealHoppers.Material-Values").getRoutesAsStrings(false)) {
                try {
                    Material m = Material.valueOf(material);
                    Double cost = RHConfig.file().getDouble("RealHoppers.Material-Values." + material);
                    this.getMaterialCost().put(m, cost);
                } catch (IllegalArgumentException e) {
                    rh.getLogger().severe(material + " isn't a valid material type! Skipping.");
                }
            }
        }
    }

    @Override
    public void delete(RHopper h) {
        RHHoppers.file().remove("Hoppers." + h.getSerializedLocation());
        RHHoppers.save();
        h.stopHopper();
        //loop through all hoppers and check if any trait constains this hopper and if so remove it
        for (RHopper hopper : this.getHoppers()) {
            if (hopper.getLinkedHoppers().contains(h)) {
                hopper.removeTrait(h);
            }
        }

        this.getHoppersMap().remove(h.getBlock());
    }

    @Override
    public void stopHoppers() {
        this.getHoppers().forEach(RHopper::stopHopper);
        //stopHopper only queues the balance write, and on shutdown there is no flush left to run
        RHHoppers.saveIfDirty();
    }

    @Override
    public Map<Material, Double> getMaterialCost() {
        return materialCost;
    }

}