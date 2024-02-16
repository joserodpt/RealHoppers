package joserodpt.realhoppers.plugin;

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

import joserodpt.realhoppers.api.RealHoppersAPI;
import joserodpt.realhoppers.api.hopper.RHopper;
import joserodpt.realhoppers.api.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHBlockBreakingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHDummyTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHMobKillingTrait;
import joserodpt.realhoppers.api.hopper.trait.traits.RHSuctionTrait;
import joserodpt.realhoppers.api.utils.Text;
import me.mattstudios.mf.annotations.Alias;
import me.mattstudios.mf.annotations.Command;
import me.mattstudios.mf.annotations.Completion;
import me.mattstudios.mf.annotations.Default;
import me.mattstudios.mf.annotations.Permission;
import me.mattstudios.mf.annotations.SubCommand;
import me.mattstudios.mf.annotations.WrongUsage;
import me.mattstudios.mf.base.CommandBase;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

@Command("realhoppers")
@Alias("rh")
public class RealHoppersCMD extends CommandBase {
    
    private final String onlyPlayers = "[RealHoppers] Only players can run this command.";

    RealHoppersAPI rh;
    public RealHoppersCMD(RealHoppersAPI r)
    {
        this.rh = r;
    }

    @Default
    public void defaultCommand(final CommandSender commandSender) {
        Text.sendList(commandSender, Arrays.asList("         &fReal&dHoppers", "         &7Release &a" + rh.getPlugin().getDescription().getVersion()));
    }

    @SubCommand("reload")
    @Alias("rl")
    @Permission("realhoppers.admin")
    public void reloadcmd(final CommandSender commandSender) {
        rh.reload();
        Text.send(commandSender, "&aReloaded!");
    }


    @SubCommand("settrait")
    @Permission("realhoppers.admin")
    @Completion("#enum")
    @WrongUsage("&c/rr settrait <trait>")
    public void set(final CommandSender commandSender, final RHopperTrait t) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            Block b = p.getTargetBlock(null, 5);

            RHopper h = rh.getHopperManager().getHopper(b);
            if (h != null) {
                switch (t) {
                    case KILL_MOB:
                        h.setTrait(t, new RHMobKillingTrait(h));
                        break;
                    case BLOCK_BREAKING:
                        h.setTrait(t, new RHBlockBreakingTrait(h));
                        break;
                    case SUCTION:
                        h.setTrait(t, new RHSuctionTrait(h));
                        break;
                    case AUTO_SELL:
                        h.setTrait(t, new RHDummyTrait(h, RHopperTrait.AUTO_SELL));
                        break;
                }
            }
        } else {
            Text.send(commandSender, onlyPlayers);
        }

    }
}