package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

final class EnergyCompat {
    private EnergyCompat() {
    }

    static long[] read(BlockEntity entity) {
        try {
            IEnergyStorage storage = entity.getCapability(CapabilityEnergy.ENERGY, null).orElse(null);
            for (int i = 0; (storage == null || storage.getMaxEnergyStored() <= 0) && i < Direction.values().length; i++) {
                storage = entity.getCapability(CapabilityEnergy.ENERGY, Direction.values()[i]).orElse(null);
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
