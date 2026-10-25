package dev.identify.look;

import dev.identify.Texts;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class ModLines {
    private static final Object MISSING = new Object();
    private static final Map<String, Object> MEMBERS = new ConcurrentHashMap<>();

    private ModLines() {
    }

    static List<Component> read(BlockEntity entity) {
        String name = entity.getClass().getName();
        if (name.startsWith("net.minecraft.")) {
            return null;
        }
        List<Component> lines = new ArrayList<>(2);
        try {
            if (extendsClass(entity, "com.simibubi.create.content.kinetics.base.KineticBlockEntity",
                    "com.simibubi.create.content.contraptions.base.KineticTileEntity")) {
                create(entity, lines);
            } else if (name.startsWith("mekanism.")) {
                mekanism(entity, lines);
            } else if (name.startsWith("appeng.")) {
                ae2(entity, lines);
            } else if (name.startsWith("vectorwing.farmersdelight.")) {
                farmersDelight(entity, lines);
            } else if (name.startsWith("vazkii.botania.")) {
                botania(entity, lines);
            } else if (name.startsWith("cofh.thermal.")) {
                thermal(entity, lines);
            } else if (name.startsWith("blusunrize.immersiveengineering.")) {
                immersiveEngineering(entity, lines);
            } else if (name.startsWith("slimeknights.tconstruct.")) {
                tinkers(entity, lines);
            } else if (name.startsWith("com.jaquadro.minecraft.storagedrawers.")) {
                storageDrawers(entity, lines);
            } else if (name.startsWith("net.p3pp3rf1y.sophisticatedstorage.")) {
                sophisticatedStorage(entity, lines);
            } else if (extendsClass(entity, "com.hrznstudio.titanium.block.tile.ActiveTile")) {
                titanium(entity, lines);
            } else if (name.startsWith("com.refinedmods.refinedstorage.")) {
                refinedStorage(entity, lines);
            } else if (name.startsWith("com.hollingsworth.arsnouveau.")) {
                arsNouveau(entity, lines);
            } else if (name.startsWith("wayoftime.bloodmagic.")) {
                bloodMagic(entity, lines);
            } else if (name.startsWith("techreborn.")) {
                techReborn(entity, lines);
            } else if (name.startsWith("owmii.powah.")) {
                powah(entity, lines);
            } else if (name.startsWith("me.desht.pneumaticcraft.")) {
                pneumaticCraft(entity, lines);
            } else if (name.startsWith("com.blakebr0.mysticalagriculture.")) {
                mysticalAgriculture(entity, lines);
            } else if (name.startsWith("com.enderio.")) {
                enderIO(entity, lines);
            } else if (name.startsWith("com.klikli_dev.occultism.")) {
                occultism(entity, lines);
            } else if (name.startsWith("cy.jdkdigital.productivebees.")) {
                productiveBees(entity, lines);
            } else if (name.startsWith("com.lothrazar.cyclic.")) {
                cyclic(entity, lines);
            } else if (name.startsWith("org.cyclops.integrateddynamics.")) {
                integratedDynamics(entity, lines);
            } else if (name.startsWith("aztech.modern_industrialization.")) {
                modernIndustrialization(entity, lines);
            } else if (name.startsWith("rearth.oritech.")) {
                oritech(entity, lines);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return lines.isEmpty() ? null : lines;
        }
        return lines.isEmpty() ? null : lines;
    }

    private static void create(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        float speed = floatField(entity, "speed");
        lines.add(Texts.translatable("identify.create.speed", decimal(Math.abs(speed))));
        float capacity = floatField(entity, "capacity");
        float stress = floatField(entity, "stress");
        if (capacity > 0F || stress > 0F) {
            String key = boolField(entity, "overStressed") ? "identify.create.overstressed" : "identify.create.stress";
            lines.add(Texts.translatable(key, number(stress), number(capacity)));
        }
    }

    private static void mekanism(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getScaledProgress")) {
            lines.add(Texts.translatable("identify.progress", percent(((Number) call(entity, "getScaledProgress")).doubleValue(), 1D)));
        } else if (hasField(entity, "progress") && field(entity, "progress") instanceof int[] progress && hasMethod(entity, "getTicksRequired")) {
            long required = longCall(entity, "getTicksRequired");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < progress.length && i < 9; i++) {
                builder.append(i == 0 ? "" : " ").append(percent(progress[i], required));
            }
            if (builder.length() > 0) {
                lines.add(Texts.translatable("identify.progress", builder.toString()));
            }
        }
        int shown = 0;
        for (String method : new String[]{"getChemicalTanks", "getGasTanks", "getInfusionTanks", "getPigmentTanks", "getSlurryTanks"}) {
            if (hasMethod1(entity, method) && call1(entity, method, null) instanceof List<?> tanks) {
                for (Object tank : tanks) {
                    if (shown++ >= 3) {
                        break;
                    }
                    try {
                        chemical(tank, lines);
                    } catch (ReflectiveOperationException | RuntimeException e) {
                        continue;
                    }
                }
            }
        }
        if (hasMethod(entity, "canHandleHeat") && isTrue(call(entity, "canHandleHeat")) && hasMethod(entity, "getTotalTemperature")) {
            double kelvin = ((Number) call(entity, "getTotalTemperature")).doubleValue();
            lines.add(Texts.translatable("identify.temperature", decimal(kelvin)));
        }
    }

    private static void ae2(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getMaxProcessingTime") && hasMethod(entity, "getProcessingTime")) {
            lines.add(Texts.translatable("identify.progress", percent(longCall(entity, "getProcessingTime"), longCall(entity, "getMaxProcessingTime"))));
        }
        if (hasMethod(entity, "getAEMaxPower")) {
            double max = ((Number) call(entity, "getAEMaxPower")).doubleValue();
            if (max > 0D) {
                double stored = ((Number) call(entity, "getAECurrentPower")).doubleValue();
                lines.add(Texts.translatable("identify.ae2.power", number(stored), number(max)));
            }
        }
        if (!hasMethod(entity, "getMainNode")) {
            return;
        }
        Object managed = call(entity, "getMainNode");
        if (managed == null) {
            return;
        }
        if (!isTrue(call(managed, "isOnline"))) {
            lines.add(Texts.translatable(isTrue(call(managed, "isPowered")) ? "identify.ae2.no_channel" : "identify.ae2.offline"));
            return;
        }
        Object node = call(managed, "getNode");
        if (node != null) {
            long max = longCall(node, "getMaxChannels");
            if (max > 0L) {
                lines.add(Texts.translatable("identify.ae2.channels", BlockDetails.number(longCall(node, "getUsedChannels")), BlockDetails.number(max)));
            }
        }
    }

    private static void farmersDelight(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasField(entity, "cookTimeTotal")) {
            progress(longField(entity, "cookTime"), longField(entity, "cookTimeTotal"), lines);
        } else if (hasField(entity, "cookingTimeTotal")) {
            progress(longField(entity, "cookingTime"), longField(entity, "cookingTimeTotal"), lines);
        } else if (hasField(entity, "cookingProgress") && field(entity, "cookingProgress") instanceof int[] done
                && field(entity, "cookingTime") instanceof int[] total) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < done.length && i < total.length; i++) {
                if (total[i] > 0) {
                    builder.append(builder.length() == 0 ? "" : " ").append(percent(done[i], total[i]));
                }
            }
            if (builder.length() > 0) {
                lines.add(Texts.translatable("identify.progress", builder.toString()));
            }
        }
        if (hasMethod(entity, "isHeated")) {
            lines.add(Texts.translatable(isTrue(call(entity, "isHeated")) ? "identify.heated" : "identify.not_heated"));
        }
    }

    private static void botania(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        long max = hasMethod(entity, "getMaxMana") ? longCall(entity, "getMaxMana") : 0L;
        if (max <= 0L) {
            return;
        }
        long current = hasMethod(entity, "getCurrentMana") ? longCall(entity, "getCurrentMana")
                : hasMethod(entity, "getMana") ? longCall(entity, "getMana") : -1L;
        if (current >= 0L) {
            lines.add(Texts.translatable("identify.botania.mana", BlockDetails.number(current), BlockDetails.number(max)));
        }
    }

    private static void thermal(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasField(entity, "processMax")) {
            long max = longField(entity, "processMax");
            long left = longField(entity, "process");
            if (max > 0L && left > 0L) {
                progress(max - left, max, lines);
            }
        }
        if (hasField(entity, "fuelMax")) {
            long max = longField(entity, "fuelMax");
            if (max > 0L) {
                lines.add(Texts.translatable("identify.thermal.fuel", percent(longField(entity, "fuel"), max)));
            }
        }
    }

    private static void immersiveEngineering(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        Object target = entity;
        if (hasMethod(entity, "master")) {
            Object master = call(entity, "master");
            if (master != null) {
                target = master;
            }
        }
        if (hasMethod(target, "getCurrentProcessesStep") && call(target, "getCurrentProcessesStep") instanceof int[] step
                && call(target, "getCurrentProcessesMax") instanceof int[] max) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < step.length && i < max.length && i < 9; i++) {
                if (max[i] > 0) {
                    builder.append(builder.length() == 0 ? "" : " ").append(percent(step[i], max[i]));
                }
            }
            if (builder.length() > 0) {
                lines.add(Texts.translatable("identify.progress", builder.toString()));
            }
        } else if (hasMethod(target, "getSampleProgress")) {
            lines.add(Texts.translatable("identify.progress", percent(((Number) call(target, "getSampleProgress")).doubleValue(), 1D)));
        } else if (hasMethod(target, "getGuiProgress")) {
            lines.add(Texts.translatable("identify.progress", percent(((Number) call(target, "getGuiProgress")).doubleValue(), 1D)));
        }
        if (hasMethod(target, "isBlocked") && isTrue(call(target, "isBlocked"))) {
            lines.add(Texts.translatable("identify.ie.blocked"));
        }
    }

    private static void tinkers(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getCoolingTime")) {
            long cooling = longCall(entity, "getCoolingTime");
            if (cooling > 0L) {
                progress(longCall(entity, "getTimer"), cooling, lines);
            }
            return;
        }
        Object inventory = hasMethod(entity, "getMeltingInventory") ? call(entity, "getMeltingInventory")
                : hasField(entity, "meltingInventory") ? field(entity, "meltingInventory") : null;
        if (inventory != null && hasMethod(inventory, "getSlots")) {
            long slots = longCall(inventory, "getSlots");
            if (!hasMethod1(inventory, "getCurrentTime") || !hasMethod1(inventory, "getRequiredTime")) {
                return;
            }
            StringBuilder builder = new StringBuilder();
            int shown = 0;
            for (int i = 0; i < slots && shown < 9; i++) {
                long total = ((Number) call1(inventory, "getRequiredTime", i)).longValue();
                if (total > 0L) {
                    builder.append(shown++ == 0 ? "" : " ").append(percent(((Number) call1(inventory, "getCurrentTime", i)).doubleValue(), total));
                }
            }
            if (builder.length() > 0) {
                lines.add(Texts.translatable("identify.progress", builder.toString()));
            }
        }
        if (hasMethod(entity, "getFuelModule")) {
            Object fuel = call(entity, "getFuelModule");
            if (fuel != null) {
                long temperature = hasMethod(fuel, "hasFuel") && isTrue(call(fuel, "hasFuel")) ? longCall(fuel, "getTemperature") : 0L;
                lines.add(temperature > 0L ? Texts.translatable("identify.tinkers.temperature", BlockDetails.number(temperature))
                        : Texts.translatable("identify.tinkers.no_fuel"));
            }
        }
    }

    private static void sophisticatedStorage(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (!hasMethod(entity, "getStorageWrapper")) {
            return;
        }
        Object wrapper = call(entity, "getStorageWrapper");
        Object handler = wrapper == null ? null : call(wrapper, "getInventoryHandler");
        if (handler == null) {
            return;
        }
        int slots = (int) longCall(handler, "getSlots");
        int used = 0;
        for (int i = 0; i < slots; i++) {
            if (call1(handler, "getStackInSlot", i) instanceof ItemStack stack && !stack.isEmpty()) {
                used++;
            }
        }
        if (slots > 0) {
            lines.add(Texts.translatable("identify.storage.slots", BlockDetails.number(used), BlockDetails.number(slots)));
        }
        if (hasMethod(handler, "getStackSizeMultiplier")) {
            double multiplier = ((Number) call(handler, "getStackSizeMultiplier")).doubleValue();
            if (multiplier > 1.0D) {
                lines.add(Texts.translatable("identify.storage.multiplier", number(multiplier)));
            }
        }
        if (hasMethod(entity, "isLocked") && isTrue(call(entity, "isLocked"))) {
            lines.add(Texts.translatable("identify.storage.locked"));
        }
    }

    private static void titanium(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        for (String bar : new String[]{"progressBar", "workingBar"}) {
            if (hasField(entity, bar)) {
                Object component = field(entity, bar);
                if (component != null) {
                    progress(longCall(component, "getProgress"), longCall(component, "getMaxProgress"), lines);
                    return;
                }
            }
        }
    }

    private static void refinedStorage(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (!hasField(entity, "mainNetworkNode")) {
            return;
        }
        Object node = field(entity, "mainNetworkNode");
        if (node == null) {
            return;
        }
        if (hasMethod(node, "isActive")) {
            lines.add(Texts.translatable(isTrue(call(node, "isActive")) ? "identify.rs.active" : "identify.rs.inactive"));
        }
        if (hasMethod(node, "getStored") && hasMethod(node, "getCapacity") && !hasMethod(node, "getActualStored")) {
            long capacity = longCall(node, "getCapacity");
            String stored = BlockDetails.number(longCall(node, "getStored"));
            lines.add(capacity > 0L ? Texts.translatable("identify.rs.stored", stored, BlockDetails.number(capacity))
                    : Texts.translatable("identify.rs.stored.infinite", stored));
        }
        if (hasMethod(node, "getEnergyUsage")) {
            long usage = longCall(node, "getEnergyUsage");
            if (usage > 0L) {
                lines.add(Texts.translatable("identify.rs.usage", BlockDetails.number(usage)));
            }
        }
    }

    private static void arsNouveau(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (!hasMethod(entity, "getSource") || !hasMethod(entity, "getMaxSource")) {
            return;
        }
        long max = longCall(entity, "getMaxSource");
        if (max > 0L) {
            lines.add(Texts.translatable("identify.ars.source", BlockDetails.number(longCall(entity, "getSource")),
                    BlockDetails.number(max)));
        }
    }

    private static void bloodMagic(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getCurrentBlood") && hasMethod(entity, "getCapacity")) {
            lines.add(Texts.translatable("identify.bloodmagic.blood", BlockDetails.number(longCall(entity, "getCurrentBlood")),
                    BlockDetails.number(longCall(entity, "getCapacity"))));
            if (hasMethod(entity, "getTier")) {
                lines.add(Texts.translatable("identify.bloodmagic.tier", BlockDetails.number(longCall(entity, "getTier"))));
            }
            if (hasMethod(entity, "isActive") && isTrue(call(entity, "isActive")) && hasMethod(entity, "getLiquidRequired")) {
                progress(longCall(entity, "getProgress"), longCall(entity, "getLiquidRequired"), lines);
            }
        } else if (hasMethod(entity, "getProgressForGui")) {
            double done = ((Number) call(entity, "getProgressForGui")).doubleValue();
            if (done > 0D) {
                lines.add(Texts.translatable("identify.progress", percent(done, 1D)));
            }
        }
    }

    private static void techReborn(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getRecipeCrafter")) {
            Object crafter = call(entity, "getRecipeCrafter");
            if (crafter != null && longField(crafter, "currentTickTime") > 0L) {
                progress(longField(crafter, "currentTickTime"), longField(crafter, "currentNeededTicks"), lines);
            }
        } else if (hasMethod(entity, "getTotalBurnTime")) {
            long total = longCall(entity, "getTotalBurnTime");
            if (total > 0L && longCall(entity, "getBurnTime") > 0L) {
                lines.add(Texts.translatable("identify.fuel", percent(longCall(entity, "getBurnTime"), total)));
            }
        }
    }

    private static void powah(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasField(entity, "fuel") && hasField(entity, "temp") && hasMethod(entity, "isBuilt")) {
            if (isTrue(call(entity, "isBuilt"))) {
                powahTicker(field(entity, "fuel"), lines);
                double temp = ((Number) call(field(entity, "temp"), "getTicks")).doubleValue();
                lines.add(Texts.translatable("identify.powah.temperature", number(temp)));
            }
        } else if (hasMethod(entity, "getCarbon")) {
            powahTicker(call(entity, "getCarbon"), lines);
        } else if (hasField(entity, "generating")) {
            long generating = longField(entity, "generating");
            if (generating > 0L) {
                lines.add(Texts.translatable("identify.powah.generating", BlockDetails.number(generating)));
            }
        }
    }

    private static void powahTicker(Object ticker, List<Component> lines) throws ReflectiveOperationException {
        if (ticker == null) {
            return;
        }
        double max = ((Number) call(ticker, "getMax")).doubleValue();
        double ticks = ((Number) call(ticker, "getTicks")).doubleValue();
        if (max > 0.0D && ticks > 0.0D) {
            lines.add(Texts.translatable("identify.fuel", percent(ticks, max)));
        }
    }

    private static void pneumaticCraft(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getPressure") && hasMethod(entity, "getDangerPressure")) {
            float pressure = ((Number) call(entity, "getPressure")).floatValue();
            float danger = ((Number) call(entity, "getDangerPressure")).floatValue();
            lines.add(Texts.translatable("identify.pneumaticcraft.pressure", decimal(pressure), decimal(danger)));
        }
        if (hasMethod(entity, "getHeatExchanger")) {
            Object heat = call(entity, "getHeatExchanger");
            if (heat != null && hasMethod(heat, "getTemperature")) {
                double kelvin = ((Number) call(heat, "getTemperature")).doubleValue();
                lines.add(Texts.translatable("identify.pneumaticcraft.temperature", number(kelvin - 273.0D)));
            }
        }
    }

    private static void mysticalAgriculture(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getProgress") && hasMethod(entity, "getOperationTime")) {
            progress(longCall(entity, "getProgress"), longCall(entity, "getOperationTime"), lines);
        } else if (hasField(entity, "progress") && hasMethod(entity, "isActive") && isTrue(call(entity, "isActive"))) {
            progress(longField(entity, "progress"), 100L, lines);
        }
        if (hasMethod(entity, "getFuelLeft") && hasMethod(entity, "getFuelItemValue")) {
            long left = longCall(entity, "getFuelLeft");
            long value = longCall(entity, "getFuelItemValue");
            if (left > 0 && value > 0) {
                lines.add(Texts.translatable("identify.fuel", percent(left, value)));
            }
        }
    }

    private static void enderIO(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        for (String getter : new String[]{"getCraftingProgress", "getSpawnProgress", "getChargeProgress"}) {
            if (hasMethod(entity, getter)) {
                double done = ((Number) call(entity, getter)).doubleValue();
                if (done > 0D) {
                    lines.add(Texts.translatable("identify.progress", percent(done, 1D)));
                }
                break;
            }
        }
        if (hasMethod(entity, "getBurnProgress")) {
            double left = ((Number) call(entity, "getBurnProgress")).doubleValue();
            if (left > 0D) {
                lines.add(Texts.translatable("identify.fuel", percent(left, 1D)));
            }
        }
        if (hasMethod(entity, "getGenerationRate") && (!hasMethod(entity, "isGenerating") || Boolean.TRUE.equals(call(entity, "isGenerating")))) {
            double rate = ((Number) call(entity, "getGenerationRate")).doubleValue();
            if (rate > 0D) {
                lines.add(Texts.translatable("identify.generating", number(rate)));
            }
        }
    }

    private static void occultism(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasField(entity, "maxItemTypes") && hasField(entity, "usedTotalItemCount")) {
            lines.add(Texts.translatable("identify.occultism.types",
                    BlockDetails.number(longField(entity, "usedItemTypes")), BlockDetails.number(longField(entity, "maxItemTypes"))));
            lines.add(Texts.translatable("identify.rs.stored",
                    BlockDetails.number(longField(entity, "usedTotalItemCount")), BlockDetails.number(longField(entity, "maxTotalItemCount"))));
        }
        if (hasField(entity, "maxMiningTime")) {
            progress(longField(entity, "miningTime"), longField(entity, "maxMiningTime"), lines);
        }
        if (hasField(entity, "ritualActive") && boolField(entity, "ritualActive")) {
            lines.add(Texts.translatable("identify.occultism.ritual"));
        }
    }

    private static void oritech(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasField(entity, "currentMaxBurnTime")) {
            long max = longField(entity, "currentMaxBurnTime");
            long done = longField(entity, "progress");
            if (max > 0 && done < max) {
                lines.add(Texts.translatable("identify.fuel", percent(max - done, max)));
            }
        } else if (hasMethod(entity, "getCurrentRecipe") && hasField(entity, "progress")) {
            Object recipe = call(entity, "getCurrentRecipe");
            if (recipe != null && hasMethod(recipe, "getTime")) {
                double total = longCall(recipe, "getTime") * ((Number) call(entity, "getSpeedMultiplier")).doubleValue();
                long done = longField(entity, "progress");
                if (done > 0 && total > 0) {
                    progress(done, (long) Math.ceil(total), lines);
                }
            }
        }
    }

    private static void modernIndustrialization(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getCrafterComponent")) {
            Object crafter = call(entity, "getCrafterComponent");
            if (crafter != null && Boolean.TRUE.equals(call(crafter, "hasActiveRecipe"))) {
                progress((long) (((Number) call(crafter, "getProgress")).floatValue() * 1000), 1000L, lines);
                lines.add(Texts.translatable("identify.mi.eu", BlockDetails.number(longCall(crafter, "getCurrentRecipeEu"))));
                long max = longCall(crafter, "getMaxEfficiencyTicks");
                if (max > 0) {
                    lines.add(Texts.translatable("identify.mi.efficiency", BlockDetails.number(longCall(crafter, "getEfficiencyTicks")), BlockDetails.number(max)));
                }
            }
        }
        if (hasField(entity, "steamHeater")) {
            Object heater = field(entity, "steamHeater");
            if (heater != null) {
                double temperature = ((Number) call(heater, "getTemperature")).doubleValue();
                double max = ((Number) field(heater, "temperatureMax")).doubleValue();
                lines.add(Texts.translatable("identify.mi.temperature", decimal(temperature), decimal(max)));
            }
        }
        if (hasField(entity, "fuelBurning")) {
            Object burning = field(entity, "fuelBurning");
            if (burning != null && Boolean.TRUE.equals(call(burning, "isBurning"))) {
                double left = ((Number) call(burning, "getBurningProgress")).doubleValue();
                lines.add(Texts.translatable("identify.fuel", percent((long) (left * 1000), 1000L)));
            }
        }
        if (hasMethod(entity, "getEnergyComponent")) {
            Object energy = call(entity, "getEnergyComponent");
            if (energy != null) {
                lines.add(Texts.translatable("identify.mi.energy", BlockDetails.number(longCall(energy, "getEu")), BlockDetails.number(longCall(energy, "getCapacity"))));
            }
        }
    }

    private static void integratedDynamics(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasField(entity, "currentlyBurning") && hasField(entity, "currentlyBurningMax")) {
            long burned = longField(entity, "currentlyBurning");
            long max = longField(entity, "currentlyBurningMax");
            if (max > 0 && burned < max) {
                lines.add(Texts.translatable("identify.fuel", percent(max - burned, max)));
            }
        } else if (hasMethod(entity, "getMaxProgress") && hasMethod(entity, "getProgress")) {
            progress(longCall(entity, "getProgress"), longCall(entity, "getMaxProgress"), lines);
        }
    }

    private static void cyclic(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (!hasField(entity, "burnTimeMax")) {
            return;
        }
        long max = longField(entity, "burnTimeMax");
        if (hasField(entity, "burnTime")) {
            long left = longField(entity, "burnTime");
            if (left > 0 && max > 0) {
                if (entity.getClass().getName().startsWith("com.lothrazar.cyclic.block.generator")) {
                    lines.add(Texts.translatable("identify.fuel", percent(left, max)));
                } else {
                    progress(max - left, max, lines);
                }
            }
        } else if (hasField(entity, "timer")) {
            long left = longField(entity, "timer");
            if (left > 0 && left <= max) {
                progress(max - left, max, lines);
            }
        }
    }

    private static void productiveBees(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (hasMethod(entity, "getRecipeProgress") && hasMethod(entity, "getCurrentRecipe") && hasMethod1(entity, "getProcessingTime")) {
            long done = longCall(entity, "getRecipeProgress");
            Object recipe = call(entity, "getCurrentRecipe");
            if (done > 0 && recipe != null) {
                progress(done, ((Number) call1(entity, "getProcessingTime", recipe)).longValue(), lines);
            }
        }
        if (hasMethod(entity, "isSedated") && Boolean.TRUE.equals(call(entity, "isSedated"))) {
            lines.add(Texts.translatable("identify.productivebees.sedated"));
        }
    }

    private static void storageDrawers(BlockEntity entity, List<Component> lines) throws ReflectiveOperationException {
        if (!hasMethod(entity, "getDrawerCount") || !hasMethod1(entity, "getDrawer")) {
            return;
        }
        long count = longCall(entity, "getDrawerCount");
        boolean any = false;
        int shown = 0;
        for (int i = 0; i < count; i++) {
            Object drawer = call1(entity, "getDrawer", i);
            if (drawer == null || !isTrue(call(drawer, "isEnabled")) || isTrue(call(drawer, "isEmpty"))) {
                continue;
            }
            any = true;
            if (shown++ >= 4 || !(call(drawer, "getStoredItemPrototype") instanceof ItemStack stack)) {
                continue;
            }
            lines.add(Texts.translatable("identify.drawers.slot", stack.getHoverName().copy(),
                    BlockDetails.number(longCall(drawer, "getStoredItemCount")), BlockDetails.number(longCall(drawer, "getMaxCapacity"))));
        }
        if (!any && count > 0L) {
            lines.add(Texts.translatable("identify.drawers.empty"));
        }
    }

    private static void progress(long done, long total, List<Component> lines) {
        if (total > 0L) {
            lines.add(Texts.translatable("identify.progress", percent(done, total)));
        }
    }

    private static void chemical(Object tank, List<Component> lines) throws ReflectiveOperationException {
        long capacity = longCall(tank, "getCapacity");
        if (capacity <= 0L) {
            return;
        }
        Object stack = call(tank, "getStack");
        if (stack == null) {
            return;
        }
        if (isTrue(call(stack, "isEmpty"))) {
            lines.add(Texts.translatable("identify.chemical.empty", BlockDetails.number(capacity)));
        } else {
            if (!(call(stack, "getTextComponent") instanceof Component text)) {
                return;
            }
            Component label = text.copy();
            lines.add(Texts.translatable("identify.chemical", label, BlockDetails.number(longCall(stack, "getAmount")), BlockDetails.number(capacity)));
        }
    }

    static boolean extendsClass(Object object, String... names) {
        for (Class<?> type = object.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            for (String name : names) {
                if (type.getName().equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean implementsClass(Object object, String name) {
        for (Class<?> type = object.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
            if (hasInterface(type, name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasInterface(Class<?> type, String name) {
        for (Class<?> face : type.getInterfaces()) {
            if (face.getName().equals(name) || hasInterface(face, name)) {
                return true;
            }
        }
        return false;
    }

    static Object field(Object object, String name) throws ReflectiveOperationException {
        String key = object.getClass().getName() + "#" + name;
        Object member = MEMBERS.computeIfAbsent(key, k -> findField(object.getClass(), name));
        if (member == MISSING) {
            throw new NoSuchFieldException(name);
        }
        return ((Field) member).get(object);
    }

    static boolean hasField(Object object, String name) {
        String key = object.getClass().getName() + "#" + name;
        return MEMBERS.computeIfAbsent(key, k -> findField(object.getClass(), name)) != MISSING;
    }

    static Object call(Object object, String name) throws ReflectiveOperationException {
        String key = object.getClass().getName() + "." + name;
        Object member = MEMBERS.computeIfAbsent(key, k -> findMethod(object.getClass(), name));
        if (member == MISSING) {
            throw new NoSuchMethodException(name);
        }
        return ((Method) member).invoke(object);
    }

    static boolean hasMethod(Object object, String name) {
        String key = object.getClass().getName() + "." + name;
        return MEMBERS.computeIfAbsent(key, k -> findMethod(object.getClass(), name)) != MISSING;
    }

    static Object call1(Object object, String name, Object arg) throws ReflectiveOperationException {
        String key = object.getClass().getName() + "." + name + "/1";
        Object member = MEMBERS.computeIfAbsent(key, k -> findMethod1(object.getClass(), name));
        if (member == MISSING) {
            throw new NoSuchMethodException(name);
        }
        return ((Method) member).invoke(object, arg);
    }

    static boolean hasMethod1(Object object, String name) {
        String key = object.getClass().getName() + "." + name + "/1";
        return MEMBERS.computeIfAbsent(key, k -> findMethod1(object.getClass(), name)) != MISSING;
    }

    private static Object findMethod1(Class<?> type, String name) {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                return method;
            }
        }
        return MISSING;
    }

    private static Object findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException | RuntimeException e) {
                continue;
            }
        }
        return MISSING;
    }

    private static Object findMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    try {
                        method.setAccessible(true);
                        return method;
                    } catch (RuntimeException e) {
                        return MISSING;
                    }
                }
            }
        }
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 0) {
                return method;
            }
        }
        return MISSING;
    }

    static float floatField(Object object, String name) throws ReflectiveOperationException {
        return ((Number) field(object, name)).floatValue();
    }

    static long longField(Object object, String name) throws ReflectiveOperationException {
        return ((Number) field(object, name)).longValue();
    }

    static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value);
    }

    static boolean boolField(Object object, String name) throws ReflectiveOperationException {
        return Boolean.TRUE.equals(field(object, name));
    }

    static long longCall(Object object, String name) throws ReflectiveOperationException {
        return ((Number) call(object, name)).longValue();
    }

    static String number(double value) {
        return BlockDetails.number(Math.round(value));
    }

    static String decimal(double value) {
        if (Math.abs(value) >= 100_000D || Math.abs(value - Math.rint(value)) < 0.05D) {
            return BlockDetails.number(Math.round(value));
        }
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    static String percent(double done, double total) {
        if (total <= 0D) {
            return "0%";
        }
        return Math.round(Math.max(0D, Math.min(1D, done / total)) * 100D) + "%";
    }
}
