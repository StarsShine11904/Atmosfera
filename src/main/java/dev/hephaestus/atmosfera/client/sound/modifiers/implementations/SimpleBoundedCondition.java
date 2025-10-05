package dev.hephaestus.atmosfera.client.sound.modifiers.implementations;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.Bound;
import dev.hephaestus.atmosfera.world.context.EnvironmentContext;
import net.minecraft.world.World;

import java.util.function.Function;

import static dev.hephaestus.atmosfera.client.sound.modifiers.CommonAttributes.getBound;

public record SimpleBoundedCondition(Bound bound, Function<EnvironmentContext, Number> valueGetter) implements AtmosphericSoundModifier, AtmosphericSoundModifier.Factory {
    @Override
    public float getModifier(EnvironmentContext context) {
        float value = this.valueGetter.apply(context).floatValue();

        return bound.apply(value);
    }

    @Override
    public AtmosphericSoundModifier create(World world) {
        return this;
    }

    public static SimpleBoundedCondition altitude(JsonElement element) {
        return create(element, EnvironmentContext::getAltitude);
    }

    public static SimpleBoundedCondition elevation(JsonElement element) {
        return create(element, EnvironmentContext::getElevation);
    }

    public static SimpleBoundedCondition skyVisibility(JsonElement element) {
        return create(element, EnvironmentContext::getSkyVisibility);
    }

    public static SimpleBoundedCondition create(JsonElement element, Function<EnvironmentContext, Number> valueGetter) {
        JsonObject object = element.getAsJsonObject();

        Bound bound = getBound(object);

        return new SimpleBoundedCondition(bound, valueGetter);
    }
}
