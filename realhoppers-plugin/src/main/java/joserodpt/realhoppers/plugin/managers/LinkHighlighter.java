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

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Outlines the hoppers a player is linking, with the same particle edges RealMines draws around a
 * mine. Only the player doing the linking sees them - it is their selection, nobody else's.
 */
public class LinkHighlighter {

    /** How often the outline is redrawn. Dust particles fade on their own, so it has to be. */
    private static final long DRAW_INTERVAL_TICKS = 10L;
    /** How long a finished link stays outlined, so the player sees what they just linked. */
    private static final long LINGER_TICKS = 3 * 20L;
    private static final double STEP = 0.25D;

    private final Plugin plugin;
    private final Map<UUID, Highlight> highlights = new HashMap<>();

    public LinkHighlighter(final Plugin plugin) {
        this.plugin = plugin;
    }

    private static final class Highlight {
        private final Map<Block, Color> blocks = new LinkedHashMap<>();
        private BukkitTask draw;
        private BukkitTask stop;
    }

    /**
     * Outlines a block for this player until {@link #stop(Player)} or {@link #stopLater(Player)}.
     * Highlighting again during the linger of a finished link keeps it going instead.
     */
    public void highlight(final Player player, final Block block, final Color color) {
        final UUID uuid = player.getUniqueId();
        final Highlight h = this.highlights.computeIfAbsent(uuid, u -> new Highlight());
        if (h.stop != null) {
            h.stop.cancel();
            h.stop = null;
        }
        h.blocks.put(block, color);
        if (h.draw == null) {
            h.draw = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> this.draw(uuid), 0L, DRAW_INTERVAL_TICKS);
        }
    }

    /** Leaves the outline up for a few more seconds, then takes it down. */
    public void stopLater(final Player player) {
        final UUID uuid = player.getUniqueId();
        final Highlight h = this.highlights.get(uuid);
        if (h == null) {
            return;
        }
        if (h.stop != null) {
            h.stop.cancel();
        }
        h.stop = Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.stop(uuid), LINGER_TICKS);
    }

    public void stop(final Player player) {
        this.stop(player.getUniqueId());
    }

    public void stop(final UUID uuid) {
        final Highlight h = this.highlights.remove(uuid);
        if (h == null) {
            return;
        }
        if (h.draw != null) {
            h.draw.cancel();
        }
        if (h.stop != null) {
            h.stop.cancel();
        }
    }

    private void draw(final UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        final Highlight h = this.highlights.get(uuid);
        if (player == null || h == null) {
            this.stop(uuid);
            return;
        }
        h.blocks.forEach((block, color) -> {
            //a hopper in another world, or one the player walked away from, draws nothing anyway
            if (block.getWorld() == player.getWorld()) {
                this.drawEdges(player, block, new Particle.DustOptions(color, 1));
            }
        });
    }

    /** Walks the 12 edges of the block. The X edges own the corners, so none repeats. */
    private void drawEdges(final Player player, final Block block, final Particle.DustOptions dust) {
        final double minX = block.getX(), minY = block.getY(), minZ = block.getZ();
        final double maxX = minX + 1, maxY = minY + 1, maxZ = minZ + 1;
        for (final double y : new double[]{minY, maxY}) {
            for (final double z : new double[]{minZ, maxZ}) {
                for (double x = minX; x <= maxX; x += STEP) {
                    player.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0, 0, 0, 0, dust);
                }
            }
        }
        for (final double x : new double[]{minX, maxX}) {
            for (final double z : new double[]{minZ, maxZ}) {
                for (double y = minY + STEP; y < maxY; y += STEP) {
                    player.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0, 0, 0, 0, dust);
                }
            }
            for (final double y : new double[]{minY, maxY}) {
                for (double z = minZ + STEP; z < maxZ; z += STEP) {
                    player.spawnParticle(Particle.REDSTONE, x, y, z, 1, 0, 0, 0, 0, dust);
                }
            }
        }
    }
}
