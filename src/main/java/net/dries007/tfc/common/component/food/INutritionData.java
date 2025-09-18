package net.dries007.tfc.common.component.food;

import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public interface INutritionData
{
    /**
     * @return The average nutrition of the player
     */
    float getAverageNutrition();

    /**
     * @return The nutrient value, in [0, 1]
     */
    float getNutrient(Nutrient nutrient);

    /**
     * @return An array of all nutrient values, in [0, 1]
     */
    float[] getNutrients();

    /**
     * Set the current {@code hunger} value of the player, in {@code [0, PlayerInfo.MAX_HUNGER]}.
     * This may update the nutrition of the player.
     */
    default void setHungerAndUpdate(int hunger)
    {
        setHunger(hunger);
    }

    /**
     * Set the current {@code hunger} value of the player, in {@code [0, PlayerInfo.MAX_HUNGER]}.
     * This <strong>must not</strong> update the nutrition of the player.
     */
    void setHunger(int hunger);

    /**
     * Sets data from a packet, received on client side. Does not contain the full data only the important information
     */
    void onClientUpdate(float[] nutrients);

    /**
     * Applies nutrients to the food data
     */
    void addNutrients(FoodData data);

    /**
     * Applies nutrients to the food data, and incorporates the current hunger level of the player
     */
    default void addNutrients(FoodData data, int currentHunger)
    {
        addNutrients(data);
    }

    /**
     * @return The relevant data written to an NBT Tag
     */
    Tag writeToNbt();

    /**
     * Reads relevant data from an NBT Tag
     */
    void readFromNbt(@Nullable Tag nbt);
}
