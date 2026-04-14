package com.github.hahahha.BiomeRunestone.command;

import com.github.hahahha.BiomeRunestone.util.Config;
import com.github.hahahha.BiomeRunestone.util.RunegateLandingRules;
import com.github.hahahha.BiomeRunestone.util.RunegatePlayerLockData;
import com.github.hahahha.BiomeRunestone.util.RunegateRuntimeAccess;
import com.github.hahahha.BiomeRunestone.util.RunegateSearchStatsData;
import com.github.hahahha.BiomeRunestone.util.RunegateSearchTuning;
import net.minecraft.Block;
import net.minecraft.ChatMessageComponent;
import net.minecraft.CommandBase;
import net.minecraft.ICommandSender;
import net.minecraft.ServerPlayer;
import net.minecraft.WorldServer;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class CommandBiomeRunestoneRune extends CommandBase {
    private static final String USAGE = "/biome_runestone_rune <stats|diagnose|reload|clearplayer <name|uuid>|clearused|clearall|clearruntime>";

    @Override
    public String getCommandName() {
        return "biome_runestone_rune";
    }

    @Override
    public List getCommandAliases() {
        return Arrays.asList("trune", "brune", "br");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            this.BiomeRunestone$sendUsage(sender);
            return;
        }

        WorldServer overworld = this.BiomeRunestone$getOverworld();
        if (overworld == null) {
            this.BiomeRunestone$sendLine(sender, "Unable to access overworld instance.");
            return;
        }

        RunegatePlayerLockData saveData = RunegatePlayerLockData.get(overworld);
        RunegateSearchStatsData statsData = RunegateSearchStatsData.get(overworld);
        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if ("stats".equals(subCommand)) {
            this.BiomeRunestone$sendStats(sender, saveData, statsData);
            return;
        }

        if ("diagnose".equals(subCommand)) {
            this.BiomeRunestone$sendDiagnose(sender, saveData, statsData);
            return;
        }

        if ("reload".equals(subCommand)) {
            Config.load();
            this.BiomeRunestone$sendLine(sender, "Config reloaded.");
            return;
        }

        if ("clearplayer".equals(subCommand)) {
            if (args.length < 2) {
                this.BiomeRunestone$sendUsage(sender);
                return;
            }

            Set<String> identities = this.BiomeRunestone$resolvePlayerIdentities(args[1]);
            int removed = saveData.clearPlayerLocksByIdentities(identities);
            this.BiomeRunestone$sendLine(sender, "Cleared player lock records: " + removed + ", target=" + args[1]);
            return;
        }

        if ("clearused".equals(subCommand)) {
            int removed = saveData.clearAllUsedDestinations();
            this.BiomeRunestone$sendLine(sender, "Cleared used-destination records: " + removed);
            return;
        }

        if ("clearall".equals(subCommand)) {
            int lockCount = saveData.getPlayerLockCount();
            int usedCount = saveData.getTotalUsedDestinationCount();
            int persistedStatCount = statsData.clearAll();
            saveData.clearAll();
            this.BiomeRunestone$sendLine(sender, "Cleared all persisted data: playerLocks=" + lockCount
                    + ", usedDestinations=" + usedCount + ", persistedEvents=" + persistedStatCount);
            return;
        }

        if ("clearruntime".equals(subCommand)) {
            RunegateRuntimeAccess runtimeAccess = this.BiomeRunestone$getRuntimeAccess();
            if (runtimeAccess == null) {
                this.BiomeRunestone$sendLine(sender, "Unable to access runegate runtime metrics.");
                return;
            }

            int portalCount = runtimeAccess.getRuntimeRandomPortalCount();
            int historyPortalCount = runtimeAccess.getRuntimeRandomHistoryPortalCount();
            int historyEntryCount = runtimeAccess.getRuntimeRandomHistoryEntryCount();
            int landCacheCount = runtimeAccess.getRuntimeLandCacheCount();
            runtimeAccess.clearRuntimeRandomCaches();
            runtimeAccess.clearRuntimeSearchMetrics();
            this.BiomeRunestone$sendLine(sender, "Cleared runtime caches/metrics: portal=" + portalCount
                    + ", historyPortal=" + historyPortalCount + ", historyEntry=" + historyEntryCount
                    + ", landCache=" + landCacheCount);
            return;
        }

        this.BiomeRunestone$sendUsage(sender);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "stats", "diagnose", "reload", "clearplayer", "clearused", "clearall", "clearruntime");
        }
        if (args.length == 2 && "clearplayer".equalsIgnoreCase(args[0])) {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null && server.getConfigurationManager() != null) {
                return getListOfStringsMatchingLastWord(args, server.getConfigurationManager().getAllUsernames());
            }
        }
        return null;
    }

    private void BiomeRunestone$sendStats(ICommandSender sender, RunegatePlayerLockData saveData, RunegateSearchStatsData statsData) {
        RunegateRuntimeAccess runtimeAccess = this.BiomeRunestone$getRuntimeAccess();
        this.BiomeRunestone$sendLine(sender, "===== Biome Runestone RuneGate Stats =====");
        this.BiomeRunestone$sendLine(sender, "Persisted: playerLocks=" + saveData.getPlayerLockCount()
                + ", usedGroups=" + saveData.getUsedGroupCount()
                + ", usedEntries=" + saveData.getTotalUsedDestinationCount());
        this.BiomeRunestone$sendLine(sender, "PersistEventRandom: " + statsData.getRandomEventSummary());
        this.BiomeRunestone$sendLine(sender, "PersistEventPlayer: " + statsData.getPlayerEventSummary());
        if (runtimeAccess == null) {
            this.BiomeRunestone$sendLine(sender, "Runtime: unavailable");
        } else {
            int runtimePortalCount = runtimeAccess.getRuntimeRandomPortalCount();
            int runtimeHistoryPortalCount = runtimeAccess.getRuntimeRandomHistoryPortalCount();
            int runtimeHistoryEntryCount = runtimeAccess.getRuntimeRandomHistoryEntryCount();
            int runtimeLandCacheCount = runtimeAccess.getRuntimeLandCacheCount();

            this.BiomeRunestone$sendLine(sender, "Runtime: portalCache=" + runtimePortalCount
                    + ", historyPortals=" + runtimeHistoryPortalCount
                    + ", historyEntries=" + runtimeHistoryEntryCount
                    + ", landCache=" + runtimeLandCacheCount);
            this.BiomeRunestone$sendLine(sender, "SearchRandom: " + runtimeAccess.getRandomSearchMetricSummary());
            this.BiomeRunestone$sendLine(sender, "SearchPlayer: " + runtimeAccess.getPlayerSearchMetricSummary());
            this.BiomeRunestone$sendLine(sender, "EventRandom: " + runtimeAccess.getRandomSearchEventSummary());
            this.BiomeRunestone$sendLine(sender, "EventPlayer: " + runtimeAccess.getPlayerSearchEventSummary());
        }
        this.BiomeRunestone$sendLine(sender, "Config: maxAttempts=" + Config.getRandomBiomeRunegateMaxAttempts()
                + ", maxSearchMs=" + Config.getRandomBiomeRunegateMaxSearchTimeMs()
                + ", failCooldownSec=" + Config.getRandomBiomeRunegateFailureCooldownSeconds()
                + ", refreshSec=" + Config.getRandomBiomeRunegateRefreshSeconds()
                + ", minDistance=" + Config.getRandomBiomeRunegateMinDistance());
        this.BiomeRunestone$sendLine(sender, "Config: portalCacheLimit=" + Config.getRandomBiomeRunegatePortalCacheLimit()
                + ", historyPerPortalLimit=" + Config.getRandomBiomeRunegateHistoryPerPortalLimit());
        this.BiomeRunestone$sendLine(sender, "Config: landCacheLimit=" + Config.getRunegateLandCacheLimit()
                + ", landCacheExpireSec=" + Config.getRunegateLandCacheExpireSeconds()
                + ", landCacheGrid=" + Config.getRunegateLandCacheGridSize());
        this.BiomeRunestone$sendLine(sender, "Config: playerLockLimit=" + Config.getPlayerBiomeRunegateLockLimit()
                + ", groupLimit=" + Config.getPlayerBiomeRunegateGroupLimit()
                + ", usedPerGroupLimit=" + Config.getPlayerBiomeRunegateUsedHistoryPerGroupLimit());
    }

    private void BiomeRunestone$sendDiagnose(ICommandSender sender, RunegatePlayerLockData saveData, RunegateSearchStatsData statsData) {
        RunegateRuntimeAccess runtimeAccess = this.BiomeRunestone$getRuntimeAccess();
        this.BiomeRunestone$sendLine(sender, "===== Biome Runestone RuneGate Diagnose =====");
        this.BiomeRunestone$sendLine(sender, "LandingRule: " + RunegateLandingRules.getRuleSummary());
        this.BiomeRunestone$sendLine(sender, "SearchTune: " + RunegateSearchTuning.getSummary()
                + ", maxAttempts=" + Config.getRandomBiomeRunegateMaxAttempts()
                + ", refreshSec=" + Config.getRandomBiomeRunegateRefreshSeconds()
                + ", minDistance=" + Config.getRandomBiomeRunegateMinDistance());
        this.BiomeRunestone$sendLine(sender, "Persisted: playerLocks=" + saveData.getPlayerLockCount()
                + ", usedGroups=" + saveData.getUsedGroupCount()
                + ", usedEntries=" + saveData.getTotalUsedDestinationCount());
        this.BiomeRunestone$sendLine(sender, "PersistEventRandom: " + statsData.getRandomEventSummary());
        this.BiomeRunestone$sendLine(sender, "PersistEventPlayer: " + statsData.getPlayerEventSummary());

        if (runtimeAccess == null) {
            this.BiomeRunestone$sendLine(sender, "Runtime: unavailable");
            return;
        }

        this.BiomeRunestone$sendLine(sender, "Runtime: portalCache=" + runtimeAccess.getRuntimeRandomPortalCount()
                + ", historyPortals=" + runtimeAccess.getRuntimeRandomHistoryPortalCount()
                + ", historyEntries=" + runtimeAccess.getRuntimeRandomHistoryEntryCount()
                + ", landCache=" + runtimeAccess.getRuntimeLandCacheCount());
        this.BiomeRunestone$sendLine(sender, "MetricRandom: " + runtimeAccess.getRandomSearchMetricSummary());
        this.BiomeRunestone$sendLine(sender, "MetricPlayer: " + runtimeAccess.getPlayerSearchMetricSummary());
        this.BiomeRunestone$sendLine(sender, "EventRandom: " + runtimeAccess.getRandomSearchEventSummary());
        this.BiomeRunestone$sendLine(sender, "EventPlayer: " + runtimeAccess.getPlayerSearchEventSummary());
    }

    private Set<String> BiomeRunestone$resolvePlayerIdentities(String token) {
        LinkedHashSet<String> identities = new LinkedHashSet<String>();
        identities.add(token);

        try {
            identities.add(UUID.fromString(token).toString());
        } catch (IllegalArgumentException ignored) {
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.getConfigurationManager() != null) {
            ServerPlayer onlinePlayer = server.getConfigurationManager().getPlayerForUsername(token);
            if (onlinePlayer != null) {
                identities.add(onlinePlayer.getCommandSenderName());
                UUID uuid = onlinePlayer.getUniqueIDSilent();
                if (uuid != null) {
                    identities.add(uuid.toString());
                }
            }
        }

        return identities;
    }

    private WorldServer BiomeRunestone$getOverworld() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        return server.worldServerForDimension(0);
    }

    private void BiomeRunestone$sendUsage(ICommandSender sender) {
        this.BiomeRunestone$sendLine(sender, "Usage: " + USAGE);
    }

    private RunegateRuntimeAccess BiomeRunestone$getRuntimeAccess() {
        if (Block.portal instanceof RunegateRuntimeAccess) {
            return (RunegateRuntimeAccess) Block.portal;
        }
        return null;
    }

    private void BiomeRunestone$sendLine(ICommandSender sender, String line) {
        sender.sendChatToPlayer(ChatMessageComponent.createFromText("[Biome Runestone] " + line));
    }
}

