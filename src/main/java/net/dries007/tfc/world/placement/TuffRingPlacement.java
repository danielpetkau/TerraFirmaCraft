/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.TuffRingNoise;

public class TuffRingPlacement extends CenterOrDistanceToPlacement<TuffRingNoise>
{
    public static final MapCodec<TuffRingPlacement> CODEC = codec(TuffRingPlacement::new);

    public TuffRingPlacement(boolean center, float distance)
    {
        super(center, distance);
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return TFCPlacements.TUFF_RING.get();
    }

    @Override
    protected TuffRingNoise createContext(Seed seed)
    {
        return new TuffRingNoise(seed);
    }
}
