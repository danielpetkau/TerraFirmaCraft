package net.dries007.tfc.common.items;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.client.ClientHelpers;
import net.dries007.tfc.common.blocks.plant.fruit.Lifecycle;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.ClimateRange;

public interface PlantableInfo
{
    static void addTooltipInfo(ItemStack stack, Consumer<Component> tooltip)
    {
        if (stack.getItem() instanceof PlantableInfo plantable)
        {
            if (!ClientHelpers.hasShiftDown())
            {
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.hold_shift").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
                return;
            }
            boolean addedText = false;

            // Growth speed info
            int growTicks = plantable.getGrowthTimeInfo();
            if (growTicks > 0)
            {
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.growth_speed"));
                tooltip.accept(indent(Calendars.get().getTimeDelta(growTicks).withStyle(ChatFormatting.GREEN), 1));
                addedText = true;
            }

            // Climate info
            ClimateRange climate = plantable.getClimateRangeInfo();
            if (climate != null)
            {
                if (addedText) tooltip.accept(Component.empty());
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.climate"));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.plantable.climate.temperature", climate.minTemperature(), climate.maxTemperature()), 1));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.plantable.climate.hydration", climate.minHydration(), climate.maxHydration()), 1));
                addedText = true;
            }

            // Nutrient info
            PlantNutrients nutrients = plantable.getNutrientsInfo();
            if (nutrients != null)
            {
                if (addedText) tooltip.accept(Component.empty());
                final float n = nutrients.nitrogen(), p = nutrients.phosphorus(), k = nutrients.potassium();
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.nutrients"));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.fertilizer.nitrogen", formatNutrientAmount(n)), 1));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.fertilizer.phosphorus", formatNutrientAmount(p)), 1));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.fertilizer.potassium", formatNutrientAmount(k)), 1));
                addedText = true;
            }

            // Lifecycle info
            Lifecycle[] lifecycle = plantable.getLifecycleInfo();
            if (lifecycle != null)
            {
                if (addedText) tooltip.accept(Component.empty());
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.lifecycle"));
                MutableComponent component = Component.empty();
                for (Month month : Month.values())
                {
                    int index = month.ordinal();
                    if (index > lifecycle.length)
                    {
                        break;
                    }
                    int color = getLifecycleColor(lifecycle[index]);
                    component.append(Component.translatable(month.getTranslationKey(Month.Style.SHORT_MONTH)).withColor(color).append(" "));
                }
                tooltip.accept(indent(component, 1));
                tooltip.accept(Component.empty());
                for (Lifecycle stage : Lifecycle.values())
                {
                    tooltip.accept(indent(Component.translatable("tfc.tooltip.plantable.lifecycle." + stage.getSerializedName()).withColor(getLifecycleColor(stage)), 2));
                }
            }

        }
    }

    // Colors taken from the patchouli lifecycle chart for fruit trees / bushes
    private static int getLifecycleColor(Lifecycle stage)
    {
        return switch (stage)
        {
            case DORMANT -> 0xA8986A;
            case HEALTHY -> 0x6AB553;
            case FLOWERING -> 0xCCA0DB;
            case FRUITING -> 0xA217FF;
        };
    }

    private static Component indent(Component component, int amount)
    {
        return Component.literal(" ".repeat(amount)).append(component);
    }

    private static String formatNutrientAmount(float value)
    {
        if (value < 0)
        {
            // Crops that restore nutrients restore 30% of actual value by default with no fertilizer
            return String.format("+%.0f", Math.abs(value) * 0.3 * 100);
        }
        return String.format("%.0f", value * 100);
    }

    default @Nullable PlantableInfo.PlantNutrients getNutrientsInfo()
    {
        return null;
    }

    default @Nullable ClimateRange getClimateRangeInfo()
    {
        return null;
    }

    default @Nullable Lifecycle[] getLifecycleInfo()
    {
        return null;
    }

    default int getGrowthTimeInfo()
    {
        return -1;
    }

    record PlantNutrients(float nitrogen, float phosphorus, float potassium) {}
}
