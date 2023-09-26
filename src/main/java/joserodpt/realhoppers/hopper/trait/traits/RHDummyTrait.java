package joserodpt.realhoppers.hopper.trait.traits;

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

import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.hopper.trait.RHopperTraitBase;
import org.bukkit.entity.Player;

public class RHDummyTrait extends RHopperTraitBase {

    final RHopperTrait t;
    public RHDummyTrait(RHopper main, RHopperTrait t) {
        super(main);
        this.t = t;
    }

    @Override
    public void executeAction(Player p) { }

    @Override
    public void executeLoop() { }

    @Override
    public RHopperTrait getTraitType() {
        return this.t;
    }

    @Override
    public void stopTask() {
        super.getHopper().saveData(RHopper.Data.BALANCE, true);
    }

    @Override
    public String getSerializedSave() {
        return getTraitType().name();
    }
}
