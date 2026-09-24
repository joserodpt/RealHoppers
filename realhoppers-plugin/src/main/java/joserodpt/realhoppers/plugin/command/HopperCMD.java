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
 * @author José Rodrigues © 2023-2026
 * @link https://github.com/joserodpt/RealHoppers
 */

import joserodpt.realhoppers.api.config.TranslatableLine;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.RHopperAccess;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import joserodpt.realhoppers.api.utils.Text;
import joserodpt.realhoppers.plugin.RealHoppers;
import joserodpt.realhoppers.plugin.managers.HopperOwnership;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.CommandPlaceholder;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Single;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.annotation.SuggestWith;
import revxrsal.commands.annotation.Usage;
import revxrsal.commands.autocomplete.SuggestionProvider;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.node.ExecutionContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.NAME;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.PLAYER;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.TRAIT;
import static joserodpt.realhoppers.api.config.TranslatableLine.TranslatableLinePlaceholder.VALUE;

@Command({"realhoppers", "rh"})
public class HopperCMD {

    /** How far ahead of the player the commands that act on a hopper look for one. */
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
    @Usage("&c/rh settrait <trait> [tier]")
    @SuppressWarnings("unused")
    public void setTrait(final Player p, final RHopperTrait trait, @Optional final Integer tier) {
        final Block b = p.getTargetBlock(null, REACH);
        final RHopper hopper = rh.getHopperManager().getHopper(b);

        if (hopper == null) {
            TranslatableLine.HOPPER_NOT_LOOKING_AT.send(p);
            return;
        }

        if (hopper.hasTrait(trait)) {
            TranslatableLine.TRAIT_ALREADY_PRESENT
                    .with(TRAIT, trait.getName()).send(p);
            return;
        }

        final RHopperTraitBase built = trait.build(hopper);
        if (built == null) {
            //a constant with no implementation behind it. The old switch covered four of the seven
            //and did nothing at all for the rest.
            TranslatableLine.TRAIT_UNAVAILABLE
                    .with(TRAIT, trait.getName()).send(p);
            return;
        }

        hopper.setTrait(trait, built);
        TranslatableLine.TRAIT_ADDED
                .with(TRAIT, trait.getName()).send(p);

        if (tier != null) {
            if (!trait.isScalable()) {
                TranslatableLine.TRAIT_NOT_SCALABLE
                        .with(TRAIT, trait.getName()).send(p);
            } else {
                //clamped rather than refused, so /rh settrait SUCTION 99 gives the top tier
                final int set = hopper.setTraitTier(trait, tier);
                TranslatableLine.TRAIT_TIER_SET
                        .with(TRAIT, trait.getName())
                        .with(VALUE, String.valueOf(set)).send(p);
            }
        }

        //the trait is on either way, it just sits idle until the hopper has somewhere to point
        if (trait.requiresLink() && !hopper.hasLink()) {
            TranslatableLine.TRAIT_NEEDS_LINK
                    .with(TRAIT, trait.getName()).send(p);
        }
    }

    /** The hopper the player is looking at, or null having told them there is none. */
    private RHopper targetHopper(final Player p) {
        final RHopper hopper = rh.getHopperManager().getHopper(p.getTargetBlock(null, REACH));
        if (hopper == null) {
            TranslatableLine.HOPPER_NOT_LOOKING_AT.send(p);
        }
        return hopper;
    }

    @Subcommand("info")
    @SuppressWarnings("unused")
    public void info(final Player p) {
        final RHopper hopper = this.targetHopper(p);
        if (hopper == null) {
            return;
        }

        if (!hopper.canAccess(p)) {
            TranslatableLine.ACCESS_DENIED
                    .with(NAME, hopper.getName())
                    .with(PLAYER, hopper.getOwnerDisplayName()).send(p);
            return;
        }

        final String whitelist = hopper.getWhitelist().isEmpty()
                ? TranslatableLine.WHITELIST_EMPTY.get()
                : String.join(", ", hopper.getWhitelist().values());

        Text.sendList(p, Arrays.asList(
                TranslatableLine.INFO_HEADER.with(NAME, hopper.getName()).get(),
                TranslatableLine.INFO_OWNER.with(PLAYER, hopper.getOwnerDisplayName()).get(),
                TranslatableLine.INFO_ACCESS.with(VALUE, hopper.getAccess().getDisplayName()).get(),
                TranslatableLine.INFO_LOCATION.with(VALUE, Text.cords(hopper.getLocation())).get(),
                TranslatableLine.INFO_WHITELIST.with(VALUE, whitelist).get()));
    }

