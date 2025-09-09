package net.dries007.tfc.world.volcano;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.ChunkHeightFiller;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.BiomeExtension;
import net.dries007.tfc.world.biome.BiomeSourceExtension;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;

import static net.dries007.tfc.world.TFCChunkGenerator.*;

public class CenteredFeatureNoise
{
    public static CenteredFeatureNoiseSampler cinder(Seed seed)
    {
        return new CenteredFeatureNoiseSampler()
        {
            final Cellular2D cellNoise = new Cellular2D(seed.seed()).spread(0.009f);
            final Noise2D jitterNoise = new OpenSimplex2D(seed.seed() + 8179234123L).octaves(2).scaled(-0.0016f, 0.0016f).spread(0.128f);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                Cellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock(cell.cx()), QuartPos.fromBlock(cell.cy()));
                final int rarity = biome.getCenteredFeatureRarity();
                if (checkCellRarity(cell, rarity))
                {
                    if (biome == TFCBiomes.ACTIVE_SHIELD_VOLCANO)
                        return modifyHeightShieldVolcano(cell, x, z, biome, heightIn);
                    else
                        return modifyHeight(cell, x, z, biome, heightIn);
                }
                return ChunkHeightFiller.NOT_PRESENT_RETURN;
            }

            /**
             * Calculate the closeness value to a volcano, in the range [0, 1]. 1 = Center of a volcano, 0 = Nowhere near.
             */
            public float calculateEasing(int x, int z, int rarity)
            {
                final Cellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return calculateClampedEasing((float) cell.f1());
                }
                return 0;
            }

            private double modifyHeight(Cellular2D.Cell cell, int x, int z, BiomeExtension biome, double heightIn)
            {
                final double f1 = cell.f1();
                final double easing = Mth.clamp(calculateEasing((float) f1) + jitterNoise.noise(x, z), 0, 1);
                final double shape = calculateShape(1 - easing);
                final double volcanoAdditionalHeight = shape * biome.getCenteredFeatureScaleHeight();
                final double volcanoHeight = (SEA_LEVEL_Y + biome.getCenteredFeatureBaseHeight() + volcanoAdditionalHeight);
                final double weight = 10f * Mth.clamp((float) cell.f2() - f1, 0f, 0.1f);
                return Mth.lerp(easing * weight, heightIn, (0.2 * volcanoHeight + 0.8 * Math.max(volcanoHeight, heightIn + 0.6f * volcanoAdditionalHeight)));

            }

            private double modifyHeightShieldVolcano(Cellular2D.Cell cell, int x, int z, BiomeExtension biome, double heightIn)
            {
                if (cell != null)
                {
                    final double f1 = cell.f1();
                    final double easing = Mth.clamp(calculateEasing((float) f1) + (jitterNoise.noise(x, z)), 0, 1);
                    final double shape = calculateShape(1 - easing);
                    final double volcanoAdditionalHeight = shape * biome.getCenteredFeatureScaleHeight();
                    final double volcanoHeight = (SEA_LEVEL_Y + biome.getCenteredFeatureBaseHeight() + volcanoAdditionalHeight);
                    final double weight = 10f * Mth.clamp(cell.f2() - f1, 0f, 0.1f);
                    return Mth.lerp(easing * weight, heightIn, (0.2 * volcanoHeight + 0.8 * Math.max(volcanoHeight, heightIn + 0.6f * volcanoAdditionalHeight)));
                }
                return heightIn;
            }

            private static float calculateEasing(float f1)
            {
                return Mth.map(f1, 0, 0.23f, 1, 0);
            }

            private static float calculateClampedEasing(float f1)
            {
                return Mth.clamp(calculateEasing(f1), 0, 1);
            }

            /**
             * @param t The unscaled square distance from the volcano, roughly in [0, 1.2]
             * @return A noise function determining the volcano's height at any given position, in the range [0, 1]
             */
            private static double calculateShape(double t)
            {
                if (t > 0.025)
                {
                    return (5 / (9 * t + 1) - 0.5) * 0.279173646008;
                }
                else
                {
                    double a = (t * 9 + 0.05);
                    return (8 * a * a + 2.97663265306) * 0.279173646008;
                }
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return biome.hasCinderCones();
            }

            /**
             * Calculate the center of the nearest volcano, if one exists, to the given x, z, at the given y.
             */
            @Override
            @Nullable
            public BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final Cellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return new BlockPos((int) cell.x(), y, (int) cell.y());
                }
                return null;
            }
        };
    }

    public static CenteredFeatureNoiseSampler tuffRing(Seed seed)
    {
        return new CenteredFeatureNoiseSampler()
        {
            final Cellular2D cellNoise = new Cellular2D(seed.seed()).spread(0.003f);
            final Noise2D jitterNoise = new OpenSimplex2D(seed.seed() + 1234123L).octaves(2).scaled(-0.032f, 0.032f).spread(0.064f);
            final Noise2D addedNoise = new OpenSimplex2D(seed.seed()).octaves(2).spread(0.1).scaled(-2, 8);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                Cellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock(cell.cx()), QuartPos.fromBlock(cell.cy()));
                final int rarity = biome.getCenteredFeatureRarity();
                if (checkCellRarity(cell, rarity))
                {
                    return modifyHeight(cell, x, z, biome, heightIn);
                }
                return ChunkHeightFiller.NOT_PRESENT_RETURN;
            }

            private double modifyHeight(Cellular2D.Cell cell, int x, int z, BiomeExtension biome, double heightIn)
            {
                if (cell != null)
                {
                    final double f1 = cell.f1();
                    final double easing = Mth.clamp(calculateEasing((float) f1) + jitterNoise.noise(x, z), 0, 1);
                    final double shape = calculateShape(1 - easing);
                    final double ringAdditionalHeight = (shape * biome.getCenteredFeatureScaleHeight() + (shape > 0.5 ? addedNoise.noise(x, z) : 0f));
                    final double ringHeight = SEA_LEVEL_Y + biome.getCenteredFeatureBaseHeight() + ringAdditionalHeight;
                    //Linearly scales between baseHeight and the max of baseHeight and ringHeight near the edges of cells
                    return Mth.lerp(50 * Mth.clamp(cell.f2() - f1, 0, 0.02), heightIn, Math.max(ringHeight, heightIn));
                }
                return heightIn;
            }

            /**
             * Calculate the closeness value to a tuff ring center, in the range [0, 1]. 1 = Center, 0 = Nowhere near.
             */
            public float calculateEasing(int x, int z, int rarity)
            {
                final Cellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return calculateClampedEasing((float) cell.f1());
                }
                return 0;
            }

            private static float calculateClampedEasing(float f1)
            {
                return Mth.clamp(calculateEasing(f1), 0, 1);
            }

            private static float calculateEasing(float f1)
            {
                return Mth.map(f1, 0, 0.23f, 1, 0);
            }

            /**
             * @param t The unscaled square distance from the tuff ring center, roughly in [0, 1.2]
             * @return A noise function determining the tuff ring's height at any given position, in the range [0, 1]
             */
            private static double calculateShape(double t)
            {
                return t < 0.03f ? 0 // Flat
                    : t < 0.10f ? t * 7f - 0.21f // Slope up to 0.5
                    : t < 0.15f ? t * 6.6667f // Cliff, 0.67 slope to 1.0
                    : t < 0.20f ? 1 - (t - 0.15f) * 6.6667f // Ridge, slope 1.0 down to 0.67
                    : 1.5f - (5 * t); // Cliff base, slope 0.5 down
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return biome.hasTuffRings();
            }

            /**
             * Calculate the center of the nearest tuff ring, if one exists, to the given x, z, at the given y.
             */
            @Override
            @Nullable
            public BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final Cellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return new BlockPos((int) cell.x(), y, (int) cell.y());
                }
                return null;
            }
        };
    }

    public static CenteredFeatureNoiseSampler tuya(Seed seed)
    {
        return new CenteredFeatureNoiseSampler()
        {
            final Cellular2D cellNoise = new Cellular2D(seed.seed(), 0.21f, 1).spread(0.0033f);
            final Noise2D jitterNoise = new OpenSimplex2D(seed.seed() + 1234123L).octaves(2).scaled(-0.016f, 0.016f).spread(0.128f);
            final Noise2D addedNoise = new OpenSimplex2D(seed.seed()).octaves(2).spread(0.1).scaled(-2, 3);

            @Override
            public double setColumnAndSampleHeight(double heightIn, int x, int z, BiomeSourceExtension biomeSource)
            {
                Cellular2D.Cell cell = cellNoise.cell(x, z);
                final BiomeExtension biome = biomeSource.getBiomeExtension(QuartPos.fromBlock(cell.cx()), QuartPos.fromBlock(cell.cy()));
                final int rarity = biome.getCenteredFeatureRarity();
                if (checkCellRarity(cell, rarity))
                {
                    return modifyHeight(cell, x, z, biome, heightIn);
                }
                return ChunkHeightFiller.NOT_PRESENT_RETURN;
            }

            public double modifyHeight(Cellular2D.Cell cell, double x, double z, BiomeExtension biome, double heightIn)
            {
                if (cell != null)
                {
                    final double easing = Mth.clamp(calculateEasing((float) cell.f1()) + jitterNoise.noise(x, z), 0, 1);
                    final double shape = biome.getCenteredFeatureIce() ? calculateIcyShape(1 - easing) : calculateShape(1 - easing);
                    final double additionalHeight = (float) (shape * biome.getCenteredFeatureScaleHeight() + addedNoise.noise(x, z));
                    final double tuyaHeight = SEA_LEVEL_Y + biome.getCenteredFeatureBaseHeight() + additionalHeight;
                    return Mth.lerp(easing, heightIn, 0.5f * (tuyaHeight + Math.max(tuyaHeight, heightIn + 0.4f * additionalHeight)));
                }
                return heightIn;
            }

            /**
             * @param t The unscaled square distance from the tuya, roughly in [0, 1.2]
             * @return A noise function determining the tuya's height at any given position, in the range [0, 1]
             */
            private static double calculateShape(double t)
            {
                return t < 0.015f ? Mth.map(t, 0f, 0.015f, 0.8f, 1f)// Caldera
                    : t < 0.125f ? Mth.map(t, 0.025f, 0.125f, 1f, 0.75f) // Shallow slope down
                    : Mth.clampedMap(t, 0.125f, 0.16f, 0.75f, 0f); // Cliff
            }

            /**
             * @param t The unscaled square distance from the tuya, roughly in [0, 1.2]
             * @return A noise function determining the tuya's height at any given position, in the range [0, 1]
             */
            private static double calculateIcyShape(double t)
            {
                return t < 0.015f ? Mth.map(t, 0f, 0.015f, 0.8f, 1f) // Caldera
                    : t < 0.125f ? Mth.map(t, 0.025f, 0.125f, 1f, 0.75f) // Shallow slope down
                    : t < 0.16 ? Mth.map(t, 0.125f, 0.16f, 0.75f, 0f) // Cliff
                    : Mth.clampedMap(t, 0.16f, 0.19f, 0f, 0.5f); // Ice edge
            }

            /**
             * Calculate the closeness value to a tuya, in the range [0, 1]. 1 = Center of a tuya, 0 = Nowhere near.
             */
            public float calculateEasing(int x, int z, int rarity)
            {
                final Cellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return calculateClampedEasing((float) cell.f1());
                }
                return 0;
            }

            private static float calculateEasing(float f1)
            {
                return Mth.map(f1, 0, 0.23f, 1, 0);
            }

            private static float calculateClampedEasing(float f1)
            {
                return Mth.clamp(calculateEasing(f1), 0, 1);
            }

            @Override
            public boolean isValidBiome(BiomeExtension biome)
            {
                return biome.hasTuyas();
            }

            /**
             * Calculate the center of the nearest tuya, if one exists, to the given x, z, at the given y.
             */
            @Nullable
            public BlockPos calculateCenter(int x, int y, int z, int rarity)
            {
                final Cellular2D.Cell cell = cellNoise.cell(x, z);
                if (checkCellRarity(cell, rarity))
                {
                    return new BlockPos((int) cell.x(), y, (int) cell.y());
                }
                return null;
            }
        };
    }


}
