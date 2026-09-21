package joserodpt.realhoppers.api.utils;

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

import joserodpt.realhoppers.api.config.RHConfig;
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
		p.sendMessage(Text.color( RHConfig.file().getString("RealHoppers.Prefix") + " &r" + string));
	}
	public static void send(CommandSender p, String string) {
		p.sendMessage(Text.color(RHConfig.file().getString("RealHoppers.Prefix") +" &r" + string));
	}

	/**
	 * DIAMOND_ORE as "Diamond Ore". Same helper RealMines carries, for the same reason: a raw
	 * material name in a GUI reads like a config key.
	 */
	public static String beautifyMaterialName(final org.bukkit.Material material) {
		final String[] words = material.name().toLowerCase().split("_");
		final StringBuilder out = new StringBuilder();
		for (final String word : words) {
			if (word.isEmpty()) {
				continue;
			}
			if (out.length() > 0) {
				out.append(' ');
			}
			out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
		}
		return out.toString();
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