package dev.identify.look;

import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Method;

final class EnergyCompat {
    private static boolean loaded;
    private static Object lookup;
    private static Method find;
    private static Method amount;
    private static Method capacity;

    private EnergyCompat() {
    }

    static long[] read(BlockEntity entity) {
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
            long max = (Long) capacity.invoke(storage);
            if (max <= 0) {
                return null;
            }
            return new long[]{(Long) amount.invoke(storage), max};
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static synchronized boolean load() {
        if (!loaded) {
            loaded = true;
            try {
                Class<?> storage = Class.forName("team.reborn.energy.api.EnergyStorage");
                Class<?> lookupClass = Class.forName("net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup");
                Object sided = storage.getField("SIDED").get(null);
                Method method = null;
                for (Method candidate : lookupClass.getMethods()) {
                    if (candidate.getName().equals("find") && candidate.getParameterCount() == 3) {
                        method = candidate;
                    }
                }
                if (method != null) {
                    amount = storage.getMethod("getAmount");
                    capacity = storage.getMethod("getCapacity");
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
