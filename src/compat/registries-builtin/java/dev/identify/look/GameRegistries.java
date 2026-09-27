package dev.identify.look;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

final class GameRegistries {
    private GameRegistries() {
    }

    static String blockNamespace(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).getNamespace();
    }

    static String entityNamespace(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace();
    }

    static Iterable<MobEffect> mobEffects() {
        return BuiltInRegistries.MOB_EFFECT;
    }

    static String mobEffectId(MobEffect effect) {
        return String.valueOf(BuiltInRegistries.MOB_EFFECT.getKey(effect));
    }
}
