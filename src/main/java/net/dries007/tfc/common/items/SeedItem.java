package net.dries007.tfc.common.items;

import java.util.function.Supplier;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.common.blocks.crop.Crop;
import net.dries007.tfc.util.climate.ClimateRange;

public class SeedItem extends ItemNameBlockItem implements PlantableInfo
{
    private final PlantNutrients nutrients;
    private final Supplier<ClimateRange> climateRange;

    public SeedItem(Crop crop, Block block, Properties properties)
    {
        super(block, properties);
        nutrients = new PlantNutrients(crop.getNitrogen(), crop.getPhosphorous(), crop.getPotassium());
        climateRange = crop.getClimateRange();
    }

    @Override
    public @Nullable PlantableInfo.PlantNutrients getNutrientsInfo()
    {
        return nutrients;
    }

    @Override
    public @Nullable ClimateRange getClimateRangeInfo()
    {
        return climateRange.get();
    }
}
