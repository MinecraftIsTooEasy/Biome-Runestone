package com.github.hahahha.BiomeRunestone.mixins;

import com.github.hahahha.BiomeRunestone.block.BlockBiomeRunestone;
import com.github.hahahha.BiomeRunestone.util.Config;
import com.github.hahahha.BiomeRunestone.util.RandomDestinationSearchResult;
import com.github.hahahha.BiomeRunestone.util.RandomXZSearchResult;
import com.github.hahahha.BiomeRunestone.util.RunegateLandingRules;
import com.github.hahahha.BiomeRunestone.util.RunegateExternalTeamCompat;
import com.github.hahahha.BiomeRunestone.util.RunegatePlayerLockData;
import com.github.hahahha.BiomeRunestone.util.RunegateRuntimeAccess;
import com.github.hahahha.BiomeRunestone.util.RunegateSearchMetrics;
import com.github.hahahha.BiomeRunestone.util.RunegateSearchStatsData;
import com.github.hahahha.BiomeRunestone.util.RunegateSearchTuning;
import net.minecraft.BiomeGenBase;
import net.minecraft.Block;
import net.minecraft.BlockPortal;
import net.minecraft.ChunkPosition;
import net.minecraft.EnumSignal;
import net.minecraft.Material;
import net.minecraft.Packet85SimpleSignal;
import net.minecraft.ServerPlayer;
import net.minecraft.World;
import net.minecraft.WorldServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;

@Mixin(BlockPortal.class)
public abstract class BlockPortalBiomeRunegateMixin implements RunegateRuntimeAccess {
    private static final Object RANDOM_DESTINATION_LOCK = new Object();
    private static final Map<String, int[]> RANDOM_DESTINATION_CACHE = new LinkedHashMap<String, int[]>();
    private static final Map<String, Long> RANDOM_DESTINATION_CACHE_EXPIRES_AT = new LinkedHashMap<String, Long>();
    private static final Map<String, Long> RANDOM_DESTINATION_LAST = new LinkedHashMap<String, Long>();
    private static final Map<String, LinkedHashSet<Long>> RANDOM_DESTINATION_HISTORY = new LinkedHashMap<String, LinkedHashSet<Long>>();
    private static final Map<String, Long> RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL = new LinkedHashMap<String, Long>();
    private static final LinkedHashMap<String, Long> RANDOM_DESTINATION_PORTAL_TOUCH = new LinkedHashMap<String, Long>();
    private static final Object LAND_DESTINATION_CACHE_LOCK = new Object();
    private static final Map<String, int[]> LAND_DESTINATION_CACHE = new LinkedHashMap<String, int[]>();
    private static final Map<String, Long> LAND_DESTINATION_CACHE_EXPIRES_AT = new LinkedHashMap<String, Long>();
    private static final LinkedHashMap<String, Long> LAND_DESTINATION_CACHE_TOUCH = new LinkedHashMap<String, Long>();
    private static long RANDOM_CACHE_LAST_SWEEP_AT = 0L;
    private static long LAND_CACHE_LAST_SWEEP_AT = 0L;
    private static final Object PLAYER_LOCKED_DESTINATION_LOCK = new Object();
    private static final Object SEARCH_METRIC_LOCK = new Object();
    private static final LinkedList<Long> RANDOM_SEARCH_LATENCY_MS = new LinkedList<Long>();
    private static final LinkedList<Integer> RANDOM_SEARCH_ATTEMPTS = new LinkedList<Integer>();
    private static final LinkedHashMap<String, Integer> RANDOM_SEARCH_EVENTS = new LinkedHashMap<String, Integer>();
    private static final LinkedList<Long> PLAYER_SEARCH_LATENCY_MS = new LinkedList<Long>();
    private static final LinkedList<Integer> PLAYER_SEARCH_ATTEMPTS = new LinkedList<Integer>();
    private static final LinkedHashMap<String, Integer> PLAYER_SEARCH_EVENTS = new LinkedHashMap<String, Integer>();

    @Override
    public int getRuntimeRandomPortalCount() {
        synchronized (RANDOM_DESTINATION_LOCK) {
            return RANDOM_DESTINATION_CACHE.size();
        }
    }

    @Override
    public int getRuntimeRandomHistoryPortalCount() {
        synchronized (RANDOM_DESTINATION_LOCK) {
            return RANDOM_DESTINATION_HISTORY.size();
        }
    }

    @Override
    public int getRuntimeRandomHistoryEntryCount() {
        synchronized (RANDOM_DESTINATION_LOCK) {
            int total = 0;
            for (Set<Long> values : RANDOM_DESTINATION_HISTORY.values()) {
                if (values != null) {
                    total += values.size();
                }
            }
            return total;
        }
    }

    @Override
    public int getRuntimeLandCacheCount() {
        synchronized (LAND_DESTINATION_CACHE_LOCK) {
            return LAND_DESTINATION_CACHE.size();
        }
    }

    @Override
    public void clearRuntimeRandomCaches() {
        synchronized (RANDOM_DESTINATION_LOCK) {
            RANDOM_DESTINATION_CACHE.clear();
            RANDOM_DESTINATION_CACHE_EXPIRES_AT.clear();
            RANDOM_DESTINATION_LAST.clear();
            RANDOM_DESTINATION_HISTORY.clear();
            RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.clear();
            RANDOM_DESTINATION_PORTAL_TOUCH.clear();
            RANDOM_CACHE_LAST_SWEEP_AT = 0L;
        }
        synchronized (LAND_DESTINATION_CACHE_LOCK) {
            LAND_DESTINATION_CACHE.clear();
            LAND_DESTINATION_CACHE_EXPIRES_AT.clear();
            LAND_DESTINATION_CACHE_TOUCH.clear();
            LAND_CACHE_LAST_SWEEP_AT = 0L;
        }
    }

    @Override
    public void clearRuntimeSearchMetrics() {
        synchronized (SEARCH_METRIC_LOCK) {
            RANDOM_SEARCH_LATENCY_MS.clear();
            RANDOM_SEARCH_ATTEMPTS.clear();
            RANDOM_SEARCH_EVENTS.clear();
            PLAYER_SEARCH_LATENCY_MS.clear();
            PLAYER_SEARCH_ATTEMPTS.clear();
            PLAYER_SEARCH_EVENTS.clear();
        }
    }

    @Override
    public String getRandomSearchMetricSummary() {
        synchronized (SEARCH_METRIC_LOCK) {
            return RunegateSearchMetrics.buildMetricSummary(RANDOM_SEARCH_LATENCY_MS, RANDOM_SEARCH_ATTEMPTS);
        }
    }

