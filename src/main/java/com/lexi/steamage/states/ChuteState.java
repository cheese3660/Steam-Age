package com.lexi.steamage.states;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.universe.world.chunk.state.TickableBlockState;
import com.hypixel.hytale.server.core.universe.world.meta.BlockState;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.worldmap.WorldMapManager;
import com.lexi.steamage.SteamAge;

import javax.annotation.Nullable;
import java.util.Objects;

@SuppressWarnings("removal")
public class ChuteState extends ItemContainerState implements TickableBlockState {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final Codec<ChuteState> CODEC = BuilderCodec.builder(ChuteState.class, ChuteState::new, BlockState.BASE_CODEC)
            .append(new KeyedCodec<>("Custom", Codec.BOOLEAN), (state, o) -> state.custom = o, state -> state.custom)
            .add()
            .append(new KeyedCodec<>("AllowViewing", Codec.BOOLEAN), (state, o) -> state.allowViewing = o, state -> state.allowViewing)
            .add()
            .append(new KeyedCodec<>("Droplist", Codec.STRING), (state, o) -> state.droplist = o, state -> state.droplist)
            .add()
            .append(new KeyedCodec<>("Marker", WorldMapManager.MarkerReference.CODEC), (state, o) -> state.marker = o, state -> state.marker)
            .add()
            .append(new KeyedCodec<>("ItemContainer", SimpleItemContainer.CODEC), (state, o) -> state.itemContainer = o, state -> state.itemContainer)
            .add()
            .build();


    @Override
    public void tick(float v, int i, ArchetypeChunk<ChunkStore> archetypeChunk, Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer) {
        var up = getBlockPosition().add(0, 1, 0 );
        var down = getBlockPosition().add(0, -1, 0 );
        if (getChunk() == null) return;
        var upState = getChunk().getState(up.x, up.y, up.z);
        var downState = getChunk().getState(down.x, down.y, down.z);
        if (downState instanceof ItemContainerState itemContainerState) {
            transferFirstItem(getItemContainer(), itemContainerState.getItemContainer());
        }
        if (upState instanceof ItemContainerState itemContainerState) {
            transferFirstItem(itemContainerState.getItemContainer(), this.getItemContainer());
        }
    }

    private short getFirstStack(ItemContainer container)
    {
        var cap = container.getCapacity();
        for (short i = 0; i < cap; i++) {
            var stack = container.getItemStack(i);
            if (stack != null && !stack.isEmpty()) return i;
        }
        return -1;
    }

    private boolean insertItem(ItemContainer container, ItemStack stackToInsertFrom)
    {
        var transaction = container.addItemStack(Objects.requireNonNull(stackToInsertFrom.withQuantity(1)));
        return transaction.succeeded();
    }

    private void transferFirstItem(ItemContainer upContainer, ItemContainer downContainer)
    {
        var stackToTakeFrom = getFirstStack(upContainer);
        if (stackToTakeFrom == -1) return;
        var actualStack = upContainer.getItemStack(stackToTakeFrom);
        assert actualStack != null;
        if (insertItem(downContainer, actualStack))
        {
             upContainer.setItemStackForSlot(stackToTakeFrom, actualStack.withQuantity(actualStack.getQuantity() - 1));
        }
    }
}
