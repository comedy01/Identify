package dev.identify.showcase;

import com.mojang.datafixers.util.Pair;
import dev.identify.client.IdentifyClient;
import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.config.IdentifyConfig;
import dev.identify.showcase.mixin.ContainerScreenAccessor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.Vec3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class Showcase {
    private static final String WORLD = "identify-showcase";
    private static final long SETTLE_NANOS = 3_000_000_000L;
    private static final int GLIDE = 24;
    private static final int BLOCKS = 20;
    private static final int FARM = 10;
    private static final int MOBS = 0;
    private static final int ANIMALS = -10;
    private static final int BEACON = -20;
    private static final int ROW = -8;
    private static final int STAND = -13;
    private static final int EDGE = 28;
    private static final String[] ARROW = {
            "B",
            "BB",
            "BWB",
            "BWWB",
            "BWWWB",
            "BWWWWB",
            "BWWWWWB",
            "BWWWWWWB",
            "BWWWWWWWB",
            "BWWWWWWWWB",
            "BWWWWWWWWWB",
            "BWWWWWWBBBBB",
            "BWWWBWWB",
            "BWWB BWWB",
            "BWB  BWWB",
            "BB    BWWB",
            "B     BWWB",
            "       BWWB",
            "        BB"};
    private static Showcase instance;

    private final Path out;
    private final Recorder recorder;
    private final long seed;
    private final List<Action> actions = new ArrayList<>();
    private final AtomicReference<BlockPos> site = new AtomicReference<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int frame;
    private long actionStart;
    private Path pendingStill;
    private boolean cursorShown;
    private boolean shift;
    private double cursorX;
    private double cursorY;

    private interface Action {
        boolean run(int frame);
    }

    private record Key(int move, int hold, Vec3 target) {
    }

    private record Stop(int frame, Supplier<double[]> where) {
    }

    private Showcase(Path out, String ffmpeg, long seed) {
        this.out = out;
        this.recorder = new Recorder(ffmpeg);
        this.seed = seed;
    }

    public static void start() {
        String ffmpeg = System.getProperty("identify.ffmpeg", "ffmpeg");
        long seed = Long.parseLong(System.getProperty("identify.seed", "12345"));
        instance = new Showcase(Path.of(System.getProperty("identify.showcase")), ffmpeg, seed);
    }

    public static boolean hideHand() {
        return instance != null && instance.started;
    }

    public static boolean shiftDown() {
        return instance != null && instance.shift;
    }

    public static void frame() {
        if (instance != null) {
            instance.onFrame();
        }
    }

    public static void drawCursor(GuiGraphicsExtractor graphics) {
        if (instance == null || !instance.started || !instance.cursorShown) {
            return;
        }
        float cell = 2.0F / (float) Minecraft.getInstance().getWindow().getGuiScale();
        graphics.nextStratum();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) instance.cursorX, (float) instance.cursorY);
        graphics.pose().scale(cell, cell);
        for (int row = 0; row < ARROW.length; row++) {
            String line = ARROW[row];
            int x = 0;
            while (x < line.length()) {
                char c = line.charAt(x);
                int end = x;
                while (end < line.length() && line.charAt(end) == c) {
                    end++;
                }
                if (c != ' ') {
                    graphics.fill(x, row, end, row + 1, c == 'B' ? 0xFF000000 : 0xFFFFFFFF);
                }
                x = end;
            }
        }
        graphics.pose().popMatrix();
    }

    private void onFrame() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.level == null && Screens.overlay(mc) == null && Screens.current(mc) != null && ++idle > 120) {
                started = true;
                setUpOptions(mc);
                plan(mc);
                log("creating world with seed " + seed);
                deleteWorld(mc);
                Worlds.create(mc, WORLD, false, seed);
            }
            return;
        }
        Clock.step();
        mc.gui.toastManager().clear();
        try {
            while (index < actions.size()) {
                if (frame == 0) {
                    actionStart = System.nanoTime();
                }
                if (!actions.get(index).run(frame++)) {
                    if (cursorShown && Screens.current(mc) != null) {
                        double scale = mc.getWindow().getGuiScale();
                        Ui.moveMouse(mc, cursorX * scale, cursorY * scale);
                    }
                    return;
                }
                index++;
                frame = 0;
            }
        } catch (Throwable e) {
            log("FAILED: " + e);
            e.printStackTrace();
        }
        finish(mc);
    }

    private void finish(Minecraft mc) {
        finished = true;
        shift = false;
        Clock.release();
        Thread watchdog = new Thread(() -> {
            try {
                Thread.sleep(90_000L);
            } catch (InterruptedException e) {
                return;
            }
            log("the game did not close, forcing it");
            Runtime.getRuntime().halt(1);
        }, "identify-showcase-watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
        IdentifyClient.config().resetToDefaults();
        IdentifyClient.saveConfig();
        log("showcase done");
        mc.stop();
    }

    private static void setUpOptions(Minecraft mc) {
        mc.options.pauseOnLostFocus = false;
        mc.options.tutorialStep = TutorialSteps.NONE;
        mc.options.chatVisibility().set(ChatVisiblity.HIDDEN);
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.renderDistance().set(12);
        mc.options.bobView().set(false);
        mc.options.setCameraType(CameraType.FIRST_PERSON);
        mc.options.guiScale().set(4);
        mc.resizeGui();
    }

    private void once(Runnable action) {
        actions.add(f -> {
            action.run();
            return true;
        });
    }

    private void until(BooleanSupplier ready) {
        actions.add(f -> ready.getAsBoolean());
    }

    private void holdFor(int frames, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= frames;
        });
    }

    private void settle(Minecraft mc, Runnable hold) {
        actions.add(f -> {
            hold.run();
            return f >= 90 && System.nanoTime() - actionStart > SETTLE_NANOS
                    && (mc.levelRenderer.hasRenderedAllSections() || System.nanoTime() - actionStart > 20 * SETTLE_NANOS);
        });
    }

    private void record(String name, int frames, IntConsumer script) {
        actions.add(f -> {
            if (f == 0) {
                log("recording " + name);
                recorder.start(out.resolve("clips").resolve(name + ".mp4"));
            } else {
                Path still = pendingStill;
                pendingStill = null;
                recorder.capture(still);
            }
            if (f < frames) {
                script.accept(f);
                return false;
            }
            return true;
        });
        until(recorder::drained);
        once(recorder::stop);
    }

    private void shot(String name) {
        pendingStill = out.resolve("stills").resolve(name + ".png");
    }

    static double smooth(double t) {
        t = Math.max(0.0, Math.min(1.0, t));
        return t * t * (3.0 - 2.0 * t);
    }

    static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static Vec3 lerp(Vec3 a, Vec3 b, double t) {
        return a.add(b.subtract(a).scale(t));
    }

    static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static void cmd(Minecraft mc, String format, Object... args) {
        run(mc, String.format(Locale.ROOT, format, args));
    }

    private static void face(Minecraft mc, double yaw, double pitch) {
        LocalPlayer player = mc.player;
        player.setYRot((float) yaw);
        player.setXRot((float) pitch);
        player.yRotO = (float) yaw;
        player.xRotO = (float) pitch;
        player.setYHeadRot((float) yaw);
        player.yHeadRotO = (float) yaw;
    }

    private static void lookAt(Minecraft mc, Vec3 target) {
        Vec3 eye = mc.player.getEyePosition();
        double dx = target.x - eye.x;
        double dy = target.y - eye.y;
        double dz = target.z - eye.z;
        face(mc, Math.toDegrees(Math.atan2(-dx, dz)), -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
    }

    private static void follow(Minecraft mc, int f, Key... keys) {
        Vec3 target = keys[0].target();
        int t = f - keys[0].hold();
        for (int i = 1; i < keys.length && t >= 0; i++) {
            Key key = keys[i];
            if (t < key.move()) {
                target = lerp(keys[i - 1].target(), key.target(), smooth(t / (double) key.move()));
                break;
            }
            t -= key.move();
            target = key.target();
            t -= key.hold();
        }
        lookAt(mc, target);
    }

    private void cursor(int f, Stop... stops) {
        double[] at = stops[0].where().get();
        for (int i = 1; i < stops.length; i++) {
            Stop stop = stops[i];
            if (f >= stop.frame()) {
                at = stop.where().get();
            } else if (f >= stop.frame() - GLIDE) {
                double[] to = stop.where().get();
                double s = smooth((f - (stop.frame() - GLIDE)) / (double) GLIDE);
                at = new double[] {lerp(at[0], to[0], s), lerp(at[1], to[1], s)};
                break;
            } else {
                break;
            }
        }
        cursorShown = true;
        cursorX = at[0];
        cursorY = at[1];
    }

    private static void select(Minecraft mc, int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
    }

    private static boolean wanted(String scene) {
        String only = System.getProperty("identify.scenes");
        return only == null || List.of(only.split(",")).contains(scene);
    }

    private Vec3 at(double dx, double dy, double dz) {
        BlockPos c = site.get();
        return new Vec3(c.getX() + dx + 0.5, c.getY() + dy, c.getZ() + dz + 0.5);
    }

    private BlockPos block(int dx, int dy, int dz) {
        return site.get().offset(dx, dy, dz);
    }

    private static int height(ServerLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
    }

    private void locate(Minecraft mc) {
        once(() -> {
            IntegratedServer server = mc.getSingleplayerServer();
            server.execute(() -> {
                ServerLevel level = server.overworld();
                ChunkGenerator generator = level.getChunkSource().getGenerator();
                RandomState noise = level.getChunkSource().randomState();
                Pair<BlockPos, Holder<Biome>> pair = level.findClosestBiome3d(h -> h.is(Biomes.PLAINS),
                        BlockPos.ZERO, 6400, 32, 64);
                BlockPos found = pair == null ? BlockPos.ZERO : pair.getFirst();
                BlockPos best = null;
                int bestScore = Integer.MAX_VALUE;
                for (int ox = -1536; ox <= 1536; ox += 96) {
                    for (int oz = -1536; oz <= 1536; oz += 96) {
                        int cx = found.getX() + ox;
                        int cz = found.getZ() + oz;
                        int centre = generator.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, level, noise);
                        if (!level.getBiome(new BlockPos(cx, centre, cz)).is(Biomes.PLAINS)) {
                            continue;
                        }
                        List<Integer> heights = new ArrayList<>();
                        int wet = 0;
                        for (int dx = -40; dx <= 40; dx += 8) {
                            for (int dz = -40; dz <= 40; dz += 8) {
                                int top = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.WORLD_SURFACE_WG,
                                        level, noise);
                                int floor = generator.getBaseHeight(cx + dx, cz + dz, Heightmap.Types.OCEAN_FLOOR_WG,
                                        level, noise);
                                heights.add(top);
                                if (top != floor) {
                                    wet++;
                                }
                            }
                        }
                        int usual = mode(heights);
                        int score = wet * 3;
                        for (int h : heights) {
                            if (Math.abs(h - usual) > 1) {
                                score++;
                            }
                        }
                        if (score < bestScore) {
                            bestScore = score;
                            best = new BlockPos(cx, centre, cz);
                        }
                    }
                }
                site.set(best == null ? found : best);
                log("plains at " + site.get().toShortString() + ", " + bestScore + " uneven samples");
            });
        });
        until(() -> site.get() != null);
    }

    private static int mode(List<Integer> values) {
        Map<Integer, Integer> counts = new HashMap<>();
        int best = values.get(0);
        for (int value : values) {
            int count = counts.merge(value, 1, Integer::sum);
            if (count > counts.get(best)) {
                best = value;
            }
        }
        return best;
    }

    private void level(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        int ground = server.submit(() -> {
            ServerLevel level = server.overworld();
            BlockPos c = site.get();
            List<Integer> heights = new ArrayList<>();
            for (int dx = -EDGE; dx <= EDGE; dx += 2) {
                for (int dz = -EDGE; dz <= EDGE; dz += 2) {
                    heights.add(height(level, c.getX() + dx, c.getZ() + dz));
                }
            }
            return mode(heights);
        }).join();
        BlockPos c = site.get();
        site.set(new BlockPos(c.getX(), ground + 1, c.getZ()));
        log("set stands at y " + (ground + 1));
    }

    private static void fill(Minecraft mc, int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        int layers = Math.max(1, 32768 / ((Math.abs(x2 - x1) + 1) * (Math.abs(z2 - z1) + 1)));
        for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y += layers) {
            cmd(mc, "fill %d %d %d %d %d %d %s", x1, y, z1, x2, Math.min(y + layers - 1, Math.max(y1, y2)), z2, block);
        }
    }

    private void area(Minecraft mc, int dx1, int dy1, int dz1, int dx2, int dy2, int dz2, String block) {
        BlockPos a = block(dx1, dy1, dz1);
        BlockPos b = block(dx2, dy2, dz2);
        fill(mc, a.getX(), a.getY(), a.getZ(), b.getX(), b.getY(), b.getZ(), block);
    }

    private void put(Minecraft mc, int dx, int dy, int dz, String block) {
        BlockPos p = block(dx, dy, dz);
        cmd(mc, "setblock %d %d %d %s", p.getX(), p.getY(), p.getZ(), block);
    }

    private void summon(Minecraft mc, String type, Vec3 at, String extra) {
        cmd(mc, "summon minecraft:%s %.2f %.2f %.2f {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"set\"],"
                + "Rotation:[180f,0f]%s}", type, at.x, at.y, at.z, extra);
    }

    private static String uuid(Minecraft mc) {
        UUID id = mc.player.getUUID();
        long hi = id.getMostSignificantBits();
        long lo = id.getLeastSignificantBits();
        return String.format(Locale.ROOT, "[I;%d,%d,%d,%d]", (int) (hi >> 32), (int) hi, (int) (lo >> 32), (int) lo);
    }

    private void buildSet(Minecraft mc) {
        BlockPos c = site.get();
        int x = c.getX();
        int z = c.getZ();
        int ground = c.getY() - 1;
        fill(mc, x - EDGE, ground + 1, z - EDGE, x + EDGE, ground + 40, z + EDGE, "minecraft:air");
        fill(mc, x - EDGE, ground - 6, z - EDGE, x + EDGE, ground - 1, z + EDGE, "minecraft:dirt");
        fill(mc, x - EDGE, ground, z - EDGE, x + EDGE, ground, z + EDGE, "minecraft:grass_block");
        cmd(mc, "place feature minecraft:fancy_oak %d %d %d", x, ground + 1, z + 14);
        String[] kinds = {"oak", "birch", "fancy_oak"};
        int[][] trees = {{-9, 12}, {8, 17}, {-16, 19}, {15, 10}, {-23, 8}, {23, 6}, {-5, 24}, {19, 23}, {25, 16},
                {-25, 18}, {-20, -24}, {8, -25}, {22, -23}, {-6, -26}};
        for (int i = 0; i < trees.length; i++) {
            cmd(mc, "place feature minecraft:%s %d %d %d", kinds[i % kinds.length], x + trees[i][0], ground + 1,
                    z + trees[i][1]);
        }
        Random random = new Random(seed);
        String[] flowers = {"dandelion", "poppy", "oxeye_daisy", "cornflower", "azure_bluet"};
        for (int dx = -EDGE + 1; dx <= EDGE - 1; dx++) {
            for (int dz = -EDGE + 1; dz <= EDGE - 1; dz++) {
                if (dz >= STAND - 2 && dz <= ROW + 1 || Math.abs(dx) <= 6 && dz >= -1 && dz <= 6) {
                    continue;
                }
                double roll = random.nextDouble();
                String block;
                if (roll < 0.12) {
                    block = "short_grass";
                } else if (roll < 0.145) {
                    block = flowers[random.nextInt(flowers.length)];
                } else {
                    continue;
                }
                cmd(mc, "setblock %d %d %d minecraft:%s keep", x + dx, ground + 1, z + dz, block);
            }
        }

        put(mc, BLOCKS + 2, 0, ROW, "minecraft:oak_log");
        put(mc, BLOCKS + 1, 0, ROW, "minecraft:iron_ore");
        put(mc, BLOCKS, 0, ROW, "minecraft:diamond_ore");
        put(mc, BLOCKS - 1, 0, ROW, "minecraft:obsidian");

        area(mc, FARM + 2, -1, ROW - 1, FARM + 3, -1, ROW, "minecraft:farmland[moisture=7]");
        put(mc, FARM + 3, 0, ROW - 1, "minecraft:wheat[age=3]");
        put(mc, FARM + 2, 0, ROW - 1, "minecraft:wheat[age=7]");
        put(mc, FARM + 3, 0, ROW, "minecraft:wheat[age=5]");
        put(mc, FARM + 2, 0, ROW, "minecraft:wheat[age=7]");
        put(mc, FARM, 0, ROW, "minecraft:beehive[facing=north,honey_level=3]");
        put(mc, FARM - 2, 0, ROW, "minecraft:redstone_wire");
        put(mc, FARM - 3, 0, ROW, "minecraft:redstone_block");

        put(mc, BEACON + 2, 0, ROW, "minecraft:spawner{SpawnCount:0s,Delay:20000s,MinSpawnDelay:20000s,"
                + "MaxSpawnDelay:20000s,SpawnData:{entity:{id:\"minecraft:blaze\"}}}");
        for (int tier = 1; tier <= 4; tier++) {
            area(mc, BEACON - 2 - tier, -tier, ROW - tier, BEACON - 2 + tier, -tier, ROW + tier,
                    tier == 1 ? "minecraft:gold_block" : "minecraft:iron_block");
        }
        put(mc, BEACON - 2, 0, ROW, "minecraft:beacon{primary_effect:\"minecraft:speed\","
                + "secondary_effect:\"minecraft:regeneration\"}");

        run(mc, "kill @e[type=!minecraft:player]");
        summon(mc, "sheep", at(3, 0, 4), ",Color:0b");
        summon(mc, "cow", at(0, 0, 4), "");
        summon(mc, "pig", at(-3, 0, 4), "");
        String owner = uuid(mc);
        summon(mc, "zombie", at(MOBS + 1.5, 0, ROW), ",equipment:{head:{id:\"minecraft:iron_helmet\"},"
                + "chest:{id:\"minecraft:iron_chestplate\"},mainhand:{id:\"minecraft:iron_sword\"}},"
                + "active_effects:[{id:\"minecraft:speed\",amplifier:1b,duration:-1,show_particles:1b},"
                + "{id:\"minecraft:strength\",amplifier:0b,duration:-1,show_particles:1b}]");
        summon(mc, "villager", at(MOBS - 1.5, 0, ROW), ",VillagerData:{profession:\"minecraft:librarian\",level:4,"
                + "type:\"minecraft:plains\"}");
        summon(mc, "wolf", at(ANIMALS, 0, ROW), ",Owner:" + owner + ",CollarColor:14b");
        summon(mc, "horse", at(ANIMALS - 2.5, 0, ROW), ",Tame:1b,Owner:" + owner + ",Variant:3,Health:30f,"
                + "equipment:{saddle:{id:\"minecraft:saddle\"}},attributes:[{id:\"minecraft:max_health\",base:30d},"
                + "{id:\"minecraft:movement_speed\",base:0.3375d},{id:\"minecraft:jump_strength\",base:0.9d}]");
        kit(mc);
    }

    private void baby(Minecraft mc) {
        run(mc, "kill @e[type=minecraft:cow,tag=baby]");
        summon(mc, "cow", at(ANIMALS + 2.5, 0, ROW), ",Age:-24000,Tags:[\"set\",\"baby\"]");
    }

    private static void kit(Minecraft mc) {
        run(mc, "clear @p");
        run(mc, "item replace entity @p hotbar.0 with minecraft:iron_sword");
        run(mc, "item replace entity @p hotbar.1 with minecraft:iron_pickaxe");
        run(mc, "item replace entity @p hotbar.2 with minecraft:oak_log 64");
        run(mc, "item replace entity @p hotbar.3 with minecraft:torch 64");
        run(mc, "item replace entity @p hotbar.4 with minecraft:bread 16");
    }

    private void scene(Minecraft mc, IdentifyConfig config, Vec3 feet, Runnable setup, Runnable hold) {
        once(() -> {
            Screens.open(mc, null);
            config.resetToDefaults();
            cursorShown = false;
            shift = false;
            run(mc, "gamemode creative @p");
            cmd(mc, "tp @p %.3f %.3f %.3f", feet.x, feet.y, feet.z);
            run(mc, "kill @e[type=!minecraft:player,tag=!set]");
            kit(mc);
            select(mc, 8);
        });
        holdFor(20, hold);
        once(setup);
        settle(mc, hold);
    }

    private void plan(Minecraft mc) {
        IdentifyConfig config = IdentifyClient.config();

        until(() -> mc.level != null && mc.player != null && Screens.current(mc) == null
                && mc.getSingleplayerServer() != null);
        once(() -> {
            log("world loaded");
            Clock.fix();
            config.resetToDefaults();
            run(mc, "time set 2500");
            run(mc, "weather clear 1000000");
            run(mc, "gamerule advance_time false");
            run(mc, "gamerule spawn_mobs false");
            run(mc, "gamerule random_tick_speed 0");
        });
        locate(mc);
        once(() -> {
            BlockPos c = site.get();
            cmd(mc, "forceload add %d %d %d %d", c.getX() - 40, c.getZ() - 40, c.getX() + 40, c.getZ() + 40);
            cmd(mc, "tp @p %d %d %d", c.getX(), c.getY() + 45, c.getZ());
            mc.player.getAbilities().flying = true;
            mc.player.onUpdateAbilities();
        });
        settle(mc, () -> {
        });
        once(() -> level(mc));
        once(() -> buildSet(mc));
        holdFor(60, () -> {
        });
        once(() -> {
            mc.player.getAbilities().flying = false;
            mc.player.onUpdateAbilities();
        });
        once(() -> film(mc, config));
    }

    private void film(Minecraft mc, IdentifyConfig config) {
        Vec3 spot = at(0, 0, 0);

        if (wanted("intro")) {
            Key sheep = new Key(0, 70, at(3, 0.8, 4));
            Key cow = new Key(50, 70, at(0, 0.9, 4));
            Key pig = new Key(50, 90, at(-3, 0.6, 4));
            scene(mc, config, spot, () -> {
            }, () -> follow(mc, 0, sheep));
            record("01-intro", 330, f -> {
                follow(mc, f, sheep, cow, pig);
                if (f == 140) {
                    shot("intro");
                }
            });
        }

        if (wanted("blocks")) {
            Vec3 stand = at(BLOCKS + 0.5, 0, STAND);
            Key log = new Key(0, 50, at(BLOCKS + 2, 0.5, ROW));
            Key iron = new Key(18, 56, at(BLOCKS + 1, 0.5, ROW));
            Key diamond = new Key(18, 56, at(BLOCKS, 0.5, ROW));
            Key obsidian = new Key(18, 84, at(BLOCKS - 1, 0.5, ROW));
            scene(mc, config, stand, () -> {
            }, () -> follow(mc, 0, log));
            record("02-blocks", 300, f -> {
                follow(mc, f, log, iron, diamond, obsidian);
                if (f == 40) {
                    shot("block-oak-log");
                } else if (f == 180) {
                    shot("block-diamond-ore");
                } else if (f == 270) {
                    shot("block-obsidian");
                }
            });
        }

        if (wanted("farm")) {
            Vec3 stand = at(FARM + 0.5, 0, STAND);
            Key young = new Key(0, 60, at(FARM + 3, 0.3, ROW - 1));
            Key ripe = new Key(18, 60, at(FARM + 2, 0.6, ROW - 1));
            Key hive = new Key(24, 64, at(FARM, 0.5, ROW));
            Key wire = new Key(24, 80, at(FARM - 2, 0.03, ROW));
            scene(mc, config, stand, () -> {
            }, () -> follow(mc, 0, young));
            record("03-farm", 330, f -> {
                follow(mc, f, young, ripe, hive, wire);
                if (f == 50) {
                    shot("crop-growth");
                } else if (f == 120) {
                    shot("crop-grown");
                } else if (f == 200) {
                    shot("beehive");
                } else if (f == 310) {
                    shot("redstone");
                }
            });
        }

        if (wanted("beacon")) {
            Vec3 stand = at(BEACON, 0, STAND);
            Key spawner = new Key(0, 100, at(BEACON + 2, 0.5, ROW));
            Key beacon = new Key(28, 142, at(BEACON - 2, 0.5, ROW));
            scene(mc, config, stand, () -> {
            }, () -> follow(mc, 0, spawner));
            record("04-beacon", 270, f -> {
                follow(mc, f, spawner, beacon);
                if (f == 80) {
                    shot("spawner");
                } else if (f == 230) {
                    shot("beacon");
                }
            });
        }

        if (wanted("mobs")) {
            Vec3 stand = at(MOBS, 0, STAND);
            Key zombie = new Key(0, 120, at(MOBS + 1.5, 1.0, ROW));
            Key villager = new Key(28, 122, at(MOBS - 1.5, 1.1, ROW));
            scene(mc, config, stand, () -> {
            }, () -> follow(mc, 0, zombie));
            record("05-mobs", 270, f -> {
                follow(mc, f, zombie, villager);
                if (f == 100) {
                    shot("mob-zombie");
                } else if (f == 240) {
                    shot("mob-villager");
                }
            });
        }

        if (wanted("animals")) {
            Vec3 stand = at(ANIMALS, 0, STAND);
            Key calf = new Key(0, 96, at(ANIMALS + 2.5, 0.45, ROW));
            Key wolf = new Key(26, 80, at(ANIMALS, 0.5, ROW));
            Key horse = new Key(26, 102, at(ANIMALS - 2.5, 1.1, ROW));
            scene(mc, config, stand, () -> baby(mc), () -> follow(mc, 0, calf));
            record("06-animals", 330, f -> {
                follow(mc, f, calf, wolf, horse);
                if (f == 80) {
                    shot("animal-baby");
                } else if (f == 190) {
                    shot("animal-wolf");
                } else if (f == 300) {
                    shot("animal-horse");
                }
            });
        }

        if (wanted("tooltips")) {
            Vec3 stand = at(FARM, 0, STAND);
            scene(mc, config, stand, () -> {
                run(mc, "gamemode survival @p");
                run(mc, "item replace entity @p inventory.1 with minecraft:diamond_pickaxe[minecraft:damage=412]");
                run(mc, "item replace entity @p inventory.3 with minecraft:golden_sword[minecraft:damage=27]");
                run(mc, "item replace entity @p inventory.5 with minecraft:golden_carrot 24");
                run(mc, "item replace entity @p inventory.7 with minecraft:coal 32");
                run(mc, "item replace entity @p inventory.11 with minecraft:cooked_beef 12");
                run(mc, "item replace entity @p inventory.15 with minecraft:lava_bucket");
                run(mc, "item replace entity @p inventory.19 with minecraft:cobblestone 64");
            }, () -> lookAt(mc, at(FARM, 0.5, ROW)));
            once(() -> Screens.open(mc, new InventoryScreen(mc.player)));
            holdFor(20, () -> cursor(0, new Stop(0, () -> rest(mc))));
            record("07-tooltips", 360, f -> {
                cursor(f, new Stop(0, () -> rest(mc)),
                        new Stop(36, () -> slotAt(mc, "diamond_pickaxe")),
                        new Stop(116, () -> slotAt(mc, "golden_sword")),
                        new Stop(196, () -> slotAt(mc, "golden_carrot")),
                        new Stop(276, () -> slotAt(mc, "coal")));
                if (f == 85) {
                    shot("tooltip-durability");
                } else if (f == 165) {
                    shot("tooltip-durability-low");
                } else if (f == 245) {
                    shot("tooltip-food");
                } else if (f == 345) {
                    shot("tooltip-fuel");
                }
            });
        }

        if (wanted("compare")) {
            Vec3 stand = at(FARM, 0, STAND);
            scene(mc, config, stand, () -> {
                run(mc, "gamemode survival @p");
                run(mc, "item replace entity @p armor.head with minecraft:iron_helmet");
                run(mc, "item replace entity @p armor.chest with minecraft:iron_chestplate");
                run(mc, "item replace entity @p inventory.2 with minecraft:diamond_chestplate");
                run(mc, "item replace entity @p inventory.6 with minecraft:netherite_sword");
                run(mc, "item replace entity @p inventory.12 with minecraft:shield");
                run(mc, "item replace entity @p inventory.19 with minecraft:cobblestone 64");
                select(mc, 0);
            }, () -> lookAt(mc, at(FARM, 0.5, ROW)));
            once(() -> Screens.open(mc, new InventoryScreen(mc.player)));
            holdFor(20, () -> cursor(0, new Stop(0, () -> rest(mc))));
            record("08-compare", 330, f -> {
                cursor(f, new Stop(0, () -> rest(mc)),
                        new Stop(36, () -> slotAt(mc, "diamond_chestplate")),
                        new Stop(210, () -> slotAt(mc, "netherite_sword")));
                if (f == 100) {
                    shift = true;
                }
                if (f == 80) {
                    shot("compare-hint");
                } else if (f == 170) {
                    shot("compare-armor");
                } else if (f == 310) {
                    shot("compare-sword");
                }
            });
            once(() -> shift = false);
        }

        if (wanted("placement")) {
            Vec3 stand = at(MOBS, 0, STAND);
            Vec3 zombie = at(MOBS + 1.5, 1.0, ROW);
            int[][] spots = {{50, 0}, {0, 0}, {100, 0}, {100, 50}, {0, 50}, {50, 0}};
            scene(mc, config, stand, () -> {
            }, () -> lookAt(mc, zombie));
            record("09-placement", 330, f -> {
                int[] pos = spots[Math.min(spots.length - 1, f / 55)];
                config.setXPosition(pos[0]);
                config.setYPosition(pos[1]);
                lookAt(mc, zombie);
                if (f == 80) {
                    shot("placement-top-left");
                } else if (f == 190) {
                    shot("placement-right");
                }
            });
        }

        if (wanted("settings")) {
            Vec3 stand = at(MOBS, 0, STAND);
            Vec3 villager = at(MOBS - 1.5, 1.1, ROW);
            scene(mc, config, stand, () -> {
            }, () -> lookAt(mc, villager));
            once(() -> Screens.open(mc, new IdentifySettingsScreen(null, mc.options)));
            holdFor(20, () -> cursor(0, new Stop(0, () -> corner(mc))));
            record("10-settings", 330, f -> {
                if (f < 216) {
                    cursor(f, new Stop(0, () -> corner(mc)),
                            new Stop(40, () -> labelAt(mc, "Mod Name", 0.5)),
                            new Stop(95, () -> sliderAt(mc, "Horizontal", 1.0)),
                            new Stop(150, () -> sliderAt(mc, "Vertical", 0.3)));
                }
                if (f == 50) {
                    clickLabel(mc, "Mod Name", 0.5);
                } else if (f == 105) {
                    clickSlider(mc, "Horizontal", 1.0);
                } else if (f == 160) {
                    clickSlider(mc, "Vertical", 0.3);
                } else if (f == 190) {
                    shot("settings");
                } else if (f == 216) {
                    cursorShown = false;
                    Screens.open(mc, null);
                }
                lookAt(mc, villager);
                if (f == 300) {
                    shot("settings-result");
                }
            });
            once(() -> Screens.open(mc, null));
        }

        if (wanted("outro")) {
            once(() -> run(mc, "time set 12300"));
            Key from = new Key(0, 0, at(-4, 0.8, 4));
            Key to = new Key(330, 0, at(4, 0.8, 4));
            scene(mc, config, spot, () -> {
            }, () -> follow(mc, 0, from, to));
            record("11-outro", 330, f -> {
                follow(mc, f, from, to);
                if (f == 150) {
                    shot("sunset");
                }
            });
            once(() -> run(mc, "time set 2500"));
        }
    }

    private static double[] corner(Minecraft mc) {
        Screen screen = Screens.current(mc);
        return new double[] {screen.width - 36, screen.height - 30};
    }

    private static double[] rest(Minecraft mc) {
        Screen screen = Screens.current(mc);
        return new double[] {screen.width * 0.5 + 118, screen.height * 0.5 - 28};
    }

    private static double[] slotAt(Minecraft mc, String item) {
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) Screens.current(mc);
        ContainerScreenAccessor access = (ContainerScreenAccessor) screen;
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.getItem().isEmpty()
                    && BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).getPath().equals(item)) {
                return new double[] {access.idShowcase$leftPos() + slot.x + 8, access.idShowcase$topPos() + slot.y + 8};
            }
        }
        throw new IllegalStateException("no " + item + " on screen");
    }

    private static double[] labelAt(Minecraft mc, String label, double along) {
        AbstractWidget widget = find(Screens.current(mc), label);
        if (widget == null) {
            throw new IllegalStateException("no widget " + label);
        }
        return new double[] {widget.getX() + widget.getWidth() * along, widget.getY() + widget.getHeight() / 2.0};
    }

    private static void clickLabel(Minecraft mc, String label, double along) {
        double[] at = labelAt(mc, label, along);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static double[] sliderAt(Minecraft mc, String label, double value) {
        AbstractWidget widget = find(Screens.current(mc), label);
        if (widget == null) {
            throw new IllegalStateException("no slider " + label);
        }
        return new double[] {widget.getX() + 4 + value * (widget.getWidth() - 8), widget.getY() + widget.getHeight() / 2.0};
    }

    private static void clickSlider(Minecraft mc, String label, double value) {
        double[] at = sliderAt(mc, label, value);
        Ui.click(Screens.current(mc), at[0], at[1]);
    }

    private static AbstractWidget find(GuiEventListener node, String label) {
        if (node instanceof AbstractWidget widget && widget.getMessage().getString().startsWith(label)) {
            return widget;
        }
        if (node instanceof ContainerEventHandler container) {
            for (GuiEventListener child : container.children()) {
                AbstractWidget found = find(child, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void deleteWorld(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        if (Files.exists(dir)) {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    static void log(String message) {
        System.out.println("[identify-showcase] " + message);
    }
}
