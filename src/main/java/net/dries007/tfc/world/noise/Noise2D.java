/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.noise;

import java.util.function.DoubleUnaryOperator;
import net.minecraft.util.Mth;

/**
 * Wrapper for a 2D noise layer
 */
@FunctionalInterface
public interface Noise2D
{
    double noise(double x, double z);

    /**
     * @param octaves The number of octaves
     */
    default Noise2D octaves(int octaves)
    {
        final double[] frequency = new double[octaves];
        final double[] amplitude = new double[octaves];
        for (int i = 0; i < octaves; i++)
        {
            frequency[i] = 1 << i;
            amplitude[i] = (double) Math.pow(0.5f, octaves - i);
        }
        return (x, y) -> {
            double value = 0;
            for (int i = 0; i < octaves; i++)
            {
                value += Noise2D.this.noise(x / frequency[i], y / frequency[i]) * amplitude[i];
            }
            return value;
        };
    }

    /**
     * Creates ridged noise using absolute value
     *
     * @return a new noise function
     */
    default Noise2D ridged()
    {
        return (x, y) -> {
            double value = Noise2D.this.noise(x, y);
            value = value < 0 ? -value : value;
            return 1f - 2f * value;
        };
    }

    /**
     * Takes the absolute value of a noise function. Does not scale the result
     *
     * @return a new noise function
     */
    default Noise2D abs()
    {
        return (x, y) -> Math.abs(Noise2D.this.noise(x, y));
    }

    /**
     * Creates "terraces" by taking the nearest level and rounding
     * Input must be in range [-1, 1]
     *
     * @param levels The number of levels to round to
     * @return a new noise function
     */
    default Noise2D terraces(int levels)
    {
        return (x, y) -> {
            double value = 0.5f * Noise2D.this.noise(x, y) + 0.5f;
            double rounded = (int) (value * levels); // In range [0, levels)
            return (rounded * 2f) / levels - 1f;
        };
    }

    /**
     * Spreads out the noise via the input parameters
     *
     * @param scaleFactor The scale for the input params
     * @return a new noise function
     */
    default Noise2D spread(double scaleFactor)
    {
        return (x, y) -> Noise2D.this.noise(x * scaleFactor, y * scaleFactor);
    }

    default Noise2D scaled(double min, double max)
    {
        return scaled(-1, 1, min, max);
    }

    /**
     * Re-scales the output of the noise to a new range
     *
     * @param oldMin the old minimum value (typically -1)
     * @param oldMax the old maximum value (typically 1)
     * @param min    the new minimum value
     * @param max    the new maximum value
     * @return a new noise function
     */
    default Noise2D scaled(double oldMin, double oldMax, double min, double max)
    {
        final double scale = (max - min) / (oldMax - oldMin);
        final double shift = min - oldMin * scale;
        return affine(scale, shift);
    }

    default Noise2D affine(double scale, double shift)
    {
        return (x, y) -> Noise2D.this.noise(x, y) * scale + shift;
    }

    default Noise2D warped(OpenSimplex2D warp)
    {
        warp.fnl.SetDomainWarpType(FastNoiseLite.DomainWarpType.OpenSimplex2);
        warp.fnl.SetFractalType(FastNoiseLite.FractalType.DomainWarpIndependent);
        warp.fnl.SetDomainWarpAmp(warp.getAmplitude() * 2);
        final FastNoiseLite.Vector2 cursor = new FastNoiseLite.Vector2(0, 0);
        return (x, z) -> {
            cursor.x = x;
            cursor.y = z;
            warp.fnl.DomainWarp(cursor);
            return Noise2D.this.noise(cursor.x, cursor.y);
        };
    }

    /**
     * A domain-warp designed to warp the location of noise peaks without distorting their shapes
     * From an input value, procedurally determines a displacement vector
     *
     * @param warp              noise map to generate offsets from, designed to be used with a cellular hash map
     * @param velocityScale     first-order distance scaling
     * @param accelScale        second-order distance scaling
     * @return this noise function, with a cellular domain warp effect
     */
    default Noise2D hotSpotWarp(Noise2D warp, int velocityScale, double accelScale)
    {
        return (x, z) -> {
            // Random vector
            final double ux = warp.noise(x, z);
            // Random magnitude from pev vector by multiplying and taking modulo, random direction based on magnitude
            final double uz = (Math.abs(ux * 16) % 1 > 0.5 ? 1 : -1) * (ux * 256) % 1;

            // Increase magnitude of vector to ensure islands in the same chain don't generate on top of each other
            final int sx = ux > 0 ? 1 : -1;
            final int sz = uz > 0 ? 1 : -1;
            final double vx = (ux + sx) * velocityScale;
            final double vz = (uz + sz) * velocityScale;

            // Perpendicular acceleration vector to create curved chains
            final double ax = -(vz) * accelScale;
            final double az = vx * accelScale;

            return Noise2D.this.noise(x + vx + ax, z + vz + az);
        };
    }

