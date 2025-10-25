/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blockentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SeasonalPlantBlockEntity extends TickCounterBlockEntity
{
    public static void reset(Level level, BlockPos pos)
    {
        level.getBlockEntity(pos, TFCBlockEntities.SEASONAL_PLANT.get()).ifPresent(TickCounterBlockEntity::resetCounter);
    }

    public static void addTicks(Level level, BlockPos pos, long ticks)
    {
        level.getBlockEntity(pos, TFCBlockEntities.SEASONAL_PLANT.get()).ifPresent(entity -> entity.increaseCounter(ticks));
    }

    private BlockPos stemPos;

    public SeasonalPlantBlockEntity(BlockPos pos, BlockState state)
    {
        this(TFCBlockEntities.SEASONAL_PLANT.get(), pos, state, pos);
    }

    protected SeasonalPlantBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockPos stemPos)
    {
        super(type, pos, state);
        this.stemPos = stemPos;
    }

    @Override
    public void loadAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        if (nbt.contains("stemPos", CompoundTag.TAG_INT_ARRAY))
        {
            final int[] stemArray = nbt.getIntArray("stemPos");
            stemPos = new BlockPos(stemArray[0], stemArray[1], stemArray[2]);
        }
        else
        {
            stemPos = nbt.contains("stemPos", CompoundTag.TAG_LONG) ? BlockPos.of(nbt.getLong("stemPos")) : worldPosition;
        }
        super.loadAdditional(nbt, provider);
    }

    @Override
    public void saveAdditional(CompoundTag nbt, HolderLookup.Provider provider)
    {
        nbt.putLong("stemPos", stemPos.asLong());
        nbt.putIntArray("stemPos", new int[] {stemPos.getX(), stemPos.getY(), stemPos.getZ()});
        super.saveAdditional(nbt, provider);
    }

    public void setStemPos(BlockPos stemPos)
    {
        this.stemPos = stemPos;
        setChanged();
    }

    public BlockPos getStemPos()
    {
        return stemPos;
    }
}
