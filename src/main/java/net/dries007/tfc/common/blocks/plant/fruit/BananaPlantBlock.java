/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.plant.fruit;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.blockentities.SeasonalPlantBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blocks.ExtendedProperties;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.FarmlandBlock;
import net.dries007.tfc.common.blocks.soil.HoeOverlayBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.Climate;
import net.dries007.tfc.util.climate.ClimateRange;
import net.dries007.tfc.util.climate.ClimateRanges;

public class BananaPlantBlock extends SeasonalPlantBlock implements HoeOverlayBlock
{
    public static final long TICKS_TO_GROW_BANANA_PLANT = ICalendar.CALENDAR_TICKS_IN_DAY * 4; // TODO: Should be a config, should have a similar config for fruit trees

    public static void kill(Level level, BlockPos pos)
    {
        // picking bananas kills the plant. this propagates death to the whole stalk.
        Block deadBlock = TFCBlocks.DEAD_BANANA_PLANT.get();
        if (!level.isClientSide)
        {
            BlockState deadState = deadBlock.defaultBlockState();
            BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(pos.below());
            while (true)
            {
                BlockState foundState = level.getBlockState(mutable);
                if (!Helpers.isBlock(foundState, TFCBlocks.BANANA_PLANT.get())) break;
                level.setBlockAndUpdate(mutable, deadState.setValue(STAGE, foundState.getValue(STAGE)));
                mutable.move(Direction.DOWN);
            }
        }
    }

    public static final VoxelShape PLANT = box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);
    private static final VoxelShape TRUNK_0 = box(4.0, 0.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape TRUNK_1 = box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);

    public BananaPlantBlock(ExtendedProperties properties, Supplier<? extends Item> productItem, Lifecycle[] stages)
    {
        super(properties, ClimateRanges.BANANA_PLANT, productItem, stages);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
    {
        final BlockPos basePos;
        if (level.getBlockEntity(pos) instanceof SeasonalPlantBlockEntity plant)
        {
            basePos = plant.getStemPos();
        }
        else
        {
            basePos = pos;
        }
        final int hydration = getFruitBushHydrationFromRootPos(level, basePos);
        final float temp = Climate.getAverageTemperature(level, basePos);

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
            int cycles = (int) (counter.getTicksSinceUpdate() / TICKS_TO_GROW_BANANA_PLANT);
            if (cycles >= 1)
            {
                grow(state, level, pos, rand, cycles);
                counter.resetCounter();
            }
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult)
    {
        final ItemInteractionResult result = super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        if (result.consumesAction())
        {
            kill(level, pos);
        }
        return result;
    }

    @Override
    public BlockState stateAfterPicking(BlockState state)
    {
        return TFCBlocks.DEAD_BANANA_PLANT.get().defaultBlockState().setValue(STAGE, 2);
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
    public ItemStack getProductItem(RandomSource random)
    {
        return new ItemStack(productItem.get(), Mth.nextInt(random, 3, 6));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return switch (state.getValue(STAGE))
        {
            case 0 -> TRUNK_0;
            case 1 -> TRUNK_1;
            default -> PLANT;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
    {
        return state.getValue(STAGE) == 2 ? Shapes.empty() : getShape(state, level, pos, context);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        // no op the superclass
    }

    /**
     * Performs growth of the plant
     */
    protected void grow(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, int cycles)
    {
        cycles = Math.min(cycles - 1, 8); // TODO

        // TODO: I don't expect this to work in its current state, and I'd like to rework bananas to always have leaves anyways
        onUpdate(level, pos, state);

        // Must be in an active lifecycle to grow at all
        if (!state.getValue(LIFECYCLE).active()) // TODO: Probably should just check this earlier?
        {
            return;
        }

        final int oldStage = state.getValue(STAGE);

        // Bananas only grow for stage 1
        if (oldStage == 1)
        {
            final BlockPos abovePos = pos.above();
            if (level.isEmptyBlock(abovePos) && level.canSeeSky(abovePos))
            {
                // If not too tall, continue to grow upwards
                int distanceToGround = distanceToGround(level, pos, 6);
                if (distanceToGround < random.nextInt(3, 6))
                {
                    final BlockState newState = state.setValue(STAGE, 0);
                    placeTrunk(level, abovePos, pos, state, newState, cycles);
                    return;
                }
                // Otherwise, place top block
                level.setBlockAndUpdate(pos, state.setValue(STAGE, 2));
            }
        }
    }

    private void placeTrunk(ServerLevel level, BlockPos newPos, BlockPos oldPos, BlockState newPosState, BlockState oldPosState, int cycles)
    {
        level.setBlockAndUpdate(newPos, newPosState);
        level.setBlockAndUpdate(oldPos, oldPosState);
        // If block grows, set the new block's stem position to match the original
        if (level.getBlockEntity(oldPos) instanceof SeasonalPlantBlockEntity sourceBush && level.getBlockEntity(newPos) instanceof SeasonalPlantBlockEntity newBush)
        {
            newBush.resetCounter();
            newBush.increaseCounter(TICKS_TO_GROW_BANANA_PLANT * cycles);

            newBush.setStemPos(sourceBush.getStemPos());
        }
        else
        {
            TerraFirmaCraft.LOGGER.error("Failed to update growing berry bush block entity at: {}", oldPos);
        }
        level.getBlockState(newPos).randomTick(level, newPos, level.random);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if (state.getValue(STAGE) == 2 && newState.isAir())
        {
            kill(level, pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        BlockPos belowPos = pos.below();
        BlockState belowState = level.getBlockState(belowPos);
        return Helpers.isBlock(belowState, TFCTags.Blocks.BUSH_PLANTABLE_ON) || Helpers.isBlock(belowState, this);
    }
}
