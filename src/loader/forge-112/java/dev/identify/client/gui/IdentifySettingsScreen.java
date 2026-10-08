package dev.identify.client.gui;

import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import dev.identify.config.IdentifyPolicy;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public final class IdentifySettingsScreen extends GuiScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;
    private static final int GAP = 10;

    private final GuiScreen lastScreen;
    private final Map<GuiButton, String> tooltips = new IdentityHashMap<>();
    private final Map<GuiButton, Runnable> actions = new IdentityHashMap<>();
    private int y;

    public IdentifySettingsScreen(GuiScreen lastScreen) {
        this.lastScreen = lastScreen;
    }

    @Override
    public void initGui() {
        IdentifyConfig config = IdentifyClient.config();
        buttonList.clear();
        tooltips.clear();
        actions.clear();
        y = height / 6 - 12;

        addRow(
                toggleButton("identify.options.enabled", "identify.options.enabled.tooltip",
                        config::enabled, config::setEnabled),
                toggleButton("identify.options.item_tooltips", "identify.options.item_tooltips.tooltip",
                        config::itemTooltips, config::setItemTooltips));

        addRow(
                toggleButton("identify.options.blocks", "identify.options.blocks.tooltip",
                        config::showBlocks, config::setShowBlocks),
                toggleButton("identify.options.entities", "identify.options.entities.tooltip",
                        config::showEntities, config::setShowEntities));

        addRow(
                toggleButton("identify.options.icon", null,
                        config::showIcon, config::setShowIcon),
                toggleButton("identify.options.details", "identify.options.details.tooltip",
                        config::showDetails, config::setShowDetails));

        addRow(
                toggleButton("identify.options.mod_name", null,
                        config::showModName, config::setShowModName),
                toggleButton("identify.options.compare_items", "identify.options.compare_items.tooltip",
                        config::compareItems, config::setCompareItems));

        addRow(slider("identify.options.range", "identify.options.range.tooltip", "",
                (int) IdentifyPolicy.MIN_RANGE, (int) IdentifyPolicy.MAX_RANGE,
                (int) Math.round(config.range()), config::setRange));

        addRow(
                slider("identify.options.x_position", "identify.options.x_position.tooltip", "%",
                        IdentifyPolicy.MIN_POSITION, IdentifyPolicy.MAX_POSITION,
                        config.xPosition(), config::setXPosition),
                slider("identify.options.y_position", "identify.options.y_position.tooltip", "%",
                        IdentifyPolicy.MIN_POSITION, IdentifyPolicy.MAX_POSITION,
                        config.yPosition(), config::setYPosition));

        addRow(button(WIDTH, I18n.format("identify.options.reset"), () -> {
            config.resetToDefaults();
            mc.displayGuiScreen(new IdentifySettingsScreen(lastScreen));
        }));

        y += 6;
        addRow(button(200, I18n.format("gui.done"), () -> mc.displayGuiScreen(lastScreen)));
    }

    private void addRow(GuiButton... buttons) {
        int total = 0;
        for (GuiButton button : buttons) {
            total += button.width;
        }
        int x = (width - total - GAP * (buttons.length - 1)) / 2;
        for (GuiButton button : buttons) {
            button.id = buttonList.size();
            button.x = x;
            button.y = y;
            buttonList.add(button);
            x += button.width + GAP;
        }
        y += 24;
    }

    private GuiButton button(int buttonWidth, String label, Runnable action) {
        GuiButton button = new GuiButton(0, 0, 0, buttonWidth, HEIGHT, label);
        actions.put(button, action);
        return button;
    }

    private GuiButton toggleButton(String key, String tooltipKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        GuiButton[] holder = new GuiButton[1];
        holder[0] = button(WIDTH, onOffLabel(key, getter.getAsBoolean()), () -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(next);
            holder[0].displayString = onOffLabel(key, next);
        });
        if (tooltipKey != null) {
            tooltips.put(holder[0], I18n.format(tooltipKey));
        }
        return holder[0];
    }

    private static String onOffLabel(String key, boolean value) {
        return I18n.format(key) + ": " + I18n.format(value ? "options.on" : "options.off");
    }

    private GuiButton slider(
            String key, String tooltipKey, String suffix, int min, int max, int initial, IntConsumer onChange) {
        GuiSlider slider = new GuiSlider(
                0, 0, 0, WIDTH, HEIGHT, I18n.format(key) + ": ", suffix, min, max, initial, false, true,
                changed -> onChange.accept(changed.getValueInt()));
        tooltips.put(slider, I18n.format(tooltipKey));
        return slider;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        Runnable action = actions.get(button);
        if (action != null) {
            action.run();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(lastScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRenderer, I18n.format("identify.options.title"), width / 2, 15, 0xFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (Map.Entry<GuiButton, String> entry : tooltips.entrySet()) {
            GuiButton button = entry.getKey();
            if (button.visible
                    && mouseX >= button.x && mouseX < button.x + button.width
                    && mouseY >= button.y && mouseY < button.y + button.height) {
                drawHoveringText(fontRenderer.listFormattedStringToWidth(entry.getValue(), 200), mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public void onGuiClosed() {
        IdentifyClient.saveConfig();
    }
}
