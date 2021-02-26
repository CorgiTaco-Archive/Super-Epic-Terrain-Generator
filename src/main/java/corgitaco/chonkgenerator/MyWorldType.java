package corgitaco.chonkgenerator;

import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

public class MyWorldType extends WorldPreset {
    public MyWorldType() {
        super("chunkgenerator");
    }

    @Override
    protected ChunkGenerator generator(Registry<Biome> biomeRegistry, Registry<NoiseGeneratorSettings> chunkGeneratorSettingsRegistry, long seed) {
        return new NewChunkGenerator(new FixedBiomeSource(biomeRegistry.get(Biomes.PLAINS)), seed);
    }

    public static void init() {
        WorldPreset.PRESETS.add(new MyWorldType());
    }
}