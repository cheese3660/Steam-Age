package com.lexi.steamage.systems;

import com.hypixel.hytale.builtin.adventure.farming.config.FarmingCoopAsset;
import com.hypixel.hytale.builtin.adventure.farming.states.CoopBlock;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockComponentChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.lexi.steamage.components.ChuteBlock;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Objects;

public class ChuteSystems {
    public static class OnChuteAdded extends RefSystem<ChunkStore> {
        private static final Query<ChunkStore> QUERY = Query.and(BlockModule.BlockStateInfo.getComponentType(), ChuteBlock.getComponentType());

        @Override
        public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason reason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
            ChuteBlock chuteBlock = commandBuffer.getComponent(ref, ChuteBlock.getComponentType());
            if (chuteBlock == null) return;
            WorldTimeResource worldTimeResource = commandBuffer
                    .getExternalData()
                    .getWorld()
                    .getEntityStore()
                    .getStore()
                    .getResource(WorldTimeResource.getResourceType());
            BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

            assert info != null;

            int x = ChunkUtil.xFromBlockInColumn(info.getIndex());
            int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
            int z = ChunkUtil.zFromBlockInColumn(info.getIndex());
            BlockChunk blockChunk = commandBuffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());

            assert blockChunk != null;

            BlockSection blockSection = blockChunk.getSectionAtBlockY(y);
            blockSection.scheduleTick(ChunkUtil.indexBlock(x,y,z), chuteBlock.getNextScheduleTick(worldTimeResource));
        }

        @Override
        public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason reason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
            if (reason == RemoveReason.UNLOAD) return;
            ChuteBlock chuteBlock = commandBuffer.getComponent(ref, ChuteBlock.getComponentType());
            if (chuteBlock == null) return;
            BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

            assert info != null;

            Store<EntityStore> entityStore = commandBuffer.getExternalData().getWorld().getEntityStore().getStore();
            int x = ChunkUtil.xFromBlockInColumn(info.getIndex());
            int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
            int z = ChunkUtil.zFromBlockInColumn(info.getIndex());
            BlockChunk blockChunk = commandBuffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());

            assert blockChunk != null;

            ChunkColumn column = commandBuffer.getComponent(info.getChunkRef(), ChunkColumn.getComponentType());

            assert column != null;

            Ref<ChunkStore> sectionRef = column.getSection(ChunkUtil.chunkCoordinate(y));

            assert sectionRef != null;

            BlockSection blockSection = commandBuffer.getComponent(sectionRef, BlockSection.getComponentType());

            assert blockSection != null;

            ChunkSection chunkSection = commandBuffer.getComponent(sectionRef, ChunkSection.getComponentType());

            assert chunkSection != null;

            int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getX(), x);
            int worldY = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getY(), y);
            int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getZ(), z);

            World world = commandBuffer.getExternalData().getWorld();
            chuteBlock.handleBlockBroken(world, entityStore, worldX, worldY, worldZ);
        }

        @Override
        public @Nullable Query<ChunkStore> getQuery() {
            return QUERY;
        }
    }

    public static class ChuteTicking extends EntityTickingSystem<ChunkStore> {
        private static final Query<ChunkStore> QUERY = Query.and(BlockSection.getComponentType(), ChunkSection.getComponentType());

        // Fields for accessing protected item containers because gods
        private static final Field COOP_ITEM_CONTAINER;

        static {
            try {
                COOP_ITEM_CONTAINER = CoopBlock.class.getDeclaredField("itemContainer");
                COOP_ITEM_CONTAINER.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
            BlockSection blocks = archetypeChunk.getComponent(index, BlockSection.getComponentType());
            assert blocks != null;
            if (blocks.getTickingBlocksCountCopy() == 0) return;
            ChunkSection section = archetypeChunk.getComponent(index, ChunkSection.getComponentType());

            assert section != null;

            if (section.getChunkColumnReference() == null) return;
            if (!section.getChunkColumnReference().isValid()) return;

            BlockComponentChunk blockComponentChunk = commandBuffer.getComponent(section.getChunkColumnReference(), BlockComponentChunk.getComponentType());
            assert blockComponentChunk != null;

            blocks.forEachTicking(blockComponentChunk, commandBuffer, section.getY(), (componentChunk, buffer, localX, localY, localZ, blockId) -> {
                Ref<ChunkStore> blockRef = componentChunk.getEntityReference(ChunkUtil.indexBlockInColumn(localX,localY,localZ));
                if (blockRef == null) return BlockTickStrategy.IGNORED;
                ChuteBlock chuteBlock = buffer.getComponent(blockRef, ChuteBlock.getComponentType());
                if (chuteBlock == null) return BlockTickStrategy.IGNORED;
                tickChute(buffer,componentChunk,blockRef,chuteBlock);
                return BlockTickStrategy.SLEEP;
            });
        }

        private static void tickChute(CommandBuffer<ChunkStore> commandBuffer, BlockComponentChunk blockComponentChunk, Ref<ChunkStore> blockRef, ChuteBlock chuteBlock)
        {
            BlockModule.BlockStateInfo info = commandBuffer.getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType());
            assert info != null;
            Store<EntityStore> store = commandBuffer.getExternalData().getWorld().getEntityStore().getStore();
            WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());

            int x = ChunkUtil.xFromBlockInColumn(info.getIndex());
            int y = ChunkUtil.yFromBlockInColumn(info.getIndex());
            int z = ChunkUtil.zFromBlockInColumn(info.getIndex());
            BlockChunk blockChunk = commandBuffer.getComponent(info.getChunkRef(), BlockChunk.getComponentType());

            assert blockChunk != null;

            ChunkColumn column = commandBuffer.getComponent(info.getChunkRef(), ChunkColumn.getComponentType());

            assert column != null;

            Ref<ChunkStore> sectionRef = column.getSection(ChunkUtil.chunkCoordinate(y));

            assert sectionRef != null;

            BlockSection blockSection = commandBuffer.getComponent(sectionRef, BlockSection.getComponentType());

            assert blockSection != null;

            ChunkSection chunkSection = commandBuffer.getComponent(sectionRef, ChunkSection.getComponentType());

            assert chunkSection != null;

            int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getX(), x);
            int worldY = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getY(), y);
            int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkSection.getZ(), z);

            World world = commandBuffer.getExternalData().getWorld();
            WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(worldX, worldZ));

            assert chunk != null;

            var upContainer = getItemContainer(chunk, worldX, worldY + 1, worldZ);
            var downContainer = getItemContainer(chunk, worldX, worldY - 1, worldZ);
            var chuteContainer = chuteBlock.storage;
            if (downContainer != null)
            {
                transferFirstItem(chuteContainer,downContainer);
            }

            if (upContainer != null)
            {
                transferFirstItem(upContainer,chuteContainer);
            }

            var nextTick = chuteBlock.getNextScheduleTick(worldTimeResource);
            blockSection.scheduleTick(ChunkUtil.indexBlock(x,y,z),nextTick);
        }

        @Nullable
        private static ItemContainer getItemContainer(WorldChunk worldChunk, int worldX, int worldY, int worldZ)
        {
            /* Handle chests/item containers in general */
            if (worldChunk.getState(worldX, worldY, worldZ) instanceof ItemContainerState itemContainerState) {
                return itemContainerState.getItemContainer();
            }

            var worldEntity = worldChunk.getBlockComponentEntity(worldX, worldY, worldZ);
            if (worldEntity == null) return null;
            var store = worldEntity.getStore();
            var archetype = store.getArchetype(worldEntity);

            /* Handle other Chutes */
            if (store.getComponent(worldEntity, ChuteBlock.getComponentType()) instanceof ChuteBlock chuteBlock) {
                return chuteBlock.storage;
            }

            /* Handle coops */
            if (store.getComponent(worldEntity, CoopBlock.getComponentType()) instanceof CoopBlock coopBlock) {
                /* Reflection cuz of course it has to be protected, and java doesn't have publicizers */
                try {
                    return (ItemContainer)COOP_ITEM_CONTAINER.get(coopBlock);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            /* Otherwise no such state exists */
            return null;
        }

        private static short getFirstStack(ItemContainer container)
        {
            var cap = container.getCapacity();
            for (short i = 0; i < cap; i++) {
                var stack = container.getItemStack(i);
                if (stack != null && !stack.isEmpty()) return i;
            }
            return -1;
        }

        private static boolean insertItem(ItemContainer container, ItemStack stackToInsertFrom)
        {
            var transaction = container.addItemStack(Objects.requireNonNull(stackToInsertFrom.withQuantity(1)));
            return transaction.succeeded();
        }

        private static void transferFirstItem(ItemContainer fromContainer, ItemContainer toContainer)
        {
            var stackToTakeFrom = getFirstStack(fromContainer);
            if (stackToTakeFrom == -1) return;
            var actualStack = fromContainer.getItemStack(stackToTakeFrom);
            assert actualStack != null;
            if (insertItem(toContainer, actualStack))
            {
                fromContainer.setItemStackForSlot(stackToTakeFrom, actualStack.withQuantity(actualStack.getQuantity() - 1));
            }
        }

        @Override
        public @Nullable Query<ChunkStore> getQuery() {
            return QUERY;
        }
    }
}
