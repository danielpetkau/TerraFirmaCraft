/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.surface.builder;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;

public class ShoreSurfaceBuilder implements SurfaceBuilder
{
    public static final SurfaceBuilderFactory INSTANCE = ShoreSurfaceBuilder::new;

    private final Noise2D variantNoise;

    protected ShoreSurfaceBuilder(Seed seed)
    {
        this.variantNoise = new OpenSimplex2D(seed.seed()).octaves(5).spread(0.0003f).abs();
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        // Adjust slope, shores should have high relief carving where they intersect with the landmass
        final double slope = context.getSlope();
        context.setSlope(slope * (slope + 0.2));

        final float variantNoiseValue = (float) variantNoise.noise(context.pos().getX(), context.pos().getZ());
        if (variantNoiseValue > 0.65f)
        {
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, SurfaceStates.RARE_SHORE_SAND, SurfaceStates.RARE_SHORE_SAND, SurfaceStates.RARE_SHORE_SANDSTONE);
        }
        else
        {
            final SurfaceState top;
            final SurfaceState sandstone;
            if (variantNoiseValue > 0.4)
            {
                top = SurfaceStates.RED_SAND;
                sandstone = SurfaceStates.RED_SANDSTONE;
            }
            else if (variantNoiseValue > 0.22)
            {
                top = SurfaceStates.BROWN_SAND;
                sandstone = SurfaceStates.BROWN_SANDSTONE;
            }
            else
            {
                top = SurfaceStates.YELLOW_SAND;
                sandstone = SurfaceStates.YELLOW_SANDSTONE;
            }
            NormalSurfaceBuilder.ROCKY.buildSurface(context, startY, endY, top, top, sandstone);
        }
    }
}