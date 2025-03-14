/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.biome;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.LongFunction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.world.BiomeNoiseSampler;
import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.TFCChunkGenerator;
import net.dries007.tfc.world.noise.Noise2D;
import net.dries007.tfc.world.river.RiverBlendType;
import net.dries007.tfc.world.surface.builder.SurfaceBuilderFactory;
import net.dries007.tfc.world.surface.builder.TuffRingsSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.TuyasSurfaceBuilder;
import net.dries007.tfc.world.surface.builder.VolcanoesSurfaceBuilder;
import net.dries007.tfc.world.Seed;

import static net.dries007.tfc.world.TFCChunkGenerator.*;

public class BiomeBuilder
{
    public static BiomeBuilder builder()
    {
        return new BiomeBuilder();
    }

    @Nullable private Function<Seed, Noise2D> heightNoiseFactory;
    @Nullable private Function<Seed, BiomeNoiseSampler> noiseFactory;
    @Nullable private SurfaceBuilderFactory surfaceBuilderFactory;

    private AquiferLookahead aquiferSurfaceHeight;
    private BiomeBlendType biomeBlendType;
    private RiverBlendType riverBlendType;
    private boolean salty;
    private boolean volcanic;
    private boolean hasTuyas;
    private boolean hasTuffRings;
    private int volcanoFrequency;
    private int volcanoBasaltHeight;
    private int tuffRingFrequency;
    private int tuyaFrequency;
    private boolean spawnable;
    private boolean rivers;
    private boolean shore;
    private int cliffBaseHeight;
    private boolean sandyRiverShores;

    private BiomeBuilder()
    {
        aquiferSurfaceHeight = (sampler, x, z) -> {
            sampler.setColumn(x, z);
            return sampler.height();
        };
        biomeBlendType = BiomeBlendType.LAND;
        riverBlendType = RiverBlendType.NONE;
        salty = false;
        volcanic = false;
        hasTuffRings = false;
        hasTuyas = false;
        volcanoFrequency = 0;
        tuffRingFrequency = 0;
        tuyaFrequency = 0;
        volcanoBasaltHeight = 0;
        spawnable = false;
        rivers = true;
        shore = false;
        cliffBaseHeight = 0; // Above/below sea level
        sandyRiverShores = true;
    }

    public BiomeBuilder heightmap(LongFunction<Noise2D> heightNoiseFactory)
    {
        this.heightNoiseFactory = seed -> heightNoiseFactory.apply(seed.seed());
        this.noiseFactory = seed -> BiomeNoiseSampler.fromHeightNoise(heightNoiseFactory.apply(seed.seed()));
        return this;
    }

    public BiomeBuilder surface(SurfaceBuilderFactory surfaceBuilderFactory)
    {
        this.surfaceBuilderFactory = surfaceBuilderFactory;
        return this;
    }

    public BiomeBuilder carving(BiFunction<Long, Noise2D, BiomeNoiseSampler> carvingNoiseFactory)
    {
        Objects.requireNonNull(heightNoiseFactory, "Height noise must not be null");

        final Function<Seed, Noise2D> baseHeightNoiseFactory = heightNoiseFactory;
        this.noiseFactory = seed -> carvingNoiseFactory.apply(seed.seed(), baseHeightNoiseFactory.apply(seed));
        this.aquiferSurfaceHeight = (sampler, x, z) -> TFCChunkGenerator.SEA_LEVEL_Y - 16; // Expect sea level carving to restrict aquifers

        return this;
    }

    public BiomeBuilder noise(LongFunction<BiomeNoiseSampler> noiseFactory)
    {
        this.noiseFactory = seed -> noiseFactory.apply(seed.seed());
        return this;
    }

    public BiomeBuilder aquiferHeightOffset(final double delta)
    {
        return aquiferHeight((sampler, x, z) -> {
            sampler.setColumn(x, z);
            return sampler.height() + delta;
        });
    }

    public BiomeBuilder aquiferHeight(AquiferLookahead aquiferSurfaceHeight)
    {
        this.aquiferSurfaceHeight = aquiferSurfaceHeight;
        return this;
    }

    public BiomeBuilder type(BiomeBlendType type)
    {
        this.biomeBlendType = type;
        return this;
    }

    public BiomeBuilder type(RiverBlendType type)
    {
        this.riverBlendType = type;
        if (type == RiverBlendType.CAVE)
            this.sandyRiverShores = false;
        return this;
    }

