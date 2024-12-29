/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.surface.builder;

import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;

public class MountainSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory NORMAL = seed -> new MountainSurfaceBuilder(seed, (x, z)-> 0);
    public static final SurfaceBuilderFactory OLD = seed -> new MountainSurfaceBuilder(seed, (x, z)-> 0);
    public static final SurfaceBuilderFactory OCEANIC = seed -> new MountainSurfaceBuilder(seed, (x, z)-> 0);
    public static final SurfaceBuilderFactory GLACIATED = seed -> new MountainSurfaceBuilder(seed, (x, z)-> 0);

    private final Noise2D talusNoise;

    public MountainSurfaceBuilder(long seed, Noise2D talusNoise)
    {
        this.talusNoise = talusNoise;
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final NormalSurfaceBuilder surfaceBuilder = NormalSurfaceBuilder.ROCKY;
        // TODO: ? final double heightNoise = this.heightNoise.noise(context.pos().getX(), context.pos().getZ()) * 4f + startY;
        surfaceBuilder.buildSurface(context, startY, endY);

        //TODO: Have something to make mountains a bit rockier?
//        if (heightNoise > cutOffHeight)
//        {
//            final double surfaceMaterialValue = surfaceMaterialNoise.noise(context.pos().getX(), context.pos().getZ()) + 0.1f * context.random().nextFloat() - 0.05f;
//            if (surfaceMaterialValue > 0.3f)
//            {
//                surfaceBuilder.buildSurface(context, startY, endY, SurfaceStates.COBBLE, SurfaceStates.COBBLE, SurfaceStates.RAW);
//            }
//            else if (surfaceMaterialValue < -0.3f)
//            {
//                surfaceBuilder.buildSurface(context, startY, endY, SurfaceStates.GRAVEL, SurfaceStates.GRAVEL, SurfaceStates.RAW);
//            }
//            else
//            {
//                surfaceBuilder.buildSurface(context, startY, endY, SurfaceStates.RAW, SurfaceStates.RAW, SurfaceStates.RAW);
//            }
//        }
//        else
//        {
//            surfaceBuilder.buildSurface(context, startY, endY, SurfaceStates.GRASS, SurfaceStates.DIRT, SurfaceStates.GRAVEL);
//        }
    }
}