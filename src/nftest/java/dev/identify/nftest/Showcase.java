package dev.identify.nftest;

import dev.identify.client.IdentifyClient;
import dev.identify.look.BlockDetails;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Showcase {
    private record Part(int dx, String spec, int placeAt, boolean feed, Direction side, String[] items, int look) {
    }

    private record Station(String name, List<Part> parts) {
    }

    private static Part part(int dx, String spec, int look) {
        return new Part(dx, spec, 0, false, null, new String[0], look);
    }

    private static Part fed(int dx, String spec, int look) {
        return new Part(dx, spec, 0, true, null, new String[0], look);
    }

    private static final List<Station> STATIONS = List.of(
            new Station("storage", List.of(
                    fed(0, "cyclic:battery", 100),
                    fed(2, "enderio:basic_capacitor_bank", 100),
                    fed(4, "immersiveengineering:capacitor_lv", 100),
                    fed(6, "integrateddynamics:energy_battery", 100),
                    fed(8, "mekanism:basic_energy_cube", 100),
                    fed(10, "powah:energy_cell_basic", 100))),
            new Station("powah", List.of(
                    new Part(0, "powah:furnator_basic", 0, false, null, new String[]{"minecraft:coal*16"}, 180))),
            new Station("ae2", List.of(
                    fed(0, "ae2:energy_acceptor", 0),
                    part(1, "ae2:controller", 160),
                    part(2, "ae2:energy_cell", 220),
                    new Part(3, "ae2:inscriber", 0, true, Direction.UP, new String[]{"ae2:silicon_press*1", "ae2:silicon*8"}, 220))),
            new Station("mekanism", List.of(
                    new Part(0, "mekanism:enrichment_chamber", 0, true, null, new String[]{"minecraft:iron_ore*16"}, 200),
                    fed(3, "mekanism:resistive_heater", 200))),
            new Station("create", List.of(
                    part(0, "create:creative_motor[facing=east]", 0),
                    part(1, "create:shaft[axis=x]", 0),
                    part(2, "create:shaft[axis=x]", 220),
                    new Part(3, "create:mechanical_press[facing=east]", 100, false, null, new String[0], 220),
                    new Part(4, "create:mechanical_press[facing=east]", 300, false, null, new String[0], 0))),
            new Station("furnace", List.of(
                    part(0, "minecraft:furnace{Items:[{Slot:0b,id:\"minecraft:iron_ore\",count:16},{Slot:1b,id:\"minecraft:coal\",count:8}]}", 220))),
            new Station("modern_industrialization", List.of(
                    new Part(0, "modern_industrialization:bronze_boiler", 0, false, null, new String[]{"minecraft:coal*16"}, 220))),
            new Station("pneumaticcraft", List.of(
                    new Part(0, "pneumaticcraft:air_compressor", 0, false, null, new String[]{"minecraft:coal*16"}, 600))),
            new Station("refinedstorage", List.of(
                    fed(0, "refinedstorage:controller", 180)))
    );

    private static final Vec3[] AIMS = {
            new Vec3(0.5, 0.5, 0.5),
            new Vec3(0.5, 0.95, 0.97),
            new Vec3(0.03, 0.5, 0.97),
            new Vec3(0.5, 0.05, 0.97),
            new Vec3(0.97, 0.95, 0.97)
    };

    private static final class Stock {
        private final BlockPos pos;
        private final Part part;
        private final List<String> items;
        private int tries;

        private Stock(BlockPos pos, Part part) {
            this.pos = pos;
            this.part = part;
            this.items = new ArrayList<>(List.of(part.items()));
        }
    }

    private final List<BlockPos> fedPos = new ArrayList<>();
    private final List<Stock> stock = new ArrayList<>();
    private final Map<String, Set<String>> seen = new LinkedHashMap<>();
    private final List<Part> looks = new ArrayList<>();
    private int idle;
    private int phase;
    private int wait;
    private int floor;
    private int station = -1;
    private int clock;
    private int look;
    private int lookClock;
    private int aim;
    private boolean finished;
    private final String recordDir = System.getenv("IDENTIFY_RECORD");
    private final Recorder recorder = recordDir == null ? null : new Recorder(System.getenv().getOrDefault("IDENTIFY_FFMPEG", "ffmpeg"));

    void tick(Minecraft mc) {
        if (finished) {
            return;
        }
        mc.getToasts().clear();
        if (phase == 0) {
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++idle > 40) {
                phase = 1;
                mc.options.pauseOnLostFocus = false;
                GLFW.glfwSetWindowSize(mc.getWindow().getWindow(), 1600, 900);
                String name = "identify-showcase-" + System.currentTimeMillis();
                LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(name, settings, new WorldOptions(0L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            }
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) {
            return;
        }
        mc.gui.getChat().clearMessages(false);
        if (phase == 1) {
            if (++wait < 100) {
                return;
            }
            floor = (int) Math.floor(mc.player.getY());
            command(server, "gamerule doDaylightCycle false");
            command(server, "gamerule doFireTick false");
            command(server, "gamerule randomTickSpeed 0");
            command(server, "time set noon");
            command(server, "gamerule sendCommandFeedback false");
            command(server, "gamerule logAdminCommands false");
            command(server, "kill @e[type=!player]");
            command(server, "gamerule doMobSpawning false");
            command(server, "clear @p");
            IdentifyClient.config().setShowBestTool(false);
            phase = 2;
            begin(mc, server);
            return;
        }
        feed(server);
        Station current = STATIONS.get(station);
        for (Part part : current.parts()) {
            if (part.placeAt() == clock) {
                place(server, part);
            }
        }
        if (look >= looks.size()) {
            if (station + 1 >= STATIONS.size()) {
                report();
                mc.stop();
                return;
            }
            begin(mc, server);
            return;
        }
        Part target = looks.get(look);
        BlockPos pos = pos(target);
        if (lookClock == 0) {
            command(server, "kill @e[type=item]");
            command(server, "tp @p " + (pos.getX() + 0.5) + " " + floor + " " + (pos.getZ() + 2.8));
            aim = 0;
        }
        if (lookClock > 5 && !(mc.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(pos))) {
            aim = (aim + 1) % AIMS.length;
        }
        mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atLowerCornerOf(pos).add(AIMS[aim]));
        if (lookClock > 10 && lookClock % 20 == 0) {
            String text;
            try {
                text = text(BlockDetails.of(mc.level, pos, mc.level.getBlockState(pos), false));
            } catch (Throwable e) {
                text = "EXCEPTION " + e;
            }
            String key = current.name() + " " + id(target.spec());
            seen.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(text);
            System.out.println("[Identify showcase] " + key + " t=" + lookClock / 20 + "s -> " + text);
        }
        if (lookClock == target.look() - 10) {
            String shot = "mod-" + current.name() + "-" + id(target.spec()).replaceAll(".*:", "");
            Screenshot.grab(mc.gameDirectory, "identify-" + shot + ".png", mc.getMainRenderTarget(), message -> {
            });
            System.out.println("[Identify showcase] shot " + shot);
        }
        if (recorder != null && lookClock == target.look() - 70) {
            recorder.start(Path.of(recordDir, current.name() + "-" + id(target.spec()).replaceAll(".*:", "") + ".mp4"));
        }
        if (recorder != null && lookClock == target.look() - 4) {
            recorder.stop();
        }
        clock++;
        if (++lookClock >= target.look()) {
            look++;
            lookClock = 0;
        }
    }

    private void begin(Minecraft mc, IntegratedServer server) {
        station++;
        clock = 0;
        look = 0;
        lookClock = 0;
        looks.clear();
        server.execute(stock::clear);
        Station current = STATIONS.get(station);
        System.out.println("[Identify showcase] station " + current.name());
        for (Part part : current.parts()) {
            if (!known(part.spec())) {
                System.out.println("[Identify showcase] skip " + part.spec());
                continue;
            }
            if (part.placeAt() == 0) {
                place(server, part);
            }
            if (part.look() > 0) {
                looks.add(part);
            }
        }
        clock = 1;
    }

    private BlockPos pos(Part part) {
        return new BlockPos(station * 14 + part.dx(), floor, 0);
    }

    private void place(IntegratedServer server, Part part) {
        if (!known(part.spec())) {
            return;
        }
        BlockPos pos = pos(part);
        command(server, "setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " " + part.spec());
        if (part.feed()) {
            fedPos.add(pos);
        }
        if (part.items().length > 0) {
            server.execute(() -> stock.add(new Stock(pos, part)));
        }
    }

    private static boolean insert(ServerLevel level, Stock entry) {
        entry.tries++;
        for (int i = 0; i < entry.items.size(); i++) {
            String item = entry.items.get(i);
            String[] spec = item.split("\\*");
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec[0])), Integer.parseInt(spec[1]));
            List<Direction> sides = new ArrayList<>();
            if (item.equals(entry.part.items()[0]) && entry.part.side() != null) {
                sides.add(entry.part.side());
            }
            sides.add(null);
            sides.add(Direction.NORTH);
            for (Direction side : Direction.values()) {
                if (!sides.contains(side)) {
                    sides.add(side);
                }
            }
            for (Direction side : sides) {
                IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, entry.pos, side);
                if (handler == null) {
                    continue;
                }
                for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
                    stack = handler.insertItem(slot, stack, false);
                }
                if (stack.isEmpty()) {
                    break;
                }
            }
            if (stack.isEmpty()) {
                System.out.println("[Identify showcase] insert " + item + " into " + id(entry.part.spec()) + " ok");
                entry.items.remove(i--);
            } else {
                if (entry.tries == 1) {
                    System.out.println("[Identify showcase] insert " + item + " into " + id(entry.part.spec()) + " left " + stack.getCount() + ", retrying");
                }
                entry.items.set(i, spec[0] + "*" + stack.getCount());
            }
        }
        return entry.items.isEmpty();
    }

    private void feed(IntegratedServer server) {
        List<BlockPos> targets = List.copyOf(fedPos);
        boolean restock = clock % 20 == 1;
        server.execute(() -> {
            ServerLevel level = server.overworld();
            if (restock) {
                stock.removeIf(entry -> insert(level, entry));
            }
            for (BlockPos pos : targets) {
                boolean found = false;
                for (int d = -1; d < 6; d++) {
                    IEnergyStorage energy = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, d < 0 ? null : Direction.values()[d]);
                    if (energy == null) {
                        continue;
                    }
                    int amount = Math.max(4000, energy.getMaxEnergyStored() / 600);
                    found = true;
                    if (energy.receiveEnergy(amount, true) > 0) {
                        energy.receiveEnergy(amount, false);
                        break;
                    }
                }
                if (restock && clock < 60) {
                    System.out.println("[Identify showcase] feed " + pos + " cap=" + found + " be=" + level.getBlockEntity(pos));
                }
            }
        });
    }

    private void report() {
        finished = true;
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : seen.entrySet()) {
            boolean ok = entry.getValue().size() > 1;
            System.out.println("[Identify showcase] " + (ok ? "ok   " : "FAIL ") + entry.getKey() + " distinct=" + entry.getValue().size());
            if (!ok) {
                failures.add(entry.getKey());
            }
        }
        System.out.println("[Identify showcase] failures=" + failures + " RESULT=" + (failures.isEmpty() ? "PASS" : "FAIL"));
    }

    private static String id(String spec) {
        return spec.replaceAll("[\\[{].*", "");
    }

    private static boolean known(String spec) {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id(spec)));
    }

    private static void command(IntegratedServer server, String command) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }

    private static String text(List<Component> lines) {
        StringBuilder out = new StringBuilder();
        for (Component line : lines) {
            if (out.length() > 0) {
                out.append(" | ");
            }
            out.append(line.getString());
        }
        return out.toString();
    }
}
