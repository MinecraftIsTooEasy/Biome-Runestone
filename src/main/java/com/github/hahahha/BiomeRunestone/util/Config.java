package com.github.hahahha.BiomeRunestone.util;

import com.github.hahahha.BiomeRunestone.BiomeRunestone;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.logging.Level;

public class Config {
    private static final int CURRENT_CONFIG_VERSION = 3;
    private static final String CONFIG_FILE = "config/biome_runestone.json";
    private static ConfigData data = null;

    private static class ConfigData {
        int configVersion = CURRENT_CONFIG_VERSION;
        int randomBiomeRunegateMaxAttempts = 8192;
        int randomBiomeRunegateMaxSearchTimeMs = 50;
        int randomBiomeRunegateFailureCooldownSeconds = 8;
        int randomBiomeRunegateRefreshSeconds = 5;
        int randomBiomeRunegateMinDistance = 1000;
        int randomBiomeRunegatePortalCacheLimit = 256;
        int randomBiomeRunegateHistoryPerPortalLimit = 2048;
        int[] runegateSearchRadii = new int[]{256, 512, 1024, 2048, 4096, 8192};
        int runegateSearchMetricSampleLimit = 512;
        int runegateLandSearchSameBiomeRadius = 96;
        int runegateLandSearchAnyBiomeRadius = 256;
        int runegateLandSearchStep = 16;
        int runegateLandSearchRingSamples = 24;
        int runegateCacheSweepIntervalMs = 30000;
        int runegateLandCacheLimit = 2048;
        int runegateLandCacheExpireSeconds = 300;
        int runegateLandCacheGridSize = 128;
        int playerBiomeRunegateLockLimit = 8192;
        int playerBiomeRunegateGroupLimit = 64;
        int playerBiomeRunegateUsedHistoryPerGroupLimit = 8192;
    }

    public static void load() {
        File file = new File(CONFIG_FILE);
        if (!file.exists()) {
            saveDefault();
        }

        try (Reader reader = new FileReader(file)) {
            Gson gson = new Gson();
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            ConfigData merged = new ConfigData();
            boolean shouldRewrite = json == null;
            int loadedVersion = 0;
            if (json != null) {
                if (json.has("configVersion")) {
                    loadedVersion = json.get("configVersion").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("randomBiomeRunegateMaxAttempts")) {
                    merged.randomBiomeRunegateMaxAttempts = json.get("randomBiomeRunegateMaxAttempts").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("randomBiomeRunegateMaxSearchTimeMs")) {
                    merged.randomBiomeRunegateMaxSearchTimeMs = json.get("randomBiomeRunegateMaxSearchTimeMs").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("randomBiomeRunegateFailureCooldownSeconds")) {
                    merged.randomBiomeRunegateFailureCooldownSeconds = json.get("randomBiomeRunegateFailureCooldownSeconds").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("randomBiomeRunegateRefreshSeconds")) {
                    merged.randomBiomeRunegateRefreshSeconds = json.get("randomBiomeRunegateRefreshSeconds").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("randomBiomeRunegateMinDistance")) {
                    merged.randomBiomeRunegateMinDistance = json.get("randomBiomeRunegateMinDistance").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("randomBiomeRunegatePortalCacheLimit")) {
                    merged.randomBiomeRunegatePortalCacheLimit = json.get("randomBiomeRunegatePortalCacheLimit").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("randomBiomeRunegateHistoryPerPortalLimit")) {
                    merged.randomBiomeRunegateHistoryPerPortalLimit = json.get("randomBiomeRunegateHistoryPerPortalLimit").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateSearchRadii")) {
                    int[] parsedRadii = parseIntArray(json.get("runegateSearchRadii"));
                    if (parsedRadii != null && parsedRadii.length > 0) {
                        merged.runegateSearchRadii = parsedRadii;
                    } else {
                        shouldRewrite = true;
                    }
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateSearchMetricSampleLimit")) {
                    merged.runegateSearchMetricSampleLimit = json.get("runegateSearchMetricSampleLimit").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateLandSearchSameBiomeRadius")) {
                    merged.runegateLandSearchSameBiomeRadius = json.get("runegateLandSearchSameBiomeRadius").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateLandSearchAnyBiomeRadius")) {
                    merged.runegateLandSearchAnyBiomeRadius = json.get("runegateLandSearchAnyBiomeRadius").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateLandSearchStep")) {
                    merged.runegateLandSearchStep = json.get("runegateLandSearchStep").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateLandSearchRingSamples")) {
                    merged.runegateLandSearchRingSamples = json.get("runegateLandSearchRingSamples").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateCacheSweepIntervalMs")) {
                    merged.runegateCacheSweepIntervalMs = json.get("runegateCacheSweepIntervalMs").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateLandCacheLimit")) {
                    merged.runegateLandCacheLimit = json.get("runegateLandCacheLimit").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateLandCacheExpireSeconds")) {
                    merged.runegateLandCacheExpireSeconds = json.get("runegateLandCacheExpireSeconds").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("runegateLandCacheGridSize")) {
                    merged.runegateLandCacheGridSize = json.get("runegateLandCacheGridSize").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("playerBiomeRunegateLockLimit")) {
                    merged.playerBiomeRunegateLockLimit = json.get("playerBiomeRunegateLockLimit").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("playerBiomeRunegateGroupLimit")) {
                    merged.playerBiomeRunegateGroupLimit = json.get("playerBiomeRunegateGroupLimit").getAsInt();
                } else {
                    shouldRewrite = true;
                }
                if (json.has("playerBiomeRunegateUsedHistoryPerGroupLimit")) {
                    merged.playerBiomeRunegateUsedHistoryPerGroupLimit = json.get("playerBiomeRunegateUsedHistoryPerGroupLimit").getAsInt();
                } else {
                    shouldRewrite = true;
                }
            }

            migrateToCurrentVersion(merged, loadedVersion);
            if (loadedVersion != CURRENT_CONFIG_VERSION) {
                shouldRewrite = true;
            }
            data = merged;
            if (shouldRewrite) {
                save(data);
            }
        } catch (Exception e) {
            backupBrokenConfig(file);
            data = new ConfigData();
            save(data);
            BiomeRunestone.LOGGER.log(Level.WARNING,
                    "[Config] Failed to load " + CONFIG_FILE + ", recreated with defaults.", e);
        }
    }

