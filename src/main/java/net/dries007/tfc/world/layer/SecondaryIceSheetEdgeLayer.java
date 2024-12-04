/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.layer;

import java.util.function.IntPredicate;
import java.util.function.Predicate;

import net.dries007.tfc.world.layer.framework.AdjacentTransformLayer;
import net.dries007.tfc.world.layer.framework.AreaContext;

import static net.dries007.tfc.world.layer.IceSheetEdgeLayer.*;
import static net.dries007.tfc.world.layer.TFCLayers.*;

public enum SecondaryIceSheetEdgeLayer implements AdjacentTransformLayer
{
    INSTANCE;

    @Override
    public int apply(AreaContext context, int north, int east, int south, int west, int center)
    {
        final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);

        // Ensure only knob and kettle topography near ice sheet edges
        if (isNotValidIceSheetEdgeBorder(center))
        {
            if (matcher.test(i -> i == ICE_SHEET_EDGE))
            {
                return KNOB_AND_KETTLE;
            }
        }

        return center;
    }
}