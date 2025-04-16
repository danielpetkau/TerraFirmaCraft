/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.common.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.dries007.tfc.client.TFCSounds;
import net.dries007.tfc.config.TFCConfig;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.events.StartFireEvent;

public class FirestarterItem extends Item
{
    public FirestarterItem(Item.Properties properties)
    {
        super(properties);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntityIn, ItemStack stack, int countLeft)
    {
        if (livingEntityIn instanceof final Player player)
        {
            final BlockHitResult result = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);

            final BlockPos pos = result.getBlockPos();
            final BlockPos abovePos = pos.above();
            double chance = TFCConfig.SERVER.fireStarterChance.get() * (level.isRainingAt(abovePos) ? 0.3 : 1);
            if (level.isClientSide())
            {
                Vec3 location = result.getLocation();
                makeEffects(level, player, location.x(), location.y(), location.z(), countLeft, getUseDuration(stack, player), level.random);
            }
            else if (countLeft == 1)
            {
                if (!player.isCreative())
                {
                    Helpers.damageItem(stack, player, InteractionHand.MAIN_HAND);
                }
                StartFireEvent.startFire(level, pos, level.getBlockState(pos), result.getDirection(), player, stack, StartFireEvent.FireStrength.STRONG, chance);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context)
    {
        final Player player = context.getPlayer();
        if (player != null)
        {
            player.startUsingItem(context.getHand());
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack)
    {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity)
    {
        return 72;
    }

    private void makeEffects(Level world, Player player, double x, double y, double z, int countLeft, int total, RandomSource random)
    {
        int count = total - countLeft;
        if (random.nextFloat() + 0.3 < count / (double) total)
        {
            world.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0F, 0.1F, 0.0F);
        }
        if (countLeft < 10 && random.nextFloat() + 0.3 < count / (double) total)
        {
            world.addParticle(ParticleTypes.FLAME, x, y, z, 0.0F, 0.1F, 0.0F);
        }
        if (count % 3 == 1)
        {
            player.playSound(TFCSounds.FIRESTARTER.get(), 0.5F, 0.05F);
        }
    }
}
