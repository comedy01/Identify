package dev.identify.look;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.concurrent.atomic.AtomicBoolean;

final class ServerBeacon {
    private static final long REFRESH_TICKS = 10L;
    private static final AtomicBoolean PENDING = new AtomicBoolean();

    private static volatile Snapshot latest;
    private static BlockPos requestedPos;
    private static ResourceKey<Level> requestedDimension;
    private static long requestedAt;

    private ServerBeacon() {
    }

    static CompoundTag tag(Level level, BlockPos pos, BeaconBlockEntity clientBeacon) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server != null) {
            request(server, level, pos);
            Snapshot snapshot = latest;
            if (snapshot != null && snapshot.pos().equals(pos) && snapshot.dimension().equals(level.dimension())) {
                return snapshot.tag();
            }
        }
        return BlockEntityCompat.beaconTag(level, clientBeacon);
    }

    private static void request(MinecraftServer server, Level level, BlockPos pos) {
        long now = level.getGameTime();
        ResourceKey<Level> dimension = level.dimension();
        boolean same = pos.equals(requestedPos) && dimension.equals(requestedDimension);
        if (same && now >= requestedAt && now - requestedAt < REFRESH_TICKS) {
            return;
        }
        if (!PENDING.compareAndSet(false, true)) {
            return;
        }
        BlockPos target = pos.immutable();
        requestedPos = target;
        requestedDimension = dimension;
        requestedAt = now;
        try {
            server.execute(() -> {
                try {
                    latest = read(server, dimension, target);
                } catch (RuntimeException e) {
                    latest = null;
                } finally {
                    PENDING.set(false);
                }
            });
        } catch (RuntimeException e) {
            PENDING.set(false);
        }
    }

    private static Snapshot read(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return null;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof BeaconBlockEntity beacon)) {
            return null;
        }
        return new Snapshot(dimension, pos, BlockEntityCompat.beaconTag(level, beacon));
    }

    private record Snapshot(ResourceKey<Level> dimension, BlockPos pos, CompoundTag tag) {
    }
}
