package joserodpt.realhoppers.api.utils;

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
import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.config.TranslatableLine;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerInput implements Listener {

    private static final Map<UUID, PlayerInput> inputs = new HashMap<>();
    private final UUID uuid;

    private final List<String> texts;

    private final InputRunnable runGo;
    private final InputRunnable runCancel;
    private final BukkitTask taskId;
    private boolean clearInput = true;

    public PlayerInput(final boolean clearInput, final Player p, final InputRunnable correct, final InputRunnable cancel) {
        this(clearInput, p, RHLanguage.file().getStringList("System.Type-Input"), correct, cancel);
    }

    /**
     * Waits for the player to type something in chat, with its own two title lines in place of the
     * search prompt.
     *
     * @param clearInput strip colour codes from what is typed. Without it, {@code &} codes reach
     *                   the runnable as typed, for input such as a hopper's name that may be coloured
     */
    public PlayerInput(final boolean clearInput, final Player p, final List<String> titles,
                       final InputRunnable correct, final InputRunnable cancel) {
        this.texts = Text.color(titles.size() >= 2 ? titles : RHLanguage.file().getStringList("System.Type-Input"));
        this.uuid = p.getUniqueId();
        p.closeInventory();
        this.runGo = correct;
        this.runCancel = cancel;
        this.clearInput = clearInput;
        this.taskId = new BukkitRunnable() {
            public void run() {
                p.getPlayer().sendTitle(PlayerInput.this.texts.get(0), PlayerInput.this.texts.get(1), 0, 21, 0);
            }
        }.runTaskTimer(RealHoppersAPI.getInstance().getPlugin(), 0L, 20);

        this.register();
    }

    public static Listener getListener() {
        return new Listener() {
            @EventHandler(priority = EventPriority.HIGHEST)
            public void onPlayerChat(final AsyncPlayerChatEvent event) {
                final Player p = event.getPlayer();
                final String input = event.getMessage();
                final UUID uuid = p.getUniqueId();

                if (inputs.containsKey(uuid)) {
                    event.setCancelled(true);
                    handlePlayerInput(p, input, uuid);
                }
            }
        };
    }

    private static void handlePlayerInput(final Player p, String input, final UUID uuid) {
        final PlayerInput current = inputs.get(uuid);

        if (current.clearInput) {
            input = ChatColor.stripColor(Text.color(input)).trim();
        }

        try {
            current.taskId.cancel();
            p.sendTitle("", "", 0, 1, 0);
            current.unregister();
            final String cleanInput = current.clearInput ? ChatColor.stripColor(Text.color(input)) : input.trim();
            if (input.equalsIgnoreCase("cancel")) {
                TranslatableLine.SYSTEM_INPUT_CANCELLED.send(p);
                Bukkit.getScheduler().scheduleSyncDelayedTask(RealHoppersAPI.getInstance().getPlugin(), () -> current.runCancel.run(cleanInput), 3);
            } else {
                Bukkit.getScheduler().scheduleSyncDelayedTask(RealHoppersAPI.getInstance().getPlugin(), () -> current.runGo.run(cleanInput), 3);
            }
        } catch (final Exception e) {
            TranslatableLine.SYSTEM_ERROR_OCCURRED.send(p);
            RealHoppersAPI.getInstance().getPlugin().getLogger().warning(e.getMessage());
        }
    }

    private void register() {
        inputs.put(this.uuid, this);
    }

    private void unregister() {
        inputs.remove(this.uuid);
    }

    @FunctionalInterface
    public interface InputRunnable {
        void run(String input);
    }
}
