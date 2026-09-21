package joserodpt.realhoppers.api.hopper.trait.traits;

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
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides what the hopper is allowed to keep.
 *
 * <p>Only governs storing. Anything turned away still meets the rest of the hopper - so a filter
 * beside AUTO_SELL keeps what is listed and sells the rest, and beside VOID keeps what is listed
 * and destroys the rest.</p>
 *
 * <p>An empty list accepts everything. A filter that refused everything the moment it was switched
 * on would look like a broken hopper rather than an empty list.</p>
 */
public class RHFilterTrait extends RHopperTraitBase {

    private final Set<Material> materials = EnumSet.noneOf(Material.class);

    public RHFilterTrait(RHopper main) {
        super(main);
    }

    public Set<Material> getMaterials() {
        return this.materials;
    }

    /** Whether the hopper may keep this material. */
    public boolean accepts(final Material material) {
        return this.materials.isEmpty() || this.materials.contains(material);
    }

    public boolean add(final Material material) {
        if (!this.materials.add(material)) {
            return false;
        }
        super.getHopper().saveData(RHopper.Data.TRAITS);
        return true;
    }

    public boolean remove(final Material material) {
        if (!this.materials.remove(material)) {
            return false;
        }
        super.getHopper().saveData(RHopper.Data.TRAITS);
        return true;
    }

    @Override
    public void saveSettings() {
        super.saveSettings();
        RHHoppers.file().set(super.settingsRoute() + ".Materials",
                this.materials.stream().map(Material::name).collect(Collectors.toList()));
    }

    @Override
    public void loadSettings() {
        this.materials.clear();

        final List<String> saved = new ArrayList<>(RHHoppers.file().getStringList(super.settingsRoute() + ".Materials"));
        for (final String name : saved) {
            try {
                this.materials.add(Material.valueOf(name));
            } catch (final IllegalArgumentException e) {
                //a material this server does not have, from a hand-edited file or an older version
                RealHoppersAPI.getInstance().getLogger().warning(name
                        + " in the filter of the hopper at " + super.getHopper().getSerializedLocation()
                        + " is not a material! Skipping.");
            }
        }
    }

    @Override
    public void executeAction(Player p) { }

    @Override
    protected void executeLoop() { }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.FILTER;
    }

    @Override
    public void stopTask() {
        //nothing scheduled to cancel
    }
}
