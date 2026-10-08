package dev.identify.look;

import net.minecraft.client.Minecraft;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import java.util.concurrent.atomic.AtomicBoolean;

final class ServerBeacon {
    private static final long REFRESH_TICKS = 10L;
    private static final int FIELDS = 3;
    private static final AtomicBoolean PENDING = new AtomicBoolean();

    private static volatile Snapshot latest;
    private static BlockPos requestedPos;
    private static int requestedDimension;
    private static long requestedAt;

    private ServerBeacon() {
    }

    static int[] fields(World world, BlockPos pos, TileEntityBeacon clientBeacon) {
        IntegratedServer server = Minecraft.getMinecraft().getIntegratedServer();
        int dimension = world.provider.getDimension();
        if (server != null) {
            request(server, world, dimension, pos);
            Snapshot snapshot = latest;
            if (snapshot != null && snapshot.pos.equals(pos) && snapshot.dimension == dimension) {
                return snapshot.fields;
            }
        }
        return read(clientBeacon);
    }

    private static void request(IntegratedServer server, World world, int dimension, BlockPos pos) {
        long now = world.getTotalWorldTime();
        boolean same = pos.equals(requestedPos) && dimension == requestedDimension;
        if (same && now >= requestedAt && now - requestedAt < REFRESH_TICKS) {
            return;
        }
        if (!PENDING.compareAndSet(false, true)) {
            return;
        }
        final BlockPos target = pos.toImmutable();
        requestedPos = target;
        requestedDimension = dimension;
        requestedAt = now;
        try {
            server.addScheduledTask(() -> {
                try {
                    WorldServer serverWorld = DimensionManager.getWorld(dimension);
                    TileEntity tile = serverWorld == null ? null : serverWorld.getTileEntity(target);
                    latest = tile instanceof TileEntityBeacon
                            ? new Snapshot(dimension, target, read((TileEntityBeacon) tile))
                            : null;
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

    private static int[] read(TileEntityBeacon beacon) {
        int[] fields = new int[FIELDS];
        for (int i = 0; i < FIELDS; i++) {
            fields[i] = beacon.getField(i);
        }
        return fields;
    }

    private static final class Snapshot {
        private final int dimension;
        private final BlockPos pos;
        private final int[] fields;

        private Snapshot(int dimension, BlockPos pos, int[] fields) {
            this.dimension = dimension;
            this.pos = pos;
            this.fields = fields;
        }
    }
}
