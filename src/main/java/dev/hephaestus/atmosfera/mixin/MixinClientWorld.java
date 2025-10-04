package dev.hephaestus.atmosfera.mixin;

import dev.hephaestus.atmosfera.client.sound.AtmosphericSoundHandler;
import dev.hephaestus.atmosfera.client.sound.util.ClientWorldDuck;
import dev.hephaestus.atmosfera.world.context.ContextUtil;
import dev.hephaestus.atmosfera.world.context.EnvironmentContext;
import dev.hephaestus.atmosfera.world.context.Sphere;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.function.BooleanSupplier;

@Mixin(ClientWorld.class)
public class MixinClientWorld implements ClientWorldDuck {
    private AtmosphericSoundHandler atmosfera$soundHandler;
    private EnumMap<EnvironmentContext.Size, Sphere> atmosfera$environmentContexts;
    private boolean atmosfera$initialized;
    private int atmosfera$updateTimer = 0;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initializeSoundHandler(ClientPlayNetworkHandler networkHandler, ClientWorld.Properties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimensionType, int loadDistance, int simulationDistance, WorldRenderer worldRenderer, boolean debugWorld, long seed, int seaLevel, CallbackInfo ci) {
        this.atmosfera$soundHandler = new AtmosphericSoundHandler((ClientWorld) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickSoundHandler(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        this.atmosfera$soundHandler.tick();
    }

    @Override
    public AtmosphericSoundHandler atmosfera$getAtmosphericSoundHandler() {
        return this.atmosfera$soundHandler;
    }

    @Override
    public EnvironmentContext atmosfera$getEnvironmentContext(EnvironmentContext.Size size, EnvironmentContext.Shape shape) {
        if(!atmosfera$isEnvironmentContextInitialized()) return null;
        return switch (shape) {
            case UPPER_HEMISPHERE -> this.atmosfera$environmentContexts.get(size).getUpperHemisphere();
            case LOWER_HEMISPHERE -> this.atmosfera$environmentContexts.get(size).getLowerHemisphere();
            case SPHERE -> this.atmosfera$environmentContexts.get(size);
        };
    }

    @Override
    public void atmosfera$updateEnvironmentContext() {
        if(!this.atmosfera$initialized) {
            ClientPlayerEntity player = MinecraftClient.getInstance().player;
            this.atmosfera$environmentContexts = new EnumMap<>(EnvironmentContext.Size.class);
            this.atmosfera$environmentContexts.put(EnvironmentContext.Size.SMALL, new Sphere(EnvironmentContext.Size.SMALL, player));
            this.atmosfera$environmentContexts.put(EnvironmentContext.Size.MEDIUM, new Sphere(EnvironmentContext.Size.MEDIUM, player));
            this.atmosfera$environmentContexts.put(EnvironmentContext.Size.LARGE, new Sphere(EnvironmentContext.Size.LARGE, player));
            atmosfera$initialized = true;
        }

        if (--atmosfera$updateTimer <= 0 && ContextUtil.EXECUTOR.getQueue().isEmpty()) {
            this.atmosfera$environmentContexts.get(EnvironmentContext.Size.SMALL ).update();
            this.atmosfera$environmentContexts.get(EnvironmentContext.Size.MEDIUM).update();
            this.atmosfera$environmentContexts.get(EnvironmentContext.Size.LARGE ).update();
            atmosfera$updateTimer = 20;
        }
    }

    @Override
    public boolean atmosfera$isEnvironmentContextInitialized() {
        return atmosfera$initialized;
    }
}
