/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.region;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;

import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Noise2D;

public enum AddHotspots implements RegionTask
{
    INSTANCE;

    @Override
    public void apply(RegionGenerator.Context context)
    {
        final Region region = context.region;
        final long seed = context.generator().levelSeed();

        final Noise2D hotspotAge = BiomeNoise.hotSpotAge(seed).spread(128);
        final Noise2D hotspotIntensity = BiomeNoise.hotSpotIntensity(seed).spread(128); //TODO: Remove the point.intensity, this noise map can cover us

        final IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

        // If a location reaches a value of at least 0.55 (large enough to form a crater) a hot spot is placed
        for (final var point : region.points())
        {
            double val = hotspotIntensity.noise(shift(point.x), shift(point.z));
            if (val > 0.55)
            {
                point.hotSpotAge = (byte) (int) hotspotAge.noise(shift(point.x), shift(point.z));

                queue.enqueue(point.index);
            }
        }

        // From the above crater locations, the hotspots are extended outwards
        while (!queue.isEmpty())
        {
            final int index = queue.dequeueInt();
            for (int dx = -1; dx <= 1; dx++)
            {
                for (int dz = -1; dz <= 1; dz++)
                {
                    final Region.Point next = region.atOffset(index, dx, dz);
                    if (next != null)
                    {
                        if (next.hotSpotAge == 0)
                        {
                            if (hotspotIntensity.noise(shift(next.x), shift(next.z)) > 0.15)
                            {
                                queue.enqueue(next.index);
                                next.hotSpotAge = (byte) (int) hotspotAge.noise(shift(next.x), shift(next.z));
                            }
                            // This guarantees the 8 points around any caldera are filled in to keep biome blending away from the crater
                            else if (hotspotIntensity.noise(shift(next.x) - dx, shift(next.z) - dz ) > 0.55)
                            {
                                next.hotSpotAge = (byte) (int) hotspotAge.noise(shift(next.x) - dx, shift(next.z) - dz );
                            }
                        }
                    }
                }
            }
        }

    }

    // Shifts the location by 0.5 towards the origin.
    // Use this when sampling noise from regional coordinates to sample the center of the region point
    public double shift(int point)
    {
        if (point == 0) return 0;
        return point - (0.5 * Math.signum(point));
    }
}