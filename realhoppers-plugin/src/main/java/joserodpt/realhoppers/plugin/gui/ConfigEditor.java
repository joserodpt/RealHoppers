package joserodpt.realhoppers.plugin.gui;

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
import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realutils.dialog.SettingsDialog;
import joserodpt.realutils.dialog.SettingsStore;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * config.yml as dialogs, for {@code /rh settings}: a menu of categories, each its own form. There is
 * no inventory version, so on a server without dialogs the player is told to edit the file.
 *
 * <p>Everything here is read where it is used, so a save applies straight away, bar the save
 * interval, which is set when the plugin starts.</p>
 */
public final class ConfigEditor {

    private static final String ACCESS = "RealHoppers.Hoppers.Default-Access";

    private ConfigEditor() {
    }

    public static void open(final Player p) {
        settings().open(p, SettingsStore.of(RHConfig.file()::get, RHConfig.file()::set, RHConfig::save),
                () -> TranslatableLine.SYSTEM_SETTINGS_NEED_DIALOGS.send(p));
    }

    private static SettingsDialog settings() {
        final SettingsDialog settings = new SettingsDialog("&fReal&6Hoppers &8| &fSettings")
                .icon(Material.HOPPER)
                .onSave((p, category) -> TranslatableLine.SYSTEM_SETTINGS_SAVED.send(p));

        settings.category("&eGeneral", "&7Prefix, effects and saving")
                .text("RealHoppers.Prefix", "Plugin prefix", 64)
                .toggle("RealHoppers.Effects.Sounds", "Sounds")
                .toggle("RealHoppers.Effects.Particles", "Particles")
                .slider("RealHoppers.Teleportation-Cooldown", "Ticks before a teleported player can be teleported again", 0, 200, 5)
                .note("20 is a second")
                .slider("RealHoppers.Save-Interval-Seconds", "Seconds between hopper saves", 5, 600, 5).note("after a restart")
                .toggle("RealHoppers.Drop-Items-If-Full", "Drop broken blocks when the hopper is full")
                .toggle("RealHoppers.useDialogs", "Use dialogs").note("off: chat prompts");

        settings.category("&6Hoppers", "&7Names, access and limits")
                .text("RealHoppers.Hoppers.Default-Name", "Name new hoppers get", 64)
                .custom((form, p) -> form.toggle("Default-Access", "&eNew hoppers are private", isPrivate()),
                        (answers, p) -> {
                            final boolean was = isPrivate();
                            final boolean now = answers.toggle("Default-Access", was);
                            if (now == was) {
                                return false;
                            }
                            RHConfig.file().set(ACCESS, now ? "PRIVATE" : "PUBLIC");
                            return true;
                        })
                .slider("RealHoppers.Hoppers.Name-Max-Length", "Longest a hopper's name can be", 1, 64, 1)
                .note("colour codes don't count")
                .slider("RealHoppers.Hoppers.Whitelist-Max-Size", "Most players on one hopper's whitelist", 0, 54, 1)
                .text("RealHoppers.Hoppers.Max-Per-Player", "Hoppers a player can own", 16)
                .note("\"unlimited\" or a number")
                .toggle("RealHoppers.Hoppers.Place-Default-When-Limit-Reached", "Place plain hoppers once a player is at their limit")
                .toggle("RealHoppers.Hoppers.Keep-Private-Contents-On-Break", "Private hoppers keep their owner and contents when broken");

        settings.category("&bTraits", "&7What each trait is worth at tier 1")
                .description("&7Each tier's Power multiplies these; tiers and prices stay in config.yml.")
                .decimal(trait(RHopperTrait.SUCTION, "Radius"), "Suction: radius", 0.5, 16, 0.5).sprite(RHopperTrait.SUCTION.getIcon())
                .slider(trait(RHopperTrait.BLOCK_BREAKING, "Blocks"), "Block breaking: blocks each cycle", 1, 16, 1)
                .sprite(RHopperTrait.BLOCK_BREAKING.getIcon())
                .decimal(trait(RHopperTrait.KILL_MOB, "Damage"), "Kill mob: damage", 0.5, 40, 0.5).sprite(RHopperTrait.KILL_MOB.getIcon())
                .decimal(trait(RHopperTrait.KILL_MOB, "Radius"), "Kill mob: radius", 0.5, 8, 0.5).note("not multiplied by the tier")
                .sprite(RHopperTrait.KILL_MOB.getIcon())
                .slider(trait(RHopperTrait.ITEM_TRANSF, "Items"), "Item transfer: items each cycle", 1, 64, 1)
                .sprite(RHopperTrait.ITEM_TRANSF.getIcon())
                .decimal(trait(RHopperTrait.AUTO_SELL, "Price-Multiplier"), "Auto sell: price multiplier", 0.1, 10, 0.1)
                .sprite(RHopperTrait.AUTO_SELL.getIcon())
                .slider(trait(RHopperTrait.AUTO_COMPACT, "Passes"), "Auto compact: passes each cycle", 1, 8, 1)
                .sprite(RHopperTrait.AUTO_COMPACT.getIcon())
                .decimal(trait(RHopperTrait.XP_COLLECT, "Radius"), "Experience: radius", 0.5, 16, 0.5).sprite(RHopperTrait.XP_COLLECT.getIcon())
                .decimal(trait(RHopperTrait.MOB_PULL, "Radius"), "Mob pulling: radius", 0.5, 16, 0.5).sprite(RHopperTrait.MOB_PULL.getIcon())
                .slider(trait(RHopperTrait.HARVEST, "Radius"), "Harvest: radius", 1, 16, 1).sprite(RHopperTrait.HARVEST.getIcon())
                .slider(trait(RHopperTrait.GROW, "Radius"), "Growth: radius", 1, 16, 1).sprite(RHopperTrait.GROW.getIcon())
                .decimal(trait(RHopperTrait.GROW, "Chance"), "Growth: chance each cycle (%)", 0, 100, 1).sprite(RHopperTrait.GROW.getIcon());
        return settings;
    }

    private static String trait(final RHopperTrait trait, final String key) {
        return "RealHoppers.Traits." + trait.name() + "." + key;
    }

    private static boolean isPrivate() {
        return "PRIVATE".equalsIgnoreCase(RHConfig.file().getString(ACCESS, "PUBLIC"));
    }
}
