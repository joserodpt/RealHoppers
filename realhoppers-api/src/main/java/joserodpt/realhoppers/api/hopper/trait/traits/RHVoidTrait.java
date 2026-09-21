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

/**
 * Destroys whatever the hopper cannot take, instead of dropping it on the floor.
 *
 * <p>The last step of {@link RHopper#offer}, after selling: a hopper with both sells what has a
 * price and voids the rest. Has no loop and no tier - a thing is either destroyed or it is not.</p>
 */
public class RHVoidTrait extends RHopperTraitBase {

    public RHVoidTrait(RHopper main) {
        super(main);
    }

    @Override
    public void executeAction(Player p) { }

    @Override
    protected void executeLoop() { }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.VOID;
    }

    @Override
    public void stopTask() {
        //nothing scheduled to cancel
    }
}
