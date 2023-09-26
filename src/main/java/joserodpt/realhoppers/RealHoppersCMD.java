package joserodpt.realhoppers;

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

import joserodpt.realhoppers.config.Config;
import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.trait.RHopperTrait;
import joserodpt.realhoppers.hopper.trait.traits.RHDummyTrait;
import joserodpt.realhoppers.hopper.trait.traits.RHBlockBreaking;
import joserodpt.realhoppers.hopper.trait.traits.RHMobKilling;
import joserodpt.realhoppers.utils.Text;
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

    RealHoppers rh;
    public RealHoppersCMD(RealHoppers r)
    {
        this.rh = r;
    }

    @Default
    public void defaultCommand(final CommandSender commandSender) {
        Text.sendList(commandSender, Arrays.asList("         &fReal&dHoppers", "         &7Release &a" + rh.getDescription().getVersion()));
    }

    @SubCommand("reload")
    @Alias("rl")
    @Permission("realhoppers.admin")
    public void reloadcmd(final CommandSender commandSender) {
        Config.reload();
        Hoppers.reload();

        //reload worlds config
        rh.getHopperManager().loadHoppers();
        Text.send(commandSender, "&aReloaded!");
    }


    @SubCommand("settrait")
    @Permission("realhoppers.admin")
    @Completion("#enum")
    @WrongUsage("&c/rr settrait <trait>")
    public void loadcmd(final CommandSender commandSender, final RHopperTrait t) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            Block b = p.getTargetBlock(null, 5);

            RHopper h = rh.getHopperManager().getHopper(b);
            if (h != null) {
                switch (t) {
                    case KILL_MOB:
                        h.setTrait(t, new RHMobKilling(h));
                        break;
                    case BLOCK_BREAKING:
                        h.setTrait(t, new RHBlockBreaking(h));
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