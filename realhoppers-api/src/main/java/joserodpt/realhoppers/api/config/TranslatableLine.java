package joserodpt.realhoppers.api.config;

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

import dev.dejvokep.boostedyaml.YamlDocument;
import joserodpt.realutils.text.LanguageLine;
import joserodpt.realutils.text.LanguageMessage;
import joserodpt.realutils.text.Placeholder;

/**
 * Every line the plugin says to a player, as a constant pointing at its route in language.yml.
 *
 * <p>Placeholders are filled with {@link #with(Placeholder, Object)}, which hands back a new
 * {@link LanguageMessage} rather than changing the constant:</p>
 *
 * <pre>{@code
 * TranslatableLine.ACCESS_DENIED.with(NAME, hopper.getName()).with(PLAYER, owner).send(p);
 * }</pre>
 */
public enum TranslatableLine implements LanguageLine {
    // Hopper related messages
    HOPPER_TELEPORTED("Hoppers.Teleported"),
    HOPPER_REMOVED("Hoppers.Removed"),
    HOPPER_BALANCE_COLLECTED("Hoppers.Balance-Collected"),
    HOPPER_XP_COLLECTED("Hoppers.Xp-Collected"),
    HOPPER_BALANCE_COLLECTED_ON_BREAK("Hoppers.Balance-Collected-On-Break"),
    HOPPER_NOT_LOOKING_AT("Hoppers.Not-Looking-At"),
    HOPPER_LIMIT_REACHED("Hoppers.Limit-Reached"),
    HOPPER_LIMIT_REACHED_DEFAULT("Hoppers.Limit-Reached-Default"),
    HOPPER_UNKNOWN_OWNER("Hoppers.Unknown-Owner"),

    // Ownership and access
    ACCESS_DENIED("Hoppers.Access.Denied"),
    ACCESS_NO_BREAK("Hoppers.Access.No-Break"),
    ACCESS_NO_LINK("Hoppers.Access.No-Link"),
    ACCESS_NO_MANAGE("Hoppers.Access.No-Manage"),
    ACCESS_SET("Hoppers.Access.Set"),
    NAME_SET("Hoppers.Name.Set"),
    NAME_TOO_LONG("Hoppers.Name.Too-Long"),
    NAME_EMPTY("Hoppers.Name.Empty"),
    WHITELIST_ADDED("Hoppers.Whitelist.Added"),
    WHITELIST_REMOVED("Hoppers.Whitelist.Removed"),
    WHITELIST_ALREADY("Hoppers.Whitelist.Already"),
    WHITELIST_NOT_ON("Hoppers.Whitelist.Not-On"),
    WHITELIST_NOT_FOUND("Hoppers.Whitelist.Not-Found"),
    WHITELIST_FULL("Hoppers.Whitelist.Full"),
    WHITELIST_IS_OWNER("Hoppers.Whitelist.Is-Owner"),
    WHITELIST_EMPTY("Hoppers.Whitelist.Empty"),
    WHITELIST_LIST_HEADER("Hoppers.Whitelist.List-Header"),
    WHITELIST_LIST_ENTRY("Hoppers.Whitelist.List-Entry"),
    INFO_HEADER("Hoppers.Info.Header"),
    INFO_OWNER("Hoppers.Info.Owner"),
    INFO_ACCESS("Hoppers.Info.Access"),
    INFO_LOCATION("Hoppers.Info.Location"),
    INFO_WHITELIST("Hoppers.Info.Whitelist"),
    LIST_HEADER("Hoppers.List.Header"),
    LIST_ENTRY("Hoppers.List.Entry"),
    LIST_EMPTY("Hoppers.List.Empty"),
    LIST_PLAYER_NOT_FOUND("Hoppers.List.Player-Not-Found"),

    // Traits
    TRAIT_ADDED("Hoppers.Traits.Added"),
    TRAIT_ALREADY_PRESENT("Hoppers.Traits.Already-Present"),
    TRAIT_NEEDS_LINK("Hoppers.Traits.Needs-Link"),
    TRAIT_UNAVAILABLE("Hoppers.Traits.Unavailable"),
    TRAIT_NO_PERMISSION("Hoppers.Traits.No-Permission"),
    TRAIT_REMOVED("Hoppers.Traits.Removed"),
    FILTER_ADDED("Hoppers.Filter.Added"),
    FILTER_REMOVED("Hoppers.Filter.Removed"),
    FILTER_ALREADY_LISTED("Hoppers.Filter.Already-Listed"),
    FILTER_NOTHING_HELD("Hoppers.Filter.Nothing-Held"),
    TRAIT_TIER_SET("Hoppers.Traits.Tier-Set"),
    TRAIT_NOT_SCALABLE("Hoppers.Traits.Not-Scalable"),
    TRAIT_TIER_UPGRADED("Hoppers.Traits.Tier-Upgraded"),
    TRAIT_TIER_MAX("Hoppers.Traits.Tier-Max"),
    TRAIT_TIER_TOO_EXPENSIVE("Hoppers.Traits.Tier-Too-Expensive"),
    TRAIT_UPGRADE_CONFIRM("Hoppers.Traits.Upgrade-Confirm"),
    TRAIT_UPGRADE_CONFIRM_BUTTON("Hoppers.Traits.Upgrade-Confirm-Button"),
    TRAIT_REMOVE_CONFIRM("Hoppers.Traits.Remove-Confirm"),
    TRAIT_REMOVE_CONFIRM_BUTTON("Hoppers.Traits.Remove-Confirm-Button"),
    HOPPER_SETTINGS_TITLE("Hoppers.Settings.Title"),

