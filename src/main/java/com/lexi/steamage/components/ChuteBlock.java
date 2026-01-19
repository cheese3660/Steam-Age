package com.lexi.steamage.components;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lexi.steamage.SteamAge;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class ChuteBlock implements Component<ChunkStore> {
    public static final BuilderCodec<ChuteBlock> CODEC = BuilderCodec.<ChuteBlock>builder(ChuteBlock.class, ChuteBlock::new)
            .append(new KeyedCodec<>("Storage", ItemContainer.CODEC), (chute, storage) -> chute.storage = storage, chute -> chute.storage)
            .add()
            .build();


    public static ComponentType<ChunkStore, ChuteBlock> getComponentType() {
        return SteamAge.get().getChuteBlockStateComponentType();
    }

    public ItemContainer storage;

    public ChuteBlock()
    {
        List<ItemStack> remainder = new ObjectArrayList<>();
        this.storage = ItemContainer.ensureContainerCapacity(this.storage, (short) 5, SimpleItemContainer::new, remainder);
    }

    public ChuteBlock(ItemContainer storage) {
        this.storage = storage.clone();
        List<ItemStack> remainder = new ObjectArrayList<>();
        this.storage = ItemContainer.ensureContainerCapacity(this.storage, (short) 5, SimpleItemContainer::new, remainder);
    }

    /**
     * We want to tick this block every 0.4 seconds
     * @param time the world time
     * @return the next tick to schedule
     */
    public Instant getNextScheduleTick(WorldTimeResource time)
    {
        Instant gameTime = time.getGameTime();
        return gameTime.plus(400, ChronoUnit.MILLIS);
    }

    /**
     * @return A duplicated instance of the ChuteBlock
     */
    @Override
    public @Nullable Component<ChunkStore> clone() {
        return new ChuteBlock(this.storage);
    }

    /**
     * Make sure drops get added to the world after the chute breaks
     *
     * @param world  The world
     * @param store  The entity store to be utilized for spawning the items
     * @param blockX The X position of the ChuteBlock
     * @param blockY The Y position of the ChuteBlock
     * @param blockZ The Z position of the ChuteBlock
     */
    public void handleBlockBroken(World world, Store<EntityStore> store, int blockX, int blockY, int blockZ)
    {
        Vector3d dropPosition = new Vector3d(blockX + 0.5F, blockY, blockZ + 0.5F);
        Holder<EntityStore>[] itemEntityHolders = ItemComponent.generateItemDrops(store, this.storage.removeAllItemStacks(), dropPosition, Vector3f.ZERO);
        if (itemEntityHolders.length > 0)
        {
            world.execute(() -> store.addEntities(itemEntityHolders, AddReason.SPAWN));
        }
    }
}
