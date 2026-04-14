package com.github.hahahha.BiomeRunestone.util;

import net.minecraft.Block;
import net.minecraft.WorldServer;

public final class RunegateLandingRules {
    public static final int MIN_SAFE_FEET_Y = 65;

    private RunegateLandingRules() {
    }

    public static boolean isValidFeetY(WorldServer world, int x, int y, int z) {
        if (world == null) {
            return false;
        }
        if (y < MIN_SAFE_FEET_Y || y >= 255) {
            return false;
        }
        if (!isOpenSky(world, x, y, z)) {
            return false;
        }
        if (!world.isAirOrPassableBlock(x, y, z, false) || !world.isAirOrPassableBlock(x, y + 1, z, false)) {
            return false;
        }
        if (world.isAirOrPassableBlock(x, y - 1, z, true)) {
            return false;
        }

        Block ground = world.getBlock(x, y - 1, z);
        return ground != null && !ground.isLiquid();
    }

    public static boolean isOpenSky(WorldServer world, int x, int y, int z) {
        if (!world.canBlockSeeTheSky(x, y, z) || !world.canBlockSeeTheSky(x, y + 1, z)) {
            return false;
        }
        return world.getPrecipitationHeight(x, z) <= y;
    }

    public static String getRuleSummary() {
        return "open_sky=true, feet_min_y=" + MIN_SAFE_FEET_Y + ", ground_not_liquid=true";
    }
}
