package com.github.hahahha.BiomeRunestone.recipe;

import net.minecraft.CraftingManager;
import net.minecraft.CraftingResult;
import net.minecraft.IRecipe;
import net.minecraft.InventoryCrafting;
import net.minecraft.ItemStack;
import net.minecraft.Material;
import net.minecraft.RecipeHelper;
import net.minecraft.World;

import java.util.LinkedHashSet;
import java.util.Set;

public class BiomeRunestoneSwitchRecipe implements IRecipe {
    private final ItemStack recipeOutput;
    private final ItemStack[] components;
    private final Set<Integer> centerItemIds;

    private float difficulty;
    private boolean includeInLowestCraftingDifficultyDetermination;
    private int[] skillsets;
    private Material materialToCheckToolBenchHardnessAgainst;

    public BiomeRunestoneSwitchRecipe(ItemStack recipeOutput, ItemStack[] centerCandidates, ItemStack topLeft, ItemStack topRight, ItemStack bottomLeft, ItemStack bottomRight, boolean includeInLowest) {
        if (recipeOutput == null) {
            throw new IllegalArgumentException("recipeOutput cannot be null");
        }
        if (centerCandidates == null || centerCandidates.length == 0) {
            throw new IllegalArgumentException("centerCandidates cannot be empty");
        }

        this.recipeOutput = recipeOutput.copy();
        this.centerItemIds = new LinkedHashSet<Integer>();

        ItemStack representativeCenter = null;
        for (ItemStack centerCandidate : centerCandidates) {
            if (centerCandidate == null) {
                continue;
            }
            this.centerItemIds.add(Integer.valueOf(centerCandidate.itemID));
            if (representativeCenter == null) {
                representativeCenter = centerCandidate.copy();
            }
        }
        if (this.centerItemIds.isEmpty() || representativeCenter == null) {
            throw new IllegalArgumentException("centerCandidates must contain at least one non-null stack");
        }

        this.components = new ItemStack[9];
        this.components[0] = copyStack(topLeft);
        this.components[2] = copyStack(topRight);
        this.components[4] = representativeCenter;
        this.components[6] = copyStack(bottomLeft);
        this.components[8] = copyStack(bottomRight);

        RecipeHelper.addRecipe(this, includeInLowest);
        CraftingManager.getInstance().getRecipeList().add(this);
    }

    @Override
    public boolean matches(InventoryCrafting inventoryCrafting, World world) {
        ItemStack center = inventoryCrafting.getStackInRowAndColumn(1, 1);
        if (center == null) {
            return false;
        }
        if (!this.centerItemIds.contains(Integer.valueOf(center.itemID))) {
            return false;
        }
        if (center.itemID == this.recipeOutput.itemID) {
            return false;
        }

        if (!matchSlot(inventoryCrafting.getStackInRowAndColumn(0, 0), this.components[0])) {
            return false;
        }
        if (!matchSlot(inventoryCrafting.getStackInRowAndColumn(2, 0), this.components[2])) {
            return false;
        }
        if (!matchSlot(inventoryCrafting.getStackInRowAndColumn(0, 2), this.components[6])) {
            return false;
        }
        if (!matchSlot(inventoryCrafting.getStackInRowAndColumn(2, 2), this.components[8])) {
            return false;
        }

        // Edge slots must be empty.
        if (inventoryCrafting.getStackInRowAndColumn(1, 0) != null) {
            return false;
        }
        if (inventoryCrafting.getStackInRowAndColumn(0, 1) != null) {
            return false;
        }
        if (inventoryCrafting.getStackInRowAndColumn(2, 1) != null) {
            return false;
        }
        if (inventoryCrafting.getStackInRowAndColumn(1, 2) != null) {
            return false;
        }

        return true;
    }

    @Override
    public CraftingResult getCraftingResult(InventoryCrafting inventoryCrafting) {
        return new CraftingResult(this.recipeOutput.copy(), this.difficulty, this.skillsets, this);
    }

    @Override
    public int getRecipeSize() {
        int count = 0;
        for (ItemStack component : this.components) {
            if (component != null) {
                ++count;
            }
        }
        return count;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return this.recipeOutput;
    }

    @Override
    public ItemStack[] getComponents() {
        return this.components;
    }

    @Override
    public IRecipe setDifficulty(float difficulty) {
        this.difficulty = difficulty;
        return this;
    }

    @Override
    public IRecipe scaleDifficulty(float factor) {
        this.difficulty *= factor;
        return this;
    }

    @Override
    public float getUnmodifiedDifficulty() {
        return this.difficulty;
    }

    @Override
    public void setIncludeInLowestCraftingDifficultyDetermination() {
        this.includeInLowestCraftingDifficultyDetermination = true;
    }

    @Override
    public boolean getIncludeInLowestCraftingDifficultyDetermination() {
        return this.includeInLowestCraftingDifficultyDetermination;
    }

    @Override
    public void setSkillsets(int[] skillsets) {
        this.skillsets = skillsets;
    }

    @Override
    public void setSkillset(int skillset) {
        if (skillset == 0) {
            this.skillsets = null;
        } else {
            this.skillsets = new int[]{skillset};
        }
    }

    @Override
    public int[] getSkillsets() {
        return this.skillsets;
    }

    @Override
    public void setMaterialToCheckToolBenchHardnessAgainst(Material material) {
        this.materialToCheckToolBenchHardnessAgainst = material;
    }

    @Override
    public Material getMaterialToCheckToolBenchHardnessAgainst() {
        return this.materialToCheckToolBenchHardnessAgainst;
    }

    private static ItemStack copyStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private static boolean matchSlot(ItemStack actual, ItemStack required) {
        if (required == null) {
            return actual == null;
        }
        if (actual == null) {
            return false;
        }
        if (actual.itemID != required.itemID) {
            return false;
        }
        int requiredSubtype = required.getItemSubtype();
        return requiredSubtype == Short.MAX_VALUE || requiredSubtype == actual.getItemSubtype();
    }
}
