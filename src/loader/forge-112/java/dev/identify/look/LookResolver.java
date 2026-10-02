package dev.identify.look;

import dev.identify.config.IdentifyConfig;
import dev.identify.info.ModNames;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;

import java.util.Collections;
import java.util.List;

public final class LookResolver {
    private LookResolver() {
    }

    public static LookTarget resolve(Minecraft mc, float partialTick, IdentifyConfig config) {
        EntityPlayerSP player = mc.player;
        WorldClient world = mc.world;
        if (player == null || world == null) {
            return null;
        }

        double range = config.range();
        Vec3d eye = player.getPositionEyes(partialTick);
        Vec3d look = player.getLook(partialTick);
        Vec3d reach = look.scale(range);
        Vec3d end = eye.add(reach);

        RayTraceResult blockHit = world.rayTraceBlocks(eye, end, false, false, false);
        boolean hitBlock = blockHit != null && blockHit.typeOfHit == RayTraceResult.Type.BLOCK;
        double blockDistanceSq = hitBlock ? eye.squareDistanceTo(blockHit.hitVec) : range * range;

        if (config.showEntities()) {
            Entity entity = pickEntity(world, player, eye, end, reach, blockDistanceSq);
            if (entity != null) {
                return describeEntity(mc, entity, config);
            }
        }

        if (config.showBlocks() && hitBlock) {
            return describeBlock(world, player, blockHit, config);
        }
        return null;
    }

    private static Entity pickEntity(
            WorldClient world, EntityPlayerSP player, Vec3d eye, Vec3d end, Vec3d reach, double maxDistanceSq) {
        AxisAlignedBB search = player.getEntityBoundingBox().expand(reach.x, reach.y, reach.z).grow(1.0D);
        List<Entity> candidates = world.getEntitiesInAABBexcluding(player, search, entity -> entity != null
                && entity.canBeCollidedWith()
                && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator()));

        Entity best = null;
        double bestDistanceSq = maxDistanceSq;
        for (Entity entity : candidates) {
            if (entity.getLowestRidingEntity() == player.getLowestRidingEntity() && !entity.canRiderInteract()) {
                continue;
            }
            AxisAlignedBB box = entity.getEntityBoundingBox().grow(entity.getCollisionBorderSize());
            double distanceSq;
            if (box.contains(eye)) {
                distanceSq = 0.0D;
            } else {
                RayTraceResult hit = box.calculateIntercept(eye, end);
                if (hit == null) {
                    continue;
                }
                distanceSq = eye.squareDistanceTo(hit.hitVec);
            }
            if (distanceSq < bestDistanceSq) {
                best = entity;
                bestDistanceSq = distanceSq;
            }
        }
        return best;
    }

    private static LookTarget describeBlock(
            WorldClient world, EntityPlayerSP player, RayTraceResult hit, IdentifyConfig config) {
        BlockPos pos = hit.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (block.isAir(state, world, pos)) {
            return null;
        }
        ItemStack icon = block.getPickBlock(state, hit, world, pos, player);
        if (icon == null) {
            icon = ItemStack.EMPTY;
        }
        Item item = icon.getItem();
        boolean sameBlock = item instanceof ItemBlock && ((ItemBlock) item).getBlock() == block;
        String name = sameBlock ? icon.getDisplayName() : block.getLocalizedName();
        ResourceLocation id = block.getRegistryName();
        String namespace = id == null ? "" : id.getNamespace();
        List<String> details = config.showDetails()
                ? BlockDetails.of(world, pos, state)
                : Collections.<String>emptyList();
        return new LookTarget(name, icon, namespace, details);
    }

    private static LookTarget describeEntity(Minecraft mc, Entity entity, IdentifyConfig config) {
        ItemStack pick = entity.getPickedResult(new RayTraceResult(entity));
        ItemStack icon = pick == null ? ItemStack.EMPTY : pick;
        ResourceLocation id = EntityList.getKey(entity);
        String namespace = id == null ? "minecraft" : id.getNamespace();
        List<String> details = config.showDetails()
                ? EntityDetails.of(mc, entity)
                : Collections.<String>emptyList();
        return new LookTarget(entity.getDisplayName().getFormattedText(), icon, namespace, details);
    }

    public static String modLabel(LookTarget target) {
        return ModNames.pretty(target.namespace());
    }
}
