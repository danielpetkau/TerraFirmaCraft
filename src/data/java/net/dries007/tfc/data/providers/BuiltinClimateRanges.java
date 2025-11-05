/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.data.providers;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;

import net.dries007.tfc.util.climate.ClimateRange;
import net.dries007.tfc.util.data.DataManager;

import static net.dries007.tfc.common.blocks.crop.Crop.*;
import static net.dries007.tfc.common.blocks.plant.fruit.FruitBlocks.SpreadingBush.*;
import static net.dries007.tfc.common.blocks.plant.fruit.FruitBlocks.StationaryBush.*;
import static net.dries007.tfc.common.blocks.plant.fruit.FruitBlocks.Tree.*;
import static net.dries007.tfc.util.climate.ClimateRanges.*;

public class BuiltinClimateRanges extends DataManagerProvider<ClimateRange>
{
    public BuiltinClimateRanges(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup)
    {
        super(ClimateRange.MANAGER, output, lookup);
    }

    @Override
    protected void addData(HolderLookup.Provider provider)
    {
        // DO NOT EDIT FRUITS DIRECTLY - Imported directly from spreadsheet
        // https://docs.google.com/spreadsheets/d/1USmCWiRrj5205WyoRNNTUkoNrqm9AStRzwnD1v6633o
        add(BANANA_PLANT, new ClimateRange.Builder().minHydration(25).temperature(9.8f, 41.2f).build());

        add(FRUIT_TREES, CHERRY, b -> b.hydration(7, 80).temperature(-6.4f, 15.2f));
        add(FRUIT_TREES, GREEN_APPLE, b -> b.hydration(10, 75).temperature(-11.8f, 11.6f));
        add(FRUIT_TREES, RED_APPLE, b -> b.hydration(16, 75).temperature(-11.8f, 11.6f));
        add(FRUIT_TREES, LEMON, b -> b.hydration(19, 95).temperature(6.2f, 26.f));
        add(FRUIT_TREES, OLIVE, b -> b.hydration(22, 95).temperature(0.8f, 24.2f));
        add(FRUIT_TREES, ORANGE, b -> b.hydration(27, 100).temperature(8.f, 41.2f));
        add(FRUIT_TREES, PEACH, b -> b.hydration(15, 95).temperature(-4.6f, 17.f));
        add(FRUIT_TREES, PLUM, b -> b.hydration(9, 75).temperature(-8.2f, 13.4f));


        add(SPREADING_BUSHES, BLACKBERRY, b -> b.hydration(17, 100).temperature(-6.4f, 20.6f));
        add(SPREADING_BUSHES, RASPBERRY, b -> b.hydration(15, 95).temperature(-11.8f, 15.2f));
        add(SPREADING_BUSHES, BLUEBERRY, b -> b.hydration(12, 90).temperature(-10.f, 9.8f));
        add(SPREADING_BUSHES, ELDERBERRY, b -> b.hydration(9, 85).temperature(-6.4f, 17.f));

        add(STATIONARY_BUSHES, SNOWBERRY, b -> b.hydration(17, 100).temperature(-11.8f, 6.2f));
        add(STATIONARY_BUSHES, BUNCHBERRY, b -> b.hydration(25, 100).temperature(-15.4f, 2.6f));
        add(STATIONARY_BUSHES, GOOSEBERRY, b -> b.hydration(17, 100).temperature(-8.2f, 13.4f));
        add(STATIONARY_BUSHES, CLOUDBERRY, b -> b.hydration(5, 80).temperature(-15.4f, 8f));
        add(STATIONARY_BUSHES, STRAWBERRY, b -> b.hydration(11, 90).temperature(-2.8f, 18.8f));
        add(STATIONARY_BUSHES, WINTERGREEN_BERRY, b -> b.hydration(7, 85).temperature(-10.f, 8f));

        add(CRANBERRY_BUSH, new ClimateRange.Builder().minHydration(25).temperature(-15.4f, 9.8f).build());
        // DO NOT EDIT CROPS DIRECTLY - Imported directly from spreadsheet
        // https://docs.google.com/spreadsheets/d/1USmCWiRrj5205WyoRNNTUkoNrqm9AStRzwnD1v6633o
        add(CROPS, CASSAVA, b -> b.hydration(45, 100).temperature(4, 36));
        add(CROPS, GREEN_BEAN, b -> b.hydration(25, 90).temperature(-11, 22));
        add(CROPS, LENTIL, b -> b.hydration(15, 50).temperature(-15, 22));
        add(CROPS, PEANUT, b -> b.hydration(20, 80).temperature(6, 36));
        add(CROPS, SOYBEAN, b -> b.hydration(25, 90).temperature(-17, 19));
        add(CROPS, BARLEY, b -> b.hydration(10, 70).temperature(-17, 21));
        add(CROPS, OAT, b -> b.hydration(25, 85).temperature(-17, 19));
        add(CROPS, RYE, b -> b.hydration(15, 80).temperature(-17, 13));
        add(CROPS, MAIZE, b -> b.hydration(50, 100).temperature(-17, 25));
        add(CROPS, WHEAT, b -> b.hydration(15, 85).temperature(-17, 19));
        add(CROPS, RICE, b -> b.hydration(35, 100).temperature(2, 36));
        add(CROPS, BEET, b -> b.hydration(10, 70).temperature(-21, 25));
        add(CROPS, CABBAGE, b -> b.hydration(10, 65).temperature(-21, 25));
        add(CROPS, CARROT, b -> b.hydration(15, 85).temperature(-21, 25));
        add(CROPS, GARLIC, b -> b.hydration(10, 70).temperature(-13, 19));
        add(CROPS, ONION, b -> b.hydration(15, 85).temperature(-15, 24));
        add(CROPS, POTATO, b -> b.hydration(35, 90).temperature(-17, 19));
        add(CROPS, SQUASH, b -> b.hydration(15, 85).temperature(-17, 22));
        add(CROPS, TOMATO, b -> b.hydration(20, 85).temperature(-5, 36));
        add(CROPS, RED_BELL_PEPPER, b -> b.hydration(30, 95).temperature(6, 36));
        add(CROPS, YELLOW_BELL_PEPPER, b -> b.hydration(30, 95).temperature(6, 36));
        add(CROPS, PUMPKIN, b -> b.hydration(20, 85).temperature(-17, 25));
        add(CROPS, MELON, b -> b.hydration(35, 100).temperature(-1, 36));
        add(CROPS, CANOLA, b -> b.hydration(20, 75).temperature(-35, -5));
        add(CROPS, RADISH, b -> b.hydration(30, 90).temperature(-33, -1));
        add(CROPS, ALFALFA, b -> b.hydration(40, 100).temperature(-37, -3));
        add(CROPS, JUTE, b -> b.hydration(15, 90).temperature(-5, 22));
        add(CROPS, PAPYRUS, b -> b.hydration(50, 100).temperature(6, 36));
        add(CROPS, SUGARCANE, b -> b.hydration(25, 100).temperature(12, 36));
    }

    private <T> void add(Map<T, DataManager.Reference<ClimateRange>> map, T value, UnaryOperator<ClimateRange.Builder> builder)
    {
        add(map.get(value), builder.apply(new ClimateRange.Builder()).build());
    }
}
