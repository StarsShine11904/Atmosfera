package dev.hephaestus.atmosfera.client.sound;

import com.google.common.collect.ImmutableList;
import dev.hephaestus.atmosfera.Atmosfera;
import dev.hephaestus.atmosfera.AtmosferaConfig;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.MusicType;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AtmosphericSoundHandler {
    private static final Random RANDOM = Random.create();

    private static final Map<AtmosphericSound, MusicSound> MUSIC = new HashMap<>();

    private final ImmutableList<AtmosphericSound> sounds;
    private final ImmutableList<AtmosphericSound> musics;
    private final Map<AtmosphericSound, AtmosphericSoundInstance> playingSounds = new HashMap<>();

    public AtmosphericSoundHandler(ClientWorld world) {
        this.sounds = getSoundsFromDefinitions(Atmosfera.SOUND_DEFINITIONS, world);
        this.musics = getSoundsFromDefinitions(Atmosfera.MUSIC_DEFINITIONS, world);
    }

    private static ImmutableList<AtmosphericSound> getSoundsFromDefinitions(Map<Identifier, AtmosphericSoundDefinition> definitions, ClientWorld world) {
        var sounds = ImmutableList.<AtmosphericSound>builder();

        for (var definition : definitions.values()) {
            var modifiers = ImmutableList.<AtmosphericSoundModifier>builder();

            for (var factory : definition.modifierFactories()) {
                modifiers.add(factory.create(world));
            }

            sounds.add(new AtmosphericSound(definition.id(), definition.soundId(), definition.shape(), definition.size(), modifiers.build()));
        }

        return sounds.build();
    }

    public void tick() {
        var client = MinecraftClient.getInstance();
        var world = client.world;
        if (world == null)
            return;

        world.atmosfera$updateEnvironmentContext();

        playingSounds.values().removeIf(AtmosphericSoundInstance::isDone);

        for (var sound : sounds) {
            if (playingSounds.containsKey(sound))
                continue;

            float volume = sound.getVolume(world);

            // The non-zero volume prevents the events getting triggered multiple times at volumes near zero.
            if (volume >= 0.0125 && client.options.getSoundVolume(SoundCategory.AMBIENT) > 0) {
                var soundInstance = new AtmosphericSoundInstance(sound, 0.0001f);
                playingSounds.put(sound, soundInstance);
                client.getSoundManager().playNextTick(soundInstance);
                Atmosfera.debug("volume > 0: {} - {}", sound.id(), volume);
            }
        }
    }

    @SuppressWarnings("DataFlowIssue")
    public MusicSound getMusicSound(MusicSound original) {
        var client = MinecraftClient.getInstance();
        var world = client.world;
        if (world == null || !world.atmosfera$isEnvironmentContextInitialized() || client.options.getSoundVolume(SoundCategory.MUSIC) == 0)
            return original;

        var soundManager = client.getSoundManager();
        float originalWeight = soundManager.get(original.getSound().value().id()).getWeight(); // TODO soundManager.get() returns null with Music Control...?!

        List<Pair<Float, MusicSound>> candidates = new ArrayList<>();
        float total = 0;

        candidates.add(new Pair<>(originalWeight, original));
        total += originalWeight;

        for (var music : musics) {
            float volume = music.getVolume(world);

            if (volume >= 0.0125) {
                float weight = AtmosferaConfig.customMusicWeightScale() * soundManager.get(music.soundId()).getWeight();

                candidates.add(new Pair<>(weight, MUSIC.computeIfAbsent(music, id -> {
                    Atmosfera.debug("createIngameMusic: {}", music.id());
                    return MusicType.createIngameMusic(RegistryEntry.of(SoundEvent.of(music.soundId())));
                })));

                total += weight;
            }
        }

        float i = total <= 0 ? 0 : RANDOM.nextFloat() * total;

        for (Pair<Float, MusicSound> pair : candidates) {
            i -= pair.getLeft();

            if (i < 0)
                return pair.getRight();
        }

        // due to float imprecision, i might not have fallen below 0, count this towards the last element
        return candidates.getLast().getRight();
    }
}
