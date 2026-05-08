package com.github.hahahha.BiomeRunestone.util;

import net.minecraft.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;

public final class RunegateExternalTeamCompat {
    private static final String TEAM_API_PROVIDER_CLASS_NAME = "com.github.hahahha.team.api.TeamApiProvider";
    private static final String TEAM_API_INTERFACE_CLASS_NAME = "com.github.hahahha.team.api.TeamApiV1";
    private static final String TEAM_SERVICE_CLASS_NAME = "com.github.hahahha.team.system.TeamService";

    private static volatile boolean initialized = false;
    private static volatile boolean apiAvailable = false;
    private static volatile boolean fallbackAvailable = false;

    private static Class<?> teamApiProviderClass;
    private static Method teamApiGetMethod;
    private static Method teamApiGetLeaderMethod;
    private static Class<?> teamServiceClass;
    private static Field playerToTeamField;
    private static Field teamsField;
    private static Field teamLeaderField;

    private RunegateExternalTeamCompat() {
    }

    public static String getLeaderIdentity(ServerPlayer player) {
        if (player == null) {
            return null;
        }

        initializeIfNeeded();

        if (apiAvailable && teamApiProviderClass != null && teamApiGetMethod != null && teamApiGetLeaderMethod != null) {
            try {
                Object api = teamApiGetMethod.invoke(null);
                if (api != null) {
                    Object leader = teamApiGetLeaderMethod.invoke(api, player);
                    String normalizedLeader = normalizeNameKey(leader instanceof String ? (String) leader : null);
                    if (normalizedLeader != null) {
                        return normalizedLeader;
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        if (fallbackAvailable && teamServiceClass != null && playerToTeamField != null && teamsField != null && teamLeaderField != null) {
            String playerNameKey = normalizeNameKey(player.getCommandSenderName());
            if (playerNameKey == null) {
                return null;
            }

            try {
                synchronized (teamServiceClass) {
                    Object playerToTeamObj = playerToTeamField.get(null);
                    Object teamsObj = teamsField.get(null);
                    if (!(playerToTeamObj instanceof Map) || !(teamsObj instanceof Map)) {
                        return null;
                    }

                    Map<?, ?> playerToTeam = (Map<?, ?>) playerToTeamObj;
                    Map<?, ?> teams = (Map<?, ?>) teamsObj;

                    Object teamIdObj = playerToTeam.get(playerNameKey);
                    if (!(teamIdObj instanceof String)) {
                        return null;
                    }

                    Object teamData = teams.get(teamIdObj);
                    if (teamData == null) {
                        return null;
                    }

                    Object leaderObj = teamLeaderField.get(teamData);
                    if (!(leaderObj instanceof String)) {
                        return null;
                    }
                    return normalizeNameKey((String) leaderObj);
                }
            } catch (Throwable ignored) {
                return null;
            }
        }
        return null;
    }

    private static void initializeIfNeeded() {
        if (initialized) {
            return;
        }
        synchronized (RunegateExternalTeamCompat.class) {
            if (initialized) {
                return;
            }
            initialized = true;

            try {
                Class<?> providerClass = Class.forName(TEAM_API_PROVIDER_CLASS_NAME);
                Method getMethod = providerClass.getMethod("get");
                Class<?> apiClass = Class.forName(TEAM_API_INTERFACE_CLASS_NAME);
                Method getLeaderMethod = apiClass.getMethod("getLeaderIdentityKey", ServerPlayer.class);
                teamApiProviderClass = providerClass;
                teamApiGetMethod = getMethod;
                teamApiGetLeaderMethod = getLeaderMethod;
                apiAvailable = true;
            } catch (Throwable ignored) {
                apiAvailable = false;
            }

            try {
                Class<?> serviceClass = Class.forName(TEAM_SERVICE_CLASS_NAME);
                Field playerToTeam = serviceClass.getDeclaredField("PLAYER_TO_TEAM");
                Field teams = serviceClass.getDeclaredField("TEAMS");
                playerToTeam.setAccessible(true);
                teams.setAccessible(true);

                Class<?> teamDataClass = Class.forName(serviceClass.getName() + "$TeamData");
                Field leader = teamDataClass.getDeclaredField("leader");
                leader.setAccessible(true);

                teamServiceClass = serviceClass;
                playerToTeamField = playerToTeam;
                teamsField = teams;
                teamLeaderField = leader;
                fallbackAvailable = true;
            } catch (Throwable ignored) {
                fallbackAvailable = false;
            }
        }
    }

    private static String normalizeNameKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
