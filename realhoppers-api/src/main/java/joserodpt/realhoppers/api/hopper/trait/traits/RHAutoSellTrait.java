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
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Turns what the hopper cannot fit into money instead.
 *
 * <p>Has no loop of its own: it is the other traits that reach it, when suction, block breaking or
 * a mob drop has something the hopper has no room for.</p>
 */
public class RHAutoSellTrait extends RHopperTraitBase {

    public RHAutoSellTrait(RHopper main) {
        super(main);
    }

    /**
     * Sells one of {@code type} into the hopper's balance.
     *
     * @return whether the material had a price. False leaves the item the caller's problem, which is
     *         what stops an unpriced drop quietly disappearing into a full hopper.
     */
    public boolean sell(final Material type) {
        final Double price = RealHoppersAPI.getInstance().getHopperManager().getMaterialCost().get(type);
        if (price == null) {
            return false;
        }
        final double payout = price
                * RHopperTrait.AUTO_SELL.configValue("Price-Multiplier", 1) * super.getTier();
        super.getHopper().setBalance(super.getHopper().getBalance() + payout);
        return true;
    }

    @Override
    public void executeAction(Player p) { }

    @Override
    protected void executeLoop() { }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.AUTO_SELL;
    }

    @Override
    public void stopTask() {
        //nothing scheduled to cancel. The balance is written by RHopper#stopHopper, which runs
        //right after this for every trait on the hopper.
    }
}