    public BiomeBuilder salty()
    {
        this.salty = true;
        return this;
    }

    public BiomeBuilder spawnable()
    {
        this.spawnable = true;
        return this;
    }

    public BiomeBuilder noRivers()
    {
        this.rivers = false;
        this.sandyRiverShores = false;
        return this;
    }

    public BiomeBuilder noSandyRiverShores()
    {
        this.sandyRiverShores = false;
        return this;
    }

    public BiomeBuilder setCliffBaseHeight(int cliffBaseHeight)
    {
        this.cliffBaseHeight = cliffBaseHeight;
        return this;
    }

    public BiomeBuilder shore()
    {
        this.shore = true;
        return this;
    }

    public BiomeBuilder volcanoes(int frequency, int baseHeight, int scaleHeight, int volcanoBasaltHeight)
    {
        return volcanoes(frequency, baseHeight, scaleHeight, volcanoBasaltHeight, false);
    }

    public BiomeBuilder volcanoes(int frequency, int baseHeight, int scaleHeight, int volcanoBasaltHeight, boolean additive)
    {
        this.volcanic = true;
        this.volcanoFrequency = frequency;
        this.volcanoBasaltHeight = SEA_LEVEL_Y + volcanoBasaltHeight;

        assert heightNoiseFactory != null : "volcanoes must be called after setting a heightmap";
        assert surfaceBuilderFactory != null : "volcanoes must be called after setting a surface builder";

        final Function<Seed, Noise2D> baseHeightNoiseFactory = this.heightNoiseFactory;
        this.heightNoiseFactory = seed -> BiomeNoise.addVolcanoes(seed, baseHeightNoiseFactory.apply(seed), frequency, baseHeight, scaleHeight, additive);

        this.noiseFactory = seed -> BiomeNoiseSampler.fromHeightNoise(heightNoiseFactory.apply(seed));

        this.surfaceBuilderFactory = VolcanoesSurfaceBuilder.create(surfaceBuilderFactory);

        return this;
    }

    public BiomeBuilder tuffRings(int frequency, int baseHeight, int scaleHeight)
    {
        this.hasTuffRings = true;
        this.tuffRingFrequency = frequency;

        assert heightNoiseFactory != null : "tuff rings must be called after setting a heightmap";
        assert surfaceBuilderFactory != null : "volcanoes must be called after setting a surface builder";

        final Function<Seed, Noise2D> baseHeightNoiseFactory = this.heightNoiseFactory;
        this.heightNoiseFactory = seed -> BiomeNoise.addTuffRings(seed, baseHeightNoiseFactory.apply(seed), frequency, baseHeight, scaleHeight);
        this.noiseFactory = seed -> BiomeNoiseSampler.fromHeightNoise(heightNoiseFactory.apply(seed));

        this.surfaceBuilderFactory = TuffRingsSurfaceBuilder.create(surfaceBuilderFactory);

        return this;
    }

    public BiomeBuilder tuyas(int frequency, int baseHeight, int scaleHeight, int volcanoBasaltHeight, boolean icy)
    {
        this.hasTuyas = true;
        this.tuyaFrequency = frequency;
        this.volcanoBasaltHeight = SEA_LEVEL_Y + volcanoBasaltHeight;

        assert heightNoiseFactory != null : "volcanoes must be called after setting a heightmap";
        assert surfaceBuilderFactory != null : "volcanoes must be called after setting a surface builder";

        final Function<Seed, Noise2D> baseHeightNoiseFactory = this.heightNoiseFactory;
        this.heightNoiseFactory = seed -> BiomeNoise.addTuyas(seed, baseHeightNoiseFactory.apply(seed), frequency, baseHeight, scaleHeight, icy);
        this.noiseFactory = seed -> BiomeNoiseSampler.fromHeightNoise(heightNoiseFactory.apply(seed));

        this.surfaceBuilderFactory = TuyasSurfaceBuilder.create(surfaceBuilderFactory);

        return this;
    }

    public BiomeExtension build(ResourceKey<Biome> key)
    {
        assert surfaceBuilderFactory != null : "missing surface builder";

        return new BiomeExtension(key, noiseFactory, surfaceBuilderFactory, aquiferSurfaceHeight, biomeBlendType, riverBlendType, salty, volcanic, hasTuffRings, hasTuyas, volcanoFrequency, volcanoBasaltHeight, tuffRingFrequency, tuyaFrequency, spawnable, rivers, shore, cliffBaseHeight, sandyRiverShores);
    }
}
