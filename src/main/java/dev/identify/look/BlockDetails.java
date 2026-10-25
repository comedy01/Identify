package dev.identify.look;

import dev.identify.Texts;
import dev.identify.info.TickTime;
import net.minecraft.core.BlockPos;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ComparatorMode;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class BlockDetails {
    private static final int MAX_LINES = 4;
    private static final int MAX_FLUID_LINES = 2;
    private static final int BEACON_RANGE_BASE = 10;
    private static final int BEACON_RANGE_PER_TIER = 10;
    private static final int COMPOSTER_READY = 8;
    private static final int COMPOSTER_MAX = 7;
    private static final int REPEATER_MAX = 4;
    private static final int NOTE_MAX = 24;
    private static final int ANCHOR_MAX = 4;
    private static final int BOTTLE_SLOTS = 3;
    private static final int BLAZE_FUEL_MAX = 20;
    private static final String[] NOTE_NAMES = {"F#", "G", "G#", "A", "A#", "B", "C", "C#", "D", "D#", "E", "F"};

    private BlockDetails() {
    }

    public static List<Component> of(BlockState state) {
        return capped(stateLines(state), toolLine(state));
    }

    public static List<Component> of(Level level, BlockPos pos, BlockState state, boolean bestTool) {
        List<Component> lines = stateLines(state);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity != null) {
            entityLines(level, pos, state, entity, lines);
        }
        return capped(lines, bestTool ? toolLine(state) : null);
    }

    private static List<Component> capped(List<Component> lines, Component tool) {
        int limit = MAX_LINES - (tool != null ? 1 : 0);
        List<Component> result = new ArrayList<>(lines.subList(0, Math.min(limit, lines.size())));
        addIfPresent(result, tool);
        return result;
    }

    private static List<Component> stateLines(BlockState state) {
        List<Component> lines = new ArrayList<>(4);

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

        if (state.hasProperty(BlockStateProperties.LEVEL_COMPOSTER)) {
            int level = state.getValue(BlockStateProperties.LEVEL_COMPOSTER);
            if (level >= COMPOSTER_READY) {
                lines.add(Texts.translatable("identify.composter.ready"));
            } else {
                lines.add(Texts.translatable("identify.composter.level", level, COMPOSTER_MAX));
            }
        }

        if (state.hasProperty(BlockStateProperties.LEVEL_CAULDRON)) {
            int max = Collections.max(BlockStateProperties.LEVEL_CAULDRON.getPossibleValues());
            lines.add(Texts.translatable("identify.cauldron.level", state.getValue(BlockStateProperties.LEVEL_CAULDRON), max));
        }

        if (state.hasProperty(BlockStateProperties.DELAY)) {
            lines.add(Texts.translatable("identify.repeater.delay", state.getValue(BlockStateProperties.DELAY), REPEATER_MAX));
        }

        if (state.hasProperty(BlockStateProperties.MODE_COMPARATOR)) {
            boolean subtract = state.getValue(BlockStateProperties.MODE_COMPARATOR) == ComparatorMode.SUBTRACT;
            lines.add(Texts.translatable(subtract ? "identify.comparator.subtract" : "identify.comparator.compare"));
        }

        if (state.hasProperty(BlockStateProperties.NOTE) && state.hasProperty(BlockStateProperties.NOTEBLOCK_INSTRUMENT)) {
            int note = state.getValue(BlockStateProperties.NOTE);
            String instrument = state.getValue(BlockStateProperties.NOTEBLOCK_INSTRUMENT).getSerializedName();
            lines.add(Texts.translatable("identify.note", NOTE_NAMES[note % NOTE_NAMES.length], note, NOTE_MAX,
                    instrumentName(instrument)));
        }

        if (state.hasProperty(BlockStateProperties.RESPAWN_ANCHOR_CHARGES)) {
            lines.add(Texts.translatable("identify.anchor.charges",
                    state.getValue(BlockStateProperties.RESPAWN_ANCHOR_CHARGES), ANCHOR_MAX));
        }

        if (state.hasProperty(BlockStateProperties.HAS_BOTTLE_0)) {
            int bottles = 0;
            if (state.getValue(BlockStateProperties.HAS_BOTTLE_0)) {
                bottles++;
            }
            if (state.getValue(BlockStateProperties.HAS_BOTTLE_1)) {
                bottles++;
            }
            if (state.getValue(BlockStateProperties.HAS_BOTTLE_2)) {
                bottles++;
            }
            lines.add(Texts.translatable("identify.brewing.bottles", bottles, BOTTLE_SLOTS));
        }
        return lines;
    }

    private static void entityLines(Level level, BlockPos pos, BlockState state, BlockEntity entity, List<Component> lines) {
        if (entity instanceof SpawnerBlockEntity spawner) {
            addIfPresent(lines, spawnerLine(level, pos, spawner));
            return;
        }
        ServerData data = ServerBlockData.get(level, pos, BlockDetails::readServer);
        if (entity instanceof BeaconBlockEntity beacon) {
            CompoundTag tag = data != null && data.beacon() != null ? data.beacon() : BlockEntityCompat.beaconTag(level, beacon);
            beaconLines(tag, lines);
        } else if (entity instanceof AbstractFurnaceBlockEntity) {
            furnaceLines(state, data, lines);
        } else if (entity instanceof BrewingStandBlockEntity) {
            brewingLines(data, lines);
        } else if (entity instanceof CampfireBlockEntity campfire) {
            campfireLines(campfire, data, lines);
        } else if (entity instanceof JukeboxBlockEntity jukebox) {
            jukeboxLine(state, jukebox, data, lines);
        } else if (entity instanceof LecternBlockEntity) {
            lecternLine(state, data, lines);
        }
        if (data == null) {
            return;
        }
        if (data.extra() != null) {
            lines.addAll(data.extra());
        }
        if (data.energy() != null) {
            lines.add(Texts.translatable("identify.energy", number(data.energy()[0]), number(data.energy()[1])));
        }
        if (data.fluids() != null) {
            fluidLines(data.fluids(), lines);
        }
    }

    private static void fluidLines(List<FluidTank> tanks, List<Component> lines) {
        long emptyShown = -1;
        int shown = 0;
        for (int i = 0; i < tanks.size() && shown < MAX_FLUID_LINES; i++) {
            FluidTank tank = tanks.get(i);
            if (tank.name() == null) {
                if (tank.capacity() == emptyShown) {
                    continue;
                }
                emptyShown = tank.capacity();
                shown++;
                lines.add(Texts.translatable("identify.fluid.empty", number(tank.capacity())));
            } else {
                shown++;
                lines.add(Texts.translatable("identify.fluid", tank.name(), number(tank.amount()), number(tank.capacity())));
            }
        }
    }

    private static ServerData readServer(BlockEntity entity) {
        CompoundTag beacon = null;
        int[] machine = null;
        ItemStack[] slots = null;
        int campfireLeft = -1;
        int page = -1;
        ItemStack disc = null;
        if (entity instanceof BeaconBlockEntity b && entity.getLevel() != null) {
            beacon = BlockEntityCompat.beaconTag(entity.getLevel(), b);
        } else if (entity instanceof AbstractFurnaceBlockEntity furnace) {
            machine = containerData(AbstractFurnaceBlockEntity.class, furnace);
            slots = slots(furnace);
        } else if (entity instanceof BrewingStandBlockEntity brewing) {
            machine = containerData(BrewingStandBlockEntity.class, brewing);
            slots = slots(brewing);
        } else if (entity instanceof CampfireBlockEntity campfire) {
            campfireLeft = campfireLeft(campfire);
        } else if (entity instanceof LecternBlockEntity lectern) {
            page = lectern.getBook().isEmpty() ? -1 : lectern.getPage();
        } else if (entity instanceof JukeboxBlockEntity jukebox) {
            disc = BlockEntityCompat.disc(jukebox).copy();
        }
        long[] energy = null;
        try {
            energy = EnergyCompat.read(entity);
        } catch (RuntimeException | LinkageError e) {
            energy = null;
        }
        List<FluidTank> fluids = null;
        try {
            fluids = FluidCompat.read(entity);
        } catch (RuntimeException | LinkageError e) {
            fluids = null;
        }
        List<Component> extra = null;
        try {
            extra = ModLines.read(entity);
        } catch (RuntimeException | LinkageError e) {
            extra = null;
        }
        if (beacon == null && machine == null && slots == null && campfireLeft < 0 && page < 0 && disc == null
                && energy == null && fluids == null && extra == null) {
            return null;
        }
        return new ServerData(beacon, machine, slots, campfireLeft, page, disc, energy, fluids, extra);
    }

    private static int[] containerData(Class<?> owner, Object target) {
        for (Field field : owner.getDeclaredFields()) {
            if (!ContainerData.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                ContainerData data = (ContainerData) field.get(target);
                if (data == null) {
                    return null;
                }
                int[] values = new int[data.getCount()];
                for (int i = 0; i < values.length; i++) {
                    values[i] = data.get(i);
                }
                return values;
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
        return null;
    }

    private static ItemStack[] slots(Container container) {
        ItemStack[] slots = new ItemStack[container.getContainerSize()];
        for (int i = 0; i < slots.length; i++) {
            slots[i] = container.getItem(i).copy();
        }
        return slots;
    }

    private static int campfireLeft(CampfireBlockEntity campfire) {
        List<int[]> arrays = new ArrayList<>(2);
        for (Field field : CampfireBlockEntity.class.getDeclaredFields()) {
            if (field.getType() != int[].class) {
                continue;
            }
            try {
                field.setAccessible(true);
                arrays.add((int[]) field.get(campfire));
            } catch (ReflectiveOperationException | RuntimeException e) {
                return -1;
            }
        }
        if (arrays.size() != 2 || arrays.get(0) == null || arrays.get(1) == null) {
            return -1;
        }
        int[] first = arrays.get(0);
        int[] second = arrays.get(1);
        int longest = -1;
        for (int i = 0; i < Math.min(first.length, second.length); i++) {
            if (first[i] == 0 && second[i] == 0 || campfire.getItems().get(i).isEmpty()) {
                continue;
            }
            int left = Math.max(first[i], second[i]) - Math.min(first[i], second[i]);
            longest = Math.max(longest, left);
        }
        return longest;
    }

    private static void furnaceLines(BlockState state, ServerData data, List<Component> lines) {
        int[] values = data == null ? null : data.machine();
        ItemStack[] slots = data == null ? null : data.slots();
        if (values == null || values.length < 4 || slots == null || slots.length < 3) {
            if (state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
                lines.add(Texts.translatable("identify.furnace.lit"));
            }
            return;
        }
        ItemStack input = slots[0];
        ItemStack fuel = slots[1];
        ItemStack result = slots[2];
        if (!input.isEmpty() && values[3] > 0) {
            lines.add(Texts.translatable("identify.furnace.cooking", input.getCount(), input.getHoverName(),
                    TickTime.percent(values[2], values[3])));
        }
        if (values[0] > 0) {
            if (fuel.isEmpty()) {
                lines.add(Texts.translatable("identify.furnace.burning", TickTime.clock(values[0])));
            } else {
                lines.add(Texts.translatable("identify.furnace.burning_fuel", TickTime.clock(values[0]),
                        fuel.getCount(), fuel.getHoverName()));
            }
        }
        if (!result.isEmpty()) {
            lines.add(Texts.translatable("identify.furnace.output", result.getCount(), result.getHoverName()));
        }
    }

    private static void brewingLines(ServerData data, List<Component> lines) {
        int[] values = data == null ? null : data.machine();
        ItemStack[] slots = data == null ? null : data.slots();
        if (values == null || values.length < 2 || slots == null || slots.length < 4) {
            return;
        }
        ItemStack ingredient = slots[3];
        if (!ingredient.isEmpty()) {
            if (values[0] > 0) {
                lines.add(Texts.translatable("identify.brewing.progress", ingredient.getHoverName(), TickTime.clock(values[0])));
            } else {
                lines.add(Texts.translatable("identify.brewing.ingredient", ingredient.getHoverName()));
            }
        }
        lines.add(Texts.translatable("identify.brewing.fuel", values[1], BLAZE_FUEL_MAX));
    }

    private static void campfireLines(CampfireBlockEntity campfire, ServerData data, List<Component> lines) {
        int total = campfire.getItems().size();
        int cooking = 0;
        for (ItemStack stack : campfire.getItems()) {
            if (!stack.isEmpty()) {
                cooking++;
            }
        }
        if (cooking == 0) {
            return;
        }
        if (data != null && data.campfireLeft() >= 0) {
            lines.add(Texts.translatable("identify.campfire.cooking_time", cooking, total, TickTime.clock(data.campfireLeft())));
        } else {
            lines.add(Texts.translatable("identify.campfire.cooking", cooking, total));
        }
    }

    private static void jukeboxLine(BlockState state, JukeboxBlockEntity jukebox, ServerData data, List<Component> lines) {
        ItemStack disc = data != null && data.disc() != null ? data.disc() : BlockEntityCompat.disc(jukebox);
        if (!disc.isEmpty()) {
            lines.add(Texts.translatable("identify.jukebox.playing", songName(disc)));
        } else if (state.hasProperty(BlockStateProperties.HAS_RECORD) && state.getValue(BlockStateProperties.HAS_RECORD)) {
            lines.add(Texts.translatable("identify.jukebox.has_disc"));
        }
    }

    private static void lecternLine(BlockState state, ServerData data, List<Component> lines) {
        if (data != null && data.page() >= 0) {
            lines.add(Texts.translatable("identify.lectern.page", data.page() + 1));
        } else if (state.hasProperty(BlockStateProperties.HAS_BOOK) && state.getValue(BlockStateProperties.HAS_BOOK)) {
            lines.add(Texts.translatable("identify.lectern.book"));
        }
    }

    private static Component songName(ItemStack disc) {
        String descriptionId = disc.getItem().getDescriptionId();
        Language language = Language.getInstance();
        if (language.has(descriptionId + ".desc")) {
            return Texts.translatable(descriptionId + ".desc");
        }
        if (descriptionId.startsWith("item.")) {
            String id = descriptionId.substring("item.".length());
            int dot = id.indexOf('.');
            if (dot > 0) {
                String path = id.substring(dot + 1);
                if (path.startsWith("music_disc_")) {
                    path = path.substring("music_disc_".length());
                }
                String key = "jukebox_song." + id.substring(0, dot) + "." + path;
                if (language.has(key)) {
                    return Texts.translatable(key);
                }
            }
        }
        return disc.getHoverName();
    }

    private static Component instrumentName(String instrument) {
        String key = "identify.instrument." + instrument;
        if (Language.getInstance().has(key)) {
            return Texts.translatable(key);
        }
        String spaced = instrument.replace('_', ' ');
        if (spaced.isEmpty()) {
            return Texts.literal(spaced);
        }
        return Texts.literal(spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1));
    }

    private static final String[] UNITS = {"K", "M", "B", "T", "Q"};

    static String number(long value) {
        if (Math.abs(value) < 100_000L) {
            return String.format(Locale.ROOT, "%,d", value);
        }
        double scaled = value;
        int unit = -1;
        while (unit < UNITS.length - 1 && Math.abs(Math.round(scaled * 10D) / 10D) >= 1000D) {
            scaled /= 1000D;
            unit++;
        }
        double rounded = Math.round(scaled * 10D) / 10D;
        String text = Math.abs(rounded) >= 100D || rounded == Math.rint(rounded)
                ? Long.toString(Math.round(rounded))
                : String.format(Locale.ROOT, "%.1f", rounded);
        return text + UNITS[unit];
    }

    private static Component spawnerLine(Level level, BlockPos pos, SpawnerBlockEntity spawner) {
        Entity display = BlockEntityCompat.spawnerEntity(level, pos, spawner);
        if (display == null) {
            return null;
        }
        return Texts.translatable("identify.spawner.mob", display.getType().getDescription());
    }

    private static void beaconLines(CompoundTag tag, List<Component> lines) {
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
        String tool = state.getDestroySpeed(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) == 0 ? null : HarvestTools.tool(state);
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

    private record ServerData(CompoundTag beacon, int[] machine, ItemStack[] slots, int campfireLeft, int page,
                              ItemStack disc, long[] energy, List<FluidTank> fluids, List<Component> extra) {
    }
}
