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
 * @author José Rodrigues © 2023-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import com.google.common.collect.ImmutableList;
import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.config.RHHoppers;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
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
                loaded.setLinkLocation(RHHoppers.file().getString("Hoppers." + hopperSTR + ".Link"));

                if (RHHoppers.file().isList("Hoppers." + hopperSTR + ".Traits")) {
                    List<String> traits = RHHoppers.file().getStringList("Hoppers." + hopperSTR + ".Traits");

                    for (final String trait : traits) {
                        //traits used to be saved as TRAIT|<destination>, one link per trait. The
                        //link belongs to the hopper now, so an old entry hands its destination over
                        //to the hopper on the way past and is read as a plain trait name.
                        final String[] split = trait.split("\\|");
                        final String traitType = split[0];
                        if (split.length > 1 && !RHHoppers.file().isString("Hoppers." + hopperSTR + ".Link")) {
                            RHHoppers.file().set("Hoppers." + hopperSTR + ".Link", split[1]);
                            loaded.setLinkLocation(split[1]);
                            RHHoppers.markDirty();
                        }

                        final RHopperTrait type;
                        try {
                            type = RHopperTrait.valueOf(traitType);
                        } catch (final IllegalArgumentException e) {
                            //valueOf used to throw straight out of the loop, so one unreadable
                            //entry cost every hopper after it
                            rh.getLogger().severe(traitType + " is not a trait RealHoppers knows! Skipping.");
                            continue;
                        }

                        //the enum builds every trait, so one added there is loaded here without
                        //this loop having to be remembered. Forgetting it is exactly how SUCTION
                        //came back from disk as a block breaker.
                        final RHopperTraitBase built = type.build(loaded);
                        if (built == null) {
                            rh.getLogger().severe(traitType + " trait is not supported in this version of RealHoppers! Skipping.");
                            continue;
                        }
                        traitMap.put(type, built);
                    }
                }

                loaded.setTraits(traitMap, false);

                this.getHoppersMap().put(b, loaded);
            }

            //links first: a hopper can be linked to one stored after it, and a trait that follows
            //the link cannot run until the link has been resolved
            this.getHoppersMap().values().forEach(RHopper::loadLink);
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
        //anything pointing at the hopper that is going away is left pointing at nothing, so the
        //link is dropped and whatever followed it stops
        for (RHopper hopper : this.getHoppers()) {
            if (hopper.getLink() == h) {
                hopper.removeLink();
            }
        }

        this.getHoppersMap().remove(h.getBlock());
    }

    @Override
    public void tick() {
        for (final RHopper hopper : this.getHoppers()) {
            //a hopper in an unloaded chunk is left entirely alone. Reading its block would pull the
            //chunk back in, and the outline it used to draw was doing exactly that.
            if (!hopper.isChunkLoaded()) {
                continue;
            }

            //a trait on a timer notices this for itself, but a hopper carrying only AUTO_SELL or
            //AUTO_SMELT has no timer, and one carrying nothing at all has nothing to notice with
            if (!hopper.isValid()) {
                rh.getLogger().info("Hopper at " + hopper.getSerializedLocation() + " is no longer a hopper. Unregistering.");
                this.delete(hopper);
                continue;
            }

            hopper.loopView();
        }
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