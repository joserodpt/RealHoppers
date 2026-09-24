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

import joserodpt.realhoppers.api.utils.Text;
import org.bukkit.command.CommandSender;

/**
 * Every line the plugin says to a player, as a constant pointing at its route in language.yml.
 * Same shape as RealMines' TranslatableLine, so the two read alike.
 */
public enum TranslatableLine {
    // Hopper related messages
    HOPPER_TELEPORTED("Hoppers.Teleported"),
    HOPPER_REMOVED("Hoppers.Removed"),
    HOPPER_BALANCE_COLLECTED("Hoppers.Balance-Collected", ReplacableVar.MONEY),
    HOPPER_XP_COLLECTED("Hoppers.Xp-Collected", ReplacableVar.VALUE),
    HOPPER_BALANCE_COLLECTED_ON_BREAK("Hoppers.Balance-Collected-On-Break", ReplacableVar.MONEY),
    HOPPER_NOT_LOOKING_AT("Hoppers.Not-Looking-At"),
    HOPPER_PLACED("Hoppers.Placed", ReplacableVar.NAME, ReplacableVar.VALUE),
    HOPPER_UNKNOWN_OWNER("Hoppers.Unknown-Owner"),

    // Ownership and access
    ACCESS_DENIED("Hoppers.Access.Denied", ReplacableVar.NAME, ReplacableVar.PLAYER),
    ACCESS_NO_BREAK("Hoppers.Access.No-Break", ReplacableVar.NAME),
    ACCESS_NO_LINK("Hoppers.Access.No-Link", ReplacableVar.NAME),
    ACCESS_NO_MANAGE("Hoppers.Access.No-Manage"),
    ACCESS_SET("Hoppers.Access.Set", ReplacableVar.NAME, ReplacableVar.VALUE),
    NAME_SET("Hoppers.Name.Set", ReplacableVar.NAME),
    NAME_TOO_LONG("Hoppers.Name.Too-Long", ReplacableVar.VALUE),
    NAME_EMPTY("Hoppers.Name.Empty"),
    WHITELIST_ADDED("Hoppers.Whitelist.Added", ReplacableVar.PLAYER, ReplacableVar.NAME),
    WHITELIST_REMOVED("Hoppers.Whitelist.Removed", ReplacableVar.PLAYER, ReplacableVar.NAME),
    WHITELIST_ALREADY("Hoppers.Whitelist.Already", ReplacableVar.PLAYER),
    WHITELIST_NOT_ON("Hoppers.Whitelist.Not-On", ReplacableVar.PLAYER),
    WHITELIST_NOT_FOUND("Hoppers.Whitelist.Not-Found", ReplacableVar.PLAYER),
    WHITELIST_FULL("Hoppers.Whitelist.Full", ReplacableVar.VALUE),
    WHITELIST_IS_OWNER("Hoppers.Whitelist.Is-Owner"),
    WHITELIST_EMPTY("Hoppers.Whitelist.Empty"),
    WHITELIST_LIST_HEADER("Hoppers.Whitelist.List-Header", ReplacableVar.NAME, ReplacableVar.VALUE),
    WHITELIST_LIST_ENTRY("Hoppers.Whitelist.List-Entry", ReplacableVar.PLAYER),
    INFO_HEADER("Hoppers.Info.Header", ReplacableVar.NAME),
    INFO_OWNER("Hoppers.Info.Owner", ReplacableVar.PLAYER),
    INFO_ACCESS("Hoppers.Info.Access", ReplacableVar.VALUE),
    INFO_LOCATION("Hoppers.Info.Location", ReplacableVar.VALUE),
    INFO_WHITELIST("Hoppers.Info.Whitelist", ReplacableVar.VALUE),
    LIST_HEADER("Hoppers.List.Header", ReplacableVar.PLAYER, ReplacableVar.VALUE),
    LIST_ENTRY("Hoppers.List.Entry", ReplacableVar.NAME, ReplacableVar.VALUE),
    LIST_EMPTY("Hoppers.List.Empty", ReplacableVar.PLAYER),
    LIST_PLAYER_NOT_FOUND("Hoppers.List.Player-Not-Found", ReplacableVar.PLAYER),

