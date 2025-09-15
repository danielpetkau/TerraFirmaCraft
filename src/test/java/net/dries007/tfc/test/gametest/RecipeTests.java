/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.test.gametest;

import java.util.Collection;
import java.util.List;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import net.dries007.tfc.common.component.food.FoodCapability;
import net.dries007.tfc.common.component.food.IFood;
import net.dries007.tfc.test.TestAssertions;
import net.dries007.tfc.util.calendar.CalendarTransaction;
import net.dries007.tfc.util.calendar.Calendars;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@GameTestHolder
public class RecipeTests
{
    @GameTestGenerator
    public Collection<TestFunction> generator()
    {
        return TestAssertions.testGenerator();
    }

    @MyTest(unitTest = true)
    public void testNoRecipeMatchesEmptyGrid(GameTestHelper helper)
    {
        for (RecipeHolder<CraftingRecipe> recipe : helper.getLevel().getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING))
        {
            assertFalse(recipe.value().matches(CraftingInput.EMPTY, helper.getLevel()), "Recipe: " + recipe.id() + " of type " + recipe.value().getType() + " and serializer " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.value().getSerializer()) + " matches an empty grid");
        }
    }

    @MyTest(unitTest = true)
    public void testNoRecipeProducesRottenOutput(GameTestHelper helper)
    {
        for (RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes())
        {
            final Recipe<?> recipe = holder.value();
            // If the output is non-empty and non-rotten, then we should assert that it doesn't become rotten if we jump ahead
            final ItemStack output = getOutputOfRecipe(recipe);
            final IFood food = FoodCapability.get(output);
            if (!output.isEmpty() && food != null && !food.isRotten())
            {
                try (CalendarTransaction tr = Calendars.SERVER.transaction())
                {
                    tr.add(1_000_000_000);

                    final ItemStack oldOutput = getOutputOfRecipe(recipe);
                    final IFood oldFood = FoodCapability.get(oldOutput);

                    assertNotNull(oldFood);
                    assertFalse(oldFood.isRotten(), "Recipe: " + holder.id() + " of type " + recipe.getType() + " and serializer " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()) + " produced rotten output");
                }
            }
        }
    }

    @MyTest(unitTest = true)
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testAllRecipesEncodeAndDecode(GameTestHelper helper)
    {
        for (RecipeHolder<?> holder : helper.getLevel().getRecipeManager().getRecipes())
        {
            final Recipe<?> before = holder.value();
            final FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            final RegistryFriendlyByteBuf regBuffer = new RegistryFriendlyByteBuf(buffer, helper.getLevel().registryAccess(), ConnectionType.OTHER);

            ((RecipeSerializer) before.getSerializer()).streamCodec().mapStream(factory -> regBuffer);
            final Recipe<?> after = before.getSerializer().streamCodec().decode(regBuffer);;

            assertEquals(0, buffer.readableBytes(), "Buffer has " + buffer.readableBytes() + " remaining bytes after reading recipe: " + holder.id());
            assertEquals(before, after);
        }
    }

    @SuppressWarnings("unchecked")
    private ItemStack getOutputOfRecipe(Recipe<?> recipe)
    {
        final RegistryAccess.Frozen registryAccess = ServerLifecycleHooks.getCurrentServer().registryAccess();
        try
        {
            return ((Recipe<RecipeInput>) recipe).assemble(CraftingInput.of(1, 1, List.of(ItemStack.EMPTY)), registryAccess);
        }
        catch (Throwable t) { /* Ignore */ }
        return recipe.getResultItem(registryAccess);
    }
}
