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

import org.bukkit.Location;

/**
 * RealHoppers' own formatting, for what RealUtils' Text has no use for elsewhere.
 */
public final class Format {

    private Format() {
    }

    public static String cords(final Location l) {
        return "X: " + l.getBlockX() + " Y: " + l.getBlockY() + " Z: "
                + l.getBlockZ();
    }
}
