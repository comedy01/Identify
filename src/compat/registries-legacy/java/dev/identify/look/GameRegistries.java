package dev.identify.look;

import net.minecraft.core.Registry;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;

final class GameRegistries {
    private GameRegistries() {
    }

    static String blockNamespace(Block block) {
        return Registry.BLOCK.getKey(block).getNamespace();
    }

    static String entityNamespace(EntityType<?> type) {
        return Registry.ENTITY_TYPE.getKey(type).getNamespace();
    }

    static Iterable<MobEffect> mobEffects() {
        return Registry.MOB_EFFECT;
    }

    static String mobEffectId(MobEffect effect) {
        return String.valueOf(Registry.MOB_EFFECT.getKey(effect));
    }
}
