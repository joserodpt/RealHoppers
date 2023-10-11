package joserodpt.realhoppers.utils;

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
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Text {

	public static String color(final String string) {
		return ChatColor.translateAlternateColorCodes('&', string);
	}

	public static void sendList(CommandSender cs, List<String> list) {
		list.forEach(s -> cs.sendMessage(Text.color(s)));
	}

	public static List<String> color(List<String> list) {
		return list.stream()
				.map(s -> Text.color("&f" + s))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public static String cords(Location l) {
		return "X: " + l.getBlockX() + " Y: " + l.getBlockY() + " Z: "
				+ l.getBlockZ();
	}

	public static void send(Player p, String string) {
		p.sendMessage(Text.color( Config.file().getString("RealHoppers.Prefix") + " &r" + string));
	}
	public static void send(CommandSender p, String string) {
		p.sendMessage(Text.color(Config.file().getString("RealHoppers.Prefix") +" &r" + string));
	}

	public static String formatNumber(double number) {
		String[] suffixes = {"", "k", "M", "T"};
		int index = 0;
		while (number >= 1_000 && index < suffixes.length - 1) {
			number /= 1_000;
			index++;
		}
		return new DecimalFormat("#.#").format(number) + suffixes[index];
	}
}