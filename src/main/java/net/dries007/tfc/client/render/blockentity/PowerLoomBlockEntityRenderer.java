package net.dries007.tfc.client.render.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import net.dries007.tfc.client.RenderHelpers;
import net.dries007.tfc.common.blockentities.rotation.PowerLoomBlockEntity;
import net.dries007.tfc.common.blocks.wood.TFCLoomBlock;
import net.dries007.tfc.common.recipes.LoomRecipe;

public class PowerLoomBlockEntityRenderer implements BlockEntityRenderer<PowerLoomBlockEntity>
{
    @Override
    public void render(PowerLoomBlockEntity loom, float partialTick, PoseStack stack, MultiBufferSource buffer, int packedLight, int packedOverlay)
    {
        if (loom.getLevel() == null)
            return;

        stack.pushPose();
        stack.translate(0.5D, 0.03125D, 0.5D);
        int meta = loom.getBlockState().getValue(TFCLoomBlock.FACING).get2DDataValue();
        stack.mulPose(Axis.YP.rotationDegrees((float) meta));
        stack.popPose();

        final LoomRecipe recipe = loom.getRecipe();
        final ResourceLocation lastTex = loom.getLastTexture();
        final VertexConsumer cutout = buffer.getBuffer(RenderType.cutout());
        if (recipe != null || lastTex != null)
        {
            stack.pushPose();
            stack.translate(0.5D, 0.0D, 0.5D);
            stack.mulPose(Axis.YP.rotationDegrees(180.0F - 90.0F * meta));
            stack.translate(-0.5D, 0.0D, -0.5D);

            if (recipe != null)
            {
                final TextureAtlasSprite progressSprite = Minecraft.getInstance().getTextureAtlas(RenderHelpers.BLOCKS_ATLAS).apply(recipe.getInProgressTexture());

                final float fill = Mth.clampedMap(loom.getCount(), 0, recipe.getInputCount(), 2f, 14f);
                RenderHelpers.renderTexturedCuboid(stack, cutout, progressSprite, packedLight, packedOverlay, 2f / 16f, 10.25f / 16f, 0f, fill / 16f, 11.25f / 16f, 1f);

            }

            stack.popPose();

        }
    }
}
