/*
 * Licensed under the EUPL, Version 1.2.
 * You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 */

package net.dries007.tfc.data.providers;

import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.common.TFCTags;
import net.dries007.tfc.common.entities.TFCEntities;
import net.dries007.tfc.common.entities.aquatic.Fish;

import static net.dries007.tfc.common.TFCTags.Entities.*;

public class BuiltinEntityTags extends EntityTypeTagsProvider
{
    public BuiltinEntityTags(GatherDataEvent event, CompletableFuture<HolderLookup.Provider> provider)
    {
        super(event.getGenerator().getPackOutput(), provider, TerraFirmaCraft.MOD_ID, event.getExistingFileHelper());
    }

    @Override
    protected void addTags(HolderLookup.Provider provider)
    {
        // ===== Vanilla Tags ===== //

        // ===== TFC Tags ===== //

        tag(MONSTERS)
            .addTag(EntityTypeTags.UNDEAD)
            .add(
                EntityType.CREEPER,
                EntityType.SPIDER,
                EntityType.WITCH,
                EntityType.SLIME
            );

        tag(SPAWNS_ON_COLD_BLOCKS)
            .add(
                TFCEntities.PENGUIN.get(),
                TFCEntities.POLAR_BEAR.get()
            );

        tag(TURTLE_FRIENDS)
            .add(
                EntityType.PLAYER,
                TFCEntities.DOLPHIN.get()
            );

        tag(BUBBLE_COLUMN_IMMUNE)
            .add(
                TFCEntities.ORCA.get(),
                TFCEntities.DOLPHIN.get(),
                TFCEntities.SQUID.get(),
                TFCEntities.OCTOPOTEUTHIS.get(),
                TFCEntities.ISOPOD.get(),
                TFCEntities.LOBSTER.get(),
                TFCEntities.HORSESHOE_CRAB.get(),
                TFCEntities.COD.get(),
                TFCEntities.PUFFERFISH.get(),
                TFCEntities.TROPICAL_FISH.get(),
                TFCEntities.JELLYFISH.get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.SALMON).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.RAINBOW_TROUT).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.LAKE_TROUT).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.BLUEGILL).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.LARGEMOUTH_BASS).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.SMALLMOUTH_BASS).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.CRAPPIE).get()
            );

        tag(NEEDS_LARGE_FISHING_BAIT)
            .add(
                TFCEntities.ORCA.get(),
                TFCEntities.DOLPHIN.get()
            );
    }
}
