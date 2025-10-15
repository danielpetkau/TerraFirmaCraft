/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks;

import java.util.function.Supplier;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.fluids.FluidHelpers;
import net.dries007.tfc.common.fluids.SimpleFluid;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.client.particle.TFCParticles;
import net.dries007.tfc.util.Helpers;

public class TFCBubbleColumnBlock extends BubbleColumnBlock
{
    private final Supplier<? extends Fluid> fluid;
    public static final BooleanProperty DRAG_DOWN = BubbleColumnBlock.DRAG_DOWN;
    // Map fluids to their respective bubble column blocks
    enum BubbleFluid
    {
        FRESH_WATER(Fluids.WATER.getSource(), (TFCBubbleColumnBlock) TFCBlocks.FRESHWATER_BUBBLE_COLUMN.get()),
        SALT_WATER(TFCFluids.SALT_WATER.getSource(), (TFCBubbleColumnBlock) TFCBlocks.SALTWATER_BUBBLE_COLUMN.get()),
        SPRING_WATER(TFCFluids.SPRING_WATER.getSource(), (TFCBubbleColumnBlock) TFCBlocks.SPRING_WATER_BUBBLE_COLUMN.get());
        final Fluid fluid;
        final TFCBubbleColumnBlock block;
        static final Map<Fluid, BubbleFluid> LOOKUP = Arrays.stream(values()).collect(Collectors.toMap(b -> b.fluid, b -> b)); // Maps fluids so you don't have to loop
        BubbleFluid(Fluid f, TFCBubbleColumnBlock b) { block = b; this.fluid = f; }
        static TFCBubbleColumnBlock of(Fluid f) { return LOOKUP.getOrDefault(f, BubbleFluid.FRESH_WATER).block; }
    }

    // Determine if bubbles go up or down 
    public static boolean isDownBubbles(LevelAccessor level, BlockPos pos)
    {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TFCBubbleColumnBlock) return state.getValue(BubbleColumnBlock.DRAG_DOWN);
        if (state.getBlock() instanceof TFCMagmaBlock) return true;
        return false;
    }

    @Override
    public FluidState getFluidState(BlockState state)
    {
        return fluid.get().defaultFluidState();
    }

    public static void updateColumnForFluid(LevelAccessor level, BlockPos pos)
    {
        BlockState current = level.getBlockState(pos);
        if (current.getBlock() instanceof TFCMagmaBlock)
        {
            BlockPos above = pos.above();
            BlockState aboveState = level.getBlockState(above);
            if (canExistIn(aboveState))
            {
                Fluid fluid = aboveState.getFluidState().getType();
                BlockState newColumn = BubbleFluid.of(fluid)
                        .defaultBlockState()
                        .setValue(BubbleColumnBlock.DRAG_DOWN, true); // magma is always down
                level.setBlock(above, newColumn, Block.UPDATE_ALL);
            }
        }
        boolean downBubbles = isDownBubbles(level, pos);
        BlockPos.MutableBlockPos cursor = pos.mutable();
        Direction direction = downBubbles ? Direction.DOWN : Direction.UP;
        cursor.move(direction);
        while (true) {
            if (cursor.getY() < level.getMinBuildHeight() || cursor.getY() >= level.getMaxBuildHeight()) break;

            BlockState state = level.getBlockState(cursor);

            if (!canExistIn(state)) {
                if (state.getBlock() instanceof TFCBubbleColumnBlock) {
                    level.setBlock(cursor, state.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
                }
                break;
            }
            if (state.getBlock() instanceof TFCMagmaBlock) break;
            Fluid currentFluid = state.getFluidState().getType();
            TFCBubbleColumnBlock columnBlock = BubbleFluid.of(currentFluid);
            BlockState newColumn = columnBlock.defaultBlockState().setValue(BubbleColumnBlock.DRAG_DOWN, downBubbles);
            if (!state.is(newColumn.getBlock())) {
                level.setBlock(cursor, newColumn, Block.UPDATE_ALL);
            } else break;
            cursor.move(direction);
        }
    }

    @Override // Ignore trying to understand this method, I spent way too long trying to get the whirlpool animation working
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        // Call super if its vanilla water
        if (level.getFluidState(pos).is(Fluids.WATER))
        {
            super.animateTick(state, level, pos, random);
            return;
        }
        boolean downBubbles = isDownBubbles(level, pos);
        for (int i = 0; i < 2 + random.nextInt(2); i++)
        {
            double px = pos.getX() + 0.5;
            double py = pos.getY() + 0.9;
            double pz = pos.getZ() + 0.5;

            double dy = downBubbles ? -0.02 - random.nextDouble() * 0.05 : 0.03 + random.nextDouble() * 0.02;

            if (downBubbles)
            {
                double radius = 0.4;
                long seed = pos.asLong() ^ 0x9E3779B97F4A7C15L;
                double phaseOffset = (seed % 360) / 360.0;

                double time = (level.getGameTime() * 12.0 + phaseOffset * 360.0) % 360.0;
                double angle = Math.toRadians(time);

                double dx = Math.cos(angle) * radius;
                double dz = Math.sin(angle) * radius;

                double vx = -Math.sin(angle) * 0.02;
                double vz = Math.cos(angle) * 0.02;

                level.addParticle(TFCParticles.BUBBLE.get(), px + dx, py, pz + dz, vx, dy, vz);
            }
            else
            { // Up Bubbles
                double dx = (random.nextDouble() - 0.5) * 0.02;
                double dz = (random.nextDouble() - 0.5) * 0.02;
                level.addParticle(TFCParticles.BUBBLE.get(), px, py, pz, dx, dy, dz);
            }
        }
    }

    // Determines if bubble column is allowed to exist in current block state
    public static boolean canExistIn(BlockState state)
    {
        // Bubble column already exists here
        if (state.getBlock() instanceof TFCBubbleColumnBlock) return true;
        // If block state is air or empty, check if fluid supports bubble columns
        if (FluidHelpers.isAirOrEmptyFluid(state)) {
            return BubbleFluid.LOOKUP.get(state.getFluidState().getType()) != null;
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
        
        if (facing == Direction.DOWN ||(facing == Direction.UP && !(facingState.getBlock() instanceof TFCBubbleColumnBlock) && canExistIn(facingState)))
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
