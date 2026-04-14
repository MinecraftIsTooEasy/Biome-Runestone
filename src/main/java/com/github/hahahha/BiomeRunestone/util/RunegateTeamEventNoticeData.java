package com.github.hahahha.BiomeRunestone.util;

import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.World;
import net.minecraft.WorldSavedData;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class RunegateTeamEventNoticeData extends WorldSavedData {
    private static final String DATA_NAME = "biome_runestone_team_event_notices";
    private static final String TAG_NOTICES = "Notices";
    private static final String TAG_TARGET = "T";
    private static final String TAG_COUNT = "C";
    private static final int MAX_PENDING_COUNT_PER_PLAYER = 99;

    private final LinkedHashMap<String, Integer> pendingNoticeCountByIdentity = new LinkedHashMap<String, Integer>();

    public RunegateTeamEventNoticeData() {
        this(DATA_NAME);
    }

    public RunegateTeamEventNoticeData(String mapName) {
        super(mapName);
    }

    public static RunegateTeamEventNoticeData get(World world) {
        RunegateTeamEventNoticeData data = (RunegateTeamEventNoticeData) world.loadItemData(RunegateTeamEventNoticeData.class, DATA_NAME);
        if (data == null) {
            data = new RunegateTeamEventNoticeData(DATA_NAME);
            world.setItemData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.pendingNoticeCountByIdentity.clear();
        NBTTagList notices = nbt.getTagList(TAG_NOTICES);
        for (int i = 0; i < notices.tagCount(); ++i) {
            NBTTagCompound notice = (NBTTagCompound) notices.tagAt(i);
            String target = this.BiomeRunestone$normalizeIdentity(notice.getString(TAG_TARGET));
            if (target == null) {
                continue;
            }

            int count = notice.getInteger(TAG_COUNT);
            if (count <= 0) {
                continue;
            }
            if (count > MAX_PENDING_COUNT_PER_PLAYER) {
                count = MAX_PENDING_COUNT_PER_PLAYER;
            }
            this.pendingNoticeCountByIdentity.put(target, Integer.valueOf(count));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList notices = new NBTTagList();
        for (Map.Entry<String, Integer> entry : this.pendingNoticeCountByIdentity.entrySet()) {
            String target = this.BiomeRunestone$normalizeIdentity(entry.getKey());
            Integer countObj = entry.getValue();
            if (target == null || countObj == null) {
                continue;
            }
            int count = countObj.intValue();
            if (count <= 0) {
                continue;
            }
            if (count > MAX_PENDING_COUNT_PER_PLAYER) {
                count = MAX_PENDING_COUNT_PER_PLAYER;
            }
            NBTTagCompound notice = new NBTTagCompound();
            notice.setString(TAG_TARGET, target);
            notice.setInteger(TAG_COUNT, count);
            notices.appendTag(notice);
        }
        nbt.setTag(TAG_NOTICES, notices);
    }

    public void addPendingNotice(String targetIdentity) {
        String target = this.BiomeRunestone$normalizeIdentity(targetIdentity);
        if (target == null) {
            return;
        }

        Integer current = this.pendingNoticeCountByIdentity.get(target);
        int nextCount = current == null ? 1 : current.intValue() + 1;
        if (nextCount > MAX_PENDING_COUNT_PER_PLAYER) {
            nextCount = MAX_PENDING_COUNT_PER_PLAYER;
        }
        if (current == null || current.intValue() != nextCount) {
            this.pendingNoticeCountByIdentity.put(target, Integer.valueOf(nextCount));
            this.markDirty();
        }
    }

    public void addPendingNotices(Collection<String> targetIdentities) {
        if (targetIdentities == null || targetIdentities.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (String identity : targetIdentities) {
            String target = this.BiomeRunestone$normalizeIdentity(identity);
            if (target == null) {
                continue;
            }
            Integer current = this.pendingNoticeCountByIdentity.get(target);
            int nextCount = current == null ? 1 : current.intValue() + 1;
            if (nextCount > MAX_PENDING_COUNT_PER_PLAYER) {
                nextCount = MAX_PENDING_COUNT_PER_PLAYER;
            }
            if (current == null || current.intValue() != nextCount) {
                this.pendingNoticeCountByIdentity.put(target, Integer.valueOf(nextCount));
                changed = true;
            }
        }
        if (changed) {
            this.markDirty();
        }
    }

    public int consumePendingNoticeCount(String canonicalIdentity, Collection<String> identityCandidates) {
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalIdentity);
        if (canonical == null) {
            return 0;
        }

        String key = this.BiomeRunestone$resolveExistingKey(this.pendingNoticeCountByIdentity, canonical, identityCandidates, true);
        if (key == null) {
            return 0;
        }

        Integer count = this.pendingNoticeCountByIdentity.remove(key);
        this.markDirty();
        return count == null ? 0 : Math.max(0, count.intValue());
    }

    private String BiomeRunestone$resolveExistingKey(LinkedHashMap<String, Integer> map, String canonical, Collection<String> fallbackCandidates, boolean migrateToCanonical) {
        if (canonical != null && map.containsKey(canonical)) {
            return canonical;
        }

        if (fallbackCandidates == null) {
            return null;
        }

        for (String rawCandidate : fallbackCandidates) {
            String candidate = this.BiomeRunestone$normalizeIdentity(rawCandidate);
            if (candidate == null || candidate.equals(canonical) || !map.containsKey(candidate)) {
                continue;
            }

            if (!migrateToCanonical || canonical == null) {
                return candidate;
            }

            Integer value = map.remove(candidate);
            map.put(canonical, value);
            this.markDirty();
            return canonical;
        }

        return null;
    }

    public void clearInvalid() {
        boolean changed = false;
        Iterator<Map.Entry<String, Integer>> iterator = this.pendingNoticeCountByIdentity.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            String key = this.BiomeRunestone$normalizeIdentity(entry.getKey());
            Integer count = entry.getValue();
            if (key == null || count == null || count.intValue() <= 0) {
                iterator.remove();
                changed = true;
            }
        }
        if (changed) {
            this.markDirty();
        }
    }

    private String BiomeRunestone$normalizeIdentity(String identity) {
        if (identity == null) {
            return null;
        }
        String trimmed = identity.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }
}
