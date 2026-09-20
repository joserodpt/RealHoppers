package joserodpt.realhoppers.api.hopper.trait.traits;

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

import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

public class RHSuctionTrait extends RHopperTraitBase {

    public RHSuctionTrait(RHopper main) {
        super(main);
    }

    @Override
    public void executeAction(Player p) { }

    private int taskID = -1;

    private int area = 2;

    @Override
    protected void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppersAPI.getInstance().getPlugin(), () -> {
            if (!super.hopperReady()) {
                return;
            }

            for (Entity ent : super.getHopper().getWorld().getNearbyEntities(super.getHopper().getLocation(), area, area, area)) {
                if (ent.getType() == EntityType.DROPPED_ITEM) {
                    Item droppedItem = (Item) ent;
                    Material m = droppedItem.getItemStack().getType();
                    if (super.getHopper().hasHopperSpace(m)) {
                        super.getHopper().addItem(m);
                        ent.remove();
                    } else if (super.getHopper().hasTrait(RHopperTrait.AUTO_SELL) && super.getHopper().sell(m)) {
                        ent.remove();
                    }
                }
            }
        }, 20, 20);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.SUCTION;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }

    @Override
    public String getSerializedSave() {
        return getTraitType().name();
    }
}
