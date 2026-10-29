package dev.identify.nftest;

import dev.identify.client.IdentifyClient;
import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.look.ItemCompare;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

final class Gallery {
    private record Scene(String name, String mob, int delay, String... setup) {
    }

    private static Scene block(String name, int delay, String... setup) {
        return new Scene(name, null, delay, setup);
    }

    private static Scene mob(String name, String mob, String... setup) {
        return new Scene(name, mob, 40, setup);
    }

    private static final List<Scene> SCENES = List.of(
            block("crop", 30, "setblock %1$d %4$d %3$d farmland", "setblock %1$d %2$d %3$d wheat[age=4]"),
            block("best-tool", 30, "setblock %1$d %2$d %3$d diamond_ore"),
            block("spawner", 40, "setblock %1$d %2$d %3$d spawner{SpawnData:{entity:{id:\"minecraft:zombie\"}}}"),
            block("beacon", 120, "fill %5$d %4$d %6$d %7$d %4$d %8$d iron_block",
                    "setblock %1$d %2$d %3$d beacon{primary_effect:\"minecraft:speed\"}"),
            block("beehive", 30, "setblock %1$d %2$d %3$d beehive[honey_level=5]"),
            block("composter", 30, "setblock %1$d %2$d %3$d composter[level=5]"),
            block("cauldron", 30, "setblock %1$d %2$d %3$d water_cauldron[level=2]"),
            block("jukebox", 30, "setblock %1$d %2$d %3$d jukebox[has_record=true]{RecordItem:{id:\"minecraft:music_disc_cat\",count:1}}"),
            block("respawn-anchor", 30, "setblock %1$d %2$d %3$d respawn_anchor[charges=3]"),
            block("note-block", 30, "setblock %1$d %2$d %3$d note_block[note=12]"),
            block("repeater", 30, "setblock %1$d %2$d %3$d repeater[delay=3,facing=north]"),
            block("comparator", 30, "setblock %1$d %2$d %3$d comparator[mode=subtract,facing=north]"),
            block("redstone", 30, "setblock %9$d %2$d %3$d redstone_block", "setblock %1$d %2$d %3$d redstone_wire"),
            block("lectern", 30, "setblock %1$d %2$d %3$d lectern[has_book=true]{Book:{id:\"minecraft:writable_book\",count:1,components:{\"minecraft:writable_book_content\":{pages:[\"a\",\"b\",\"c\"]}}},Page:1}"),
            block("furnace", 200, "setblock %1$d %2$d %3$d furnace{Items:[{Slot:0b,id:\"minecraft:iron_ore\",count:16},{Slot:1b,id:\"minecraft:coal\",count:8}]}"),
            block("smoker", 120, "setblock %1$d %2$d %3$d smoker{Items:[{Slot:0b,id:\"minecraft:beef\",count:12},{Slot:1b,id:\"minecraft:coal\",count:4}]}"),
            block("blast-furnace", 160, "setblock %1$d %2$d %3$d blast_furnace{Items:[{Slot:0b,id:\"minecraft:raw_gold\",count:20},{Slot:1b,id:\"minecraft:coal\",count:6}]}"),
            block("brewing", 160, "setblock %1$d %2$d %3$d brewing_stand{Items:["
                    + "{Slot:0b,id:\"minecraft:potion\",count:1,components:{\"minecraft:potion_contents\":{potion:\"minecraft:awkward\"}}},"
                    + "{Slot:1b,id:\"minecraft:potion\",count:1,components:{\"minecraft:potion_contents\":{potion:\"minecraft:awkward\"}}},"
                    + "{Slot:2b,id:\"minecraft:potion\",count:1,components:{\"minecraft:potion_contents\":{potion:\"minecraft:awkward\"}}},"
                    + "{Slot:3b,id:\"minecraft:sugar\",count:1},{Slot:4b,id:\"minecraft:blaze_powder\",count:4}]}"),
            block("campfire", 140, "setblock %1$d %2$d %3$d campfire[lit=true]{Items:[{Slot:0b,id:\"minecraft:beef\",count:1}],"
                    + "CookingTimes:[I;0,0,0,0],CookingTotalTimes:[I;600,600,600,600]}"),
            mob("baby-cow", "minecraft:cow", "summon cow %1$d.5 %2$d %3$d.5 {Age:-24000,NoAI:1b,Rotation:[0f,0f]}"),
            mob("villager", "minecraft:villager", "summon villager %1$d.5 %2$d %3$d.5 {NoAI:1b,Rotation:[0f,0f],"
                    + "VillagerData:{type:\"minecraft:plains\",profession:\"minecraft:librarian\",level:3}}"),
            mob("wolf", "minecraft:wolf", "summon wolf %1$d.5 %2$d %3$d.5 {NoAI:1b,Rotation:[0f,0f],Owner:%10$s,"
                    + "CollarColor:14b,InLove:600}"),
            mob("horse", "minecraft:horse", "summon horse %1$d.5 %2$d %3$d.5 {NoAI:1b,Tame:1b,Variant:3,Rotation:[0f,0f],"
                    + "attributes:[{id:\"minecraft:generic.movement_speed\",base:0.3125d},{id:\"minecraft:generic.jump_strength\",base:0.9d}]}"),
            mob("zombie", "minecraft:zombie", "summon zombie %1$d.5 %2$d %3$d.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[0f,0f],"
                    + "HandItems:[{id:\"minecraft:diamond_sword\",count:1},{}],"
                    + "ArmorItems:[{},{},{id:\"minecraft:iron_chestplate\",count:1},{id:\"minecraft:iron_helmet\",count:1}],"
                    + "active_effects:[{id:\"minecraft:speed\",duration:99999,amplifier:1b},{id:\"minecraft:fire_resistance\",duration:99999}]}")
    );

