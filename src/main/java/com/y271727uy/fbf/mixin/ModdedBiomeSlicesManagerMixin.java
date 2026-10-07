package com.y271727uy.fbf.mixin;

import com.mojang.logging.LogUtils;
import com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 *我他妈早晚给这个傻逼来一个overwiter
 */
@Mixin(targets = "com.teamabnormals.blueprint.common.world.modification.ModdedBiomeSlicesManager", remap = false)
public abstract class ModdedBiomeSlicesManagerMixin {
    private static final Logger FRMC$LOGGER = LogUtils.getLogger();

    @Inject(
        method = "lambda$onServerAboutToStart$3",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private static void frmc$sortFeaturesWithCycleFallback(
        ModdedBiomeSource source,
        ChunkGenerator generator,
        CallbackInfoReturnable<List<FeatureSorter.StepFeatureData>> cir
    ) {
        List<Holder<Biome>> biomes = List.copyOf(source.possibleBiomes());
        try {
            cir.setReturnValue(FeatureSorter.buildFeaturesPerStep(
                biomes,
                holder -> generator.getBiomeGenerationSettings(holder).features(),
                true
            ));
            return;
        } catch (IllegalStateException cycle) {
            if (!FRMC$isFeatureOrderCycle(cycle)) {
                throw cycle;
            }

            List<Holder<Biome>> reduced = new ArrayList<>(biomes);
            for (int index = 0; index < biomes.size(); index++) {
                Holder<Biome> removed = reduced.remove(0);
                try {
                    List<FeatureSorter.StepFeatureData> result = FeatureSorter.buildFeaturesPerStep(
                        reduced,
                        holder -> generator.getBiomeGenerationSettings(holder).features(),
                        false
                    );
                    FRMC$LOGGER.warn(
                        "Blueprint feature order cycle fallback removed biome source {} from the sorter input",
                        removed.unwrapKey().map(key -> key.location().toString()).orElse("<unregistered>")
                    );
                    cir.setReturnValue(result);
                    return;
                } catch (IllegalStateException retry) {
                    if (!FRMC$isFeatureOrderCycle(retry)) {
                        throw retry;
                    }
                }
            }

            throw cycle;
        }
    }

    private static boolean FRMC$isFeatureOrderCycle(IllegalStateException exception) {
        String message = exception.getMessage();
        return message != null && message.startsWith("Feature order cycle found");
    }
}