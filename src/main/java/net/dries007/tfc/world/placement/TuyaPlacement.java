/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;

import net.dries007.tfc.world.Seed;
import net.dries007.tfc.world.biome.TuyaNoise;

public class TuyaPlacement extends CenterOrDistanceToPlacement<TuyaNoise>
{
    public static final MapCodec<TuyaPlacement> CODEC = codec(TuyaPlacement::new);

    public TuyaPlacement(boolean center, float distance)
    {
        super(center, distance);
    }

    @Override
    public PlacementModifierType<?> type()
    {
        return TFCPlacements.TUYA.get();
    }

    @Override
    protected TuyaNoise createContext(Seed seed)
    {
        return new TuyaNoise(seed);
    }
}