    private static void saveDefault() {
        save(new ConfigData());
    }

    private static void save(ConfigData configData) {
        File file = new File(CONFIG_FILE);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        try (Writer writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(configData, writer);
        } catch (IOException e) {
            BiomeRunestone.LOGGER.log(Level.WARNING,
                    "[Config] Failed to save " + CONFIG_FILE, e);
        }
    }

    private static void backupBrokenConfig(File file) {
        if (file == null || !file.exists()) {
            return;
        }

        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        String backupName = file.getName() + ".broken_" + System.currentTimeMillis();
        File backup = parent != null ? new File(parent, backupName) : new File(backupName);
        if (file.renameTo(backup)) {
            BiomeRunestone.LOGGER.warning("[Config] Backed up invalid config to " + backup.getPath());
        } else {
            BiomeRunestone.LOGGER.warning("[Config] Failed to backup invalid config: " + file.getPath());
        }
    }

    private static void migrateToCurrentVersion(ConfigData configData, int loadedVersion) {
        if (configData == null) {
            return;
        }

        if (loadedVersion < 2) {
            if (configData.randomBiomeRunegateFailureCooldownSeconds <= 0) {
                configData.randomBiomeRunegateFailureCooldownSeconds = 8;
            }
        }
        configData.configVersion = CURRENT_CONFIG_VERSION;
    }

    private static int[] parseIntArray(JsonElement element) {
        if (element == null || !element.isJsonArray()) {
            return null;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() <= 0) {
            return new int[0];
        }
        int[] values = new int[array.size()];
        for (int i = 0; i < array.size(); ++i) {
            JsonElement item = array.get(i);
            if (item == null || !item.isJsonPrimitive() || !item.getAsJsonPrimitive().isNumber()) {
                return null;
            }
            values[i] = item.getAsInt();
        }
        return values;
    }

    private static ConfigData get() {
        if (data == null) {
            load();
        }
        return data;
    }

    public static int getRandomBiomeRunegateRefreshSeconds() {
        return Math.max(1, get().randomBiomeRunegateRefreshSeconds);
    }

    public static int getRandomBiomeRunegateMaxAttempts() {
        int attempts = get().randomBiomeRunegateMaxAttempts;
        if (attempts < 128) {
            return 128;
        }
        if (attempts > 65536) {
            return 65536;
        }
        return attempts;
    }

