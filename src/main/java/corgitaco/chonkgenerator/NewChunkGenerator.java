package corgitaco.chonkgenerator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.material.Material;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class NewChunkGenerator extends ChunkGenerator {
    public static final Codec<NewChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter((generator) -> generator.biomeSource),
            Codec.LONG.fieldOf("seed").stable().forGetter((generator) -> generator.seed))
            .apply(instance, instance.stable(NewChunkGenerator::new)));
    private final long seed;

    public NewChunkGenerator(BiomeSource biomeSource, long seed) {
        super(biomeSource, new StructureSettings(Optional.empty(), new HashMap<>()));
        this.seed = seed;
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGenerator withSeed(long seed) {
        return new NewChunkGenerator(this.biomeSource, seed);
    }

    @Override
    public void buildSurfaceAndBedrock(WorldGenRegion region, ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        WorldgenRandom worldgenRandom = new WorldgenRandom();
        worldgenRandom.setBaseChunkSeed(chunkX, chunkZ);
        int minX = chunkPos.getMinBlockX();
        int minY = chunkPos.getMinBlockZ();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int moveX = 0; moveX < 16; ++moveX) {
            for (int moveZ = 0; moveZ < 16; ++moveZ) {
                int x = minX + moveX;
                int z = minY + moveZ;
                int height = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, moveX, moveZ) + 1;
                double e = 1;
                region.getBiome(mutable.set(minX + moveX, height, minY + moveZ)).buildSurfaceAt(worldgenRandom, chunk, x, z, height, e, Blocks.STONE.defaultBlockState(), Blocks.WATER.defaultBlockState(), this.getSeaLevel(), region.getSeed());
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, StructureFeatureManager accessor, ChunkAccess chunk) {
        int maxBuildHeight = chunk.getSectionIndex(chunk.getMaxBuildHeight() - 1);
        int minBuildHeight = chunk.getSectionIndex(chunk.getMinBuildHeight());
        ReferenceOpenHashSet<LevelChunkSection> set = new ReferenceOpenHashSet<>();
        acquireSections(chunk, maxBuildHeight, minBuildHeight, set);



        return CompletableFuture.supplyAsync(() -> {
            return getProcessedChunk(chunk);
        }, Util.backgroundExecutor()).thenApplyAsync((chunkAccess) -> {
            releaseSections(set);
            return chunkAccess;
        }, executor);
    }

    private ChunkAccess getProcessedChunk(ChunkAccess chunk) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (int yHeight = 0; yHeight < 150; ++yHeight) {
            BlockState blockState = Blocks.STONE.defaultBlockState();

            if (blockState != null) {
                int trueY = (chunk.getMinBuildHeight() + 1) + yHeight;

                for (int x = 0; x < 16; ++x) {
                    for (int z = 0; z < 16; ++z) {
                        int trueX = chunk.getPos().getMinBlockX() + x;
                        int trueZ = chunk.getPos().getMinBlockZ() + z;
                        mutable.set(trueX, yHeight, trueZ);

                        setBlockForSection(chunk, blockState, trueY, x, z);
                        updateLights(chunk, mutable, blockState);
                        updateHeightMaps(chunk, blockState, trueY, x, z);

                    }
                }
            }
        }
        return chunk;
    }

    private void updateHeightMaps(ChunkAccess chunk, BlockState blockState, int trueY, int x, int z) {
        int xLocal = x & 15;
        int zLocal = z & 15;
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG).update(xLocal, trueY, zLocal, blockState);
        chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG).update(xLocal, trueY, zLocal, blockState);
    }

    private void updateLights(ChunkAccess chunk, BlockPos.MutableBlockPos mutable, BlockState blockState) {
        if (blockState.getMaterial() != Material.AIR) {
            if (blockState.getLightEmission() != 0 &&chunk instanceof ProtoChunk) {
                ((ProtoChunk) chunk).addLight(mutable.immutable());
            }
        }
    }

    private void setBlockForSection(ChunkAccess chunk, BlockState blockState, int y, int x, int z) {
        LevelChunkSection levelChunkSection = chunk.getOrCreateSection(chunk.getSectionsCount() - 1);

        int currentSection = chunk.getSectionIndex(y);
        if (chunk.getSectionIndex(levelChunkSection.bottomBlockY()) != currentSection) {
            levelChunkSection = chunk.getOrCreateSection(currentSection);
        }
        levelChunkSection.setBlockState(x & 15, y & 15, z & 15, blockState, false);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world) {
        return 0;
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world) {
        return new NoiseColumn(0, new BlockState[]{});
    }

    private void releaseSections(Set<LevelChunkSection> sections) {
        for (LevelChunkSection levelChunkSection : sections) {
            levelChunkSection.release();
        }
    }

    private void acquireSections(ChunkAccess chunk, int maxBuildHeight, int minBuildHeight, Set<LevelChunkSection> sections) {
        for (int sectionIDX = maxBuildHeight; sectionIDX >= minBuildHeight; --sectionIDX) {
            LevelChunkSection levelChunkSection = chunk.getOrCreateSection(sectionIDX);
            levelChunkSection.acquire();
            sections.add(levelChunkSection);
        }
    }
}
