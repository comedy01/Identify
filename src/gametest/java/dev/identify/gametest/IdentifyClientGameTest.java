package dev.identify.gametest;

import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import dev.identify.look.BlockDetails;
import dev.identify.look.ItemCompare;
import dev.identify.look.ItemDetails;
import dev.identify.look.LookResolver;
import dev.identify.look.LookTarget;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class IdentifyClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.waitTicks(20);
            IdentifyConfig config = IdentifyClient.config();
            config.resetToDefaults();

            world.getServer().runCommand("tp @p 0 -60 0 0 0");
            world.getServer().runCommand("setblock 0 -59 3 diamond_block");
            context.waitTicks(10);

            LookTarget block = look(context);
            check(block != null, "no target found while looking at a diamond block");
            log("block target: " + block.name().getString() + " / " + block.namespace() + " / " + text(block.details()));
            check(block.name().getString().equals("Block of Diamond"), "wrong block name: " + block.name().getString());
            check(block.namespace().equals("minecraft"), "wrong namespace: " + block.namespace());
            check(!block.icon().isEmpty(), "block icon is empty");
            log("screenshot: " + context.takeScreenshot("identify-block"));

            world.getServer().runCommand("setblock 0 -59 3 air");
            world.getServer().runCommand("tp @p 0 -60 0 0 18");
            world.getServer().runCommand("summon cow 0 -60 4 {Age:-24000,NoAI:1b}");
            context.waitTicks(20);

            LookTarget cow = look(context);
            check(cow != null, "no target found while looking at a baby cow");
            String cowText = text(cow.details());
            log("entity target: " + cow.name().getString() + " / " + cowText);
            check(cow.name().getString().equals("Cow"), "wrong entity name: " + cow.name().getString());
            check(cowText.contains("Health: 10 / 10"), "missing health line: " + cowText);
            check(cowText.contains("grows up in 19:"), "missing baby grow-up timer: " + cowText);
            log("screenshot: " + context.takeScreenshot("identify-baby-cow"));

            config.setShowEntities(false);
            LookTarget ground = look(context);
            check(ground != null && ground.name().getString().equals("Grass Block"),
                    "expected the grass behind the cow with entities off, got " + (ground == null ? "nothing" : ground.name().getString()));
            config.setShowEntities(true);

            config.setShowDetails(false);
            LookTarget bare = look(context);
            check(bare != null && bare.details().isEmpty(), "details shown although switched off");
            config.setShowDetails(true);

            world.getServer().runCommand("kill @e[type=cow]");
            world.getServer().runCommand("tp @p 0 -60 0 0 0");
            context.waitTicks(10);
            check(look(context) == null, "target reported while looking at empty air");

            checkCrops(context);
            checkItems(context);
            checkMobIntel(context, world);
            checkBlockEntities(context, world);
            checkCompare(context);
        }
        log("ALL CHECKS PASSED");
    }

    private static void checkMobIntel(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("tp @p 0 -60 0 0 15");

        String villager = lookAfter(context, world,
                "summon villager 0 -60 4 {NoAI:1b,VillagerData:{type:\"minecraft:plains\",profession:\"minecraft:farmer\",level:3}}");
        log("villager: " + villager);
        check(villager.contains("Farmer · Journeyman"), "wrong villager line: " + villager);

        AtomicReference<int[]> uuid = new AtomicReference<>();
        AtomicReference<String> name = new AtomicReference<>();
        context.runOnClient(client -> {
            uuid.set(UUIDUtil.uuidToIntArray(client.player.getUUID()));
            name.set(client.player.getName().getString());
        });
        int[] id = uuid.get();
        String wolf = lookAfter(context, world,
                "summon wolf 0 -60 4 {NoAI:1b,Owner:[I;" + id[0] + "," + id[1] + "," + id[2] + "," + id[3] + "]}");
        log("wolf: " + wolf);
        check(wolf.contains("Owner: " + name.get()), "wrong owner line: " + wolf);

        String horse = lookAndShoot(context, world, "identify-horse",
                "summon horse 0 -60 4 {NoAI:1b,Tame:1b,attributes:[{id:\"minecraft:movement_speed\",base:0.225d},{id:\"minecraft:jump_strength\",base:0.6d}]}");
        log("horse: " + horse);
        check(horse.contains("Speed: 9.7 blocks/s · Jump: 2.2"), "wrong horse line: " + horse);
        check(horse.contains("Tamed"), "missing tamed line: " + horse);

        String husk = lookAndShoot(context, world, "identify-husk",
                "summon husk 0 -60 4 {NoAI:1b,equipment:{mainhand:{id:\"minecraft:iron_sword\"},head:{id:\"minecraft:iron_helmet\"}}}",
                "effect give @e[type=husk,limit=1] minecraft:speed 100 1");
        log("husk: " + husk);
        check(husk.contains("Armor:"), "missing armor: " + husk);
        check(husk.contains("Effects: Speed II"), "missing effects: " + husk);
        check(husk.contains("Holding: Iron Sword"), "missing held item: " + husk);
        check(husk.split(" \\| ").length <= 4, "mob panel grew past four lines: " + husk);
        world.getServer().runCommand("tp @p 0 -60 0 0 0");
    }

    private static void checkBlockEntities(ClientGameTestContext context, TestSingleplayerContext world) {
        world.getServer().runCommand("tp @p 0 -60 0 0 0");

        world.getServer().runCommand("setblock 0 -59 3 spawner{SpawnData:{entity:{id:\"minecraft:zombie\"}}}");
        context.waitTicks(20);
        String spawner = detailsText(context);
        log("spawner: " + spawner);
        check(spawner.contains("Spawns: Zombie"), "wrong spawner line: " + spawner);

        world.getServer().runCommand("setblock 0 -59 3 bee_nest[honey_level=5]");
        context.waitTicks(10);
        check(detailsText(context).contains("Honey ready to harvest"), "missing ready hive: " + detailsText(context));
        world.getServer().runCommand("setblock 0 -59 3 bee_nest[honey_level=2]");
        context.waitTicks(10);
        check(detailsText(context).contains("Honey: 2/5"), "missing hive honey: " + detailsText(context));

        world.getServer().runCommand("fill -1 -60 2 1 -60 4 iron_block");
        world.getServer().runCommand("setblock 0 -59 3 beacon{primary_effect:\"minecraft:speed\"}");
        context.waitTicks(200);
        String beacon = detailsText(context);
        log("beacon: " + beacon);
        check(beacon.contains("Tier 1 · Range: 20 blocks"), "wrong beacon tier line: " + beacon);
        check(beacon.contains("Effect: Speed"), "wrong beacon effect line: " + beacon);
        log("screenshot: " + context.takeScreenshot("identify-beacon"));

        world.getServer().runCommand("setblock 0 -59 3 air");
        world.getServer().runCommand("fill -1 -60 2 1 -60 4 grass_block");
    }

    private static void checkCompare(ClientGameTestContext context) {
        AtomicReference<String> armor = new AtomicReference<>();
        AtomicReference<String> sword = new AtomicReference<>();
        AtomicReference<Boolean> blockIgnored = new AtomicReference<>();
        AtomicReference<Boolean> sameIgnored = new AtomicReference<>();
        context.runOnClient(client -> {
            ItemStack diamond = new ItemStack(Items.DIAMOND_CHESTPLATE);
            ItemStack iron = new ItemStack(Items.IRON_CHESTPLATE);
            armor.set(ItemCompare.difference(diamond, iron, ItemCompare.slotFor(diamond)).getString());

            ItemStack diamondSword = new ItemStack(Items.DIAMOND_SWORD);
            ItemStack ironSword = new ItemStack(Items.IRON_SWORD);
            sword.set(ItemCompare.difference(diamondSword, ironSword, ItemCompare.slotFor(diamondSword)).getString());

            blockIgnored.set(ItemCompare.difference(
                    new ItemStack(Items.STONE), ironSword, EquipmentSlot.MAINHAND) == null);
            sameIgnored.set(ItemCompare.difference(iron, new ItemStack(Items.IRON_CHESTPLATE), EquipmentSlot.CHEST) == null);
        });
        log("compare: " + armor.get() + " | " + sword.get());
        check(armor.get().equals("+2 Armor, +2 Toughness"), "wrong armor comparison: " + armor.get());
        check(sword.get().equals("+1 Damage"), "wrong sword comparison: " + sword.get());
        check(blockIgnored.get(), "a block was compared against a sword");
        check(sameIgnored.get(), "identical items were compared");
    }

    private static String lookAfter(ClientGameTestContext context, TestSingleplayerContext world, String... commands) {
        return lookAndShoot(context, world, null, commands);
    }

    private static String lookAndShoot(
            ClientGameTestContext context, TestSingleplayerContext world, String screenshot, String... commands) {
        for (String command : commands) {
            world.getServer().runCommand(command);
        }
        context.waitTicks(15);
        String text = detailsText(context);
        if (screenshot != null) {
            log("screenshot: " + context.takeScreenshot(screenshot));
        }
        world.getServer().runCommand("kill @e[type=!player]");
        context.waitTicks(5);
        return text;
    }

    private static String detailsText(ClientGameTestContext context) {
        LookTarget target = look(context);
        check(target != null, "no target found");
        return text(target.details());
    }

    private static void checkCrops(ClientGameTestContext context) {
        AtomicReference<String> growing = new AtomicReference<>();
        AtomicReference<String> mature = new AtomicReference<>();
        context.runOnClient(client -> {
            BlockState young = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3);
            BlockState old = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
            growing.set(text(BlockDetails.of(young)));
            mature.set(text(BlockDetails.of(old)));
        });
        log("crop details: " + growing.get() + " | " + mature.get());
        check(growing.get().contains("Growth: 3/7 (43%)"), "wrong growth line: " + growing.get());
        check(mature.get().contains("Fully grown"), "wrong mature line: " + mature.get());
    }

    private static void checkItems(ClientGameTestContext context) {
        AtomicReference<String> tool = new AtomicReference<>();
        AtomicReference<String> food = new AtomicReference<>();
        AtomicReference<String> fuel = new AtomicReference<>();
        context.runOnClient(client -> {
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            pickaxe.setDamageValue(500);
            tool.set(itemText(pickaxe));
            food.set(itemText(new ItemStack(Items.BREAD)));
            fuel.set(itemText(new ItemStack(Items.COAL)));
        });
        log("item details: " + tool.get() + " | " + food.get() + " | " + fuel.get());
        check(tool.get().contains("Durability: 1061/1561 (68%)"), "wrong durability line: " + tool.get());
        check(food.get().contains("Food: 5 hunger, 6.0 saturation"), "wrong food line: " + food.get());
        check(fuel.get().contains("smelts 8 items"), "wrong fuel line: " + fuel.get());
    }

    private static String itemText(ItemStack stack) {
        List<Component> lines = new ArrayList<>();
        ItemDetails.append(stack, lines);
        return text(lines);
    }

    private static LookTarget look(ClientGameTestContext context) {
        AtomicReference<LookTarget> result = new AtomicReference<>();
        context.runOnClient(client ->
                result.set(LookResolver.resolve(client, 1.0F, IdentifyClient.config())));
        return result.get();
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void log(String message) {
        System.out.println("[identify-gametest] " + message);
    }
}