    public static int getRandomBiomeRunegateMaxSearchTimeMs() {
        int maxSearchTimeMs = get().randomBiomeRunegateMaxSearchTimeMs;
        if (maxSearchTimeMs < 5) {
            return 5;
        }
        if (maxSearchTimeMs > 5000) {
            return 5000;
        }
        return maxSearchTimeMs;
    }

    public static int getRandomBiomeRunegateFailureCooldownSeconds() {
        int cooldown = get().randomBiomeRunegateFailureCooldownSeconds;
        if (cooldown < 0) {
            return 0;
        }
        if (cooldown > 300) {
            return 300;
        }
        return cooldown;
    }

    public static int getRandomBiomeRunegateMinDistance() {
        return Math.max(0, get().randomBiomeRunegateMinDistance);
    }

    public static int getRandomBiomeRunegatePortalCacheLimit() {
        return Math.max(16, get().randomBiomeRunegatePortalCacheLimit);
    }

    public static int getRandomBiomeRunegateHistoryPerPortalLimit() {
        return Math.max(64, get().randomBiomeRunegateHistoryPerPortalLimit);
    }

    public static int[] getRunegateSearchRadii() {
        int[] configured = get().runegateSearchRadii;
        if (configured == null || configured.length == 0) {
            return new int[]{256, 512, 1024, 2048, 4096, 8192};
        }

        int[] copied = Arrays.copyOf(configured, configured.length);
        Arrays.sort(copied);
        int[] normalized = new int[copied.length];
        int count = 0;
        int prev = Integer.MIN_VALUE;
        for (int value : copied) {
            int radius = value;
            if (radius < 64) {
                radius = 64;
            } else if (radius > 65536) {
                radius = 65536;
            }
            if (radius == prev) {
                continue;
            }
            normalized[count++] = radius;
            prev = radius;
        }

        if (count <= 0) {
            return new int[]{256, 512, 1024, 2048, 4096, 8192};
        }
        return Arrays.copyOf(normalized, count);
    }

    public static int getRunegateSearchMetricSampleLimit() {
        int sampleLimit = get().runegateSearchMetricSampleLimit;
        if (sampleLimit < 32) {
            return 32;
        }
        if (sampleLimit > 8192) {
            return 8192;
        }
        return sampleLimit;
    }

    public static int getRunegateLandSearchSameBiomeRadius() {
        int radius = get().runegateLandSearchSameBiomeRadius;
        if (radius < 0) {
            return 0;
        }
        if (radius > 4096) {
            return 4096;
        }
        return radius;
    }

    public static int getRunegateLandSearchAnyBiomeRadius() {
        int radius = get().runegateLandSearchAnyBiomeRadius;
        if (radius < 0) {
            radius = 0;
        } else if (radius > 8192) {
            radius = 8192;
        }
        return Math.max(radius, getRunegateLandSearchSameBiomeRadius());
    }

    public static int getRunegateLandSearchStep() {
        int step = get().runegateLandSearchStep;
        if (step < 4) {
            return 4;
        }
        if (step > 256) {
            return 256;
        }
        return step;
    }

    public static int getRunegateLandSearchRingSamples() {
        int samples = get().runegateLandSearchRingSamples;
        if (samples < 8) {
            return 8;
        }
        if (samples > 128) {
            return 128;
        }
        return samples;
    }

    public static int getRunegateCacheSweepIntervalMs() {
        int interval = get().runegateCacheSweepIntervalMs;
        if (interval < 1000) {
            return 1000;
        }
        if (interval > 300000) {
            return 300000;
        }
        return interval;
    }

    public static int getRunegateLandCacheLimit() {
        return Math.max(64, get().runegateLandCacheLimit);
    }

    public static int getRunegateLandCacheExpireSeconds() {
        return Math.max(5, get().runegateLandCacheExpireSeconds);
    }

    public static int getRunegateLandCacheGridSize() {
        int gridSize = get().runegateLandCacheGridSize;
        if (gridSize < 16) {
            return 16;
        }
        if (gridSize > 1024) {
            return 1024;
        }
        return gridSize;
    }

    public static int getPlayerBiomeRunegateLockLimit() {
        return Math.max(128, get().playerBiomeRunegateLockLimit);
    }

    public static int getPlayerBiomeRunegateGroupLimit() {
        return Math.max(16, get().playerBiomeRunegateGroupLimit);
    }

    public static int getPlayerBiomeRunegateUsedHistoryPerGroupLimit() {
        return Math.max(128, get().playerBiomeRunegateUsedHistoryPerGroupLimit);
    }

}