    private static final class Tooltips extends Screen {
        private final List<List<Component>> groups;

        private Tooltips(List<List<Component>> groups) {
            super(Component.empty());
            this.groups = groups;
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            for (int i = 0; i < groups.size(); i++) {
                int x = width * (4 + i * 32) / 100;
                graphics.renderComponentTooltip(font, groups.get(i), x, height / 4);
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    private int idle;
    private int phase;
    private int wait;
    private int floor;
    private int scene;
    private int clock;
    private LivingEntity target;
    private boolean finished;

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
                String name = "identify-gallery-" + System.currentTimeMillis();
                LevelSettings settings = new LevelSettings(name, GameType.CREATIVE, false, Difficulty.EASY,
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
            command(server, "gamerule sendCommandFeedback false");
            command(server, "gamerule logAdminCommands false");
            command(server, "gamerule doDaylightCycle false");
            command(server, "gamerule doMobSpawning false");
            command(server, "gamerule doFireTick false");
            command(server, "gamerule randomTickSpeed 0");
            command(server, "time set noon");
            command(server, "weather clear");
            command(server, "kill @e[type=!player]");
            command(server, "clear @p");
            IdentifyClient.config().setShowBestTool(false);
            phase = 2;
            return;
        }
        if (phase == 2) {
            scenes(mc, server);
            return;
        }
        items(mc, server);
    }

    private void scenes(Minecraft mc, IntegratedServer server) {
        if (scene >= SCENES.size()) {
            phase = 3;
            clock = 0;
            return;
        }
        Scene current = SCENES.get(scene);
        int x = scene * 8;
        BlockPos pos = new BlockPos(x, floor, 0);
        if (clock == 0) {
            String owner = Arrays.stream(UUIDUtil.uuidToIntArray(mc.player.getUUID()))
                    .mapToObj(String::valueOf).collect(Collectors.joining(",", "[I;", "]"));
            for (String setup : current.setup()) {
                command(server, String.format(setup, x, floor, 0, floor - 1, x - 1, -1, x + 1, 1, x + 1, owner));
            }
            double back = current.mob() == null ? 2.8 : 4.2;
            command(server, "tp @p " + (x + 0.5) + " " + floor + " " + back);
            target = null;
        }
        if (current.mob() != null && target == null && clock > 5) {
            target = mc.level.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(2),
                    entity -> entity != mc.player).stream().findFirst().orElse(null);
        }
        if (target != null) {
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, target.getBoundingBox().getCenter().add(0, 0.2, 0));
        } else if (current.mob() == null) {
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
        }
        if (clock == current.delay()) {
            shot(mc, current.name());
        }
        if (++clock > current.delay() + 5) {
            scene++;
            clock = 0;
        }
    }

    private void items(Minecraft mc, IntegratedServer server) {
        if (clock == 0) {
            command(server, "item replace entity @p armor.chest with iron_chestplate");
            command(server, "item replace entity @p weapon.mainhand with iron_sword");
            command(server, "tp @p 0.5 " + floor + " -30 180 -10");
        }
        if (clock == 20) {
            mc.options.hideGui = true;
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.setDamageValue(500);
            mc.setScreen(new Tooltips(List.of(
                    tooltip(mc, pickaxe, false),
                    tooltip(mc, new ItemStack(Items.COOKED_BEEF), false),
                    tooltip(mc, new ItemStack(Items.COAL_BLOCK), false))));
        }
        if (clock == 40) {
            shot(mc, "tooltips");
        }
        if (clock == 50) {
            mc.setScreen(new Tooltips(List.of(
                    tooltip(mc, new ItemStack(Items.DIAMOND_CHESTPLATE), true),
                    tooltip(mc, new ItemStack(Items.NETHERITE_SWORD), true),
                    tooltip(mc, new ItemStack(Items.GOLDEN_HELMET), false))));
        }
        if (clock == 70) {
            shot(mc, "compare");
        }
        if (clock == 80) {
            mc.options.hideGui = false;
            mc.setScreen(new IdentifySettingsScreen(null, mc.options));
        }
        if (clock == 100) {
            shot(mc, "settings");
        }
        if (clock == 110) {
            finished = true;
            System.out.println("[Identify gallery] done");
            mc.stop();
        }
        clock++;
    }

    private static List<Component> tooltip(Minecraft mc, ItemStack stack, boolean compare) {
        List<Component> lines = new ArrayList<>(stack.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL));
        if (compare) {
            String hint = Component.translatable("identify.compare.hint").getString();
            lines.removeIf(line -> line.getString().equals(hint));
            EquipmentSlot slot = ItemCompare.slotFor(stack);
            Component parts = ItemCompare.difference(stack, mc.player.getItemBySlot(slot), slot);
            if (parts != null) {
                lines.add(Component.translatable("identify.compare.line", parts).withStyle(ChatFormatting.GRAY));
            }
        }
        return lines;
    }

    private static void shot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, "identify-" + name + ".png", mc.getMainRenderTarget(), message -> {
        });
        System.out.println("[Identify gallery] shot " + name);
    }

    private static void command(IntegratedServer server, String command) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
    }
}
