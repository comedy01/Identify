package dev.identify.nftest;

import dev.identify.look.BlockDetails;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

import java.util.ArrayList;
import java.util.List;

final class WorldCheck {
    private static final String[][] CASES = {
            {"minecraft:wheat[age=3]", "Growth: 3/7"},
            {"minecraft:wheat[age=7]", "Fully grown"},
            {"minecraft:redstone_wire[power=12]", "Power:"},
            {"minecraft:composter[level=4]", "Compost: 4/"},
            {"minecraft:composter[level=8]", "Compost ready"},
            {"minecraft:beehive[honey_level=2]", "Honey: 2/5"},
            {"minecraft:beehive[honey_level=5]", "Honey ready"},
            {"minecraft:water_cauldron[level=2]", "Fill: 2/3"},
            {"minecraft:repeater[delay=3]", "Delay: 3/4"},
            {"minecraft:comparator[mode=subtract]", "Mode: Subtract"},
            {"minecraft:note_block[note=5]", "Note:"},
            {"minecraft:respawn_anchor[charges=3]", "Charges: 3/4"},
            {"minecraft:stone", "Best tool: Pickaxe"},
            {"minecraft:diamond_ore", "Iron or better"},
            {"minecraft:oak_log", "Best tool: Axe"},
            {"minecraft:spawner{SpawnData:{entity:{id:\"minecraft:zombie\"}}}", "Spawns: Zombie"},
            {"minecraft:furnace{Items:[{Slot:0b,id:\"minecraft:iron_ore\",count:5},{Slot:1b,id:\"minecraft:coal\",count:3}]}", "Cooking:|Burning"},
            {"minecraft:brewing_stand{Fuel:10b,Items:[{Slot:0b,id:\"minecraft:potion\",count:1,components:{\"minecraft:potion_contents\":{potion:\"minecraft:water\"}}},{Slot:3b,id:\"minecraft:nether_wart\",count:1}]}", "Bottles: 1/3"},
            {"minecraft:campfire{Items:[{Slot:0b,id:\"minecraft:beef\",count:1}],CookingTimes:[I;0,0,0,0],CookingTotalTimes:[I;600,0,0,0]}", "Cooking: 1/4"},
            {"minecraft:jukebox[has_record=true]{RecordItem:{id:\"minecraft:music_disc_cat\",count:1}}", "Playing|disc"},
            {"minecraft:lectern[has_book=true]{Book:{id:\"minecraft:writable_book\",count:1},Page:0}", "Page:|Holds a book"},
            {"mekanism:basic_energy_cube", "Energy:"},
            {"mekanism:creative_energy_cube", "Energy:"},
            {"mekanism:basic_fluid_tank", "Fluid:"},
            {"mekanism:basic_chemical_tank", "Chemical:"},
            {"create:fluid_tank", "Fluid:"},
            {"ae2:inscriber", "Energy:|Progress"},
            {"powah:energy_cell_basic", "Energy:"},
            {"cyclic:battery", "Energy:"},
            {"enderio:fluid_tank", "Fluid:"},
            {"occultism:storage_controller", "Item Types"},
            {"refinedstorage:creative_controller", "Network"},
            {"pneumaticcraft:air_compressor", "Pressure:"},
            {"modern_industrialization:bronze_boiler", "Temperature:"},
            {"ars_nouveau:source_jar", "Source:"}
    };
    private static final int BATCH = 64;

    private final List<String> failures = new ArrayList<>();
    private final List<Block> sweep = new ArrayList<>();
    private int idle;
    private int wait;
    private int phase;
    private int batch;
    private int floor;
    private int passed;
    private int skipped;
    private int swept;
    private int withLines;
    private int errors;
    private long slowest;
    private String slowestId = "";
    private boolean finished;

