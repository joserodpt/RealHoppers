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
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerInput implements Listener {

    //read and taken from the async chat thread, written from the main thread
    private static final Map<UUID, PlayerInput> inputs = new ConcurrentHashMap<>();
    /**
     * Whether {@link DialogInput} is in use. Nothing touches that class unless this is set: it
     * refers to UniDialog, which is built for Java 21, so on an older server even loading it fails.
     */
    private static boolean dialogs = false;
    private final UUID uuid;

    private final List<String> texts;

    private final InputRunnable runGo;
    private final InputRunnable runCancel;
    /** The title reminder, or null while the question is asked in a dialog instead. */
    private BukkitTask taskId;
    private boolean clearInput = true;

    public PlayerInput(final boolean clearInput, final Player p, final InputRunnable correct, final InputRunnable cancel) {
        this(clearInput, p, RHLanguage.file().getStringList("System.Type-Input"),
                RHLanguage.file().getStringList("System.Type-Input-Dialog"), correct, cancel);
    }

    /** Asks in chat, with these two title lines, even on a server that has dialogs. */
    public PlayerInput(final boolean clearInput, final Player p, final List<String> titles,
                       final InputRunnable correct, final InputRunnable cancel) {
        this(clearInput, p, titles, null, correct, cancel);
    }

    /**
     * Waits for the player to type something in chat, with its own two title lines in place of the
     * search prompt.
     *
     * <p>On a server with dialogs (1.21.6 and up) the question is asked in a text box instead,
     * titled and described by {@code dialog}; the chat still answers it if the box cannot be shown.</p>
     *
     * @param clearInput strip colour codes from what is typed. Without it, {@code &} codes reach
     *                   the runnable as typed, for input such as a hopper's name that may be coloured
     * @param dialog     the text box's title and description, or null to always ask in chat
     */
    public PlayerInput(final boolean clearInput, final Player p, final List<String> titles, final List<String> dialog,
                       final InputRunnable correct, final InputRunnable cancel) {
        this.texts = Text.color(titles.size() >= 2 ? titles : RHLanguage.file().getStringList("System.Type-Input"));
        this.uuid = p.getUniqueId();
        p.closeInventory();
        this.runGo = correct;
        this.runCancel = cancel;
        this.clearInput = clearInput;
        this.register();

        if (dialog != null && dialog.size() >= 2 && dialogs) {
            final List<String> lines = Text.color(dialog);
            //a tick later, as this is usually built from inside an inventory click
            Bukkit.getScheduler().runTask(RealHoppersAPI.getInstance().getPlugin(), () -> {
                //answered, replaced or dropped in the meantime
                if (inputs.get(this.uuid) != this || !p.isOnline()) {
                    return;
                }
                if (!DialogInput.open(p, lines.get(0), lines.get(1))) {
                    this.startTitles(p);
                }
            });
        } else {
            this.startTitles(p);
        }
    }

    private void startTitles(final Player p) {
        this.taskId = new BukkitRunnable() {
            public void run() {
                p.getPlayer().sendTitle(PlayerInput.this.texts.get(0), PlayerInput.this.texts.get(1), 0, 21, 0);
            }
        }.runTaskTimer(RealHoppersAPI.getInstance().getPlugin(), 0L, 20);
    }

    /** Stops whatever is asking the question: the title reminder, or the text box. */
    private void stop() {
        if (this.taskId != null) {
            this.taskId.cancel();
        }
        if (dialogs) {
            DialogInput.close(this.uuid);
        }
    }

    /**
     * Asks in dialogs from here on, if the server has them. Called once the plugin is enabled, and
     * undone by {@link #shutdownDialogs}.
     */
    public static void setupDialogs(final Plugin plugin) {
        shutdownDialogs();
        //dialogs came with 1.21.6, which already needs Java 21
        if (Runtime.version().feature() < 21) {
            return;
        }
        try {
            DialogInput.setup(plugin, (uuid, input) -> answer(uuid, input, false), uuid -> answer(uuid, null, true));
            dialogs = DialogInput.isAvailable();
        } catch (final Throwable e) {
            plugin.getLogger().log(Level.WARNING, "Dialogs are not available, so input is asked for in chat instead.", e);
        }
    }

    public static void shutdownDialogs() {
        if (dialogs) {
            dialogs = false;
            DialogInput.shutdown();
        }
    }

    /** An answer from a text box: what was typed, or its cancel button. */
    private static void answer(final UUID uuid, final String input, final boolean cancelled) {
        //the dialog's events are not promised to arrive on the main thread
        Bukkit.getScheduler().runTask(RealHoppersAPI.getInstance().getPlugin(), () -> {
            final Player p = Bukkit.getPlayer(uuid);
            final PlayerInput current = inputs.remove(uuid);
            if (p == null || current == null) {
                return;
            }
            if (cancelled) {
                current.stop();
                TranslatableLine.SYSTEM_INPUT_CANCELLED.send(p);
                Bukkit.getScheduler().scheduleSyncDelayedTask(RealHoppersAPI.getInstance().getPlugin(), () -> current.runCancel.run(""), 3);
            } else {
                handlePlayerInput(p, input, current);
            }
        });
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
                    current.stop();
                }
            }
        };
    }

    /** Drops every unanswered prompt, for a reload: their callbacks hold hoppers that are about to be replaced. */
    public static void cancelAll() {
        for (final UUID uuid : inputs.keySet()) {
            final PlayerInput current = inputs.remove(uuid);
            if (current != null) {
                current.stop();
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
            current.stop();
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
            previous.stop();
        }
    }

    @FunctionalInterface
    public interface InputRunnable {
        void run(String input);
    }
}
