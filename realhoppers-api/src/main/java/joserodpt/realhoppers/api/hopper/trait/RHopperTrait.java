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
 * @author José Rodrigues © 2023-2026
 * @link https://github.com/joserodpt/RealHoppers
 */


import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.traits.RHBlockBreakingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSellTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSmeltTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHItemTransferTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHMobKillingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHSuctionTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHTeleportationTrait;
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
     * Whether the trait does nothing until its hopper is linked to another one. Such a trait can be
     * put on an unlinked hopper, it simply sits idle until there is a link to follow.
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
     * <p>{@link #TELEPORT} and {@link #ITEM_TRANS} build the same as the rest: the hopper they
     * point at is the hopper's own link, not something the trait is handed. Whether the hopper
     * actually has one is a separate question, {@link #requiresLink()}, and it decides whether the
     * trait runs rather than whether it can exist.</p>
     *
     * @return the trait, or null for a constant with no implementation behind it
     */
    public RHopperTraitBase build(final RHopper hopper) {
        switch (this) {
            case TELEPORT:
                return new RHTeleportationTrait(hopper);
            case ITEM_TRANS:
                return new RHItemTransferTrait(hopper);
            case BLOCK_BREAKING:
                return new RHBlockBreakingTrait(hopper);
            case KILL_MOB:
                return new RHMobKillingTrait(hopper);
            case SUCTION:
                return new RHSuctionTrait(hopper);
            case AUTO_SELL:
                return new RHAutoSellTrait(hopper);
            case AUTO_SMELT:
                return new RHAutoSmeltTrait(hopper);
            default:
                return null;
        }
    }

    /**
     * The trait's display name from language.yml. A constant with no entry falls back to its own
     * name, which at least says which trait it is - this used to return "none" for all of them.
     */
    public String getName() {
        return RHLanguage.file().getString("Traits." + this.name(), this.name());
    }
}
