package dev.identify.selftest;

import dev.identify.client.IdentifyClient;
import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.config.IdentifyConfig;
import dev.identify.look.ItemCompare;
import dev.identify.look.LookResolver;
import dev.identify.look.LookTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

@Mod(modid = "identify_selftest", name = "Identify self test", version = "1.0.0", clientSideOnly = true,
        dependencies = "required-after:identify")
public final class IdentifySelfTest {
    private static final String WORLD = "identify-selftest";
    private static final int TIMEOUT = 600;

    private static final class Step {
        final int delay;
        final BooleanSupplier ready;
        final Runnable action;

        Step(int delay, BooleanSupplier ready, Runnable action) {
            this.delay = delay;
            this.ready = ready;
            this.action = action;
        }
    }

    private final List<Step> steps = new ArrayList<>();
    private boolean started;
    private boolean finished;
    private int idle;
    private int index;
    private int delay;
    private int waited;
    private int floor;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tick();
        }
    }

    private void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            return;
        }
        if (!started) {
            if (mc.currentScreen instanceof GuiMainMenu && ++idle > 40) {
                started = true;
                mc.gameSettings.pauseOnLostFocus = false;
                plan(mc);
                delay = steps.get(0).delay;
                log("creating world");
                deleteWorld(new File(new File(mc.gameDir, "saves"), WORLD));
                WorldSettings settings = new WorldSettings(0L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.enableCommands();
                mc.launchIntegratedServer(WORLD, WORLD, settings);
            }
            return;
        }
        if (delay > 0) {
            delay--;
            return;
        }
        Step step = steps.get(index);
        if (!step.ready.getAsBoolean()) {
            if (++waited > TIMEOUT) {
                finish(mc, new AssertionError("timed out at step " + index));
            }
            return;
        }
        waited = 0;
        index++;
        try {
            step.action.run();
        } catch (Throwable e) {
            finish(mc, e);
            return;
        }
        if (index >= steps.size()) {
            finish(mc, null);
        } else {
            delay = steps.get(index).delay;
        }
    }

    private void plan(Minecraft mc) {
        steps.add(new Step(0, () -> mc.world != null && mc.player != null && mc.currentScreen == null
                && mc.getIntegratedServer() != null, () -> {
            floor = (int) Math.floor(mc.player.posY);
            log("world loaded, floor " + floor);
        }));
        then(40, () -> {
            IdentifyClient.config().resetToDefaults();
            check(new File(Loader.instance().getConfigDir(), IdentifyConfig.FILE_NAME).exists(),
                    "config file missing");
            run(mc, "difficulty peaceful");
            run(mc, "gamerule doMobSpawning false");
            run(mc, "kill @e[type=!player]");
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 0");
            run(mc, "time set 6000");
            run(mc, "setblock 0 " + floor + " 3 farmland");
            run(mc, "setblock 0 " + (floor + 1) + " 3 wheat 5");
        });
        then(20, () -> {
            expect(look(mc), "Crops", "Growth: 5/7 (71%)");
            screenshot(mc, "identify-crops");
            run(mc, "setblock 0 " + (floor + 1) + " 3 air");
            run(mc, "setblock 0 " + floor + " 3 air");
            run(mc, "setblock 0 " + (floor + 1) + " 3 diamond_ore");
        });
        then(20, () -> {
            expect(look(mc), "Diamond Ore", "Best tool: Pickaxe (Iron or better)");
            run(mc, "setblock 0 " + (floor + 1) + " 3 mob_spawner");
        });
        then(20, () -> {
            expect(look(mc), "Monster Spawner", "Spawns: Pig");
            run(mc, "setblock 0 " + (floor + 1) + " 3 air");
            run(mc, "fill -1 " + floor + " 2 1 " + floor + " 4 iron_block");
            run(mc, "setblock 0 " + (floor + 1) + " 3 beacon 0 replace {Primary:1,Secondary:10}");
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
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 20");
            run(mc, "summon cow 0.5 " + floor + " 3.5 {NoAI:1b,Age:-6000}");
        });
        then(40, () -> {
            expect(look(mc), "Cow", "Health: 10 / 10", "Baby, grows up in ");
            run(mc, "kill @e[type=cow]");
            run(mc, "summon horse 0.5 " + floor + " 3.5 {NoAI:1b,Tame:1b}");
            run(mc, "effect @e[type=horse] speed 100 1");
        });
        then(20, () -> {
            expect(look(mc), "Horse", "Tamed", "Speed: ", "Jump: ", "Effects: Speed II");
            screenshot(mc, "identify-horse");
            check(LookResolver.modLabel(look(mc)).equals("Minecraft"), "wrong mod label: " + LookResolver.modLabel(look(mc)));
            run(mc, "effect @e[type=horse] invisibility 100 0 true");
        });
        then(20, () -> {
            LookTarget hidden = look(mc);
            check(hidden == null || !plain(hidden.name()).equals("Horse"), "invisible horse was identified");
            run(mc, "kill @e[type=horse]");
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 0");
            run(mc, "replaceitem entity @p slot.armor.chest iron_chestplate");
        });
        then(10, () -> {
            checkTooltips(mc);
            ItemStack worn = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
            check(worn.getItem() == Items.IRON_CHESTPLATE, "chestplate not equipped: " + worn);
            String diff = plain(ItemCompare.difference(
                    new ItemStack(Items.DIAMOND_CHESTPLATE), worn, EntityEquipmentSlot.CHEST));
            log("compare diamond vs iron: " + diff);
            check(diff != null && diff.contains("+2 Armor") && diff.contains("+2 Toughness"),
                    "wrong comparison: " + diff);
            String downgrade = plain(ItemCompare.difference(
                    new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.DIAMOND_CHESTPLATE),
                    EntityEquipmentSlot.CHEST));
            log("compare iron vs diamond: " + downgrade);
            check(downgrade != null && downgrade.contains("-2 Toughness"), "toughness loss hidden: " + downgrade);
            mc.displayGuiScreen(FMLClientHandler.instance()
                    .getGuiFactoryFor(Loader.instance().getIndexedModList().get(IdentifyClient.MOD_ID))
                    .createConfigGui(null));
        });
        then(10, () -> {
            check(mc.currentScreen instanceof IdentifySettingsScreen, "settings screen not open: " + mc.currentScreen);
            screenshot(mc, "identify-settings");
            click(mc.currentScreen, "Reset to Defaults");
        });
        then(10, () -> {
            check(mc.currentScreen instanceof IdentifySettingsScreen,
                    "settings screen gone after reset: " + mc.currentScreen);
            click(mc.currentScreen, "Blocks: ON");
            check(!IdentifyClient.config().showBlocks(), "Blocks toggle did nothing after reset");
            click(mc.currentScreen, "Blocks: OFF");
            check(IdentifyClient.config().showBlocks(), "Blocks toggle did not turn back on");
            mc.displayGuiScreen(null);
            run(mc, "setblock 0 " + floor + " 3 redstone_block");
            run(mc, "setblock 0 " + floor + " 2 redstone_wire");
        });
        then(20, () -> {
            run(mc, "tp @p 0.5 " + floor + " 0.5 0 45");
        });
        then(20, () -> {
            expect(look(mc), "Redstone Dust", "Power: 15");
            screenshot(mc, "identify-redstone");
            pressKey(mc, "key.identify.blocks", 36);
        });
        then(5, () -> {
            check(!IdentifyClient.config().showBlocks(), "block key did not hide block info");
            check(look(mc) == null, "redstone still identified with block info hidden");
            check(IdentifyClient.config().showEntities(), "block key changed entity info");
            pressKey(mc, "key.identify.blocks", 36);
        });
        then(5, () -> {
            check(IdentifyClient.config().showBlocks(), "block key did not show block info again");
            expect(look(mc), "Redstone Dust");
            pressKey(mc, "key.identify.entities", 37);
        });
        then(5, () -> {
            check(!IdentifyClient.config().showEntities(), "entity key did not hide entity info");
            check(IdentifyClient.config().showBlocks(), "entity key changed block info");
            pressKey(mc, "key.identify.toggle", 100);
        });
        then(5, () -> {
            check(!IdentifyClient.config().showBlocks() && !IdentifyClient.config().showEntities(),
                    "panel key did not hide the panel");
            pressKey(mc, "key.identify.toggle", 100);
        });
        then(5, () -> {
            check(IdentifyClient.config().showBlocks() && IdentifyClient.config().showEntities(),
                    "panel key did not show the panel again");
            expect(look(mc), "Redstone Dust");
        });
    }

    private static void pressKey(Minecraft mc, String name, int code) {
        for (KeyBinding key : mc.gameSettings.keyBindings) {
            if (key.getKeyDescription().equals(name)) {
                key.setKeyCode(code);
                KeyBinding.resetKeyBindingArrayAndHash();
                KeyBinding.onTick(code);
                log("pressed " + name);
                return;
            }
        }
        throw new AssertionError("key not registered: " + name);
    }

    private static void changeBeaconSilently(Minecraft mc, BlockPos pos) {
        IntegratedServer server = mc.getIntegratedServer();
        server.addScheduledTask(() -> {
            TileEntity beacon = server.getWorld(0).getTileEntity(pos);
            for (Field field : beacon.getClass().getDeclaredFields()) {
                if (field.getType() == Potion.class && !Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        field.set(beacon, MobEffects.HASTE);
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
        for (String line : target.details()) {
            shown.append(plain(line)).append(" | ");
        }
        log(plain(target.name()) + " -> " + shown + " [" + LookResolver.modLabel(target) + "]");
        check(plain(target.name()).equals(name), "expected " + name + ", got " + plain(target.name()));
        for (String detail : details) {
            check(shown.toString().contains(detail), name + " is missing \"" + detail + "\"");
        }
    }

    private static void checkTooltips(Minecraft mc) {
        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.setItemDamage(500);
        String tool = text(pickaxe.getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL));
        String food = text(new ItemStack(Items.BREAD).getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL));
        String coal = text(new ItemStack(Items.COAL).getTooltip(mc.player, ITooltipFlag.TooltipFlags.NORMAL));
        log("tooltips: " + tool + " / " + food + " / " + coal);
        check(tool.contains("Durability: 1061/1561 (68%)"), "durability line missing");
        check(food.contains("Food: 5 hunger, 6.0 saturation"), "food line missing");
        check(coal.contains("Fuel: smelts 8 items"), "fuel line missing");
    }

    private static void click(GuiScreen screen, String label) {
        List<GuiButton> buttons = ReflectionHelper.getPrivateValue(GuiScreen.class, screen, "buttonList", "field_146292_n");
        for (GuiButton button : buttons) {
            if (button.displayString.equals(label)) {
                Method mouseClicked = ReflectionHelper.findMethod(
                        GuiScreen.class, "mouseClicked", "func_73864_a", int.class, int.class, int.class);
                try {
                    mouseClicked.invoke(screen, button.x + 1, button.y + 1, 0);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
                return;
            }
        }
        throw new AssertionError("no button \"" + label + "\"");
    }

    private static String text(List<String> lines) {
        StringBuilder out = new StringBuilder();
        for (String line : lines) {
            out.append(plain(line)).append(" | ");
        }
        return out.toString();
    }

    private static String plain(String text) {
        return text == null ? null : TextFormatting.getTextWithoutFormattingCodes(text);
    }

    private static void run(Minecraft mc, String command) {
        IntegratedServer server = mc.getIntegratedServer();
        server.addScheduledTask(() -> server.getCommandManager().executeCommand(server, command));
    }

    private static void screenshot(Minecraft mc, String name) {
        ScreenShotHelper.saveScreenshot(mc.gameDir, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
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
        mc.shutdown();
    }

    private static void deleteWorld(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteWorld(child);
            }
        }
        file.delete();
    }

    private static void log(String message) {
        System.out.println("[Identify selftest] " + message);
    }
}
