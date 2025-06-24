/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.surface.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.surface.SurfaceBuilderContext;
import net.dries007.tfc.world.surface.SurfaceState;
import net.dries007.tfc.world.surface.SurfaceStates;

import static net.dries007.tfc.world.surface.SurfaceStates.*;

public class SeaCliffSurfaceBuilder implements SurfaceBuilder
{

    public static final SurfaceBuilderFactory NORMAL = seed -> new SeaCliffSurfaceBuilder(seed, NormalSurfaceBuilder.ROCKY, 3);
    public static final SurfaceBuilderFactory UNDERCUT = seed -> new SeaCliffSurfaceBuilder(seed, NormalSurfaceBuilder.ROCKY, 6);

    private final Seed seed;
    private final Noise2D variantNoise;
    private final Noise2D sandVariantNoise;
    private final NormalSurfaceBuilder landBuilder;
    private final int sandHeightOffset;

    public SeaCliffSurfaceBuilder(Seed seed, NormalSurfaceBuilder landBuilder, int sandHeightOffset)
    {
        this.seed = seed;
        this.landBuilder = landBuilder;
        this.sandHeightOffset = sandHeightOffset;
        this.variantNoise = new OpenSimplex2D(seed.seed()).octaves(5).spread(0.0001f).abs();
        this.sandVariantNoise = new OpenSimplex2D(seed.seed()).octaves(5).spread(0.0003f).abs();
    }

    @Override
    public void buildSurface(SurfaceBuilderContext context, int startY, int endY)
    {
        final BlockPos pos = context.pos();
        final int x = pos.getX();
        final int z = pos.getZ();
        final float gravelBias = context.averageTemperature() / 45;
        final float variantNoiseValue = (float) variantNoise.noise(x, z);
        final float gravelSandValue = variantNoiseValue + gravelBias;
        final int sandHeight = (int) (BiomeNoise.shoreTideLevelNoise(seed).noise(x, z)) + sandHeightOffset;

        // Increase the slope (decrease soil thickness) in the higher elevation areas of the biome
        // TODO: I think I'd rather do this by just detecting the edges from the biome noise map if possible
        context.setSlope(context.getSlope() + Mth.clampedMap(startY, sandHeight - 3, sandHeight + 7, 0, 1.0));

        final SurfaceBuilder beachBuilder;
        final SurfaceState beachTopState;

        if (gravelSandValue > 0)
        {
            beachBuilder = ShoreSurfaceBuilder.SANDY.apply(seed);
            beachTopState = RARE_SHORE_SAND;
        }
        else
        {
            beachBuilder = ShoreSurfaceBuilder.GRAVELLY.apply(seed);
            beachTopState = GRAVEL;
        }

        if (startY < sandHeight)
        {
            beachBuilder.buildSurface(context, startY, endY);
        }
        else if (gravelSandValue > 0)
        {
            final float sandVariantNoiseValue = (float) sandVariantNoise.noise(x, z);

            // The following shall match the sand selection in ShoreSurfaceBuilder.java
            // Rare sands are the "typical" state
            if (sandVariantNoiseValue > 0.65f)
            {
                landBuilder.buildSurface(context, startY, endY, RARE_SHORE_SAND, RARE_SHORE_SAND, RARE_SHORE_SANDSTONE, sandHeight, true);
            }
            else
            {
                final SurfaceState top;
                final SurfaceState under;
                if (sandVariantNoiseValue > 0.4)
                {
                    top = SurfaceStates.RED_SAND;
                    under = SurfaceStates.RED_SANDSTONE;
                }
                else if (sandVariantNoiseValue > 0.22)
                {
                    top = SurfaceStates.BROWN_SAND;
                    under = SurfaceStates.BROWN_SANDSTONE;
                }
                else
                {
                    top = SurfaceStates.YELLOW_SAND;
                    under = SurfaceStates.YELLOW_SANDSTONE;
                }
                landBuilder.buildSurface(context, startY, endY, top, top, under, sandHeight, true);
            }
        }
        else
        {
            landBuilder.buildSurface(context, startY, endY, beachTopState, beachTopState, SurfaceStates.RAW, sandHeight, true);
        }
    }
}
