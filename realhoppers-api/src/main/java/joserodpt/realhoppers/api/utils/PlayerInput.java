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

import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.config.TranslatableLine;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Collections;
import java.util.List;

/**
 * Asks a player to type something, through RealUtils' prompt: a dialog's text box on servers that
 * have them, the chat everywhere else. Kept here, with its own constructors and
 * {@link InputRunnable}, so every screen that asks for input is unchanged.
 */
public class PlayerInput {

    /** Waits for the player to type something, with the search prompt's titles and text box. */
    public PlayerInput(final boolean clearInput, final Player p, final InputRunnable correct, final InputRunnable cancel) {
        this(clearInput, p, null, null, correct, cancel);
    }

    /** Asks in chat, with these two title lines, even on a server that has dialogs. */
    public PlayerInput(final boolean clearInput, final Player p, final List<String> titles,
                       final InputRunnable correct, final InputRunnable cancel) {
        this(clearInput, p, titles, Collections.emptyList(), correct, cancel);
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
     * @param dialog     the text box's title and description, null for the search prompt's, or an
     *                   empty list to always ask in chat
     */
    public PlayerInput(final boolean clearInput, final Player p, final List<String> titles, final List<String> dialog,
                       final InputRunnable correct, final InputRunnable cancel) {
        new joserodpt.realutils.input.PlayerInput(p, clearInput, titles, dialog, correct::run, cancel::run);
    }

    /** Where the prompt's words come from. Called once the plugin is enabled. */
    public static void setup(final Plugin plugin) {
        joserodpt.realutils.input.PlayerInput.setup(plugin,
                p -> RHLanguage.file().getStringList("System.Type-Input"),
                p -> RHLanguage.file().getStringList("System.Type-Input-Dialog"),
                TranslatableLine.SYSTEM_INPUT_CANCELLED::send,
                TranslatableLine.SYSTEM_ERROR_OCCURRED::send);
    }

    public static Listener getListener() {
        return joserodpt.realutils.input.PlayerInput.getListener();
    }

    /** Drops every unanswered prompt, for a reload: their callbacks hold hoppers that are about to be replaced. */
    public static void cancelAll() {
        joserodpt.realutils.input.PlayerInput.cancelAll();
    }

    @FunctionalInterface
    public interface InputRunnable {
        void run(String input);
    }
}
