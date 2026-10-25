package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class FluidCompat {
    private static final long DROPLETS_PER_MB = 81L;

    private static boolean loaded;
    private static Object lookup;
    private static Method find;
    private static Method iterator;
    private static Method contextIterator;
    private static Method openOuter;
    private static Method resource;
    private static Method amount;
    private static Method capacity;
    private static Method blank;
    private static Method fluid;
    private static Method name;

    private FluidCompat() {
    }

    static List<FluidTank> read(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null || !load()) {
            return null;
        }
        try {
            Object storage = find.invoke(lookup, level, entity.getBlockPos(), null);
            for (int i = 0; storage == null && i < Direction.values().length; i++) {
                storage = find.invoke(lookup, level, entity.getBlockPos(), Direction.values()[i]);
            }
            if (storage == null) {
                return null;
            }
            List<FluidTank> tanks = new ArrayList<>(2);
            if (iterator != null) {
                collect((Iterator<?>) iterator.invoke(storage), tanks);
            } else {
                try (AutoCloseable transaction = (AutoCloseable) openOuter.invoke(null)) {
                    collect((Iterator<?>) contextIterator.invoke(storage, transaction), tanks);
                }
            }
            return tanks.isEmpty() ? null : tanks;
        } catch (Exception e) {
            return null;
        }
    }

    private static void collect(Iterator<?> views, List<FluidTank> tanks) throws ReflectiveOperationException {
        while (views.hasNext()) {
            Object view = views.next();
            long max = (Long) capacity.invoke(view) / DROPLETS_PER_MB;
            if (max <= 0) {
                continue;
            }
            if ((Boolean) blank.invoke(view)) {
                tanks.add(new FluidTank(null, 0L, max));
            } else {
                tanks.add(new FluidTank(name(resource.invoke(view)), (Long) amount.invoke(view) / DROPLETS_PER_MB, max));
            }
        }
    }

    private static Component name(Object variant) throws ReflectiveOperationException {
        if (name != null) {
            try {
                return ((Component) name.invoke(null, variant)).copy();
            } catch (ReflectiveOperationException | RuntimeException e) {
                name = null;
            }
        }
        Fluid type = (Fluid) fluid.invoke(variant);
        return type.defaultFluidState().createLegacyBlock().getBlock().getName().copy();
    }

    private static synchronized boolean load() {
        if (!loaded) {
            loaded = true;
            try {
                Class<?> storages = Class.forName("net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage");
                Class<?> lookupClass = Class.forName("net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup");
                Class<?> storage = Class.forName("net.fabricmc.fabric.api.transfer.v1.storage.Storage");
                Class<?> view = Class.forName("net.fabricmc.fabric.api.transfer.v1.storage.StorageView");
                Class<?> variant = Class.forName("net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant");
                Object sided = storages.getField("SIDED").get(null);
                Method method = null;
                for (Method candidate : lookupClass.getMethods()) {
                    if (candidate.getName().equals("find") && candidate.getParameterCount() == 3) {
                        method = candidate;
                    }
                }
                try {
                    iterator = storage.getMethod("iterator");
                } catch (NoSuchMethodException e) {
                    Class<?> context = Class.forName("net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext");
                    contextIterator = storage.getMethod("iterator", context);
                    openOuter = Class.forName("net.fabricmc.fabric.api.transfer.v1.transaction.Transaction").getMethod("openOuter");
                }
                try {
                    name = Class.forName("net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes").getMethod("getName", variant);
                } catch (ReflectiveOperationException | LinkageError e) {
                    name = null;
                }
                resource = view.getMethod("getResource");
                amount = view.getMethod("getAmount");
                capacity = view.getMethod("getCapacity");
                blank = view.getMethod("isResourceBlank");
                fluid = variant.getMethod("getFluid");
                if (method != null) {
                    find = method;
                    lookup = sided;
                }
            } catch (ReflectiveOperationException | LinkageError e) {
                lookup = null;
            }
        }
        return lookup != null;
    }
}
