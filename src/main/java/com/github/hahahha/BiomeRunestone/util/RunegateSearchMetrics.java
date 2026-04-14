package com.github.hahahha.BiomeRunestone.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;

public final class RunegateSearchMetrics {
    private RunegateSearchMetrics() {
    }

    public static <T> void appendSample(LinkedList<T> samples, T value, int maxSize) {
        samples.add(value);
        while (samples.size() > maxSize) {
            samples.removeFirst();
        }
    }

    public static String buildMetricSummary(java.util.List<Long> latencySamples, java.util.List<Integer> attemptSamples) {
        int samples = Math.min(latencySamples.size(), attemptSamples.size());
        if (samples <= 0) {
            return "samples=0";
        }

        double avgLatency = averageLong(latencySamples);
        long p95Latency = percentileLong(latencySamples, 95.0);
        double avgAttempts = averageInt(attemptSamples);
        int p95Attempts = percentileInt(attemptSamples, 95.0);

        return "samples=" + samples
                + ", avgMs=" + String.format(Locale.ROOT, "%.2f", avgLatency)
                + ", p95Ms=" + p95Latency
                + ", avgAttempts=" + String.format(Locale.ROOT, "%.2f", avgAttempts)
                + ", p95Attempts=" + p95Attempts;
    }

    public static String buildEventSummary(Map<String, Integer> events) {
        if (events == null || events.isEmpty()) {
            return "none";
        }

        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Integer> entry : events.entrySet()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return builder.toString();
    }

    private static double averageLong(java.util.List<Long> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        long total = 0L;
        for (Long value : values) {
            total += value.longValue();
        }
        return (double) total / (double) values.size();
    }

    private static double averageInt(java.util.List<Integer> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        long total = 0L;
        for (Integer value : values) {
            total += value.intValue();
        }
        return (double) total / (double) values.size();
    }

    private static long percentileLong(java.util.List<Long> values, double percentile) {
        if (values.isEmpty()) {
            return 0L;
        }
        ArrayList<Long> sorted = new ArrayList<Long>(values);
        java.util.Collections.sort(sorted);
        int index = Math.max(0, Math.min(sorted.size() - 1, (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1));
        return sorted.get(index).longValue();
    }

    private static int percentileInt(java.util.List<Integer> values, double percentile) {
        if (values.isEmpty()) {
            return 0;
        }
        ArrayList<Integer> sorted = new ArrayList<Integer>(values);
        java.util.Collections.sort(sorted);
        int index = Math.max(0, Math.min(sorted.size() - 1, (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1));
        return sorted.get(index).intValue();
    }
}
