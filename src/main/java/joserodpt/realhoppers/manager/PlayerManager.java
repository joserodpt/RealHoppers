package joserodpt.realhoppers.manager;

import joserodpt.realhoppers.hopper.RHopper;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerManager {

    public List<Player> tpFreeze = new ArrayList<>();
    public Map<Player, RHopper> clickedHoppers = new HashMap<>();

    public Map<Player, RHopper> getClickedHoppers() {
        return clickedHoppers;
    }

    public List<Player> getTpFreeze() {
        return tpFreeze;
    }
}