    /** The last String is greedy, which is what lets a name have spaces in it. */
    @Subcommand("name")
    @Usage("&c/rh name <name>")
    @SuppressWarnings("unused")
    public void name(final Player p, final String name) {
        final RHopper hopper = this.targetHopper(p);
        if (hopper != null) {
            HopperOwnership.rename(p, hopper, name);
        }
    }

    @Subcommand("access")
    @Usage("&c/rh access <public|private>")
    @SuppressWarnings("unused")
    public void access(final Player p, final RHopperAccess access) {
        final RHopper hopper = this.targetHopper(p);
        if (hopper != null) {
            HopperOwnership.setAccess(p, hopper, access);
        }
    }

    @Subcommand("whitelist add")
    @Usage("&c/rh whitelist add <player>")
    @SuppressWarnings("unused")
    public void whitelistAdd(final Player p, @Single @SuggestWith(OnlinePlayers.class) final String player) {
        final RHopper hopper = this.targetHopper(p);
        if (hopper != null) {
            HopperOwnership.addToWhitelist(p, hopper, player);
        }
    }

    @Subcommand("whitelist remove")
    @Usage("&c/rh whitelist remove <player>")
    @SuppressWarnings("unused")
    public void whitelistRemove(final Player p, @Single @SuggestWith(OnlinePlayers.class) final String player) {
        final RHopper hopper = this.targetHopper(p);
        if (hopper != null) {
            HopperOwnership.removeFromWhitelist(p, hopper, player);
        }
    }

    @Subcommand("whitelist list")
    @SuppressWarnings("unused")
    public void whitelistList(final Player p) {
        final RHopper hopper = this.targetHopper(p);
        if (hopper == null || !HopperOwnership.checkManage(p, hopper)) {
            return;
        }

        if (hopper.getWhitelist().isEmpty()) {
            TranslatableLine.WHITELIST_EMPTY.send(p);
            return;
        }

        TranslatableLine.WHITELIST_LIST_HEADER
                .with(NAME, hopper.getName())
                .with(VALUE, String.valueOf(hopper.getWhitelist().size())).send(p);
        Text.sendList(p, hopper.getWhitelist().entrySet().stream()
                .map(entry -> TranslatableLine.WHITELIST_LIST_ENTRY
                        .with(PLAYER, entry.getValue() == null
                                ? entry.getKey().toString() : entry.getValue()).get())
                .collect(Collectors.toList()));
    }

    /**
     * The hoppers a player owns: your own with no argument, anyone's with realhoppers.admin. Read
     * from the database, so hoppers in worlds that are not loaded are listed too.
     */
    @Subcommand("list")
    @Usage("&c/rh list [player]")
    @SuppressWarnings("unused")
    public void list(final CommandSender sender, @Optional @Single @SuggestWith(OnlinePlayers.class) final String player) {
        final OfflinePlayer target;
        if (player == null) {
            if (!(sender instanceof Player)) {
                TranslatableLine.SYSTEM_PLAYER_ONLY.send(sender);
                return;
            }
            target = (Player) sender;
        } else {
            if (!sender.hasPermission(RHopper.ADMIN_PERMISSION)
                    && !(sender instanceof Player && ((Player) sender).getName().equalsIgnoreCase(player))) {
                TranslatableLine.SYSTEM_ERROR_PERMISSION.send(sender);
                return;
            }
            target = HopperOwnership.findPlayer(player);
            if (target == null) {
                TranslatableLine.LIST_PLAYER_NOT_FOUND
                        .with(PLAYER, player).send(sender);
                return;
            }
        }

        final String targetName = target.getName() == null ? player : target.getName();
        rh.getDatabaseManager().getOwnedHoppers(target.getUniqueId()).thenAccept(owned ->
                //back on the main thread to talk to the player
                Bukkit.getScheduler().runTask(rh.getPlugin(), () -> {
                    if (owned.isEmpty()) {
                        TranslatableLine.LIST_EMPTY
                                .with(PLAYER, targetName).send(sender);
                        return;
                    }

                    TranslatableLine.LIST_HEADER
                            .with(PLAYER, targetName)
                            .with(VALUE, String.valueOf(owned.size())).send(sender);
                    Text.sendList(sender, owned.stream().map(h -> TranslatableLine.LIST_ENTRY
                            .with(NAME, h.getName())
                            .with(VALUE, h.getLocation().replace(":", " ")
                                    + " &8- " + RHopperAccess.parse(h.getAccess()).getDisplayName()).get())
                            .collect(Collectors.toList()));
                }));
    }

    /** Tab-completes the names of the players online. */
    public static class OnlinePlayers implements SuggestionProvider<BukkitCommandActor> {
        @Override
        public Collection<String> getSuggestions(final ExecutionContext<BukkitCommandActor> context) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(HumanEntity::getName)
                    .collect(Collectors.toList());
        }
    }
}
