package dev.identify.gametest;

import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import dev.identify.look.BlockDetails;
import dev.identify.look.ItemDetails;
import dev.identify.look.LookResolver;
import dev.identify.look.LookTarget;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.network.chat.Component;
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
        }
        log("ALL CHECKS PASSED");
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