    @Override
    public String getPlayerSearchMetricSummary() {
        synchronized (SEARCH_METRIC_LOCK) {
            return RunegateSearchMetrics.buildMetricSummary(PLAYER_SEARCH_LATENCY_MS, PLAYER_SEARCH_ATTEMPTS);
        }
    }

    @Override
    public String getRandomSearchEventSummary() {
        synchronized (SEARCH_METRIC_LOCK) {
            return RunegateSearchMetrics.buildEventSummary(RANDOM_SEARCH_EVENTS);
        }
    }

    @Override
    public String getPlayerSearchEventSummary() {
        synchronized (SEARCH_METRIC_LOCK) {
            return RunegateSearchMetrics.buildEventSummary(PLAYER_SEARCH_EVENTS);
        }
    }

    @Shadow
    private int getFrameMinX(World world, int x, int y, int z) {
        throw new AssertionError();
    }

    @Shadow
    private int getFrameMaxX(World world, int x, int y, int z) {
        throw new AssertionError();
    }

    @Shadow
    private int getFrameMinY(World world, int x, int y, int z) {
        throw new AssertionError();
    }

    @Shadow
    private int getFrameMaxY(World world, int x, int y, int z) {
        throw new AssertionError();
    }

    @Shadow
    private int getFrameMinZ(World world, int x, int y, int z) {
        throw new AssertionError();
    }

    @Shadow
    private int getFrameMaxZ(World world, int x, int y, int z) {
        throw new AssertionError();
    }

    @Inject(method = "getRunegateDestinationCoords", at = @At("HEAD"), cancellable = true)
    private void BiomeRunestone$redirectRunegateToMappedOverworldBiome(WorldServer world, int x, int y, int z, CallbackInfoReturnable<int[]> cir) {
        if (!world.isOverworld()) {
            return;
        }

        BlockBiomeRunestone runestone = this.BiomeRunestone$getUniformCornerBiomeRunestone(world, x, y, z);
        if (runestone == null) {
            return;
        }

        int biomeId = runestone.getTargetBiomeId();
        if (BiomeGenBase.biomeList == null || biomeId < 0 || biomeId >= BiomeGenBase.biomeList.length) {
            return;
        }

        BiomeGenBase targetBiome = BiomeGenBase.biomeList[biomeId];
        if (targetBiome == null) {
            return;
        }

        int[] destination;
        if (runestone.isRandomDestinationMode()) {
            destination = this.BiomeRunestone$getRandomBiomeDestinationWithCache(world, x, y, z, runestone, targetBiome);
        } else {
            destination = this.BiomeRunestone$findBiomeDestination(world, x, z, runestone.getRunestoneMaterial(), targetBiome);
        }

        if (destination != null) {
            cir.setReturnValue(destination);
        }
    }

    @Inject(method = "isRunegate(Lnet/minecraft/World;IIIZ)Z", at = @At("HEAD"), cancellable = true)
    private void BiomeRunestone$recognizeBiomeRunestoneRunegate(World world, int x, int y, int z, boolean intensiveCheck, CallbackInfoReturnable<Boolean> cir) {
        if (!world.isOverworld()) {
            return;
        }
        if (!intensiveCheck) {
            return;
        }
        if (this.BiomeRunestone$getUniformCornerBiomeRunestone(world, x, y, z) != null) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "initiateRunegateTeleport(Lnet/minecraft/WorldServer;IIILnet/minecraft/ServerPlayer;Z)V", at = @At("HEAD"), cancellable = true)
    private void BiomeRunestone$handlePlayerLockedRunegateTeleport(WorldServer world, int x, int y, int z, ServerPlayer player, boolean isPortalToWorldSpawn, CallbackInfo ci) {
        if (isPortalToWorldSpawn || !world.isOverworld()) {
            return;
        }

        BlockBiomeRunestone runestone = this.BiomeRunestone$getUniformCornerBiomeRunestone(world, x, y, z);
        if (runestone == null || !runestone.isPlayerLockedRandomMode()) {
            return;
        }

        int biomeId = runestone.getTargetBiomeId();
        if (BiomeGenBase.biomeList == null || biomeId < 0 || biomeId >= BiomeGenBase.biomeList.length) {
            return;
        }

        BiomeGenBase targetBiome = BiomeGenBase.biomeList[biomeId];
        if (targetBiome == null) {
            return;
        }

        int[] destination = this.BiomeRunestone$getPlayerLockedDestination(world, x, z, runestone, targetBiome, player);
        if (destination == null) {
            return;
        }

        player.is_runegate_teleporting = true;
        player.runegate_destination_coords = destination;
        player.playerNetServerHandler.sendPacketToPlayer(new Packet85SimpleSignal(EnumSignal.runegate_start));
        player.prevent_runegate_achievement = false;
        ci.cancel();
    }

    private BlockBiomeRunestone BiomeRunestone$getUniformCornerBiomeRunestone(World world, int x, int y, int z) {
        int frameMinX = this.getFrameMinX(world, x, y, z);
        int frameMaxX = this.getFrameMaxX(world, x, y, z);
        int frameMinY = this.getFrameMinY(world, x, y, z);
        int frameMaxY = this.getFrameMaxY(world, x, y, z);
        int frameMinZ = this.getFrameMinZ(world, x, y, z);
        int frameMaxZ = this.getFrameMaxZ(world, x, y, z);

        if (frameMaxX - frameMinX > frameMaxZ - frameMinZ) {
            Block firstBlock = world.getBlock(frameMinX, frameMinY, z);
            if (!(firstBlock instanceof BlockBiomeRunestone)) {
                return null;
            }
            if (world.getBlock(frameMaxX, frameMinY, z) != firstBlock) {
                return null;
            }
            if (world.getBlock(frameMinX, frameMaxY, z) != firstBlock) {
                return null;
            }
            if (world.getBlock(frameMaxX, frameMaxY, z) != firstBlock) {
                return null;
            }
            return (BlockBiomeRunestone) firstBlock;
        } else {
            Block firstBlock = world.getBlock(x, frameMinY, frameMinZ);
            if (!(firstBlock instanceof BlockBiomeRunestone)) {
                return null;
            }
            if (world.getBlock(x, frameMinY, frameMaxZ) != firstBlock) {
                return null;
            }
            if (world.getBlock(x, frameMaxY, frameMinZ) != firstBlock) {
                return null;
            }
            if (world.getBlock(x, frameMaxY, frameMaxZ) != firstBlock) {
                return null;
            }
            return (BlockBiomeRunestone) firstBlock;
        }
    }

