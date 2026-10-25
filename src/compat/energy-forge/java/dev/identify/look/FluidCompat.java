package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

final class FluidCompat {
    private FluidCompat() {
    }

    static List<FluidTank> read(BlockEntity entity) {
        try {
            IFluidHandler handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, null).orElse(null);
            for (int i = 0; handler == null && i < Direction.values().length; i++) {
                handler = entity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.values()[i]).orElse(null);
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
                tanks.add(new FluidTank(stack.isEmpty() ? null : stack.getDisplayName().copy(), stack.getAmount(), capacity));
            }
            return tanks.isEmpty() ? null : tanks;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
