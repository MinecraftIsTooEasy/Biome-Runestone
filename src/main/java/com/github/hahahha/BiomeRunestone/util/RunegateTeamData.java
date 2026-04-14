package com.github.hahahha.BiomeRunestone.util;

import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.World;
import net.minecraft.WorldSavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RunegateTeamData extends WorldSavedData {
    private static final String DATA_NAME = "biome_runestone_runegate_teams";
    private static final String TAG_MEMBERS = "Members";
    private static final String TAG_INVITES = "Invites";
    private static final String TAG_MEMBER = "M";
    private static final String TAG_LEADER = "L";
    private static final String TAG_TARGET = "T";
    private static final String TAG_INVITE_EXPIRES_AT = "E";
    private static final long INVITE_EXPIRE_MILLIS = 300000L;
    private static final long INVITE_PENDING_ONLINE_ACTIVATION = -1L;

    private final LinkedHashMap<String, String> memberToLeader = new LinkedHashMap<String, String>();
    private final LinkedHashMap<String, String> inviteTargetToLeader = new LinkedHashMap<String, String>();
    private final LinkedHashMap<String, Long> inviteTargetToExpiresAt = new LinkedHashMap<String, Long>();

    public RunegateTeamData() {
        this(DATA_NAME);
    }

    public RunegateTeamData(String mapName) {
        super(mapName);
    }

    public static RunegateTeamData get(World world) {
        RunegateTeamData data = (RunegateTeamData) world.loadItemData(RunegateTeamData.class, DATA_NAME);
        if (data == null) {
            data = new RunegateTeamData(DATA_NAME);
            world.setItemData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.memberToLeader.clear();
        this.inviteTargetToLeader.clear();
        this.inviteTargetToExpiresAt.clear();

        NBTTagList members = nbt.getTagList(TAG_MEMBERS);
        for (int i = 0; i < members.tagCount(); ++i) {
            NBTTagCompound memberTag = (NBTTagCompound) members.tagAt(i);
            String member = this.BiomeRunestone$normalizeIdentity(memberTag.getString(TAG_MEMBER));
            String leader = this.BiomeRunestone$normalizeIdentity(memberTag.getString(TAG_LEADER));
            if (member == null || leader == null) {
                continue;
            }
            this.memberToLeader.put(member, leader);
        }

        NBTTagList invites = nbt.getTagList(TAG_INVITES);
        long now = System.currentTimeMillis();
        for (int i = 0; i < invites.tagCount(); ++i) {
            NBTTagCompound inviteTag = (NBTTagCompound) invites.tagAt(i);
            String target = this.BiomeRunestone$normalizeIdentity(inviteTag.getString(TAG_TARGET));
            String leader = this.BiomeRunestone$normalizeIdentity(inviteTag.getString(TAG_LEADER));
            if (target == null || leader == null) {
                continue;
            }
            long expiresAt = inviteTag.getLong(TAG_INVITE_EXPIRES_AT);
            if (expiresAt == 0L) {
                expiresAt = now + INVITE_EXPIRE_MILLIS;
            }
            if (expiresAt > 0L && expiresAt <= now) {
                continue;
            }
            this.inviteTargetToLeader.put(target, leader);
            this.inviteTargetToExpiresAt.put(target, Long.valueOf(expiresAt));
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList members = new NBTTagList();
        for (Map.Entry<String, String> entry : this.memberToLeader.entrySet()) {
            String member = this.BiomeRunestone$normalizeIdentity(entry.getKey());
            String leader = this.BiomeRunestone$normalizeIdentity(entry.getValue());
            if (member == null || leader == null) {
                continue;
            }
            NBTTagCompound memberTag = new NBTTagCompound();
            memberTag.setString(TAG_MEMBER, member);
            memberTag.setString(TAG_LEADER, leader);
            members.appendTag(memberTag);
        }
        nbt.setTag(TAG_MEMBERS, members);

        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        NBTTagList invites = new NBTTagList();
        for (Map.Entry<String, String> entry : this.inviteTargetToLeader.entrySet()) {
            String target = this.BiomeRunestone$normalizeIdentity(entry.getKey());
            String leader = this.BiomeRunestone$normalizeIdentity(entry.getValue());
            Long expiresAt = this.inviteTargetToExpiresAt.get(entry.getKey());
            if (target == null || leader == null || expiresAt == null) {
                continue;
            }
            NBTTagCompound inviteTag = new NBTTagCompound();
            inviteTag.setString(TAG_TARGET, target);
            inviteTag.setString(TAG_LEADER, leader);
            inviteTag.setLong(TAG_INVITE_EXPIRES_AT, expiresAt.longValue());
            invites.appendTag(inviteTag);
        }
        nbt.setTag(TAG_INVITES, invites);
    }

    public String getTeamLeader(String canonicalMemberIdentity, Collection<String> memberIdentityCandidates) {
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalMemberIdentity);
        if (canonical == null) {
            return null;
        }

        String memberKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, memberIdentityCandidates, true);
        if (memberKey == null) {
            return null;
        }

        String leader = this.BiomeRunestone$normalizeIdentity(this.memberToLeader.get(memberKey));
        if (leader == null) {
            this.memberToLeader.remove(memberKey);
            this.markDirty();
            return null;
        }

        if (!this.BiomeRunestone$isActiveLeaderInternal(leader)) {
            this.memberToLeader.remove(memberKey);
            this.markDirty();
            return null;
        }
        return leader;
    }

    public boolean isLeader(String canonicalMemberIdentity, Collection<String> memberIdentityCandidates) {
        String leader = this.getTeamLeader(canonicalMemberIdentity, memberIdentityCandidates);
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalMemberIdentity);
        return canonical != null && canonical.equals(leader);
    }

    public boolean isTeamActive(String leaderIdentity) {
        String leader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (leader == null) {
            return false;
        }
        return this.BiomeRunestone$isActiveLeaderInternal(leader);
    }

    public boolean createTeam(String leaderIdentity) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String leader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (leader == null) {
            return false;
        }

        if (this.getTeamLeader(leader, null) != null) {
            return false;
        }

        boolean changed = false;
        if (this.inviteTargetToLeader.remove(leader) != null) {
            changed = true;
        }
        if (this.inviteTargetToExpiresAt.remove(leader) != null) {
            changed = true;
        }
        this.memberToLeader.remove(leader);
        this.memberToLeader.put(leader, leader);
        changed = true;

        if (changed) {
            this.markDirty();
        }
        return true;
    }

    public boolean setInvite(String targetIdentity, String leaderIdentity) {
        return this.setInvite(targetIdentity, leaderIdentity, true);
    }

    public boolean setInvite(String targetIdentity, String leaderIdentity, boolean activateNow) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String target = this.BiomeRunestone$normalizeIdentity(targetIdentity);
        String leader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (target == null || leader == null) {
            return false;
        }

        if (!this.BiomeRunestone$isActiveLeaderInternal(leader)) {
            return false;
        }

        long expiresAt = activateNow ? (System.currentTimeMillis() + INVITE_EXPIRE_MILLIS) : INVITE_PENDING_ONLINE_ACTIVATION;
        String previous = this.inviteTargetToLeader.put(target, leader);
        Long previousExpiresAt = this.inviteTargetToExpiresAt.put(target, Long.valueOf(expiresAt));
        if (previous == null || !previous.equals(leader) || previousExpiresAt == null || previousExpiresAt.longValue() != expiresAt) {
            this.markDirty();
        }
        return true;
    }

    public String getInviteLeader(String canonicalTargetIdentity, Collection<String> targetIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalTargetIdentity);
        if (canonical == null) {
            return null;
        }

        String targetKey = this.BiomeRunestone$resolveExistingKey(this.inviteTargetToLeader, canonical, targetIdentityCandidates, true);
        if (targetKey == null) {
            return null;
        }

        String leader = this.BiomeRunestone$normalizeIdentity(this.inviteTargetToLeader.get(targetKey));
        Long expiresAt = this.inviteTargetToExpiresAt.get(targetKey);
        if (leader == null || expiresAt == null || !this.BiomeRunestone$isActiveLeaderInternal(leader)) {
            this.inviteTargetToLeader.remove(targetKey);
            this.inviteTargetToExpiresAt.remove(targetKey);
            this.markDirty();
            return null;
        }
        if (expiresAt.longValue() == INVITE_PENDING_ONLINE_ACTIVATION) {
            long activatedExpiresAt = System.currentTimeMillis() + INVITE_EXPIRE_MILLIS;
            this.inviteTargetToExpiresAt.put(targetKey, Long.valueOf(activatedExpiresAt));
            this.markDirty();
            return leader;
        }
        if (System.currentTimeMillis() >= expiresAt.longValue()) {
            this.inviteTargetToLeader.remove(targetKey);
            this.inviteTargetToExpiresAt.remove(targetKey);
            this.markDirty();
            return null;
        }
        return leader;
    }

    public boolean clearInvite(String canonicalTargetIdentity, Collection<String> targetIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalTargetIdentity);
        if (canonical == null) {
            return false;
        }

        String targetKey = this.BiomeRunestone$resolveExistingKey(this.inviteTargetToLeader, canonical, targetIdentityCandidates, true);
        if (targetKey == null) {
            return false;
        }

        this.inviteTargetToLeader.remove(targetKey);
        this.inviteTargetToExpiresAt.remove(targetKey);
        this.markDirty();
        return true;
    }

    public boolean joinTeam(String memberIdentity, String leaderIdentity) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String member = this.BiomeRunestone$normalizeIdentity(memberIdentity);
        String leader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (member == null || leader == null) {
            return false;
        }
        if (!this.BiomeRunestone$isActiveLeaderInternal(leader)) {
            return false;
        }

        boolean changed = false;
        if (this.inviteTargetToLeader.remove(member) != null) {
            changed = true;
        }
        if (this.inviteTargetToExpiresAt.remove(member) != null) {
            changed = true;
        }

        String previous = this.memberToLeader.remove(member);
        this.memberToLeader.put(member, leader);
        if (previous == null || !previous.equals(leader)) {
            changed = true;
        }

        if (changed) {
            this.markDirty();
        }
        return true;
    }

    public boolean leaveTeam(String canonicalMemberIdentity, Collection<String> memberIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalMemberIdentity);
        if (canonical == null) {
            return false;
        }

        String memberKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, memberIdentityCandidates, true);
        if (memberKey == null) {
            return false;
        }

        String leader = this.memberToLeader.get(memberKey);
        if (leader == null || memberKey.equals(leader)) {
            return false;
        }

        this.memberToLeader.remove(memberKey);
        this.inviteTargetToLeader.remove(memberKey);
        this.inviteTargetToExpiresAt.remove(memberKey);
        this.markDirty();
        return true;
    }

    public int disbandTeam(String canonicalLeaderIdentity, Collection<String> leaderIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalLeaderIdentity);
        if (canonical == null) {
            return 0;
        }

        String leaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, leaderIdentityCandidates, true);
        if (leaderKey == null) {
            return 0;
        }

        String leader = this.memberToLeader.get(leaderKey);
        if (!leaderKey.equals(leader)) {
            return 0;
        }

        boolean changed = false;
        int removedMembers = 0;

        Iterator<Map.Entry<String, String>> teamIterator = this.memberToLeader.entrySet().iterator();
        while (teamIterator.hasNext()) {
            Map.Entry<String, String> entry = teamIterator.next();
            if (!leaderKey.equals(entry.getValue())) {
                continue;
            }
            if (!leaderKey.equals(entry.getKey())) {
                ++removedMembers;
            }
            teamIterator.remove();
            changed = true;
        }

        Iterator<Map.Entry<String, String>> inviteIterator = this.inviteTargetToLeader.entrySet().iterator();
        while (inviteIterator.hasNext()) {
            Map.Entry<String, String> entry = inviteIterator.next();
            if (!leaderKey.equals(entry.getKey()) && !leaderKey.equals(entry.getValue())) {
                continue;
            }
            this.inviteTargetToExpiresAt.remove(entry.getKey());
            inviteIterator.remove();
            changed = true;
        }

        if (changed) {
            this.markDirty();
        }
        return removedMembers;
    }

    public List<String> getMembers(String canonicalLeaderIdentity, Collection<String> leaderIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalLeaderIdentity);
        if (canonical == null) {
            return new ArrayList<String>();
        }

        String leaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, leaderIdentityCandidates, true);
        if (leaderKey == null || !this.BiomeRunestone$isActiveLeaderInternal(leaderKey)) {
            return new ArrayList<String>();
        }

        ArrayList<String> members = new ArrayList<String>();
        for (Map.Entry<String, String> entry : this.memberToLeader.entrySet()) {
            if (leaderKey.equals(entry.getValue()) && !leaderKey.equals(entry.getKey())) {
                members.add(entry.getKey());
            }
        }
        return members;
    }

    public int getTeamSize(String canonicalLeaderIdentity, Collection<String> leaderIdentityCandidates) {
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalLeaderIdentity);
        if (canonical == null) {
            return 0;
        }
        String leaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, leaderIdentityCandidates, true);
        if (leaderKey == null || !this.BiomeRunestone$isActiveLeaderInternal(leaderKey)) {
            return 0;
        }

        int size = 0;
        for (Map.Entry<String, String> entry : this.memberToLeader.entrySet()) {
            if (leaderKey.equals(entry.getValue())) {
                ++size;
            }
        }
        return size;
    }

    public List<String> getPendingInviteTargets(String canonicalLeaderIdentity, Collection<String> leaderIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalLeaderIdentity);
        if (canonical == null) {
            return new ArrayList<String>();
        }

        String leaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, leaderIdentityCandidates, true);
        if (leaderKey == null || !this.BiomeRunestone$isActiveLeaderInternal(leaderKey)) {
            return new ArrayList<String>();
        }

        ArrayList<String> targets = new ArrayList<String>();
        for (Map.Entry<String, String> entry : this.inviteTargetToLeader.entrySet()) {
            String target = this.BiomeRunestone$normalizeIdentity(entry.getKey());
            String inviteLeader = this.BiomeRunestone$normalizeIdentity(entry.getValue());
            if (target == null || inviteLeader == null || !leaderKey.equals(inviteLeader)) {
                continue;
            }
            if (!this.inviteTargetToExpiresAt.containsKey(entry.getKey())) {
                continue;
            }
            targets.add(target);
        }
        return targets;
    }

    public int getPendingInviteCountForLeader(String canonicalLeaderIdentity, Collection<String> leaderIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalLeaderIdentity);
        if (canonical == null) {
            return 0;
        }

        String leaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, leaderIdentityCandidates, true);
        if (leaderKey == null || !this.BiomeRunestone$isActiveLeaderInternal(leaderKey)) {
            return 0;
        }

        int count = 0;
        for (Map.Entry<String, String> entry : this.inviteTargetToLeader.entrySet()) {
            String inviteLeader = this.BiomeRunestone$normalizeIdentity(entry.getValue());
            if (inviteLeader == null || !leaderKey.equals(inviteLeader)) {
                continue;
            }
            if (!this.inviteTargetToExpiresAt.containsKey(entry.getKey())) {
                continue;
            }
            ++count;
        }
        return count;
    }

    public int getPendingOnlineInviteCountForLeader(String canonicalLeaderIdentity, Collection<String> leaderIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalLeaderIdentity);
        if (canonical == null) {
            return 0;
        }

        String leaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, canonical, leaderIdentityCandidates, true);
        if (leaderKey == null || !this.BiomeRunestone$isActiveLeaderInternal(leaderKey)) {
            return 0;
        }

        int count = 0;
        for (Map.Entry<String, String> entry : this.inviteTargetToLeader.entrySet()) {
            String inviteLeader = this.BiomeRunestone$normalizeIdentity(entry.getValue());
            if (inviteLeader == null || !leaderKey.equals(inviteLeader)) {
                continue;
            }
            Long expiresAt = this.inviteTargetToExpiresAt.get(entry.getKey());
            if (expiresAt == null || expiresAt.longValue() != INVITE_PENDING_ONLINE_ACTIVATION) {
                continue;
            }
            ++count;
        }
        return count;
    }

    public int getTeamCount() {
        int count = 0;
        for (Map.Entry<String, String> entry : this.memberToLeader.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equals(entry.getValue())) {
                ++count;
            }
        }
        return count;
    }

    public int getMemberRecordCount() {
        return this.memberToLeader.size();
    }

    public int getInviteCount() {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        return this.inviteTargetToLeader.size();
    }

    public int getPendingOnlineInviteCount() {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        int count = 0;
        for (Long expiresAt : this.inviteTargetToExpiresAt.values()) {
            if (expiresAt != null && expiresAt.longValue() == INVITE_PENDING_ONLINE_ACTIVATION) {
                ++count;
            }
        }
        return count;
    }

    public boolean activateInviteOnOnline(String canonicalTargetIdentity, Collection<String> targetIdentityCandidates) {
        String canonical = this.BiomeRunestone$normalizeIdentity(canonicalTargetIdentity);
        if (canonical == null) {
            return false;
        }

        String targetKey = this.BiomeRunestone$resolveExistingKey(this.inviteTargetToLeader, canonical, targetIdentityCandidates, true);
        if (targetKey == null) {
            return false;
        }

        Long expiresAt = this.inviteTargetToExpiresAt.get(targetKey);
        if (expiresAt == null || expiresAt.longValue() != INVITE_PENDING_ONLINE_ACTIVATION) {
            return false;
        }

        this.inviteTargetToExpiresAt.put(targetKey, Long.valueOf(System.currentTimeMillis() + INVITE_EXPIRE_MILLIS));
        this.markDirty();
        return true;
    }

    public String transferLeadership(String canonicalCurrentLeaderIdentity, Collection<String> currentLeaderIdentityCandidates,
                                     String canonicalNewLeaderIdentity, Collection<String> newLeaderIdentityCandidates) {
        this.BiomeRunestone$sweepExpiredInvites(System.currentTimeMillis());
        String currentCanonical = this.BiomeRunestone$normalizeIdentity(canonicalCurrentLeaderIdentity);
        String newCanonical = this.BiomeRunestone$normalizeIdentity(canonicalNewLeaderIdentity);
        if (currentCanonical == null || newCanonical == null) {
            return null;
        }

        String currentLeaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, currentCanonical, currentLeaderIdentityCandidates, true);
        if (currentLeaderKey == null) {
            return null;
        }
        String currentLeaderSelf = this.memberToLeader.get(currentLeaderKey);
        if (!currentLeaderKey.equals(currentLeaderSelf)) {
            return null;
        }

        String newLeaderKey = this.BiomeRunestone$resolveExistingKey(this.memberToLeader, newCanonical, newLeaderIdentityCandidates, true);
        if (newLeaderKey == null || currentLeaderKey.equals(newLeaderKey)) {
            return null;
        }

        String newLeaderCurrentLeader = this.memberToLeader.get(newLeaderKey);
        if (!currentLeaderKey.equals(newLeaderCurrentLeader)) {
            return null;
        }

        boolean changed = false;
        for (Map.Entry<String, String> entry : this.memberToLeader.entrySet()) {
            if (currentLeaderKey.equals(entry.getValue())) {
                entry.setValue(newLeaderKey);
                changed = true;
            }
        }

        if (!newLeaderKey.equals(this.memberToLeader.get(newLeaderKey))) {
            this.memberToLeader.put(newLeaderKey, newLeaderKey);
            changed = true;
        }

        for (Map.Entry<String, String> entry : this.inviteTargetToLeader.entrySet()) {
            if (currentLeaderKey.equals(entry.getValue())) {
                entry.setValue(newLeaderKey);
                changed = true;
            }
        }

        if (this.inviteTargetToLeader.remove(newLeaderKey) != null) {
            changed = true;
        }
        if (this.inviteTargetToExpiresAt.remove(newLeaderKey) != null) {
            changed = true;
        }

        if (changed) {
            this.markDirty();
        }
        return newLeaderKey;
    }

    public void clearAll() {
        if (!this.memberToLeader.isEmpty() || !this.inviteTargetToLeader.isEmpty() || !this.inviteTargetToExpiresAt.isEmpty()) {
            this.memberToLeader.clear();
            this.inviteTargetToLeader.clear();
            this.inviteTargetToExpiresAt.clear();
            this.markDirty();
        }
    }

    public static int getInviteExpireSeconds() {
        return (int) (INVITE_EXPIRE_MILLIS / 1000L);
    }

    private boolean BiomeRunestone$isActiveLeaderInternal(String leaderIdentity) {
        String leader = this.BiomeRunestone$normalizeIdentity(leaderIdentity);
        if (leader == null) {
            return false;
        }
        String selfLeader = this.memberToLeader.get(leader);
        return leader.equals(selfLeader);
    }

    private void BiomeRunestone$sweepExpiredInvites(long now) {
        boolean changed = false;
        Iterator<Map.Entry<String, Long>> iterator = this.inviteTargetToExpiresAt.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            Long expiresAt = entry.getValue();
            if (expiresAt != null && (expiresAt.longValue() == INVITE_PENDING_ONLINE_ACTIVATION || now < expiresAt.longValue())) {
                continue;
            }

            String target = entry.getKey();
            iterator.remove();
            if (this.inviteTargetToLeader.remove(target) != null) {
                changed = true;
            } else {
                changed = true;
            }
        }

        Iterator<Map.Entry<String, String>> inviteIterator = this.inviteTargetToLeader.entrySet().iterator();
        while (inviteIterator.hasNext()) {
            Map.Entry<String, String> entry = inviteIterator.next();
            if (this.inviteTargetToExpiresAt.containsKey(entry.getKey())) {
                continue;
            }
            inviteIterator.remove();
            changed = true;
        }

        if (changed) {
            this.markDirty();
        }
    }

    private String BiomeRunestone$resolveExistingKey(LinkedHashMap<String, String> map, String canonical, Collection<String> fallbackCandidates, boolean migrateToCanonical) {
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

            String value = map.remove(candidate);
            map.put(canonical, value);
            this.markDirty();
            return canonical;
        }

        return null;
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
