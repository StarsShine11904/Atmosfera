package dev.hephaestus.atmosfera.mixin;

import net.minecraft.client.sound.SoundSystem;
import net.minecraft.client.sound.TickableSoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(SoundSystem.class)
public interface SoundSystemAccessor {
    @Accessor
    List<TickableSoundInstance> getTickingSounds();
}
