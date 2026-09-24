package dev.identify.look;

import net.minecraft.nbt.CompoundTag;

final class NbtCompat {
    private NbtCompat() {
    }

    static int intOr(CompoundTag tag, String key, int fallback) {
        return tag.contains(key) ? tag.getInt(key) : fallback;
    }

    static String stringOr(CompoundTag tag, String key, String fallback) {
        return tag.contains(key) ? tag.getString(key) : fallback;
    }
}
