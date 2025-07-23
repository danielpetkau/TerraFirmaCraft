/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.mixin;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.WaterFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.dries007.tfc.common.fluids.TFCFluids;

@Mixin(WaterFluid.class)
public class WaterMixin
{
    @Inject(method = "isSame", at = @At("HEAD"), cancellable = true)
    private void inject$isSame(Fluid fluid, CallbackInfoReturnable<Boolean> cir)
    {
        cir.setReturnValue(fluid == Fluids.WATER || fluid == Fluids.FLOWING_WATER || fluid == TFCFluids.RIVER_WATER.get()
            || fluid == TFCFluids.SALT_WATER.getSource() || fluid == TFCFluids.SALT_WATER.getFlowing());
    }
}