    // Traits
    TRAIT_ADDED("Hoppers.Traits.Added", ReplacableVar.TRAIT),
    TRAIT_ALREADY_PRESENT("Hoppers.Traits.Already-Present", ReplacableVar.TRAIT),
    TRAIT_NEEDS_LINK("Hoppers.Traits.Needs-Link", ReplacableVar.TRAIT),
    TRAIT_UNAVAILABLE("Hoppers.Traits.Unavailable", ReplacableVar.TRAIT),
    TRAIT_REMOVED("Hoppers.Traits.Removed", ReplacableVar.TRAIT),
    FILTER_ADDED("Hoppers.Filter.Added", ReplacableVar.MATERIAL),
    FILTER_REMOVED("Hoppers.Filter.Removed", ReplacableVar.MATERIAL),
    FILTER_ALREADY_LISTED("Hoppers.Filter.Already-Listed", ReplacableVar.MATERIAL),
    FILTER_NOTHING_HELD("Hoppers.Filter.Nothing-Held"),
    TRAIT_TIER_SET("Hoppers.Traits.Tier-Set", ReplacableVar.TRAIT, ReplacableVar.VALUE),
    TRAIT_NOT_SCALABLE("Hoppers.Traits.Not-Scalable", ReplacableVar.TRAIT),
    TRAIT_TIER_UPGRADED("Hoppers.Traits.Tier-Upgraded", ReplacableVar.TRAIT, ReplacableVar.VALUE),
    TRAIT_TIER_MAX("Hoppers.Traits.Tier-Max", ReplacableVar.TRAIT),
    TRAIT_TIER_TOO_EXPENSIVE("Hoppers.Traits.Tier-Too-Expensive", ReplacableVar.MONEY),

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
    GUI_PICK_NAME("GUI.Items.Picker.Pick.Name", ReplacableVar.MATERIAL),
    GUI_BACK_NAME("GUI.Items.Back.Name"),
    GUI_CLOSE_NAME("GUI.Items.Close.Name"),
    GUI_HOPPER_NAME("GUI.Items.Hopper.Name"),
    GUI_HOPPER_BALANCE("GUI.Items.Hopper.Balance", ReplacableVar.MONEY),
    GUI_HOPPER_LINK("GUI.Items.Hopper.Link", ReplacableVar.VALUE),
    GUI_HOPPER_XP("GUI.Items.Hopper.Xp", ReplacableVar.VALUE),
    GUI_XP_NAME("GUI.Items.Xp.Name", ReplacableVar.VALUE),
    GUI_HOPPER_TRAITS_HEADER("GUI.Items.Hopper.Traits-Header"),
    GUI_HOPPER_NO_TRAITS("GUI.Items.Hopper.No-Traits"),
    GUI_HOPPER_TRAIT_ENTRY("GUI.Items.Hopper.Trait-Entry", ReplacableVar.TRAIT, ReplacableVar.VALUE),
    GUI_HOPPER_COLLECT("GUI.Items.Hopper.Collect"),
    GUI_HOPPER_OWNER("GUI.Items.Hopper.Owner", ReplacableVar.PLAYER),
    GUI_HOPPER_ACCESS("GUI.Items.Hopper.Access", ReplacableVar.VALUE),
    GUI_RENAME_NAME("GUI.Items.Rename.Name", ReplacableVar.NAME),
    GUI_ACCESS_NAME("GUI.Items.Access.Name", ReplacableVar.VALUE),
    GUI_WHITELIST_NAME("GUI.Items.Whitelist.Name", ReplacableVar.VALUE),
    GUI_OWNER_NAME("GUI.Items.Owner.Name", ReplacableVar.PLAYER),
    GUI_WHITELIST_TITLE("GUI.Whitelist-Title", ReplacableVar.NAME),
    GUI_WHITELIST_ADD_NAME("GUI.Items.Whitelist.Add.Name"),
    GUI_WHITELIST_ENTRY_NAME("GUI.Items.Whitelist.Entry.Name", ReplacableVar.PLAYER),
    GUI_WHITELIST_EMPTY_NAME("GUI.Items.Whitelist.Empty.Name"),

    // System related messages
    SYSTEM_RELOADED("System.Reloaded"),
    SYSTEM_INPUT_CANCELLED("System.Input-Cancelled"),
    SYSTEM_ERROR_OCCURRED("System.Error-Occurred"),
    SYSTEM_NOTHING_FOUND("System.Nothing-Found"),
    SYSTEM_PLAYER_ONLY("System.Player-Only"),
    SYSTEM_VAULT_MISSING("System.Vault-Missing"),
    SYSTEM_ERROR_PERMISSION("System.Error-Permission"),
    SYSTEM_ERROR_COMMAND("System.Error-Command"),
    SYSTEM_ERROR_USAGE("System.Error-Usage");

    private final String configPath;
    private ReplacableVar v1;
    private ReplacableVar v2;

    TranslatableLine(String configPath) {
        this.configPath = configPath;
    }

    TranslatableLine(String configPath, ReplacableVar v1) {
        this.configPath = configPath;
        this.v1 = v1;
    }

    TranslatableLine(String configPath, ReplacableVar v1, ReplacableVar v2) {
        this.configPath = configPath;
        this.v1 = v1;
        this.v2 = v2;
    }

    public TranslatableLine setV1(ReplacableVar v1) {
        this.v1 = v1;
        return this;
    }

    public TranslatableLine setV2(ReplacableVar v2) {
        this.v2 = v2;
        return this;
    }

    public String get() {
        String s = RHLanguage.file().getString(this.configPath);
        if (s == null) {
            //a route missing from a hand-edited or older language.yml, said plainly rather than
            //printed to the player as the word "null"
            return Text.color("&cMissing language entry: " + this.configPath);
        }
        if (v1 != null) {
            s = s.replace(v1.getKey(), v1.getVal());
        }
        if (v2 != null) {
            s = s.replace(v2.getKey(), v2.getVal());
        }

        return Text.color(s);
    }

    public void send(CommandSender p) {
        Text.send(p, this.get());
    }

    public enum ReplacableVar {

        MONEY("%money%"),
        TRAIT("%trait%"),
        MATERIAL("%material%"),
        PLAYER("%player%"),
        NAME("%name%"),
        VALUE("%value%");

        private final String key;
        private String val;

        ReplacableVar(String key) {
            this.key = key;
        }

        public ReplacableVar eq(String val) {
            this.val = val;
            return this;
        }

        public String getKey() {
            return key;
        }

        public String getVal() {
            return val;
        }
    }
}
