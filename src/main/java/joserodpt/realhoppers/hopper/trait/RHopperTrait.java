package joserodpt.realhoppers.hopper.trait;

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


import joserodpt.realhoppers.config.Language;
import org.bukkit.Material;

public enum RHopperTrait {
    TELEPORT(Material.ENDER_PEARL, false),
    ITEM_TRANS(Material.ENDER_EYE, false),
    AUTO_SELL(Material.EMERALD, true),
    AUTO_SMELT(Material.FURNACE, false),
    SUCTION(Material.FEATHER, false),
    BLOCK_BREAKING(Material.TNT, false),
    KILL_MOB(Material.DIAMOND_SWORD, true);

    private final Material icon;
    private final boolean hasEconomyCapabilities;

    RHopperTrait(Material icon, boolean hasEconomyCapabilities) {
        this.icon = icon;
        this.hasEconomyCapabilities = hasEconomyCapabilities;
    }

    public boolean hasEconomyCapabilities() {
        return this.hasEconomyCapabilities;
    }

    public Material getIcon() {
        return icon;
    }

    public String getName() {
        switch (this) {
            case BLOCK_BREAKING:
                return Language.file().getString("Traits.BLOCK_BREAKING");
            case KILL_MOB:
                return Language.file().getString("Traits.KILL_MOB");
            case TELEPORT:
                return Language.file().getString("Traits.TELEPORT");
            case ITEM_TRANS:
                return Language.file().getString("Traits.ITEM_TRANS");
            case SUCTION:
                return Language.file().getString("Traits.SUCTION");
            case AUTO_SELL:
                return Language.file().getString("Traits.AUTO_SELL");
            case AUTO_SMELT:
                return Language.file().getString("Traits.AUTO_SMELT");
        }
        return "none";
    }
}
