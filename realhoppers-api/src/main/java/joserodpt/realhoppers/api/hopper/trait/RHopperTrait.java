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
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.traits.RHBlockBreakingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHDummyTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHMobKillingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHSuctionTrait;
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

    /**
     * Builds the trait implementation this constant stands for.
     *
     * <p>The command used to carry a switch over four of the seven constants, silently doing
     * nothing for the other three. Everything that attaches a trait goes through here instead, so a
     * constant with nothing behind it is one answer - null - rather than silence.</p>
     *
     * @return the trait, or null when this constant cannot be attached on its own:
     *         {@link #TELEPORT} and {@link #ITEM_TRANS} need a second hopper and are built by the
     *         linking flow, and {@link #AUTO_SMELT} has no implementation yet
     */
    public RHopperTraitBase build(final RHopper hopper) {
        switch (this) {
            case BLOCK_BREAKING:
                return new RHBlockBreakingTrait(hopper);
            case KILL_MOB:
                return new RHMobKillingTrait(hopper);
            case SUCTION:
                return new RHSuctionTrait(hopper);
            case AUTO_SELL:
                return new RHDummyTrait(hopper, AUTO_SELL);
            default:
                return null;
        }
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
