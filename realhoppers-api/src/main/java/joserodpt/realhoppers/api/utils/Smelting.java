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
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What the server's furnace recipes turn each material into, which is what the AUTO_SMELT trait
 * needs to know.
 *
 * <p>Read from the server rather than from a config: a datapack or another plugin can add furnace
 * recipes, and anything the player could smelt by hand should smelt in a hopper too. Built once
 * when the plugin enables and again on reload, because the recipe list does not change in between.</p>
 */
public class Smelting {

    private static final Map<Material, ItemStack> RESULTS = new HashMap<>();

    /**
     * Walks the server's recipes and keeps the furnace ones. Blasting, smoking and campfire recipes
     * are left alone - a hopper that smelts is a furnace, and for the materials where they overlap
     * the result is the same anyway.
     */
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

            if (!(recipe instanceof FurnaceRecipe)) {
                continue;
            }

            final FurnaceRecipe furnace = (FurnaceRecipe) recipe;
            final ItemStack result = furnace.getResult();
            if (result == null || result.getType() == Material.AIR) {
                continue;
            }

            for (final Material input : inputsOf(furnace)) {
                RESULTS.putIfAbsent(input, result);
            }
        }
    }

    /**
     * Every material a recipe accepts. getInput() answers with one of them, which is not enough:
     * vanilla's charcoal recipe takes the whole logs tag, so reading only the first would smelt oak
     * and leave every other log alone.
     */
    private static List<Material> inputsOf(final FurnaceRecipe furnace) {
        final RecipeChoice choice = furnace.getInputChoice();

        if (choice instanceof RecipeChoice.MaterialChoice) {
            return ((RecipeChoice.MaterialChoice) choice).getChoices();
        }

        if (choice instanceof RecipeChoice.ExactChoice) {
            return ((RecipeChoice.ExactChoice) choice).getChoices().stream()
                    .map(ItemStack::getType)
                    .collect(Collectors.toList());
        }

        //a choice type from a newer API than this one is built against
        return Collections.singletonList(furnace.getInput().getType());
    }

    public static boolean canSmelt(final Material material) {
        return RESULTS.containsKey(material);
    }

    /**
     * The smelted form of a stack, keeping its size: eight cobblestone come out as eight stone, the
     * way they would out of a furnace.
     *
     * @return a new stack, or null when the material does not smelt into anything
     */
    public static ItemStack smelt(final ItemStack input) {
        final ItemStack result = RESULTS.get(input.getType());
        if (result == null) {
            return null;
        }
        final ItemStack smelted = result.clone();
        smelted.setAmount(result.getAmount() * input.getAmount());
        return smelted;
    }

    /** How many recipes were found, for the line the plugin logs on startup. */
    public static int size() {
        return RESULTS.size();
    }
}
