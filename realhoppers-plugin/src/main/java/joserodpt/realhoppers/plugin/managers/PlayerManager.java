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
 * @author José Rodrigues
 * @link https://github.com/joserodpt/RealHoppers
 */

import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.managers.PlayerManagerAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerManager extends PlayerManagerAPI {

    public List<Player> tpFreeze = new ArrayList<>();
    public Map<Player, RHopper> clickedHoppers = new HashMap<>();

    @Override
    public Map<Player, RHopper> getClickedHoppers() {
        return clickedHoppers;
    }

    @Override
    public List<Player> getTpFreeze() {
        return tpFreeze;
    }
}
