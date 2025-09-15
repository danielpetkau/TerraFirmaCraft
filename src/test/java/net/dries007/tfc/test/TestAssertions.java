/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import net.dries007.tfc.common.component.fluid.FluidComponent;
import net.dries007.tfc.test.gametest.MyTest;
import net.dries007.tfc.util.Helpers;

import static net.dries007.tfc.TerraFirmaCraft.*;

public class TestAssertions extends Assertions
{
    public static void assertEquals(FluidComponent expected, @Nullable FluidComponent actual)
    {
        assertNotNull(actual);
        if (!FluidStack.matches(expected.content(), actual.content())) fail("expected: " + expected + ", actual: " + actual);
    }

    public static void assertEquals(FluidStack expected, FluidStack actual)
    {
        if (!FluidStack.matches(expected, actual)) fail("expected: " + expected + ", actual: " + actual);
    }

    public static void assertEquals(ItemStack expected, ItemStack actual)
    {
        if (!ItemStack.matches(expected, actual)) fail("expected: " + expected + ", actual: " + actual);
    }

    public static void assertNotEquals(ItemStack expected, ItemStack actual)
    {
        if (ItemStack.matches(expected, actual)) fail("expected: " + expected + ", actual: " + actual);
    }

    public static Collection<TestFunction> testGenerator()
    {
        return testGenerator(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass());
    }

    static Collection<TestFunction> testGenerator(Class<?> clazz)
    {
        try
        {
            final List<TestFunction> functions = new ArrayList<>();
            final String className = clazz.getSimpleName();
            final Object instance = clazz.getDeclaredConstructor().newInstance();
            for (Method method : clazz.getDeclaredMethods())
            {
                if (method.isAnnotationPresent(MyTest.class))
                {
                    final MyTest annotation = method.getAnnotation(MyTest.class);
                    final String methodName = method.getName();

                    final boolean isStatic = Modifier.isStatic(method.getModifiers());
                    final boolean isNoArg = method.getParameterCount() == 0;
                    final Function<GameTestHelper, Object[]> args;

                    if (isNoArg)
                    {
                        args = helper -> new Object[0];
                    }
                    else if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == GameTestHelper.class)
                    {
                        args = helper -> new Object[]{ helper };
                    }
                    else
                    {
                        LOGGER.error("Incompatible parameter types {} for @MyTest method {}. This method will be skipped!", Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toList(), methodName);
                        continue;
                    }

                    final Consumer<GameTestHelper> action = helper -> {
                        try
                        {
                            GameTestAssertions.setHelper(helper);
                            method.invoke(isStatic ? null : instance, args.apply(helper));
                        }
                        catch (InvocationTargetException e)
                        {
                            if (e.getTargetException() instanceof AssertionError ae)
                            {
                                throw ae;
                            }
                            if (e.getTargetException() instanceof GameTestAssertException ae)
                            {
                                throw ae;
                            }
                            throwAsUnchecked(e);
                        }
                        catch (IllegalAccessException e)
                        {
                            throwAsUnchecked(e);
                        }
                    };

                    // N.B. Test Names
                    // Mojang uses the test name very particularly
                    // - The test name is checked against the class name, by matching ignoring case with "ClassName."
                    // - The test name must be parse-able as a ResourceLocation, to be stored in the structure block, which is used
                    //   for /test runthis to work later
                    // - It is also used for display purposes
                    //
                    // Unfortunately, these requirements lead to very ugly, hard-to-read display names using standard Java naming conventions.
                    // Avoiding that would require a mixin or multiple to change these behaviors. So for now, we're accepting that /test runthis
                    // won't work, and taking readable names that also work with /test runall <class name>
                    final Consumer<GameTestHelper> testAction = annotation.unitTest() ? asUnitTest(className, methodName, action) : action;
                    final String testName = className + "." + methodName;

                    functions.add(new TestFunction("default", testName, Helpers.identifier(annotation.structure()).toString(), annotation.timeoutTicks(), annotation.setupTicks(), true, testAction));
                }
            }
            functions.sort(Comparator.comparing(TestFunction::testName));
            return functions;
        }
        catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e)
        {
            return throwAsUnchecked(e);
        }
    }

    /**
     * Invoked from a {@link net.minecraft.gametest.framework.GameTest} method.
     * Treats the contents as a JUnit unit test, where an assertion failing indicates a failing test, otherwise the test succeeds.
     */
    public static Consumer<GameTestHelper> asUnitTest(String className, String methodName, Consumer<GameTestHelper> action)
    {
        return helper -> {
            try
            {
                action.accept(helper);
                helper.succeed();
            }
            catch (AssertionError e)
            {
                LOGGER.error("AutoGameTest {}.{}() failed: {}", className, methodName, e.getMessage());
                LOGGER.error("Error", e);
                helper.fail("Assertion Failed: " + e.getMessage());
            }
        };
    }

    public static Type wrap(ItemStack stack)
    {
        record TItemStack(Item item, int count, DataComponentMap tag) {}
        return new Named<>(new TItemStack(stack.getItem(), stack.getCount(), stack.getComponents()), stack + stack.getComponents().toString());
    }

    public static Type wrap(Ingredient ingredient)
    {
        record TIngredient(Class<?> clazz, @Nullable ICustomIngredient serializer, List<Type> stacks) implements Type {}
        return new TIngredient(ingredient.getClass(), ingredient.getCustomIngredient(), wrap(ingredient.getItems(), TestAssertions::wrap));
    }

    public static Type wrap(Recipe<?> recipe)
    {
        record TRecipe(Class<?> clazz, String group, Type result, List<Type> ingredients) implements Type {}
        return new Named<>(new TRecipe(recipe.getClass(), recipe.getGroup(), wrap(recipe.getResultItem(ServerLifecycleHooks.getCurrentServer().registryAccess())), wrap(recipe.getIngredients(), TestAssertions::wrap)), "[Recipe of type " + recipe.getType() + " and serializer " + BuiltInRegistries.RECIPE_SERIALIZER.getKey(recipe.getSerializer()) + "]");
    }


    private static <T> List<Type> wrap(T[] array, Function<T, Type> wrap)
    {
        return Arrays.stream(array).map(wrap).toList();
    }

    private static <T> List<Type> wrap(List<T> list, Function<T, Type> wrap)
    {
        return list.stream().map(wrap).toList();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable, T> T throwAsUnchecked(Exception e) throws E
    {
        LOGGER.error("Unhandled Exception Thrown", e);
        throw (E) e;
    }

    interface Type {}

    record Named<T>(T t, String name) implements Type
    {
        @Override
        public String toString()
        {
            return name;
        }
    }
}
