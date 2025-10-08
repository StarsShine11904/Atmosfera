package dev.hephaestus.atmosfera.client.sound;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.hephaestus.atmosfera.Atmosfera;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import dev.hephaestus.atmosfera.client.sound.modifiers.implementations.ConfigModifier;
import dev.hephaestus.atmosfera.world.context.EnvironmentContext;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.util.Locale;
import java.util.Map;

public record AtmosphericSoundSerializer(String sourceFolder, Map<Identifier, AtmosphericSoundDefinition> destination) implements SynchronousResourceReloader {
    public Identifier getFabricId() {
        return Atmosfera.id(this.sourceFolder);
    }

    @Override
    public void reload(ResourceManager manager) {
        this.destination.clear();

        Map<Identifier, Resource> resources = manager.findResources(this.sourceFolder + "/definitions", id -> id.getPath().endsWith(".json"));

        for (Identifier resource : resources.keySet()) {
            Identifier id = Identifier.of(
                    resource.getNamespace(),
                    resource.getPath().substring(
                            resource.getPath().indexOf("definitions/") + "definitions/".length(),
                            resource.getPath().indexOf(".json")
                    )
            );

            try (var reader = resources.get(resource).getReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

                Identifier soundId = Identifier.of(JsonHelper.getString(json, "sound"));

                EnvironmentContext.Shape shape = getShape(json, id);
                EnvironmentContext.Size size = getSize(json, id);
                ImmutableCollection<AtmosphericSoundModifier.Factory> modifiers = getModifiers(json, id);
                int defaultVolume = JsonHelper.getInt(json, "default_volume", 100);
                boolean showSubtitlesByDefault = JsonHelper.getBoolean(json, "default_subtitle", true);

                this.destination.put(id, new AtmosphericSoundDefinition(id, soundId, shape, size, defaultVolume, showSubtitlesByDefault, modifiers));
            } catch (Exception e) {
                Atmosfera.error("Failed to load sound event '{}'", id, e);
            }
        }
    }

    private static EnvironmentContext.Shape getShape(JsonObject json, Identifier id) {
        if (json.has("shape")) {
            return EnvironmentContext.Shape.valueOf(json.getAsJsonPrimitive("shape").getAsString().toUpperCase(Locale.ROOT));
        } else {
            throw new RuntimeException("Sound definition '%s' is missing \"shape\" field.".formatted(id));
        }
    }

    private static EnvironmentContext.Size getSize(JsonObject json, Identifier id) {
        if (json.has("size")) {
            return json.has("size") ? EnvironmentContext.Size.valueOf(json.getAsJsonPrimitive("size").getAsString().toUpperCase(Locale.ROOT)) : EnvironmentContext.Size.MEDIUM;
        } else {
            throw new RuntimeException("Sound definition '%s' is missing \"size\" field.".formatted(id));
        }
    }

    private static ImmutableCollection<AtmosphericSoundModifier.Factory> getModifiers(JsonObject json, Identifier id) {
        ImmutableCollection.Builder<AtmosphericSoundModifier.Factory> modifiers = ImmutableList.builder();

        modifiers.add(new ConfigModifier(id));

        if (json.has("modifiers")) {
            for (JsonElement element : json.get("modifiers").getAsJsonArray()) {
                JsonObject modifier = element.getAsJsonObject();

                if (!modifier.has("type")) {
                    throw new RuntimeException("Modifier for sound definition '%s' is missing \"type\" field.".formatted(id));
                }

                String type = modifier.get("type").getAsString();
                AtmosphericSoundModifier.FactoryFactory factory = AtmosphericSoundModifierRegistry.get(type);

                if (factory == null) {
                    Atmosfera.warn("Modifier type \"{}\" does not exist", type);
                } else {
                    modifiers.add(factory.create(modifier));
                }
            }
        }

        return modifiers.build();
    }
}
