package com.github.hahahha.BiomeRunestone.command;

import com.github.hahahha.BiomeRunestone.util.Config;
import com.github.hahahha.BiomeRunestone.util.RunegatePlayerIdentityIndexData;
import com.github.hahahha.BiomeRunestone.util.RunegatePlayerLockData;
import com.github.hahahha.BiomeRunestone.util.RunegateTeamEventNoticeData;
import com.github.hahahha.BiomeRunestone.util.RunegateTeamData;
import net.minecraft.ChatMessageComponent;
import net.minecraft.CommandBase;
import net.minecraft.ICommandSender;
import net.minecraft.ServerPlayer;
import net.minecraft.WorldServer;
import net.minecraft.server.MinecraftServer;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class CommandBiomeRunestoneTeam extends CommandBase {
    private static final String USAGE = "/biome_runestone_team <create|invite <player>|accept|deny|kick <player>|transfer <player>|leave [confirm]|disband [confirm]|info|query>";
    private static final String MESSAGE_PREFIX_KEY = "message.biome_runestone.team.prefix";
    private static final long CONFIRM_TIMEOUT_MS = 30000L;
    private static final int MAX_PENDING_CONFIRMATIONS = 1024;
    private static final LinkedHashMap<String, PendingConfirmation> PENDING_CONFIRMATIONS = new LinkedHashMap<String, PendingConfirmation>();
    private static final int MAX_INVITE_COOLDOWN_TRACK = 2048;
    private static final LinkedHashMap<String, Long> INVITE_COOLDOWN_BY_LEADER = new LinkedHashMap<String, Long>();

    private static final class PendingConfirmation {
        private final String action;
        private final long expiresAt;

        private PendingConfirmation(String action, long expiresAt) {
            this.action = action;
            this.expiresAt = expiresAt;
        }
    }

    private static final class InviteTarget {
        private final String canonicalIdentity;
        private final Set<String> identityCandidates;
        private final ServerPlayer onlinePlayer;
        private final boolean offlineFromNameToken;

        private InviteTarget(String canonicalIdentity, Set<String> identityCandidates, ServerPlayer onlinePlayer, boolean offlineFromNameToken) {
            this.canonicalIdentity = canonicalIdentity;
            this.identityCandidates = identityCandidates;
            this.onlinePlayer = onlinePlayer;
            this.offlineFromNameToken = offlineFromNameToken;
        }
    }

    @Override
    public String getCommandName() {
        return "biome_runestone_team";
    }

    @Override
    public List getCommandAliases() {
        return Arrays.asList("brteam", "bteam");
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof ServerPlayer)) {
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.only_player");
            return;
        }

        ServerPlayer player = (ServerPlayer) sender;
        WorldServer overworld = this.BiomeRunestone$getOverworld();
        if (overworld == null) {
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.overworld_unavailable");
            return;
        }

        this.BiomeRunestone$sweepPendingConfirmations();

        String playerIdentity = this.BiomeRunestone$getPlayerIdentity(player);
        Set<String> playerIdentityCandidates = this.BiomeRunestone$getPlayerIdentityCandidates(player);
        RunegateTeamData teamData = RunegateTeamData.get(overworld);

        if (args.length == 0) {
            this.BiomeRunestone$sendUsage(sender);
            return;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if ("info".equals(subCommand)) {
            this.BiomeRunestone$sendTeamInfo(sender, teamData, playerIdentity, playerIdentityCandidates);
            return;
        }

        if ("query".equals(subCommand)) {
            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader == null || !playerIdentity.equals(currentLeader)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.query_only_leader");
                return;
            }
            List<String> members = teamData.getMembers(playerIdentity, playerIdentityCandidates);
            List<String> pendingInvites = teamData.getPendingInviteTargets(playerIdentity, playerIdentityCandidates);
            int pendingOnlineInvites = teamData.getPendingOnlineInviteCountForLeader(playerIdentity, playerIdentityCandidates);
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.query_title");
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_members_count", members.size());
            if (!members.isEmpty()) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_member_list", this.BiomeRunestone$joinIdentities(members, 12));
            }
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_pending_invites_count", pendingInvites.size());
            if (!pendingInvites.isEmpty()) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_pending_invites_list", this.BiomeRunestone$joinIdentities(pendingInvites, 12));
            }
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_pending_online_invites_count", pendingOnlineInvites);
            return;
        }

        if ("create".equals(subCommand)) {
            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader != null) {
                if (playerIdentity.equals(currentLeader)) {
                    this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.already_leader");
                } else {
                    this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.already_in_team", this.BiomeRunestone$formatIdentity(currentLeader));
                }
                return;
            }

            if (!teamData.createTeam(playerIdentity)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.create_failed");
                return;
            }

            this.BiomeRunestone$clearPendingConfirmation(playerIdentity);
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.created");
            return;
        }

        if ("invite".equals(subCommand)) {
            if (args.length < 2) {
                this.BiomeRunestone$sendUsage(sender);
                return;
            }

            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader == null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.create_first");
                return;
            }
            if (!playerIdentity.equals(currentLeader)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_only_leader");
                return;
            }

            int maxTeamMembers = Config.getTeamMaxMembers();
            int currentTeamSize = teamData.getTeamSize(currentLeader, playerIdentityCandidates);
            if (currentTeamSize >= maxTeamMembers) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_team_full", maxTeamMembers);
                return;
            }

            boolean senderIsOp = sender.canCommandSenderUseCommand(2, this.getCommandName());
            if (!senderIsOp) {
                int maxPendingInvites = Config.getTeamMaxPendingInvitesPerLeader();
                int pendingInviteCount = teamData.getPendingInviteCountForLeader(currentLeader, playerIdentityCandidates);
                if (pendingInviteCount >= maxPendingInvites) {
                    this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_pending_limit", maxPendingInvites);
                    return;
                }

                long cooldownRemainingMillis = this.BiomeRunestone$getInviteCooldownRemainingMillis(currentLeader);
                if (cooldownRemainingMillis > 0L) {
                    int cooldownRemainSeconds = (int) ((cooldownRemainingMillis + 999L) / 1000L);
                    this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_cooldown", cooldownRemainSeconds);
                    return;
                }
            }

            InviteTarget target = this.BiomeRunestone$resolveInviteTarget(args[1]);
            if (target == null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_target_invalid", args[1]);
                return;
            }

            if (this.BiomeRunestone$isIdentityMatch(target.canonicalIdentity, playerIdentity, playerIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_self");
                return;
            }

            String targetLeader = teamData.getTeamLeader(target.canonicalIdentity, target.identityCandidates);
            if (targetLeader != null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.target_in_team", this.BiomeRunestone$formatIdentity(targetLeader));
                return;
            }

            if (!teamData.setInvite(target.canonicalIdentity, playerIdentity, target.onlinePlayer != null)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_failed");
                return;
            }

            this.BiomeRunestone$setInviteCooldown(currentLeader);

            int inviteExpireSeconds = RunegateTeamData.getInviteExpireSeconds();
            if (target.onlinePlayer != null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_sent", target.onlinePlayer.getCommandSenderName(), inviteExpireSeconds);
                this.BiomeRunestone$sendKey(target.onlinePlayer, "message.biome_runestone.team.invite_notify", player.getCommandSenderName(), inviteExpireSeconds);
            } else {
                String messageKey = target.offlineFromNameToken
                        ? "message.biome_runestone.team.invite_sent_offline_name"
                        : "message.biome_runestone.team.invite_sent_offline_uuid";
                String displayTarget = target.offlineFromNameToken ? args[1] : target.canonicalIdentity;
                this.BiomeRunestone$sendKey(sender, messageKey, displayTarget);
            }
            return;
        }

        if ("accept".equals(subCommand)) {
            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader != null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.accept_leave_first");
                return;
            }

            String inviteLeader = teamData.getInviteLeader(playerIdentity, playerIdentityCandidates);
            if (inviteLeader == null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.no_invite");
                return;
            }

            int maxTeamMembers = Config.getTeamMaxMembers();
            int inviteLeaderTeamSize = teamData.getTeamSize(inviteLeader, this.BiomeRunestone$singleIdentitySet(inviteLeader));
            if (inviteLeaderTeamSize >= maxTeamMembers) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.join_team_full", maxTeamMembers);
                return;
            }

            if (!teamData.joinTeam(playerIdentity, inviteLeader)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.join_failed");
                return;
            }

            this.BiomeRunestone$clearPendingConfirmation(playerIdentity);
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.joined", this.BiomeRunestone$formatIdentity(inviteLeader));

            this.BiomeRunestone$broadcastTeamEvent(
                    overworld,
                    teamData,
                    inviteLeader,
                    this.BiomeRunestone$singleIdentitySet(inviteLeader),
                    this.BiomeRunestone$singleIdentitySet(playerIdentity),
                    "message.biome_runestone.team.team_broadcast_joined",
                    player.getCommandSenderName()
            );
            return;
        }

        if ("deny".equals(subCommand)) {
            if (!teamData.clearInvite(playerIdentity, playerIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.no_invite");
                return;
            }
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.invite_rejected");
            return;
        }

        if ("kick".equals(subCommand)) {
            if (args.length < 2) {
                this.BiomeRunestone$sendUsage(sender);
                return;
            }

            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader == null || !this.BiomeRunestone$isIdentityMatch(currentLeader, playerIdentity, playerIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.kick_only_leader");
                return;
            }

            ServerPlayer target = this.BiomeRunestone$getOnlinePlayerByName(args[1]);
            if (target == null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.player_offline", args[1]);
                return;
            }

            String targetIdentity = this.BiomeRunestone$getPlayerIdentity(target);
            Set<String> targetIdentityCandidates = this.BiomeRunestone$getPlayerIdentityCandidates(target);
            if (this.BiomeRunestone$isIdentityMatch(currentLeader, targetIdentity, targetIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.kick_self");
                return;
            }

            String targetLeader = teamData.getTeamLeader(targetIdentity, targetIdentityCandidates);
            if (targetLeader == null || !currentLeader.equals(targetLeader)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.kick_not_same_team");
                return;
            }

            if (!teamData.leaveTeam(targetIdentity, targetIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.kick_failed");
                return;
            }

            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.kick_success", target.getCommandSenderName());
            this.BiomeRunestone$sendKey(target, "message.biome_runestone.team.kick_notify", player.getCommandSenderName());

            LinkedHashSet<String> exclude = this.BiomeRunestone$singleIdentitySet(playerIdentity);
            exclude.add(targetIdentity);
            if (targetIdentityCandidates != null) {
                exclude.addAll(targetIdentityCandidates);
            }
            this.BiomeRunestone$broadcastTeamEvent(
                    overworld,
                    teamData,
                    currentLeader,
                    this.BiomeRunestone$singleIdentitySet(currentLeader),
                    exclude,
                    "message.biome_runestone.team.team_broadcast_kicked",
                    target.getCommandSenderName(),
                    player.getCommandSenderName()
            );
            return;
        }

        if ("transfer".equals(subCommand)) {
            if (args.length < 2) {
                this.BiomeRunestone$sendUsage(sender);
                return;
            }

            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader == null || !this.BiomeRunestone$isIdentityMatch(currentLeader, playerIdentity, playerIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.transfer_only_leader");
                return;
            }

            ServerPlayer target = this.BiomeRunestone$getOnlinePlayerByName(args[1]);
            if (target == null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.player_offline", args[1]);
                return;
            }

            String targetIdentity = this.BiomeRunestone$getPlayerIdentity(target);
            Set<String> targetIdentityCandidates = this.BiomeRunestone$getPlayerIdentityCandidates(target);
            if (this.BiomeRunestone$isIdentityMatch(currentLeader, targetIdentity, targetIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.transfer_self");
                return;
            }

            String targetLeader = teamData.getTeamLeader(targetIdentity, targetIdentityCandidates);
            if (targetLeader == null || !currentLeader.equals(targetLeader)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.transfer_not_same_team");
                return;
            }

            String transferredLeader = teamData.transferLeadership(playerIdentity, playerIdentityCandidates, targetIdentity, targetIdentityCandidates);
            if (transferredLeader == null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.transfer_failed");
                return;
            }

            LinkedHashSet<String> oldLeaderCandidates = new LinkedHashSet<String>();
            oldLeaderCandidates.add(currentLeader);
            oldLeaderCandidates.addAll(playerIdentityCandidates);
            int migratedLocks = RunegatePlayerLockData.get(overworld).transferPlayerLockIdentity(transferredLeader, oldLeaderCandidates);

            this.BiomeRunestone$clearPendingConfirmation(playerIdentity);
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.transfer_success", target.getCommandSenderName());
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.transfer_lock_migrated", migratedLocks);
            this.BiomeRunestone$sendKey(target, "message.biome_runestone.team.transfer_notify", player.getCommandSenderName());

            LinkedHashSet<String> exclude = this.BiomeRunestone$singleIdentitySet(playerIdentity);
            exclude.add(targetIdentity);
            if (targetIdentityCandidates != null) {
                exclude.addAll(targetIdentityCandidates);
            }
            this.BiomeRunestone$broadcastTeamEvent(
                    overworld,
                    teamData,
                    transferredLeader,
                    this.BiomeRunestone$singleIdentitySet(transferredLeader),
                    exclude,
                    "message.biome_runestone.team.team_broadcast_transfer",
                    player.getCommandSenderName(),
                    target.getCommandSenderName()
            );
            return;
        }

        if ("leave".equals(subCommand)) {
            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader == null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.not_in_team");
                return;
            }
            if (playerIdentity.equals(currentLeader)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.leader_cannot_leave");
                return;
            }

            boolean confirm = args.length >= 2 && "confirm".equalsIgnoreCase(args[1]);
            if (!confirm) {
                this.BiomeRunestone$setPendingConfirmation(playerIdentity, "leave");
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.leave_confirm_prompt");
                return;
            }

            if (!this.BiomeRunestone$consumePendingConfirmation(playerIdentity, "leave")) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.confirmation_expired", "/biome_runestone_team leave");
                return;
            }

            if (!teamData.leaveTeam(playerIdentity, playerIdentityCandidates)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.leave_failed");
                return;
            }

            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.left");
            return;
        }

        if ("disband".equals(subCommand)) {
            String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
            if (currentLeader == null || !playerIdentity.equals(currentLeader)) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.disband_only_leader");
                return;
            }

            boolean confirm = args.length >= 2 && "confirm".equalsIgnoreCase(args[1]);
            if (!confirm) {
                this.BiomeRunestone$setPendingConfirmation(playerIdentity, "disband");
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.disband_confirm_prompt");
                return;
            }

            if (!this.BiomeRunestone$consumePendingConfirmation(playerIdentity, "disband")) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.confirmation_expired", "/biome_runestone_team disband");
                return;
            }

            List<String> membersBeforeDisband = teamData.getMembers(playerIdentity, playerIdentityCandidates);
            int removedMembers = teamData.disbandTeam(playerIdentity, playerIdentityCandidates);
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.disbanded", removedMembers);
            if (!membersBeforeDisband.isEmpty()) {
                this.BiomeRunestone$broadcastEventToIdentities(
                        overworld,
                        new LinkedHashSet<String>(membersBeforeDisband),
                        null,
                        "message.biome_runestone.team.team_broadcast_disband",
                        player.getCommandSenderName()
                );
            }
            return;
        }

        this.BiomeRunestone$sendUsage(sender);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, "create", "invite", "accept", "deny", "kick", "transfer", "leave", "disband", "info", "query");
        }
        if (args.length == 2 && ("invite".equalsIgnoreCase(args[0]) || "kick".equalsIgnoreCase(args[0]) || "transfer".equalsIgnoreCase(args[0]))) {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null && server.getConfigurationManager() != null) {
                return getListOfStringsMatchingLastWord(args, server.getConfigurationManager().getAllUsernames());
            }
        }
        if (args.length == 2 && ("leave".equalsIgnoreCase(args[0]) || "disband".equalsIgnoreCase(args[0]))) {
            return getListOfStringsMatchingLastWord(args, "confirm");
        }
        return null;
    }

    private void BiomeRunestone$sendTeamInfo(ICommandSender sender, RunegateTeamData teamData, String playerIdentity, Set<String> playerIdentityCandidates) {
        String currentLeader = teamData.getTeamLeader(playerIdentity, playerIdentityCandidates);
        String inviteLeader = teamData.getInviteLeader(playerIdentity, playerIdentityCandidates);

        if (currentLeader == null) {
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_none");
            if (inviteLeader != null) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_pending_invite", this.BiomeRunestone$formatIdentity(inviteLeader));
            }
            return;
        }

        if (playerIdentity.equals(currentLeader)) {
            List<String> members = teamData.getMembers(playerIdentity, playerIdentityCandidates);
            List<String> pendingInvites = teamData.getPendingInviteTargets(playerIdentity, playerIdentityCandidates);
            int pendingOnlineInvites = teamData.getPendingOnlineInviteCountForLeader(playerIdentity, playerIdentityCandidates);
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_role_leader");
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_members_count", members.size());
            if (!members.isEmpty()) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_member_list", this.BiomeRunestone$joinIdentities(members, 12));
            }
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_pending_invites_count", pendingInvites.size());
            if (!pendingInvites.isEmpty()) {
                this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_pending_invites_list", this.BiomeRunestone$joinIdentities(pendingInvites, 12));
            }
            this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_pending_online_invites_count", pendingOnlineInvites);
            return;
        }

        this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_role_member");
        this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.info_leader", this.BiomeRunestone$formatIdentity(currentLeader));
    }

    private String BiomeRunestone$joinIdentities(List<String> identities, int maxDisplayCount) {
        StringBuilder builder = new StringBuilder();
        int limit = Math.max(1, maxDisplayCount);
        int displayCount = Math.min(identities.size(), limit);
        for (int i = 0; i < displayCount; ++i) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(this.BiomeRunestone$formatIdentity(identities.get(i)));
        }
        if (identities.size() > displayCount) {
            builder.append(", ...");
        }
        return builder.toString();
    }

    private String BiomeRunestone$formatIdentity(String identity) {
        if (identity == null || identity.isEmpty()) {
            return "unknown";
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return identity;
        }

        String[] usernames = server.getConfigurationManager().getAllUsernames();
        if (usernames == null) {
            return identity;
        }

        for (String username : usernames) {
            ServerPlayer onlinePlayer = server.getConfigurationManager().getPlayerForUsername(username);
            if (onlinePlayer == null) {
                continue;
            }
            if (identity.equals(this.BiomeRunestone$getPlayerIdentity(onlinePlayer))) {
                return onlinePlayer.getCommandSenderName();
            }
        }
        return identity;
    }

    private ServerPlayer BiomeRunestone$getOnlinePlayerByName(String username) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return null;
        }
        return server.getConfigurationManager().getPlayerForUsername(username);
    }

    private ServerPlayer BiomeRunestone$getOnlinePlayerByUuid(String uuidString) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return null;
        }
        String[] usernames = server.getConfigurationManager().getAllUsernames();
        if (usernames == null) {
            return null;
        }
        for (String username : usernames) {
            ServerPlayer onlinePlayer = server.getConfigurationManager().getPlayerForUsername(username);
            if (onlinePlayer == null) {
                continue;
            }
            String identity = this.BiomeRunestone$getPlayerIdentity(onlinePlayer);
            if (uuidString.equals(identity)) {
                return onlinePlayer;
            }
        }
        return null;
    }

    private InviteTarget BiomeRunestone$resolveInviteTarget(String token) {
        String normalizedToken = this.BiomeRunestone$normalizeIdentity(token);
        if (normalizedToken == null) {
            return null;
        }

        try {
            String uuidIdentity = UUID.fromString(normalizedToken).toString();
            ServerPlayer onlineByUuid = this.BiomeRunestone$getOnlinePlayerByUuid(uuidIdentity);
            if (onlineByUuid != null) {
                return new InviteTarget(
                        this.BiomeRunestone$getPlayerIdentity(onlineByUuid),
                        this.BiomeRunestone$getPlayerIdentityCandidates(onlineByUuid),
                        onlineByUuid,
                        false
                );
            }
            LinkedHashSet<String> candidates = new LinkedHashSet<String>();
            candidates.add(uuidIdentity);
            return new InviteTarget(uuidIdentity, candidates, null, false);
        } catch (IllegalArgumentException ignored) {
        }

        ServerPlayer onlineByName = this.BiomeRunestone$getOnlinePlayerByName(normalizedToken);
        if (onlineByName != null) {
            return new InviteTarget(
                    this.BiomeRunestone$getPlayerIdentity(onlineByName),
                    this.BiomeRunestone$getPlayerIdentityCandidates(onlineByName),
                    onlineByName,
                    false
            );
        }

        String cachedUuidIdentity = this.BiomeRunestone$getCachedUuidByName(normalizedToken);
        if (cachedUuidIdentity != null) {
            ServerPlayer onlineByCachedUuid = this.BiomeRunestone$getOnlinePlayerByUuid(cachedUuidIdentity);
            if (onlineByCachedUuid != null) {
                return new InviteTarget(
                        this.BiomeRunestone$getPlayerIdentity(onlineByCachedUuid),
                        this.BiomeRunestone$getPlayerIdentityCandidates(onlineByCachedUuid),
                        onlineByCachedUuid,
                        false
                );
            }
            LinkedHashSet<String> candidates = new LinkedHashSet<String>();
            candidates.add(cachedUuidIdentity);
            candidates.add(normalizedToken);
            candidates.add(normalizedToken.toLowerCase(Locale.ROOT));
            return new InviteTarget(cachedUuidIdentity, candidates, null, true);
        }

        String offlineNameIdentity = normalizedToken.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> candidates = new LinkedHashSet<String>();
        candidates.add(normalizedToken);
        candidates.add(offlineNameIdentity);
        return new InviteTarget(offlineNameIdentity, candidates, null, true);
    }

    private String BiomeRunestone$getCachedUuidByName(String playerName) {
        WorldServer overworld = this.BiomeRunestone$getOverworld();
        if (overworld == null) {
            return null;
        }
        return RunegatePlayerIdentityIndexData.get(overworld).getUuidByName(playerName);
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

    private WorldServer BiomeRunestone$getOverworld() {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return null;
        }
        return server.worldServerForDimension(0);
    }

    private LinkedHashSet<String> BiomeRunestone$singleIdentitySet(String identity) {
        LinkedHashSet<String> identities = new LinkedHashSet<String>();
        String normalized = this.BiomeRunestone$normalizeIdentity(identity);
        if (normalized != null) {
            identities.add(normalized);
        }
        return identities;
    }

    private long BiomeRunestone$getInviteCooldownRemainingMillis(String leaderIdentity) {
        int cooldownSeconds = Config.getTeamInviteCooldownSeconds();
        if (cooldownSeconds <= 0) {
            return 0L;
        }

        String normalizedLeader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (normalizedLeader == null) {
            return 0L;
        }

        long now = System.currentTimeMillis();
        synchronized (INVITE_COOLDOWN_BY_LEADER) {
            Long lastInviteAt = INVITE_COOLDOWN_BY_LEADER.get(normalizedLeader);
            if (lastInviteAt == null) {
                return 0L;
            }
            long cooldownMillis = cooldownSeconds * 1000L;
            long remaining = (lastInviteAt.longValue() + cooldownMillis) - now;
            if (remaining <= 0L) {
                INVITE_COOLDOWN_BY_LEADER.remove(normalizedLeader);
                return 0L;
            }
            return remaining;
        }
    }

    private void BiomeRunestone$setInviteCooldown(String leaderIdentity) {
        String normalizedLeader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (normalizedLeader == null) {
            return;
        }
        long now = System.currentTimeMillis();
        synchronized (INVITE_COOLDOWN_BY_LEADER) {
            INVITE_COOLDOWN_BY_LEADER.put(normalizedLeader, Long.valueOf(now));
            while (INVITE_COOLDOWN_BY_LEADER.size() > MAX_INVITE_COOLDOWN_TRACK) {
                String oldest = INVITE_COOLDOWN_BY_LEADER.keySet().iterator().next();
                INVITE_COOLDOWN_BY_LEADER.remove(oldest);
            }
        }
    }

    private void BiomeRunestone$broadcastTeamEvent(WorldServer overworld, RunegateTeamData teamData,
                                                    String leaderIdentity, Set<String> leaderIdentityCandidates,
                                                    Set<String> excludedIdentities,
                                                    String translationKey, Object... substitutionArgs) {
        if (overworld == null || teamData == null) {
            return;
        }

        LinkedHashSet<String> recipients = new LinkedHashSet<String>();
        String normalizedLeader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (normalizedLeader != null) {
            recipients.add(normalizedLeader);
        }
        List<String> members = teamData.getMembers(leaderIdentity, leaderIdentityCandidates);
        if (members != null && !members.isEmpty()) {
            recipients.addAll(members);
        }
        this.BiomeRunestone$broadcastEventToIdentities(overworld, recipients, excludedIdentities, translationKey, substitutionArgs);
    }

    private void BiomeRunestone$broadcastEventToIdentities(WorldServer overworld,
                                                           Set<String> recipientIdentities,
                                                           Set<String> excludedIdentities,
                                                           String translationKey, Object... substitutionArgs) {
        if (overworld == null || recipientIdentities == null || recipientIdentities.isEmpty()) {
            return;
        }

        RunegateTeamEventNoticeData noticeData = RunegateTeamEventNoticeData.get(overworld);
        for (String rawIdentity : recipientIdentities) {
            String identity = this.BiomeRunestone$normalizeIdentity(rawIdentity);
            if (identity == null) {
                continue;
            }
            if (excludedIdentities != null && excludedIdentities.contains(identity)) {
                continue;
            }

            ServerPlayer onlinePlayer = this.BiomeRunestone$getOnlinePlayerByIdentity(identity);
            if (onlinePlayer != null) {
                this.BiomeRunestone$sendKey(onlinePlayer, translationKey, substitutionArgs);
            } else {
                noticeData.addPendingNotice(identity);
            }
        }
    }

    private ServerPlayer BiomeRunestone$getOnlinePlayerByIdentity(String identity) {
        String normalizedIdentity = this.BiomeRunestone$normalizeIdentity(identity);
        if (normalizedIdentity == null) {
            return null;
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null || server.getConfigurationManager() == null) {
            return null;
        }

        String[] usernames = server.getConfigurationManager().getAllUsernames();
        if (usernames == null) {
            return null;
        }

        for (String username : usernames) {
            ServerPlayer player = server.getConfigurationManager().getPlayerForUsername(username);
            if (player == null) {
                continue;
            }

            String playerIdentity = this.BiomeRunestone$getPlayerIdentity(player);
            if (normalizedIdentity.equals(playerIdentity)) {
                return player;
            }
            Set<String> candidates = this.BiomeRunestone$getPlayerIdentityCandidates(player);
            if (candidates != null && candidates.contains(normalizedIdentity)) {
                return player;
            }
        }
        return null;
    }

    private void BiomeRunestone$setPendingConfirmation(String playerIdentity, String action) {
        String identity = this.BiomeRunestone$normalizeIdentity(playerIdentity);
        String normalizedAction = this.BiomeRunestone$normalizeIdentity(action);
        if (identity == null || normalizedAction == null) {
            return;
        }

        long expiresAt = System.currentTimeMillis() + CONFIRM_TIMEOUT_MS;
        synchronized (PENDING_CONFIRMATIONS) {
            PENDING_CONFIRMATIONS.put(identity, new PendingConfirmation(normalizedAction, expiresAt));
            while (PENDING_CONFIRMATIONS.size() > MAX_PENDING_CONFIRMATIONS) {
                String oldest = PENDING_CONFIRMATIONS.keySet().iterator().next();
                PENDING_CONFIRMATIONS.remove(oldest);
            }
        }
    }

    private boolean BiomeRunestone$consumePendingConfirmation(String playerIdentity, String action) {
        String identity = this.BiomeRunestone$normalizeIdentity(playerIdentity);
        String normalizedAction = this.BiomeRunestone$normalizeIdentity(action);
        if (identity == null || normalizedAction == null) {
            return false;
        }

        synchronized (PENDING_CONFIRMATIONS) {
            PendingConfirmation pending = PENDING_CONFIRMATIONS.get(identity);
            if (pending == null) {
                return false;
            }
            if (System.currentTimeMillis() > pending.expiresAt || !normalizedAction.equals(pending.action)) {
                PENDING_CONFIRMATIONS.remove(identity);
                return false;
            }
            PENDING_CONFIRMATIONS.remove(identity);
            return true;
        }
    }

    private void BiomeRunestone$clearPendingConfirmation(String playerIdentity) {
        String identity = this.BiomeRunestone$normalizeIdentity(playerIdentity);
        if (identity == null) {
            return;
        }
        synchronized (PENDING_CONFIRMATIONS) {
            PENDING_CONFIRMATIONS.remove(identity);
        }
    }

    private void BiomeRunestone$sweepPendingConfirmations() {
        long now = System.currentTimeMillis();
        synchronized (PENDING_CONFIRMATIONS) {
            java.util.Iterator<java.util.Map.Entry<String, PendingConfirmation>> iterator = PENDING_CONFIRMATIONS.entrySet().iterator();
            while (iterator.hasNext()) {
                PendingConfirmation pending = iterator.next().getValue();
                if (pending == null || now > pending.expiresAt) {
                    iterator.remove();
                }
            }
        }
    }

    private String BiomeRunestone$normalizeIdentity(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private boolean BiomeRunestone$isUuidString(String value) {
        if (value == null) {
            return false;
        }
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    private void BiomeRunestone$sendUsage(ICommandSender sender) {
        this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.usage", USAGE);
        this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.usage_hint_1");
        this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.usage_hint_2");
        this.BiomeRunestone$sendKey(sender, "message.biome_runestone.team.usage_hint_3");
    }

    private boolean BiomeRunestone$isIdentityMatch(String expectedIdentity, String canonicalIdentity, Set<String> identityCandidates) {
        if (expectedIdentity == null) {
            return false;
        }
        if (expectedIdentity.equals(canonicalIdentity)) {
            return true;
        }
        return identityCandidates != null && identityCandidates.contains(expectedIdentity);
    }

    private void BiomeRunestone$sendKey(ICommandSender sender, String translationKey, Object... substitutionArgs) {
        ChatMessageComponent body = ChatMessageComponent.createFromTranslationWithSubstitutions(translationKey, substitutionArgs);
        sender.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(MESSAGE_PREFIX_KEY, body));
    }
}
