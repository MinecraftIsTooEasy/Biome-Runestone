package com.github.hahahha.BiomeRunestone.block;

import com.github.hahahha.BiomeRunestone.BiomeRunestone;
import com.github.hahahha.BiomeRunestone.recipe.BiomeRunestoneSwitchRecipe;
import com.github.hahahha.BiomeRunestone.util.RunegateRecipeConflictDetector;
import net.minecraft.Block;
import net.minecraft.Item;
import net.minecraft.ItemStack;
import net.minecraft.Material;
import net.xiaoyu233.fml.reload.event.ItemRegistryEvent;
import net.xiaoyu233.fml.reload.event.RecipeRegistryEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class Blocks {
    private static int nextBlockID = 2048;

    private static final String[] BIOME_KEYS = new String[]{
            "plains",
            "desert",
            "extreme_hills",
            "forest",
            "taiga",
            "swampland",
            "ice_plains",
            "ice_mountains",
            "beach",
            "desert_hills",
            "forest_hills",
            "taiga_hills",
            "extreme_hills_edge",
            "jungle",
            "jungle_hills",
            "river",
            "ocean",
            "frozen_ocean",
            "frozen_river",
            "desert_river",
            "jungle_river",
            "swamp_river"
    };

    private static final int[] TARGET_BIOME_IDS = new int[]{
            1, 2, 3, 4, 5, 6, 12, 13, 16, 17, 18, 19, 20, 21, 22, 7,
            0, 10, 11, 23, 24, 25
    };

    private static final String[] BIOME_RUNESTONE_REGISTRY_NAMES = new String[BIOME_KEYS.length];
    private static final String[] RANDOM_BIOME_RUNESTONE_REGISTRY_NAMES = new String[BIOME_KEYS.length];
    private static final String[] PLAYER_BIOME_RUNESTONE_REGISTRY_NAMES = new String[BIOME_KEYS.length];
    private static final CornerSpec[] BIOME_CORNER_SPECS = createBiomeCornerSpecs();

    public static final BlockBiomeRunestone[] BIOME_RUNESTONES = new BlockBiomeRunestone[BIOME_KEYS.length];
    public static final BlockBiomeRunestone[] RANDOM_BIOME_RUNESTONES = new BlockBiomeRunestone[BIOME_KEYS.length];
    public static final BlockBiomeRunestone[] PLAYER_BIOME_RUNESTONES = new BlockBiomeRunestone[BIOME_KEYS.length];

    private enum RecipeCore {
        NORMAL,
        RANDOM,
        PLAYER
    }

    private static final class CornerSpec {
        private final ItemStack topLeft;
        private final ItemStack topRight;
        private final ItemStack bottomLeft;
        private final ItemStack bottomRight;

        private CornerSpec(ItemStack topLeft, ItemStack topRight, ItemStack bottomLeft, ItemStack bottomRight) {
            this.topLeft = topLeft;
            this.topRight = topRight;
            this.bottomLeft = bottomLeft;
            this.bottomRight = bottomRight;
        }
    }

    static {
        if (TARGET_BIOME_IDS.length != BIOME_KEYS.length) {
            throw new IllegalStateException("TARGET_BIOME_IDS length must match BIOME_KEYS length");
        }
        if (BIOME_CORNER_SPECS.length != BIOME_KEYS.length) {
            throw new IllegalStateException("BIOME_CORNER_SPECS length must match BIOME_KEYS length");
        }

        for (int i = 0; i < BIOME_KEYS.length; ++i) {
            String key = BIOME_KEYS[i];
            BIOME_RUNESTONE_REGISTRY_NAMES[i] = "biome_runestone_" + key;
            RANDOM_BIOME_RUNESTONE_REGISTRY_NAMES[i] = "random_biome_runestone_" + key;
            PLAYER_BIOME_RUNESTONE_REGISTRY_NAMES[i] = "player_biome_runestone_" + key;

            BIOME_RUNESTONES[i] = new BlockBiomeRunestone(
                    getNextBlockID(),
                    Material.mithril,
                    i,
                    TARGET_BIOME_IDS[i],
                    BIOME_RUNESTONE_REGISTRY_NAMES[i],
                    BiomeRunestone.MOD_ID + ":" + BIOME_RUNESTONE_REGISTRY_NAMES[i],
                    false,
                    false
            );

            RANDOM_BIOME_RUNESTONES[i] = new BlockBiomeRunestone(
                    getNextBlockID(),
                    Material.mithril,
                    i,
                    TARGET_BIOME_IDS[i],
                    RANDOM_BIOME_RUNESTONE_REGISTRY_NAMES[i],
                    BiomeRunestone.MOD_ID + ":" + RANDOM_BIOME_RUNESTONE_REGISTRY_NAMES[i],
                    true,
                    false
            );

            PLAYER_BIOME_RUNESTONES[i] = new BlockBiomeRunestone(
                    getNextBlockID(),
                    Material.mithril,
                    i,
                    TARGET_BIOME_IDS[i],
                    PLAYER_BIOME_RUNESTONE_REGISTRY_NAMES[i],
                    BiomeRunestone.MOD_ID + ":" + PLAYER_BIOME_RUNESTONE_REGISTRY_NAMES[i],
                    false,
                    true
            );
        }
    }

    private static int getNextBlockID() {
        while (nextBlockID < Block.blocksList.length && Block.blocksList[nextBlockID] != null) {
            ++nextBlockID;
        }
        if (nextBlockID >= Block.blocksList.length) {
            throw new IllegalStateException("No free block ID slots left");
        }
        return nextBlockID++;
    }

    public static void registerItemBlocks(ItemRegistryEvent event) {
        for (int i = 0; i < BIOME_RUNESTONES.length; ++i) {
            event.registerItemBlock(BiomeRunestone.MOD_ID, BIOME_RUNESTONE_REGISTRY_NAMES[i], BIOME_RUNESTONE_REGISTRY_NAMES[i], BIOME_RUNESTONES[i]);
            event.registerItemBlock(BiomeRunestone.MOD_ID, RANDOM_BIOME_RUNESTONE_REGISTRY_NAMES[i], RANDOM_BIOME_RUNESTONE_REGISTRY_NAMES[i], RANDOM_BIOME_RUNESTONES[i]);
            event.registerItemBlock(BiomeRunestone.MOD_ID, PLAYER_BIOME_RUNESTONE_REGISTRY_NAMES[i], PLAYER_BIOME_RUNESTONE_REGISTRY_NAMES[i], PLAYER_BIOME_RUNESTONES[i]);
        }
    }

    public static void registerRecipes(RecipeRegistryEvent event) {
        ItemStack[] normalCenters = createCenterCandidates(BIOME_RUNESTONES);
        ItemStack[] randomCenters = createCenterCandidates(RANDOM_BIOME_RUNESTONES);
        ItemStack[] playerCenters = createCenterCandidates(PLAYER_BIOME_RUNESTONES);

        // Direct recipes for each target biome runestone.
        for (int i = 0; i < BIOME_RUNESTONES.length; ++i) {
            CornerSpec cornerSpec = BIOME_CORNER_SPECS[i];
            registerBaseRunestoneRecipe(event, new ItemStack(BIOME_RUNESTONES[i], 1), cornerSpec, RecipeCore.NORMAL);
            registerBaseRunestoneRecipe(event, new ItemStack(RANDOM_BIOME_RUNESTONES[i], 1), cornerSpec, RecipeCore.RANDOM);
            registerBaseRunestoneRecipe(event, new ItemStack(PLAYER_BIOME_RUNESTONES[i], 1), cornerSpec, RecipeCore.PLAYER);
        }

        // Biome switching recipes: center same-family runestone + corner biome materials.
        for (int i = 0; i < BIOME_RUNESTONES.length; ++i) {
            CornerSpec cornerSpec = BIOME_CORNER_SPECS[i];
            registerRunestoneSwitchRecipeCompact(new ItemStack(BIOME_RUNESTONES[i], 1), normalCenters, cornerSpec);
            registerRunestoneSwitchRecipeCompact(new ItemStack(RANDOM_BIOME_RUNESTONES[i], 1), randomCenters, cornerSpec);
            registerRunestoneSwitchRecipeCompact(new ItemStack(PLAYER_BIOME_RUNESTONES[i], 1), playerCenters, cornerSpec);
        }

        RunegateRecipeConflictDetector.logPotentialConflictsForOutputs(BiomeRunestone.MOD_ID, collectRunestoneOutputItemIds());
    }

    private static void registerBaseRunestoneRecipe(RecipeRegistryEvent event, ItemStack output, CornerSpec cornerSpec, RecipeCore recipeCore) {
        char north = ' ';
        char west = ' ';
        char east = ' ';
        char south = ' ';
        if (recipeCore == RecipeCore.NORMAL) {
            north = 'N';
            west = 'N';
            east = 'N';
            south = 'N';
        } else if (recipeCore == RecipeCore.RANDOM) {
            north = 'M';
            west = 'N';
            east = 'N';
            south = 'N';
        }

        String row1 = new String(new char[]{cornerChar(cornerSpec.topLeft, 'A'), north, cornerChar(cornerSpec.topRight, 'B')});
        String row2 = new String(new char[]{west, 'O', east});
        String row3 = new String(new char[]{cornerChar(cornerSpec.bottomLeft, 'C'), south, cornerChar(cornerSpec.bottomRight, 'D')});

        ArrayList<Object> recipe = new ArrayList<Object>();
        recipe.add(row1);
        recipe.add(row2);
        recipe.add(row3);
        recipe.add(Character.valueOf('O'));
        recipe.add(Block.obsidian);

        if (recipeCore == RecipeCore.NORMAL || recipeCore == RecipeCore.RANDOM) {
            recipe.add(Character.valueOf('N'));
            recipe.add(Item.mithrilNugget);
        }
        if (recipeCore == RecipeCore.RANDOM) {
            recipe.add(Character.valueOf('M'));
            recipe.add(Item.ingotMithril);
        }

        addCornerIngredient(recipe, 'A', cornerSpec.topLeft);
        addCornerIngredient(recipe, 'B', cornerSpec.topRight);
        addCornerIngredient(recipe, 'C', cornerSpec.bottomLeft);
        addCornerIngredient(recipe, 'D', cornerSpec.bottomRight);

        event.registerShapedRecipe(output, true, recipe.toArray(new Object[0]));
    }

    private static void registerRunestoneSwitchRecipeCompact(ItemStack output, ItemStack[] centerCandidates, CornerSpec cornerSpec) {
        new BiomeRunestoneSwitchRecipe(
                output,
                centerCandidates,
                cornerSpec.topLeft,
                cornerSpec.topRight,
                cornerSpec.bottomLeft,
                cornerSpec.bottomRight,
                true
        );
    }

    private static char cornerChar(ItemStack cornerStack, char cornerSymbol) {
        return cornerStack == null ? ' ' : cornerSymbol;
    }

    private static void addCornerIngredient(ArrayList<Object> recipe, char symbol, ItemStack stack) {
        if (stack == null) {
            return;
        }
        recipe.add(Character.valueOf(symbol));
        recipe.add(stack.copy());
    }

    private static CornerSpec[] createBiomeCornerSpecs() {
        return new CornerSpec[]{
                all(blockStack(Block.tallGrass, 1)),                          // plains
                all(blockStack(Block.sandStone)),                             // desert
                all(blockStack(Block.cobblestone)),                           // extreme_hills
                all(blockStack(Block.wood, 0)),                               // forest (oak)
                all(blockStack(Block.wood, 1)),                               // taiga (spruce)
                all(blockStack(Block.blockClay)),                             // swampland
                all(blockStack(Block.blockSnow)),                             // ice_plains
                twoTwo(blockStack(Block.blockSnow), blockStack(Block.cobblestone)), // ice_mountains
                all(blockStack(Block.sand)),                                  // beach
                twoTwo(blockStack(Block.sand), blockStack(Block.cobblestone)),       // desert_hills
                twoTwo(blockStack(Block.wood, 0), blockStack(Block.cobblestone)),    // forest_hills
                twoTwo(blockStack(Block.wood, 1), blockStack(Block.cobblestone)),    // taiga_hills
                all(blockStack(Block.stone)),                                 // extreme_hills_edge
                all(blockStack(Block.wood, 3)),                               // jungle
                twoTwo(blockStack(Block.wood, 3), blockStack(Block.cobblestone)),    // jungle_hills
                twoTwo(blockStack(Block.sand), blockStack(Block.blockClay)),          // river
                oneCorner(itemStack(Item.fishRaw)),                           // ocean
                all(blockStack(Block.ice)),                                   // frozen_ocean
                twoTwo(blockStack(Block.ice), blockStack(Block.blockClay)),           // frozen_river
                threeOne(blockStack(Block.sand), blockStack(Block.blockClay)),        // desert_river
                threeOne(blockStack(Block.sand), blockStack(Block.wood, 3)),          // jungle_river
                oneThree(blockStack(Block.sand), blockStack(Block.blockClay))         // swamp_river
        };
    }

    private static CornerSpec all(ItemStack stack) {
        return new CornerSpec(copyStack(stack), copyStack(stack), copyStack(stack), copyStack(stack));
    }

    private static CornerSpec twoTwo(ItemStack top, ItemStack bottom) {
        return new CornerSpec(copyStack(top), copyStack(top), copyStack(bottom), copyStack(bottom));
    }

    private static CornerSpec threeOne(ItemStack major, ItemStack minor) {
        return new CornerSpec(copyStack(major), copyStack(major), copyStack(major), copyStack(minor));
    }

    private static CornerSpec oneThree(ItemStack major, ItemStack minor) {
        return new CornerSpec(copyStack(major), copyStack(minor), copyStack(minor), copyStack(minor));
    }

    private static CornerSpec oneCorner(ItemStack stack) {
        return new CornerSpec(copyStack(stack), null, null, null);
    }

    private static ItemStack copyStack(ItemStack stack) {
        return stack == null ? null : stack.copy();
    }

    private static ItemStack blockStack(Block block) {
        return new ItemStack(block, 1, 0);
    }

    private static ItemStack blockStack(Block block, int metadata) {
        return new ItemStack(block, 1, metadata);
    }

    private static ItemStack itemStack(Item item) {
        return new ItemStack(item, 1);
    }

    private static ItemStack[] createCenterCandidates(BlockBiomeRunestone[] runestones) {
        ItemStack[] stacks = new ItemStack[runestones.length];
        for (int i = 0; i < runestones.length; ++i) {
            stacks[i] = new ItemStack(runestones[i], 1);
        }
        return stacks;
    }

    private static Set<Integer> collectRunestoneOutputItemIds() {
        LinkedHashSet<Integer> itemIds = new LinkedHashSet<Integer>();
        collectOutputItemIds(itemIds, BIOME_RUNESTONES);
        collectOutputItemIds(itemIds, RANDOM_BIOME_RUNESTONES);
        collectOutputItemIds(itemIds, PLAYER_BIOME_RUNESTONES);
        return itemIds;
    }

    private static void collectOutputItemIds(Set<Integer> outputItemIds, BlockBiomeRunestone[] runestones) {
        for (BlockBiomeRunestone runestone : runestones) {
            ItemStack stack = new ItemStack(runestone, 1);
            outputItemIds.add(Integer.valueOf(stack.itemID));
        }
    }
}

