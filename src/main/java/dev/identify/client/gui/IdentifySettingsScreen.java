package dev.identify.client.gui;

import dev.identify.client.IdentifyClient;
import dev.identify.config.IdentifyConfig;
import dev.identify.config.IdentifyPolicy;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

public final class IdentifySettingsScreen extends OptionsSubScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public IdentifySettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Component.translatable("identify.options.title"));
    }

    @Override
    protected void addOptions() {
        IdentifyConfig config = IdentifyClient.config();

        list.addSmall(List.of(
                toggleButton("identify.options.enabled", "identify.options.enabled.tooltip",
                        config::enabled, config::setEnabled),
                toggleButton("identify.options.item_tooltips", "identify.options.item_tooltips.tooltip",
                        config::itemTooltips, config::setItemTooltips)));

        list.addSmall(List.of(
                toggleButton("identify.options.blocks", null,
                        config::showBlocks, config::setShowBlocks),
                toggleButton("identify.options.entities", null,
                        config::showEntities, config::setShowEntities)));

        list.addSmall(List.of(
                toggleButton("identify.options.icon", null,
                        config::showIcon, config::setShowIcon),
                toggleButton("identify.options.details", "identify.options.details.tooltip",
                        config::showDetails, config::setShowDetails)));

        list.addSmall(List.of(
                toggleButton("identify.options.mod_name", null,
                        config::showModName, config::setShowModName),
                toggleButton("identify.options.compare_items", "identify.options.compare_items.tooltip",
                        config::compareItems, config::setCompareItems)));

        AbstractWidget rangeSlider = new StepSlider(
                "identify.options.range", "identify.options.range.tooltip",
                IdentifyPolicy.MIN_RANGE, IdentifyPolicy.MAX_RANGE, 1.0,
                config.range(),
                value -> String.format(Locale.ROOT, "%.0f", value),
                config::setRange);
        list.addSmall(List.of(rangeSlider));

        AbstractWidget xSlider = new StepSlider(
                "identify.options.x_position", "identify.options.x_position.tooltip",
                IdentifyPolicy.MIN_POSITION, IdentifyPolicy.MAX_POSITION, 1.0,
                config.xPosition(),
                value -> (int) value + "%",
                value -> config.setXPosition((int) value));
        AbstractWidget ySlider = new StepSlider(
                "identify.options.y_position", "identify.options.y_position.tooltip",
                IdentifyPolicy.MIN_POSITION, IdentifyPolicy.MAX_POSITION, 1.0,
                config.yPosition(),
                value -> (int) value + "%",
                value -> config.setYPosition((int) value));
        list.addSmall(List.of(xSlider, ySlider));

        list.addSmall(List.of(resetButton(config)));
    }

    @Override
    public void removed() {
        super.removed();
        IdentifyClient.saveConfig();
    }

    private AbstractWidget toggleButton(String key, String tooltipKey, BooleanSupplier getter, Consumer<Boolean> setter) {
        Button.Builder builder = Button.builder(onOffLabel(key, getter.getAsBoolean()), button -> {
                    boolean next = !getter.getAsBoolean();
                    setter.accept(next);
                    button.setMessage(onOffLabel(key, next));
                })
                .width(WIDTH);
        if (tooltipKey != null) {
            builder.tooltip(Tooltip.create(Component.translatable(tooltipKey)));
        }
        return builder.build();
    }

    private static Component onOffLabel(String key, boolean value) {
        Component state = Component.translatable(value ? "options.on" : "options.off");
        return Component.translatable("options.generic_value", Component.translatable(key), state);
    }

    private AbstractWidget resetButton(IdentifyConfig config) {
        return Button.builder(Component.translatable("identify.options.reset"), button -> {
                    config.resetToDefaults();
                    rebuildWidgets();
                })
                .width(WIDTH)
                .build();
    }

    private static final class StepSlider extends AbstractSliderButton {
        private final String captionKey;
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
            super(0, 0, WIDTH, HEIGHT, Component.empty(), 0.0);
            this.captionKey = captionKey;
            this.min = min;
            this.max = max;
            this.step = step;
            this.format = format;
            this.onChange = onChange;
            this.value = (snap(initial) - min) / (max - min);
            setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
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
            Component shown = Component.literal(format.apply(current()));
            setMessage(Component.translatable("options.generic_value", Component.translatable(captionKey), shown));
        }

        @Override
        protected void applyValue() {
            double snapped = current();
            value = (snapped - min) / (max - min);
            onChange.accept(snapped);
        }
    }
}
