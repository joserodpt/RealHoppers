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
import joserodpt.realhoppers.api.utils.Smelting;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Smelts what the hopper takes in, by the server's own furnace recipes.
 *
 * <p>Has no loop of its own: the hopper asks it on the way in, so it applies to whatever any other
 * trait feeds the hopper rather than running on a timer of its own.</p>
 */
public class RHAutoSmeltTrait extends RHopperTraitBase {

    public RHAutoSmeltTrait(RHopper main) {
        super(main);
    }

    /**
     * The smelted form of an incoming stack.
     *
     * @return the stack the hopper should store: the smelted one, or the original when the material
     *         does not smelt into anything
     */
    public ItemStack smelt(final ItemStack incoming) {
        final ItemStack smelted = Smelting.smelt(incoming);
        return smelted == null ? incoming : smelted;
    }

    @Override
    public void executeAction(Player p) { }

    @Override
    protected void executeLoop() { }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.AUTO_SMELT;
    }

    @Override
    public void stopTask() {
        //nothing scheduled to cancel
    }
}