    private int[] BiomeRunestone$findBiomeDestination(WorldServer world, int originX, int originZ, Material runestoneMaterial, BiomeGenBase targetBiome) {
        int maxRadius = world.getRunegateDomainRadius(runestoneMaterial);
        List<BiomeGenBase> targetBiomes = Collections.singletonList(targetBiome);
        Random random = new Random((((long)originX) << 32) ^ (originZ & 0xffffffffL) ^ targetBiome.biomeID);

        ChunkPosition biomePos = null;
        if (world.getBiomeGenForCoords(originX, originZ) == targetBiome) {
            biomePos = new ChunkPosition(originX, 0, originZ);
        } else {
            for (int radius : RunegateSearchTuning.getSearchRadii()) {
                if (radius > maxRadius) {
                    break;
                }
                biomePos = world.getWorldChunkManager().findBiomePosition(originX, originZ, radius, targetBiomes, random);
                if (biomePos != null) {
                    break;
                }
            }
        }

        if (biomePos == null) {
            return null;
        }

        return this.BiomeRunestone$resolveLandDestination(
                world,
                biomePos.x,
                biomePos.z,
                targetBiome,
                RunegateSearchTuning.getLandSearchSameBiomeRadius(),
                RunegateSearchTuning.getLandSearchAnyBiomeRadius()
        );
    }

    private int[] BiomeRunestone$getRandomBiomeDestinationWithCache(WorldServer world, int x, int y, int z, BlockBiomeRunestone runestone, BiomeGenBase targetBiome) {
        String portalKey = this.BiomeRunestone$getPortalCacheKey(world, x, y, z, runestone);
        long now = System.currentTimeMillis();
        long refreshMillis = this.BiomeRunestone$getRandomDestinationRefreshMillis();
        long minDistanceSq = this.BiomeRunestone$getRandomDestinationMinDistanceSq();
        int cacheLimit = Config.getRandomBiomeRunegatePortalCacheLimit();
        int historyLimit = Config.getRandomBiomeRunegateHistoryPerPortalLimit();

        synchronized (RANDOM_DESTINATION_LOCK) {
            this.BiomeRunestone$sweepExpiredRandomCacheEntries(now);
            this.BiomeRunestone$touchRandomPortalKey(portalKey, now, cacheLimit);

            Long expiresAt = RANDOM_DESTINATION_CACHE_EXPIRES_AT.get(portalKey);
            int[] cachedDestination = RANDOM_DESTINATION_CACHE.get(portalKey);
            if (cachedDestination != null && expiresAt != null && now < expiresAt.longValue()) {
                this.BiomeRunestone$prepareChunksAroundDestination(world, cachedDestination[0], cachedDestination[2]);
            }
            if (cachedDestination != null && expiresAt != null && now < expiresAt.longValue()
                    && this.BiomeRunestone$isLandDestination(world, cachedDestination)) {
                return new int[]{cachedDestination[0], cachedDestination[1], cachedDestination[2]};
            }

            if (this.BiomeRunestone$isRandomFailureCooldownActive(portalKey, now)) {
                int[] nearestFallback = this.BiomeRunestone$findBiomeDestination(world, x, z, runestone.getRunestoneMaterial(), targetBiome);
                if (nearestFallback != null) {
                    RANDOM_DESTINATION_CACHE.put(portalKey, nearestFallback);
                    RANDOM_DESTINATION_CACHE_EXPIRES_AT.put(portalKey, now + refreshMillis);
                    this.BiomeRunestone$recordSearchMetric(world, false, 0L, 0, "cooldown_nearest");
                    return new int[]{nearestFallback[0], nearestFallback[1], nearestFallback[2]};
                }
            }

            LinkedHashSet<Long> usedDestinations = RANDOM_DESTINATION_HISTORY.get(portalKey);
            if (usedDestinations == null) {
                usedDestinations = new LinkedHashSet<Long>();
                RANDOM_DESTINATION_HISTORY.put(portalKey, usedDestinations);
            }
            this.BiomeRunestone$trimPackedHistory(usedDestinations, historyLimit);

            long lastPacked = Long.MIN_VALUE;
            Long lastPackedObject = RANDOM_DESTINATION_LAST.get(portalKey);
            if (lastPackedObject != null) {
                lastPacked = lastPackedObject.longValue();
            }

            long searchStartedAtNanos = System.nanoTime();
            long searchDeadlineNanos = this.BiomeRunestone$getRandomSearchDeadlineNanos();
            RandomDestinationSearchResult searchResult = this.BiomeRunestone$findRandomBiomeDestination(world, runestone.getRunestoneMaterial(), targetBiome, usedDestinations, lastPacked, minDistanceSq, searchDeadlineNanos);
            long elapsedNanos = System.nanoTime() - searchStartedAtNanos;

            int[] destination = searchResult.destination;
            if (destination == null) {
                if (cachedDestination != null) {
                    this.BiomeRunestone$recordSearchMetric(world, false, elapsedNanos, searchResult.attempts, "fallback_cache");
                    this.BiomeRunestone$putRandomFailureCooldown(portalKey, now);
                    RANDOM_DESTINATION_CACHE_EXPIRES_AT.put(portalKey, now + refreshMillis);
                    return new int[]{cachedDestination[0], cachedDestination[1], cachedDestination[2]};
                }

                int[] nearestFallback = this.BiomeRunestone$findBiomeDestination(world, x, z, runestone.getRunestoneMaterial(), targetBiome);
                if (nearestFallback == null) {
                    this.BiomeRunestone$recordSearchMetric(world, false, elapsedNanos, searchResult.attempts, "fail_" + searchResult.failureReason);
                    this.BiomeRunestone$putRandomFailureCooldown(portalKey, now);
                    return null;
                }

                destination = nearestFallback;
                this.BiomeRunestone$recordSearchMetric(world, false, elapsedNanos, searchResult.attempts, "fallback_nearest");
                this.BiomeRunestone$putRandomFailureCooldown(portalKey, now);
            } else {
                this.BiomeRunestone$recordSearchMetric(world, false, elapsedNanos, searchResult.attempts, "success");
                RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.remove(portalKey);
            }

            long packed = this.BiomeRunestone$packXZ(destination[0], destination[2]);
            usedDestinations.add(packed);
            this.BiomeRunestone$trimPackedHistory(usedDestinations, historyLimit);
            RANDOM_DESTINATION_LAST.put(portalKey, packed);
            RANDOM_DESTINATION_CACHE.put(portalKey, destination);
            RANDOM_DESTINATION_CACHE_EXPIRES_AT.put(portalKey, now + refreshMillis);

            return new int[]{destination[0], destination[1], destination[2]};
        }
    }

