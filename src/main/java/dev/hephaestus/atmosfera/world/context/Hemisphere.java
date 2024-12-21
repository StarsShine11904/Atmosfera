package dev.hephaestus.atmosfera.world.context;

import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

class Hemisphere implements EnvironmentContext {
    private final byte[][] offsets;
    private final Sphere sphere;

    // TODO all of this is updated on one thread and read from another, leading to inconsistent state
    private final Map<Block, Integer> blockTypes = new ConcurrentHashMap<>();
    private final Map<Identifier, Integer> blockTags = new ConcurrentHashMap<>();
    private final Map<Biome, Integer> biomeTypes = new ConcurrentHashMap<>();
    private final Map<Identifier, Integer> biomeTags = new ConcurrentHashMap<>();

    private final AtomicInteger blockCount = new AtomicInteger();
    private final AtomicInteger skyVisibility = new AtomicInteger();

    Hemisphere(byte[][] offsets, Sphere sphere) {
        this.sphere = sphere;
        this.offsets = offsets;
    }

    @Override
    public ClientPlayerEntity getPlayer() {
        return this.sphere.player;
    }

    @Override
    public float getBlockTypePercentage(Block block) {
        return this.blockTypes.getOrDefault(block, 0) / (float) this.blockCount.get();
    }

    @Override
    public float getBlockTagPercentage(TagKey<Block> blocks) {
        return this.blockTags.getOrDefault(blocks.id(), 0) / (float) this.blockCount.get();
    }

    @Override
    public float getBiomePercentage(Biome biome) {
        return this.biomeTypes.getOrDefault(biome, 0) / (float) this.blockCount.get();
    }

    @Override
    public float getBiomeTagPercentage(TagKey<Biome> biomes) {
        return this.biomeTags.getOrDefault(biomes.id(), 0) / (float) this.blockCount.get();
    }

    @Override
    public float getAltitude() {
        return this.sphere.altitude;
    }

    @Override
    public float getElevation() {
        return this.sphere.elevation;
    }

    @Override
    public float getSkyVisibility() {
        return this.skyVisibility.get() / (float) this.blockCount.get();
    }

    @Override
    public boolean isDaytime() {
        return this.sphere.isDay;
    }

    @Override
    public boolean isRainy() {
        return this.sphere.isRainy;
    }

    @Override
    public boolean isStormy() {
        return this.sphere.isStormy;
    }

    @Override
    public Entity getVehicle() {
        return this.sphere.vehicle;
    }

    @Override
    public Collection<String> getBossBars() {
        return this.sphere.bossBars;
    }

    private void clear() {
        this.blockCount.set(0);
        this.skyVisibility.set(0);
        this.blockTypes.replaceAll((block, integer) -> 0);
        this.blockTags.replaceAll((identifier, integer) -> 0);
        this.biomeTypes.replaceAll((biome, integer) -> 0);
        this.blockTags.replaceAll((identifier, integer) -> 0);
    }

    private void add(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        this.blockTypes.merge(block, 1, Integer::sum);
        block.getRegistryEntry().streamTags().forEach(blockTag -> {
            this.blockTags.merge(blockTag.id(), 1, Integer::sum);
        });

        RegistryEntry<Biome> biomeEntry = world.getBiome(pos);
        Biome biome = biomeEntry.value();
        biomeEntry.streamTags().forEach(biomeTag -> {
            this.biomeTags.merge(biomeTag.id(), 1, Integer::sum);
        });

        this.biomeTypes.merge(biome, 1, Integer::sum);
        this.skyVisibility.addAndGet(world.getLightLevel(LightType.SKY, pos) / world.getMaxLightLevel());
        this.blockCount.incrementAndGet();
    }

    // runs on worker threads
    void update(BlockPos center) {
        this.clear();

        BlockPos.Mutable mut = new BlockPos.Mutable();
        World world = getPlayer().world;

        for (byte[] a : this.offsets) {
            mut.set(center.getX() + a[0], center.getY() + a[1], center.getZ() + a[2]);
            this.add(world, mut);
        }
    }
}
