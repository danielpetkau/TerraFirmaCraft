/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.surface.builder;

import java.util.Random;

import net.dries007.tfc.common.blocks.soil.SoilBlockType;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SoilSurfaceState;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;

import static net.dries007.tfc.world.TFCChunkGenerator.*;

public class GrassyDunesSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = seed -> new GrassyDunesSurfaceBuilder(seed);

    private final Noise2D grassHeightVariationNoise;

    public GrassyDunesSurfaceBuilder(long seed)
    {
        final Random random = new Random(seed);

        grassHeightVariationNoise = new OpenSimplex2D(random.nextLong()).octaves(2).scaled(SEA_LEVEL_Y + 8, SEA_LEVEL_Y + 14).spread(0.08f);
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final double heightVariation = grassHeightVariationNoise.noise(context.pos().getX(), context.pos().getZ());
        final double trueSlope = context.getSlope();
        context.setSlope(trueSlope * (1 - context.weight()));
        SurfaceState sand = SurfaceStates.SAND;

        // TODO: Dune noise seems broken???
        if (startY > heightVariation && trueSlope < 5)
        {
            SurfaceState grass = SoilSurfaceState.buildSurfaceType(SoilBlockType.GRASS, true);
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY, grass, sand, sand, sand, sand);
        }
        else
        {
            NormalSurfaceBuilder.INSTANCE.buildSurface(context, startY, endY, sand, sand, sand, sand, sand);
        }
    }
}