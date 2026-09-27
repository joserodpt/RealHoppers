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

import io.github.projectunified.unidialog.core.DialogManager;
import io.github.projectunified.unidialog.core.action.DialogActionBuilder;
import io.github.projectunified.unidialog.core.body.DialogBodyBuilder;
import io.github.projectunified.unidialog.core.body.TextBody;
import io.github.projectunified.unidialog.core.dialog.ConfirmationDialog;
import io.github.projectunified.unidialog.core.dialog.Dialog;
import io.github.projectunified.unidialog.core.input.DialogInputBuilder;
import io.github.projectunified.unidialog.core.input.TextInput;
import joserodpt.realhoppers.api.config.TranslatableLine;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The text box {@link PlayerInput} asks in on servers that have dialogs (1.21.6 and up), in place
 * of a title and the chat.
 *
 * <p>UniDialog is built for Java 21 and the 1.21.6 API, while this plugin still loads on servers
 * much older than that. So only {@link PlayerInput} talks to this class, and only once
 * {@link #setup} has found the server has dialogs; the platform's manager is created by name, so
 * neither backend is loaded on the platform it is not for.</p>
 */
final class DialogInput {

    private static final String PAPER_MANAGER = "io.github.projectunified.unidialog.paper.PaperDialogManager";
    private static final String SPIGOT_MANAGER = "io.github.projectunified.unidialog.spigot.SpigotDialogManager";

    private static final String SUBMIT = "input_submit";
    private static final String CANCEL = "input_cancel";
    private static final String FIELD = "input";
    /** Chat allows this many, so a dialog should not take fewer. */
    private static final int MAX_LENGTH = 256;

    @SuppressWarnings("rawtypes")
    private static DialogManager manager;
    private static Logger logger;

    private DialogInput() {
    }

    /**
     * Uses dialogs from here on if the server has them.
     *
     * @param submitted run with what was typed, when the player confirms
     * @param cancelled run when they press cancel instead
     */
    static void setup(final Plugin plugin, final BiConsumer<UUID, String> submitted, final Consumer<UUID> cancelled) {
        logger = plugin.getLogger();
        final String managerClass = hasClass("io.papermc.paper.dialog.Dialog") ? PAPER_MANAGER
                : hasClass("net.md_5.bungee.api.dialog.Dialog") && hasClass("org.bukkit.event.player.PlayerCustomClickEvent") ? SPIGOT_MANAGER
                : null;
        if (managerClass == null) {
            return;
        }

        try {
            @SuppressWarnings("rawtypes") final DialogManager created = (DialogManager) Class.forName(managerClass)
                    .getConstructor(Plugin.class).newInstance(plugin);
            created.registerCustomAction(SUBMIT, (BiConsumer<UUID, Map<String, String>>)
                    (uuid, values) -> submitted.accept(uuid, values.getOrDefault(FIELD, "")));
            created.registerCustomAction(CANCEL, (BiConsumer<UUID, Map<String, String>>)
                    (uuid, values) -> cancelled.accept(uuid));
            created.register();
            manager = created;
        } catch (final Throwable e) {
            //a LinkageError too: a server whose dialog API is not the one UniDialog was built for
            //keeps the chat prompt rather than failing to enable
            plugin.getLogger().log(Level.WARNING, "Dialogs are not available, so input is asked for in chat instead.", e);
        }
    }

    static boolean isAvailable() {
        return manager != null;
    }

    /**
     * Shows the text box.
     *
     * @return false if it could not be shown, and the chat prompt should be used instead
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static boolean open(final Player p, final String title, final String description) {
        if (manager == null) {
            return false;
        }
        try {
            final ConfirmationDialog dialog = manager.createConfirmationDialog();
            dialog.title(title);
            dialog.body((Consumer<DialogBodyBuilder<?>>) body -> {
                final TextBody<?> text = body.text();
                text.text(description);
            });
            dialog.input(FIELD, (Consumer<DialogInputBuilder>) input -> {
                final TextInput<?> field = input.textInput();
                field.maxLength(MAX_LENGTH);
            });
            dialog.yesAction((Consumer<DialogActionBuilder<?, ?>>) action -> {
                action.label(TranslatableLine.SYSTEM_DIALOG_CONFIRM.get());
                action.dynamicCustom(SUBMIT);
            });
            dialog.noAction((Consumer<DialogActionBuilder<?, ?>>) action -> {
                action.label(TranslatableLine.SYSTEM_DIALOG_CANCEL.get());
                action.dynamicCustom(CANCEL);
            });
            //Escape would close it without telling the server, leaving the prompt waiting forever
            dialog.canCloseWithEscape(false);
            //set even though closing is what it does anyway: UniDialog's Paper backend has no default
            //for it and hands Paper a null, which Paper's codec throws on
            dialog.afterAction(Dialog.AfterAction.CLOSE);
            return dialog.opener().open(p.getUniqueId());
        } catch (final Throwable e) {
            //the player still gets asked, in chat, but a dialog that never shows is worth saying so
            logger.log(Level.WARNING, "Couldn't show a dialog to " + p.getName() + ", asking in chat instead.", e);
            return false;
        }
    }

    /** Takes the text box off a player's screen, when the prompt it belongs to is answered some other way. */
    static void close(final UUID uuid) {
        if (manager != null) {
            try {
                manager.clearDialog(uuid);
            } catch (final Throwable ignored) {
                //nothing on screen to clear
            }
        }
    }

    static void shutdown() {
        if (manager != null) {
            manager.unregister();
            manager = null;
        }
    }

    private static boolean hasClass(final String name) {
        try {
            Class.forName(name, false, DialogInput.class.getClassLoader());
            return true;
        } catch (final Throwable e) {
            return false;
        }
    }
}
