package joserodpt.realhoppers.plugin.command;

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

import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.plugin.RealHoppers;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.Usage;
import revxrsal.commands.bukkit.annotation.CommandPermission;

import java.util.Arrays;

@Command({"realhoppers", "rh"})
public class HopperCMD {

    /** How far ahead of the player {@code settrait} looks for a hopper. */
    private static final int REACH = 5;

    private final RealHoppers rh;

    public HopperCMD(final RealHoppers rh) {
        this.rh = rh;
    }

    @CommandPlaceholder
    @SuppressWarnings("unused")
    public void defaultCommand(final CommandSender commandSender) {
        Text.sendList(commandSender, Arrays.asList("         &fReal&dHoppers",
                "         &7Release &a" + rh.getPlugin().getDescription().getVersion()));
    }

    @Subcommand({"reload", "rl"})
    @CommandPermission("realhoppers.admin")
    @SuppressWarnings("unused")
    public void reload(final CommandSender commandSender) {
        rh.reload();
        TranslatableLine.SYSTEM_RELOADED.send(commandSender);
    }

    /**
     * Attaches a trait to the hopper the player is looking at. Taking a Player rather than a
     * CommandSender is what tells console it cannot run this - Lamp raises SenderNotPlayerException
     * and the exception handler answers it.
     */
    @Subcommand("settrait")
    @CommandPermission("realhoppers.admin")
    @Usage("&c/rh settrait <trait>")
    @SuppressWarnings("unused")
    public void setTrait(final Player p, final RHopperTrait trait) {
        final Block b = p.getTargetBlock(null, REACH);
        final RHopper hopper = rh.getHopperManager().getHopper(b);

        if (hopper == null) {
            TranslatableLine.HOPPER_NOT_LOOKING_AT.send(p);
            return;
        }

        if (hopper.hasTrait(trait)) {
            TranslatableLine.TRAIT_ALREADY_PRESENT
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(p);
            return;
        }

        final RHopperTraitBase built = trait.build(hopper);
        if (built == null) {
            //TELEPORT and ITEM_TRANS need a second hopper, AUTO_SMELT has no implementation.
            //The old switch covered four constants and did nothing at all for these three.
            (trait.requiresLink() ? TranslatableLine.TRAIT_NEEDS_LINK : TranslatableLine.TRAIT_UNAVAILABLE)
                    .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(p);
            return;
        }

        hopper.setTrait(trait, built);
        TranslatableLine.TRAIT_ADDED
                .setV1(TranslatableLine.ReplacableVar.TRAIT.eq(trait.getName())).send(p);
    }
}
