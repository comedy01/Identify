package dev.identify.hud;

import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import dev.identify.config.IdentifyPolicy;
import dev.identify.look.LookResolver;
import dev.identify.look.LookTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;

public final class IdentifyHudRenderer {
    private static final int PADDING = 5;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 5;
    private static final int LINE_HEIGHT = 10;
    private static final int BACKGROUND = 0xC0100810;
    private static final int BORDER = 0xB05A2D9A;
    private static final int NAME_COLOR = 0xFFFFFFFF;
    private static final int DETAIL_COLOR = 0xFFB0B0B0;
    private static final int MOD_COLOR = 0xFF5B7BFF;

    private IdentifyHudRenderer() {
    }

    public static void render(ScaledResolution resolution, float partialTick) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null || mc.currentScreen != null || mc.gameSettings.hideGUI) {
            return;
        }
        IdentifyConfig config = IdentifyClient.config();
        if (!config.enabled()) {
            return;
        }

        LookTarget target = LookResolver.resolve(mc, partialTick, config);
        if (target == null) {
            return;
        }
        draw(resolution, mc, target, config);
    }

    private static void draw(ScaledResolution resolution, Minecraft mc, LookTarget target, IdentifyConfig config) {
        FontRenderer font = mc.fontRenderer;
        boolean icon = config.showIcon() && !target.icon().isEmpty();
        boolean modLine = config.showModName();

        List<String> lines = new ArrayList<>();
        lines.add(target.name());
        lines.addAll(target.details());
        if (modLine) {
            lines.add(TextFormatting.ITALIC + LookResolver.modLabel(target));
        }

        int textWidth = 0;
        for (String line : lines) {
            textWidth = Math.max(textWidth, font.getStringWidth(line));
        }

        int textHeight = lines.size() * LINE_HEIGHT - 1;
        int contentHeight = icon ? Math.max(textHeight, ICON_SIZE) : textHeight;
        int contentWidth = textWidth + (icon ? ICON_SIZE + ICON_GAP : 0);
        int boxWidth = contentWidth + PADDING * 2;
        int boxHeight = contentHeight + PADDING * 2;

        int left = IdentifyPolicy.place(config.xPosition(), resolution.getScaledWidth(), boxWidth);
        int top = IdentifyPolicy.place(config.yPosition(), resolution.getScaledHeight(), boxHeight);

        Gui.drawRect(left, top, left + boxWidth, top + boxHeight, BORDER);
        Gui.drawRect(left + 1, top + 1, left + boxWidth - 1, top + boxHeight - 1, BACKGROUND);

        int textX = left + PADDING + (icon ? ICON_SIZE + ICON_GAP : 0);
        int textY = top + PADDING + (contentHeight - textHeight) / 2;

        if (icon) {
            RenderHelper.enableGUIStandardItemLighting();
            mc.getRenderItem().renderItemAndEffectIntoGUI(
                    target.icon(), left + PADDING, top + PADDING + (contentHeight - ICON_SIZE) / 2);
            RenderHelper.disableStandardItemLighting();
        }

        for (int i = 0; i < lines.size(); i++) {
            int color = i == 0 ? NAME_COLOR : modLine && i == lines.size() - 1 ? MOD_COLOR : DETAIL_COLOR;
            font.drawString(lines.get(i), textX, textY + i * LINE_HEIGHT, color, i == 0);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
