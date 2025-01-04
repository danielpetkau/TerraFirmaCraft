/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.biome;

import java.util.Random;
import net.minecraft.util.Mth;

import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.noise.Cellular2D;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.noise.OpenSimplex2D;
import net.dries007.tfc.world.region.Units;

import static net.dries007.tfc.world.TFCChunkGenerator.*;

/**
 * Collections of biome noise factories
 * These are built by hand and assigned to different biomes
 */
public final class BiomeNoise
{

    /**
     * Basic noise map used by several biomes with connected ridge-noise valleys
     */
    public static Noise2D connectedValleyBaseNoise(long seed)
    {
        return new OpenSimplex2D(seed).spread(0.0025);
    }

    /**
     * Ridge-noise form of connectedValleyBaseNoise
     */
    public static Noise2D connectedValleyAbsNoise(long seed)
    {
        return connectedValleyBaseNoise(seed).abs();
    }

    /**
     * Generates a flat base with twisting carved canyons using many smaller terraces.
     * Inspired by imagery of Drumheller, Alberta
     */
    public static Noise2D badlands(long seed, int height, float depth)
    {
        return new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.025f)
            .scaled(SEA_LEVEL_Y + height, SEA_LEVEL_Y + height + 10)
            .add(new OpenSimplex2D(seed + 1)
                .octaves(4)
                .spread(0.04f)
                .ridged()
                .map(x -> 1.3f * -(x > 0 ? x * x * x : 0.5f * x))
                .scaled(-1f, 0.3f, -1f, 1f)
                .terraces(15)
                .scaled(-depth, 0)
            )
            .map(x -> x < SEA_LEVEL_Y ? SEA_LEVEL_Y - 0.3f * (SEA_LEVEL_Y - x) : x);
    }

    /**
     * Creates a variant of badlands with stacked pillar like structures, as opposed to relief carved.
     * Inspired by imagery of Bryce Canyon, Utah.
     */
    public static Noise2D bryceCanyon(long seed) // TODO: consider hooking up to global connected canyon noise systems
    {
        final Random generator = new Random(seed);

        Noise2D noise = new OpenSimplex2D(generator.nextLong()).octaves(4).spread(0.1f).scaled(SEA_LEVEL_Y + 2, SEA_LEVEL_Y + 14);
        for (int layer = 0; layer < 3; layer++)
        {
            final float threshold = 0.25f;
            final float delta = 0.015f;

            noise = noise.add(new OpenSimplex2D(generator.nextLong())
                .octaves(3)
                .spread(0.02f + 0.01f * layer)
                .abs()
                .affine(1, -0.05f * layer)
                .map(t -> Mth.clampedMap(t, threshold, threshold + delta, 0, 1))
                .lazyProduct(new OpenSimplex2D(generator.nextLong())
                    .octaves(4)
                    .spread(0.1f)
                    .scaled(5, 11)));
        }
        return noise;
    }

    /**
     * Domain warping creates twisting land patterns
     */
    public static Noise2D canyons(long seed, int minHeight, int maxHeight)
    {
        final OpenSimplex2D warp = new OpenSimplex2D(seed).octaves(4).spread(0.03f).scaled(-100f, 100f);
        return new OpenSimplex2D(seed + 1)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, SEA_LEVEL_Y + minHeight, SEA_LEVEL_Y + maxHeight);
    }

    /**
     * Simple noise with little variance.
     */
    public static Noise2D hills(long seed, int minHeight, int maxHeight)
    {
        return new OpenSimplex2D(seed).octaves(4).spread(0.05f).scaled(SEA_LEVEL_Y + minHeight, SEA_LEVEL_Y + maxHeight);
    }

    /**
     * Effectively a {@code lerp(noiseA(), noiseB(), piecewise(noiseB()) + noiseC()} with the following additional techniques:
     * <ul>
     *     <li>{@code noiseA} is scaled to outside it's range, then biased towards 1.0, to expose more cliffs, as opposed to hills </li>
     *     <li>{@code piecewise()} is a piecewise linear function that creates cliff shapes from the standard noise distribution.</li>
     *     <li>{@code noiseC} is added on top to provide additional variance (in places where the piecewise function would otherwise flatten areas.</li>
     * </ul>
     */
    public static Noise2D sharpHills(long seed, float minMeight, float maxHeight)
    {
        final Noise2D base = new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.08f);

        final Noise2D lerp = new OpenSimplex2D(seed + 7198234123L)
            .spread(0.013f)
            .scaled(-0.3f, 1.6f)
            .clamped(0, 1);

        final Noise2D lerpMapped = (x, z) -> {
            double in = base.noise(x, z);
            return Mth.lerp(lerp.noise(x, z), in, sharpHillsMap(in));
        };

        final OpenSimplex2D variance = new OpenSimplex2D(seed + 67981832123L)
            .octaves(3)
            .spread(0.06f)
            .scaled(-0.2f, 0.2f);

        return lerpMapped
            .add(variance)
            .scaled(-0.75f, 0.7f, SEA_LEVEL_Y - minMeight, SEA_LEVEL_Y + maxHeight);
    }

    /**
     * Jointed surface of ice sheets/glaciers, considered by biome noise, not by surface builder
     */
    public static Noise2D glacierTexture(long seed)
    {
        final Noise2D warp = new OpenSimplex2D(seed + 413L).spread(0.02).scaled(-12, 12);
        return (x, z) -> {
            final double yOfX = Math.min(Helpers.triangle(25, 18, 0.035, x + warp.noise(x, z)), 0.0);
            final double yOfZ = Math.min(Helpers.triangle(40, 30, 0.025, z + warp.noise(z, x)), 0.0);
            return Math.min(yOfX, yOfZ);
        };


    }

    // Referenced from multiple locations
    public static Noise2D glacialBase(long seed)
    {
        return knobAndKettle(seed).addConstant(1.5);
    }

    public static Noise2D glacialOceanicBase(long seed)
    {
        return hills(seed, -12, -2);
    }

    public static Noise2D glacialIceSurface(long seed)
    {
        return BiomeNoise.hills(seed, 23, 38);
    }

    public static Noise2D glacialMontaneIceSurface(long seed)
    {
        return BiomeNoise.hills(seed, 40, 48);
    }

    public static Noise2D glacialOceanicIceSurface(long seed)
    {
        return BiomeNoise.hills(seed, 18, 26);
    }

    public static Noise2D glacialCirquesIceSurface(long seed)
    {
        return connectedValleyAbsNoise(seed)
            .map(y ->
                y < 0.38 ? -100 :
                y < 0.43 ? Mth.map(y, 0.38, 0.43, -50, 0) :
                    Mth.map(y, 0.43, 1, 0, 32))
            .add(BiomeNoise.hills(seed, 15, 23)
            .add(BiomeNoise.glacialCirquesCliffsScale(seed)));
    }

    /**
     * Should mirror the upper surface of {@link BiomeNoise#glacialCirquesIceSurface(long)}, but 3 blocks highers
     * Reverses slope instead of plunging vertically at edges of glaciers to avoid creating cliffs that block the mouths of cirques
     */
    public static Noise2D glacialCirquesCliffsStartHeight(long seed)
    {
        return connectedValleyAbsNoise(seed)
            .map(y -> y < 0.43
                ? Mth.map(y, 0, 0.43, 32, 0)
                : Mth.map(y, 0.43, 1, 0, 32))
            .add(BiomeNoise.hills(seed, 18, 26));
    }

    public static Noise2D glacialCirquesCliffsScale(long seed)
    {
        return new OpenSimplex2D(seed + 78267L).spread(0.015).add(glacialValleyShapeNoise(seed)).scaled(-10, 8).clamped(0, 7);

    }

    public static Noise2D glacialValleyShapeNoise(long seed)
    {
        return connectedValleyBaseNoise(seed).map(y -> Math.min(6 * y * y, 0.75 + 0.25 * y)).add(new OpenSimplex2D(seed + 5287L).octaves(4).spread(0.06).scaled(-0.2, 0.2));
    }

    /**
     * By default, this function is at the correct height for oceanic glacial mountains. It is shifted up for standard glacial mountains
     */
    public static Noise2D glacialCirques(long seed)
    {

        // Noise for the large, continuous valleys
        final Noise2D shape = glacialValleyShapeNoise(seed);
        final Noise2D shapeMap = connectedValleyAbsNoise(seed);

        // Glacial mountain noise is based on cellular noise. Cells are either bowl-shaped cirques, or cone-shaped horns
        final double cellScale = 0.010;
        final Cellular2D cells = new Cellular2D(seed).spread(cellScale);
        final Noise2D warp = new OpenSimplex2D(seed).spread(0.02).add(shapeMap).scaled(-1, 2, -0.25, 0.2);
        final Noise2D roughPeaks = new OpenSimplex2D(seed).octaves(3).spread(0.08).scaled(0.6, 1.6);

        // Cliffs in valleys
        final Noise2D cliffScale = new OpenSimplex2D(seed + 785267L).spread(0.01).scaled(-12, 15).clamped(0, 10);
        final Noise2D cliffStartHeight = glacialOceanicIceSurface(seed).addConstant(-8);

        final Noise2D cirques = (x, z) -> {
            Cellular2D.Cell cell = cells.cell(x, z);

            final double f1 = cell.f1();
            final double f2 = cell.f2();
            final double f2f1 = (f1 > 0 ? (f2 - f1) : 1);

            final double shapeAtCenter = shapeMap.noise(cell.cx() / cellScale, cell.cy() / cellScale);

            // Whether a cell is a cirque or a horn is based on the shape noise, a way of approximating the distance to the nearest valley
            if (shapeAtCenter > 0.60)
            {
                // Horn height function
                double y = (f2f1 + warp.noise(x, z));
                final double rough = roughPeaks.noise(x, z);
                final double scale = Math.min(Helpers.lerp(2 * y, 1.0, (rough)), rough);
                y = 1 + scale * y;
                return y;
            }
            else
            {
                // Cirque height function
                double y = 1 - (f2f1 - warp.noise(x, z));
                y = 0.5 * (1 + y * y);

                final double shapeAtPoint = shapeMap.noise(x, z);
                final double valleyCloseness = Math.min(shapeAtPoint - shapeAtCenter, 0);

                return y + Mth.clampedMap(f2f1,0, 0.1, 0, valleyCloseness);
            }
        };
        return cirques.scaled(0, 1, 12, 64).lazyProduct(shape).addConstant(SEA_LEVEL_Y - 15).cliffMap(cliffStartHeight, cliffScale).cliffMap(glacialCirquesCliffsStartHeight(seed), glacialCirquesCliffsScale(seed));
    }

    // Polygonal incisions 1 block deep, mimics patterned ground caused by permafrost
    public static Noise2D patternedGround(long seed)
    {
        Cellular2D cells = new Cellular2D(seed).spread(0.05);
        return (x, z) -> {
            Cellular2D.Cell cell = cells.cell(x, z);

            final double f1 = cell.f1();
            final double f2 = cell.f2();

            return f2 - f1 < 0.12 ? -1 : 0;
        };
    }

    // Ring-shaped protrusions 1 block high, based on those found in the Svalbard Archipelago and other polar climates
    public static Noise2D stoneCircles(long seed)
    {
        Cellular2D cells = new Cellular2D(seed, 0.26f).spread(0.11);
        return (x, z) -> {
            Cellular2D.Cell cell = cells.cell(x, z);

            final double f1 = cell.f1();

            return f1 > 0.07 && f1 < 0.15 ? 1 : 0;
        };
    }

    public static Noise2D knobAndKettle(long seed)
    {
        return new OpenSimplex2D(seed).octaves(2).spread(0.03f)
            .map(y -> y > 0.3 ? y - 0.3 : y < -0.3 ? y + 0.3 : 0)
            .scaled(-12, 10).add(BiomeNoise.hills(seed, -3, 3));
    }

    public static Noise2D drumlins(long seed)
    {
        return new OpenSimplex2D(seed).octaves(3).spread(0.04f).scaled(SEA_LEVEL_Y - 16, SEA_LEVEL_Y + 32).stretchZ(2.5);
    }

    /**
     * Inspired by the bare Karst at Burren, Ireland
     * Can be applied over any base terrain noise map, adds to the base terrain
     */
    public static Noise2D burren(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final int minHeight = SEA_LEVEL_Y + 2;

        final Noise2D crevices = burrenCrevices(seed).map(y -> y < 0.15 ? -scale : y < 0.4 ? (y - 0.4) * scale : 0);

        return crevices.add(baseTerrainNoise).map(y -> Math.max(y, minHeight));
    }

    /**
     * The seeded, absolute value noise used by the Burren Biome
     * Used by surface builder as well as Burren noise
     */
    public static Noise2D burrenCrevices(long seed)
    {
        return new OpenSimplex2D(seed + 398767567L)
            .octaves(2)
            .spread(0.08f)
            .map(
                y -> {
                    return y = Math.abs(y);
                }
            );
    }

    /**
     * Inspired by the "Stone Forests" of Shilin, China
     * Can be applied over any base terrain noise map, takes the max value of the two noises
     */
    public static Noise2D shilin(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final int minHeight = SEA_LEVEL_Y + 2;

        final Noise2D ridges = shilinRidges(seed);

        final Noise2D bumps = new OpenSimplex2D(seed + 83436545633L).spread(0.16).scaled(0.6, 1.0);

        return ridges.lazyProduct(bumps).scaled(SEA_LEVEL_Y, SEA_LEVEL_Y + scale).max(baseTerrainNoise).map(y -> Math.max(y, minHeight));
    }

    /**
     * Unscaled shilin noise, not applied to terrain
     * Used by surface builder as well as Shilin noise
     */
    public static Noise2D shilinRidges(long seed)
    {
        final double widthTop = 0.1;
        final double widthBot = 0.2;

        // Basic ridge shapes following zeroes in the noise
        final Noise2D ridges = new OpenSimplex2D(398767567L)
            .octaves(2)
            .spread(0.06f)
            .map(
                y -> {
                    y = Math.abs(y);
                    return y < widthTop ? 1 : y < widthBot ? 1 + (0.67 * (y - widthTop) / (widthTop - widthBot)) : 0;
                }
            );

        // Cuts continuous paths through ridges to make them more passable
        final Noise2D cuts = new OpenSimplex2D(45764379L)
            .octaves(2)
            .spread(0.03f)
            .map(
                y -> {
                    y = Math.abs(y);
                    y = y < widthTop * 0.65 ? 1 : y < widthBot * 1.2 ? 1 + ((y - widthTop * 0.65) / (widthTop * 0.65 - widthBot * 1.2)) : 0;

                    return 1 - y;
                }
            );

        return ridges.lazyProduct(cuts);
    }

    /**
     * Fengcong, aka "Cone Karsts"
     * Can be applied over any base terrain noise map, adds to the base terrain
     */
    public static Noise2D fengcong(long seed, Noise2D baseTerrainNoise)
    {
        final double scale = 37;

        final Noise2D cones = new OpenSimplex2D(seed)
            .octaves(3)
            .spread(0.06)
            .map(y -> {
                y = -0.5 * Math.cos(Math.PI * (Math.abs(y))) + 0.5;
                y = (Math.max(y, 0.25) - 0.25) / 0.75;
                y = scale * y;
                return y;
            });

        return baseTerrainNoise.add(cones);
    }

    /**
     * Fenglin, aka "Tower Karsts"
     * Can be applied over any base terrain noise map, adds to the base terrain
     */
    public static Noise2D fenglin(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final Noise2D cliffScale = new OpenSimplex2D(seed + 78535267L)
            .spread(0.06)
            .scaled(0, 0.25);

        final Noise2D cliffStartHeight = new OpenSimplex2D(seed + 390798L)
            .spread(0.06)
            .scaled(0, 0.7);

        final Noise2D towers = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.05)
            .map(y -> {
                y = Math.abs(y) - 0.45;
                y = y > 0 ? Math.sqrt(y / 0.55) : 0;
                return y;
            })
            .fenglinCliffMap(cliffStartHeight, cliffScale, cliffScale.map(x -> 1 - x))
            .map(y -> scale * y);

        return baseTerrainNoise.add(towers);
    }

    /**
     * Bowl dolines/shallow sinkholes, similar noise to cone karsts
     * Can be applied over any base terrain noise map, subtracts from the base terrain
     */
    public static Noise2D bowlDolines(long seed, Noise2D baseTerrainNoise, double scale)
    {
        final Noise2D bowls = new OpenSimplex2D(seed)
            .octaves(3)
            .spread(0.72 / scale)
            .map(x -> {
                x = -0.5 * Math.cos(Math.PI * x) + 0.5;
                x = (Math.max(x, 0.1) - 0.1);
                x = -scale * x;
                return x;
            });

        return baseTerrainNoise.add(bowls);
    }

    /**
     * Cenotes/deep sinkholes, similar noise to tower karsts
     * Can be applied over any base terrain noise map, subtracts from the base terrain
     */
    public static Noise2D cenotes(long seed, Noise2D baseTerrainNoise, double vertScale, double horizScale)
    {
        final Noise2D cliffScale = new OpenSimplex2D(seed + 78535267L)
            .spread(0.72 / horizScale)
            .scaled(0, 0.4);

        final Noise2D cliffStartHeight = new OpenSimplex2D(seed + 390798L)
            .spread(0.72 / horizScale)
            .scaled(0, 0.7);

        final Noise2D cenotes = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.6 / horizScale)
            .map(y -> {
                y = Math.abs(y) - 0.45;
                y = y > 0 ? Math.sqrt(y / 0.55) : 0;
                return y;
            })
            .fenglinCliffMap(cliffStartHeight, cliffScale, cliffScale.map(x -> 1 - x))
            .map(y -> -vertScale * y);

        return baseTerrainNoise.add(cenotes);
    }
    /**
     * Multi-tiered sinkholes inspired by the Xiaozhai Tiankeng
     * Essentially applies two "cenotes" of different sizes on top of each other
     */

    public static Noise2D tiankeng(long seed, Noise2D baseTerrainNoise)
    {
        final Noise2D cliffScale = new OpenSimplex2D(seed + 78535267L)
            .spread(0.04)
            .scaled(0, 0.04);

        final Noise2D cliffStartHeight = new OpenSimplex2D(seed + 390798L)
            .spread(0.04)
            .scaled(0, 0.7);

        final Noise2D wide = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.02)
            .map(y -> {
                y = Math.abs(y) - 0.3;
                y = y > 0 ? Math.sqrt(y / 0.7) : 0;
                return y;
            })
            .fenglinCliffMap(cliffStartHeight, cliffScale, cliffScale.map(x -> 1 - x))
            .map(y -> -22 * y);

        final Noise2D deep = new OpenSimplex2D(seed)
            .octaves(2)
            .spread(0.02)
            .map(y -> {
                y = Math.abs(y) - 0.65;
                y = y > 0 ? Math.sqrt(y / 0.35) : 0;
                return y;
            })
            .fenglinCliffMap(cliffStartHeight, cliffScale, cliffScale.map(x -> 1 - x))
            .map(y -> -24 * y);

        return baseTerrainNoise.add(wide).add(deep);
    }

    public static double sharpHillsMap(double in)
    {

        final double in0 = 1.0f, in1 = 0.67f, in2 = 0.15f, in3 = -0.15f, in4 = -0.67f, in5 = -1.0f;
        final double out0 = 1.0f, out1 = 0.7f, out2 = 0.5f, out3 = -0.5f, out4 = -0.7f, out5 = -1.0f;

        if (in > in1)
            return Mth.map(in, in1, in0, out1, out0);
        if (in > in2)
            return Mth.map(in, in2, in1, out2, out1);
        if (in > in3)
            return Mth.map(in, in3, in2, out3, out2);
        if (in > in4)
            return Mth.map(in, in4, in3, out4, out3);
        else
            return Mth.map(in, in5, in4, out5, out4);
    }

    public static Noise2D lake(long seed)
    {
        return new OpenSimplex2D(seed).octaves(4).spread(0.15f).scaled(SEA_LEVEL_Y - 12, SEA_LEVEL_Y + 2)
            .add(new OpenSimplex2D(seed + 1)
                .octaves(5)
                .spread(0.1f)
                .map(val -> val * val * val * val)
                .scaled(-2, 2)
                .clamped(0, 2)
            );
    }

    /**
     * Noise right around sea level which has been flattened, to produce lots of small pockets above and below water
     */
    public static Noise2D lowlands(long seed)
    {
        return hills(seed, -3, -2)
            .add(new OpenSimplex2D(seed + 1)
                .octaves(6)
                .spread(0.55f)
                .scaled(-2, 2)
                .clamped(-2, 1)
            );
    }

    /**
     * Very flat biome
     */
    public static Noise2D flats(long seed)
    {
        return new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.03f)
            .scaled(SEA_LEVEL_Y - 12, SEA_LEVEL_Y + 8)
            .clamped(SEA_LEVEL_Y, SEA_LEVEL_Y + 2);
    }

    /**
     * Noise just above sea level
     */
    public static Noise2D saltFlats(long seed)
    {
        return new OpenSimplex2D(seed)
            .octaves(4)
            .spread(0.05f)
            .scaled(SEA_LEVEL_Y - 16, SEA_LEVEL_Y + 10)
            .clamped(SEA_LEVEL_Y - 2, SEA_LEVEL_Y);
    }

    /**
     * Noise just above sea level
     */
    public static Noise2D dunes(long seed, int minHeight, int maxHeight)
    {
        return new OpenSimplex2D(seed)
            .spread(0.02)
            .scaled(-3, 3)
            .add((x, z) -> x / 6 + 20 * Math.sin(z / 240))
            .map(value -> 1.3 * (Math.abs((value % 5) - 1) * ((value % 5) - (value % 1) > 0 ? 0.5 : 2) - 1))
            .clamped(-1, 1)
            .lazyProduct(new OpenSimplex2D(seed)
                .octaves(4)
                .spread(0.1)
                .scaled(-1, 2)
                .clamped(0.4, 1))
            .scaled(SEA_LEVEL_Y + minHeight, SEA_LEVEL_Y + maxHeight);
    }

    public static Noise2D mountains(long seed, int baseHeight, int scaleHeight)
    {
        final Noise2D baseNoise = new OpenSimplex2D(seed) // A simplex noise forms the majority of the base
            .octaves(6) // High octaves to create highly fractal terrain
            .spread(0.14f)
            .add(new OpenSimplex2D(seed + 1) // Ridge noise is added to mimic real mountain ridges. It is scaled smaller than the base noise to not be overpowering
                .octaves(4)
                .spread(0.02f)
                .scaled(-0.7f, 0.7f)
                .ridged() // Ridges are applied after octaves as it creates less directional artifacts this way
            )
            .map(x -> {
                final double x0 = 0.125f * (x + 1) * (x + 1) * (x + 1); // Power scaled, flattens most areas but maximizes peaks
                return SEA_LEVEL_Y + baseHeight + scaleHeight * x0; // Scale the entire thing to mountain ranges
            });

        // Cliff noise consists of noise that's been artificially clamped over half the domain, which is then selectively added above a base height level
        // This matches up with the distinction between dirt and stone
        final Noise2D cliffNoise = new OpenSimplex2D(seed + 2).octaves(2).spread(0.01f).scaled(-25, 25).map(x -> x > 0 ? x : 0);
        final Noise2D cliffHeightNoise = new OpenSimplex2D(seed + 3).octaves(2).spread(0.01f).scaled(140 - 20, 140 + 20);

        return (x, z) -> {
            double height = baseNoise.noise(x, z);
            if (height > 120) // Only sample each cliff noise layer if the base noise could be influenced by it
            {
                final double cliffHeight = cliffHeightNoise.noise(x, z) - height;
                if (cliffHeight < 0)
                {
                    final double mappedCliffHeight = Mth.clampedMap(cliffHeight, 0, -1, 0, 1);
                    height += mappedCliffHeight * cliffNoise.noise(x, z);
                }
            }
            return height;
        };
    }

    /**
     * Uses domain warping to achieve a swirly hills effect
     */
    public static Noise2D ocean(long seed, int depthMin, int depthMax)
    {
        final OpenSimplex2D warp = new OpenSimplex2D(seed).octaves(2).spread(0.015f).scaled(-30, 30);
        return new OpenSimplex2D(seed + 1)
            .octaves(4)
            .spread(0.11f)
            .scaled(SEA_LEVEL_Y + depthMin, SEA_LEVEL_Y + depthMax)
            .warped(warp);
    }

    /**
     * Applies elements from deep ocean and badlands.
     * Inverse power scaled ridge noise (cubic) is used to create ridges, inside the domain warped ocean noise
     */
    public static Noise2D oceanRidge(long seed, int depthMin, int depthMax)
    {
        final OpenSimplex2D warp = new OpenSimplex2D(seed).octaves(2).spread(0.015f).scaled(-30, 30);
        final Noise2D ridgeNoise = new OpenSimplex2D(seed + 1).octaves(4).spread(0.015f).ridged().map(x -> { // In [-1, 1]
            if (x > -0.3f)
            {
                x = (x + 0.3f) / 1.3f;  // In [0, 1]
                x = x * x * x; // Power scaled
                return -16f * x; // In [0, -16]
            }
            return 0; // No modifications outside of ridge area
        });
        return new OpenSimplex2D(seed + 2).octaves(4).spread(0.11f).scaled(SEA_LEVEL_Y + depthMin, SEA_LEVEL_Y + depthMax).add(ridgeNoise).warped(warp);
    }

    public static Noise2D shore(long seed)
    {
        return new OpenSimplex2D(seed).octaves(4).spread(0.17f).scaled(SEA_LEVEL_Y, SEA_LEVEL_Y + 5f);
    }

    public static Noise2D tidalFlats(long seed)
    {
        return new OpenSimplex2D(seed).octaves(4).spread(0.17f).scaled(SEA_LEVEL_Y, SEA_LEVEL_Y + 1.8f);
    }

    // Shield volcanoes with minimal erosion, recent lava flows on surface, no/small calderas
    public static Noise2D activeShieldVolcano(long seed, Noise2D hotspot)
    {
        final double edgeElev = SEA_LEVEL_Y + 1;
        final double calderaEdgeElev = SEA_LEVEL_Y + 115;
        final double cliffEdgeElev = SEA_LEVEL_Y + 90;
        final double calderaCenterElev = SEA_LEVEL_Y + 60;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.75 ? Mth.map(y, 0, 0.75, edgeElev, calderaEdgeElev) // Slope upwards to mountain top or crater rim
            : y < 0.78 ? Mth.map(y, 0.75, 0.78, calderaEdgeElev, cliffEdgeElev) // Cliff at edge of crater
            : Mth.map(y, 0.78, 1, cliffEdgeElev, calderaCenterElev)); // Interior of crater

        final Noise2D flows = lavaFlow(seed).map(y -> y < 0.45 ? 0 : 1);
        final OpenSimplex2D warp = new OpenSimplex2D(seed).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 1)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -8, 8);

        return volcano.add(flows).add(surface);

    }

    // Shield volcanoes with some erosion, no recent lava flows, large calderas with open sides
    public static Noise2D dormantShieldVolcano(long seed, Noise2D hotspot)
    {
        final double seaElev = SEA_LEVEL_Y + 9;
        final double mtnBaseElev = SEA_LEVEL_Y + 40;
        final double calderaEdgeElev = SEA_LEVEL_Y + 70;
        final double cliffEdgeElev = SEA_LEVEL_Y + 50;
        final double calderaCenterElev = SEA_LEVEL_Y + 15;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.45 ? Mth.map(y, 0, 0.45, seaElev, mtnBaseElev) // Coastal slopes
                : y < 0.7 ? Mth.map(y, 0.45, 0.7, mtnBaseElev, calderaEdgeElev) // Mountain side/slope up to caldera
                : y < 0.73 ? Mth.map(y, 0.7, 0.73, calderaEdgeElev, cliffEdgeElev) // Caldera cliff
                : y < 0.85 ? Mth.map(y, 0.73, 0.85, cliffEdgeElev, calderaCenterElev) : calderaCenterElev); // Downward slope to flat bottom of caldera

        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -12, 12);

        return volcano.add(surface);
    }

    public static Noise2D extinctShieldVolcano(long seed, Noise2D hotspot)
    {
        final double seaElev = SEA_LEVEL_Y + 6;
        final double mtnBaseElev = SEA_LEVEL_Y + 25;
        final double calderaEdgeElev = SEA_LEVEL_Y + 55;
        final double cliffEdgeElev = SEA_LEVEL_Y + 25;
        final double calderaCenterElev = SEA_LEVEL_Y - 10;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.4 ? Mth.map(y, 0, 0.4, seaElev, mtnBaseElev) // Coastal slopes
                : y < 0.6 ? Mth.map(y, 0.4, 0.6, mtnBaseElev, calderaEdgeElev) // Mountain side/slope up to caldera
                : y < 0.62 ? Mth.map(y, 0.6, 0.62, calderaEdgeElev, cliffEdgeElev) // Caldera cliff
                : y < 0.75 ? Mth.map(y, 0.62, 0.75, cliffEdgeElev, calderaCenterElev) : calderaCenterElev); // Downward slope to flat bottom of caldera

        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -9, 9);

        return volcano.add(surface);
    }

    // Shield volcanoes with large calderas with open sides, and rough surfaces to fill with glaciers
    public static Noise2D glaciatedShieldVolcano(long seed, Noise2D hotspot)
    {
        final double seaElev = SEA_LEVEL_Y + 15;
        final double mtnBaseElev = SEA_LEVEL_Y + 70;
        final double calderaEdgeElev = SEA_LEVEL_Y + 100;
        final double cliffEdgeElev = SEA_LEVEL_Y + 60;
        final double calderaCenterElev = SEA_LEVEL_Y + 50;

        final Noise2D volcano = hotspot.map(y ->
            y < 0.45 ? Mth.map(y, 0, 0.45, seaElev, mtnBaseElev) // Coastal slopes
                : y < 0.72 ? Mth.map(y, 0.45, 0.72, mtnBaseElev, calderaEdgeElev) // Mountain side/slope up to caldera
                : y < 0.74 ? Mth.map(y, 0.72, 0.74, calderaEdgeElev, cliffEdgeElev) // Caldera cliff
                : y < 0.85 ? Mth.map(y, 0.74, 0.85, cliffEdgeElev, calderaCenterElev) : calderaCenterElev); // Downward slope to flat bottom of caldera

        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.02f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -48, 32);

        return volcano.add(surface);
    }

    // Surface for Ice Sheet Shield Volcanos
    public static Noise2D shieldVolcanoIceSheetSurface(long seed, Noise2D hotspot)
    {
        final double edgeElev = 0;
        final double calderaCenterElev = 51;

        return hotspot.map(y ->
            y < 0.9 ? Mth.map(y, 0.0, 0.9, edgeElev, calderaCenterElev) : calderaCenterElev) // Interior of crater
            .add(glacialIceSurface(seed));
    }

    // Surface for Glaciated Shield Volcanos
    public static Noise2D shieldVolcanoGlacierSurface(long seed, Noise2D hotspot)
    {
        final double edgeElev = -20;
        final double mtnBaseElev = 0;
        final double calderaCenterElev = 51;

        return hotspot.map(y ->
            y < 0.45 ? Mth.map(y, 0.0, 0.45, edgeElev, mtnBaseElev)
                : y < 0.9 ? Mth.map(y, 0.45, 0.9, mtnBaseElev, calderaCenterElev) : calderaCenterElev) // Interior of crater
            .add(glacialIceSurface(seed));
    }

    // Shield volcanoes with some erosion, no recent lava flows, large calderas with open sides
    public static Noise2D sunkenShieldVolcano(long seed, Noise2D hotspot)
    {
        final Noise2D volcano = hotspot.map(y ->
                y < 0.25 ? 50
                : y < 0.45 ? Mth.map(y, 0.25, 0.45, 50, SEA_LEVEL_Y)
                : y < 0.6 ? Mth.map(y, 0.45, 0.6, SEA_LEVEL_Y, 95)
                : y < 0.62 ? Mth.map(y, 0.6, 0.62, 94, 80)
                : y < 0.75 ? Mth.map(y, 0.62, 0.75, 80, 52) : 52);

        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -6, 6);

        final Noise2D scale = new OpenSimplex2D(seed + 789913L).octaves(2).spread(0.008f).scaled(0.45, 1);

        return volcano.lazyProduct(scale).add(surface);
    }

    // Shield volcanoes with heavily eroded calderas
    public static Noise2D ancientShieldVolcano(long seed, double minElev, double maxElev, Noise2D hotspot)
    {
        final Noise2D volcano = hotspot.map(y ->
            y < 0.15 ? 90
                : y < 0.6 ? Mth.map(y, 0.15, 0.6, 90, 130)
                : y < 0.63 ? Mth.map(y, 0.6, 0.63, 129, 108)
                : y < 0.7 ? Mth.map(y, 0.63, 0.7, 108, 90) : 90);

        final OpenSimplex2D warp = new OpenSimplex2D(seed + 43L).octaves(4).spread(0.03f).scaled(-100f, 100f);
        final Noise2D surface = new OpenSimplex2D(seed + 44L)
            .octaves(4)
            .spread(0.06f)
            .warped(warp)
            .map(x -> x > 0.4 ? x - 0.8f : -x)
            .scaled(-0.4f, 0.8f, -20, 0);

        final Noise2D valleys = new OpenSimplex2D(seed + 90183L).spread(0.01).ridged().octaves(3).scaled(maxElev * 2.2, minElev);

        final Noise2D scale = new OpenSimplex2D(seed + 789913L).octaves(2).spread(0.008f).scaled(0.6, 1);

        return volcano.lazyProduct(scale).min(valleys).add(surface);
    }

    public static Noise2D lavaFlow(long seed)
    {
        return new OpenSimplex2D(seed + 23891L).ridged().spread(0.01);
    }

    // Currently erupting location in a hotspot chain, used for biome noise and regional hotspot placement
    public static Noise2D activeHotSpots(long seed)
    {
        final double horizontalScale = 0.003;
        final double cutoff = 0.75;
        final double rescale = 7.2;

        Noise2D hotspots = new OpenSimplex2D(seed).map(y -> {
            y = y > cutoff ? y - cutoff : 0;
            y = (y * rescale);
            return y;
        }).octaves(3).spread(horizontalScale);
        return hotspots;
    }

    // Second location in a hotspot chain
    public static Noise2D dormantHotSpots(long seed)
    {
        return activeHotSpots(seed).hotSpotWarp(plateRegions(seed), 1024, 0).map(y -> Math.max(y - 0.1, 0) * 1.111);
    }

    // Third location in a hotspot chain
    public static Noise2D extinctHotSpots(long seed)
    {
        return activeHotSpots(seed).hotSpotWarp(plateRegions(seed), 2048, 0.25).map(y -> Math.max(y - 0.2, 0) * 1.25);
    }

    // Fourth location in a hotspot chain
    public static Noise2D ancientHotSpots(long seed)
    {
        return activeHotSpots(seed).hotSpotWarp(plateRegions(seed), 3072, 0.5).map(y -> Math.max(y - 0.3, 0) * 1.4286);
    }

    // Intensity of hotspots, regardless of age
    public static Noise2D hotSpotIntensity(long seed)
    {
        return activeHotSpots(seed).max(dormantHotSpots(seed)).max(extinctHotSpots(seed)).max(ancientHotSpots(seed));
    }

    // This takes noise maps of each of the age categories of hotspots, and maps which one is dominant at every location in the world
    public static Noise2D hotSpotAge(long seed)
    {
        Noise2D active = activeHotSpots(seed);
        Noise2D dormant = dormantHotSpots(seed);
        Noise2D extinct = extinctHotSpots(seed);
        Noise2D ancient = ancientHotSpots(seed);

        return active.mapAges(dormant, extinct, ancient);
    }

    // No related to the region generator cells, used for hot spot directions
    public static Cellular2D plateRegions(long seed)
    {
        return new Cellular2D(seed).spread(0.00590625f / Units.CELL_WIDTH_IN_GRID);
    }

    /**
     * Adds volcanoes to a base noise height map
     */
    public static Noise2D addVolcanoes(long seed, Noise2D baseNoise, int rarity, int baseVolcanoHeight, int scaleVolcanoHeight, boolean onShieldVolcano)
    {
        final VolcanoNoise volcanoes = new VolcanoNoise(seed);
        return (x, z) -> onShieldVolcano ? volcanoes.modifyShieldVolcanoHeight(x, z, baseNoise.noise(x, z), rarity, baseVolcanoHeight, scaleVolcanoHeight) : volcanoes.modifyHeight(x, z, baseNoise.noise(x, z), rarity, baseVolcanoHeight, scaleVolcanoHeight);
    }

    /**
     * Adds volcanoes to a base noise height map
     */
    public static Noise2D addTuffRings(long seed, Noise2D baseNoise, int rarity, int baseRingHeight, int scaleRingHeight)
    {
        final TuffRingNoise rings = new TuffRingNoise(seed);
        return (x, z) -> rings.modifyHeight(x, z, baseNoise, rarity, baseRingHeight, scaleRingHeight, seed);
    }

    /**
     * Adds tuya volcanoes to a base noise height map
     */
    public static Noise2D addTuyas(long seed, Noise2D baseNoise, int rarity, int baseVolcanoHeight, int scaleVolcanoHeight, boolean icy)
    {
        final TuyaNoise tuyas = new TuyaNoise(seed);
        return (x, z) -> tuyas.modifyHeight(x, z, baseNoise.noise(x, z), rarity, baseVolcanoHeight, scaleVolcanoHeight, icy);
    }

    public static BiomeNoiseSampler undergroundLakes(long seed, Noise2D heightNoise)
    {
        final Noise2D blobsNoise = new OpenSimplex2D(seed + 1).spread(0.04f).abs();
        final Noise2D depthNoise = new OpenSimplex2D(seed + 2).octaves(4).scaled(2, 18).spread(0.2f);
        final Noise2D centerNoise = new OpenSimplex2D(seed + 3).octaves(2).spread(0.06f).scaled(SEA_LEVEL_Y - 4, SEA_LEVEL_Y + 4);

        return new BiomeNoiseSampler()
        {
            private double surfaceHeight, center, height;

            @Override
            public void setColumn(int x, int z)
            {
                double h0 = Mth.clamp((0.7f - blobsNoise.noise(x, z)) * (1 / 0.3f), 0, 1);
                double h1 = depthNoise.noise(x, z);

                surfaceHeight = heightNoise.noise(x, z);
                center = centerNoise.noise(x, z);
                height = h0 * h1;
            }

            @Override
            public double height()
            {
                return surfaceHeight;
            }

            @Override
            public double noise(int y)
            {
                double delta = Math.abs(center - y);
                return Mth.clamp(0.4f + 0.05f * (height - delta), 0, 1);
            }
        };
    }
}