    // Linking two hoppers with the stick
    LINK_SOURCE_SELECTED("Hoppers.Link.Source-Selected"),
    LINK_SAME_HOPPER("Hoppers.Link.Same-Hopper"),
    LINK_DONE("Hoppers.Link.Done"),
    LINK_CANCELLED("Hoppers.Link.Cancelled"),

    // GUI related messages
    GUI_TITLE("GUI.Title"),
    GUI_FILTER_TITLE("GUI.Filter-Title"),
    GUI_FILTER_ADD_NAME("GUI.Items.Filter.Add.Name"),
    GUI_FILTER_PICK_NAME("GUI.Items.Filter.Pick.Name"),
    GUI_FILTER_EMPTY_NAME("GUI.Items.Filter.Empty.Name"),
    GUI_PICKER_TITLE("GUI.Picker-Title"),
    GUI_NEXT_PAGE_NAME("GUI.Items.Picker.Next.Name"),
    GUI_PREVIOUS_PAGE_NAME("GUI.Items.Picker.Back.Name"),
    GUI_SEARCH_ITEM_NAME("GUI.Items.Picker.Search.Name"),
    GUI_PICK_NAME("GUI.Items.Picker.Pick.Name"),
    GUI_BACK_NAME("GUI.Items.Back.Name"),
    GUI_CLOSE_NAME("GUI.Items.Close.Name"),
    GUI_HOPPER_NAME("GUI.Items.Hopper.Name"),
    GUI_HOPPER_BALANCE("GUI.Items.Hopper.Balance"),
    GUI_HOPPER_LINK("GUI.Items.Hopper.Link"),
    GUI_HOPPER_XP("GUI.Items.Hopper.Xp"),
    GUI_XP_NAME("GUI.Items.Xp.Name"),
    GUI_HOPPER_TRAITS_HEADER("GUI.Items.Hopper.Traits-Header"),
    GUI_HOPPER_NO_TRAITS("GUI.Items.Hopper.No-Traits"),
    GUI_HOPPER_TRAIT_ENTRY("GUI.Items.Hopper.Trait-Entry"),
    GUI_HOPPER_COLLECT("GUI.Items.Hopper.Collect"),
    GUI_HOPPER_OWNER("GUI.Items.Hopper.Owner"),
    GUI_HOPPER_ACCESS("GUI.Items.Hopper.Access"),
    GUI_RENAME_NAME("GUI.Items.Rename.Name"),
    GUI_WHITELIST_NAME("GUI.Items.Whitelist.Name"),
    GUI_OWNER_NAME("GUI.Items.Owner.Name"),
    GUI_WHITELIST_TITLE("GUI.Whitelist-Title"),
    GUI_WHITELIST_ADD_NAME("GUI.Items.Whitelist.Add.Name"),
    GUI_WHITELIST_ENTRY_NAME("GUI.Items.Whitelist.Entry.Name"),
    GUI_WHITELIST_EMPTY_NAME("GUI.Items.Whitelist.Empty.Name"),

    // System related messages
    SYSTEM_RELOADED("System.Reloaded"),
    SYSTEM_NEW_UPDATE("System.New-Update"),
    SYSTEM_DIALOG_SAVE("System.Dialog-Save"),
    SYSTEM_DIALOG_BACK("System.Dialog-Back"),
    SYSTEM_DIALOG_CLOSE("System.Dialog-Close"),
    SYSTEM_SETTINGS_SAVED("System.Settings-Saved"),
    SYSTEM_SETTINGS_NEED_DIALOGS("System.Settings-Need-Dialogs"),
    SYSTEM_INPUT_CANCELLED("System.Input-Cancelled"),
    SYSTEM_DIALOG_CONFIRM("System.Dialog-Confirm"),
    SYSTEM_DIALOG_CANCEL("System.Dialog-Cancel"),
    SYSTEM_ERROR_OCCURRED("System.Error-Occurred"),
    SYSTEM_NOTHING_FOUND("System.Nothing-Found"),
    SYSTEM_PLAYER_ONLY("System.Player-Only"),
    SYSTEM_VAULT_MISSING("System.Vault-Missing"),
    SYSTEM_ERROR_PERMISSION("System.Error-Permission"),
    SYSTEM_ERROR_COMMAND("System.Error-Command"),
    SYSTEM_ERROR_USAGE("System.Error-Usage");

    private final String configPath;

    TranslatableLine(String configPath) {
        this.configPath = configPath;
    }

    @Override
    public String getPath() {
        return this.configPath;
    }

    @Override
    public YamlDocument getLanguageFile() {
        return RHLanguage.file();
    }

    /** The tokens a line in language.yml may contain. {@code NAME} is written {@code %name%}. */
    public enum TranslatableLinePlaceholder implements Placeholder {
        MONEY, TRAIT, MATERIAL, PLAYER, VALUE, NAME
    }
}
