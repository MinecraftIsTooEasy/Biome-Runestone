package com.github.hahahha.BiomeRunestone.util;

public class RandomXZSearchResult {
    public final int[] xz;
    public final int attempts;
    public final String failureReason;

    public RandomXZSearchResult(int[] xz, int attempts, String failureReason) {
        this.xz = xz;
        this.attempts = attempts;
        this.failureReason = failureReason;
    }
}

