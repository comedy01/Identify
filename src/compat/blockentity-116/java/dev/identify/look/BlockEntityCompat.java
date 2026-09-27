package dev.identify.look;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

final class BlockEntityCompat {
    private BlockEntityCompat() {
    }

    static Entity spawnerEntity(Level level, BlockPos pos, SpawnerBlockEntity spawner) {
        return spawner.getSpawner().getOrCreateDisplayEntity();
    }

    static CompoundTag beaconTag(Level level, BeaconBlockEntity beacon) {
        return beacon.getUpdateTag();
    }

    static String effectId(CompoundTag tag, boolean primary) {
        String key = primary ? "Primary" : "Secondary";
        MobEffect effect = tag.contains(key) ? MobEffect.byId(tag.getInt(key)) : null;
        return effect == null ? "" : String.valueOf(Registry.MOB_EFFECT.getKey(effect));
    }
}