    default Noise2D mapAges(Noise2D young, Noise2D old, Noise2D oldest)
    {
        return (x, z) -> {
            if (this.max(young).max(old).max(oldest).noise(x, z) <= -0.8)
            {
                return 0;
            }
            else if (this.noise(x, z) > young.noise(x, z) && this.noise(x, z) > old.noise(x, z) && this.noise(x, z) > oldest.noise(x, z))
            {
                return 1;
            }
            else if (young.noise(x, z) > this.noise(x, z) && young.noise(x, z) > old.noise(x, z) && young.noise(x, z) > oldest.noise(x, z))
            {
                return 2;
            }
            else if (old.noise(x, z) > young.noise(x, z) && old.noise(x, z) > this.noise(x, z) && old.noise(x, z) > oldest.noise(x, z))
            {
                return 3;
            }
            else if (oldest.noise(x, z) > young.noise(x, z) && oldest.noise(x, z) > old.noise(x, z) && oldest.noise(x, z) > this.noise(x, z))
            {
                return 4;
            }
            else
                return 0;
        };
    }

    /**
     * Creates clamped noise by cutting off values above or below a threshold
     *
     * @param min the minimum noise value
     * @param max the maximum noise value
     * @return a new noise function
     */
    default Noise2D clamped(double min, double max)
    {
        return (x, y) -> Mth.clamp(Noise2D.this.noise(x, y), min, max);
    }

    /**
     * Sum of two noises.
     */
    default Noise2D add(Noise2D other)
    {
        return (x, y) -> Noise2D.this.noise(x, y) + other.noise(x, y);
    }

    /**
     * Sum of a noise and a constant.
     */
    default Noise2D addConstant(double constant)
    {
        return (x, y) -> Noise2D.this.noise(x, y) + constant;
    }

    /**
     * Minimum of two noises.
     */
    default Noise2D min(Noise2D other)
    {
        return (x, y) -> Math.min(Noise2D.this.noise(x, y), other.noise(x, y));
    }

    /**
     * Maximum of two noises.
     */
    default Noise2D max(Noise2D other)
    {
        return (x, y) -> Math.max(Noise2D.this.noise(x, y), other.noise(x, y));
    }

    /**
     * Product of two noises - lazily evaluates the second if the first evaluates to zero.
     */
    default Noise2D lazyProduct(Noise2D other)
    {
        return (x, y) -> {
            final double value = Noise2D.this.noise(x, y);
            return value == 0 ? 0 : value * other.noise(x, y);
        };
    }

    default Noise2D map(DoubleUnaryOperator mappingFunction)
    {
        return (x, y) -> mappingFunction.applyAsDouble(Noise2D.this.noise(x, y));
    }

    /**
     * Used to generate varying-height cliffs starting at various noise values
     * @param compare value above which cliffs should be added
     * @param addend cliff height noise
     */
    default Noise2D cliffMap(Noise2D compare, Noise2D addend)
    {
        return (x, z) -> {
            if (Noise2D.this.noise(x, z) > compare.noise(x, z))
            {
                return Noise2D.this.noise(x, z) + addend.noise(x, z);
            }
            else
            {
                return Noise2D.this.noise(x, z);
            }
        };
    }

    /**
     * Used to generate varying-height cliffs starting at various noise values
     * @param compare value above which cliffs should be added
     * @param addend cliff height noise
     * @param scale how much to scale this noise by if cliffs are added
     */
    default Noise2D fenglinCliffMap(Noise2D compare, Noise2D addend, Noise2D scale)
    {
        return (x, z) -> {
            if (Noise2D.this.noise(x, z) > compare.noise(x, z))
            {
                return Noise2D.this.lazyProduct(scale).noise(x, z) + addend.noise(x, z);
            }
            else
            {
                return Noise2D.this.noise(x, z);
            }
        };
    }
}