package dev.hephaestus.atmosfera.client.sound;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import dev.hephaestus.atmosfera.Atmosfera;
import dev.hephaestus.atmosfera.AtmosferaConfig;
import dev.hephaestus.atmosfera.client.sound.modifiers.AtmosphericSoundModifier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MusicType;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.MusicSound;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Pair;

import java.util.*;

public class AtmosphericSoundHandler {
    private static final Random RANDOM = new Random();

    private static final Map<AtmosphericSound, MusicSound> MUSIC = new HashMap<>();

    private final Collection<AtmosphericSound> sounds = new ArrayList<>();
    private final Collection<AtmosphericSound> musics = new ArrayList<>();
    private final Map<AtmosphericSound, AtmosphericSoundInstance> soundInstances = new WeakHashMap<>();

    public AtmosphericSoundHandler(ClientWorld world) {
        for (AtmosphericSoundDefinition definition : Atmosfera.SOUND_DEFINITIONS.values()) {
            ImmutableCollection.Builder<AtmosphericSoundModifier> modifiers = ImmutableList.builder();

            for (AtmosphericSoundModifier.Factory factory : definition.modifiers()) {
                modifiers.add(factory.create(world));
            }

            this.sounds.add(new AtmosphericSound(definition.id(), definition.soundId(), definition.shape(), definition.size(), definition.defaultVolume(), definition.hasSubtitleByDefault(), modifiers.build()));
        }

        for (AtmosphericSoundDefinition definition : Atmosfera.MUSIC_DEFINITIONS.values()) {
            ImmutableCollection.Builder<AtmosphericSoundModifier> modifiers = ImmutableList.builder();

            for (AtmosphericSoundModifier.Factory factory : definition.modifiers()) {
                modifiers.add(factory.create(world));
            }

            this.musics.add(new AtmosphericSound(definition.id(), definition.soundId(), definition.shape(), definition.size(), definition.defaultVolume(), definition.hasSubtitleByDefault(), modifiers.build()));
        }
    }

    public void tick() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;

        if (world != null) {
            SoundManager soundManager = client.getSoundManager();

            world.atmosfera$updateEnvironmentContext();

            for (AtmosphericSound definition : this.sounds) {
                if (!this.soundInstances.containsKey(definition) || this.soundInstances.get(definition).isDone()) {
                    float volume = definition.getVolume(world);

                    // The non-zero volume prevents the events getting triggered multiple times at volumes near zero.
                    if (volume >= 0.0125 && client.options.getSoundVolume(SoundCategory.AMBIENT) > 0) {
                        AtmosphericSoundInstance soundInstance = new AtmosphericSoundInstance(definition, 0.0001F);
                        this.soundInstances.put(definition, soundInstance);
                        soundManager.playNextTick(soundInstance);
                        Atmosfera.debug("volume > 0: {} - {}", definition.id(), volume);
                    }
                }
            }
        }

        this.soundInstances.values().removeIf(AtmosphericSoundInstance::isDone);
    }

    public MusicSound getMusicSound(MusicSound defaultSound) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;

        if (world != null && client.options.getSoundVolume(SoundCategory.MUSIC) > 0 && client.player != null && world.atmosfera$isEnvironmentContextInitialized()) {
            SoundManager soundManager = client.getSoundManager();
            float total = Objects.requireNonNull(soundManager.get(defaultSound.getSound().getId())).getWeight();

            List<Pair<Float, MusicSound>> sounds = new ArrayList<>();
            sounds.add(new Pair<>(total, defaultSound));

            for (AtmosphericSound definition : this.musics) {
                float volume = definition.getVolume(world);

                if (volume > 0.0125) {
                    float weight = AtmosferaConfig.customMusicWeightScale() * Objects.requireNonNull(soundManager.get(definition.soundId())).getWeight();

                    sounds.add(new Pair<>(weight, MUSIC.computeIfAbsent(definition, id -> {
                        Atmosfera.debug("createIngameMusic: {}", definition.id());
                        return MusicType.createIngameMusic(new SoundEvent(definition.soundId()));
                    })));

                    total += weight;
                }
            }

            float i = total <= 0 ? 0 : RANDOM.nextFloat() * total;

            for (Pair<Float, MusicSound> pair : sounds) {
                i -= pair.getLeft();

                if (i < 0)
                    return pair.getRight();
            }

            // due to float imprecision, i might not have fallen below 0, count this towards the last element
            return sounds.get(sounds.size() - 1).getRight();
        }

        return defaultSound;
    }
}
