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
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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

            final Material ingredient;
            final ItemStack result;

            if (recipe instanceof ShapedRecipe) {
                ingredient = soleIngredientOf((ShapedRecipe) recipe);
                result = ((ShapedRecipe) recipe).getResult();
            } else if (recipe instanceof ShapelessRecipe) {
                //a pack is free to write nine of a thing as shapeless, and it means the same
                ingredient = soleIngredientOf((ShapelessRecipe) recipe);
                result = ((ShapelessRecipe) recipe).getResult();
            } else {
                continue;
            }

            if (ingredient == null || result == null || result.getType() == Material.AIR) {
                continue;
            }

            RESULTS.putIfAbsent(ingredient, result);
        }

        if (RESULTS.isEmpty()) {
            //said out loud rather than left as a quiet zero on the startup line
            Bukkit.getLogger().warning("[RealHoppers] Found no compacting recipes. AUTO_SMELT hoppers will work, "
                    + "AUTO_COMPACT hoppers will have nothing to do.");
        }
    }

    /**
     * The one material a 3x3 recipe is nine of, or null when it is anything else.
     *
     * <p>Compares the materials behind the grid rather than the letters in the shape. The server
     * hands out a different letter for every slot - a full grid comes back as "abc", "def", "ghi" -
     * so looking for nine of the same letter finds nothing at all, which is what this did.</p>
     */
    private static Material soleIngredientOf(final ShapedRecipe shaped) {
        final String[] shape = shaped.getShape();
        if (shape.length != 3) {
            return null;
        }

        Material sole = null;
        int slots = 0;

        for (final String row : shape) {
            if (row.length() != 3) {
                return null;
            }
            for (final char slot : row.toCharArray()) {
                //a gap means it is not nine of anything
                if (slot == ' ') {
                    return null;
                }

                final Material material = materialAt(shaped, slot);
                if (material == null) {
                    return null;
                }
                if (sole == null) {
                    sole = material;
                } else if (sole != material) {
                    return null;
                }
                slots++;
            }
        }

        return slots == GRID ? sole : null;
    }

    /** The same question of a shapeless recipe: nine entries, all of one material. */
    private static Material soleIngredientOf(final ShapelessRecipe shapeless) {
        final List<RecipeChoice> choices = shapeless.getChoiceList();
        if (choices.size() != GRID) {
            return null;
        }

        Material sole = null;
        for (final RecipeChoice choice : choices) {
            final Material material = materialOf(choice);
            if (material == null) {
                return null;
            }
            if (sole == null) {
                sole = material;
            } else if (sole != material) {
                return null;
            }
        }
        return sole;
    }

    private static Material materialAt(final ShapedRecipe shaped, final char slot) {
        final Material fromChoice = materialOf(shaped.getChoiceMap().get(slot));
        if (fromChoice != null) {
            return fromChoice;
        }
        //older servers, where the choice map may not be populated
        final ItemStack legacy = shaped.getIngredientMap().get(slot);
        return legacy == null ? null : legacy.getType();
    }

    /**
     * The single material a recipe slot accepts.
     *
     * <p>Null when it accepts more than one - an ingredient written as a tag is ambiguous here in
     * a way it is not for smelting, since "nine of any of these" has no one answer.</p>
     */
    private static Material materialOf(final RecipeChoice choice) {
        if (choice instanceof RecipeChoice.MaterialChoice) {
            final List<Material> choices = ((RecipeChoice.MaterialChoice) choice).getChoices();
            return choices.size() == 1 ? choices.get(0) : null;
        }
        if (choice instanceof RecipeChoice.ExactChoice) {
            final List<ItemStack> choices = ((RecipeChoice.ExactChoice) choice).getChoices();
            return choices.size() == 1 ? choices.get(0).getType() : null;
        }
        return null;
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
