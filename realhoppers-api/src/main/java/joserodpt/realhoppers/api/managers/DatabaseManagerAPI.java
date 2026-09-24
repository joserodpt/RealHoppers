package joserodpt.realhoppers.api.managers;

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

import joserodpt.realhoppers.api.hopper.RHopper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Where hoppers are kept between restarts. Hoppers never write themselves out: they mark
 * themselves dirty, and the flush task writes whatever changed since the last one.
 */
public abstract class DatabaseManagerAPI {

    /** Queues the hopper to be written on the next flush. Cheap, and safe to call constantly. */
    public abstract void markDirty(RHopper hopper);

    /**
     * Writes every hopper marked dirty. The rows are taken from the hoppers on the calling thread,
     * which must be the main one; with async they are then written off it.
     */
    public abstract void flush(boolean async);

    /** Removes a hopper, its traits and its whitelist from the database. */
    public abstract void delete(RHopper hopper);

    /**
     * The locations and names of every hopper a player owns, read from the database rather than
     * memory, so it includes hoppers in worlds that are not loaded.
     */
    public abstract CompletableFuture<List<OwnedHopper>> getOwnedHoppers(UUID owner);

    /** Flushes what is left and closes the connection. Called once, on shutdown. */
    public abstract void close();

    /** One row of {@link #getOwnedHoppers(UUID)}. */
    public static class OwnedHopper {
        private final String location;
        private final String name;
        private final String access;

        public OwnedHopper(final String location, final String name, final String access) {
            this.location = location;
            this.name = name;
            this.access = access;
        }

        /** Serialized as {@code x:y:z:world}. */
        public String getLocation() {
            return location;
        }

        public String getName() {
            return name;
        }

        public String getAccess() {
            return access;
        }
    }
}
