/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.shore;

import net.minecraft.util.Mth;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeNoise;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.Noise3D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

import static net.dries007.tfc.world.TFCChunkGenerator.*;

public final class ShoreNoise
{
    // Typical monoslope beach
    public static ShoreNoiseSampler sandyBeach(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            double sandHeight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.sandHeight = ShoreNoise.simpleBeach(seed, x, z, heightIn, landWeight, oceanWeight);

                return sandHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight) return 0;

                return 0.7;
            }
        };
    }

    // TODO:
    // Cliffs at a distance from the shore with dunes below
    public static ShoreNoiseSampler setbackCliffs(Seed seed)
    {
        return new ShoreNoiseSampler()
        {

            final OpenSimplex2D warpNoise = new OpenSimplex2D(seed.seed()).scaled(-10, 10).spread(0.06);
            final Noise2D duneNoise = new OpenSimplex2D(seed.seed()).spread(0.04).abs().scaled(-2, 6).warped(warpNoise);
            final Noise3D cliffNoise = BiomeNoise.cliffNoise(seed);

            final double cliffBaseWeight = 0.25;
            final double cliffTopWeight = 0.45;

            double duneHeight;
            double landWeight;
            int x;
            int z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.landWeight = landWeight;
                this.x = x;
                this.z = z;

                final double sandHeight = ShoreNoise.simpleBeach(seed, x, z, heightIn, landWeight, oceanWeight);
                final double fullDuneHeight = duneNoise.noise(x, z) + sandHeight;

                if (landWeight >= cliffBaseWeight)
                {
                    this.duneHeight = fullDuneHeight;
                    return Mth.clampedMap(landWeight, cliffBaseWeight, cliffTopWeight, duneHeight, heightIn);
                }
                else if (oceanWeight == 0)
                {
                    this.duneHeight = fullDuneHeight;
                }
                else
                {
                    this.duneHeight = Mth.clampedMap(oceanWeight, 0, 0.4, fullDuneHeight, sandHeight);
                }

                return fullDuneHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= duneHeight) return 0;
                if (landWeight > cliffBaseWeight)
                {
                    // TODO: Better system
                    return cliffNoise.noise(x, yIn, z) - landWeight;
                }
                return Mth.clampedMap(yIn, duneHeight, duneHeight + 10, 0, 3);
            }
        };
    }

    // Small sea dunes
    public static ShoreNoiseSampler dunes(Seed seed)
    {
        return new ShoreNoiseSampler()
        {

            final OpenSimplex2D warpNoise = new OpenSimplex2D(seed.seed()).scaled(-10, 10).spread(0.05);
            final Noise2D duneNoise = new OpenSimplex2D(seed.seed()).spread(0.03).abs().scaled(-2, 8).warped(warpNoise);

            double duneHeight;
            double sandHeight;
            double oceanWeight;
            double thisWeight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.sandHeight = ShoreNoise.simpleBeach(seed, x, z, heightIn, landWeight, oceanWeight);
                this.oceanWeight = oceanWeight;
                this.thisWeight = thisWeight;
                final double tideLevelAtOcean = BiomeNoise.shoreTideLevelNoise(seed).noise(x, z) - 4;

                final double fullDuneHeight = duneNoise.noise(x, z) + sandHeight;
                if (oceanWeight > 0)
                {
                    this.duneHeight = Mth.clampedMap(oceanWeight, 0, 0.25, fullDuneHeight,tideLevelAtOcean);
                }
                else
                {
                    this.duneHeight = fullDuneHeight;
                }
                this.duneHeight = Mth.clampedMap(thisWeight, 0.5, 1, sandHeight, duneHeight);


                return duneHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= duneHeight) return 0;

                return 0.7;
            }
        };
    }

    // TODO:
    // Complex rocks and tidepools
    public static ShoreNoiseSampler rockyShores(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            double sandHeight;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.sandHeight = ShoreNoise.simpleBeach(seed, x, z, heightIn, landWeight, oceanWeight);

                return sandHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight) return 0;

                return 0.7;
            }
        };
    }

    // Typical monoslope beaches interspersed with rocky outcrops
    // TODO:
    public static ShoreNoiseSampler embayments(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                return heightIn;
            }
        };
    }

    // Mirrors functionality of 1.20 coasts
    public static ShoreNoiseSampler classic(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            final Noise2D shoreNoise = new OpenSimplex2D(seed.seed() + 8719234132L).octaves(2).spread(0.003f).scaled(-0.1, 1.1);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                // First, calculate cliff "influence" factor (between 0 = no cliffs, 1.0 = full cliffs)
                // This is computed from a global influence noise, plus a factor from the initial height - higher areas have larger cliff influence
                final int cliffHeightAdjustment = biome.getShoreBaseHeight();

                final double cliffInfluence = Mth.clamp(
                    shoreNoise.noise(x, z) + Mth.map(heightIn, cliffHeightAdjustment, cliffHeightAdjustment + 20, 0, 0.6),
                    0.0, 1.0
                );
                final double adjustedCliffInfluence = 1.0 - (1.0 - cliffInfluence) * (1.0 - cliffInfluence);

                // Then, calculate the re-weighted shore and normal biome height
                final double x2 = Mth.lerp(adjustedCliffInfluence, 0.8, 0.515);
                final double y2 = 1.15 - 0.3 * x2;

                // Adjust shore weight based on a piecewise function that creates a sharper cliff, then a smoother flatter area
                final double adjustedShoreWeight = shoreWeight < x2
                    ? Mth.map(shoreWeight, 0.5, x2, 0.5, y2) // Cliff from [0.5, x2] -> rapidly increase shore weight
                    : Mth.map(shoreWeight, x2, 1.0, y2, 1.0); // From [x2, 1.0], interpolate high shore weight, creates flatter area

                final double normalWeight = 1.0 - shoreWeight;
                final double adjustedNormalWeight = 1.0 - adjustedShoreWeight;

                // Calculate the adjusted height, using this re-weighting
                // Only apply if we are above the cliff base height (sea level by default), by taking a max here
                final double adjustedHeight = Math.max(
                    (adjustedShoreWeight / shoreWeight) * shoreHeight + (adjustedNormalWeight / normalWeight) * normalHeight,
                    cliffHeightAdjustment
                );

                if (adjustedHeight < heightIn)
                {
                    heightIn = adjustedHeight;
                }
                return heightIn;
            }
        };
    }

    // Sea Stacks and arches
    public static ShoreNoiseSampler seaStacks(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            final double minStackDensity = 0.85;
            final double noiseScale = 3;

            final Cellular2D cellularNoise = new Cellular2D(seed.seed() + 323L).spread(0.05);

            final Noise2D seaStackDistributionNoise = new OpenSimplex2D(seed.seed() + 5424L).octaves(2).map(y -> 1 - Math.abs(y)).spread(0.015);
            final Noise2D f2MinusF1Noise = (x, z) -> {
                Cellular2D.Cell cell = cellularNoise.cell(x, z);
                final double f1 = cell.f1();
                final double f2 = cell.f2();
                return (f1 > 0 ? (f2 - f1) : 1);
            };
            final Noise2D f1Noise = (x, z) -> {
                Cellular2D.Cell cell = cellularNoise.cell(x, z);
                final double centerX = cell.x();
                final double centerZ = cell.y();
                final double stackDensity = seaStackDistributionNoise.noise(centerX, centerZ);
                if (stackDensity < minStackDensity) return noiseScale;
                return cell.f1();
            };

            final Noise3D cliffNoise = BiomeNoise.cliffNoise(seed);

            private double stackNoiseValue;
            private double sandHeight;
            private double landWeight;

            private int x, z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.landWeight = landWeight;

                this.sandHeight = ShoreNoise.simpleBeach(seed, x, z, heightIn, landWeight, oceanWeight);

                final double f2MinusF1 = f2MinusF1Noise.noise(x, z);

                final double outputMin = Mth.clampedMap(seaStackDistributionNoise.noise(x, z), 0.2, 0.6, 3, 1);
                this.stackNoiseValue = f1Noise.noise(x, z) * Mth.clampedMap(Math.abs(f2MinusF1), 0, 0.25, outputMin, 1);

                return Mth.clampedMap(oceanWeight, 0, 0.5, heightIn, shoreHeight / shoreWeight) ;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight) return 0;

                final double overhangHeight = SEA_LEVEL_Y + 14;
                final double stackBaseHeight = SEA_LEVEL_Y + 1;

                double y = yIn - stackBaseHeight;

                final double cliffNoiseModifier = 0.04 * Math.abs(cliffNoise.noise(x, y, z));
                final double stackMinWidth = 0.06 + cliffNoiseModifier;
                final double stackMaxWidth = 0.18 + cliffNoiseModifier;
                final double cliffBorderTopWeight = 0.32 + cliffNoiseModifier;
                final double cliffBorderBaseWeight = 0.36 + cliffNoiseModifier;

                final double height = overhangHeight - stackBaseHeight;

                final double stackWidth = widthFunction(false, y, height, stackMinWidth, stackMaxWidth);
                final double stackOutput = Math.clamp((stackNoiseValue - stackWidth) * 10, 0, 1) * Mth.clampedMap(yIn, stackBaseHeight, overhangHeight, noiseScale, 0.75);
                // landWeight where cliff top edges form
                if (landWeight >= cliffBorderTopWeight)
                {
                    final double cliffBorderWeight = widthFunction(true, y, height, cliffBorderBaseWeight, cliffBorderTopWeight);
                    return Math.min(Math.clamp((cliffBorderWeight - landWeight) * 10, 0, 1) * noiseScale, stackOutput);
                }
                else
                {
                    return stackOutput;
                }
            }
        };
    }

    // Terraced coastline, upper
    public static ShoreNoiseSampler upperTerrace(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            final double noiseScale = 3;

            final Noise3D cliffNoise = BiomeNoise.cliffNoise(seed);
            final Noise2D lowerTerraceNoise = BiomeNoise.lowerTerraceNoise(seed);
            final Noise2D upperTerraceNoise = BiomeNoise.upperTerraceNoise(seed);

            private double landWeight;

            private int x, z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.landWeight = landWeight;

                return upperTerraceNoise.noise(x, z);
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                final double lowerHeight = lowerTerraceNoise.noise(x, z);
                if (yIn <= lowerHeight) return 0;

                final double overhangHeight = SEA_LEVEL_Y + 25;
                final double cliffBaseHeight = SEA_LEVEL_Y + 11;

                final double y = yIn - cliffBaseHeight;
                final double cliffNoiseModifier = 0.04 * Math.abs(cliffNoise.noise(x, y, z));
                final double cliffBorderTopWeight = 0.32 + cliffNoiseModifier;
                final double cliffBorderBaseWeight = 0.36 + cliffNoiseModifier;

                final double height = overhangHeight - cliffBaseHeight;

                // landWeight where cliff top edges form
                if (landWeight >= cliffBorderTopWeight)
                {
                    final double cliffBorderWeight = widthFunction(true, y, height, cliffBorderBaseWeight, cliffBorderTopWeight);
                    return Math.clamp((cliffBorderWeight - landWeight) * 10, 0, 1) * noiseScale;
                }
                else
                {
                    return Mth.clampedMap(yIn, lowerHeight, lowerHeight + 6, 0, noiseScale);
                }
            }
        };
    }

    // Terraced coastline, lower
    public static ShoreNoiseSampler lowerTerrace(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            final double noiseScale = 1;

            final Noise3D cliffNoise = BiomeNoise.cliffNoise(seed);
            final Noise2D lowerTerraceNoise = BiomeNoise.lowerTerraceNoise(seed);

            private double oceanWeight;
            private double thisWeight;
            private double lowerTerraceHeight;
            private double sandHeight;

            private int x, z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.oceanWeight = oceanWeight;
                // TODO: Using thisWeight vs oceanWeight doesn't appear to help, probably remove thisWeight in the long run, but keep for now
                this.thisWeight = thisWeight;
                this.lowerTerraceHeight = lowerTerraceNoise.noise(x, z);
                this.sandHeight = ShoreNoise.simpleBeach(seed, x, z, heightIn, landWeight, oceanWeight);

                return lowerTerraceHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight) return 0;

                final double overhangHeight = SEA_LEVEL_Y + 11;
                final double cliffBaseHeight = SEA_LEVEL_Y - 2;

                double y = yIn - cliffBaseHeight;

                final double cliffNoiseModifier = 0.12 * Math.abs(cliffNoise.noise(x, y, z));
                final double cliffBorderTopWeight = 0.26 - cliffNoiseModifier;
                final double cliffBorderBaseWeight = 0.22 - cliffNoiseModifier;

                final double height = overhangHeight - cliffBaseHeight;

                // landWeight where cliff top edges form
                if (oceanWeight >= cliffBorderBaseWeight)
                {
                    final double cliffBorderWeight = widthFunction(false, y, height, cliffBorderBaseWeight, cliffBorderTopWeight);
                    return Math.clamp((oceanWeight - cliffBorderWeight) * 20, 0, 1) * noiseScale;
                }
                else
                {
                    return 0;
                }
            }

        };
    }

    // Helper functions

    private static double widthFunction(boolean inverted, double y, double height, double baseWidth, double topWidth)
    {
        final double curve = baseWidth + (y * y / (height * height)) * (topWidth - baseWidth);
        if (inverted) return Math.max(curve, topWidth);
        return Math.min(curve, topWidth);
    }

    private static double simpleBeach(Seed seed, int x, int z, double heightIn, double landWeight, double oceanWeight)
    {
        final double tideLevel = BiomeNoise.shoreTideLevelNoise(seed).noise(x, z);

        // Basic heights that the shore height will approach at edges with other biomes
        final double simpleShoreLandHeight = tideLevel + 3;
        final double simpleShoreSelfHeight = tideLevel - 1;
        final double simpleShoreOceanHeight = tideLevel - 4;

        if (landWeight >= 0.2)
        {
            return Mth.clampedMap(landWeight, 0.4, 0.2, Math.min(heightIn, simpleShoreLandHeight), simpleShoreSelfHeight);
        }
        else
        {
            return Mth.clampedMap(oceanWeight, 0.05, 0.5, simpleShoreSelfHeight, simpleShoreOceanHeight);
        }
    }
}
