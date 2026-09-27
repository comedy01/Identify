package dev.identify.look;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

final class BlockEntityCompat {
    private BlockEntityCompat() {
    }

    static Entity spawnerEntity(Level level, BlockPos pos, SpawnerBlockEntity spawner) {
        return spawner.getSpawner().getOrCreateDisplayEntity(level, pos);
    }

    static CompoundTag beaconTag(Level level, BeaconBlockEntity beacon) {
        return beacon.getUpdateTag(level.registryAccess());
    }

    static String effectId(CompoundTag tag, boolean primary) {
        return NbtCompat.stringOr(tag, primary ? "primary_effect" : "secondary_effect", "");
    }
}
