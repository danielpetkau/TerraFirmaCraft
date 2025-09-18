/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.util.events;

import net.neoforged.bus.api.Event;

import net.dries007.tfc.common.component.food.INutritionData;
import net.dries007.tfc.common.player.PlayerInfo;

/**
 * An event that serves as a hook for addons to change the implementation of {@link INutritionData} they want {@link PlayerInfo} to use
 */
public class NutritionDataEvent extends Event
{
    private PlayerInfo.NutritionDataSupplier<INutritionData> supplier;

    public NutritionDataEvent(PlayerInfo.NutritionDataSupplier<INutritionData> supplier)
    {
        this.supplier = supplier;
    }

    public PlayerInfo.NutritionDataSupplier<INutritionData> getSupplier()
    {
        return this.supplier;
    }

    public void setSupplier(PlayerInfo.NutritionDataSupplier<INutritionData> supplier)
    {
        this.supplier = supplier;
    }
}
