package com.github.hahahha.BiomeRunestone.util;

import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.World;
import net.minecraft.WorldSavedData;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

public class RunegatePlayerLockData extends WorldSavedData {
    private static final String DATA_NAME = "biome_runestone_player_biome_runegate_locks";
    private static final String TAG_PLAYER_LOCKS = "PlayerLocks";
    private static final String TAG_USED_GROUPS = "UsedGroups";
    private static final String TAG_KEY = "K";
    private static final String TAG_X = "X";
    private static final String TAG_Y = "Y";
    private static final String TAG_Z = "Z";
    private static final String TAG_VALUES = "V";
    private static final String TAG_PACKED = "P";
    private static final String TAG_PLAYER_MIGRATION_CHECKED = "PlayerMigrationChecked";
    private static final String TAG_GROUP_MIGRATION_CHECKED = "GroupMigrationChecked";

    private final LinkedHashMap<String, int[]> playerLocks = new LinkedHashMap<String, int[]>();
    private final LinkedHashMap<String, LinkedHashSet<Long>> usedDestinationsByGroup = new LinkedHashMap<String, LinkedHashSet<Long>>();
    private final LinkedHashSet<String> checkedPlayerMigrationKeys = new LinkedHashSet<String>();
    private final LinkedHashSet<String> checkedGroupMigrationKeys = new LinkedHashSet<String>();

    public RunegatePlayerLockData() {
        this(DATA_NAME);
    }

    public RunegatePlayerLockData(String mapName) {
        super(mapName);
    }

