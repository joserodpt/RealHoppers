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


import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.traits.RHBlockBreakingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHFilterTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHGrowTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHHarvestTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHMobPullTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoCompactTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSellTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHAutoSmeltTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHItemTransferTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHMobKillingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHSuctionTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHTeleportationTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHVoidTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHXpCollectTrait;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum RHopperTrait {
    TELEPORT(Material.ENDER_PEARL, false, true, false),
    ITEM_TRANSF(Material.ENDER_EYE, false, true, true),
    AUTO_SELL(Material.EMERALD, true, false, true),
    AUTO_SMELT(Material.FURNACE, false, false, false),
    SUCTION(Material.FEATHER, false, false, true),
    BLOCK_BREAKING(Material.TNT, false, false, true),
    KILL_MOB(Material.DIAMOND_SWORD, true, false, true),
    VOID(Material.LAVA_BUCKET, false, false, false),
    MOB_PULL(Material.LEAD, false, false, true),
    HARVEST(Material.DIAMOND_HOE, false, false, true),
    GROW(Material.BONE_MEAL, false, false, true),
    AUTO_COMPACT(Material.IRON_BLOCK, false, false, true),
    XP_COLLECT(Material.EXPERIENCE_BOTTLE, false, false, true),
    FILTER(Material.HOPPER, false, false, false);

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

    /**
     * What each of this trait's tiers is worth and what it costs, read from config.yml.
     *
     * <p>Held rather than read per use because the suction radius and the rest are asked for on
     * every cycle of every hopper. {@link #loadTiers()} fills it on startup and on reload.</p>
     */
    private final Map<Integer, Tier> tiers = new HashMap<>();

    /**
     * Reads every trait's tier table out of config.yml. Call once the config is up, and again after
     * a reload.
     */
    public static void loadTiers() {
        for (final RHopperTrait trait : values()) {
            trait.tiers.clear();

            if (!trait.scalable) {
                //one tier, free, doing exactly what the config says
                trait.tiers.put(1, new Tier(1D, 0D));
                continue;
            }

            final String route = "RealHoppers.Traits." + trait.name() + ".Tiers";
            if (RHConfig.file().isSection(route)) {
                //keys are read as text on purpose: an unquoted 2 in YAML is a number, and looking
                //it up as a string route would then miss
                for (final String key : RHConfig.file().getSection(route).getRoutesAsStrings(false)) {
                    final int number;
                    try {
                        number = Integer.parseInt(key.trim());
                    } catch (final NumberFormatException e) {
                        RealHoppersAPI.getInstance().getLogger()
                                .severe("Tier '" + key + "' of the " + trait.name() + " trait is not a number! Skipping.");
                        continue;
                    }
                    trait.tiers.put(number, new Tier(
                            RHConfig.file().getDouble(route + "." + key + ".Power", (double) number),
                            RHConfig.file().getDouble(route + "." + key + ".Price", 0D)));
                }
            }

            if (trait.tiers.isEmpty()) {
                //no table for this trait: fall back to what the tier number itself says, free
                RealHoppersAPI.getInstance().getLogger()
                        .warning("The " + trait.name() + " trait has no Tiers in config.yml. Using tier 1 only.");
                trait.tiers.put(1, new Tier(1D, 0D));
            }
        }
    }

    /** The highest tier this trait can be raised to, which is however many are configured. */
    public int getMaxTier() {
        return this.tiers.keySet().stream().mapToInt(Integer::intValue).max().orElse(1);
    }

    /**
     * What a tier multiplies the trait's configured value by. A tier with no entry is worth its own
     * number, which is the linear behaviour a config without a tier table gets.
     */
    public double getTierPower(final int tier) {
        final Tier spec = this.tiers.get(tier);
        return spec == null ? Math.max(1, tier) : spec.power;
    }

    /** What upgrading to a tier costs. Tier 1 is where a trait starts, so it is never charged. */
    public double getTierPrice(final int tier) {
        final Tier spec = this.tiers.get(tier);
        return spec == null ? 0D : spec.price;
    }

    /** One row of a trait's tier table. */
    private static final class Tier {
        private final double power;
        private final double price;

        private Tier(final double power, final double price) {
            this.power = power;
            this.price = price;
        }
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
     * <p>{@link #TELEPORT} and {@link #ITEM_TRANSF} build the same as the rest: the hopper they
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
            case ITEM_TRANSF:
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
            case VOID:
                return new RHVoidTrait(hopper);
            case MOB_PULL:
                return new RHMobPullTrait(hopper);
            case HARVEST:
                return new RHHarvestTrait(hopper);
            case GROW:
                return new RHGrowTrait(hopper);
            case AUTO_COMPACT:
                return new RHAutoCompactTrait(hopper);
            case XP_COLLECT:
                return new RHXpCollectTrait(hopper);
            case FILTER:
                return new RHFilterTrait(hopper);
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

    /**
     * What the trait does, in a line or two, for the icon in the trait screen. A constant with no
     * entry simply shows nothing rather than a gap where a description should be.
     */
    public List<String> getDescription() {
        return RHLanguage.file().getStringList("Trait-Descriptions." + this.name());
    }
}
