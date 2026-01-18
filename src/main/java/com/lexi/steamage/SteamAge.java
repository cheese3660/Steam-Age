package com.lexi.steamage;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.meta.BlockStateModule;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.lexi.steamage.states.ChuteState;

public class SteamAge extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();


    public SteamAge(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        super.setup();
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));
        this.getBlockStateRegistry().registerBlockState(ChuteState.class, "Lexi_SteamAge_Chute", ChuteState.CODEC, ItemContainerState.ItemContainerStateData.class, ItemContainerState.ItemContainerStateData.CODEC);
    }
}
