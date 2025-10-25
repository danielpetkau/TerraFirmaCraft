/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks;

import java.util.Map;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.client.particle.TFCParticles;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.util.Helpers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class TFCBubbleColumnBlock extends BubbleColumnBlock
{
    private final Supplier<? extends Fluid> fluid;
    public static final BooleanProperty DRAG_DOWN = BubbleColumnBlock.DRAG_DOWN;
    
    // Map fluids to their respective bubble column blocks, set a lazy map to avoid crashes on load
    private static Map<Fluid, TFCBubbleColumnBlock> BubbleFluid;
    public static Map<Fluid, TFCBubbleColumnBlock> getBubbleFluidMap ()
    {
            if(BubbleFluid == null)
            {
                BubbleFluid = Map.of(
                Fluids.WATER.getSource(), (TFCBubbleColumnBlock) TFCBlocks.FRESHWATER_BUBBLE_COLUMN.get(),
                TFCFluids.SALT_WATER.getSource(), (TFCBubbleColumnBlock) TFCBlocks.SALTWATER_BUBBLE_COLUMN.get(),
                TFCFluids.SPRING_WATER.getSource(), (TFCBubbleColumnBlock) TFCBlocks.SPRING_WATER_BUBBLE_COLUMN.get()
                );
            }
            return BubbleFluid;
    }
        
    // Small helper function call map or default to freshwater bubble column
    public static TFCBubbleColumnBlock columnOf(Fluid fluid)
    {
        return getBubbleFluidMap().getOrDefault(fluid, (TFCBubbleColumnBlock) TFCBlocks.FRESHWATER_BUBBLE_COLUMN.get());
    }

    // Determine if bubbles go up or down 
    public static boolean isDownBubbles(LevelAccessor level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TFCBubbleColumnBlock)
            return state.getValue(BubbleColumnBlock.DRAG_DOWN);

        if (state.getBlock() instanceof TFCMagmaBlock)
            return true;
        return false;
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return fluid.get().defaultFluidState();
    }

    public static void updateColumnForFluid(LevelAccessor level, BlockPos pos)
    {
        BlockState startState = level.getBlockState(pos);
        if (!(startState.getBlock() instanceof TFCMagmaBlock)) return;

        BlockPos.MutableBlockPos cursor = pos.above().mutable();
        Fluid startingFluid = level.getFluidState(cursor).getType();
        TFCBubbleColumnBlock columnBlock = columnOf(startingFluid);
        BlockState newColumn = columnBlock.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, true);

        while (cursor.getY() >= level.getMinBuildHeight() && cursor.getY() < level.getMaxBuildHeight())
        {
            BlockState state = level.getBlockState(cursor);

            if (!canExistIn(state) || state.getBlock() instanceof TFCMagmaBlock)
            {
                // Replace existing bubble column with fluid if necessary
                if (state.getBlock() instanceof TFCBubbleColumnBlock)
                {
                    level.setBlock(cursor, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
                }
                break;
            }

            // Only set the block if it's not already correct
            if (!Helpers.isBlock(state, newColumn.getBlock()))
            {
                level.setBlock(cursor, newColumn, Block.UPDATE_ALL);
            }

            cursor.move(Direction.UP);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        double d0 = (double)pos.getX();
        double d1 = (double)pos.getY();
        double d2 = (double)pos.getZ();
        if (level.isEmptyBlock(pos.above()) && this == columnOf(TFCFluids.SPRING_WATER.getSource()))
            level.addParticle(TFCParticles.STEAM.get(), d0, d1 + 1.0D, d2, 0.0D, 0.0D, 0.0D);
        if (isDownBubbles(level, pos))
        {
            level.addParticle(TFCParticles.BUBBLE_COLUMN_DOWN.get(), d0 + 0.5, d1 + 0.8, d2, 0.0, 0.0, 0.0);
        }
        else
        {
            level.addAlwaysVisibleParticle(TFCParticles.BUBBLE_COLUMN_UP.get(), d0 + 0.5, d1, d2 + 0.5, 0.0, 0.04, 0.0);
            level.addAlwaysVisibleParticle(TFCParticles.BUBBLE_COLUMN_UP.get(), d0 + (double)random.nextFloat(), d1 + (double)random.nextFloat(), d2 + (double)random.nextFloat(), 0.0, 0.04, 0.0);
        }
    }

    // Determines if bubble column is allowed to exist in current block state
    public static boolean canExistIn(BlockState state)
    {
        // Bubble column already exists here
        if (state.getBlock() instanceof TFCBubbleColumnBlock) return true;
        // If block state is air or empty, check if fluid supports bubble columns
        if (FluidHelpers.isAirOrEmptyFluid(state) && !state.isAir())
        {
            return columnOf(state.getFluidState().getType()) != null;
        }
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
    {
        BlockState below = level.getBlockState(pos.below());
        return below.getBlock() instanceof TFCBubbleColumnBlock
            || below.getBlock() instanceof TFCMagmaBlock;
    }

    public TFCBubbleColumnBlock(Properties properties, Supplier<? extends Fluid> fluid)
    {
        super(properties);
        this.fluid = fluid;
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity)
    {
        // Override to add the entity immune check for all our aquatic animals, who want to ignore these
        if (!Helpers.isEntity(entity, TFCTags.Entities.BUBBLE_COLUMN_IMMUNE))
        {
            super.entityInside(state, level, pos, entity);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource rand)
    {
        updateColumnForFluid(level, pos);
    }

    // Modified from the vanilla one in order to support TFC fluids
    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos)
    {
        final Fluid fluid = getFluid();

        level.scheduleTick(pos, fluid, fluid.getTickDelay(level));
        if (!state.canSurvive(level, pos))
        {
            return fluid.defaultFluidState().createLegacyBlock();
        } 
        
        if (
            facing == Direction.DOWN ||
            (facing == Direction.UP && !(facingState.getBlock() instanceof TFCBubbleColumnBlock) && canExistIn(facingState)))
        {
            level.scheduleTick(pos, this, 5);
        }

        return state;
    }

    @Override
    public ItemStack pickupBlock(@Nullable Player player, LevelAccessor level, BlockPos pos, BlockState state)
    {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        return new ItemStack(getFluid().getBucket());
    }

    public Fluid getFluid()
    {
        return fluid.get();
    }
}
