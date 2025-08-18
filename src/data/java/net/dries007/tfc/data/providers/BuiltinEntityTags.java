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

        tag(SMALL_FISH)
            .add(
                TFCEntities.COD.get(),
                TFCEntities.PUFFERFISH.get(),
                TFCEntities.TROPICAL_FISH.get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.SALMON).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.RAINBOW_TROUT).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.LAKE_TROUT).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.BLUEGILL).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.LARGEMOUTH_BASS).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.SMALLMOUTH_BASS).get(),
                TFCEntities.FRESHWATER_FISH.get(Fish.CRAPPIE).get()
            );

        tag(BUBBLE_COLUMN_IMMUNE)
            .addTags(SMALL_FISH)
            .add(
                TFCEntities.ORCA.get(),
                TFCEntities.DOLPHIN.get(),
                TFCEntities.SQUID.get(),
                TFCEntities.OCTOPOTEUTHIS.get(),
                TFCEntities.ISOPOD.get(),
                TFCEntities.LOBSTER.get(),
                TFCEntities.HORSESHOE_CRAB.get(),
                TFCEntities.JELLYFISH.get()
            );

        tag(NEEDS_LARGE_FISHING_BAIT)
            .add(
                TFCEntities.ORCA.get(),
                TFCEntities.DOLPHIN.get()
            );

        tag(RAMMING_ANIMALS)
            .add(
                TFCEntities.BOAR.get(),
                TFCEntities.BISON.get(),
                TFCEntities.MOOSE.get(),
                TFCEntities.WILDEBEEST.get()
            );

        tag(PESTS)
            .addTag(UNIVERSAL_PESTS)
            .addTag(COLD_PESTS)
            .addTag(DESERT_PESTS)
            .addTag(TROPICAL_PESTS);

        tag(COLD_PESTS).add(TFCEntities.LEMMING.get());
        tag(UNIVERSAL_PESTS).add(TFCEntities.RAT.get());
        tag(DESERT_PESTS).add(TFCEntities.JERBOA.get());
        tag(TROPICAL_PESTS).add(TFCEntities.MONGOOSE.get());

        tag(BIRD_PREY)
            .add(
                TFCEntities.TURKEY.get(),
                TFCEntities.PHEASANT.get(),
                TFCEntities.GROUSE.get(),
                TFCEntities.CHICKEN.get(),
                TFCEntities.PENGUIN.get(),
                TFCEntities.QUAIL.get(),
                TFCEntities.DUCK.get()
            );

        tag(HUNTED_BY_CATS)
            .addTags(
                SMALL_FISH,
                BIRD_PREY
            )
            .add(
                TFCEntities.RAT.get(),
                TFCEntities.JERBOA.get(),
                TFCEntities.LEMMING.get()
            );

        tag(HUNTED_BY_DOGS)
            //TODO:
//            .addTags(
//                HUNTED_BY_LAND_PREDATORS
//            )
            .add(
                TFCEntities.RAT.get(),
                TFCEntities.JERBOA.get(),
                TFCEntities.LEMMING.get(),
                TFCEntities.MONGOOSE.get()
            );
    }
}
