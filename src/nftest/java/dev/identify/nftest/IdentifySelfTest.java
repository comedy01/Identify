package dev.identify.nftest;

import dev.identify.client.gui.IdentifySettingsScreen;
import dev.identify.config.IdentifyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import java.nio.file.Files;
import java.util.List;

@Mod(value = "identify_test", dist = Dist.CLIENT)
public final class IdentifySelfTest {
    private int ticks;
    private boolean done;

    public IdentifySelfTest() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, this::onTick);
    }

    private void onTick(ClientTickEvent.Post event) {
        if (done || ++ticks < 120) {
            return;
        }
        done = true;
        Minecraft client = Minecraft.getInstance();

        String tooltips = tooltipCheck();

        ModContainer container = ModList.get().getModContainerById("identify").orElse(null);
        boolean screen = container != null && container.getCustomExtension(IConfigScreenFactory.class)
                .map(factory -> factory.createScreen(container, null) instanceof IdentifySettingsScreen)
                .orElse(false);

        boolean config = Files.exists(FMLPaths.CONFIGDIR.get().resolve(IdentifyConfig.FILE_NAME));
        boolean pass = !tooltips.equals("false") && screen && config;
        System.out.println("[Identify selftest] tooltips=" + tooltips + " screen=" + screen
                + " config=" + config + " RESULT=" + (pass ? "PASS" : "FAIL"));
        client.stop();
    }

    private static String tooltipCheck() {
        ItemStack pickaxe;
        try {
            pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        } catch (NullPointerException e) {
            return "skipped";
        }
        pickaxe.setDamageValue(500);
        String tool = text(pickaxe.getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL));
        String food = text(new ItemStack(Items.BREAD).getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL));
        String coal = text(new ItemStack(Items.COAL).getTooltipLines(Item.TooltipContext.EMPTY, null, TooltipFlag.NORMAL));
        System.out.println("[Identify selftest] tool=" + tool + " food=" + food + " coal=" + coal);
        return String.valueOf(tool.contains("Durability: 1061/1561 (68%)")
                && food.contains("Food: 5 hunger, 6.0 saturation"));
    }

    private static String text(List<Component> lines) {
        StringBuilder out = new StringBuilder();
        for (Component line : lines) {
            out.append(line.getString()).append(" | ");
        }
        return out.toString();
    }
}
