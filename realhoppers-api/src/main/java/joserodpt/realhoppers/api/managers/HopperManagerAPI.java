package joserodpt.realhoppers.api.managers;

import joserodpt.realhoppers.api.hopper.RHopper;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Map;

public abstract class HopperManagerAPI {
    public abstract Map<Block, RHopper> getHoppersMap();

    public abstract List<RHopper> getHoppers();

    public abstract RHopper getHopper(Block b);

    public abstract void loadHoppers();

    public abstract void delete(RHopper h);

    public abstract void stopHoppers();

    public abstract Map<Material, Double> getMaterialCost();
}
