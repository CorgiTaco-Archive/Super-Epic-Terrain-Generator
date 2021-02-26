package corgitaco.chonkgenerator;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "modid";

    @Override
    public void onInitialize() {
        MyWorldType.init();
        Registry.register(Registry.CHUNK_GENERATOR, new ResourceLocation(MOD_ID, "yes"), NewChunkGenerator.CODEC);
    }
}
