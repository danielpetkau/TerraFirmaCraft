/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.world.surface;

import java.util.function.Supplier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.dries007.tfc.common.blocks.SandstoneBlockType;
import net.dries007.tfc.common.blocks.TFCBlocks;
import net.dries007.tfc.common.blocks.rock.Rock;
import net.dries007.tfc.common.blocks.soil.SandBlockType;
import net.dries007.tfc.common.blocks.soil.SoilBlockType;
import net.dries007.tfc.common.fluids.TFCFluids;
import net.dries007.tfc.world.biome.TFCBiomes;
import net.dries007.tfc.world.settings.RockSettings;

public final class SurfaceStates
{
    public static final SurfaceState RAW = context -> context.getRock().raw().defaultBlockState();
    public static final SurfaceState COBBLE = context -> context.getRock().cobble().defaultBlockState();
    public static final SurfaceState GRAVEL = context -> context.getRock().gravel().defaultBlockState();

    public static final SurfaceState BASALT = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.RAW).get().defaultBlockState();
    public static final SurfaceState BASALT_COBBLE = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.COBBLE).get().defaultBlockState();
    public static final SurfaceState BASALT_GRAVEL = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.BASALT).get(Rock.BlockType.GRAVEL).get().defaultBlockState();

    public static final SurfaceState TUFF = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.TUFF).get(Rock.BlockType.RAW).get().defaultBlockState();
    public static final SurfaceState TUFF_GRAVEL = context -> TFCBlocks.ROCK_BLOCKS.get(Rock.TUFF).get(Rock.BlockType.GRAVEL).get().defaultBlockState();

    public static final SurfaceState GLACIER = context -> Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState(); // TODO: Replace
    public static final SurfaceState SNOW = context -> Blocks.WHITE_STAINED_GLASS.defaultBlockState(); // TODO: Replace
//    public static final SurfaceState GLACIER = context -> Blocks.PACKED_ICE.defaultBlockState();
//    public static final SurfaceState SNOW = context -> Blocks.SNOW_BLOCK.defaultBlockState();

    /**
     * Grass / Dirt / Gravel, or Sand / Sand / Sandstone
     */
    public static final SurfaceState GRASS = SoilSurfaceState.buildType(SoilBlockType.GRASS);
    public static final SurfaceState DIRT = SoilSurfaceState.buildType(SoilBlockType.DIRT);
    public static final SurfaceState MUD = SoilSurfaceState.buildType(SoilBlockType.MUD);
    public static final SurfaceState DRY_MUD = SoilSurfaceState.buildDryDirt(SoilBlockType.CRACKED_EARTH);
    public static final SurfaceState SALT_MUD = SoilSurfaceState.buildDryDirt(SoilBlockType.SALTED_EARTH);

    public static final SurfaceState SAND_OR_GRAVEL = SoilSurfaceState.buildSandOrGravel(false);
    public static final SurfaceState SANDSTONE_OR_GRAVEL = SoilSurfaceState.buildSandOrGravel(true);

    public static final SurfaceState RIVER_SAND = context -> context.getSeaLevelRock().sand().defaultBlockState();
    public static final SurfaceState SHORE_SAND = context -> context.getBottomRock().sand().defaultBlockState();
    public static final SurfaceState SHORE_SANDSTONE = context -> context.getBottomRock().sandstone().defaultBlockState();
    public static final SurfaceState SHORE_MUD = context -> TFCBlocks.SOIL.get(SoilBlockType.MUD).get(SoilBlockType.Variant.SANDY_LOAM).get().defaultBlockState();

    public static final SurfaceState RARE_SHORE_SAND = new SurfaceState()
    {
        private final Supplier<Block> pinkSand = TFCBlocks.SAND.get(SandBlockType.PINK);
        private final Supplier<Block> blackSand = TFCBlocks.SAND.get(SandBlockType.BLACK);

        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            if (context.groundwater() > 300f && context.averageTemperature() > 15f)
            {
                return pinkSand.get().defaultBlockState();
            }
            else if (context.groundwater() > 300f)
            {
                return blackSand.get().defaultBlockState();
            }
            else
            {
                return context.getBottomRock().sand().defaultBlockState();
            }
        }
    };

    public static final SurfaceState RARE_SHORE_SANDSTONE = new SurfaceState()
    {
        private final Supplier<Block> pinkSandstone = TFCBlocks.SANDSTONE.get(SandBlockType.PINK).get(SandstoneBlockType.RAW);
        private final Supplier<Block> blackSandstone = TFCBlocks.SANDSTONE.get(SandBlockType.BLACK).get(SandstoneBlockType.RAW);

        @Override
        public BlockState getState(SurfaceBuilderContext context)
        {
            if (context.groundwater() > 300f && context.averageTemperature() > 15f)
            {
                return pinkSandstone.get().defaultBlockState();
            }
            else if (context.groundwater() > 300f)
            {
                return blackSandstone.get().defaultBlockState();
            }
            else
            {
                return context.getBottomRock().sandstone().defaultBlockState();
            }
        }
    };

    public static final SurfaceState WATER = context -> context.salty() ?
        TFCFluids.SALT_WATER.createSourceBlock() :
        Fluids.WATER.defaultFluidState().createLegacyBlock();
}
