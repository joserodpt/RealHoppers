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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInput implements Listener {

    //read and taken from the async chat thread, written from the main thread
    private static final Map<UUID, PlayerInput> inputs = new ConcurrentHashMap<>();
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

                //taken in one step, so two quick messages can't both answer the same prompt
                final PlayerInput current = inputs.remove(p.getUniqueId());
                if (current != null) {
                    event.setCancelled(true);
                    //this is the async chat thread: the task, the title and the callbacks belong on the main one
                    Bukkit.getScheduler().runTask(RealHoppersAPI.getInstance().getPlugin(),
                            () -> handlePlayerInput(p, input, current));
                }
            }

            @EventHandler
            public void onQuit(final PlayerQuitEvent event) {
                //otherwise the title task runs forever and their first chat line after rejoining answers
                //a prompt from a previous session
                final PlayerInput current = inputs.remove(event.getPlayer().getUniqueId());
                if (current != null) {
                    current.taskId.cancel();
                }
            }
        };
    }

    /** Drops every unanswered prompt, for a reload: their callbacks hold hoppers that are about to be replaced. */
    public static void cancelAll() {
        for (final UUID uuid : inputs.keySet()) {
            final PlayerInput current = inputs.remove(uuid);
            if (current != null) {
                current.taskId.cancel();
                final Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    p.sendTitle("", "", 0, 1, 0);
                }
            }
        }
    }

    private static void handlePlayerInput(final Player p, String input, final PlayerInput current) {
        if (current.clearInput) {
            input = ChatColor.stripColor(Text.color(input)).trim();
        }

        try {
            current.taskId.cancel();
            p.sendTitle("", "", 0, 1, 0);
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
        final PlayerInput previous = inputs.put(this.uuid, this);
        //a new prompt replaces an unanswered one, whose title task would otherwise never stop
        if (previous != null) {
            previous.taskId.cancel();
        }
    }

    @FunctionalInterface
    public interface InputRunnable {
        void run(String input);
    }
}
