package dev.identify.hud;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class Canvas {
    private final PoseStack pose;

    public Canvas(PoseStack pose) {
        this.pose = pose;
    }

    int width() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    int height() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    void fill(int x0, int y0, int x1, int y1, int color) {
        GuiComponent.fill(pose, x0, y0, x1, y1, color);
    }

    void item(ItemStack stack, int x, int y) {
        Minecraft.getInstance().getItemRenderer().renderAndDecorateItem(stack, x, y);
    }

    void text(Font font, Component text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawShadow(pose, text, x, y, color);
        } else {
            font.draw(pose, text, x, y, color);
        }
    }
}
