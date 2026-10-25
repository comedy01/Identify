package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;

final class EnergyCompat {
    private EnergyCompat() {
    }

    static long[] read(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null) {
            return null;
        }
        try {
            EnergyHandler handler = level.getCapability(Capabilities.Energy.BLOCK, entity.getBlockPos(), null);
            for (int i = 0; (handler == null || handler.getCapacityAsLong() <= 0) && i < Direction.values().length; i++) {
                handler = level.getCapability(Capabilities.Energy.BLOCK, entity.getBlockPos(), Direction.values()[i]);
            }
            if (handler == null || handler.getCapacityAsLong() <= 0) {
                return null;
            }
            return new long[]{handler.getAmountAsLong(), handler.getCapacityAsLong()};
        } catch (RuntimeException e) {
            return null;
        }
    }
}
