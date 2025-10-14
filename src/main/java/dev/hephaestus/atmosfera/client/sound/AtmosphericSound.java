package dev.hephaestus.atmosfera.client.sound;

import com.google.common.collect.ImmutableCollection;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import dev.hephaestus.atmosfera.world.context.EnvironmentContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;

public record AtmosphericSound(Identifier id, Identifier soundId,
                               EnvironmentContext.Shape shape, EnvironmentContext.Size size,
                               ImmutableCollection<AtmosphericSoundModifier> modifiers) {
    public float getVolume(ClientWorld world) {
        var context = world.atmosfera$getEnvironmentContext(size, shape);
        if (context == null)
            return 0;

        float volume = 1;
        for (var modifier : modifiers) {
            volume *= modifier.getModifier(context);
        }

        return volume;
    }
}