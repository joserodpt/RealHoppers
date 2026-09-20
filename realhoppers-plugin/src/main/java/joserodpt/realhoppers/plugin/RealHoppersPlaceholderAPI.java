package joserodpt.realhoppers.plugin;

/*
 *   ____            _ _   _
 *  |  _ \ ___  __ _| | | | | ___  _ __  _ __   ___ _ __ ___
 *  | |_) / _ \/ _` | | |_| |/ _ \| '_ \| '_ \ / _ \ '__/ __|
 *  |  _ <  __/ (_| | |  _  | (_) | |_) | |_) |  __/ |  \__ \
 *  |_| \_\___|\__,_|_|_| |_|\___/| .__/| .__/ \___|_|  |___/
 *                                |_|   |_|
 *
 * Licensed under the MIT License
 * @author José Rodrigues © 2019-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.utils.Text;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class RealHoppersPlaceholderAPI extends PlaceholderExpansion {

    private final RealHoppers plugin;

    public RealHoppersPlaceholderAPI(final RealHoppers plugin) {
        this.plugin = plugin;
    }

    /**
     * Registered from inside the plugin, so it has to survive a PlaceholderAPI reload rather than
     * being unregistered with the external expansions.
     */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return this.plugin.getPlugin().getDescription().getAuthors().toString();
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "realhoppers";
    }

    @Override
    @NotNull
    public String getVersion() {
        return this.plugin.getVersion();
    }

    @Override
    public String onRequest(final OfflinePlayer player, final String identifier) {
        //asked for on every scoreboard tick, so everything here reads memory only
        switch (identifier) {
            case "hoppers":
                return String.valueOf(this.plugin.getHopperManager().getHoppersMap().size());
            case "balance":
                return Text.formatNumber(this.plugin.getHopperManager().getHoppers().stream()
                        .mapToDouble(RHopper::getBalance).sum());
            case "version":
                return this.plugin.getVersion();
            default:
                break;
        }

        //%realhoppers_trait_<TRAIT>% - how many hoppers carry that trait
        if (identifier.startsWith("trait_")) {
            final RHopperTrait trait;
            try {
                trait = RHopperTrait.valueOf(identifier.substring("trait_".length()).toUpperCase());
            } catch (final IllegalArgumentException e) {
                return null;
            }
            return String.valueOf(this.plugin.getHopperManager().getHoppers().stream()
                    .filter(hopper -> hopper.hasTrait(trait)).count());
        }

        return null;
    }
}
