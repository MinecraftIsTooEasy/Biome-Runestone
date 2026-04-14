package com.github.hahahha.BiomeRunestone.util;

import net.minecraft.NBTTagCompound;
import net.minecraft.NBTTagList;
import net.minecraft.World;
import net.minecraft.WorldSavedData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class RunegatePlayerIdentityIndexData extends WorldSavedData {
    private static final String DATA_NAME = "biome_runestone_player_identity_index";
    private static final String TAG_ENTRIES = "Entries";
    private static final String TAG_NAME = "N";
    private static final String TAG_UUID = "U";

    private final LinkedHashMap<String, String> lowerNameToUuid = new LinkedHashMap<String, String>();

    public RunegatePlayerIdentityIndexData() {
        this(DATA_NAME);
    }

    public RunegatePlayerIdentityIndexData(String mapName) {
        super(mapName);
    }

    public static RunegatePlayerIdentityIndexData get(World world) {
        RunegatePlayerIdentityIndexData data = (RunegatePlayerIdentityIndexData) world.loadItemData(RunegatePlayerIdentityIndexData.class, DATA_NAME);
        if (data == null) {
            data = new RunegatePlayerIdentityIndexData(DATA_NAME);
            world.setItemData(DATA_NAME, data);
        }
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        this.lowerNameToUuid.clear();
        NBTTagList entries = nbt.getTagList(TAG_ENTRIES);
        for (int i = 0; i < entries.tagCount(); ++i) {
            NBTTagCompound entry = (NBTTagCompound) entries.tagAt(i);
            String name = this.BiomeRunestone$normalizeName(entry.getString(TAG_NAME));
            String uuid = this.BiomeRunestone$normalizeUuid(entry.getString(TAG_UUID));
            if (name == null || uuid == null) {
                continue;
            }
            this.lowerNameToUuid.put(name, uuid);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        NBTTagList entries = new NBTTagList();
        for (Map.Entry<String, String> entry : this.lowerNameToUuid.entrySet()) {
            String name = this.BiomeRunestone$normalizeName(entry.getKey());
            String uuid = this.BiomeRunestone$normalizeUuid(entry.getValue());
            if (name == null || uuid == null) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString(TAG_NAME, name);
            tag.setString(TAG_UUID, uuid);
            entries.appendTag(tag);
        }
        nbt.setTag(TAG_ENTRIES, entries);
    }

    public void upsertIdentity(String playerName, String playerUuidIdentity) {
        String lowerName = this.BiomeRunestone$normalizeName(playerName);
        String uuid = this.BiomeRunestone$normalizeUuid(playerUuidIdentity);
        if (lowerName == null || uuid == null) {
            return;
        }

        String previous = this.lowerNameToUuid.put(lowerName, uuid);
        if (previous == null || !previous.equals(uuid)) {
            this.markDirty();
        }
    }

    public String getUuidByName(String playerName) {
        String lowerName = this.BiomeRunestone$normalizeName(playerName);
        if (lowerName == null) {
            return null;
        }
        return this.lowerNameToUuid.get(lowerName);
    }

    private String BiomeRunestone$normalizeName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    private String BiomeRunestone$normalizeUuid(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(trimmed).toString();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
