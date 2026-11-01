package dev.zombie.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

public final class Util {
    private Util() {}

    public static double lerp(double from, double to, double t) { return from + (to - from) * t; }

    public static LivingEntity nearestTarget(MinecraftClient mc, double range, boolean playersOnly) {
        LivingEntity best = null;
        double bestDist = range * range;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || le == mc.player || !le.isAlive()
                    || le.isSpectator() || le instanceof ArmorStandEntity) continue;
            if (playersOnly && !(le instanceof PlayerEntity)) continue;
            double d = mc.player.squaredDistanceTo(le);
            if (d < bestDist) { bestDist = d; best = le; }
        }
        return best;
    }

    /** Returns {yaw, pitch} needed to look from one point to another. */
    public static float[] rotationsTo(Vec3d from, Vec3d to) {
        double dx = to.x - from.x, dy = to.y - from.y, dz = to.z - from.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, flat));
        return new float[]{yaw, pitch};
    }

    /** Horizontal movement vector from WASD in the player's facing direction, or ZERO if no input. */
    public static Vec3d moveInput(MinecraftClient mc, double speed) {
        GameOptions o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && s == 0) return Vec3d.ZERO;
        double yaw = Math.toRadians(mc.player.getYaw());
        double x = -Math.sin(yaw) * f + Math.cos(yaw) * s;
        double z = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.sqrt(x * x + z * z);
        return new Vec3d(x / len * speed, 0, z / len * speed);
    }
}