    public static RunegatePlayerLockData get(World world) {
        RunegatePlayerLockData data = (RunegatePlayerLockData) world.loadItemData(RunegatePlayerLockData.class, DATA_NAME);
        if (data == null) {
            data = new RunegatePlayerLockData(DATA_NAME);
            world.setItemData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.playerLocks.clear();
        this.usedDestinationsByGroup.clear();
        this.checkedPlayerMigrationKeys.clear();
        this.checkedGroupMigrationKeys.clear();

        NBTTagList playerLockList = nbt.getTagList(TAG_PLAYER_LOCKS);
        for (int i = 0; i < playerLockList.tagCount(); ++i) {
            NBTTagCompound lockTag = (NBTTagCompound) playerLockList.tagAt(i);
            String key = lockTag.getString(TAG_KEY);
            if (key == null || key.isEmpty()) {
                continue;
            }

            int x = lockTag.getInteger(TAG_X);
            int y = lockTag.getInteger(TAG_Y);
            int z = lockTag.getInteger(TAG_Z);
            this.playerLocks.put(key, new int[]{x, y, z});
        }

        NBTTagList usedGroupList = nbt.getTagList(TAG_USED_GROUPS);
        for (int i = 0; i < usedGroupList.tagCount(); ++i) {
            NBTTagCompound groupTag = (NBTTagCompound) usedGroupList.tagAt(i);
            String key = groupTag.getString(TAG_KEY);
            if (key == null || key.isEmpty()) {
                continue;
            }

            NBTTagList values = groupTag.getTagList(TAG_VALUES);
            LinkedHashSet<Long> packedSet = new LinkedHashSet<Long>();
            for (int j = 0; j < values.tagCount(); ++j) {
                NBTTagCompound packedTag = (NBTTagCompound) values.tagAt(j);
                packedSet.add(Long.valueOf(packedTag.getLong(TAG_PACKED)));
            }
            this.usedDestinationsByGroup.put(key, packedSet);
        }

        this.readStringSet(nbt.getTagList(TAG_PLAYER_MIGRATION_CHECKED), this.checkedPlayerMigrationKeys);
        this.readStringSet(nbt.getTagList(TAG_GROUP_MIGRATION_CHECKED), this.checkedGroupMigrationKeys);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList playerLockList = new NBTTagList();
        for (Map.Entry<String, int[]> entry : this.playerLocks.entrySet()) {
            int[] coords = entry.getValue();
            if (coords == null || coords.length < 3) {
                continue;
            }

            NBTTagCompound lockTag = new NBTTagCompound();
            lockTag.setString(TAG_KEY, entry.getKey());
            lockTag.setInteger(TAG_X, coords[0]);
            lockTag.setInteger(TAG_Y, coords[1]);
            lockTag.setInteger(TAG_Z, coords[2]);
            playerLockList.appendTag(lockTag);
        }
        nbt.setTag(TAG_PLAYER_LOCKS, playerLockList);

        NBTTagList usedGroupList = new NBTTagList();
        for (Map.Entry<String, LinkedHashSet<Long>> entry : this.usedDestinationsByGroup.entrySet()) {
            LinkedHashSet<Long> packedSet = entry.getValue();
            if (packedSet == null || packedSet.isEmpty()) {
                continue;
            }

            NBTTagCompound groupTag = new NBTTagCompound();
            groupTag.setString(TAG_KEY, entry.getKey());

            NBTTagList values = new NBTTagList();
            for (Long packed : packedSet) {
                NBTTagCompound packedTag = new NBTTagCompound();
                packedTag.setLong(TAG_PACKED, packed.longValue());
                values.appendTag(packedTag);
            }

            groupTag.setTag(TAG_VALUES, values);
            usedGroupList.appendTag(groupTag);
        }
        nbt.setTag(TAG_USED_GROUPS, usedGroupList);
        nbt.setTag(TAG_PLAYER_MIGRATION_CHECKED, this.writeStringSet(this.checkedPlayerMigrationKeys));
        nbt.setTag(TAG_GROUP_MIGRATION_CHECKED, this.writeStringSet(this.checkedGroupMigrationKeys));
    }

    public int[] getLockedDestination(String playerLockKey) {
        int[] lockedDestination = this.playerLocks.get(playerLockKey);
        if (lockedDestination == null) {
            return null;
        }
        return new int[]{lockedDestination[0], lockedDestination[1], lockedDestination[2]};
    }

    public int[] getOrMigrateLockedDestination(String canonicalKey, Collection<String> legacyKeys) {
        int[] canonical = this.playerLocks.get(canonicalKey);
        if (canonical != null) {
            return new int[]{canonical[0], canonical[1], canonical[2]};
        }
        if (this.checkedPlayerMigrationKeys.contains(canonicalKey)) {
            return null;
        }

        int[] migrated = null;
        boolean changed = false;
        if (legacyKeys != null) {
            for (String legacyKey : legacyKeys) {
                if (legacyKey == null || legacyKey.isEmpty() || canonicalKey.equals(legacyKey)) {
                    continue;
                }

                int[] legacy = this.playerLocks.remove(legacyKey);
                if (legacy == null) {
                    continue;
                }

                if (migrated == null) {
                    migrated = legacy;
                }
                changed = true;
            }
        }

        if (migrated != null) {
            this.playerLocks.remove(canonicalKey);
            this.playerLocks.put(canonicalKey, migrated);
            changed = true;
        }

        if (this.checkedPlayerMigrationKeys.remove(canonicalKey)) {
            changed = true;
        }
        this.checkedPlayerMigrationKeys.add(canonicalKey);
        changed = true;

        if (changed) {
            this.markDirty();
        }

        if (migrated == null) {
            return null;
        }
        return new int[]{migrated[0], migrated[1], migrated[2]};
    }

    public Set<Long> getOrCreateUsedDestinations(String runestoneGroupKey) {
        LinkedHashSet<Long> usedDestinations = this.usedDestinationsByGroup.get(runestoneGroupKey);
        if (usedDestinations == null) {
            usedDestinations = new LinkedHashSet<Long>();
            this.usedDestinationsByGroup.put(runestoneGroupKey, usedDestinations);
        }
        return usedDestinations;
    }

    public Set<Long> getOrCreateUsedDestinationsWithMigration(String canonicalGroupKey, Collection<String> legacyGroupKeys) {
        LinkedHashSet<Long> target = this.usedDestinationsByGroup.get(canonicalGroupKey);
        boolean changed = false;
        if (target == null) {
            target = new LinkedHashSet<Long>();
            this.usedDestinationsByGroup.put(canonicalGroupKey, target);
            changed = true;
        }

        if (this.checkedGroupMigrationKeys.contains(canonicalGroupKey)) {
            return target;
        }

        if (legacyGroupKeys != null) {
            for (String legacyGroupKey : legacyGroupKeys) {
                if (legacyGroupKey == null || legacyGroupKey.isEmpty() || canonicalGroupKey.equals(legacyGroupKey)) {
                    continue;
                }

                LinkedHashSet<Long> legacy = this.usedDestinationsByGroup.remove(legacyGroupKey);
                if (legacy == null) {
                    continue;
                }

                if (!legacy.isEmpty()) {
                    if (target.addAll(legacy)) {
                        changed = true;
                    }
                } else {
                    changed = true;
                }
            }
        }

        if (this.checkedGroupMigrationKeys.remove(canonicalGroupKey)) {
            changed = true;
        }
        this.checkedGroupMigrationKeys.add(canonicalGroupKey);
        changed = true;

        if (changed) {
            this.markDirty();
        }
        return target;
    }

    public void putLockedDestination(String playerLockKey, int[] destination) {
        this.playerLocks.remove(playerLockKey);
        this.playerLocks.put(playerLockKey, new int[]{destination[0], destination[1], destination[2]});
        this.markDirty();
    }

    public void addUsedDestination(String runestoneGroupKey, long packedXZ) {
        LinkedHashSet<Long> usedDestinations = this.usedDestinationsByGroup.get(runestoneGroupKey);
        if (usedDestinations == null) {
            usedDestinations = new LinkedHashSet<Long>();
            this.usedDestinationsByGroup.put(runestoneGroupKey, usedDestinations);
        }

        if (usedDestinations.add(Long.valueOf(packedXZ))) {
            this.markDirty();
        }
    }

    public void prune(int maxPlayerLocks, int maxGroupCount, int maxUsedPerGroup) {
        boolean changed = false;
        changed |= trimMap(this.playerLocks, Math.max(1, maxPlayerLocks));
        changed |= trimMap(this.usedDestinationsByGroup, Math.max(1, maxGroupCount));
        changed |= trimStringSet(this.checkedPlayerMigrationKeys, Math.max(8, maxPlayerLocks * 2));
        changed |= trimStringSet(this.checkedGroupMigrationKeys, Math.max(8, maxGroupCount * 2));

        Iterator<Map.Entry<String, LinkedHashSet<Long>>> iterator = this.usedDestinationsByGroup.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LinkedHashSet<Long>> entry = iterator.next();
            LinkedHashSet<Long> values = entry.getValue();
            if (values == null) {
                iterator.remove();
                changed = true;
                continue;
            }

            if (trimLongSet(values, Math.max(1, maxUsedPerGroup))) {
                changed = true;
            }
            if (values.isEmpty()) {
                iterator.remove();
                changed = true;
            }
        }

        if (changed) {
            this.markDirty();
        }
    }

