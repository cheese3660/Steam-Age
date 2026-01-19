package com.lexi.steamage;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.lexi.steamage.components.ChuteBlock;
import com.lexi.steamage.systems.ChuteSystems;

public class SteamAge extends JavaPlugin {
    private static SteamAge instance;
    public static SteamAge get() {
        return instance;
    }

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ComponentType<ChunkStore, ChuteBlock> chuteBlockStateComponentType;


    public SteamAge(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        instance = this;
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));
        // this.getBlockStateRegistry().registerBlockState(ChuteState.class, "Lexi_SteamAge_Chute", ChuteState.CODEC, ItemContainerState.ItemContainerStateData.class, ItemContainerState.ItemContainerStateData.CODEC);
        this.chuteBlockStateComponentType = this.getChunkStoreRegistry().registerComponent(ChuteBlock.class, "Chute", ChuteBlock.CODEC);
        this.getChunkStoreRegistry().registerSystem(new ChuteSystems.OnChuteAdded());
        this.getChunkStoreRegistry().registerSystem(new ChuteSystems.ChuteTicking());
    }

    public ComponentType<ChunkStore, ChuteBlock> getChuteBlockStateComponentType() {
        return chuteBlockStateComponentType;
    }
}
