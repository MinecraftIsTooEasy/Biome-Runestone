package com.github.hahahha.BiomeRunestone.util;

public interface RunegateRuntimeAccess {
    int getRuntimeRandomPortalCount();

    int getRuntimeRandomHistoryPortalCount();

    int getRuntimeRandomHistoryEntryCount();

    int getRuntimeLandCacheCount();

    void clearRuntimeRandomCaches();

    void clearRuntimeSearchMetrics();

    String getRandomSearchMetricSummary();

    String getPlayerSearchMetricSummary();

    String getRandomSearchEventSummary();

    String getPlayerSearchEventSummary();
}

