package dev.identify.look;

import dev.identify.config.IdentifyConfig;
import dev.identify.info.ModNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class LookResolver {
    private LookResolver() {
    }

    public static LookTarget resolve(Minecraft mc, float partialTick, IdentifyConfig config) {
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return null;
        }

        double range = config.range();
        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blockHit = level.clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        boolean hitBlock = blockHit.getType() == HitResult.Type.BLOCK;
        double blockDistanceSq = hitBlock ? eye.distanceToSqr(blockHit.getLocation()) : range * range;

        if (config.showEntities()) {
            AABB box = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                    player, eye, end, box, entity -> !entity.isSpectator() && entity.isPickable(), blockDistanceSq);
            if (entityHit != null) {
                return describeEntity(mc, entityHit.getEntity(), config);
            }
        }

        if (config.showBlocks() && hitBlock) {
            return describeBlock(level, blockHit.getBlockPos(), config);
        }
        return null;
    }

    private static LookTarget describeBlock(ClientLevel level, BlockPos pos, IdentifyConfig config) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        Item item = state.getBlock().asItem();
        ItemStack icon = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        String namespace = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace();
        List<Component> details = config.showDetails() ? BlockDetails.of(state) : List.of();
        return new LookTarget(state.getBlock().getName(), icon, namespace, details);
    }

    private static LookTarget describeEntity(Minecraft mc, Entity entity, IdentifyConfig config) {
        ItemStack pick = entity.getPickResult();
        ItemStack icon = pick == null ? ItemStack.EMPTY : pick;
        String namespace = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace();
        List<Component> details = config.showDetails() ? EntityDetails.of(mc, entity) : List.of();
        return new LookTarget(entity.getDisplayName(), icon, namespace, details);
    }

    public static String modLabel(LookTarget target) {
        return ModNames.pretty(target.namespace());
    }
}
