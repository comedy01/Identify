package dev.identify.hud;

import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import dev.identify.config.IdentifyPolicy;
import dev.identify.look.LookResolver;
import dev.identify.look.LookTarget;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

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
    private static final int MOD_COLOR = 0x5B7BFF;

    private IdentifyHudRenderer() {
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || HudCompat.isScreenOpen(mc) || HudCompat.isGuiHidden(mc)) {
            return;
        }
        IdentifyConfig config = IdentifyClient.config();
        if (!config.enabled()) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        LookTarget target = LookResolver.resolve(mc, partialTick, config);
        if (target == null) {
            return;
        }
        draw(graphics, mc, target, config);
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft mc, LookTarget target, IdentifyConfig config) {
        Font font = mc.font;
        boolean icon = config.showIcon() && !target.icon().isEmpty();

        List<Component> lines = new ArrayList<>();
        lines.add(target.name());
        lines.addAll(target.details());
        Component mod = null;
        if (config.showModName()) {
            mod = Component.literal(LookResolver.modLabel(target))
                    .withStyle(Style.EMPTY.withItalic(true).withColor(MOD_COLOR));
            lines.add(mod);
        }

        int textWidth = 0;
        for (Component line : lines) {
            textWidth = Math.max(textWidth, font.width(line));
        }

        int textHeight = lines.size() * LINE_HEIGHT - 1;
        int contentHeight = icon ? Math.max(textHeight, ICON_SIZE) : textHeight;
        int contentWidth = textWidth + (icon ? ICON_SIZE + ICON_GAP : 0);
        int boxWidth = contentWidth + PADDING * 2;
        int boxHeight = contentHeight + PADDING * 2;

        int left = IdentifyPolicy.place(config.xPosition(), graphics.guiWidth(), boxWidth);
        int top = IdentifyPolicy.place(config.yPosition(), graphics.guiHeight(), boxHeight);

        graphics.fill(left, top, left + boxWidth, top + boxHeight, BORDER);
        graphics.fill(left + 1, top + 1, left + boxWidth - 1, top + boxHeight - 1, BACKGROUND);

        int textX = left + PADDING + (icon ? ICON_SIZE + ICON_GAP : 0);
        int textY = top + PADDING + (contentHeight - textHeight) / 2;

        if (icon) {
            graphics.item(target.icon(), left + PADDING, top + PADDING + (contentHeight - ICON_SIZE) / 2);
        }

        for (int i = 0; i < lines.size(); i++) {
            Component line = lines.get(i);
            int color = i == 0 ? NAME_COLOR : DETAIL_COLOR;
            graphics.text(font, line, textX, textY + i * LINE_HEIGHT, color, i == 0);
        }
    }
}
