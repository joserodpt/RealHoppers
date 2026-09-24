package joserodpt.realhoppers.api.hopper;

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
import joserodpt.realhoppers.api.config.RHLanguage;
import joserodpt.realhoppers.api.utils.Text;

/**
 * Who may open a hopper's screen. A public hopper is open to anyone; a private one only to its
 * owner, the players on its whitelist and admins - and only they may break it or link it either.
 */
public enum RHopperAccess {
    PUBLIC, PRIVATE;

    /** How the access is shown to players, from language.yml. */
    public String getDisplayName() {
        return Text.color(RHLanguage.file().getString("Hoppers.Access.Names." + this.name(), this.name()));
    }

    public RHopperAccess next() {
        return this == PUBLIC ? PRIVATE : PUBLIC;
    }

    /** What a newly placed hopper starts as, from config.yml. A typo there falls back to public. */
    public static RHopperAccess getDefault() {
        return parse(RHConfig.file().getString("RealHoppers.Hoppers.Default-Access", "PUBLIC"));
    }

    /** Reads a stored value, falling back to public for anything unreadable. */
    public static RHopperAccess parse(final String s) {
        if (s != null) {
            for (final RHopperAccess access : values()) {
                if (access.name().equalsIgnoreCase(s.trim())) {
                    return access;
                }
            }
        }
        return PUBLIC;
    }
}
