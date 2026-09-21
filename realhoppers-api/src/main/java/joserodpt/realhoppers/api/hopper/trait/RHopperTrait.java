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


import joserodpt.realhoppers.api.config.RHConfig;
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
    TELEPORT(Material.ENDER_PEARL, false, true, false),
    ITEM_TRANS(Material.ENDER_EYE, false, true, true),
    AUTO_SELL(Material.EMERALD, true, false, true),
    AUTO_SMELT(Material.FURNACE, false, false, false),
    SUCTION(Material.FEATHER, false, false, true),
    BLOCK_BREAKING(Material.TNT, false, false, true),
    KILL_MOB(Material.DIAMOND_SWORD, true, false, true);

    private final Material icon;
    private final boolean hasEconomyCapabilities;
    private final boolean requiresLink;
    private final boolean scalable;

    RHopperTrait(Material icon, boolean hasEconomyCapabilities, boolean requiresLink, boolean scalable) {
        this.icon = icon;
        this.hasEconomyCapabilities = hasEconomyCapabilities;
        this.requiresLink = requiresLink;
        this.scalable = scalable;
    }

    /**
     * Whether raising this trait's tier does anything.
     *
     * <p>False for {@link #TELEPORT} and {@link #AUTO_SMELT}, which have nothing to multiply: there
     * is one destination to send a player to, and an item either has a furnace recipe or it does
     * not. Both are pinned at tier 1 rather than offering a number that would change nothing.</p>
     */
    public boolean isScalable() {
        return this.scalable;
    }

    /** The highest tier this trait can be raised to. */
    public int getMaxTier() {
        if (!this.scalable) {
            return 1;
        }
        return Math.max(1, RHConfig.file().getInt("RealHoppers.Traits.Max-Tier", 5));
    }

    /**
     * A number this trait is configured with, read fresh so that {@code /rh reload} takes effect
     * without every hopper having to be rebuilt.
     *
     * @param key      the key under {@code RealHoppers.Traits.<TRAIT>}
     * @param fallback used when the key is missing, which is what an older config.yml looks like
     */
    public double configValue(final String key, final double fallback) {
        return RHConfig.file().getDouble("RealHoppers.Traits." + this.name() + "." + key, fallback);
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