    public int getPlayerLockCount() {
        return this.playerLocks.size();
    }

    public int getUsedGroupCount() {
        return this.usedDestinationsByGroup.size();
    }

    public int getTotalUsedDestinationCount() {
        int total = 0;
        for (LinkedHashSet<Long> values : this.usedDestinationsByGroup.values()) {
            if (values != null) {
                total += values.size();
            }
        }
        return total;
    }

    public int clearPlayerLocksByIdentities(Collection<String> identities) {
        if (identities == null || identities.isEmpty()) {
            return 0;
        }

        HashSet<String> normalized = new HashSet<String>();
        for (String identity : identities) {
            if (identity != null && !identity.isEmpty()) {
                normalized.add(identity.toLowerCase(Locale.ROOT));
            }
        }
        if (normalized.isEmpty()) {
            return 0;
        }

        int removed = 0;
        Iterator<Map.Entry<String, int[]>> iterator = this.playerLocks.entrySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            if (key == null) {
                continue;
            }
            String keyLower = key.toLowerCase(Locale.ROOT);

            for (String identity : normalized) {
                if (keyLower.endsWith(":" + identity)) {
                    iterator.remove();
                    ++removed;
                    break;
                }
            }
        }

        if (removed > 0) {
            this.markDirty();
        }
        return removed;
    }

    public int transferPlayerLockIdentity(String newCanonicalIdentity, Collection<String> oldIdentityCandidates) {
        String newIdentity = normalizeIdentityToken(newCanonicalIdentity);
        if (newIdentity == null) {
            return 0;
        }
        if (oldIdentityCandidates == null || oldIdentityCandidates.isEmpty()) {
            return 0;
        }

        HashSet<String> oldNormalized = new HashSet<String>();
        for (String identity : oldIdentityCandidates) {
            String normalized = normalizeIdentityToken(identity);
            if (normalized != null && !normalized.equals(newIdentity)) {
                oldNormalized.add(normalized);
            }
        }
        if (oldNormalized.isEmpty()) {
            return 0;
        }

        boolean changed = false;
        int moved = 0;
        ArrayList<String> keys = new ArrayList<String>(this.playerLocks.keySet());
        for (String key : keys) {
            if (key == null) {
                continue;
            }

            String keyLower = key.toLowerCase(Locale.ROOT);
            String matchedOld = null;
            for (String oldIdentity : oldNormalized) {
                if (keyLower.endsWith(":" + oldIdentity)) {
                    matchedOld = oldIdentity;
                    break;
                }
            }
            if (matchedOld == null) {
                continue;
            }

            int suffixStart = key.length() - matchedOld.length();
            if (suffixStart <= 0 || suffixStart > key.length()) {
                continue;
            }

            String prefix = key.substring(0, suffixStart);
            String newKey = prefix + newIdentity;
            if (newKey.equals(key)) {
                continue;
            }

            int[] coordinates = this.playerLocks.get(key);
            if (coordinates == null || coordinates.length < 3) {
                continue;
            }

            this.playerLocks.remove(key);
            this.playerLocks.remove(newKey);
            this.playerLocks.put(newKey, new int[]{coordinates[0], coordinates[1], coordinates[2]});
            changed = true;
            ++moved;
        }

        if (changed) {
            this.markDirty();
        }
        return moved;
    }

    public int clearAllPlayerLocks() {
        int removed = this.playerLocks.size();
        if (removed > 0 || !this.checkedPlayerMigrationKeys.isEmpty()) {
            this.playerLocks.clear();
            this.checkedPlayerMigrationKeys.clear();
            this.markDirty();
        }
        return removed;
    }

    public int clearAllUsedDestinations() {
        int removed = this.getTotalUsedDestinationCount();
        if (removed > 0 || !this.usedDestinationsByGroup.isEmpty() || !this.checkedGroupMigrationKeys.isEmpty()) {
            this.usedDestinationsByGroup.clear();
            this.checkedGroupMigrationKeys.clear();
            this.markDirty();
        }
        return removed;
    }

    public void clearAll() {
        if (!this.playerLocks.isEmpty() || !this.usedDestinationsByGroup.isEmpty()
                || !this.checkedPlayerMigrationKeys.isEmpty() || !this.checkedGroupMigrationKeys.isEmpty()) {
            this.playerLocks.clear();
            this.usedDestinationsByGroup.clear();
            this.checkedPlayerMigrationKeys.clear();
            this.checkedGroupMigrationKeys.clear();
            this.markDirty();
        }
    }

    private static <K, V> boolean trimMap(LinkedHashMap<K, V> map, int limit) {
        boolean changed = false;
        while (map.size() > limit) {
            Iterator<K> iterator = map.keySet().iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
            changed = true;
        }
        return changed;
    }

    private static boolean trimLongSet(LinkedHashSet<Long> set, int limit) {
        boolean changed = false;
        while (set.size() > limit) {
            Iterator<Long> iterator = set.iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
            changed = true;
        }
        return changed;
    }

    private static boolean trimStringSet(LinkedHashSet<String> set, int limit) {
        boolean changed = false;
        while (set.size() > limit) {
            Iterator<String> iterator = set.iterator();
            if (!iterator.hasNext()) {
                break;
            }
            iterator.next();
            iterator.remove();
            changed = true;
        }
        return changed;
    }

    private void readStringSet(NBTTagList listTag, LinkedHashSet<String> targetSet) {
        for (int i = 0; i < listTag.tagCount(); ++i) {
            NBTTagCompound keyTag = (NBTTagCompound) listTag.tagAt(i);
            String key = keyTag.getString(TAG_KEY);
            if (key != null && !key.isEmpty()) {
                targetSet.add(key);
            }
        }
    }

    private NBTTagList writeStringSet(LinkedHashSet<String> sourceSet) {
        NBTTagList list = new NBTTagList();
        for (String key : sourceSet) {
            if (key == null || key.isEmpty()) {
                continue;
            }
            NBTTagCompound keyTag = new NBTTagCompound();
            keyTag.setString(TAG_KEY, key);
            list.appendTag(keyTag);
        }
        return list;
    }

    private static String normalizeIdentityToken(String identity) {
        if (identity == null) {
            return null;
        }
        String trimmed = identity.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}

