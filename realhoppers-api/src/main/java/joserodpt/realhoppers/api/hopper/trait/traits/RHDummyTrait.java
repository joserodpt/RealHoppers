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

import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
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
    protected void executeLoop() { }

    @Override
    public RHopperTrait getTraitType() {
        return this.t;
    }

    @Override
    public void stopTask() {
        super.getHopper().saveData(RHopper.Data.BALANCE);
    }

    @Override
    public String getSerializedSave() {
        return getTraitType().name();
    }
}
