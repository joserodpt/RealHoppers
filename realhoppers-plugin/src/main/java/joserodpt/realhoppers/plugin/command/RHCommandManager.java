package joserodpt.realhoppers.plugin.command;

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

import joserodpt.realhoppers.plugin.RealHoppers;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

/**
 * Builds the Lamp instance every RealHoppers command hangs off. Lamp registers the commands
 * straight onto the server's command map, which is why none of them appear in plugin.yml.
 *
 * <p>No shared suggestion providers yet: every argument the plugin takes is an enum, and Lamp
 * completes those from the constants on its own. Anything reading live state - hopper names, linked
 * hoppers - would be wired in here, the way RealMines does it.</p>
 */
public final class RHCommandManager {

    private final Lamp<BukkitCommandActor> lamp;

    public RHCommandManager(final RealHoppers rh) {
        //Brigadier stays on. Lamp's own matcher treats leftover input as merely a worse match, so
        //`/rh reload junk` would quietly fall back to the bare `/rh` handler; Brigadier's tree
        //refuses it outright. Where it can't attach Lamp falls back on its own and
        //RHExceptionHandler's @Usage messages are what players see instead.
        this.lamp = BukkitLamp.builder(rh.getPlugin())
                .exceptionHandler(new RHExceptionHandler())
                .build();

        this.lamp.register(new HopperCMD(rh));
    }

    public Lamp<BukkitCommandActor> getLamp() {
        return this.lamp;
    }
}
