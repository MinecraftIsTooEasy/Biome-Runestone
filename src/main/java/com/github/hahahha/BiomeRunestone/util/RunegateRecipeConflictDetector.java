package com.github.hahahha.BiomeRunestone.util;

import com.github.hahahha.BiomeRunestone.BiomeRunestone;
import net.minecraft.CraftingManager;
import net.minecraft.IRecipe;
import net.minecraft.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RunegateRecipeConflictDetector {
    private RunegateRecipeConflictDetector() {
    }

    public static void logPotentialConflictsForOutputs(String ownerTag, Set<Integer> outputItemIds) {
        if (outputItemIds == null || outputItemIds.isEmpty()) {
            return;
        }

        CraftingManager craftingManager = CraftingManager.getInstance();
        if (craftingManager == null) {
            return;
        }

        List recipeList = craftingManager.getRecipeList();
        if (recipeList == null || recipeList.isEmpty()) {
            return;
        }

        LinkedHashMap<String, ArrayList<String>> grouped = new LinkedHashMap<String, ArrayList<String>>();
        int checkedCount = 0;

        for (int i = 0; i < recipeList.size(); ++i) {
            Object entry = recipeList.get(i);
            if (!(entry instanceof IRecipe)) {
                continue;
            }

            IRecipe recipe = (IRecipe) entry;
            ItemStack output = recipe.getRecipeOutput();
            if (output == null || !outputItemIds.contains(Integer.valueOf(output.itemID))) {
                continue;
            }

            ++checkedCount;
            String conflictKey = output.itemID + ":" + output.getItemSubtype() + "|" + buildComponentSignature(recipe.getComponents());
            ArrayList<String> matches = grouped.get(conflictKey);
            if (matches == null) {
                matches = new ArrayList<String>();
                grouped.put(conflictKey, matches);
            }
            matches.add(recipe.getClass().getName() + "@" + i);
        }

        int conflictGroups = 0;
        for (Map.Entry<String, ArrayList<String>> entry : grouped.entrySet()) {
            ArrayList<String> matches = entry.getValue();
            if (matches == null || matches.size() <= 1) {
                continue;
            }

            ++conflictGroups;
            BiomeRunestone.LOGGER.warning("[RecipeConflict] owner=" + ownerTag
                    + ", key=" + entry.getKey()
                    + ", matches=" + matches);
        }

        if (conflictGroups > 0) {
            BiomeRunestone.LOGGER.warning("[RecipeConflict] owner=" + ownerTag
                    + ", checked=" + checkedCount
                    + ", conflictGroups=" + conflictGroups);
        }
    }

    private static String buildComponentSignature(ItemStack[] components) {
        if (components == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("len=").append(components.length).append(":");
        for (int i = 0; i < components.length; ++i) {
            if (i > 0) {
                builder.append(',');
            }
            ItemStack component = components[i];
            if (component == null) {
                builder.append('_');
            } else {
                builder.append(component.itemID).append('/').append(component.getItemSubtype());
            }
        }
        return builder.toString();
    }
}
