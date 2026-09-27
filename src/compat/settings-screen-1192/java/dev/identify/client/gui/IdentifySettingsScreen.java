package dev.identify.client.gui;

import dev.identify.Texts;
import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import dev.identify.config.IdentifyPolicy;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public final class IdentifySettingsScreen extends OptionsPage {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public IdentifySettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Texts.translatable("identify.options.title"));
    }

    @Override
    protected void addOptions() {
        IdentifyConfig config = IdentifyClient.config();

        addRow(
                toggleButton("identify.options.enabled", "identify.options.enabled.tooltip",
                        config::enabled, config::setEnabled),
                toggleButton("identify.options.item_tooltips", "identify.options.item_tooltips.tooltip",
                        config::itemTooltips, config::setItemTooltips));

        addRow(
                toggleButton("identify.options.blocks", null,
                        config::showBlocks, config::setShowBlocks),
                toggleButton("identify.options.entities", null,
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

        AbstractWidget rangeSlider = slider(new StepSlider(
                "identify.options.range", "identify.options.range.tooltip",
                IdentifyPolicy.MIN_RANGE, IdentifyPolicy.MAX_RANGE, 1.0,
                config.range(),
                value -> String.format(Locale.ROOT, "%.0f", value),
                config::setRange));
        addRow(rangeSlider);

        AbstractWidget xSlider = slider(new StepSlider(
                "identify.options.x_position", "identify.options.x_position.tooltip",
                IdentifyPolicy.MIN_POSITION, IdentifyPolicy.MAX_POSITION, 1.0,
                config.xPosition(),
                value -> (int) value + "%",
                value -> config.setXPosition((int) value)));
        AbstractWidget ySlider = slider(new StepSlider(
                "identify.options.y_position", "identify.options.y_position.tooltip",
                IdentifyPolicy.MIN_POSITION, IdentifyPolicy.MAX_POSITION, 1.0,
                config.yPosition(),
                value -> (int) value + "%",
                value -> config.setYPosition((int) value)));
        addRow(xSlider, ySlider);

        addRow(resetButton(config));
    }

    @Override
    public void removed() {
        super.removed();
        IdentifyClient.saveConfig();
    }

    private AbstractWidget toggleButton(String key, String tooltipKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button button = new Button(0, 0, WIDTH, HEIGHT, onOffLabel(key, getter.getAsBoolean()), pressed -> {
            boolean next = !getter.getAsBoolean();
            setter.accept(next);
            pressed.setMessage(onOffLabel(key, next));
        });
        return tooltipKey == null ? button : tooltip(button, Texts.translatable(tooltipKey));
    }

    private static Component onOffLabel(String key, boolean value) {
        Component state = Texts.translatable(value ? "options.on" : "options.off");
        return Texts.translatable("options.generic_value", Texts.translatable(key), state);
    }

    private AbstractWidget resetButton(IdentifyConfig config) {
        return new Button(0, 0, WIDTH, HEIGHT, Texts.translatable("identify.options.reset"), button -> {
            config.resetToDefaults();
            ScreenOpener.open(minecraft, new IdentifySettingsScreen(lastScreen, options));
        });
    }

    private AbstractWidget slider(StepSlider slider) {
        return tooltip(slider, Texts.translatable(slider.tooltipKey));
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
        private final String tooltipKey;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<String> format;
        private final DoubleConsumer onChange;

        StepSlider(
                String captionKey,
                String tooltipKey,
                double min,
                double max,
                double step,
                double initial,
                DoubleFunction<String> format,
                DoubleConsumer onChange) {
            super(0, 0, WIDTH, HEIGHT, Texts.empty(), 0.0);
            this.captionKey = captionKey;
            this.tooltipKey = tooltipKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
            updateMessage();
        }

        private double snap(double raw) {
            double clamped = Math.max(min, Math.min(max, raw));
            double snapped = min + Math.round((clamped - min) / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        private double current() {
            return snap(min + value * (max - min));
        }

        @Override
        protected void updateMessage() {
            Component shown = Texts.literal(format.apply(current()));
            setMessage(Texts.translatable("options.generic_value", Texts.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
