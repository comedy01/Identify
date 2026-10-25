package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

final class FluidCompat {
    private FluidCompat() {
    }

    static List<FluidTank> read(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null) {
            return null;
        }
        try {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, entity.getBlockPos(), null);
            for (int i = 0; handler == null && i < Direction.values().length; i++) {
                handler = level.getCapability(Capabilities.FluidHandler.BLOCK, entity.getBlockPos(), Direction.values()[i]);
            }
            if (handler == null) {
                return null;
            }
            List<FluidTank> tanks = new ArrayList<>(2);
            for (int i = 0; i < handler.getTanks(); i++) {
                FluidStack stack = handler.getFluidInTank(i);
                int capacity = handler.getTankCapacity(i);
                if (capacity <= 0) {
                    continue;
                }
                tanks.add(new FluidTank(stack.isEmpty() ? null : stack.getHoverName().copy(), stack.getAmount(), capacity));
            }
            return tanks.isEmpty() ? null : tanks;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
