package dev.identify.look;

import dev.identify.info.TickTime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BlockDetails {
    private static final int BEACON_RANGE_BASE = 10;
    private static final int BEACON_RANGE_PER_TIER = 10;
    private static final int BEACON_LEVELS = 0;
    private static final int BEACON_PRIMARY = 1;
    private static final int BEACON_SECONDARY = 2;

    private BlockDetails() {
    }

    public static List<String> of(World world, BlockPos pos, IBlockState state) {
        List<String> lines = stateLines(state);
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof TileEntityMobSpawner) {
            addIfPresent(lines, spawnerLine((TileEntityMobSpawner) tile));
        } else if (tile instanceof TileEntityBeacon) {
            beaconLines((TileEntityBeacon) tile, lines);
        }
        addIfPresent(lines, toolLine(state));
        return lines;
    }

    private static List<String> stateLines(IBlockState state) {
        List<String> lines = new ArrayList<>(3);

        PropertyInteger age = ageProperty(state);
        if (age != null) {
            int value = state.getValue(age);
            int max = Collections.max(age.getAllowedValues());
            if (value >= max) {
                lines.add(I18n.format("identify.crop.mature"));
            } else {
                lines.add(I18n.format("identify.crop.growth", value, max, TickTime.percent(value, max)));
            }
        }

        if (state.getBlock() == Blocks.REDSTONE_WIRE) {
            lines.add(I18n.format("identify.redstone.power", state.getValue(BlockRedstoneWire.POWER)));
        }
        return lines;
    }

    private static String spawnerLine(TileEntityMobSpawner spawner) {
        Entity display = spawner.getSpawnerBaseLogic().getCachedEntity();
        if (display == null) {
            return null;
        }
        return I18n.format("identify.spawner.mob", display.getName());
    }

    private static void beaconLines(TileEntityBeacon beacon, List<String> lines) {
        int tier = beacon.getField(BEACON_LEVELS);
        if (tier <= 0) {
            return;
        }
        int range = BEACON_RANGE_BASE + tier * BEACON_RANGE_PER_TIER;
        lines.add(I18n.format("identify.beacon.tier", tier, range));

        int primaryId = beacon.getField(BEACON_PRIMARY);
        int secondaryId = beacon.getField(BEACON_SECONDARY);
        String primary = effectName(primaryId);
        String secondary = effectName(secondaryId);
        if (primary == null) {
            return;
        }
        if (secondary == null) {
            lines.add(I18n.format("identify.beacon.effects", primary));
        } else if (secondaryId == primaryId) {
            lines.add(I18n.format("identify.beacon.effects", primary + " " + I18n.format("potion.potency.1")));
        } else {
            lines.add(I18n.format("identify.beacon.effects_two", primary, secondary));
        }
    }

    private static String effectName(int id) {
        Potion potion = id <= 0 ? null : Potion.getPotionById(id);
        return potion == null ? null : I18n.format(potion.getName());
    }

    private static void addIfPresent(List<String> lines, String line) {
        if (line != null) {
            lines.add(line);
        }
    }

    private static PropertyInteger ageProperty(IBlockState state) {
        Block block = state.getBlock();
        if (!(block instanceof BlockCrops || block instanceof BlockNetherWart || block instanceof BlockCocoa)) {
            return null;
        }
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyInteger && property.getName().equals("age")) {
                return (PropertyInteger) property;
            }
        }
        return null;
    }

    private static String toolLine(IBlockState state) {
        String tool = HarvestTools.tool(state);
        if (tool == null) {
            return null;
        }
        String toolName = I18n.format("identify.tool." + tool);
        String tier = HarvestTools.tier(state);
        if (tier == null) {
            return I18n.format("identify.tool.best", toolName);
        }
        return I18n.format("identify.tool.best_tier", toolName, I18n.format("identify.tier." + tier));
    }
}
