package dev.hephaestus.atmosfera.client.sound.modifiers.implementations;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import dev.hephaestus.atmosfera.world.context.EnvironmentContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public record RidingCondition(ImmutableList<EntityType<?>> entityTypes) implements AtmosphericSoundModifier, AtmosphericSoundModifier.Factory {
    @Override
    public float getModifier(EnvironmentContext context) {
        Entity vehicle = context.getVehicle();

        if (vehicle != null) {
            for (var entityType : entityTypes) {
                if (vehicle.getType().equals(entityType)) {
                    return 1;
                }
            }
        }

        return 0;
    }

    @Override
    public AtmosphericSoundModifier create(World world) {
        return this;
    }

    public static Factory create(JsonObject object) {
        var entityTypes = ImmutableList.<EntityType<?>>builder();

        JsonElement value = object.get("value");

        if (value.isJsonPrimitive()) {
            var id = new Identifier(value.getAsString());
            Registries.ENTITY_TYPE.getOrEmpty(id).ifPresent(entityTypes::add);
        } else if (value.isJsonArray()) {
            for (JsonElement e : value.getAsJsonArray()) {
                var id = new Identifier(e.getAsString());
                Registries.ENTITY_TYPE.getOrEmpty(id).ifPresent(entityTypes::add);
            }
        }

        return new RidingCondition(entityTypes.build());
    }
}
