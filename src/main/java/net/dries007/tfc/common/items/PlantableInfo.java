package net.dries007.tfc.common.items;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

            // Climate info
            ClimateRange climate = plantable.getClimateRangeInfo();
            if (climate != null)
            {
                if (addedText) tooltip.accept(Component.empty());
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.climate").withStyle(ChatFormatting.GRAY));
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
                boolean consumesNutrients = n > 0 || p > 0 || k > 0;
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.nutrients").withStyle(ChatFormatting.GRAY));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.fertilizer.nitrogen", formatNutrientAmount(n, consumesNutrients)), 1));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.fertilizer.phosphorus", formatNutrientAmount(p, consumesNutrients)), 1));
                tooltip.accept(indent(Component.translatable("tfc.tooltip.fertilizer.potassium", formatNutrientAmount(k, consumesNutrients)), 1));
                addedText = true;
            }

            // Lifecycle info
            @Nullable Lifecycle[] lifecycle = plantable.getLifecycleInfo();
            int growTicks = plantable.getGrowthTimeInfo();

            boolean addGrowInfo = growTicks > 0;
            boolean addLifecycleInfo = lifecycle != null;
            if (addGrowInfo || addLifecycleInfo)
            {
                if (addedText) tooltip.accept(Component.empty());
                tooltip.accept(Component.translatable("tfc.tooltip.plantable.lifecycle").withStyle(ChatFormatting.GRAY));
            }
            if (addGrowInfo)
            {
                tooltip.accept(
                    indent(
                        Component.translatable("tfc.tooltip.plantable.lifecycle.growth_speed")
                            .withStyle(ChatFormatting.GREEN)
                            .append(" ")
                            .append(
                                Calendars.get().getTimeDelta(growTicks).withStyle(ChatFormatting.WHITE)
                            ),
                        1
                    )
                );
            }
            if (addLifecycleInfo)
            {
                Month growMonth = null;
                Month fruitMonth = null;
                for (int index = Month.DECEMBER.ordinal(); index > Month.JANUARY.ordinal(); index--)
                {
                    Month month = Month.valueOf(index);
                    Lifecycle stage = lifecycle[index];
                    growMonth = findEarliestMonth(Lifecycle.HEALTHY, stage, growMonth, month);
                    fruitMonth = findEarliestMonth(Lifecycle.FRUITING, stage, fruitMonth, month);

                }

                if (growMonth != null)
                {
                    tooltip.accept(
                        indent(
                            Component.translatable("tfc.tooltip.plantable.lifecycle.healthy")
                                .withColor(0x6AB553) // Healthy color (green) from the patchouli lifecycle chart
                                .append(" ")
                                .append(
                                    Component.translatable(growMonth.getTranslationKey(Month.Style.SEASON)).withStyle(ChatFormatting.WHITE)
                                ),
                            1
                        )
                    );
                }
                if (fruitMonth != null)
                {
                    tooltip.accept(
                        indent(
                            Component.translatable("tfc.tooltip.plantable.lifecycle.fruiting")
                                .withColor(0xA217FF) // Fruiting color (purple) from the patchouli lifecycle chart
                                .append(" ")
                                .append(
                                    Component.translatable(fruitMonth.getTranslationKey(Month.Style.SEASON)).withStyle(ChatFormatting.WHITE)
                                ),
                            1
                        )
                    );
                }
            }
        }
    }

    private static @Nullable Month findEarliestMonth(Lifecycle targetStage, Lifecycle currentStage, @Nullable Month foundMonth, Month currentMonth)
    {
        if (currentStage == targetStage)
        {
            if (foundMonth == null)
            {
                return currentMonth;
            }
            else
            {
                if (currentMonth.next().equals(foundMonth))
                {
                    return currentMonth;
                }
            }
        }
        return foundMonth;
    }

    private static Component indent(Component component, int amount)
    {
        return Component.literal(" ".repeat(amount)).append(component);
    }

    private static String formatNutrientAmount(float value, boolean consumesNutrients)
    {
        if (value < 0)
        {
            if (consumesNutrients)
            {
                return String.format("+%.0f", Math.abs(value) * 100);
            }
            // Crops that restore nutrients restore 30% of actual value if they do not consume nutrients
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
