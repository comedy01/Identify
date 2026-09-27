package dev.identify.selftest;

import dev.identify.client.IdentifyClient;
import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.config.IdentifyConfig;
import dev.identify.look.ItemCompare;
import dev.identify.look.LookResolver;
import dev.identify.look.LookTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

import java.io.IOException;
import java.io.UncheckedIOException;
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
                && mc.getSingleplayerServer() != null, () -> log("world loaded")));
        then(40, () -> {
            IdentifyClient.config().resetToDefaults();
            check(Files.exists(configDir.resolve(IdentifyConfig.FILE_NAME)), "config file missing");
            run(mc, "tp @p 0.5 -60 0.5 0 0");
            run(mc, "time set noon");
            run(mc, "setblock 0 -59 3 beehive[honey_level=3]");
        });
        then(20, () -> {
            LookTarget hive = look(mc);
            expect(hive, "Beehive", "Honey: 3/5", "Best tool: Axe");
            screenshot(mc, "identify-beehive");
            run(mc, "setblock 0 -59 3 air");
            run(mc, "setblock 0 -60 3 dirt");
            run(mc, "setblock 0 -59 3 sweet_berry_bush[age=2]");
        });
        then(20, () -> {
            expect(look(mc), "Sweet Berry Bush", "Growth: 2/3 (67%)");
            run(mc, "setblock 0 -59 3 air");
            run(mc, "fill -1 -60 2 1 -60 4 iron_block");
            run(mc, "setblock 0 -59 3 beacon{Primary:1,Secondary:10}");
        });
        then(200, () -> {
            expect(look(mc), "Beacon", "Tier 1", "Range: 20 blocks", "Effects: Speed, Regeneration");
            screenshot(mc, "identify-beacon");
            run(mc, "setblock 0 -59 3 air");
            run(mc, "fill -1 -60 2 1 -60 4 air");
            run(mc, "summon villager 0.5 -60 3.5 {NoAI:1b,Rotation:[180f,0f],"
                    + "VillagerData:{profession:\"minecraft:farmer\",level:2,type:\"minecraft:plains\"}}");
        });
        then(20, () -> {
            expect(look(mc), "Farmer", "Health: 20 / 20", "Farmer · Apprentice");
            screenshot(mc, "identify-villager");
            run(mc, "kill @e[type=villager]");
            run(mc, "tp @p 0.5 -60 0.5 0 20");
            run(mc, "summon horse 0.5 -60 3.5 {NoAI:1b}");
        });
        then(20, () -> {
            expect(look(mc), "Horse", "Speed: ", "Jump: ");
            run(mc, "kill @e[type=horse]");
            run(mc, "tp @p 0.5 -60 0.5 0 0");
            run(mc, "item replace entity @p armor.chest with iron_chestplate");
        });
        then(10, () -> {
            checkTooltips(mc);
            ItemStack worn = mc.player.getItemBySlot(EquipmentSlot.CHEST);
            check(worn.is(Items.IRON_CHESTPLATE), "chestplate not equipped: " + worn);
            Component diff = ItemCompare.difference(new ItemStack(Items.DIAMOND_CHESTPLATE), worn, EquipmentSlot.CHEST);
            log("compare diamond vs iron: " + (diff == null ? null : diff.getString()));
            check(diff != null && diff.getString().contains("+2 Armor") && diff.getString().contains("+2 Toughness"),
                    "wrong comparison: " + (diff == null ? null : diff.getString()));
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
            run(mc, "setblock 0 -59 3 beehive[honey_level=5]");
        });
        then(20, () -> {
            expect(look(mc), "Beehive", "Honey ready to harvest");
            screenshot(mc, "identify-beehive-ready");
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
            try {
                return (TooltipFlag) TooltipFlag.class.getField("NORMAL").get(null);
            } catch (NoSuchFieldException e) {
                return (TooltipFlag) Class.forName(TooltipFlag.class.getName() + "$Default").getField("NORMAL").get(null);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static AbstractWidget button(Screen screen, String label) {
        for (GuiEventListener child : screen.children()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getString().equals(label)) {
                return widget;
            }
        }
        throw new AssertionError("no button \"" + label + "\"");
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
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), message -> {
        });
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
