package com.github.hahahha.BiomeRunestone.util;

public class RandomDestinationSearchResult {
    public final int[] destination;
    public final int attempts;
    public final String failureReason;

    public RandomDestinationSearchResult(int[] destination, int attempts, String failureReason) {
        this.destination = destination;
        this.attempts = attempts;
        this.failureReason = failureReason;
    }
}

