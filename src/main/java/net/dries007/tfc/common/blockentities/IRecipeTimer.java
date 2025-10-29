package net.dries007.tfc.common.blockentities;

import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * This is implemented in {@link BlockEntity}s with a timer based recipe, to interact with the clocks timer mode.
 */
public interface IRecipeTimer {
    int getRecipeDuration();

    long getRemainingTime();
}
