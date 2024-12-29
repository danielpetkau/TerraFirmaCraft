/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.blocks.soil;

import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.registry.RegistrySoilVariant;

/**
 * Dirt that doesn't let grass spread to it but can still be interacted on
 */
public class TFCCoarseDirtBlock extends Block
{
    private final Supplier<? extends Block> dirt;

    public TFCCoarseDirtBlock(Properties properties, Supplier<? extends Block> dirt)
    {
        super(properties);
        this.dirt = dirt;
    }

    TFCCoarseDirtBlock(Properties properties, SoilBlockType dirtType, RegistrySoilVariant variant)
    {
        this(properties, variant.getBlock(dirtType));
    }
}