    private int[] BiomeRunestone$getPlayerLockedDestination(WorldServer world, int originX, int originZ, BlockBiomeRunestone runestone, BiomeGenBase targetBiome, ServerPlayer player) {
        String runestoneGroupKey = this.BiomeRunestone$getRunestoneGroupKey(runestone);
        Set<String> legacyGroupKeys = this.BiomeRunestone$getLegacyRunestoneGroupKeys(runestone);
        String playerIdentity = this.BiomeRunestone$getPlayerIdentity(player);
        Set<String> identityCandidates = this.BiomeRunestone$getPlayerIdentityCandidates(player);
        String effectiveLockIdentity = this.BiomeRunestone$getEffectivePlayerLockIdentity(world, playerIdentity, identityCandidates);
        Set<String> lockIdentityCandidates = this.BiomeRunestone$getLockIdentityCandidates(playerIdentity, identityCandidates, effectiveLockIdentity);
        String playerLockKey = this.BiomeRunestone$getPlayerLockKey(runestoneGroupKey, effectiveLockIdentity);
        Set<String> legacyPlayerLockKeys = this.BiomeRunestone$getLegacyPlayerLockKeys(legacyGroupKeys, lockIdentityCandidates, playerLockKey);

        synchronized (PLAYER_LOCKED_DESTINATION_LOCK) {
            RunegatePlayerLockData saveData = RunegatePlayerLockData.get(world);

            int[] lockedDestination = saveData.getOrMigrateLockedDestination(playerLockKey, legacyPlayerLockKeys);
            if (lockedDestination != null) {
                this.BiomeRunestone$prepareChunksAroundDestination(world, lockedDestination[0], lockedDestination[2]);
                if (this.BiomeRunestone$isLandDestination(world, lockedDestination)) {
                    return new int[]{lockedDestination[0], lockedDestination[1], lockedDestination[2]};
                }
            }

            Set<Long> usedDestinations = saveData.getOrCreateUsedDestinationsWithMigration(runestoneGroupKey, legacyGroupKeys);

            long searchStartedAtNanos = System.nanoTime();
            long searchDeadlineNanos = this.BiomeRunestone$getRandomSearchDeadlineNanos();
            RandomDestinationSearchResult searchResult = this.BiomeRunestone$findRandomBiomeDestination(world, runestone.getRunestoneMaterial(), targetBiome, usedDestinations, Long.MIN_VALUE, 0L, searchDeadlineNanos);
            long elapsedNanos = System.nanoTime() - searchStartedAtNanos;

            int[] destination = searchResult.destination;
            if (destination == null) {
                int[] nearestFallback = this.BiomeRunestone$findBiomeDestination(world, originX, originZ, runestone.getRunestoneMaterial(), targetBiome);
                if (nearestFallback == null) {
                    this.BiomeRunestone$recordSearchMetric(world, true, elapsedNanos, searchResult.attempts, "fail_" + searchResult.failureReason);
                    return null;
                }
                destination = nearestFallback;
                this.BiomeRunestone$recordSearchMetric(world, true, elapsedNanos, searchResult.attempts, "fallback_nearest");
            } else {
                this.BiomeRunestone$recordSearchMetric(world, true, elapsedNanos, searchResult.attempts, "success");
            }

            saveData.addUsedDestination(runestoneGroupKey, this.BiomeRunestone$packXZ(destination[0], destination[2]));
            saveData.putLockedDestination(playerLockKey, destination);
            saveData.prune(
                    Config.getPlayerBiomeRunegateLockLimit(),
                    Config.getPlayerBiomeRunegateGroupLimit(),
                    Config.getPlayerBiomeRunegateUsedHistoryPerGroupLimit()
            );
            return new int[]{destination[0], destination[1], destination[2]};
        }
    }

    private RandomDestinationSearchResult BiomeRunestone$findRandomBiomeDestination(WorldServer world, Material runestoneMaterial, BiomeGenBase targetBiome, Set<Long> usedDestinations, long lastPacked, long minDistanceSq, long deadlineNanos) {
        int maxRadius = world.getRunegateDomainRadius(runestoneMaterial);
        Random random = new Random(System.nanoTime() ^ (((long)targetBiome.biomeID) << 32) ^ maxRadius);

        if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
            return new RandomDestinationSearchResult(null, 0, "timeout");
        }

        RandomXZSearchResult candidateResult = this.BiomeRunestone$tryFindRandomBiomeXZ(world, targetBiome, maxRadius, random, usedDestinations, lastPacked, minDistanceSq, deadlineNanos);
        if (candidateResult.xz == null) {
            return new RandomDestinationSearchResult(null, candidateResult.attempts, candidateResult.failureReason);
        }

