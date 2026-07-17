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
    private static final String CONFIG_HELP_FILE = "config/biome_runestone.readme.txt";
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
            writeHelpFile();
        } catch (Exception e) {
            backupBrokenConfig(file);
            data = new ConfigData();
            save(data);
            writeHelpFile();
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

    private static void writeHelpFile() {
        File file = new File(CONFIG_HELP_FILE);
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }

        String lineSep = System.lineSeparator();
        StringBuilder text = new StringBuilder();
        text.append("Biome Runestone 配置说明").append(lineSep);
        text.append(lineSep);
        text.append("这个文件用于解释 config/biome_runestone.json 中每个配置项的含义。").append(lineSep);
        text.append("不要直接在 biome_runestone.json 里写注释，因为 JSON 注释不一定能被正常解析。").append(lineSep);
        text.append(lineSep);
        text.append("configVersion").append(lineSep);
        text.append("  配置版本号，供模组迁移旧配置时使用。通常保持默认即可。").append(lineSep);
        text.append(lineSep);
        text.append("randomBiomeRunegateMaxAttempts").append(lineSep);
        text.append("  随机群系门单次搜索时，最多尝试多少个随机候选点。").append(lineSep);
        text.append("  数值越大，随机成功率通常越高，但也会更吃性能。").append(lineSep);
        text.append(lineSep);
        text.append("randomBiomeRunegateMaxSearchTimeMs").append(lineSep);
        text.append("  单次随机搜索允许消耗的最长时间，单位毫秒。").append(lineSep);
        text.append("  如果超过这个时间还没找到可用随机落点，就可能回退到最近可用落点。").append(lineSep);
        text.append(lineSep);
        text.append("randomBiomeRunegateFailureCooldownSeconds").append(lineSep);
        text.append("  随机搜索失败后，这个门在多少秒内不再进行完整随机搜索。").append(lineSep);
        text.append("  冷却期间会优先使用最近可用落点作为回退。").append(lineSep);
        text.append(lineSep);
        text.append("randomBiomeRunegateRefreshSeconds").append(lineSep);
        text.append("  同一个随机门结果缓存的持续时间，单位秒。").append(lineSep);
        text.append("  在这段时间内重复进入同一个门，通常会继续传到同一个缓存目的地。").append(lineSep);
        text.append(lineSep);
        text.append("randomBiomeRunegateMinDistance").append(lineSep);
        text.append("  同一个随机门的新目的地，与上一次随机目的地之间要求的最小水平距离。").append(lineSep);
        text.append("  数值越大，连续随机结果会更分散。").append(lineSep);
        text.append(lineSep);
        text.append("randomBiomeRunegatePortalCacheLimit").append(lineSep);
        text.append("  运行时最多保留多少个随机门的缓存记录。").append(lineSep);
        text.append(lineSep);
        text.append("randomBiomeRunegateHistoryPerPortalLimit").append(lineSep);
        text.append("  每个随机门在运行时最多记住多少个历史目的地。").append(lineSep);
        text.append("  用来减少同一个门重复随机到旧地点的概率。").append(lineSep);
        text.append(lineSep);
        text.append("runegateSearchRadii").append(lineSep);
        text.append("  普通群系门搜索目标群系时使用的半径列表。").append(lineSep);
        text.append("  模组会按从小到大的顺序逐个尝试，直到找到目标群系。").append(lineSep);
        text.append(lineSep);
        text.append("runegateSearchMetricSampleLimit").append(lineSep);
        text.append("  `stats` 和 `diagnose` 命令保留的运行时统计样本上限。").append(lineSep);
        text.append(lineSep);
        text.append("runegateLandSearchSameBiomeRadius").append(lineSep);
        text.append("  找到目标群系坐标后，优先在同群系范围内搜索安全落点的半径。").append(lineSep);
        text.append(lineSep);
        text.append("runegateLandSearchAnyBiomeRadius").append(lineSep);
        text.append("  如果同群系内找不到安全落点，就继续在附近任意群系内扩展搜索到这个半径。").append(lineSep);
        text.append(lineSep);
        text.append("runegateLandSearchStep").append(lineSep);
        text.append("  安全落点按圆环逐步扩散搜索时，每次增加的半径步长。").append(lineSep);
        text.append(lineSep);
        text.append("runegateLandSearchRingSamples").append(lineSep);
        text.append("  每一圈落点搜索时采样检查的点数。").append(lineSep);
        text.append("  数值越大，覆盖更密，但更吃性能。").append(lineSep);
        text.append(lineSep);
        text.append("runegateCacheSweepIntervalMs").append(lineSep);
        text.append("  运行时缓存检查并清理过期条目的时间间隔，单位毫秒。").append(lineSep);
        text.append(lineSep);
        text.append("runegateLandCacheLimit").append(lineSep);
        text.append("  运行时最多保留多少个安全落点缓存。").append(lineSep);
        text.append(lineSep);
        text.append("runegateLandCacheExpireSeconds").append(lineSep);
        text.append("  安全落点缓存的过期时间，单位秒。").append(lineSep);
        text.append(lineSep);
        text.append("runegateLandCacheGridSize").append(lineSep);
        text.append("  安全落点缓存按网格分桶时使用的网格大小。").append(lineSep);
        text.append("  数值越大，附近区域更容易复用同一份落点缓存。").append(lineSep);
        text.append(lineSep);
        text.append("playerBiomeRunegateLockLimit").append(lineSep);
        text.append("  世界存档里最多保留多少条玩家锁定门的目的地记录。").append(lineSep);
        text.append(lineSep);
        text.append("playerBiomeRunegateGroupLimit").append(lineSep);
        text.append("  世界存档里最多保留多少个玩家门分组的已使用目的地历史。").append(lineSep);
        text.append(lineSep);
        text.append("playerBiomeRunegateUsedHistoryPerGroupLimit").append(lineSep);
        text.append("  每个玩家门分组最多保留多少条已使用目的地历史记录。").append(lineSep);

        try (Writer writer = new FileWriter(file)) {
            writer.write(text.toString());
        } catch (IOException e) {
            BiomeRunestone.LOGGER.log(Level.WARNING,
                    "[Config] Failed to save " + CONFIG_HELP_FILE, e);
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

