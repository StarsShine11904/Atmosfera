package dev.hephaestus.atmosfera.client.sound.modifiers.implementations;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.Bound;
import dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.Range;
import dev.hephaestus.atmosfera.world.context.EnvironmentContext;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import static dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.getBound;
import static dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.getRange;

public record PercentBiomeModifier(Range range, Bound bound, ImmutableCollection<RegistryEntry<Biome>> biomes, ImmutableCollection<TagKey<Biome>> biomeTags) implements AtmosphericSoundModifier {
    public PercentBiomeModifier(Range range, Bound bound, ImmutableCollection<RegistryEntry<Biome>> biomes, ImmutableCollection<TagKey<Biome>> biomeTags) {
        ImmutableCollection.Builder<RegistryEntry<Biome>> biomesBuilder = ImmutableList.builder();

        // Remove biomes that are already present in tags so that they aren't counted twice
        biomes:
        for (RegistryEntry<Biome> biomeEntry : biomes) {
            for (TagKey<Biome> tag : biomeTags) {
                if (biomeEntry.isIn(tag)) {
                    continue biomes;
                }
            }

            biomesBuilder.add(biomeEntry);
        }

        this.biomes = biomesBuilder.build();
        this.biomeTags = biomeTags;
        this.range = range;
        this.bound = bound;
    }

    @Override
    public float getModifier(EnvironmentContext context) {
        float modifier = 0F;

        for (RegistryEntry<Biome> biomeEntry : this.biomes) {
            modifier += context.getBiomePercentage(biomeEntry.value());
        }

        for (TagKey<Biome> tag : this.biomeTags) {
            modifier += context.getBiomeTagPercentage(tag);
        }

        return range.apply(bound.apply(modifier));
    }

    public static AtmosphericSoundModifier.Factory create(JsonObject object) {

        ImmutableCollection.Builder<Identifier> biomes = ImmutableList.builder();
        ImmutableCollection.Builder<Identifier> tags = ImmutableList.builder();

        JsonHelper.getArray(object, "biomes").forEach(biome -> {
            if (biome.getAsString().startsWith("#")) {
                tags.add(Identifier.of(biome.getAsString().substring(1)));
            } else {
                Identifier biomeID = Identifier.of(biome.getAsString());
                biomes.add(biomeID);
            }
        });

        Range range = getRange(object);
        Bound bound = getBound(object);

        return new PercentBiomeModifier.Factory(range, bound, biomes.build(), tags.build());
    }

    private record Factory(Range range, Bound bound, ImmutableCollection<Identifier> biomes, ImmutableCollection<Identifier> biomeTags) implements AtmosphericSoundModifier.Factory {

        @Override
        public AtmosphericSoundModifier create(World world) {
            ImmutableCollection.Builder<RegistryEntry<Biome>> biomes = ImmutableList.builder();

            Registry<Biome> biomeRegistry = world.getRegistryManager().getOrThrow(RegistryKeys.BIOME);

            for (Identifier id : this.biomes) {
                Biome biome = biomeRegistry.get(id);

                if (biome != null) {
                    biomes.add(biomeRegistry.getEntry(biome));
                }
            }

            ImmutableCollection.Builder<TagKey<Biome>> tags = ImmutableList.builder();

            for (Identifier id : this.biomeTags) {
                tags.add(TagKey.of(RegistryKeys.BIOME, id));
            }

            return new PercentBiomeModifier(range, bound, biomes.build(), tags.build());
        }
    }
}
