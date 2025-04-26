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
import net.dries007.tfc.world.noise.OpenSimplex3D;

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
                this.sandHeight = ShoreNoise.simpleBeach(seed.seed(), x, z, heightIn, landWeight, oceanWeight);

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
    public static ShoreNoiseSampler embayments(Seed seed)
    {
        final Cellular2D cellularNoise = new Cellular2D(seed.seed() + 34133L).spread(0.01);
        final Noise2D embaymentNoise = (x, z) -> {
            Cellular2D.Cell cell = cellularNoise.cell(x, z);

            final int dx = cell.c2x() - cell.cx();
            final int dz = cell.c2y() - cell.cy();

            //TODO: Actually tie angle to something
            if (Math.abs(x) > Math.abs(dz))
            {
                return 0;
            }

            final double f1 = cell.f1();
            final double f2 = cell.f2();
            final double f2f1 = (f1 > 0 ? (f2 - f1) : 1);
// TODO: Overcomplicated
            double y = 1 - (f2f1);
            y = Mth.clampedMap(y, 0.7, 0.8, 0, 1);
            return y;
        };

        return new ShoreNoiseSampler()
        {
            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                heightIn = heightIn * 1.2;
                final double simpleShoreHeight = biome.getShoreBaseHeight();
                final double simpleLandHeight = simpleShoreHeight + 10;
                final double simpleOceanHeight = simpleShoreHeight - 15;

                final double slopeHeight = landWeight * simpleLandHeight + (oceanWeight * simpleOceanHeight + shoreWeight * simpleShoreHeight);
                final double protrusionHeight = embaymentNoise.noise(x, z) * simpleLandHeight;
                final double adjustedHeight = Math.max(slopeHeight, protrusionHeight);

                if (adjustedHeight < heightIn)
                {
                    heightIn = adjustedHeight;
                }
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

            final Noise3D cliffNoise = new OpenSimplex3D(seed.next()).octaves(2).spread(0.1f);

            private double stackNoise;
            private double sandHeight;

            private double landWeight;

            private int x, z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.landWeight = landWeight;

                this.sandHeight = ShoreNoise.simpleBeach(seed.seed(), x, z, heightIn, landWeight, oceanWeight);

                final double f2MinusF1 = f2MinusF1Noise.noise(x, z);

                final double outputMin = Mth.clampedMap(seaStackDistributionNoise.noise(x, z), 0.2, 0.6, 3, 1);
                this.stackNoise = f1Noise.noise(x, z) * Mth.clampedMap(Math.abs(f2MinusF1), 0, 0.25, outputMin, 1);

                return heightIn;
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
                final double stackOutput = Math.clamp((stackNoise - stackWidth) * 10, 0, 1) * noiseScale;
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

            private double widthFunction(boolean inverted, double y, double height, double baseWidth, double topWidth)
            {
                final double curve = baseWidth + (y * y / (height * height)) * (topWidth - baseWidth);
                if (inverted) return Math.max(curve, topWidth);
                return Math.min(curve, topWidth);
            }

        };
    }

    // Terraced coastline, upper
    // TODO: consider adding stacks?
    public static ShoreNoiseSampler upperTerrace(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            final double noiseScale = 3;

            final Noise3D cliffNoise = new OpenSimplex3D(seed.next()).octaves(2).spread(0.1f);
            final Noise2D lowerTerraceNoise = BiomeNoise.hills(seed.next(), 7, 15);

            private double landWeight;

            private int x, z;

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, double oceanWeight, double landWeight, double shoreWeight, double thisWeight, BiomeExtension biome, double shoreHeight, double normalHeight)
            {
                this.x = x;
                this.z = z;
                this.landWeight = landWeight;

                return heightIn;
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
                    return noiseScale;
                }
            }

            private double widthFunction(boolean inverted, double y, double height, double baseWidth, double topWidth)
            {
                final double curve = baseWidth + (y * y / (height * height)) * (topWidth - baseWidth);
                if (inverted) return Math.max(curve, topWidth);
                return Math.min(curve, topWidth);
            }

        };
    }

    // Terraced coastline, lower
    // TODO: consider adding stacks?
    public static ShoreNoiseSampler lowerTerrace(Seed seed)
    {
        return new ShoreNoiseSampler()
        {
            final double noiseScale = 1;

            final Noise3D cliffNoise = new OpenSimplex3D(seed.next()).octaves(2).spread(0.1f);
            final Noise2D lowerTerraceNoise = BiomeNoise.hills(seed.seed(), 7, 15);

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
                // TODO: Using thisWeight vs oceanWeight doesn't appear to help
                this.thisWeight = thisWeight;
                this.lowerTerraceHeight = lowerTerraceNoise.noise(x, z);
                this.sandHeight = ShoreNoise.simpleBeach(seed.seed(), x, z, heightIn, landWeight, oceanWeight);

                // TODO: Pick up here. Having trouble blending terraces to stacks
                return Mth.clampedMap(landWeight, 0.3, 0, lowerTerraceHeight, heightIn);
//                return lowerTerraceHeight;
            }

            @Override
            public double noise(int yIn, double noiseIn)
            {
                if (yIn <= sandHeight) return 0;

                final double overhangHeight = SEA_LEVEL_Y + 11;
                final double cliffBaseHeight = SEA_LEVEL_Y - 2;

                double y = yIn - cliffBaseHeight;

                final double cliffNoiseModifier = 0.12 * Math.abs(cliffNoise.noise(x, y, z));
                final double cliffBorderTopWeight = 0.32 - cliffNoiseModifier;
                final double cliffBorderBaseWeight = 0.36 - cliffNoiseModifier;

                final double height = overhangHeight - cliffBaseHeight;

                // landWeight where cliff top edges form
                if (oceanWeight <= cliffBorderTopWeight)
                {
                    final double cliffBorderWeight = widthFunction(true, y, height, cliffBorderBaseWeight, cliffBorderTopWeight);
                    return Math.clamp((cliffBorderWeight - thisWeight) * 10, 0, 1) * noiseScale;
                }
                else
                {
                    return noiseScale;
                }
            }

            // TODO: This is duplicated in a bunch of places right now
            private double widthFunction(boolean inverted, double y, double height, double baseWidth, double topWidth)
            {
                final double curve = baseWidth + (y * y / (height * height)) * (topWidth - baseWidth);
                if (inverted) return Math.max(curve, topWidth);
                return Math.min(curve, topWidth);
            }

        };
    }

    private static double simpleBeach(long seed, int x, int z, double heightIn, double landWeight, double oceanWeight)
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
