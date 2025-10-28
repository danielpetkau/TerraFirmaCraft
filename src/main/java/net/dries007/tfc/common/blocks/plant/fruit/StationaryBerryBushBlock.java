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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.common.blockentities.SeasonalPlantBlockEntity;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.common.blocks.soil.HoeOverlayBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.calendar.Month;
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
        final float temp = Climate.getAverageTemperature(level, pos);

        if (!climateRange.get().checkBoth(hydration, temp, false))
        {
            SeasonalPlantBlockEntity.reset(level, pos);
        }
        else
        {
            this.tick(state, level, pos, random);
        }
        super.randomTick(state, level, pos, random);
    }

    /**
     * Should only be called after the climate range has been checked
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
    {
        super.tick(state, level, pos, rand);

        // Must be in an active lifecycle to consider growing
        if (state.getValue(LIFECYCLE).active() && level.getBlockEntity(pos) instanceof SeasonalPlantBlockEntity counter)
        {
            // Then find the max number of times the plant could have grown in the time since the last update
            int maxCycles = (int) (counter.getTicksSinceUpdate() / TICKS_TO_GROW_BERRY_BUSH);
            if (maxCycles >= 1)
            {
                // Cap the number of cycles for longer time skips
                maxCycles = Math.min(maxCycles, 8);
                int cycles = 0;
                final int daysInMonth = Calendars.SERVER.getCalendarDaysInMonth();
                final long currentTick = Calendars.SERVER.getTicks();
                final long previousTick = counter.getLastUpdateTick();

                // If it's been 6+ months, skip the simulation and set cycles to the max value
                if (currentTick - previousTick >= (long) ICalendar.CALENDAR_TICKS_IN_DAY * daysInMonth * 6)
                {
                    cycles = 8;
                }
                else
                {
                    long simulatedTick = previousTick;
                    boolean checkReverseDirection = false;

                    // Check through the skipped time and only add growth if the plant was not dormant
                    while (cycles < maxCycles && simulatedTick < currentTick)
                    {
                        Month month = ICalendar.getMonthOfYear(simulatedTick, daysInMonth);
                        Lifecycle lifecycle = this.getLifecycleForMonth(month);
                        if (lifecycle != Lifecycle.DORMANT)
                        {
                            cycles++;
                            simulatedTick = simulatedTick + TICKS_TO_GROW_BERRY_BUSH;
                        }
                        else
                        {
                            // Stop checking the forward direction and check the reverse direction if we hit a dormant season
                            checkReverseDirection = true;
                            break;
                        }
                    }
                    if (checkReverseDirection)
                    {

                        // Check through the skipped time and only add growth if the plant was not dormant, but in the opposite direction
                        simulatedTick = currentTick;
                        while (cycles < maxCycles && simulatedTick > previousTick)
                        {
                            Month month = ICalendar.getMonthOfYear(simulatedTick, daysInMonth);
                            Lifecycle lifecycle = this.getLifecycleForMonth(month);
                            if (lifecycle != Lifecycle.DORMANT)
                            {
                                cycles++;
                                simulatedTick = simulatedTick + TICKS_TO_GROW_BERRY_BUSH;
                            }
                            else
                            {
                                // If this state is reached, we have found both ends of a dormant period and all uncounted time is dormancy
                                // We can be sure it is the same dormant period because only one such period can be found in a 6-month span
                                break;
                            }
                        }
                    }
                }

                // Reset the counter because at this point we are either growing or in the middle of a dormant season
                counter.resetCounter();

                // Verify we actually had enough time to grow
                if (cycles > 0)
                {
                    growAndPropagate(state, level, pos, rand, cycles);
                }
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

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        SeasonalPlantBlockEntity.reset(level, pos);
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    /**
     * Performs growth and (optional) propagation of the bush.
     * Propagation should be naturally limited to not cause runaway generation.
     */
    protected void growAndPropagate(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, int cycles)
    {
        cycles--;

        final int oldStage = state.getValue(STAGE);
        if (oldStage < 2)
        {
            // Increment stage by one if not fully grown
            final BlockState newState = state.setValue(STAGE, state.getValue(STAGE) + 1);
            placeBlockAndResetCounter(level, pos, newState, cycles);
            return;
        }

        // Otherwise, attempt to propagate
        // Conditions to propagate:
        // 1. Must be in max growth stage, and an active lifecycle
        // 2. Must not have more than 3 total bushes within the expansion radius
        int count = 0;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 1, 2)))
        {
            if (level.getBlockState(target).getBlock() == this)
            {
                count++;
                if (count > 3)
                {
                    return;
                }
            }
        }

        // Then, try and pick a random position within the expansion radius, and place a bush there.
        final BlockPos.MutableBlockPos cursor = pos.mutable();
        for (int tries = 0; tries < 3; tries++)
        {
            cursor.setWithOffset(pos, Helpers.triangle(random, 3), 0, Helpers.triangle(random, 3));
            final BlockPos newPos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cursor);
            final BlockState placementState = getNewState(level, newPos);
            if (canPlaceNewBushAt(level, newPos, placementState))
            {
                placeBlockAndResetCounter(level, newPos, placementState, cycles);
                return;
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
        level.getBlockState(pos).tick(level, pos, level.random); // TODO: Decide if we are using, tick, random tick, or schedule tick for this, and stick to it in all versions of this method
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
