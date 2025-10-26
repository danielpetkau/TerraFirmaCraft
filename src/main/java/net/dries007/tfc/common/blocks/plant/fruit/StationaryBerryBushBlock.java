/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.plant.fruit;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.common.blockentities.SeasonalPlantBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.common.blocks.soil.HoeOverlayBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;

public class StationaryBerryBushBlock extends SeasonalPlantBlock implements HoeOverlayBlock
{
    public static final long TICKS_TO_GROW_BERRY_BUSH = ICalendar.CALENDAR_TICKS_IN_DAY * 2; // TODO: Should be a config, should have a similar config for fruit trees

    private static final VoxelShape HALF_PLANT = box(2, 0, 2, 14, 8, 14);

    public StationaryBerryBushBlock(ExtendedProperties properties, Supplier<? extends Item> productItem, Lifecycle[] lifecycle, Supplier<ClimateRange> climateRange)
    {
        super(properties, climateRange, productItem, lifecycle);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {
        return defaultBlockState().setValue(LIFECYCLE, getLifecycleForCurrentMonth(context.getLevel(), context.getClickedPos()).active() ? Lifecycle.HEALTHY : Lifecycle.DORMANT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return HALF_PLANT;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        final int hydration = getFruitBushHydration(level, pos);
        final float temp = Climate.getAverageTemperature(level, pos); // TODO: Must override this for other fruit bushes that may have separate root positions

        if (!climateRange.get().checkBoth(hydration, temp, false))
        {
            SeasonalPlantBlockEntity.reset(level, pos);
        }
        else
        {
            this.tick(state, level, pos, random);
        }
        super.randomTick(state, level, pos, random); // TODO: Verify needed
    }

    // TODO: Update this comment if I re-imagine how these things check climate
    /**
     * Should only be called after the climate range has been checked
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
    {
        super.tick(state, level, pos, rand);
        if (level.getBlockEntity(pos) instanceof SeasonalPlantBlockEntity counter)
        {
            int cycles = (int) (counter.getTicksSinceUpdate() / TICKS_TO_GROW_BERRY_BUSH);
            if (cycles >= 1)
            {
                growAndPropagate(state, level, pos, rand, cycles);
                counter.resetCounter();
            }
        }
    }

    @Override
    public void addHoeOverlayInfo(Level level, BlockPos pos, BlockState state, Consumer<Component> text, boolean isDebug)
    {
        if (level.getBlockEntity(pos) instanceof SeasonalPlantBlockEntity bush)
        {
            final ClimateRange range = climateRange.get();
            final BlockPos sourcePos = bush.getStemPos().below();
            final int hydration = getFruitBushHydration(level, pos);
            text.accept(FarmlandBlock.getHydrationTooltip(range, false, hydration));
            text.accept(FarmlandBlock.getAverageTemperatureTooltip(level, sourcePos, range, false));
        }
    }

    /**
     * Performs growth and (optional) propagation of the bush.
     * Propagation should be naturally limited to not cause runaway generation.
     */
    protected void growAndPropagate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, int cycles)
    {
        cycles = Math.min(cycles - 1, 8); // TODO: Probably move to a smarted place like wherever we check valid seasons

        // Must be in an active lifecycle to grow at all
        if (!state.getValue(LIFECYCLE).active()) // TODO: Probably should just check this earlier?
        {
            return;
        }

        final int oldStage = state.getValue(STAGE);
        if (oldStage < 2)
        {
            // Increment stage by one if not fully grown
            final BlockState newState = state.setValue(STAGE, state.getValue(STAGE) + 1);
            level.setBlock(pos, newState, 3);
            return;
        }

        // Otherwise, attempt to propagate
        // Conditions to propagate:
        // 1. Must be in max growth stage, and an active lifecycle
        // 2. Must not have more than 3 other bushes within the expansion radius
        int count = 0;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2)))
        {
            if (level.getBlockState(target).getBlock() == this)
            {
                count++;
                if (count > 4) // 3 + 1 because the above this block gets counted
                {
                    return;
                }
            }
        }

        // Then, try and pick a random position within the expansion radius, and place a bush there.
        final BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int tries = 0; tries < 6; tries++)
        {
            cursor.setWithOffset(pos, Helpers.triangle(random, 3), Helpers.triangle(random, 2), Helpers.triangle(random, 3));
            final BlockState placementState = getNewState(level, cursor);
            if (canPlaceNewBushAt(level, pos, placementState))
            {
                placeBlockAndResetCounter(level, pos, placementState, cycles);
            }
        }
    }

    protected void placeBlockAndResetCounter(ServerLevel level, BlockPos pos, BlockState state, int cycles)
    {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof SeasonalPlantBlockEntity bush)
        {
            bush.resetCounter();
            bush.increaseCounter(TICKS_TO_GROW_BERRY_BUSH * cycles);
        }
        else
        {
            TerraFirmaCraft.LOGGER.error("Failed to update propagated berry bush block entity at: {}", pos);
        }
        level.getBlockState(pos).randomTick(level, pos, level.random);
    }

    protected BlockState getNewState(Level level, BlockPos pos)
    {
        return defaultBlockState().setValue(STAGE, 0).setValue(LIFECYCLE, Lifecycle.HEALTHY);
    }

    protected boolean canPlaceNewBushAt(Level level, BlockPos pos, BlockState placementState)
    {
        return level.isEmptyBlock(pos) && placementState.canSurvive(level, pos);
    }

    protected BlockState getDeadState(BlockState state)
    {
        return TFCBlocks.DEAD_BERRY_BUSH.get().defaultBlockState().setValue(STAGE, state.getValue(STAGE));
    }
}
