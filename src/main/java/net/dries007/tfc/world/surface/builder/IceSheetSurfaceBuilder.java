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
    public static final SurfaceBuilderFactory NORMAL = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialBase(seed), BiomeNoise.glacialIceSurface(seed), true, false);
    public static final SurfaceBuilderFactory EDGE = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialBase(seed).addConstant(1.6), BiomeNoise.glacialIceSurface(seed), true, false);
    public static final SurfaceBuilderFactory LAKE = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialOceanicBase(seed), BiomeNoise.glacialIceSurface(seed), false, false);
    public static final SurfaceBuilderFactory ICE_SHEET_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialCirques(seed).addConstant(39), BiomeNoise.glacialMontaneIceSurface(seed).max(BiomeNoise.glacialCirquesIceSurface(seed).addConstant(39)), false, true);
    // TODO: rework into versions with soils
    public static final SurfaceBuilderFactory GLACIATED_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialCirques(seed).addConstant(39), BiomeNoise.glacialCirquesIceSurface(seed).addConstant(39), false, true);
    public static final SurfaceBuilderFactory OCEANIC = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialOceanicBase(seed), BiomeNoise.glacialOceanicIceSurface(seed), false, false);
    public static final SurfaceBuilderFactory ICE_SHEET_OCEANIC_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialCirques(seed), BiomeNoise.glacialIceSurface(seed).max(BiomeNoise.glacialOceanicCirquesIceSurface(seed)), false, true); // TODO: maybe change back to the oceanic ice sheet surface
    public static final SurfaceBuilderFactory GLACIATED_OCEANIC_MOUNTAINS = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.glacialCirques(seed), BiomeNoise.glacialOceanicCirquesIceSurface(seed), false, true);
    // TODO: These need special surface builders to add the basalt, if we keep moraines those should be basaltic too
    public static final SurfaceBuilderFactory ACTIVE_SHIELD_VOLCANO = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.activeShieldVolcano(seed, BiomeNoise.activeHotSpots(seed)), BiomeNoise.glacialIceSurface(seed).max(BiomeNoise.shieldVolcanoIceSheetSurface(seed, BiomeNoise.hotSpotIntensity(seed))), true, true);
    public static final SurfaceBuilderFactory DORMANT_SHIELD_VOLCANO = seed -> new IceSheetSurfaceBuilder(seed, BiomeNoise.dormantShieldVolcano(seed, BiomeNoise.hotSpotIntensity(seed)), BiomeNoise.glacialIceSurface(seed).max(BiomeNoise.shieldVolcanoIceSheetSurface(seed, BiomeNoise.hotSpotIntensity(seed))), true, true);


    private final long seed;
    private final Noise2D iceSurfaceNoise;
    private final Noise2D baseNoise;
    private final boolean hasMoraines;
    private final boolean hasStonyPeaks;

    IceSheetSurfaceBuilder(long seed, Noise2D baseNoise, Noise2D iceSurfaceNoise, boolean hasMoraines, boolean hasStonyPeaks)
    {
        this.baseNoise = baseNoise;
        this.iceSurfaceNoise = iceSurfaceNoise;
        this.seed = seed;
        this.hasMoraines = hasMoraines;
        this.hasStonyPeaks = hasStonyPeaks;
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
        SurfaceState moraineState = SurfaceStates.MORAINE;

        final int glacierBaseHeight = (int) Math.ceil(baseNoise.noise(x, z));
        final int glacierSurfaceHeight = (int) Math.ceil(iceSurfaceNoise.noise(x, z));

        int iceDepth;
        // TODO: Rivers
        if (hasMoraines && context.baseGroundwater() <= 20f)
        {
            final double moraineCrestHeight = Math.min((0.5 * (glacierSurfaceHeight + glacierBaseHeight)), glacierBaseHeight + 18);
            iceDepth = Math.max((int) ((startY - moraineCrestHeight) * 2), 0);
        }
        else
        {
            iceDepth = 36;
        }

        if ((hasStonyPeaks && startY > glacierSurfaceHeight + 3) || startY < glacierBaseHeight)
        {
            MountainSurfaceBuilder.COLD.apply(seed).buildSurface(context, startY, endY);
        }
        else {
            for (int y = startY; y >= glacierBaseHeight; --y)
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
                        surfaceY = y; // Reached surface. Place top state and switch to subsurface layers

                        surfaceDepth = context.calculateAltitudeSlopeSurfaceDepth(surfaceY, 3, -3);
                        if (surfaceDepth <= -1)
                        {
                            // skip the top layer entirely
                            context.setBlockState(y, iceState);
                        }
                        else if (iceDepth == 0 || y <= context.getSeaLevel())
                        {
                            context.setBlockState(y, moraineState);
                        }
                        else
                        {
                            context.setBlockState(y, snowState);
                        }
                        surfaceDepth = 36;
                    }
                    else if (iceDepth > 0)
                    {
                        // Subsurface layers
                        iceDepth--;
                        surfaceDepth--;
                        context.setBlockState(y, iceState);
                    }
                    else if (surfaceDepth > 0)
                    {
                        // Subsurface layers
                        surfaceDepth--;
                        context.setBlockState(y, moraineState);
                    }
                }
            }
        }
    }
}
