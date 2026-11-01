package dev.zombie.hack;

import dev.zombie.setting.BoolSetting;
import dev.zombie.setting.NumberSetting;
import dev.zombie.util.Util;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/** All modules live here as small nested classes. Each one declares its own sliders/toggles. */
public final class Hacks {
    private Hacks() {}

    // ======================= COMBAT =======================

    public static class KillAura extends Hack {
        private final NumberSetting range = num("Range", 2.0, 6.0, 0.1, 4.0, " blocks");
        private final NumberSetting turn = num("Turn speed", 5, 180, 5, 40, "\u00B0/tick");
        private final BoolSetting playersOnly = bool("Players only", true);

        public KillAura() { super("KillAura", Category.COMBAT, GLFW.GLFW_KEY_R, "Auto-attacks nearby targets."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (mc.currentScreen != null) return;
            LivingEntity target = Util.nearestTarget(mc, range.get(), playersOnly.get());
            if (target == null) return;

            float[] rot = Util.rotationsTo(p.getEyePos(), target.getBoundingBox().getCenter());
            float step = turn.f();
            float dYaw = MathHelper.wrapDegrees(rot[0] - p.getYaw());
            float dPitch = rot[1] - p.getPitch();
            float moveYaw = MathHelper.clamp(dYaw, -step, step);
            float movePitch = MathHelper.clamp(dPitch, -step, step);
            p.setYaw(p.getYaw() + moveYaw);
            p.setPitch(MathHelper.clamp(p.getPitch() + movePitch, -90f, 90f));

            // Only swing once the camera has turned close enough to the target.
            boolean facing = Math.abs(dYaw - moveYaw) <= 15f && Math.abs(dPitch - movePitch) <= 15f;
            if (facing && p.getAttackCooldownProgress(0.5f) >= 0.95f) {
                mc.interactionManager.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public static class TriggerBot extends Hack {
        private final NumberSetting charge = num("Attack charge", 50, 100, 5, 95, "%");
        private final BoolSetting playersOnly = bool("Players only", true);

        public TriggerBot() { super("TriggerBot", Category.COMBAT, -1, "Attacks what you aim at."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (mc.currentScreen != null || p.getAttackCooldownProgress(0.5f) < charge.f() / 100f) return;
            if (mc.crosshairTarget instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target
                    && (!playersOnly.get() || target instanceof PlayerEntity)) {
                mc.interactionManager.attackEntity(p, target);
                p.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public static class AimAssist extends Hack {
        private final NumberSetting range = num("Range", 3, 8, 0.5, 6, " blocks");
        private final NumberSetting speed = num("Speed", 1, 30, 1, 4, "\u00B0/tick");
        private final NumberSetting fov = num("Field of view", 10, 180, 5, 70, "\u00B0");
        private final BoolSetting playersOnly = bool("Players only", true);

        public AimAssist() { super("AimAssist", Category.COMBAT, -1, "Gently steers aim to targets."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (mc.currentScreen != null) return;
            LivingEntity target = Util.nearestTarget(mc, range.get(), playersOnly.get());
            if (target == null) return;
            float[] rot = Util.rotationsTo(p.getEyePos(), target.getBoundingBox().getCenter());
            float dYaw = MathHelper.wrapDegrees(rot[0] - p.getYaw());
            float dPitch = rot[1] - p.getPitch();
            if (Math.abs(dYaw) > fov.get() / 2.0) return;
            float sp = speed.f();
            p.setYaw(p.getYaw() + MathHelper.clamp(dYaw, -sp, sp));
            p.setPitch(MathHelper.clamp(p.getPitch() + MathHelper.clamp(dPitch, -sp * 0.75f, sp * 0.75f), -90f, 90f));
        }
    }

    /**
     * Raises the client-side reach. A server only honours it if the server's own range allows it
     * (on your own server: /attribute <you> minecraft:entity_interaction_range base set 8).
     */
    public static class Reach extends Hack {
        private final NumberSetting entityReach = num("Attack reach", 3, 8, 0.1, 4.5, " blocks");
        private final NumberSetting blockReach = num("Block reach", 4.5, 8, 0.1, 5.0, " blocks");

        public Reach() { super("Reach", Category.COMBAT, -1, "Longer attack and block reach."); }

        @Override public void onTick() { apply(entityReach.get(), blockReach.get()); }
        @Override protected void onDisable() { apply(3.0, 4.5); }

        private void apply(double entity, double block) {
            if (mc.player == null) return;
            set(EntityAttributes.ENTITY_INTERACTION_RANGE, entity);
            set(EntityAttributes.BLOCK_INTERACTION_RANGE, block);
        }

        private void set(RegistryEntry<EntityAttribute> attribute, double value) {
            EntityAttributeInstance inst = mc.player.getAttributeInstance(attribute);
            if (inst != null) inst.setBaseValue(value);
        }
    }

    /** The work is done in EntityMixin. */
    public static class Hitboxes extends Hack {
        private final NumberSetting expand = num("Expand", 0.0, 1.0, 0.05, 0.3, " blocks");
        private final BoolSetting playersOnly = bool("Players only", true);

        public Hitboxes() { super("Hitboxes", Category.COMBAT, -1, "Bigger target hitboxes."); }

        public float margin() { return expand.f(); }
        public boolean playersOnly() { return playersOnly.get(); }
    }

    public static class Criticals extends Hack {
        public Criticals() {
            super("Criticals", Category.COMBAT, -1, "Landing hits count as crits.");
            AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
                if (isEnabled() && player == mc.player && player.isOnGround()
                        && !player.isTouchingWater() && entity instanceof LivingEntity) {
                    double x = player.getX(), y = player.getY(), z = player.getZ();
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, false));
                    mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
                }
                return ActionResult.PASS;
            });
        }
    }

    /** The work is done in ClientPlayNetworkHandlerMixin. 0% on both = no knockback at all. */
    public static class Velocity extends Hack {
        private final NumberSetting horizontal = num("Horizontal", 0, 100, 5, 0, "%");
        private final NumberSetting vertical = num("Vertical", 0, 100, 5, 0, "%");

        public Velocity() { super("Velocity", Category.COMBAT, -1, "Reduces knockback."); }

        public double hScale() { return horizontal.get() / 100.0; }
        public double vScale() { return vertical.get() / 100.0; }
    }

    public static class AutoTotem extends Hack {
        private final NumberSetting delay = num("Delay", 0, 10, 1, 0, " ticks");
        private int waited;

        public AutoTotem() { super("AutoTotem", Category.COMBAT, -1, "Keeps a totem in your offhand."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (mc.currentScreen != null) return;
            if (p.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) { waited = 0; return; }
            if (waited < delay.i()) { waited++; return; }
            for (int i = 0; i < 36; i++) {
                if (p.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) {
                    int slot = i < 9 ? 36 + i : i; // hotbar slots sit at 36-44 in the player screen handler
                    mc.interactionManager.clickSlot(p.playerScreenHandler.syncId, slot, 40, SlotActionType.SWAP, p);
                    waited = 0;
                    return;
                }
            }
        }
    }

    // ======================= MOVEMENT =======================

    public static class Fly extends Hack {
        private final NumberSetting speed = num("Speed", 5, 60, 1, 15, " b/s");
        private final NumberSetting vertical = num("Up/down speed", 2, 30, 1, 10, " b/s");
        private final NumberSetting smoothing = num("Smoothing", 0, 90, 5, 30, "%");

        public Fly() { super("Fly", Category.MOVEMENT, GLFW.GLFW_KEY_G, "Free flight. Space/Shift = up/down."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            double a = 1.0 - smoothing.get() / 100.0;
            Vec3d target = Util.moveInput(mc, speed.get() / 20.0);
            double vs = vertical.get() / 20.0;
            double ty = (mc.options.jumpKey.isPressed() ? vs : 0) - (mc.options.sneakKey.isPressed() ? vs : 0);
            Vec3d v = p.getVelocity();
            p.setVelocity(Util.lerp(v.x, target.x, a), Util.lerp(v.y, ty, a), Util.lerp(v.z, target.z, a));
        }
    }

    public static class Speed extends Hack {
        private final NumberSetting speed = num("Speed", 5, 40, 1, 10, " b/s");
        private final NumberSetting smoothing = num("Smoothing", 0, 90, 5, 20, "%");
        private final BoolSetting air = bool("Works in air", true);

        public Speed() { super("Speed", Category.MOVEMENT, GLFW.GLFW_KEY_X, "Move faster."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            Vec3d target = Util.moveInput(mc, speed.get() / 20.0);
            if (target == Vec3d.ZERO) return;
            if (!air.get() && !p.isOnGround()) return;
            double a = 1.0 - smoothing.get() / 100.0;
            Vec3d v = p.getVelocity();
            p.setVelocity(Util.lerp(v.x, target.x, a), v.y, Util.lerp(v.z, target.z, a));
        }
    }

    public static class NoFall extends Hack {
        private final NumberSetting minFall = num("Trigger height", 2, 10, 1, 3, " blocks");

        public NoFall() { super("NoFall", Category.MOVEMENT, -1, "Cancels fall damage."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (p.fallDistance > minFall.get() && !p.isGliding()) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, p.horizontalCollision));
            }
        }
    }

    public static class Sprint extends Hack {
        public Sprint() { super("Sprint", Category.MOVEMENT, -1, "Always sprint while moving."); }

        @Override public void onTick() {
            ClientPlayerEntity p = mc.player;
            if (mc.options.forwardKey.isPressed() && !p.isSneaking() && !p.horizontalCollision
                    && p.getHungerManager().getFoodLevel() > 6) {
                p.setSprinting(true);
            }
        }
    }

    // ======================= RENDER =======================

    public static class Fullbright extends Hack {
        public Fullbright() { super("Fullbright", Category.RENDER, GLFW.GLFW_KEY_B, "See in the dark."); }

        @Override public void onTick() {
            StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (cur == null || cur.getDuration() < 240) {
                mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
            }
        }

        @Override protected void onDisable() {
            if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
        }
    }

    /** Glowing outline is forced in EntityMixin. */
    public static class ESP extends Hack {
        private final BoolSetting mobs = bool("Include mobs", false);

        public ESP() { super("ESP", Category.RENDER, GLFW.GLFW_KEY_H, "Glowing outlines through walls."); }

        public boolean includeMobs() { return mobs.get(); }
    }
}
