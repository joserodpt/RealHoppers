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

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Builds the item stacks the GUIs are made of. Named to match RealMines' Items; it was Itens.
 */
public class Items {

    public static ItemStack createItem(Material m, final int quantidade, final String nome) {
        m = checkValidMaterialItem(m);
        final ItemStack item = new ItemStack(m, quantidade);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null && nome != null) {
            meta.setDisplayName(Text.color(nome));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createItem(Material m, final int quantidade, final String nome, final List<String> desc) {
        m = checkValidMaterialItem(m);
        final ItemStack item = new ItemStack(m, quantidade);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(nome));
            meta.setLore(Text.color(desc));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createItemLoreEnchanted(Material m, final int i, final String name, final List<String> desc) {
        m = checkValidMaterialItem(m);
        final ItemStack item = new ItemStack(m, i);
        final ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Text.color(name));
            meta.setLore(Text.color(desc));
            meta.addEnchant(Enchantment.LUCK, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * A block that has no item form cannot be put in an inventory slot, and asking for one throws.
     * Water and lava have obvious stand-ins; anything else falls back to stone.
     */
    private static Material checkValidMaterialItem(final Material m) {
        if (m == Material.WATER) {
            return Material.WATER_BUCKET;
        }
        if (m == Material.LAVA) {
            return Material.LAVA_BUCKET;
        }
        return m.isItem() ? m : Material.STONE;
    }
}
