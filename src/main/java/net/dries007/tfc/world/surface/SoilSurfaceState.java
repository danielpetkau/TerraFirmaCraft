/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.surface;

import java.util.List;
import java.util.function.Supplier;
import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.soil.SoilBlockType;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

public class SoilSurfaceState implements SurfaceState
{
    public static final Noise2D PATCH_NOISE = new OpenSimplex2D(18273952837592L).octaves(2).spread(0.04f);

    public static SurfaceState buildSurfaceType(SoilBlockType type, boolean sandy)
    {
        final SurfaceState dry = sandy ? SurfaceStates.SAND : SurfaceStates.GRAVEL;
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            SurfaceStates.SNOW,
            SurfaceStates.SNOW,
            transition(SurfaceStates.SNOW, dry),
            dry,
            transition(dry, SurfaceStates.COARSE_SANDY_LOAM),
            SurfaceStates.COARSE_SANDY_LOAM,
            transition(SurfaceStates.COARSE_SANDY_LOAM, soil(type, SoilBlockType.Variant.SANDY_LOAM)),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.SANDY_LOAM), soil(type, SoilBlockType.Variant.LOAM)),
            soil(type, SoilBlockType.Variant.LOAM),
            soil(type, SoilBlockType.Variant.LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.LOAM), soil(type, SoilBlockType.Variant.SILTY_LOAM)),
            soil(type, SoilBlockType.Variant.SILTY_LOAM),
            soil(type, SoilBlockType.Variant.SILTY_LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.SILTY_LOAM), soil(type, SoilBlockType.Variant.SILT)),
            soil(type, SoilBlockType.Variant.SILT),
            soil(type, SoilBlockType.Variant.SILT)
        );
        return type == SoilBlockType.GRASS ? new SoilSurfaceState.NeedsPostProcessing(regions) : new SoilSurfaceState(regions);
    }

    public static SurfaceState buildMidType(SoilBlockType type, boolean sandy)
    {
        final SurfaceState dry = sandy ? SurfaceStates.SAND : SurfaceStates.GRAVEL;
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            SurfaceStates.PACKED_ICE,
            blobTransition(SurfaceStates.PACKED_ICE, dry),
            dry,
            dry,
            transition(dry, SurfaceStates.COARSE_SANDY_LOAM),
            SurfaceStates.COARSE_SANDY_LOAM,
            transition(SurfaceStates.COARSE_SANDY_LOAM, soil(type, SoilBlockType.Variant.SANDY_LOAM)),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.SANDY_LOAM), soil(type, SoilBlockType.Variant.LOAM)),
            soil(type, SoilBlockType.Variant.LOAM),
            soil(type, SoilBlockType.Variant.LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.LOAM), soil(type, SoilBlockType.Variant.SILTY_LOAM)),
            soil(type, SoilBlockType.Variant.SILTY_LOAM),
            soil(type, SoilBlockType.Variant.SILTY_LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.SILTY_LOAM), soil(type, SoilBlockType.Variant.SILT)),
            soil(type, SoilBlockType.Variant.SILT),
            soil(type, SoilBlockType.Variant.SILT)
        );
        return type == SoilBlockType.GRASS ? new SoilSurfaceState.NeedsPostProcessing(regions) : new SoilSurfaceState(regions);
    }

    public static SurfaceState buildUnderType()
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            SurfaceStates.RAW,
            SurfaceStates.RAW,
            blobTransition(SurfaceStates.RAW, SurfaceStates.GRAVEL),
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL,
            SurfaceStates.GRAVEL
        );
        return new SoilSurfaceState(regions);
    }

    public static SurfaceState buildDryDirt(SoilBlockType type)
    {
        final ImmutableList<SurfaceState> regions = ImmutableList.of(
            SurfaceStates.SNOW,
            SurfaceStates.SNOW,
            transition(SurfaceStates.SNOW, soil(type, SoilBlockType.Variant.SANDY_LOAM)),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            soil(type, SoilBlockType.Variant.SANDY_LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.SANDY_LOAM), soil(type, SoilBlockType.Variant.LOAM)),
            soil(type, SoilBlockType.Variant.LOAM),
            soil(type, SoilBlockType.Variant.LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.LOAM), soil(type, SoilBlockType.Variant.SILTY_LOAM)),
            soil(type, SoilBlockType.Variant.SILTY_LOAM),
            soil(type, SoilBlockType.Variant.SILTY_LOAM),
            blobTransition(soil(type, SoilBlockType.Variant.SILTY_LOAM), soil(type, SoilBlockType.Variant.SILT)),
            soil(type, SoilBlockType.Variant.SILT),
            soil(type, SoilBlockType.Variant.SILT)
        );
        return new SoilSurfaceState(regions);
    }

    public static SurfaceState soil(SoilBlockType type, SoilBlockType.Variant variant)
    {
        final Supplier<Block> block = TFCBlocks.SOIL.get(type).get(variant);
        return context -> block.get().defaultBlockState();
    }

    private static SurfaceState transition(SurfaceState first, SurfaceState second)
    {
        return context -> (Helpers.hash(729375982L, context.pos()) & 127) > 63 ?
                first.getState(context) : second.getState(context);
    }

    private static SurfaceState blobTransition(SurfaceState first, SurfaceState second)
    {
        return context -> {
            final BlockPos pos = context.pos();
            final double noise = PATCH_NOISE.noise(pos.getX(), pos.getZ());
            return noise > 0 ? first.getState(context) : second.getState(context);
        };
    }

    private final List<SurfaceState> regions;

    private SoilSurfaceState(List<SurfaceState> regions)
    {
        this.regions = regions;
    }

    @Override
    public BlockState getState(SurfaceBuilderContext context)
    {
        // Bias a little towards sand regions
        // Without: pure sand < 55mm, mixed sand < 110mm. With: pure sand < 73mm, mixed sand < 126mm
        // TODO: Above is with 9, we now have 18, and gravel (old: sand)

        final float rainfall = context.groundWater();
        final float temperature = Helpers.adjustAverageTemperatureByElevation(context.pos().getY(), context.averageTemperature(), context.getSeaLevel()) ;

        //TODO: check this: Rain-controlled surface: <65 pure gravel, <94 mixed gravel/dirt, <124 dirt, <154 mixed dirt/grass
        final int rainIndex = (int) Mth.clampedMap(rainfall, 35, 450, 3, regions.size() - 0.01f);

        // TODO: Temperature-controlled surface:
        // -17c = Koppen EF/ET Border
        // -12c = Koppen ET Border
        final int tempIndex = (int) Mth.clampedMap(temperature, -19, -4, 0, regions.size() - 0.01f);

        return regions.get(Math.min(rainIndex, tempIndex)).getState(context);
    }

    static class NeedsPostProcessing extends SoilSurfaceState
    {
        private NeedsPostProcessing(List<SurfaceState> regions)
        {
            super(regions);
        }

        @Override
        public void setState(SurfaceBuilderContext context)
        {
            context.chunk().setBlockState(context.pos(), getState(context), false);
            context.chunk().markPosForPostprocessing(context.pos());
        }
    }
}
