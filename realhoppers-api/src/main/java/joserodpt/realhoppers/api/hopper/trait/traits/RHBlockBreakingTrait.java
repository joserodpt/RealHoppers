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
import joserodpt.realhoppers.api.config.RHConfig;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.RHopperTraitBase;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class RHBlockBreakingTrait extends RHopperTraitBase {

    /** The tool the drops are worked out for. Not enchanted, so nothing is silk touched or fortuned. */
    private static final ItemStack TOOL = new ItemStack(Material.DIAMOND_PICKAXE);

    public RHBlockBreakingTrait(RHopper main) {
        super(main);
    }

    @Override
    public void executeAction(Player p) { }

    private int taskID = -1;

    @Override
    protected void executeLoop() {
        taskID = Bukkit.getScheduler().scheduleSyncRepeatingTask(RealHoppersAPI.getInstance().getPlugin(), () -> {
            if (!super.hopperReady()) {
                return;
            }

            final int blocks = (int) Math.max(1,
                    RHopperTrait.BLOCK_BREAKING.configValue("Blocks", 1) * super.power());

            for (int height = 1; height <= blocks; height++) {
                final Block toBreak = super.getHopper().getBlock().getRelative(0, height, 0);
                //a gap ends the run: the tier reaches further up a column, it does not mine
                //through air to whatever happens to be above it
                if (toBreak == null || !breakable(toBreak)) {
                    break;
                }

                //what the block really drops, as if mined with a pickaxe: an ore gives its ore,
                //not a copy of the ore block
                final Collection<ItemStack> drops = toBreak.getDrops(TOOL);
                if (drops.isEmpty()) {
                    //breaking it would only destroy it
                    break;
                }

                final boolean dropIfFull = RHConfig.file().getBoolean("RealHoppers.Drop-Items-If-Full");
                //checked for every drop before any is taken, so a block is never half paid out and
                //left standing
                if (!dropIfFull && !super.getHopper().hasTrait(RHopperTrait.VOID)
                        && !drops.stream().allMatch(super.getHopper()::hasHopperSpace)) {
                    //nowhere for it to go, so it stays in the ground
                    break;
                }

                for (final ItemStack drop : drops) {
                    final ItemStack left = super.getHopper().offer(drop);
                    if (left != null) {
                        //two drops competing for the last free slot; dropped rather than lost
                        super.getHopper().getWorld().dropItemNaturally(super.getHopper().getTeleportLocation(), left);
                    }
                }

                toBreak.setType(Material.AIR);
            }
        }, 10, 10);
    }

    /**
     * Whether the trait may mine this block. Not bedrock and the like (a negative hardness), not a
     * spawner, not anything with a tile entity - a container's contents or another RealHoppers
     * hopper would go with it - and not anything that has no item form.
     */
    private static boolean breakable(final Block block) {
        final Material type = block.getType();
        return type.isSolid()
                && type.isItem()
                && type.getHardness() >= 0
                && type != Material.SPAWNER
                && !(block.getState() instanceof TileState);
    }

    @Override
    public RHopperTrait getTraitType() {
        return RHopperTrait.BLOCK_BREAKING;
    }

    @Override
    public void stopTask() {
        if (taskID != -1) {
            Bukkit.getScheduler().cancelTask(taskID);
            taskID = -1;
        }
    }
}
