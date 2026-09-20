package joserodpt.realhoppers.api.managers;

import joserodpt.realhoppers.api.hopper.RHopper;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The per-player state the plugin keeps while somebody is online.
 *
 * <p>Keyed by {@link UUID} rather than by Player: a Player is a live handle to a connection, so a
 * map holding one keeps the whole player object alive after they quit, and a player who reconnects
 * arrives as a different object that no longer matches the key.</p>
 */
public abstract class PlayerManagerAPI {

    /** The first hopper of a pair, remembered between the two clicks that link them. */
    public abstract Map<UUID, RHopper> getClickedHoppers();

    /** Players inside the cooldown after a hopper teleport, so a hopper cannot bounce them. */
    public abstract Set<UUID> getTpFreeze();

    /** Forgets everything held for a player. Called when they leave. */
    public abstract void clear(UUID uuid);
}
