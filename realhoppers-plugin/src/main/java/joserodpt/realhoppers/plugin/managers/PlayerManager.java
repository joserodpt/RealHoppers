package joserodpt.realhoppers.plugin.managers;

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
import joserodpt.realhoppers.api.managers.PlayerManagerAPI;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerManager extends PlayerManagerAPI {

    private final Set<UUID> tpFreeze = new HashSet<>();
    private final Map<UUID, RHopper> pendingLinks = new HashMap<>();

    @Override
    public Map<UUID, RHopper> getPendingLinks() {
        return pendingLinks;
    }

    @Override
    public Set<UUID> getTpFreeze() {
        return tpFreeze;
    }

    @Override
    public void clear(final UUID uuid) {
        this.tpFreeze.remove(uuid);
        this.pendingLinks.remove(uuid);
    }
}
