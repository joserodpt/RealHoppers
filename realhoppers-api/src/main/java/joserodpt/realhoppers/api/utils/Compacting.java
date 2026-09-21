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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Which materials nine of a thing makes, which is what the AUTO_COMPACT trait needs to know.
 *
 * <p>Read from the server's own recipes for the same reason {@link Smelting} is: a datapack that
 * adds a compacting recipe should work in a hopper too. Built on enable and on reload.</p>
 */
public class Compacting {

    /** How many of a material a compacting recipe takes. A full 3x3 of one thing. */
    private static final int GRID = 9;

    private static final Map<Material, ItemStack> RESULTS = new HashMap<>();

    public static void load() {
        RESULTS.clear();

        final Iterator<Recipe> recipes = Bukkit.recipeIterator();
        while (recipes.hasNext()) {
            final Recipe recipe;
            try {
                recipe = recipes.next();
            } catch (final Exception e) {
                //a recipe another plugin registered badly should cost its own entry, not the rest
                continue;
            }

            if (!(recipe instanceof ShapedRecipe)) {
                continue;
            }

            final ShapedRecipe shaped = (ShapedRecipe) recipe;
            final Material ingredient = soleIngredientOf(shaped);
            if (ingredient == null) {
                continue;
            }

            final ItemStack result = shaped.getResult();
            if (result != null && result.getType() != Material.AIR) {
                RESULTS.putIfAbsent(ingredient, result);
            }
        }
    }

    /**
     * The one material a recipe is nine of, or null when it is anything else.
     *
     * <p>Nine of a single ingredient filling the whole grid is what "compacting" means here -
     * ingots to a block, nuggets to an ingot. A recipe with a gap in it, or with two different
     * things in it, is somebody's crafting recipe and not ours.</p>
     */
    private static Material soleIngredientOf(final ShapedRecipe shaped) {
        final String[] shape = shaped.getShape();
        if (shape.length != 3) {
            return null;
        }

        Character symbol = null;
        for (final String row : shape) {
            if (row.length() != 3) {
                return null;
            }
            for (final char slot : row.toCharArray()) {
                if (slot == ' ') {
                    return null;
                }
                if (symbol == null) {
                    symbol = slot;
                } else if (symbol != slot) {
                    return null;
                }
            }
        }

        if (symbol == null) {
            return null;
        }

        final ItemStack ingredient = shaped.getIngredientMap().get(symbol);
        return ingredient == null ? null : ingredient.getType();
    }

    /** How many of a material one compaction takes. */
    public static int required() {
        return GRID;
    }

    public static boolean canCompact(final Material material) {
        return RESULTS.containsKey(material);
    }

    /**
     * What nine of a material makes.
     *
     * @param times how many nines are being compacted at once
     * @return the result stack, or null when the material does not compact into anything
     */
    public static ItemStack compact(final Material material, final int times) {
        final ItemStack result = RESULTS.get(material);
        if (result == null || times <= 0) {
            return null;
        }
        final ItemStack made = result.clone();
        made.setAmount(result.getAmount() * times);
        return made;
    }

    /** How many recipes were found, for the line the plugin logs on startup. */
    public static int size() {
        return RESULTS.size();
    }
}