    void tick(Minecraft mc) {
        if (finished) {
            return;
        }
        if (phase == 0) {
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++idle > 40) {
                phase = 1;
                mc.options.pauseOnLostFocus = false;
                LevelSettings settings = new LevelSettings("identify-worldcheck", GameType.CREATIVE, false, Difficulty.PEACEFUL,
                        true, new GameRules(), WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel("identify-worldcheck", settings, new WorldOptions(0L, false, false),
                        registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                                .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), mc.screen);
            }
            return;
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) {
            return;
        }
        wait++;
        if (phase == 1) {
            if (wait < 100) {
                return;
            }
            floor = (int) Math.floor(mc.player.getY());
            command(server, "gamerule doDaylightCycle false");
            command(server, "gamerule doFireTick false");
            command(server, "gamerule randomTickSpeed 0");
            command(server, "tp @p 0.5 " + floor + " 0.5");
            for (int i = 0; i < CASES.length; i++) {
                if (known(CASES[i][0])) {
                    command(server, "setblock " + (i * 2 - CASES.length) + " " + floor + " 6 " + CASES[i][0]);
                }
            }
            next(2);
        } else if (phase == 2) {
            if (wait < 20) {
                return;
            }
            int index = (wait - 20) / 6;
            int step = (wait - 20) % 6;
            if (index < CASES.length) {
                if (step == 0 || step == 5) {
                    check(mc, index, step == 5);
                }
            } else {
                for (Block block : BuiltInRegistries.BLOCK) {
                    if (block != Blocks.AIR && !excluded(BuiltInRegistries.BLOCK.getKey(block).toString())) {
                        sweep.add(block);
                    }
                }
                place(server, true);
                next(3);
            }
        } else if (phase == 3) {
            if (wait == 10) {
                query(mc, false);
            } else if (wait == 20) {
                query(mc, true);
                place(server, false);
                batch++;
                if (batch * BATCH >= sweep.size()) {
                    report(mc);
                    return;
                }
                place(server, true);
                wait = 0;
            }
        }
    }

    private void next(int to) {
        phase = to;
        wait = 0;
    }

    private static String id(String spec) {
        return spec.replaceAll("[\\[{].*", "");
    }

    private static boolean known(String spec) {
        return BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse(id(spec)));
    }

    private void check(Minecraft mc, int i, boolean record) {
        if (!known(CASES[i][0])) {
            if (record) {
                skipped++;
            }
            return;
        }
        BlockPos pos = new BlockPos(i * 2 - CASES.length, floor, 6);
        String text;
        try {
            text = text(BlockDetails.of(mc.level, pos, mc.level.getBlockState(pos), true));
        } catch (Throwable e) {
            text = "EXCEPTION " + e;
        }
        if (!record) {
            return;
        }
        boolean ok = false;
        for (String want : CASES[i][1].split("\\|")) {
            ok |= text.contains(want);
        }
        System.out.println("[Identify worldcheck] " + (ok ? "ok   " : "FAIL ") + CASES[i][0].replaceAll("\\{.*", "{..}") + " -> " + text);
        if (ok) {
            passed++;
        } else {
            failures.add(id(CASES[i][0]));
        }
    }

    private static boolean excluded(String id) {
        if (id.contains("debug") || id.contains("creative")) {
            return true;
        }
        return id.startsWith("immersiveengineering:") && id.matches(".*(connector|transformer|breaker_switch|redstone_breaker|relay).*");
    }

    private static BlockPos sweepPos(int index, int floor) {
        int local = index % BATCH;
        return new BlockPos(40 + (local % 16) * 2, floor, (local / 16) * 2);
    }

    private void place(IntegratedServer server, boolean fill) {
        int from = batch * BATCH;
        int to = Math.min(sweep.size(), from + BATCH);
        List<Block> blocks = new ArrayList<>(sweep.subList(from, to));
        int y = floor;
        if (fill) {
            System.out.println("[Identify worldcheck] batch " + batch + " " + BuiltInRegistries.BLOCK.getKey(blocks.get(0)) + " .. " + BuiltInRegistries.BLOCK.getKey(blocks.get(blocks.size() - 1))
                    + " heap=" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576 + "M");
        }
        server.execute(() -> {
            ServerLevel level = server.overworld();
            for (int i = 0; i < blocks.size(); i++) {
                try {
                    level.setBlock(sweepPos(from + i, y), fill ? blocks.get(i).defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
                } catch (Throwable ignored) {
                }
            }
        });
    }

    private void query(Minecraft mc, boolean record) {
        int from = batch * BATCH;
        int to = Math.min(sweep.size(), from + BATCH);
        for (int i = from; i < to; i++) {
            BlockPos pos = sweepPos(i, floor);
            BlockState state = mc.level.getBlockState(pos);
            String id = BuiltInRegistries.BLOCK.getKey(sweep.get(i)).toString();
            long start = System.nanoTime();
            try {
                String text = text(BlockDetails.of(mc.level, pos, state, true));
                if (record) {
                    swept++;
                    if (!text.isEmpty()) {
                        withLines++;
                    }
                    if (!id.startsWith("minecraft:") && !text.isEmpty() && !text.startsWith("Best tool:")) {
                        System.out.println("[Identify worldcheck] mod " + id + " -> " + text);
                    }
                }
            } catch (Throwable e) {
                if (record) {
                    errors++;
                    failures.add(id + " " + e);
                    System.out.println("[Identify worldcheck] EXCEPTION " + id + " " + e);
                }
            }
            long took = System.nanoTime() - start;
            if (record && took > slowest) {
                slowest = took;
                slowestId = id;
            }
        }
    }

    private void report(Minecraft mc) {
        finished = true;
        System.out.println("[Identify worldcheck] cases passed=" + passed + " skipped=" + skipped
                + " swept=" + swept + " withLines=" + withLines + " errors=" + errors
                + " slowest=" + slowest / 1000 + "us (" + slowestId + ") failures=" + failures
                + " RESULT=" + (failures.isEmpty() ? "PASS" : "FAIL"));
        mc.stop();
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
