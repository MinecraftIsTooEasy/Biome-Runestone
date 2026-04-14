package com.github.hahahha.BiomeRunestone.util;

import java.util.Arrays;

public final class RunegateSearchTuning {
    private RunegateSearchTuning() {
    }

    public static int[] getSearchRadii() {
        return Config.getRunegateSearchRadii();
    }

    public static int getSearchMetricSampleLimit() {
        return Config.getRunegateSearchMetricSampleLimit();
    }

    public static int getLandSearchSameBiomeRadius() {
        return Config.getRunegateLandSearchSameBiomeRadius();
    }

    public static int getLandSearchAnyBiomeRadius() {
        return Config.getRunegateLandSearchAnyBiomeRadius();
    }

    public static int getLandSearchStep() {
        return Config.getRunegateLandSearchStep();
    }

    public static int getLandSearchRingSamples() {
        return Config.getRunegateLandSearchRingSamples();
    }

    public static long getCacheSweepIntervalMs() {
        return Config.getRunegateCacheSweepIntervalMs();
    }

    public static String getSummary() {
        return "radii=" + Arrays.toString(getSearchRadii())
                + ", metricSampleLimit=" + getSearchMetricSampleLimit()
                + ", landSameRadius=" + getLandSearchSameBiomeRadius()
                + ", landAnyRadius=" + getLandSearchAnyBiomeRadius()
                + ", landStep=" + getLandSearchStep()
                + ", landRingSamples=" + getLandSearchRingSamples()
                + ", cacheSweepMs=" + getCacheSweepIntervalMs()
                + ", maxSearchMs=" + Config.getRandomBiomeRunegateMaxSearchTimeMs()
                + ", failCooldownSec=" + Config.getRandomBiomeRunegateFailureCooldownSeconds()
                + ", landCacheGrid=" + Config.getRunegateLandCacheGridSize();
    }
}