        int[] landDestination = this.BiomeRunestone$resolveLandDestination(
                world,
                candidateResult.xz[0],
                candidateResult.xz[1],
                targetBiome,
                RunegateSearchTuning.getLandSearchSameBiomeRadius(),
                RunegateSearchTuning.getLandSearchAnyBiomeRadius(),
                deadlineNanos
        );
        if (landDestination == null) {
            if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
                return new RandomDestinationSearchResult(null, candidateResult.attempts, "timeout");
            }
            return new RandomDestinationSearchResult(null, candidateResult.attempts, "land_not_found");
        }
        return new RandomDestinationSearchResult(landDestination, candidateResult.attempts, null);
    }

    private RandomXZSearchResult BiomeRunestone$tryFindRandomBiomeXZ(WorldServer world, BiomeGenBase targetBiome, int maxRadius, Random random, Set<Long> usedDestinations, long lastPacked, long minDistanceSq, long deadlineNanos) {
        int bound = maxRadius * 2 + 1;
        int maxAttempts = Config.getRandomBiomeRunegateMaxAttempts();
        int stageOneAttempts = Math.max(4, maxAttempts / 4);
        List<BiomeGenBase> targetBiomes = Collections.singletonList(targetBiome);
        int attempts = 0;
        int biomeRejected = 0;
        int usedRejected = 0;
        int distanceRejected = 0;
        int domainRejected = 0;

        for (int attempt = 0; attempt < stageOneAttempts; ++attempt) {
            if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
                return new RandomXZSearchResult(null, attempts, "timeout");
            }
            ++attempts;
            int centerX = random.nextInt(bound) - maxRadius;
            int centerZ = random.nextInt(bound) - maxRadius;

            int searchRadius;
            if (maxRadius <= 256) {
                searchRadius = maxRadius;
            } else {
                int minSearch = Math.max(128, maxRadius / 8);
                int span = Math.max(1, maxRadius - minSearch + 1);
                searchRadius = minSearch + random.nextInt(span);
            }

            ChunkPosition biomePos = world.getWorldChunkManager().findBiomePosition(centerX, centerZ, searchRadius, targetBiomes, random);
            if (biomePos == null) {
                ++biomeRejected;
                continue;
            }

            int candidateX = biomePos.x;
            int candidateZ = biomePos.z;
            if (!world.isWithinBlockDomain(candidateX, candidateZ)) {
                ++domainRejected;
                continue;
            }
            int rejectCode = this.BiomeRunestone$getRandomBiomeCandidateRejectCode(candidateX, candidateZ, usedDestinations, lastPacked, minDistanceSq);
            if (rejectCode == 0) {
                return new RandomXZSearchResult(new int[]{candidateX, candidateZ}, attempts, null);
            } else if (rejectCode == 1) {
                ++usedRejected;
            } else if (rejectCode == 2) {
                ++distanceRejected;
            }

            for (int jitter = 0; jitter < 8; ++jitter) {
                if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
                    return new RandomXZSearchResult(null, attempts, "timeout");
                }
                ++attempts;
                int jitterX = candidateX + random.nextInt(33) - 16;
                int jitterZ = candidateZ + random.nextInt(33) - 16;
                if (!world.isWithinBlockDomain(jitterX, jitterZ)) {
                    ++domainRejected;
                    continue;
                }
                if (world.getBiomeGenForCoords(jitterX, jitterZ) != targetBiome) {
                    ++biomeRejected;
                    continue;
                }
                int jitterRejectCode = this.BiomeRunestone$getRandomBiomeCandidateRejectCode(jitterX, jitterZ, usedDestinations, lastPacked, minDistanceSq);
                if (jitterRejectCode == 0) {
                    return new RandomXZSearchResult(new int[]{jitterX, jitterZ}, attempts, null);
                } else if (jitterRejectCode == 1) {
                    ++usedRejected;
                } else if (jitterRejectCode == 2) {
                    ++distanceRejected;
                }
            }
        }

        for (int attempt = stageOneAttempts; attempt < maxAttempts; ++attempt) {
            if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
                return new RandomXZSearchResult(null, attempts, "timeout");
            }
            ++attempts;
            int x = random.nextInt(bound) - maxRadius;
            int z = random.nextInt(bound) - maxRadius;

            if (world.getBiomeGenForCoords(x, z) != targetBiome) {
                ++biomeRejected;
                continue;
            }

            int rejectCode = this.BiomeRunestone$getRandomBiomeCandidateRejectCode(x, z, usedDestinations, lastPacked, minDistanceSq);
            if (rejectCode == 0) {
                return new RandomXZSearchResult(new int[]{x, z}, attempts, null);
            } else if (rejectCode == 1) {
                ++usedRejected;
            } else if (rejectCode == 2) {
                ++distanceRejected;
            }
        }

        String failureReason;
        if (biomeRejected > 0 && biomeRejected >= usedRejected && biomeRejected >= distanceRejected) {
            failureReason = "biome_not_found";
        } else if (usedRejected > 0 && usedRejected >= distanceRejected) {
            failureReason = "used_conflict";
        } else if (distanceRejected > 0) {
            failureReason = "min_distance";
        } else if (domainRejected > 0) {
            failureReason = "domain_rejected";
        } else {
            failureReason = "no_candidate";
        }
        return new RandomXZSearchResult(null, attempts, failureReason);
    }

    private int BiomeRunestone$getRandomBiomeCandidateRejectCode(int x, int z, Set<Long> usedDestinations, long lastPacked, long minDistanceSq) {
        long packed = this.BiomeRunestone$packXZ(x, z);
        if (usedDestinations.contains(packed)) {
            return 1;
        }
        if (lastPacked != Long.MIN_VALUE && !this.BiomeRunestone$isFarEnoughFromLastDestination(x, z, lastPacked, minDistanceSq)) {
            return 2;
        }
        return 0;
    }

    private boolean BiomeRunestone$isFarEnoughFromLastDestination(int x, int z, long lastPacked, long minDistanceSq) {
        int lastX = (int)(lastPacked >> 32);
        int lastZ = (int)lastPacked;
        long dx = (long)x - lastX;
        long dz = (long)z - lastZ;
        return dx * dx + dz * dz >= minDistanceSq;
    }

    private long BiomeRunestone$getRandomDestinationRefreshMillis() {
        return Config.getRandomBiomeRunegateRefreshSeconds() * 1000L;
    }

    private long BiomeRunestone$getRandomDestinationMinDistanceSq() {
        long minDistance = Config.getRandomBiomeRunegateMinDistance();
        return minDistance * minDistance;
    }

    private long BiomeRunestone$getRandomFailureCooldownMillis() {
        return Config.getRandomBiomeRunegateFailureCooldownSeconds() * 1000L;
    }

    private void BiomeRunestone$putRandomFailureCooldown(String portalKey, long now) {
        long cooldownMillis = this.BiomeRunestone$getRandomFailureCooldownMillis();
        if (cooldownMillis <= 0L) {
            RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.remove(portalKey);
            return;
        }
        RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.put(portalKey, Long.valueOf(now + cooldownMillis));
    }

    private boolean BiomeRunestone$isRandomFailureCooldownActive(String portalKey, long now) {
        Long cooldownUntil = RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.get(portalKey);
        if (cooldownUntil == null) {
            return false;
        }
        if (now < cooldownUntil.longValue()) {
            return true;
        }
        RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.remove(portalKey);
        return false;
    }

    private long BiomeRunestone$getRandomSearchDeadlineNanos() {
        long budgetMillis = Config.getRandomBiomeRunegateMaxSearchTimeMs();
        return System.nanoTime() + budgetMillis * 1000000L;
    }

    private boolean BiomeRunestone$isSearchTimedOut(long deadlineNanos) {
        return System.nanoTime() >= deadlineNanos;
    }

    private long BiomeRunestone$getLandDestinationCacheExpireMillis() {
        return Config.getRunegateLandCacheExpireSeconds() * 1000L;
    }

    private int BiomeRunestone$getLandDestinationCacheLimit() {
        return Config.getRunegateLandCacheLimit();
    }

    private long BiomeRunestone$packXZ(int x, int z) {
        return ((long)x << 32) ^ (z & 0xffffffffL);
    }

    private String BiomeRunestone$getPortalCacheKey(World world, int x, int y, int z, BlockBiomeRunestone runestone) {
        int frameMinX = this.getFrameMinX(world, x, y, z);
        int frameMaxX = this.getFrameMaxX(world, x, y, z);
        int frameMinY = this.getFrameMinY(world, x, y, z);
        int frameMaxY = this.getFrameMaxY(world, x, y, z);
        int frameMinZ = this.getFrameMinZ(world, x, y, z);
        int frameMaxZ = this.getFrameMaxZ(world, x, y, z);

        return world.getDimensionId() + ":" +
                runestone.blockID + ":" +
                runestone.getTargetBiomeId() + ":" +
                frameMinX + ":" + frameMinY + ":" + frameMinZ + ":" +
                frameMaxX + ":" + frameMaxY + ":" + frameMaxZ;
    }

    private String BiomeRunestone$getPlayerLockKey(String runestoneGroupKey, String playerIdentity) {
        return runestoneGroupKey + ":" + playerIdentity;
    }

    private String BiomeRunestone$getRunestoneGroupKey(BlockBiomeRunestone runestone) {
        return runestone.getStableRunestoneKey() + ":" + runestone.getTargetBiomeId();
    }

    private Set<String> BiomeRunestone$getLegacyRunestoneGroupKeys(BlockBiomeRunestone runestone) {
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        int biomeId = runestone.getTargetBiomeId();
        keys.add(runestone.getUnlocalizedName() + ":" + biomeId);
        keys.add(runestone.blockID + ":" + biomeId);

        String stableKey = runestone.getStableRunestoneKey();
        int namespaceSeparator = stableKey.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < stableKey.length()) {
            keys.add(stableKey.substring(namespaceSeparator + 1) + ":" + biomeId);
        }
        return keys;
    }

    private String BiomeRunestone$getPlayerIdentity(ServerPlayer player) {
        UUID uuid = player.getUniqueIDSilent();
        if (uuid != null) {
            return uuid.toString();
        }
        return player.getCommandSenderName();
    }

    private Set<String> BiomeRunestone$getPlayerIdentityCandidates(ServerPlayer player) {
        LinkedHashSet<String> candidates = new LinkedHashSet<String>();
        String name = player.getCommandSenderName();
        if (name != null && !name.isEmpty()) {
            candidates.add(name);
            candidates.add(name.toLowerCase(Locale.ROOT));
        }
        UUID uuid = player.getUniqueIDSilent();
        if (uuid != null) {
            candidates.add(uuid.toString());
        }
        return candidates;
    }

    private String BiomeRunestone$getEffectivePlayerLockIdentity(WorldServer world, String playerIdentity, Set<String> playerIdentityCandidates) {
        String externalTeamLeaderIdentity = RunegateExternalTeamCompat.getLeaderIdentity(this.BiomeRunestone$getPlayerByIdentity(world, playerIdentity, playerIdentityCandidates));
        if (externalTeamLeaderIdentity != null && !externalTeamLeaderIdentity.isEmpty()) {
            return externalTeamLeaderIdentity;
        }
        return playerIdentity;
    }

    private Set<String> BiomeRunestone$getLockIdentityCandidates(String playerIdentity, Set<String> playerIdentityCandidates, String effectiveLockIdentity) {
        LinkedHashSet<String> candidates = new LinkedHashSet<String>();
        if (effectiveLockIdentity != null && !effectiveLockIdentity.isEmpty()) {
            candidates.add(effectiveLockIdentity);
            candidates.add(effectiveLockIdentity.toLowerCase(Locale.ROOT));
        }
        if (effectiveLockIdentity != null && effectiveLockIdentity.equals(playerIdentity) && playerIdentityCandidates != null) {
            candidates.addAll(playerIdentityCandidates);
        }
        return candidates;
    }

    private ServerPlayer BiomeRunestone$getPlayerByIdentity(WorldServer world, String playerIdentity, Set<String> playerIdentityCandidates) {
        if (world == null || world.playerEntities == null) {
            return null;
        }
        for (Object obj : world.playerEntities) {
            if (!(obj instanceof ServerPlayer)) {
                continue;
            }
            ServerPlayer player = (ServerPlayer) obj;
            String identity = this.BiomeRunestone$getPlayerIdentity(player);
            if (playerIdentity != null && playerIdentity.equals(identity)) {
                return player;
            }

            if (playerIdentityCandidates == null || playerIdentityCandidates.isEmpty()) {
                continue;
            }
            String name = player.getCommandSenderName();
            if (name != null && !name.isEmpty()) {
                if (playerIdentityCandidates.contains(name) || playerIdentityCandidates.contains(name.toLowerCase(Locale.ROOT))) {
                    return player;
                }
            }
            UUID uuid = player.getUniqueIDSilent();
            if (uuid != null && playerIdentityCandidates.contains(uuid.toString())) {
                return player;
            }
        }
        return null;
    }

    private Set<String> BiomeRunestone$getLegacyPlayerLockKeys(Set<String> legacyGroupKeys, Set<String> identityCandidates, String canonicalKey) {
        LinkedHashSet<String> keys = new LinkedHashSet<String>();
        if (legacyGroupKeys != null && identityCandidates != null) {
            for (String groupKey : legacyGroupKeys) {
                if (groupKey == null || groupKey.isEmpty()) {
                    continue;
                }
                for (String identity : identityCandidates) {
                    if (identity == null || identity.isEmpty()) {
                        continue;
                    }
                    keys.add(groupKey + ":" + identity);
                }
            }
        }
        keys.remove(canonicalKey);
        return keys;
    }

    private void BiomeRunestone$recordSearchMetric(WorldServer world, boolean playerMode, long elapsedNanos, int attempts, String eventKey) {
        synchronized (SEARCH_METRIC_LOCK) {
            LinkedList<Long> latencySamples = playerMode ? PLAYER_SEARCH_LATENCY_MS : RANDOM_SEARCH_LATENCY_MS;
            LinkedList<Integer> attemptSamples = playerMode ? PLAYER_SEARCH_ATTEMPTS : RANDOM_SEARCH_ATTEMPTS;
            LinkedHashMap<String, Integer> eventMap = playerMode ? PLAYER_SEARCH_EVENTS : RANDOM_SEARCH_EVENTS;

            long elapsedMs = Math.max(0L, elapsedNanos / 1000000L);
            RunegateSearchMetrics.appendSample(latencySamples, Long.valueOf(elapsedMs), RunegateSearchTuning.getSearchMetricSampleLimit());
            RunegateSearchMetrics.appendSample(attemptSamples, Integer.valueOf(Math.max(0, attempts)), RunegateSearchTuning.getSearchMetricSampleLimit());
            if (eventKey != null && !eventKey.isEmpty()) {
                Integer current = eventMap.get(eventKey);
                eventMap.put(eventKey, current == null ? Integer.valueOf(1) : Integer.valueOf(current.intValue() + 1));
            }
        }

        if (world != null && eventKey != null && !eventKey.isEmpty()) {
            RunegateSearchStatsData statsData = RunegateSearchStatsData.get(world);
            statsData.addEvent(playerMode, eventKey);
        }
    }

    private void BiomeRunestone$touchRandomPortalKey(String portalKey, long now, int limit) {
        RANDOM_DESTINATION_PORTAL_TOUCH.remove(portalKey);
        RANDOM_DESTINATION_PORTAL_TOUCH.put(portalKey, Long.valueOf(now));
        int cacheLimit = Math.max(1, limit);
        while (RANDOM_DESTINATION_PORTAL_TOUCH.size() > cacheLimit) {
            Iterator<String> iterator = RANDOM_DESTINATION_PORTAL_TOUCH.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }

            String evictedKey = iterator.next();
            iterator.remove();
            RANDOM_DESTINATION_CACHE.remove(evictedKey);
            RANDOM_DESTINATION_CACHE_EXPIRES_AT.remove(evictedKey);
            RANDOM_DESTINATION_LAST.remove(evictedKey);
            RANDOM_DESTINATION_HISTORY.remove(evictedKey);
            RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.remove(evictedKey);
        }
    }

    private void BiomeRunestone$sweepExpiredRandomCacheEntries(long now) {
        if (now - RANDOM_CACHE_LAST_SWEEP_AT < RunegateSearchTuning.getCacheSweepIntervalMs()) {
            return;
        }

        RANDOM_CACHE_LAST_SWEEP_AT = now;
        Iterator<Map.Entry<String, Long>> iterator = RANDOM_DESTINATION_CACHE_EXPIRES_AT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            Long expiresAt = entry.getValue();
            if (expiresAt != null && now < expiresAt.longValue()) {
                continue;
            }

            String cacheKey = entry.getKey();
            iterator.remove();
            RANDOM_DESTINATION_CACHE.remove(cacheKey);
            RANDOM_DESTINATION_LAST.remove(cacheKey);
            RANDOM_DESTINATION_HISTORY.remove(cacheKey);
            RANDOM_DESTINATION_PORTAL_TOUCH.remove(cacheKey);
            RANDOM_DESTINATION_FAILURE_COOLDOWN_UNTIL.remove(cacheKey);
        }
    }

    private void BiomeRunestone$trimPackedHistory(Set<Long> values, int limit) {
        int maxSize = Math.max(1, limit);
        while (values.size() > maxSize) {
            Iterator<Long> iterator = values.iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
        }
    }

    private int[] BiomeRunestone$resolveLandDestination(WorldServer world, int centerX, int centerZ, BiomeGenBase preferredBiome, int sameBiomeRadius, int anyBiomeRadius) {
        return this.BiomeRunestone$resolveLandDestination(world, centerX, centerZ, preferredBiome, sameBiomeRadius, anyBiomeRadius, Long.MAX_VALUE);
    }

    private int[] BiomeRunestone$resolveLandDestination(WorldServer world, int centerX, int centerZ, BiomeGenBase preferredBiome, int sameBiomeRadius, int anyBiomeRadius, long deadlineNanos) {
        if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
            return null;
        }
        String cacheKey = this.BiomeRunestone$getLandCacheKey(world, centerX, centerZ, preferredBiome, sameBiomeRadius, anyBiomeRadius);
        int[] cached = this.BiomeRunestone$getLandCacheDestination(cacheKey);
        if (cached != null) {
            this.BiomeRunestone$prepareChunksAroundDestination(world, cached[0], cached[2]);
            if (this.BiomeRunestone$isLandDestination(world, cached)) {
                return cached;
            }
            this.BiomeRunestone$removeLandCacheDestination(cacheKey);
        }

        int[] direct = this.BiomeRunestone$tryBuildLandDestination(world, centerX, centerZ, preferredBiome, true);
        if (direct != null) {
            this.BiomeRunestone$prepareChunksAroundDestination(world, direct[0], direct[2]);
            this.BiomeRunestone$putLandCacheDestination(cacheKey, direct);
            return direct;
        }

        int step = Math.max(4, RunegateSearchTuning.getLandSearchStep());
        int sameBiomeMax = Math.max(0, sameBiomeRadius);
        for (int radius = step; radius <= sameBiomeMax; radius += step) {
            if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
                return null;
            }
            int[] sampled = this.BiomeRunestone$sampleLandAtRadius(world, centerX, centerZ, radius, preferredBiome, true, deadlineNanos);
            if (sampled != null) {
                this.BiomeRunestone$prepareChunksAroundDestination(world, sampled[0], sampled[2]);
                this.BiomeRunestone$putLandCacheDestination(cacheKey, sampled);
                return sampled;
            }
        }

        int anyBiomeMax = Math.max(sameBiomeMax, anyBiomeRadius);
        for (int radius = step; radius <= anyBiomeMax; radius += step) {
            if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
                return null;
            }
            int[] sampled = this.BiomeRunestone$sampleLandAtRadius(world, centerX, centerZ, radius, preferredBiome, false, deadlineNanos);
            if (sampled != null) {
                this.BiomeRunestone$prepareChunksAroundDestination(world, sampled[0], sampled[2]);
                this.BiomeRunestone$putLandCacheDestination(cacheKey, sampled);
                return sampled;
            }
        }

        return null;
    }

    private int[] BiomeRunestone$sampleLandAtRadius(WorldServer world, int centerX, int centerZ, int radius, BiomeGenBase preferredBiome, boolean requirePreferredBiome, long deadlineNanos) {
        int samples = Math.max(8, RunegateSearchTuning.getLandSearchRingSamples());
        for (int i = 0; i < samples; ++i) {
            if (this.BiomeRunestone$isSearchTimedOut(deadlineNanos)) {
                return null;
            }
            double angle = (Math.PI * 2.0 * i) / (double)samples;
            int x = centerX + (int)Math.round(Math.cos(angle) * radius);
            int z = centerZ + (int)Math.round(Math.sin(angle) * radius);
            int[] candidate = this.BiomeRunestone$tryBuildLandDestination(world, x, z, preferredBiome, requirePreferredBiome);
            if (candidate != null) {
                return candidate;
            }
        }
        return null;
    }

    private int[] BiomeRunestone$tryBuildLandDestination(WorldServer world, int x, int z, BiomeGenBase preferredBiome, boolean requirePreferredBiome) {
        if (!world.isWithinBlockDomain(x, z)) {
            return null;
        }
        if (requirePreferredBiome && preferredBiome != null && world.getBiomeGenForCoords(x, z) != preferredBiome) {
            return null;
        }

        int y = this.BiomeRunestone$getLandFeetYAtColumn(world, x, z);
        if (y <= 0) {
            return null;
        }
        return new int[]{x, y, z};
    }

    private String BiomeRunestone$getLandCacheKey(World world, int centerX, int centerZ, BiomeGenBase preferredBiome, int sameBiomeRadius, int anyBiomeRadius) {
        int biomeId = preferredBiome == null ? -1 : preferredBiome.biomeID;
        int gridSize = Config.getRunegateLandCacheGridSize();
        int bucketX = Math.floorDiv(centerX, gridSize);
        int bucketZ = Math.floorDiv(centerZ, gridSize);
        return world.getDimensionId() + ":" + biomeId + ":" + bucketX + ":" + bucketZ + ":" + sameBiomeRadius + ":" + anyBiomeRadius + ":" + gridSize;
    }

    private int[] BiomeRunestone$getLandCacheDestination(String cacheKey) {
        long now = System.currentTimeMillis();
        synchronized (LAND_DESTINATION_CACHE_LOCK) {
            this.BiomeRunestone$sweepExpiredLandCacheEntries(now);
            Long expiresAt = LAND_DESTINATION_CACHE_EXPIRES_AT.get(cacheKey);
            int[] destination = LAND_DESTINATION_CACHE.get(cacheKey);
            if (destination == null || expiresAt == null || now >= expiresAt.longValue()) {
                this.BiomeRunestone$removeLandCacheDestinationUnsafe(cacheKey);
                return null;
            }

            LAND_DESTINATION_CACHE_TOUCH.remove(cacheKey);
            LAND_DESTINATION_CACHE_TOUCH.put(cacheKey, Long.valueOf(now));
            return new int[]{destination[0], destination[1], destination[2]};
        }
    }

    private void BiomeRunestone$putLandCacheDestination(String cacheKey, int[] destination) {
        long now = System.currentTimeMillis();
        int cacheLimit = Math.max(1, this.BiomeRunestone$getLandDestinationCacheLimit());
        long expireMs = Math.max(1000L, this.BiomeRunestone$getLandDestinationCacheExpireMillis());
        synchronized (LAND_DESTINATION_CACHE_LOCK) {
            this.BiomeRunestone$sweepExpiredLandCacheEntries(now);
            LAND_DESTINATION_CACHE_TOUCH.remove(cacheKey);
            LAND_DESTINATION_CACHE_TOUCH.put(cacheKey, Long.valueOf(now));
            LAND_DESTINATION_CACHE.put(cacheKey, new int[]{destination[0], destination[1], destination[2]});
            LAND_DESTINATION_CACHE_EXPIRES_AT.put(cacheKey, Long.valueOf(now + expireMs));

            while (LAND_DESTINATION_CACHE_TOUCH.size() > cacheLimit) {
                Iterator<String> iterator = LAND_DESTINATION_CACHE_TOUCH.keySet().iterator();
                if (!iterator.hasNext()) {
                    break;
                }
                String evicted = iterator.next();
                iterator.remove();
                LAND_DESTINATION_CACHE.remove(evicted);
                LAND_DESTINATION_CACHE_EXPIRES_AT.remove(evicted);
            }
        }
    }

    private void BiomeRunestone$sweepExpiredLandCacheEntries(long now) {
        if (now - LAND_CACHE_LAST_SWEEP_AT < RunegateSearchTuning.getCacheSweepIntervalMs()) {
            return;
        }

        LAND_CACHE_LAST_SWEEP_AT = now;
        Iterator<Map.Entry<String, Long>> iterator = LAND_DESTINATION_CACHE_EXPIRES_AT.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            Long expiresAt = entry.getValue();
            if (expiresAt != null && now < expiresAt.longValue()) {
                continue;
            }

            String cacheKey = entry.getKey();
            iterator.remove();
            LAND_DESTINATION_CACHE_TOUCH.remove(cacheKey);
            LAND_DESTINATION_CACHE.remove(cacheKey);
        }
    }

    private void BiomeRunestone$removeLandCacheDestination(String cacheKey) {
        synchronized (LAND_DESTINATION_CACHE_LOCK) {
            this.BiomeRunestone$removeLandCacheDestinationUnsafe(cacheKey);
        }
    }

    private void BiomeRunestone$removeLandCacheDestinationUnsafe(String cacheKey) {
        LAND_DESTINATION_CACHE_TOUCH.remove(cacheKey);
        LAND_DESTINATION_CACHE.remove(cacheKey);
        LAND_DESTINATION_CACHE_EXPIRES_AT.remove(cacheKey);
    }

    private int BiomeRunestone$getLandFeetYAtColumn(WorldServer world, int x, int z) {
        int y = world.getTopSolidOrLiquidBlock(x, z);
        if (y <= 0) {
            y = world.getHeightValue(x, z);
        }
        if (y <= 1 || y >= 255) {
            return -1;
        }
        if (this.BiomeRunestone$isLandTeleportFeetY(world, x, y, z)) {
            return y;
        }

        int upLimit = Math.min(253, y + 8);
        for (int yy = y + 1; yy <= upLimit; ++yy) {
            if (this.BiomeRunestone$isLandTeleportFeetY(world, x, yy, z)) {
                return yy;
            }
        }

        int downLimit = Math.max(2, y - 16);
        for (int yy = y - 1; yy >= downLimit; --yy) {
            if (this.BiomeRunestone$isLandTeleportFeetY(world, x, yy, z)) {
                return yy;
            }
        }

        return -1;
    }

    private boolean BiomeRunestone$isLandTeleportFeetY(WorldServer world, int x, int y, int z) {
        return RunegateLandingRules.isValidFeetY(world, x, y, z);
    }

    private boolean BiomeRunestone$isLandDestination(WorldServer world, int[] destination) {
        if (destination == null || destination.length < 3) {
            return false;
        }
        return this.BiomeRunestone$isLandTeleportFeetY(world, destination[0], destination[1], destination[2]);
    }

    private void BiomeRunestone$prepareChunksAroundDestination(WorldServer world, int x, int z) {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                world.getChunkProvider().provideChunk(chunkX + dx, chunkZ + dz);
            }
        }
    }

}

