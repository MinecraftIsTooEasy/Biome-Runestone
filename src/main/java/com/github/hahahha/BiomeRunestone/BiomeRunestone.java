package com.github.hahahha.BiomeRunestone;

import com.github.hahahha.BiomeRunestone.command.CommandBiomeRunestoneRune;
import com.github.hahahha.BiomeRunestone.util.Config;
import moddedmite.rustedironcore.api.event.Handlers;
import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.ModResourceManager;
import net.xiaoyu233.fml.reload.event.MITEEvents;

import java.util.logging.Logger;

public class BiomeRunestone implements ModInitializer {
    public static final String MOD_ID = "biome_runestone";
    public static final Logger LOGGER = Logger.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        Config.load();
        ModResourceManager.addResourcePackDomain(MOD_ID);
        MITEEvents.MITE_EVENT_BUS.register(new EventListen());
        Handlers.Command.register(event -> event.register(new CommandBiomeRunestoneRune()));
    }
}

