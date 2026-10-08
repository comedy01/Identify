package dev.identify.selftest;

import com.mojang.blaze3d.platform.InputConstants;
import dev.identify.client.IdentifyClient;
import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.config.IdentifyConfig;
import dev.identify.look.ItemCompare;
import dev.identify.look.LookResolver;
import dev.identify.look.LookTarget;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class IdentifySelfTest {
    private static final String WORLD = "identify-selftest";
    private static final int TIMEOUT = 600;

    private record Step(int delay, BooleanSupplier ready, Runnable action) {
    }

    private final Path configDir;
    private final Supplier<Screen> settingsScreen;
    private final List<Step> steps = new ArrayList<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int titleWait;
    private int floor;

    public IdentifySelfTest(Path configDir, Supplier<Screen> settingsScreen) {
        this.configDir = configDir;
        this.settingsScreen = settingsScreen;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (finished) {
            return;
        }
        if (!started) {
            if (++titleWait % 200 == 0) {
                log("waiting on screen " + (mc.screen == null ? "none" : mc.screen.getClass().getName())
                        + ", overlay " + (mc.getOverlay() == null ? "none" : mc.getOverlay().getClass().getName()));
            }
            if (mc.screen instanceof TitleScreen && mc.getOverlay() == null && ++idle > 40) {
                started = true;
                mc.options.pauseOnLostFocus = false;
                plan(mc);
                delay = steps.get(0).delay();
                log("creating world");
                deleteWorld(mc);
                Worlds.createFlat(mc, WORLD);
            }
            return;
        }
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready().getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action().run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay();
        }
    }

    private void plan(Minecraft mc) {
        steps.add(new Step(0, () -> mc.level != null && mc.player != null && mc.screen == null
                && mc.getSingleplayerServer() != null, () -> {
            floor = (int) Math.floor(mc.player.getY());
            log("world loaded, floor " + floor);
        }));
        then(40, () -> {
            IdentifyClient.config().resetToDefaults();
            check(Files.exists(configDir.resolve(IdentifyConfig.FILE_NAME)), "config file missing");
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 0");
            run(mc, "time set noon");
            run(mc, "setblock 0 " + (floor + 1) + " 3 beehive[honey_level=3]");
        });
        then(20, () -> {
            LookTarget hive = look(mc);
            expect(hive, "Beehive", "Honey: 3/5", "Best tool: Axe");
            screenshot(mc, "identify-beehive");
            run(mc, "setblock 0 " + (floor + 1) + " 3 air");
            run(mc, "setblock 0 " + floor + " 3 dirt");
            run(mc, "setblock 0 " + (floor + 1) + " 3 sweet_berry_bush[age=2]");
        });
        then(20, () -> {
            expect(look(mc), "Sweet Berry Bush", "Growth: 2/3 (67%)");
            run(mc, "setblock 0 " + (floor + 1) + " 3 air");
            run(mc, "fill -1 " + floor + " 2 1 " + floor + " 4 iron_block");
            run(mc, "setblock 0 " + (floor + 1) + " 3 beacon{Primary:1,Secondary:10}");
        });
        then(200, () -> {
            expect(look(mc), "Beacon", "Tier 1", "Range: 20 blocks", "Effects: Speed, Regeneration");
            screenshot(mc, "identify-beacon");
            changeBeaconSilently(mc, new BlockPos(0, floor + 1, 3));
        });
        then(40, () -> {
            expect(look(mc), "Beacon", "Effects: Haste, Regeneration");
            run(mc, "setblock 0 " + (floor + 1) + " 3 air");
            run(mc, "fill -1 " + floor + " 2 1 " + floor + " 4 air");
            run(mc, "summon villager 0.5 " + floor + " 3.5 {NoAI:1b,Rotation:[180f,0f],"
                    + "VillagerData:{profession:\"minecraft:farmer\",level:2,type:\"minecraft:plains\"}}");
        });
        then(40, () -> {
            expect(look(mc), "Farmer", "Health: 20 / 20", "Farmer · Apprentice");
            screenshot(mc, "identify-villager");
            check(LookResolver.modLabel(look(mc)).equals("Minecraft"), "wrong mod label: " + LookResolver.modLabel(look(mc)));
            run(mc, "effect give @e[type=villager] minecraft:invisibility 100 0 true");
        });
        then(20, () -> {
            LookTarget hidden = look(mc);
            check(hidden == null || !hidden.name().getString().equals("Farmer"), "invisible villager was identified");
            run(mc, "kill @e[type=villager]");
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 20");
            run(mc, "summon horse 0.5 " + floor + " 3.5 {NoAI:1b}");
        });
        then(40, () -> {
            expect(look(mc), "Horse", "Speed: ", "Jump: ");
            run(mc, "kill @e[type=horse]");
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 0");
            run(mc, "item replace entity @p armor.chest with iron_chestplate");
        });
        then(10, () -> {
            checkTooltips(mc);
            ItemStack worn = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            check(worn.getItem() == Items.IRON_CHESTPLATE, "chestplate not equipped: " + worn);
            Component diff = ItemCompare.difference(new ItemStack(Items.DIAMOND_CHESTPLATE), worn, EquipmentSlot.CHEST);
            log("compare diamond vs iron: " + (diff == null ? null : diff.getString()));
            check(diff != null && diff.getString().contains("+2 Armor") && diff.getString().contains("+2 Toughness"),
                    "wrong comparison: " + (diff == null ? null : diff.getString()));
            Component downgrade = ItemCompare.difference(new ItemStack(Items.IRON_CHESTPLATE),
                    new ItemStack(Items.NETHERITE_CHESTPLATE), EquipmentSlot.CHEST);
            log("compare iron vs netherite: " + (downgrade == null ? null : downgrade.getString()));
            check(downgrade != null && downgrade.getString().contains("-3 Toughness"),
                    "toughness loss hidden: " + (downgrade == null ? null : downgrade.getString()));
            mc.setScreen(settingsScreen.get());
        });
        then(10, () -> {
            check(mc.screen instanceof IdentifySettingsScreen, "settings screen not open: " + mc.screen);
            screenshot(mc, "identify-settings");
            button(mc.screen, "Reset to Defaults").onClick(0, 0);
        });
        then(10, () -> {
            check(mc.screen instanceof IdentifySettingsScreen, "settings screen gone after reset: " + mc.screen);
            button(mc.screen, "Blocks: ON").onClick(0, 0);
            check(!IdentifyClient.config().showBlocks(), "Blocks toggle did nothing after reset");
            button(mc.screen, "Blocks: OFF").onClick(0, 0);
            check(IdentifyClient.config().showBlocks(), "Blocks toggle did not turn back on");
            mc.setScreen(null);
            run(mc, "setblock 0 " + (floor + 1) + " 3 beehive[honey_level=5]");
            run(mc, "bossbar add identify:test \"Identify Test\"");
            run(mc, "bossbar set identify:test players @a");
        });
        then(20, () -> {
            expect(look(mc), "Beehive", "Honey ready to harvest");
            screenshot(mc, "identify-beehive-ready");
            pressKey(mc, "key.identify.blocks", 74);
        });
        then(5, () -> {
            check(!IdentifyClient.config().showBlocks(), "block key did not hide block info");
            check(look(mc) == null, "beehive still identified with block info hidden");
            check(IdentifyClient.config().showEntities(), "block key changed entity info");
            pressKey(mc, "key.identify.blocks", 74);
        });
        then(5, () -> {
            check(IdentifyClient.config().showBlocks(), "block key did not show block info again");
            expect(look(mc), "Beehive");
            pressKey(mc, "key.identify.entities", 75);
        });
        then(5, () -> {
            check(!IdentifyClient.config().showEntities(), "entity key did not hide entity info");
            check(IdentifyClient.config().showBlocks(), "entity key changed block info");
            pressKey(mc, "key.identify.toggle", 302);
        });
        then(5, () -> {
            check(!IdentifyClient.config().showBlocks() && !IdentifyClient.config().showEntities(),
                    "panel key did not hide the panel");
            pressKey(mc, "key.identify.toggle", 302);
        });
        then(5, () -> {
            check(IdentifyClient.config().showBlocks() && IdentifyClient.config().showEntities(),
                    "panel key did not show the panel again");
            expect(look(mc), "Beehive");
        });
    }

    private static void changeBeaconSilently(Minecraft mc, BlockPos pos) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            BlockEntity beacon = server.overworld().getBlockEntity(pos);
            for (Field field : beacon.getClass().getDeclaredFields()) {
                if (field.getType() == MobEffect.class && !Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        field.set(beacon, MobEffects.DIG_SPEED);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                    return;
                }
            }
        });
    }

    private void then(int ticks, Runnable action) {
        steps.add(new Step(ticks, () -> true, action));
    }

    private static LookTarget look(Minecraft mc) {
        return LookResolver.resolve(mc, 1.0F, IdentifyClient.config());
    }

    private static void expect(LookTarget target, String name, String... details) {
        check(target != null, "nothing identified, expected " + name);
        StringBuilder shown = new StringBuilder();
        for (Component line : target.details()) {
            shown.append(line.getString()).append(" | ");
        }
        log(target.name().getString() + " -> " + shown + " [" + LookResolver.modLabel(target) + "]");
        check(target.name().getString().equals(name), "expected " + name + ", got " + target.name().getString());
        for (String detail : details) {
            check(shown.toString().contains(detail), name + " is missing \"" + detail + "\"");
        }
    }

    private static void checkTooltips(Minecraft mc) {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.setDamageValue(500);
        String tool = text(pickaxe.getTooltipLines(mc.player, normalFlag()));
        String food = text(new ItemStack(Items.BREAD).getTooltipLines(mc.player, normalFlag()));
        String coal = text(new ItemStack(Items.COAL).getTooltipLines(mc.player, normalFlag()));
        log("tooltips: " + tool + " / " + food + " / " + coal);
        check(tool.contains("Durability: 1061/1561 (68%)"), "durability line missing");
        check(food.contains("Food: 5 hunger, 6.0 saturation"), "food line missing");
        check(coal.contains("Fuel: smelts 8 items"), "fuel line missing");
    }

    private static TooltipFlag normalFlag() {
        try {
            for (Field field : TooltipFlag.class.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && TooltipFlag.class.isAssignableFrom(field.getType())) {
                    TooltipFlag flag = (TooltipFlag) field.get(null);
                    if (!flag.isAdvanced()) {
                        return flag;
                    }
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        for (Class<?> nested : TooltipFlag.class.getDeclaredClasses()) {
            if (nested.isEnum() && TooltipFlag.class.isAssignableFrom(nested)) {
                for (Object constant : nested.getEnumConstants()) {
                    if (!((TooltipFlag) constant).isAdvanced()) {
                        return (TooltipFlag) constant;
                    }
                }
            }
        }
        throw new IllegalStateException("no normal tooltip flag");
    }

    private static AbstractWidget button(Screen screen, String label) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getString().equals(label)) {
                return widget;
            }
        }
        throw new AssertionError("no button \"" + label + "\"");
    }

    private static void pressKey(Minecraft mc, String name, int code) {
        for (KeyMapping key : mc.options.keyMappings) {
            if (key.getName().equals(name)) {
                InputConstants.Key bound = InputConstants.Type.KEYSYM.getOrCreate(code);
                key.setKey(bound);
                KeyMapping.resetMapping();
                KeyMapping.click(bound);
                log("pressed " + name);
                return;
            }
        }
        throw new AssertionError("key not registered: " + name);
    }

    private static String text(List<Component> lines) {
        StringBuilder out = new StringBuilder();
        for (Component line : lines) {
            out.append(line.getString()).append(" | ");
        }
        return out.toString();
    }

    private static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> Worlds.runCommand(server, command));
    }

    private static void screenshot(Minecraft mc, String name) {
        Worlds.screenshot(mc, name);
    }

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private void finish(Minecraft mc, Throwable failure) {
        finished = true;
        if (failure == null) {
            log("RESULT=PASS");
        } else {
            log("RESULT=FAIL " + failure);
            failure.printStackTrace();
        }
        mc.stop();
    }

    private static void deleteWorld(Minecraft mc) {
        Path dir = mc.gameDirectory.toPath().resolve("saves").resolve(WORLD);
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void log(String message) {
        System.out.println("[Identify selftest] " + message);
    }
}
