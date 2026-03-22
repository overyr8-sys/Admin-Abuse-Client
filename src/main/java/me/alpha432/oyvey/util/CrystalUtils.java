package me.alpha432.oyvey.util;

import me.alpha432.oyvey.util.traits.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Crystal utility — damage calculation and placement validation.
 * Place in: src/main/java/me/alpha432/oyvey/util/CrystalUtils.java
 */
public class CrystalUtils implements Util {

    /**
     * Calculates explosion damage from a crystal at (x, y, z) to a target entity.
     * Based on vanilla explosion damage formula.
     */
    public static float calculateDamage(Level world, double x, double y, double z, LivingEntity target, float amplifier) {
        double distance = target.position().distanceTo(new Vec3(x, y, z));
        double radius   = 12.0; // crystal explosion radius

        if (distance > radius) return 0f;

        // Exposure (ray cast through blocks)
        double exposure = getExposure(world, new Vec3(x, y, z), target);

        // Vanilla formula
        double impact   = (1.0 - (distance / radius)) * exposure;
        float  damage   = (float)((impact * impact + impact) / 2.0 * 7.0 * radius + 1.0);

        // Apply difficulty multiplier (approximation for hard mode)
        damage *= 1.5f;

        // Apply armor reduction (rough approximation)
        if (target instanceof Player p) {
            damage = applyArmorReduction(damage, p);
        }

        return Math.max(0, damage + amplifier);
    }

    /**
     * Returns how exposed (0.0 - 1.0) a target is to an explosion at origin.
     * Casts rays from explosion center to target bounding box corners.
     */
    private static double getExposure(Level world, Vec3 origin, LivingEntity target) {
        AABB box    = target.getBoundingBox();
        double xStep = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double yStep = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zStep = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);

        if (xStep < 0 || yStep < 0 || zStep < 0) return 0.0;

        int hit   = 0;
        int total = 0;

        for (double dx = 0; dx <= 1.0; dx += xStep) {
            for (double dy = 0; dy <= 1.0; dy += yStep) {
                for (double dz = 0; dz <= 1.0; dz += zStep) {
                    double px = box.minX + (box.maxX - box.minX) * dx;
                    double py = box.minY + (box.maxY - box.minY) * dy;
                    double pz = box.minZ + (box.maxZ - box.minZ) * dz;

                    Vec3 rayTarget = new Vec3(px, py, pz);
                    ClipContext clipCtx = new ClipContext(
                            origin, rayTarget,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            net.minecraft.world.phys.shapes.CollisionContext.empty()
                    );
                    boolean blocked = world.clip(clipCtx).getType() != net.minecraft.world.phys.HitResult.Type.MISS;
                    if (!blocked) {
                        hit++;
                    }
                    total++;
                }
            }
        }

        return total == 0 ? 0.0 : (double) hit / total;
    }

    /** Simple armor reduction approximation */
    private static float applyArmorReduction(float damage, Player player) {
        int armor        = player.getArmorValue();
        float protection = armor / 25.0f; // rough: 20 armor = ~80% reduction
        return damage * (1.0f - Math.min(protection, 0.8f));
    }

    /**
     * Returns all valid positions where a crystal can be placed within range.
     */
    public static List<BlockPos> findCrystalBlocks(Player player, float radius) {
        List<BlockPos> result = new ArrayList<>();
        BlockPos origin = BlockPos.containing(player.position());
        int r = (int) Math.ceil(radius);

        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    if (canPlaceCrystal(pos)) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns true if a crystal can be placed on top of this block.
     * Crystals require obsidian or bedrock below, and 2 free blocks above.
     */
    public static boolean canPlaceCrystal(BlockPos pos) {
        Level world = mc.level;
        if (world == null) return false;

        BlockState base = world.getBlockState(pos);
        if (!base.is(net.minecraft.world.level.block.Blocks.OBSIDIAN)
                && !base.is(net.minecraft.world.level.block.Blocks.BEDROCK)) return false;

        // Need 2 free blocks above
        BlockPos above1 = pos.above();
        BlockPos above2 = pos.above(2);

        if (!world.getBlockState(above1).isAir()) return false;
        if (!world.getBlockState(above2).isAir()) return false;

        // No entity occupying the space
        AABB crystalBox = new AABB(above1);
        return world.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, crystalBox, e -> true).isEmpty();
    }
}