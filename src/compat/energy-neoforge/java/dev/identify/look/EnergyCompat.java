package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

final class EnergyCompat {
    private EnergyCompat() {
    }

    static long[] read(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null) {
            return null;
        }
        try {
            IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, entity.getBlockPos(), null);
            for (int i = 0; (storage == null || storage.getMaxEnergyStored() <= 0) && i < Direction.values().length; i++) {
                storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, entity.getBlockPos(), Direction.values()[i]);
            }
            if (storage == null || storage.getMaxEnergyStored() <= 0) {
                return null;
            }
            return new long[]{storage.getEnergyStored(), storage.getMaxEnergyStored()};
        } catch (RuntimeException e) {
            return null;
        }
    }
}
