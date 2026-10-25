package dev.identify.look;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

final class ServerBlockData {
    private static final long REFRESH_TICKS = 10L;
    private static final AtomicBoolean PENDING = new AtomicBoolean();

    private static volatile Snapshot latest;
    private static BlockPos requestedPos;
    private static ResourceKey<Level> requestedDimension;
    private static long requestedAt;

    private ServerBlockData() {
    }

    @SuppressWarnings("unchecked")
    static <T> T get(Level level, BlockPos pos, Function<BlockEntity, T> reader) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        if (server == null) {
            return null;
        }
        request(server, level, pos, reader);
        Snapshot snapshot = latest;
        if (snapshot != null && snapshot.pos().equals(pos) && snapshot.dimension().equals(level.dimension())
                && snapshot.block() == level.getBlockState(pos).getBlock()) {
            return (T) snapshot.value();
        }
        return null;
    }

    private static void request(MinecraftServer server, Level level, BlockPos pos, Function<BlockEntity, ?> reader) {
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
                    latest = read(server, dimension, target, reader);
                } catch (RuntimeException | LinkageError e) {
                    latest = null;
                } finally {
                    PENDING.set(false);
                }
            });
        } catch (RuntimeException | LinkageError e) {
            PENDING.set(false);
        }
    }

    private static Snapshot read(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos,
                                 Function<BlockEntity, ?> reader) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) {
            return null;
        }
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return null;
        }
        Object value = reader.apply(entity);
        return value == null ? null : new Snapshot(dimension, pos, level.getBlockState(pos).getBlock(), value);
    }

    private record Snapshot(ResourceKey<Level> dimension, BlockPos pos, Block block, Object value) {
    }
}
