package dev.identify.look;

import dev.identify.Texts;
import dev.identify.info.TickTime;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockDetails {
    private static final int BEACON_RANGE_BASE = 10;
    private static final int BEACON_RANGE_PER_TIER = 10;

    private BlockDetails() {
    }

    public static List<Component> of(BlockState state) {
        List<Component> lines = stateLines(state);
        addIfPresent(lines, toolLine(state));
        return lines;
    }

    public static List<Component> of(Level level, BlockPos pos, BlockState state) {
        List<Component> lines = stateLines(state);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SpawnerBlockEntity spawner) {
            addIfPresent(lines, spawnerLine(level, pos, spawner));
        } else if (entity instanceof BeaconBlockEntity beacon) {
            beaconLines(level, pos, beacon, lines);
        }
        addIfPresent(lines, toolLine(state));
        return lines;
    }

    private static List<Component> stateLines(BlockState state) {
        List<Component> lines = new ArrayList<>(3);

        IntegerProperty age = ageProperty(state);
        if (age != null) {
            int value = state.getValue(age);
            int max = maxAge(state, age);
            if (value >= max) {
                lines.add(Texts.translatable("identify.crop.mature"));
            } else {
                lines.add(Texts.translatable("identify.crop.growth", value, max, TickTime.percent(value, max)));
            }
        }

        if (state.is(Blocks.REDSTONE_WIRE)) {
            lines.add(Texts.translatable("identify.redstone.power", state.getValue(BlockStateProperties.POWER)));
        }

        if (state.getBlock() instanceof BeehiveBlock && state.hasProperty(BeehiveBlock.HONEY_LEVEL)) {
            int honey = state.getValue(BeehiveBlock.HONEY_LEVEL);
            int max = Collections.max(BeehiveBlock.HONEY_LEVEL.getPossibleValues());
            if (honey >= max) {
                lines.add(Texts.translatable("identify.hive.ready"));
            } else {
                lines.add(Texts.translatable("identify.hive.honey", honey, max));
            }
        }
        return lines;
    }

    private static Component spawnerLine(Level level, BlockPos pos, SpawnerBlockEntity spawner) {
        Entity display = BlockEntityCompat.spawnerEntity(level, pos, spawner);
        if (display == null) {
            return null;
        }
        return Texts.translatable("identify.spawner.mob", display.getType().getDescription());
    }

    private static void beaconLines(Level level, BlockPos pos, BeaconBlockEntity beacon, List<Component> lines) {
        CompoundTag tag = ServerBeacon.tag(level, pos, beacon);
        int tier = NbtCompat.intOr(tag, "Levels", 0);
        if (tier <= 0) {
            return;
        }
        int range = BEACON_RANGE_BASE + tier * BEACON_RANGE_PER_TIER;
        lines.add(Texts.translatable("identify.beacon.tier", tier, range));

        String primaryId = BlockEntityCompat.effectId(tag, true);
        String secondaryId = BlockEntityCompat.effectId(tag, false);
        Component primary = effectName(primaryId);
        Component secondary = effectName(secondaryId);
        if (primary == null) {
            return;
        }
        if (secondary == null) {
            lines.add(Texts.translatable("identify.beacon.effects", primary));
        } else if (secondaryId.equals(primaryId)) {
            lines.add(Texts.translatable("identify.beacon.effects",
                    Texts.translatable("potion.withAmplifier", primary, Texts.translatable("potion.potency.1"))));
        } else {
            lines.add(Texts.translatable("identify.beacon.effects_two", primary, secondary));
        }
    }

    private static Component effectName(String id) {
        if (id.isEmpty()) {
            return null;
        }
        for (MobEffect effect : GameRegistries.mobEffects()) {
            if (id.equals(GameRegistries.mobEffectId(effect))) {
                return effect.getDisplayName();
            }
        }
        return null;
    }

    private static void addIfPresent(List<Component> lines, Component line) {
        if (line != null) {
            lines.add(line);
        }
    }

    private static IntegerProperty ageProperty(BlockState state) {
        if (!(state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof NetherWartBlock
                || state.getBlock() instanceof SweetBerryBushBlock
                || state.getBlock() instanceof CocoaBlock)) {
            return null;
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty integer && property.getName().equals("age")) {
                return integer;
            }
        }
        return null;
    }

    private static int maxAge(BlockState state, IntegerProperty age) {
        int max = Collections.max(age.getPossibleValues());
        if (state.getBlock() instanceof CropBlock crop) {
            return Math.max(max, crop.getMaxAge());
        }
        return max;
    }

    private static Component toolLine(BlockState state) {
        String tool = HarvestTools.tool(state);
        if (tool == null) {
            return null;
        }
        Component toolName = Texts.translatable("identify.tool." + tool);
        String tier = HarvestTools.tier(state);
        if (tier == null) {
            return Texts.translatable("identify.tool.best", toolName);
        }
        return Texts.translatable("identify.tool.best_tier", toolName, Texts.translatable("identify.tier." + tier));
    }
}
