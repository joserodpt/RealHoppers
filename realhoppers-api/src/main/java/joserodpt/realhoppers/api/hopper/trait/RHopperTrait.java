package joserodpt.realhoppers.api.hopper.trait;

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


import joserodpt.realhoppers.api.config.RHLanguage;
import org.bukkit.Material;

public enum RHopperTrait {
    TELEPORT(Material.ENDER_PEARL, false, true),
    ITEM_TRANS(Material.ENDER_EYE, false, true),
    AUTO_SELL(Material.EMERALD, true, false),
    AUTO_SMELT(Material.FURNACE, false, false),
    SUCTION(Material.FEATHER, false, false),
    BLOCK_BREAKING(Material.TNT, false, false),
    KILL_MOB(Material.DIAMOND_SWORD, true, false);

    private final Material icon;
    private final boolean hasEconomyCapabilities;
    private final boolean requiresLink;

    RHopperTrait(Material icon, boolean hasEconomyCapabilities, boolean requiresLink) {
        this.icon = icon;
        this.hasEconomyCapabilities = hasEconomyCapabilities;
        this.requiresLink = requiresLink;
    }

    public boolean hasEconomyCapabilities() {
        return this.hasEconomyCapabilities;
    }

    /**
     * Whether the trait is useless without a second hopper to point at. Those traits are
     * never started until their link has been resolved, because their loop dereferences it.
     */
    public boolean requiresLink() {
        return this.requiresLink;
    }

    public Material getIcon() {
        return icon;
    }

    public String getName() {
        switch (this) {
            case BLOCK_BREAKING:
                return RHLanguage.file().getString("Traits.BLOCK_BREAKING");
            case KILL_MOB:
                return RHLanguage.file().getString("Traits.KILL_MOB");
            case TELEPORT:
                return RHLanguage.file().getString("Traits.TELEPORT");
            case ITEM_TRANS:
                return RHLanguage.file().getString("Traits.ITEM_TRANS");
            case SUCTION:
                return RHLanguage.file().getString("Traits.SUCTION");
            case AUTO_SELL:
                return RHLanguage.file().getString("Traits.AUTO_SELL");
            case AUTO_SMELT:
                return RHLanguage.file().getString("Traits.AUTO_SMELT");
        }
        return "none";
    }
}
