package com.github.hahahha.BiomeRunestone.mixins;

import com.github.hahahha.BiomeRunestone.util.RunegatePlayerIdentityIndexData;
import com.github.hahahha.BiomeRunestone.util.RunegateTeamEventNoticeData;
import com.github.hahahha.BiomeRunestone.util.RunegateTeamData;
import net.minecraft.ChatMessageComponent;
import net.minecraft.INetworkManager;
import net.minecraft.ServerConfigurationManager;
import net.minecraft.ServerPlayer;
import net.minecraft.WorldServer;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Mixin(ServerConfigurationManager.class)
public class ServerConfigurationManagerBiomeRunestoneMixin {
    private static final String MESSAGE_PREFIX_KEY = "message.biome_runestone.team.prefix";

    @Inject(method = "initializeConnectionToPlayer(Lnet/minecraft/INetworkManager;Lnet/minecraft/ServerPlayer;)V", at = @At("TAIL"))
    private void BiomeRunestone$activatePendingOfflineUuidInvitesOnLogin(INetworkManager networkManager, ServerPlayer player, CallbackInfo ci) {
        if (player == null) {
            return;
        }

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) {
            return;
        }

        WorldServer overworld = server.worldServerForDimension(0);
        if (overworld == null) {
            return;
        }

        String playerIdentity = this.BiomeRunestone$getPlayerIdentity(player);
        Set<String> identityCandidates = this.BiomeRunestone$getPlayerIdentityCandidates(player);

        RunegatePlayerIdentityIndexData.get(overworld).upsertIdentity(player.getCommandSenderName(), playerIdentity);

        RunegateTeamData teamData = RunegateTeamData.get(overworld);
        if (!teamData.activateInviteOnOnline(playerIdentity, identityCandidates)) {
            int offlineEventCount = RunegateTeamEventNoticeData.get(overworld).consumePendingNoticeCount(playerIdentity, identityCandidates);
            if (offlineEventCount > 0) {
                this.BiomeRunestone$sendKey(player, "message.biome_runestone.team.offline_team_event_notify", offlineEventCount);
            }
            return;
        }

        String inviteLeader = teamData.getInviteLeader(playerIdentity, identityCandidates);
        if (inviteLeader != null) {
            this.BiomeRunestone$sendKey(
                    player,
                    "message.biome_runestone.team.invite_notify",
                    this.BiomeRunestone$formatIdentity(inviteLeader),
                    RunegateTeamData.getInviteExpireSeconds()
            );
        }

        int offlineEventCount = RunegateTeamEventNoticeData.get(overworld).consumePendingNoticeCount(playerIdentity, identityCandidates);
        if (offlineEventCount > 0) {
            this.BiomeRunestone$sendKey(player, "message.biome_runestone.team.offline_team_event_notify", offlineEventCount);
        }
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

    private void BiomeRunestone$sendKey(ServerPlayer player, String translationKey, Object... substitutionArgs) {
        ChatMessageComponent body = ChatMessageComponent.createFromTranslationWithSubstitutions(translationKey, substitutionArgs);
        player.sendChatToPlayer(ChatMessageComponent.createFromTranslationWithSubstitutions(MESSAGE_PREFIX_KEY, body));
    }
}
