package com.github.hahahha.BiomeRunestone.util;

import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.World;
import net.minecraft.WorldSavedData;

import java.util.LinkedHashMap;
import java.util.Map;

public class RunegateSearchStatsData extends WorldSavedData {
    private static final String DATA_NAME = "biome_runestone_runegate_search_stats";
    private static final String TAG_RANDOM_EVENTS = "RandomEvents";
    private static final String TAG_PLAYER_EVENTS = "PlayerEvents";
    private static final String TAG_KEY = "K";
    private static final String TAG_COUNT = "C";

    private final LinkedHashMap<String, Integer> randomEvents = new LinkedHashMap<String, Integer>();
    private final LinkedHashMap<String, Integer> playerEvents = new LinkedHashMap<String, Integer>();

    public RunegateSearchStatsData() {
        this(DATA_NAME);
    }

    public RunegateSearchStatsData(String mapName) {
        super(mapName);
    }

    public static RunegateSearchStatsData get(World world) {
        RunegateSearchStatsData data = (RunegateSearchStatsData) world.loadItemData(RunegateSearchStatsData.class, DATA_NAME);
        if (data == null) {
            data = new RunegateSearchStatsData(DATA_NAME);
            world.setItemData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.randomEvents.clear();
        this.playerEvents.clear();
        this.readEventMap(nbt.getTagList(TAG_RANDOM_EVENTS), this.randomEvents);
        this.readEventMap(nbt.getTagList(TAG_PLAYER_EVENTS), this.playerEvents);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        nbt.setTag(TAG_RANDOM_EVENTS, this.writeEventMap(this.randomEvents));
        nbt.setTag(TAG_PLAYER_EVENTS, this.writeEventMap(this.playerEvents));
    }

    public void addEvent(boolean playerMode, String eventKey) {
        if (eventKey == null || eventKey.isEmpty()) {
            return;
        }

        LinkedHashMap<String, Integer> targetMap = playerMode ? this.playerEvents : this.randomEvents;
        Integer current = targetMap.get(eventKey);
        targetMap.put(eventKey, current == null ? Integer.valueOf(1) : Integer.valueOf(current.intValue() + 1));
        this.markDirty();
    }

    public String getRandomEventSummary() {
        return RunegateSearchMetrics.buildEventSummary(this.randomEvents);
    }

    public String getPlayerEventSummary() {
        return RunegateSearchMetrics.buildEventSummary(this.playerEvents);
    }

    public int clearAll() {
        int removed = this.randomEvents.size() + this.playerEvents.size();
        if (removed > 0) {
            this.randomEvents.clear();
            this.playerEvents.clear();
            this.markDirty();
        }
        return removed;
    }

    private void readEventMap(NBTTagList list, LinkedHashMap<String, Integer> targetMap) {
        for (int i = 0; i < list.tagCount(); ++i) {
            NBTTagCompound tag = (NBTTagCompound) list.tagAt(i);
            String key = tag.getString(TAG_KEY);
            if (key == null || key.isEmpty()) {
                continue;
            }
            int count = tag.getInteger(TAG_COUNT);
            if (count <= 0) {
                continue;
            }
            targetMap.put(key, Integer.valueOf(count));
        }
    }

    private NBTTagList writeEventMap(LinkedHashMap<String, Integer> sourceMap) {
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Integer> entry : sourceMap.entrySet()) {
            Integer count = entry.getValue();
            if (count == null || count.intValue() <= 0) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(TAG_KEY, entry.getKey());
            tag.setInteger(TAG_COUNT, count.intValue());
            list.appendTag(tag);
        }
        return list;
    }
}
