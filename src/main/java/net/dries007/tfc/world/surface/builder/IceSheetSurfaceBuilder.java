/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.surface.builder;


import net.minecraft.world.level.block.state.BlockState;

import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;

public class IceSheetSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory NORMAL = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialBase(seed), false);
    public static final SurfaceBuilderFactory LAKE = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialOceanicBase(seed), false);
    public static final SurfaceBuilderFactory MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialMountainsBase(seed), false);
    public static final SurfaceBuilderFactory OCEANIC = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialOceanicBase(seed), true);
    public static final SurfaceBuilderFactory OCEANIC_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialMountainsBase(seed), true);

    private final long seed;
    private final boolean oceanic;
    private final Noise2D baseNoise;

    IceSheetSurfaceBuilder(long seed, Noise2D baseNoise, boolean oceanic)
    {
        this.baseNoise = baseNoise;
        this.oceanic = oceanic;
        this.seed = seed;
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        int surfaceDepth = -1;
        int surfaceY = 0;
        final int x = context.pos().getX();
        final int z = context.pos().getZ();
        SurfaceState snowState = SurfaceStates.SNOW;
        SurfaceState iceState = SurfaceStates.GLACIER;

        final int endHeight = (int) Math.ceil(baseNoise.noise(x, z));
        final int glacierSurfaceHeight = (int) Math.ceil(oceanic ? BiomeNoise.glacialOceanicSurface(seed).noise(x, z) : BiomeNoise.glacialSurface(seed).noise(x, z));

        if (startY < endHeight + 2 || startY > glacierSurfaceHeight + 2)
        {
            MountainSurfaceBuilder.INSTANCE.apply(seed);
        }
        else {
            for (int y = startY; y >= endHeight; --y)
            {
                final BlockState stateAt = context.getBlockState(y);
                if (stateAt.isAir())
                {
                    surfaceDepth = -1; // Reached air, reset surface depth
                }
                else if (context.isDefaultBlock(stateAt))
                {
                    // All in this if statement only occurs on the first cycle/when air resets the cycle
                    if (surfaceDepth == -1)
                    {
                        // Only include ice underwater if it is a thick layer, otherwise fall back to mountain surface builder
                        if (y < context.getSeaLevel() - 1)
                        {
                            // TODO: Figure this underwater ice stuff out
                            if (y > endHeight + 3 && y > context.getSeaLevel() - 3)
                            {
                                context.setBlockState(y, iceState);
                            }
                            else
                            {
                                MountainSurfaceBuilder.INSTANCE.apply(seed);
                            }
                        }
                        surfaceY = y; // Reached surface. Place top state and switch to subsurface layers

                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 3, -3);
                        if (surfaceDepth <= -1)
                        {
                            // skip the top layer entirely
                            context.setBlockState(y, iceState);
                        }
                        else
                        {
                            context.setBlockState(y, snowState);
                        }
                        surfaceDepth = 36;
                    }
                    else if (surfaceDepth > 0)
                    {
                        // Subsurface layers
                        surfaceDepth--;
                        context.setBlockState(y, iceState);
                    }
                }
            }
        }
    }
}
