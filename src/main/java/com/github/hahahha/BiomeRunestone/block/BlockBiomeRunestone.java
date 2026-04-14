package com.github.hahahha.BiomeRunestone.block;

import net.minecraft.BlockRunestone;
import net.minecraft.Icon;
import net.minecraft.Material;
import net.minecraft.Block;

public class BlockBiomeRunestone extends BlockRunestone {
    private static final int MAGIC_SYMBOL_COUNT = 16;
    private final int runeSymbolIndex;
    private final int targetBiomeId;
    private final String stableRunestoneKey;
    private final boolean randomDestinationMode;
    private final boolean playerLockedRandomMode;

    public BlockBiomeRunestone(int id, Material runeMetal, int runeSymbolIndex, int targetBiomeId, String unlocalizedName) {
        this(id, runeMetal, runeSymbolIndex, targetBiomeId, unlocalizedName, unlocalizedName, false, false);
    }

    public BlockBiomeRunestone(int id, Material runeMetal, int runeSymbolIndex, int targetBiomeId, String unlocalizedName, boolean randomDestinationMode) {
        this(id, runeMetal, runeSymbolIndex, targetBiomeId, unlocalizedName, unlocalizedName, randomDestinationMode, false);
    }

    public BlockBiomeRunestone(int id, Material runeMetal, int runeSymbolIndex, int targetBiomeId, String unlocalizedName, boolean randomDestinationMode, boolean playerLockedRandomMode) {
        this(id, runeMetal, runeSymbolIndex, targetBiomeId, unlocalizedName, unlocalizedName, randomDestinationMode, playerLockedRandomMode);
    }

    public BlockBiomeRunestone(int id, Material runeMetal, int runeSymbolIndex, int targetBiomeId, String unlocalizedName, String stableRunestoneKey, boolean randomDestinationMode, boolean playerLockedRandomMode) {
        super(id, runeMetal);
        this.runeSymbolIndex = runeSymbolIndex;
        this.targetBiomeId = targetBiomeId;
        this.stableRunestoneKey = stableRunestoneKey == null || stableRunestoneKey.isEmpty() ? unlocalizedName : stableRunestoneKey;
        this.randomDestinationMode = randomDestinationMode;
        this.playerLockedRandomMode = playerLockedRandomMode;
        this.setUnlocalizedName(unlocalizedName);
    }

    public Material getRunestoneMaterial() {
        return this.rune_metal;
    }

    public int getTargetBiomeId() {
        return this.targetBiomeId;
    }

    public int getRuneSymbolIndex() {
        return this.runeSymbolIndex;
    }

    private int getRuneSymbolTextureIndex() {
        int idx = this.runeSymbolIndex % MAGIC_SYMBOL_COUNT;
        if (idx < 0) {
            idx += MAGIC_SYMBOL_COUNT;
        }
        return idx;
    }

    public String getStableRunestoneKey() {
        return this.stableRunestoneKey;
    }

    public boolean isRandomDestinationMode() {
        return this.randomDestinationMode;
    }

    public boolean isPlayerLockedRandomMode() {
        return this.playerLockedRandomMode;
    }

    @Override
    public Icon getIcon(int side, int metadata) {
        return side == 0 || side == 1 ? this.blockIcon : this.iconArray[this.getRuneSymbolTextureIndex()];
    }

    @Override
    public boolean isValidMetadata(int metadata) {
        return metadata >= 0 && metadata < 16;
    }

    @Override
    public int getBlockSubtypeUnchecked(int metadata) {
        return 0;
    }

    @Override
    public String getMetadataNotes() {
        return "0=\"" + BlockRunestone.getMagicName(this.getRuneSymbolTextureIndex()) + "\"";
    }

    @Override
    public boolean canBeReplacedBy(int metadata, Block other_block, int other_block_metadata) {
        return false;
    }
}

