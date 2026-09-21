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
                loaded.restoreXp(RHHoppers.file().getInt("Hoppers." + hopperSTR + ".XP", 0));

                final String traitsRoute = "Hoppers." + hopperSTR + ".Traits";
                boolean migrated = false;

                if (RHHoppers.file().isList(traitsRoute)) {
                    //the old shape: a list of names, where a linked trait carried its destination
                    //after a pipe. Both move - the destination to the hopper's own Link, the trait
                    //to a section entry with a tier - and the file is rewritten below.
                    migrated = true;
                    for (final String entry : RHHoppers.file().getStringList(traitsRoute)) {
                        final String[] split = entry.split("\\|");
                        if (split.length > 1 && !RHHoppers.file().isString("Hoppers." + hopperSTR + ".Link")) {
                            RHHoppers.file().set("Hoppers." + hopperSTR + ".Link", split[1]);
                            loaded.setLinkLocation(split[1]);
                        }
                        putTrait(traitMap, loaded, split[0], 1);
                    }
                } else if (RHHoppers.file().isSection(traitsRoute)) {
                    for (final String name : RHHoppers.file().getSection(traitsRoute).getRoutesAsStrings(false)) {
                        final String route = traitsRoute + "." + name;

                        //a trait is a section of its own settings now. A bare number is the shape
                        //before that, and means the tier with nothing else alongside it.
                        if (RHHoppers.file().isSection(route)) {
                            putTrait(traitMap, loaded, name, RHHoppers.file().getInt(route + ".Tier", 1));
                        } else {
                            migrated = true;
                            putTrait(traitMap, loaded, name, RHHoppers.file().getInt(route, 1));
                        }
                    }
                }

                loaded.setTraits(traitMap, false);

                if (migrated) {
                    loaded.saveData(RHopper.Data.TRAITS);
                }

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

    /**
     * Builds one trait onto a hopper being read from disk, at the tier the file gives it.
     */
    private void putTrait(final Map<RHopperTrait, RHopperTraitBase> traitMap, final RHopper hopper,
                          final String name, final int tier) {
        final RHopperTrait type;
        try {
            type = RHopperTrait.valueOf(name);
        } catch (final IllegalArgumentException e) {
            //valueOf used to throw straight out of the loop, so one unreadable entry cost every
            //hopper after it
            rh.getLogger().severe(name + " is not a trait RealHoppers knows! Skipping.");
            return;
        }

        //the enum builds every trait, so one added there is loaded here without this having to be
        //remembered. Forgetting it is exactly how SUCTION came back from disk as a block breaker.
        final RHopperTraitBase built = type.build(hopper);
        if (built == null) {
            rh.getLogger().severe(name + " trait is not supported in this version of RealHoppers! Skipping.");
            return;
        }

        built.setTier(tier);
        //whatever else the trait keeps for itself, read from its own section
        built.loadSettings();
        traitMap.put(type, built);
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