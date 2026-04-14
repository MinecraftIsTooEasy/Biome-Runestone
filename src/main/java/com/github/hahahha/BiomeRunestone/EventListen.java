package com.github.hahahha.BiomeRunestone;

import com.github.hahahha.BiomeRunestone.block.Blocks;
import com.google.common.eventbus.Subscribe;
import net.xiaoyu233.fml.reload.event.ItemRegistryEvent;
import net.xiaoyu233.fml.reload.event.RecipeRegistryEvent;

public class EventListen {
    @Subscribe
    public void onItemRegister(ItemRegistryEvent event) {
        Blocks.registerItemBlocks(event);
    }

    @Subscribe
    public void onRecipeRegister(RecipeRegistryEvent event) {
        Blocks.registerRecipes(event);
    }
}

