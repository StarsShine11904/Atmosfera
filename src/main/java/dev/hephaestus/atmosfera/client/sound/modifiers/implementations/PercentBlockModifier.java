package dev.hephaestus.atmosfera.client.sound.modifiers.implementations;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.Bound;
import dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.Range;
import dev.hephaestus.atmosfera.world.context.EnvironmentContext;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.world.World;

import static dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.getBound;
import static dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.getRange;

public record PercentBlockModifier(Range range, Bound bound, ImmutableCollection<Block> blocks, ImmutableCollection<TagKey<Block>> blockTags) implements AtmosphericSoundModifier, AtmosphericSoundModifier.Factory {
    public PercentBlockModifier(Range range, Bound bound, ImmutableCollection<Block> blocks, ImmutableCollection<TagKey<Block>> blockTags) {
        ImmutableCollection.Builder<Block> blocksBuilder = ImmutableList.builder();

        // Remove blocks that are already present in tags so that they aren't counted twice
        blocks:
        for (Block block : blocks) {
            for (TagKey<Block> tag : blockTags) {
                if (block.getDefaultState().isIn(tag)) {
                    continue blocks;
                }
            }

            blocksBuilder.add(block);
        }

        this.blocks = blocksBuilder.build();
        this.blockTags = blockTags;
        this.range = range;
        this.bound = bound;
    }

    @Override
    public float getModifier(EnvironmentContext context) {
        float modifier = 0F;

        for (Block block : this.blocks) {
            modifier += context.getBlockTypePercentage(block);
        }

        for (TagKey<Block> tag : this.blockTags) {
            modifier += context.getBlockTagPercentage(tag);
        }

        return range.apply(bound.apply(modifier));
    }

    public static PercentBlockModifier create(JsonObject object) {
        ImmutableCollection.Builder<Block> blocks = ImmutableList.builder();
        ImmutableCollection.Builder<TagKey<Block>> tags = ImmutableList.builder();

        JsonHelper.getArray(object, "blocks").forEach(block -> {
            // Registers only the loaded IDs to avoid false triggers.
            if (block.getAsString().startsWith("#")) {
                Identifier tagId = Identifier.of(block.getAsString().substring(1));
                TagKey<Block> tagKey = TagKey.of(RegistryKeys.BLOCK, tagId);
                tags.add(tagKey);
            } else {
                Identifier blockId = Identifier.of(block.getAsString());

                if (Registries.BLOCK.containsId(blockId)) {
                    Block b = Registries.BLOCK.get(blockId);
                    blocks.add(b);
                }
            }
        });

        Range range = getRange(object);
        Bound bound = getBound(object);

        return new PercentBlockModifier(range, bound, blocks.build(), tags.build());
    }

    @Override
    public AtmosphericSoundModifier create(World world) {
        return this;
    }
}
