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
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.RHopperAccess;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.managers.HopperManagerAPI;
import joserodpt.realhoppers.api.utils.LocationUtil;
import joserodpt.realhoppers.plugin.RealHoppers;
import joserodpt.realhoppers.plugin.database.HopperRow;
import joserodpt.realhoppers.plugin.database.HopperTraitRow;
import joserodpt.realhoppers.plugin.database.HopperWhitelistRow;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class HopperManager extends HopperManagerAPI {
    private final RealHoppers rh;

    public HopperManager(RealHoppers rh) {
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
    public List<RHopper> getHoppersOwnedBy(final UUID owner) {
        return this.getHoppersMap().values().stream()
                .filter(hopper -> owner.equals(hopper.getOwner()))
                .collect(Collectors.toList());
    }


    @Override
    public void loadHoppers() {
        //reload comes through here too, and the hoppers already in the map own scheduled tasks.
        //Dropping them without stopping first left every task running against an orphaned hopper.
        this.stopHoppers();
        this.getHoppersMap().clear();
        for (final DatabaseManager.StoredHopper stored : rh.getDatabaseManager().loadAll()) {
            //one row that trips something up costs that hopper, not every one after it
            try {
                this.loadHopper(stored);
            } catch (final RuntimeException e) {
                rh.getLogger().severe("Could not load the hopper at " + stored.getHopper().getLocation() + ": " + e + ". Skipping.");
            }
        }

        //links first: a hopper can be linked to one stored after it, and a trait that follows
        //the link cannot run until the link has been resolved
        this.getHoppersMap().values().forEach(RHopper::loadLink);
        this.getHoppersMap().values().forEach(RHopper::startTraitTasks);

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

    private void loadHopper(final DatabaseManager.StoredHopper stored) {
        final HopperRow row = stored.getHopper();

        //a world that is not loaded, or a block that is no longer a hopper, is skipped but left
        //in the database: unloading a world for a while should not cost its hoppers
        final Location l = LocationUtil.deserializeLocation(row.getLocation());
        if (l == null) {
            rh.getLogger().warning("Could not find the world of the hopper at " + row.getLocation() + "! Skipping.");
            return;
        }

        final Block b = l.getBlock();
        if (b.getType() != Material.HOPPER) {
            rh.getLogger().warning("Block at location " + row.getLocation() + " isn't a Hopper! Skipping.");
            return;
        }

        final RHopper loaded = new RHopper(b, false);
        //not the setters: those fire state change events and queue writes, for values that
        //were just read out of the database
        loaded.restoreName(row.getName());
        loaded.restoreOwner(row.getOwnerUUID(), row.getOwnerName());
        loaded.restoreAccess(RHopperAccess.parse(row.getAccess()));
        loaded.restoreCreatedAt(row.getCreatedAt());
        loaded.restoreBalance(row.getBalance());
        loaded.restoreXp(row.getXp());
        loaded.setLinkLocation(row.getLink());

        for (final HopperWhitelistRow entry : stored.getWhitelist()) {
            loaded.restoreWhitelisted(entry.getPlayerUUID(), entry.getPlayerName());
        }

        final Map<RHopperTrait, RHopperTraitBase> traitMap = new HashMap<>();
        for (final HopperTraitRow trait : stored.getTraits()) {
            putTrait(traitMap, loaded, trait.getTrait(), trait.getTier(), trait.getSettings());
        }
        loaded.setTraits(traitMap, false);

        this.getHoppersMap().put(b, loaded);
    }

    /**
     * Builds one trait onto a hopper being read from the database, at its stored tier and with
     * whatever else it stored for itself.
     */
    private void putTrait(final Map<RHopperTrait, RHopperTraitBase> traitMap, final RHopper hopper,
                          final String name, final int tier, final String settings) {
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
        built.deserializeSettings(settings);
        traitMap.put(type, built);
    }

    @Override
    public void delete(RHopper h) {
        h.stopHopper();
        //out of the map before the database is told, so the flush cannot write it back. Only if it
        //is still this object: a hopper from before a reload must not take its replacement with it
        this.getHoppersMap().remove(h.getBlock(), h);
        rh.getDatabaseManager().delete(h);
        //anything pointing at the hopper that is going away is left pointing at nothing, so the
        //link is dropped and whatever followed it stops
        for (RHopper hopper : this.getHoppers()) {
            if (hopper.getLink() == h) {
                hopper.removeLink();
            }
        }
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

        }
    }

    @Override
    public void stopHoppers() {
        //stopHopper only marks the hopper; the flush task, or close on shutdown, writes it
        this.getHoppers().forEach(RHopper::stopHopper);
    }

    @Override
    public Map<Material, Double> getMaterialCost() {
        return materialCost;
    }

}