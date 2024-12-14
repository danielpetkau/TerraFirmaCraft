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

import static net.dries007.tfc.world.layer.TFCLayers.*;

public enum IceSheetEdgeLayer implements AdjacentTransformLayer
{
    INSTANCE;

    @Override
    public int apply(AreaContext context, int north, int east, int south, int west, int center)
    {
        final Predicate<IntPredicate> matcher = p -> p.test(north) || p.test(east) || p.test(south) || p.test(west);

        // Add ice sheet edges, replacing the ice sheet
//        if (isFlatIceSheet(center))
//        {
//            // TODO: Cleanup, move these test methods to unified locations
//            if ((matcher.test(IceSheetEdgeLayer::isNotIceSheetOrGlaciated)))
//            {
//                return ICE_SHEET_EDGE;
//            }
//        }
        if (center == KNOB_AND_KETTLE || center == PATTERNED_GROUND)
        {
            if ((matcher.test(TFCLayers::isFlatIceSheet)))
            {
                return ICE_SHEET_EDGE;
            }
        }

        // Ice sheet mountain edges
        if (center == ICE_SHEET_OCEANIC_MOUNTAINS)
        {
            if (matcher.test(IceSheetEdgeLayer::isNotIceSheet))
            {
                return ICE_SHEET_OCEANIC_MOUNTAINS_EDGE;
            }
        }
        if (center == ICE_SHEET_MOUNTAINS)
        {
            if (matcher.test(IceSheetEdgeLayer::isNotIceSheet))
            {
                return ICE_SHEET_MOUNTAINS_EDGE;
            }
        }

        if (center == ICE_SHEET_EDGE && matcher.test(i -> i == ICE_SHEET_MOUNTAINS || i == ICE_SHEET_MOUNTAINS_EDGE || i == ICE_SHEET_OCEANIC_MOUNTAINS || i == ICE_SHEET_OCEANIC))
        {
            return KNOB_AND_KETTLE;
        }

        if (isFlatIceSheet(center) && matcher.test(i -> i == OCEAN || i == OCEAN_REEF || i == DEEP_OCEAN || i == DEEP_OCEAN_TRENCH || i == ICE_SHEET_SHORE))
        {
            return ICE_SHEET_OCEANIC;
        }

        // Glaciated mountains should have glacially carved edges to avoid cirque glaciers turning to stone near borders with lower biomes
        if (isNotIceSheetOrGlaciated(center))
        {
            if (matcher.test(i -> i == GLACIATED_MOUNTAINS))
            {
                return GLACIALLY_CARVED_MOUNTAINS;
            }
            else if (matcher.test(i -> i == GLACIATED_OCEANIC_MOUNTAINS))
            {
                return GLACIALLY_CARVED_OCEANIC_MOUNTAINS;
            }
        }

        return center;
    }

    public static boolean isNotValidIceSheetEdgeBorder(int value)
    {
        return isNotIceSheet(value) && value != KNOB_AND_KETTLE && value != PATTERNED_GROUND && value != ICE_SHEET_SHORE && !isOcean(value);
    }

    public static boolean isNotIceSheet(int value)
    {
        return value != ICE_SHEET && value != ICE_SHEET_TUYAS && value != SUBGLACIAL_LAKE && value != ICE_SHEET_MOUNTAINS && value != ICE_SHEET_OCEANIC_MOUNTAINS
            && value != ICE_SHEET_SHIELD_VOLCANO && value != ICE_SHEET_ACTIVE_SHIELD_VOLCANO;
    }

    public static boolean isNotIceSheetOrGlaciated(int value)
    {
        return isNotIceSheet(value) && value != GLACIATED_MOUNTAINS && value != GLACIATED_OCEANIC_MOUNTAINS && value != GLACIATED_SHIELD_VOLCANO && value != GLACIATED_ACTIVE_SHIELD_VOLCANO;
    }
}