package joserodpt.realhoppers.plugin.managers;

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
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.RHopperAccess;
import joserodpt.realhoppers.api.utils.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

/**
 * Renaming a hopper, changing its access and editing its whitelist - the same rules whether it is
 * done from the hopper's screen or with /rh. Each one checks the player may manage the hopper,
 * tells them what happened, and returns whether it did.
 */
public final class HopperOwnership {

    private HopperOwnership() {
    }

    public static boolean checkManage(final Player p, final RHopper hopper) {
        if (hopper.canManage(p)) {
            return true;
        }
        TranslatableLine.ACCESS_NO_MANAGE.send(p);
        return false;
    }

    /** Colour codes are kept, and do not count towards the length limit. */
    public static boolean rename(final Player p, final RHopper hopper, final String name) {
        if (!checkManage(p, hopper)) {
            return false;
        }

        final String trimmed = name == null ? "" : name.trim();
        final String visible = ChatColor.stripColor(Text.color(trimmed)).trim();
        if (visible.isEmpty()) {
            TranslatableLine.NAME_EMPTY.send(p);
            return false;
        }

        final int max = RHConfig.file().getInt("RealHoppers.Hoppers.Name-Max-Length", 32);
        if (visible.length() > max) {
            TranslatableLine.NAME_TOO_LONG
                    .setV1(TranslatableLine.ReplacableVar.VALUE.eq(String.valueOf(max))).send(p);
            return false;
        }

        hopper.setName(trimmed);
        TranslatableLine.NAME_SET.setV1(TranslatableLine.ReplacableVar.NAME.eq(trimmed)).send(p);
        return true;
    }

    public static boolean setAccess(final Player p, final RHopper hopper, final RHopperAccess access) {
        if (!checkManage(p, hopper)) {
            return false;
        }

        hopper.setAccess(access);
        TranslatableLine.ACCESS_SET
                .setV1(TranslatableLine.ReplacableVar.NAME.eq(hopper.getName()))
                .setV2(TranslatableLine.ReplacableVar.VALUE.eq(access.getDisplayName())).send(p);
        return true;
    }

    public static boolean addToWhitelist(final Player p, final RHopper hopper, final String playerName) {
        if (!checkManage(p, hopper)) {
            return false;
        }

        final OfflinePlayer target = findPlayer(playerName);
        if (target == null) {
            TranslatableLine.WHITELIST_NOT_FOUND
                    .setV1(TranslatableLine.ReplacableVar.PLAYER.eq(playerName)).send(p);
            return false;
        }

        if (target.getUniqueId().equals(hopper.getOwner())) {
            TranslatableLine.WHITELIST_IS_OWNER.send(p);
            return false;
        }

        if (hopper.isWhitelisted(target.getUniqueId())) {
            TranslatableLine.WHITELIST_ALREADY
                    .setV1(TranslatableLine.ReplacableVar.PLAYER.eq(target.getName())).send(p);
            return false;
        }

        final int max = RHConfig.file().getInt("RealHoppers.Hoppers.Whitelist-Max-Size", 27);
        if (hopper.getWhitelist().size() >= max) {
            TranslatableLine.WHITELIST_FULL
                    .setV1(TranslatableLine.ReplacableVar.VALUE.eq(String.valueOf(max))).send(p);
            return false;
        }

        hopper.addToWhitelist(target);
        TranslatableLine.WHITELIST_ADDED
                .setV1(TranslatableLine.ReplacableVar.PLAYER.eq(target.getName()))
                .setV2(TranslatableLine.ReplacableVar.NAME.eq(hopper.getName())).send(p);
        return true;
    }

    /** Found by the name stored on the whitelist first, so a player who has since renamed can still be removed. */
    public static boolean removeFromWhitelist(final Player p, final RHopper hopper, final String playerName) {
        if (!checkManage(p, hopper)) {
            return false;
        }

        UUID uuid = null;
        String shown = playerName;
        for (final Map.Entry<UUID, String> entry : hopper.getWhitelist().entrySet()) {
            if (entry.getValue() != null && entry.getValue().equalsIgnoreCase(playerName)) {
                uuid = entry.getKey();
                shown = entry.getValue();
                break;
            }
        }
        if (uuid == null) {
            final OfflinePlayer target = findPlayer(playerName);
            if (target != null) {
                uuid = target.getUniqueId();
            }
        }

        return removeFromWhitelist(p, hopper, uuid, shown);
    }

    public static boolean removeFromWhitelist(final Player p, final RHopper hopper, final UUID uuid, final String shown) {
        if (!checkManage(p, hopper)) {
            return false;
        }

        if (uuid == null || !hopper.removeFromWhitelist(uuid)) {
            TranslatableLine.WHITELIST_NOT_ON
                    .setV1(TranslatableLine.ReplacableVar.PLAYER.eq(shown)).send(p);
            return false;
        }

        TranslatableLine.WHITELIST_REMOVED
                .setV1(TranslatableLine.ReplacableVar.PLAYER.eq(shown))
                .setV2(TranslatableLine.ReplacableVar.NAME.eq(hopper.getName())).send(p);
        return true;
    }

    /**
     * A player who is online or has played here before, by name, ignoring case. Never asks Mojang:
     * Bukkit.getOfflinePlayer(String) can, and would block the main thread doing it.
     */
    public static OfflinePlayer findPlayer(final String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        final Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            return online;
        }

        for (final OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (name.equalsIgnoreCase(offline.getName())) {
                return offline;
            }
        }
        return null;
    }
}
