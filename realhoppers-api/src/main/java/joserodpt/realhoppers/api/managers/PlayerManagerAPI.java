package joserodpt.realhoppers.api.managers;

import joserodpt.realhoppers.api.hopper.RHopper;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

public abstract class PlayerManagerAPI {
    public abstract Map<Player, RHopper> getClickedHoppers();

    public abstract List<Player> getTpFreeze();
}
