package joserodpt.realhoppers;

import joserodpt.realhoppers.config.Config;
import joserodpt.realhoppers.config.Hoppers;
import joserodpt.realhoppers.hopper.RHopper;
import joserodpt.realhoppers.hopper.type.RHBlockBreaking;
import joserodpt.realhoppers.utils.Text;
import me.mattstudios.mf.annotations.Alias;
import me.mattstudios.mf.annotations.Command;
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
    @WrongUsage("&c/rr settrait <trait>")
    public void loadcmd(final CommandSender commandSender, final RHopper.Trait t) {
        if (commandSender instanceof Player) {
            Player p = (Player) commandSender;
            Block b = p.getTargetBlock(null, 5);

            RHopper h = rh.getHopperManager().getHopper(b);
            if (h != null) {
                h.setTrait(t, new RHBlockBreaking(h));
            }
        } else {
            Text.send(commandSender, onlyPlayers);
        }

    }
}